import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import borrowingApi from '../services/borrowingApi';
import bookApi from '../services/bookApi';
import AppNav from '../components/AppNav';
import { recordsWithUnpaidFines, unpaidFinesTotalEuro } from '../utils/borrowUtils';

const MAX_RENEWALS = 3;

function parseUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
        return null;
    }
}

function formatDate(dateStr) {
    if (!dateStr) return '—';
    const [y, m, d] = dateStr;
    return Array.isArray(dateStr)
        ? new Date(y, m - 1, d).toLocaleDateString()
        : new Date(dateStr).toLocaleDateString();
}

function daysUntil(dateStr) {
    if (!dateStr) return null;
    const due = new Date(dateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    due.setHours(0, 0, 0, 0);
    return Math.ceil((due - today) / (1000 * 60 * 60 * 24));
}

function statusBadge(status) {
    const map = {
        ACTIVE:   { bg: '#dbeafe', color: '#2563eb', label: 'Active' },
        RENEWED:  { bg: '#ede9fe', color: '#7c3aed', label: 'Renewed' },
        OVERDUE:  { bg: '#fee2e2', color: '#dc2626', label: 'Overdue' },
        RETURNED: { bg: '#d1fae5', color: '#059669', label: 'Returned' },
    };
    const s = map[status] || { bg: '#e5e7eb', color: '#6b7280', label: status };
    return (
        <span style={{ ...styles.badge, background: s.bg, color: s.color }}>
            {s.label}
        </span>
    );
}

export default function Borrows({ onLogout }) {
    const navigate = useNavigate();
    const user = parseUser();

    // Redirect admins - they don't have borrows
    useEffect(() => {
        if (user?.role === 'ADMIN') {
            navigate('/dashboard', { replace: true });
        }
    }, [user, navigate]);

    const [records, setRecords] = useState([]);
    const [bookTitles, setBookTitles] = useState({});
    const [loading, setLoading] = useState(true);
    const [actionError, setActionError] = useState('');
    const [actionSuccess, setActionSuccess] = useState('');
    const [processing, setProcessing] = useState(null); // recordId being actioned

    const fetchBorrows = useCallback(async () => {
        if (!user?.id) return;
        setLoading(true);
        setActionError('');
        try {
            const res = await borrowingApi.get(`/borrows/user/${user.id}`);
            const data = res.data || [];
            setRecords(data);

            // Fetch titles for any bookIds we don't already have
            const newIds = [...new Set(data.map(r => r.bookId))].filter(id => !bookTitles[id]);
            if (newIds.length > 0) {
                const results = await Promise.allSettled(
                    newIds.map(id => bookApi.get(`/books/${id}`))
                );
                const newTitles = {};
                results.forEach((r, i) => {
                    if (r.status === 'fulfilled') {
                        newTitles[newIds[i]] = r.value.data.title;
                    } else {
                        newTitles[newIds[i]] = `Book #${newIds[i]}`;
                    }
                });
                setBookTitles(prev => ({ ...prev, ...newTitles }));
            }
        } catch {
            setActionError('Failed to load borrow records.');
        } finally {
            setLoading(false);
        }
    // bookTitles is intentionally excluded to prevent re-fetch loop; handled via newIds check
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user?.id]);

    useEffect(() => {
        fetchBorrows();
    }, [fetchBorrows]);

    const handleReturn = async (recordId) => {
        setActionError(''); setActionSuccess(''); setProcessing(recordId);
        try {
            await borrowingApi.put(`/borrows/${recordId}/return`);
            setActionSuccess('Book returned successfully.');
            fetchBorrows();
        } catch (err) {
            setActionError(err.response?.data || 'Failed to return book.');
        } finally {
            setProcessing(null);
        }
    };

    const handleRenew = async (recordId) => {
        setActionError(''); setActionSuccess(''); setProcessing(recordId);
        try {
            await borrowingApi.put(`/borrows/${recordId}/renew`);
            setActionSuccess('Borrow renewed successfully.');
            fetchBorrows();
        } catch (err) {
            setActionError(err.response?.data || 'Failed to renew borrow.');
        } finally {
            setProcessing(null);
        }
    };

    const active   = records.filter(r => r.status === 'ACTIVE' || r.status === 'RENEWED');
    const overdue  = records.filter(r => r.status === 'OVERDUE');
    const history  = records.filter(r => r.status === 'RETURNED');
    const finesDue = recordsWithUnpaidFines(records);
    const finesDueTotal = unpaidFinesTotalEuro(records);

    if (user?.role === 'ADMIN') return null;

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />

            <div style={styles.body}>
                <h2 style={styles.pageTitle}>My Borrows</h2>

                {actionError   && <p style={styles.error}>{actionError}</p>}
                {actionSuccess && <p style={styles.success}>{actionSuccess}</p>}

                {loading ? (
                    <p style={styles.muted}>Loading...</p>
                ) : records.length === 0 ? (
                    <div style={styles.emptyWrap}>
                        <p style={styles.muted}>You have no borrow records.</p>
                        <Link to="/books" style={styles.link}>Browse the catalogue →</Link>
                    </div>
                ) : (
                    <>
                        {/* Unpaid fines (also listed under History with full detail) */}
                        {finesDue.length > 0 && (
                            <Section
                                title={`Fines due (${finesDue.length})`}
                                accent="#b45309"
                                subtitle={
                                    finesDueTotal > 0
                                        ? `Total outstanding: €${finesDueTotal.toFixed(2)}`
                                        : null
                                }
                            >
                                <p style={styles.finesDueHint}>
                                    These amounts were charged when books were returned after the due date. Pay at the library desk unless your institution uses online payment.
                                </p>
                                {finesDue.map(r => (
                                    <BorrowCard
                                        key={r.recordId}
                                        record={r}
                                        title={bookTitles[r.bookId]}
                                        processing={processing}
                                        readOnly
                                        fineEmphasis
                                    />
                                ))}
                            </Section>
                        )}

                        {/* Overdue */}
                        {overdue.length > 0 && (
                            <Section title={`Overdue (${overdue.length})`} accent="#dc2626">
                                {overdue.map(r => (
                                    <BorrowCard
                                        key={r.recordId}
                                        record={r}
                                        title={bookTitles[r.bookId]}
                                        processing={processing}
                                        onReturn={handleReturn}
                                        canRenew={false}
                                    />
                                ))}
                            </Section>
                        )}

                        {/* Active / Renewed */}
                        {active.length > 0 && (
                            <Section title={`Active Borrows (${active.length})`} accent="#2563eb">
                                {active.map(r => (
                                    <BorrowCard
                                        key={r.recordId}
                                        record={r}
                                        title={bookTitles[r.bookId]}
                                        processing={processing}
                                        onReturn={handleReturn}
                                        onRenew={handleRenew}
                                        canRenew={r.renewCount < MAX_RENEWALS}
                                    />
                                ))}
                            </Section>
                        )}

                        {/* History */}
                        {history.length > 0 && (
                            <Section title={`History (${history.length})`} accent="#6b7280">
                                {history.map(r => (
                                    <BorrowCard
                                        key={r.recordId}
                                        record={r}
                                        title={bookTitles[r.bookId]}
                                        processing={processing}
                                        readOnly
                                    />
                                ))}
                            </Section>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

function Section({ title, accent, subtitle, children }) {
    return (
        <div style={styles.section}>
            <h3
                style={{
                    ...styles.sectionTitle,
                    borderLeftColor: accent,
                    marginBottom: subtitle ? '6px' : '12px',
                }}
            >
                {title}
            </h3>
            {subtitle && <p style={styles.sectionSubtitle}>{subtitle}</p>}
            {children}
        </div>
    );
}

function BorrowCard({ record, title, processing, onReturn, onRenew, canRenew, readOnly, fineEmphasis }) {
    const busy = processing === record.recordId;
    const days = daysUntil(record.dueDate);
    const dueSoon = days !== null && days >= 0 && days <= 3;

    return (
        <div style={fineEmphasis ? { ...styles.card, ...styles.cardFineEmphasis } : styles.card}>
            <div style={styles.cardTop}>
                <div style={styles.cardLeft}>
                    <Link to={`/books/${record.bookId}`} style={styles.bookTitle}>
                        {title || `Book #${record.bookId}`}
                    </Link>
                    <div style={styles.meta}>
                        Borrowed: {formatDate(record.borrowDate)}
                        &nbsp;&middot;&nbsp;
                        Due: <span style={dueSoon && !readOnly ? styles.dueSoon : undefined}>
                            {formatDate(record.dueDate)}
                            {dueSoon && !readOnly && ` (${days === 0 ? 'today' : `${days}d`})`}
                        </span>
                        {record.returnDate && (
                            <>&nbsp;&middot;&nbsp;Returned: {formatDate(record.returnDate)}</>
                        )}
                        {record.renewCount > 0 && (
                            <>&nbsp;&middot;&nbsp;Renewed {record.renewCount}&times;</>
                        )}
                    </div>
                    {record.fine && (
                        <div style={fineEmphasis ? { ...styles.fine, ...styles.fineEmphasis } : styles.fine}>
                            <strong>{record.fine.isPaid ? 'Fine (paid)' : 'Fine due'}:</strong>{' '}
                            €{record.fine.amount.toFixed(2)} &middot; {record.fine.daysOverdue}d past due date
                            {record.fine.isPaid ? ' · Paid' : ' · Unpaid'}
                        </div>
                    )}
                </div>
                <div style={styles.cardRight}>
                    {statusBadge(record.status)}
                </div>
            </div>

            {!readOnly && (
                <div style={styles.actions}>
                    {onReturn && (
                        <button
                            onClick={() => onReturn(record.recordId)}
                            disabled={busy}
                            style={styles.btnReturn}
                        >
                            {busy ? '…' : 'Return'}
                        </button>
                    )}
                    {onRenew && (
                        <button
                            onClick={() => onRenew(record.recordId)}
                            disabled={busy || !canRenew}
                            title={!canRenew ? `Max ${MAX_RENEWALS} renewals reached` : undefined}
                            style={canRenew ? styles.btnRenew : styles.btnRenewDisabled}
                        >
                            {busy ? '…' : `Renew${!canRenew ? ` (${MAX_RENEWALS}/${MAX_RENEWALS})` : ` (${record.renewCount}/${MAX_RENEWALS})`}`}
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}

const styles = {
    page:      { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    body:      { padding: '32px', maxWidth: '860px', margin: '0 auto', flex: 1, width: '100%', boxSizing: 'border-box' },
    pageTitle: { fontSize: '24px', marginBottom: '24px', color: '#111' },

    error:   { color: '#dc2626', fontSize: '13px', marginBottom: '16px', padding: '10px', background: '#fee2e2', borderRadius: '6px' },
    success: { color: '#059669', fontSize: '13px', marginBottom: '16px', padding: '10px', background: '#d1fae5', borderRadius: '6px' },
    muted:   { color: '#999', padding: '40px 0', textAlign: 'center' },

    emptyWrap: { textAlign: 'center', paddingTop: '40px' },
    link: { color: '#2563eb', fontSize: '14px' },

    section:         { marginBottom: '32px' },
    sectionTitle:    { fontSize: '16px', fontWeight: '600', color: '#333', borderLeft: '3px solid', paddingLeft: '10px' },
    sectionSubtitle: { fontSize: '14px', fontWeight: '600', color: '#92400e', margin: '0 0 10px 10px' },
    finesDueHint:    { fontSize: '12px', color: '#78716c', margin: '0 0 12px 0', lineHeight: 1.45 },

    card:    { background: '#fff', padding: '16px 20px', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: '10px' },
    cardFineEmphasis: {
        border: '1px solid #f59e0b',
        background: '#fffbeb',
        boxShadow: '0 1px 6px rgba(245, 158, 11, 0.2)',
    },
    cardTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' },
    cardLeft:  { flex: 1 },
    cardRight: { flexShrink: 0 },

    bookTitle: { fontSize: '15px', fontWeight: '600', color: '#2563eb', textDecoration: 'none', display: 'block', marginBottom: '4px' },
    meta:    { fontSize: '13px', color: '#666', lineHeight: '1.5' },
    dueSoon: { color: '#d97706', fontWeight: '600' },
    fine:         { fontSize: '12px', color: '#dc2626', marginTop: '4px' },
    fineEmphasis: { fontSize: '13px', color: '#92400e', marginTop: '8px' },

    actions:  { display: 'flex', gap: '8px', marginTop: '12px', paddingTop: '10px', borderTop: '1px solid #f0f0f0' },
    btnReturn: { padding: '6px 16px', background: '#059669', color: '#fff', border: 'none', borderRadius: '5px', fontSize: '13px', cursor: 'pointer' },
    btnRenew:  { padding: '6px 16px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '5px', fontSize: '13px', cursor: 'pointer' },
    btnRenewDisabled: { padding: '6px 16px', background: '#e5e7eb', color: '#9ca3af', border: 'none', borderRadius: '5px', fontSize: '13px', cursor: 'not-allowed' },

    badge: { padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: '600' },
};
