import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import AppNav from '../components/AppNav';
import notificationApi from '../services/notificationApi';
import api from '../services/api';
import borrowingApi from '../services/borrowingApi';
import bookApi from '../services/bookApi';

function parseStoredUserId() {
    try {
        const raw = localStorage.getItem('user');
        if (!raw) return null;
        const user = JSON.parse(raw);
        return user?.id ?? user?.userId ?? null;
    } catch {
        return null;
    }
}

function formatWhen(value) {
    if (!value) return '—';
    const dt = new Date(value);
    return Number.isNaN(dt.getTime()) ? String(value) : dt.toLocaleString();
}

function typeLabel(type) {
    switch (type) {
        case 'BOOK_BORROWED': return 'Borrow confirmed';
        case 'BOOK_RETURNED': return 'Return confirmed';
        case 'BOOK_RENEWED': return 'Renewed';
        case 'BOOK_OVERDUE': return 'Overdue';
        case 'FINE_APPLIED': return 'Fine applied';
        case 'DUE_SOON_WEEK': return 'Due in ~1 week';
        case 'DUE_SOON_DAY': return 'Due tomorrow';
        case 'REMINDER': return 'Reminder';
        default: return type || 'Notification';
    }
}

function prettyBookLabel(book) {
    if (!book) return null;
    const title = book.title || null;
    const author = book.author || null;
    if (title && author) return `${title} by ${author}`;
    return title || author || null;
}

function enrichRows(rows, recordToBook, booksById) {
    return rows.map((n) => {
        const bookId = recordToBook.get(n.recordId);
        const book = bookId ? booksById.get(bookId) : null;
        const bookLabel = prettyBookLabel(book);
        const defaultMessage = n.message || '—';
        let displayMessage = defaultMessage;
        if (bookLabel) {
            if (n.type === 'BOOK_BORROWED') displayMessage = `Borrow confirmed: ${bookLabel}`;
            if (n.type === 'BOOK_RENEWED') displayMessage = `Renewal confirmed: ${bookLabel}`;
            if (n.type === 'BOOK_RETURNED') displayMessage = `Return confirmed: ${bookLabel}`;
            if (n.type === 'BOOK_OVERDUE') displayMessage = `Overdue notice: ${bookLabel}`;
            if (n.type === 'DUE_SOON_WEEK') displayMessage = `Due in about a week: ${bookLabel}`;
            if (n.type === 'DUE_SOON_DAY') displayMessage = `Due tomorrow: ${bookLabel}`;
        }
        return { ...n, displayMessage };
    });
}

export default function Notifications({ onLogout }) {
    const [userId, setUserId] = useState(null);
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            setError('');
            try {
                let id = parseStoredUserId();
                if (!id) {
                    const meRes = await api.get('/auth/me');
                    id = meRes.data?.id ?? null;
                }
                setUserId(id);
                if (!id) {
                    setRows([]);
                    return;
                }
                const res = await notificationApi.get(`/notifications/users/${id}/recent`, { params: { limit: 100 } });
                const baseRows = Array.isArray(res.data) ? res.data : [];
                const borrowRes = await borrowingApi.get(`/borrows/user/${id}`);
                const borrows = Array.isArray(borrowRes.data) ? borrowRes.data : [];
                const recordToBook = new Map(
                    borrows.map((r) => [r.recordId, r.bookId]).filter(([recordId, bookId]) => recordId && bookId)
                );
                const neededBookIds = [...new Set(baseRows
                    .map((n) => recordToBook.get(n.recordId))
                    .filter(Boolean))];
                const booksById = new Map();
                if (neededBookIds.length > 0) {
                    const results = await Promise.allSettled(
                        neededBookIds.map((bookId) => bookApi.get(`/books/${bookId}`))
                    );
                    results.forEach((result, idx) => {
                        if (result.status === 'fulfilled') {
                            booksById.set(neededBookIds[idx], result.value.data);
                        }
                    });
                }
                setRows(enrichRows(baseRows, recordToBook, booksById));
            } catch {
                setError('Could not load notifications right now.');
                setRows([]);
            } finally {
                setLoading(false);
            }
        };
        load();
    }, []);

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <main style={styles.main}>
                <h2 style={styles.title}>Notifications</h2>
                <p style={styles.lead}>
                    Event updates for your account: borrow/renew/return confirmations, scheduled due-soon reminders (about one week and the day before due), overdue alerts, and fine notices.
                </p>
                {error && <p style={styles.error}>{error}</p>}
                {loading ? (
                    <p style={styles.muted}>Loading…</p>
                ) : rows.length === 0 ? (
                    <p style={styles.muted}>No notifications yet.</p>
                ) : (
                    <div style={styles.list}>
                        {rows.map((n) => (
                            <article key={n.notificationId} style={styles.card}>
                                <div style={styles.cardHead}>
                                    <span style={styles.badge}>{typeLabel(n.type)}</span>
                                    <span style={styles.when}>{formatWhen(n.createdAt)}</span>
                                </div>
                                <p style={styles.msg}>{n.displayMessage || n.message || '—'}</p>
                                <div style={styles.meta}>
                                    <span>Record: {n.recordId ?? '—'}</span>
                                    <span>Status: {n.status || 'PENDING'}</span>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
                {userId && (
                    <p style={styles.backHint}>
                        Need current borrow state? <Link to="/borrows">Open My Borrows</Link>.
                    </p>
                )}
            </main>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    main: { padding: '30px 32px', maxWidth: '920px', margin: '0 auto', width: '100%', boxSizing: 'border-box' },
    title: { margin: '0 0 8px 0', color: '#111', fontSize: '24px' },
    lead: { margin: '0 0 18px 0', color: '#555', fontSize: '14px' },
    error: { color: '#b91c1c', background: '#fee2e2', borderRadius: '8px', padding: '10px 12px' },
    muted: { color: '#777' },
    list: { display: 'grid', gap: '10px' },
    card: { background: '#fff', border: '1px solid #e5e7eb', borderRadius: '10px', padding: '12px 14px' },
    cardHead: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px', marginBottom: '6px' },
    badge: { fontSize: '12px', fontWeight: 700, color: '#1d4ed8', background: '#dbeafe', borderRadius: '999px', padding: '3px 8px' },
    when: { fontSize: '12px', color: '#666' },
    msg: { margin: '0 0 6px 0', color: '#111', fontSize: '14px' },
    meta: { display: 'flex', gap: '14px', fontSize: '12px', color: '#666' },
    backHint: { marginTop: '14px', fontSize: '13px', color: '#555' },
};
