import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import borrowingApi from '../services/borrowingApi';
import AppNav from '../components/AppNav';
import { recordsWithUnpaidFines, unpaidFinesTotalEuro } from '../utils/borrowUtils';

function Dashboard({ onLogout }) {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Borrow summary - only fetched for non-admins
    const [borrowSummary, setBorrowSummary] = useState(null);

    useEffect(() => {
        const init = async () => {
            try {
                const res = await api.get('/auth/me');
                const me = res.data;
                setUser(me);

                if (me.role !== 'ADMIN') {
                    try {
                        const borrows = await borrowingApi.get(`/borrows/user/${me.id}`);
                        const data = borrows.data || [];
                        const active  = data.filter(r => r.status === 'ACTIVE' || r.status === 'RENEWED').length;
                        const overdue = data.filter(r => r.status === 'OVERDUE').length;
                        const unpaidList = recordsWithUnpaidFines(data);
                        const unpaidTotal = unpaidFinesTotalEuro(data);
                        setBorrowSummary({
                            active,
                            overdue,
                            total: data.length,
                            unpaidCount: unpaidList.length,
                            unpaidTotal,
                        });
                    } catch {
                        // Borrowing service may be unavailable; summary stays null
                    }
                }
            } catch {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                if (onLogout) onLogout();
                navigate('/login');
            } finally {
                setLoading(false);
            }
        };
        init();
    }, [navigate, onLogout]);

    if (loading) {
        return (
            <div style={styles.page}>
                <AppNav onLogout={onLogout} />
                <div style={styles.loadingWrap}><p>Loading...</p></div>
            </div>
        );
    }

    const isAdmin = user?.role === 'ADMIN';

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <div style={styles.main}>
                <h2 style={styles.heading}>Welcome back{user?.name ? `, ${user.name}` : ''}</h2>

                {/* Borrow summary - only for students and staff */}
                {!isAdmin && borrowSummary !== null && (
                    <>
                        {borrowSummary.unpaidCount > 0 && (
                            <div style={styles.fineAlert} role="status">
                                <p style={styles.fineAlertTitle}>You have unpaid library fines</p>
                                <p style={styles.fineAlertBody}>
                                    {borrowSummary.unpaidCount} outstanding charge
                                    {borrowSummary.unpaidCount === 1 ? '' : 's'} totaling{' '}
                                    <strong>€{borrowSummary.unpaidTotal.toFixed(2)}</strong>. See{' '}
                                    <Link to="/borrows" style={styles.fineAlertLink}>My Borrows</Link> for details.
                                </p>
                            </div>
                        )}
                        <div style={styles.summaryRow}>
                            <SummaryCard
                                label="Active borrows"
                                value={borrowSummary.active}
                                accent="#2563eb"
                                bg="#dbeafe"
                            />
                            <SummaryCard
                                label="Overdue"
                                value={borrowSummary.overdue}
                                accent={borrowSummary.overdue > 0 ? '#dc2626' : '#059669'}
                                bg={borrowSummary.overdue > 0 ? '#fee2e2' : '#d1fae5'}
                            />
                            <SummaryCard
                                label="Unpaid fines"
                                value={borrowSummary.unpaidCount}
                                accent={borrowSummary.unpaidCount > 0 ? '#b45309' : '#6b7280'}
                                bg={borrowSummary.unpaidCount > 0 ? '#fef3c7' : '#f3f4f6'}
                            />
                            <SummaryCard
                                label="Total borrows"
                                value={borrowSummary.total}
                                accent="#6b7280"
                                bg="#f3f4f6"
                            />
                        </div>
                    </>
                )}

                {!isAdmin && (
                    <div style={styles.linkRow}>
                        <Link to="/borrows" style={styles.primaryLink}>View my borrows →</Link>
                        <Link to="/books" style={styles.secondaryLink}>Browse catalogue</Link>
                    </div>
                )}

                {isAdmin && (
                    <div style={styles.summaryRow}>
                        <AdminNavCard
                            to="/books"
                            title="Catalogue"
                            description="Search and manage books, categories, and removals."
                            cta="Open catalogue →"
                            accent="#2563eb"
                            bg="#dbeafe"
                        />
                        <AdminNavCard
                            to="/admin/fines"
                            title="Fines"
                            description="Review charges and mark desk payments as paid."
                            cta="Open fines →"
                            accent="#b45309"
                            bg="#fef3c7"
                        />
                        <AdminNavCard
                            to="/admin/testing"
                            title="Borrowing - testing"
                            description="Set due dates and run the overdue job manually to test fine functionality without waiting or editing the DB by hand."
                            cta="Open testing tools →"
                            accent="#7c3aed"
                            bg="#ede9fe"
                        />
                    </div>
                )}
            </div>
        </div>
    );
}

/** Same shell as patron {@link SummaryCard}: centered tile, accent title, hover lift. */
function AdminNavCard({ to, title, description, cta, accent, bg }) {
    const [hover, setHover] = useState(false);
    return (
        <Link
            to={to}
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
            style={{
                ...styles.summaryCardLink,
                ...(hover ? styles.summaryCardLinkHover : {}),
            }}
            aria-label={`${title}. ${cta}`}
        >
            <div style={{ ...styles.summaryCard, background: bg, borderColor: accent }}>
                <span style={{ ...styles.summaryValue, fontSize: '22px', color: accent }}>{title}</span>
                <span style={styles.adminNavDescription}>{description}</span>
                <span style={{ ...styles.adminNavCta, color: accent }}>{cta}</span>
            </div>
        </Link>
    );
}

function SummaryCard({ label, value, accent, bg }) {
    const [hover, setHover] = useState(false);
    return (
        <Link
            to="/borrows"
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
            style={{
                ...styles.summaryCardLink,
                ...(hover ? styles.summaryCardLinkHover : {}),
            }}
            aria-label={`${label}: ${value}. Open My Borrows`}
        >
            <div style={{ ...styles.summaryCard, background: bg, borderColor: accent }}>
                <span style={{ ...styles.summaryValue, color: accent }}>{value}</span>
                <span style={styles.summaryLabel}>{label}</span>
            </div>
        </Link>
    );
}

const styles = {
    page:        { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    loadingWrap: { padding: '40px 32px', textAlign: 'center', color: '#999', flex: 1 },
    main:        { padding: '40px 32px', maxWidth: '960px', margin: '0 auto', flex: 1, width: '100%', boxSizing: 'border-box' },
    heading:     { fontSize: '24px', marginBottom: '24px', color: '#111' },

    fineAlert:     { marginBottom: '20px', padding: '14px 16px', borderRadius: '10px', background: '#fffbeb', border: '1px solid #f59e0b', maxWidth: '520px' },
    fineAlertTitle:{ fontSize: '15px', fontWeight: '700', color: '#92400e', margin: '0 0 6px 0' },
    fineAlertBody: { fontSize: '13px', color: '#78350f', margin: 0, lineHeight: 1.5 },
    fineAlertLink: { color: '#b45309', fontWeight: '600' },

    summaryRow:  { display: 'flex', gap: '16px', marginBottom: '24px', flexWrap: 'wrap' },
    summaryCardLink: {
        flex: '1 1 140px',
        textDecoration: 'none',
        color: 'inherit',
        borderRadius: '12px',
        outline: 'none',
    },
    summaryCardLinkHover: {
        transform: 'translateY(-2px)',
        boxShadow: '0 6px 16px rgba(0,0,0,0.08)',
    },
    summaryCard: { padding: '20px', borderRadius: '10px', border: '1px solid', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px', height: '100%', boxSizing: 'border-box' },
    summaryValue: { fontSize: '32px', fontWeight: '700', lineHeight: 1 },
    summaryLabel: { fontSize: '13px', color: '#555', textAlign: 'center' },
    adminNavDescription: {
        fontSize: '12px',
        color: '#666',
        textAlign: 'center',
        lineHeight: 1.45,
        maxWidth: '240px',
    },
    adminNavCta: { fontSize: '12px', fontWeight: '600', textAlign: 'center', marginTop: '2px' },

    linkRow:       { display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' },
    primaryLink:   { color: '#2563eb', fontSize: '14px', fontWeight: '600' },
    secondaryLink: { color: '#666', fontSize: '14px' },

};

export default Dashboard;
