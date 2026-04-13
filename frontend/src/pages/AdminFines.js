import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import borrowingApi from '../services/borrowingApi';
import AppNav from '../components/AppNav';

function parseUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
        return null;
    }
}

/** Normalise API row (camelCase + legacy snake_case). */
function normalizeFine(raw) {
    if (!raw || typeof raw !== 'object') return null;
    const paid = raw.paid === true || raw.isPaid === true;
    return {
        ...raw,
        paid,
        fineId: raw.fineId ?? raw.fine_id,
        patronName: raw.patronName ?? raw.patron_name ?? '—',
        patronEmail: raw.patronEmail ?? raw.patron_email ?? '—',
        bookTitle: raw.bookTitle ?? raw.book_title ?? '—',
        bookAuthor: raw.bookAuthor ?? raw.book_author ?? '—',
        bookIsbn: raw.bookIsbn ?? raw.book_isbn ?? '—',
    };
}

function normalizeFinesList(payload) {
    if (Array.isArray(payload)) return payload.map(normalizeFine).filter(Boolean);
    return [];
}

function normalizeOverdueList(payload) {
    if (!Array.isArray(payload)) return [];
    return payload
        .filter((r) => r && typeof r === 'object')
        .map((r) => ({
            patronName: r.patronName ?? '—',
            patronEmail: r.patronEmail ?? '—',
            bookTitle: r.bookTitle ?? '—',
            bookAuthor: r.bookAuthor ?? '—',
            bookIsbn: r.bookIsbn ?? '—',
            borrowDate: r.borrowDate,
            dueDate: r.dueDate,
            renewCount: r.renewCount ?? 0,
            status: r.status,
        }));
}

function formatDateOnly(value) {
    if (!value) return '—';
    if (Array.isArray(value)) {
        const [y, mo, d] = value;
        if (!y || !mo || !d) return '—';
        return new Date(y, mo - 1, d).toLocaleDateString();
    }
    const s = String(value);
    if (s.includes('T')) {
        const dt = new Date(s);
        return Number.isNaN(dt.getTime()) ? s : dt.toLocaleDateString();
    }
    const dt = new Date(`${s}T00:00:00`);
    return Number.isNaN(dt.getTime()) ? s : dt.toLocaleDateString();
}

function formatDt(val) {
    if (!val) return '—';
    if (Array.isArray(val)) {
        const [y, mo, d, h = 0, min = 0, s = 0, nano = 0] = val;
        return new Date(y, mo - 1, d, h, min, s, Math.floor(nano / 1e6)).toLocaleString();
    }
    try {
        const d = new Date(val);
        return Number.isNaN(d.getTime()) ? String(val) : d.toLocaleString();
    } catch {
        return String(val);
    }
}

export default function AdminFines({ onLogout }) {
    const navigate = useNavigate();
    const [fines, setFines] = useState([]);
    const [overdueRecords, setOverdueRecords] = useState([]);
    const [initialLoad, setInitialLoad] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [filter, setFilter] = useState('all');
    const [payingId, setPayingId] = useState(null);
    const mountedRef = useRef(true);

    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
        };
    }, []);

    const loadFines = useCallback(async (mode = 'initial') => {
        // initial: first full-page load | refresh: user clicked Refresh | silent: reload after mark paid (no flicker)
        if (mode === 'silent') {
            /* no loading flags */
        } else if (mode === 'refresh') {
            setRefreshing(true);
            setError('');
        } else {
            setInitialLoad(true);
            setError('');
        }

        try {
            const [finesRes, overdueRes] = await Promise.all([
                borrowingApi.get('/fines'),
                borrowingApi.get('/borrows/admin/overdue'),
            ]);
            const list = normalizeFinesList(finesRes.data);
            const overdueList = normalizeOverdueList(overdueRes.data);
            if (mountedRef.current) {
                setFines(list);
                setOverdueRecords(overdueList);
            }
        } catch {
            if (mountedRef.current) {
                setError('Could not load fines. Is borrowing-service running?');
                setFines([]);
                setOverdueRecords([]);
            }
        } finally {
            if (mountedRef.current) {
                setInitialLoad(false);
                setRefreshing(false);
            }
        }
    }, []);

    useEffect(() => {
        const u = parseUser();
        if (u?.role !== 'ADMIN') {
            navigate('/dashboard', { replace: true });
            return;
        }
        loadFines('initial');
    }, [navigate, loadFines]);

    const handleMarkPaid = async (fineId) => {
        setError('');
        setSuccess('');
        setPayingId(fineId);
        try {
            await borrowingApi.put(`/fines/${fineId}/pay`);
            setSuccess('Fine recorded as paid.');
            await loadFines('silent');
        } catch (err) {
            const msg =
                typeof err.response?.data === 'string'
                    ? err.response.data
                    : err.response?.data?.message || err.message;
            setError(msg || 'Could not update fine.');
        } finally {
            setPayingId(null);
        }
    };

    const u = parseUser();
    if (u?.role !== 'ADMIN') return null;

    const filtered =
        filter === 'paid'
            ? fines.filter((f) => f.paid)
            : filter === 'unpaid'
              ? fines.filter((f) => !f.paid)
              : fines;

    const unpaidCount = fines.filter((f) => !f.paid).length;
    const paidCount = fines.filter((f) => f.paid).length;

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <div style={styles.body}>
                <h2 style={styles.title}>Fines</h2>
                <p style={styles.lead}>
                    Outstanding and settled fines from late returns. Patron contact is email (campus directory). Use &ldquo;Mark paid&rdquo; when payment is received at the desk.
                </p>

                {error && <p style={styles.error}>{error}</p>}
                {success && <p style={styles.success}>{success}</p>}

                <div style={styles.toolbar}>
                    <span style={styles.counts}>
                        Total {fines.length} &middot; <strong>Unpaid {unpaidCount}</strong> &middot; Paid {paidCount}
                    </span>
                    <div style={styles.filters}>
                        {['all', 'unpaid', 'paid'].map((key) => (
                            <button
                                key={key}
                                type="button"
                                onClick={() => setFilter(key)}
                                style={{
                                    ...styles.filterBtn,
                                    ...(filter === key ? styles.filterBtnActive : {}),
                                }}
                            >
                                {key === 'all' ? 'All' : key === 'unpaid' ? 'Unpaid' : 'Paid'}
                            </button>
                        ))}
                    </div>
                    <button
                        type="button"
                        onClick={() => loadFines('refresh')}
                        style={styles.refreshBtn}
                        disabled={refreshing || initialLoad}
                    >
                        {refreshing ? 'Refreshing…' : 'Refresh'}
                    </button>
                </div>

                {initialLoad ? (
                    <p style={styles.muted}>Loading…</p>
                ) : filtered.length === 0 ? (
                    <p style={styles.muted}>No fines in this view.</p>
                ) : (
                    <div style={{ ...styles.tableWrap, opacity: refreshing ? 0.65 : 1, transition: 'opacity 0.15s ease' }}>
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={{ ...styles.th, ...styles.thWide }}>Patron</th>
                                    <th style={{ ...styles.th, ...styles.thWide }}>Book</th>
                                    <th style={styles.th}>Amount</th>
                                    <th style={styles.th}>Days overdue</th>
                                    <th style={styles.th}>Issued</th>
                                    <th style={styles.th}>Status</th>
                                    <th style={styles.th}>Paid at</th>
                                    <th style={styles.th} />
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.map((f) => (
                                    <tr key={f.fineId}>
                                        <td style={styles.td}>
                                            <div style={styles.patronName}>{f.patronName}</div>
                                            <div style={styles.mutedSmall}>{f.patronEmail}</div>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={styles.bookTitle}>{f.bookTitle}</div>
                                            <div style={styles.mutedSmall}>{f.bookAuthor}</div>
                                            <div style={styles.isbnLine}>ISBN {f.bookIsbn}</div>
                                        </td>
                                        <td style={styles.td}>€{Number(f.amount).toFixed(2)}</td>
                                        <td style={styles.td}>{f.daysOverdue}</td>
                                        <td style={styles.td}>{formatDt(f.issuedAt)}</td>
                                        <td style={styles.td}>
                                            {f.paid ? (
                                                <span style={styles.badgePaid}>Paid</span>
                                            ) : (
                                                <span style={styles.badgeUnpaid}>Unpaid</span>
                                            )}
                                        </td>
                                        <td style={styles.td}>{formatDt(f.paidAt)}</td>
                                        <td style={styles.tdRight}>
                                            {!f.paid && (
                                                <button
                                                    type="button"
                                                    disabled={payingId === f.fineId}
                                                    onClick={() => handleMarkPaid(f.fineId)}
                                                    style={styles.payBtn}
                                                >
                                                    {payingId === f.fineId ? '…' : 'Mark paid'}
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <h3 style={styles.subTitle}>Overdue Books (not yet returned)</h3>
                <p style={styles.leadSmall}>
                    These loans are currently overdue and still active, so no fine is issued yet. Fine is calculated on return.
                </p>
                {initialLoad ? (
                    <p style={styles.muted}>Loading…</p>
                ) : overdueRecords.length === 0 ? (
                    <p style={styles.muted}>No currently overdue active loans.</p>
                ) : (
                    <div style={styles.overdueWrap}>
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={{ ...styles.th, ...styles.thWide }}>Patron</th>
                                    <th style={{ ...styles.th, ...styles.thWide }}>Book</th>
                                    <th style={styles.th}>Borrowed</th>
                                    <th style={styles.th}>Due</th>
                                    <th style={styles.th}>Renewals</th>
                                    <th style={styles.th}>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {overdueRecords.map((r, idx) => (
                                    <tr key={`${r.patronEmail}-${r.bookIsbn}-${r.dueDate}-${idx}`}>
                                        <td style={styles.td}>
                                            <div style={styles.patronName}>{r.patronName}</div>
                                            <div style={styles.mutedSmall}>{r.patronEmail}</div>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={styles.bookTitle}>{r.bookTitle}</div>
                                            <div style={styles.mutedSmall}>{r.bookAuthor}</div>
                                            <div style={styles.isbnLine}>ISBN {r.bookIsbn}</div>
                                        </td>
                                        <td style={styles.td}>{formatDateOnly(r.borrowDate)}</td>
                                        <td style={styles.td}>{formatDateOnly(r.dueDate)}</td>
                                        <td style={styles.td}>{r.renewCount}</td>
                                        <td style={styles.td}><span style={styles.badgeUnpaid}>{r.status}</span></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    body: { padding: '32px', maxWidth: '1100px', margin: '0 auto', flex: 1, width: '100%', boxSizing: 'border-box' },
    title: { fontSize: '24px', marginBottom: '8px', color: '#111' },
    subTitle: { fontSize: '20px', marginTop: '28px', marginBottom: '8px', color: '#111' },
    lead: { fontSize: '14px', color: '#555', marginBottom: '20px', lineHeight: 1.5, maxWidth: '640px' },
    leadSmall: { fontSize: '13px', color: '#666', marginBottom: '12px', lineHeight: 1.5 },

    error: { color: '#dc2626', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#fee2e2', borderRadius: '6px' },
    success: { color: '#059669', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#d1fae5', borderRadius: '6px' },
    muted: { color: '#888', padding: '24px 0' },

    toolbar: {
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: '12px',
        marginBottom: '16px',
    },
    counts: { fontSize: '13px', color: '#444' },
    filters: { display: 'flex', gap: '6px' },
    filterBtn: {
        padding: '6px 12px',
        fontSize: '13px',
        border: '1px solid #ddd',
        borderRadius: '6px',
        background: '#fff',
        cursor: 'pointer',
        color: '#555',
    },
    filterBtnActive: {
        borderColor: '#2563eb',
        background: '#eff6ff',
        color: '#1d4ed8',
        fontWeight: '600',
    },
    refreshBtn: {
        padding: '6px 12px',
        fontSize: '13px',
        border: '1px solid #ccc',
        borderRadius: '6px',
        background: '#fff',
        cursor: 'pointer',
        marginLeft: 'auto',
    },

    tableWrap: { overflowX: 'auto', background: '#fff', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' },
    overdueWrap: { overflowX: 'auto', background: '#fff', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: '8px' },
    table: { width: '100%', borderCollapse: 'collapse', fontSize: '13px' },
    th: {
        textAlign: 'left',
        padding: '12px 14px',
        borderBottom: '1px solid #e5e7eb',
        color: '#374151',
        fontWeight: '600',
        whiteSpace: 'nowrap',
    },
    thWide: { whiteSpace: 'normal', minWidth: '160px', verticalAlign: 'bottom' },
    td: { padding: '10px 14px', borderBottom: '1px solid #f3f4f6', color: '#333', verticalAlign: 'top' },
    patronName: { fontWeight: '600', color: '#111', marginBottom: '4px' },
    bookTitle: { fontWeight: '600', color: '#111', marginBottom: '4px' },
    mutedSmall: { fontSize: '12px', color: '#666', lineHeight: 1.4 },
    isbnLine: { fontSize: '11px', color: '#888', marginTop: '4px' },
    tdRight: { padding: '10px 14px', borderBottom: '1px solid #f3f4f6', textAlign: 'right' },

    badgePaid: {
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '10px',
        fontSize: '11px',
        fontWeight: '600',
        background: '#d1fae5',
        color: '#059669',
    },
    badgeUnpaid: {
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '10px',
        fontSize: '11px',
        fontWeight: '600',
        background: '#fef3c7',
        color: '#b45309',
    },
    payBtn: {
        padding: '6px 12px',
        fontSize: '12px',
        fontWeight: '600',
        background: '#059669',
        color: '#fff',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
    },
};
