import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import borrowingApi from '../services/borrowingApi';
import AppNav from '../components/AppNav';

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
                        setBorrowSummary({ active, overdue, total: data.length });
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
                            label="Total borrows"
                            value={borrowSummary.total}
                            accent="#6b7280"
                            bg="#f3f4f6"
                        />
                    </div>
                )}

                {!isAdmin && (
                    <div style={styles.linkRow}>
                        <Link to="/borrows" style={styles.primaryLink}>View my borrows →</Link>
                        <Link to="/books" style={styles.secondaryLink}>Browse catalogue</Link>
                    </div>
                )}

                {isAdmin && (
                    <div style={styles.linkRow}>
                        <Link to="/books" style={styles.primaryLink}>Manage catalogue →</Link>
                    </div>
                )}
            </div>
        </div>
    );
}

function SummaryCard({ label, value, accent, bg }) {
    return (
        <div style={{ ...styles.summaryCard, background: bg, borderColor: accent }}>
            <span style={{ ...styles.summaryValue, color: accent }}>{value}</span>
            <span style={styles.summaryLabel}>{label}</span>
        </div>
    );
}

const styles = {
    page:        { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    loadingWrap: { padding: '40px 32px', textAlign: 'center', color: '#999', flex: 1 },
    main:        { padding: '40px 32px', maxWidth: '640px', margin: '0 auto', flex: 1, width: '100%', boxSizing: 'border-box' },
    heading:     { fontSize: '24px', marginBottom: '24px', color: '#111' },

    summaryRow:  { display: 'flex', gap: '16px', marginBottom: '24px', flexWrap: 'wrap' },
    summaryCard: { flex: '1 1 140px', padding: '20px', borderRadius: '10px', border: '1px solid', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' },
    summaryValue: { fontSize: '32px', fontWeight: '700', lineHeight: 1 },
    summaryLabel: { fontSize: '13px', color: '#555', textAlign: 'center' },

    linkRow:       { display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' },
    primaryLink:   { color: '#2563eb', fontSize: '14px', fontWeight: '600' },
    secondaryLink: { color: '#666', fontSize: '14px' },
};

export default Dashboard;
