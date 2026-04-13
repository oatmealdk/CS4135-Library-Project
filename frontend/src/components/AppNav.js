import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Link, NavLink, useNavigate, useLocation } from 'react-router-dom';
import notificationApi from '../services/notificationApi';
import api from '../services/api';
import borrowingApi from '../services/borrowingApi';
import bookApi from '../services/bookApi';
import { clearSession, parseUser, getToken } from '../services/auth';

function BellIcon() {
    return (
        <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
            <path
                d="M12 3a5 5 0 0 0-5 5v2.9c0 .8-.3 1.6-.9 2.2L4.8 14.4a1 1 0 0 0 .7 1.7h13a1 1 0 0 0 .7-1.7l-1.3-1.3c-.6-.6-.9-1.4-.9-2.2V8a5 5 0 0 0-5-5Z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
            <path
                d="M9.5 18a2.5 2.5 0 0 0 5 0"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    );
}

function typeLabel(type) {
    switch (type) {
        case 'BOOK_BORROWED': return 'Book borrowed';
        case 'BOOK_RETURNED': return 'Book returned';
        case 'BOOK_RENEWED': return 'Book renewed';
        case 'BOOK_OVERDUE': return 'Book overdue';
        case 'FINE_APPLIED': return 'Fine applied';
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
        }
        return { ...n, displayMessage };
    });
}

function getNotificationMarker(notification) {
    if (!notification) return null;
    const idPart = notification.notificationId ?? 'none';
    const createdPart = notification.createdAt ?? 'none';
    return `${idPart}|${createdPart}`;
}

function toEpoch(value) {
    if (!value) return Number.NaN;
    const t = Date.parse(value);
    return Number.isNaN(t) ? Number.NaN : t;
}

function AppNav({ onLogout }) {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const user = parseUser();
    const hasToken = !!getToken();

    const logout = async () => {
        try {
            await api.post('/auth/logout');
        } catch (error) {
            // Ignore logout API failures and clear local state anyway.
        }
        clearSession();
        if (onLogout) onLogout();
        navigate('/login');
    };

    const onAuthPage = pathname === '/login' || pathname === '/register';
    const showGuestNav = onAuthPage && !hasToken;

    const navLinkStyle = ({ isActive }) =>
        isActive ? { ...styles.navLink, ...styles.navActive } : styles.navLink;
    const [notifOpen, setNotifOpen] = useState(false);
    const [notifRows, setNotifRows] = useState([]);
    const [notifLoading, setNotifLoading] = useState(false);
    const [hasUnread, setHasUnread] = useState(false);
    const menuRef = useRef(null);
    const userId = useMemo(() => user?.id ?? user?.userId ?? null, [user]);
    const [resolvedUserId, setResolvedUserId] = useState(userId);

    useEffect(() => {
        setResolvedUserId(userId);
    }, [userId]);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (resolvedUserId || !token) return;
        api.get('/auth/me')
            .then((res) => setResolvedUserId(res.data?.id ?? null))
            .catch(() => {});
    }, [resolvedUserId]);

    useEffect(() => {
        const onDocClick = (e) => {
            if (menuRef.current && !menuRef.current.contains(e.target)) {
                setNotifOpen(false);
            }
        };
        document.addEventListener('mousedown', onDocClick);
        return () => document.removeEventListener('mousedown', onDocClick);
    }, []);

    const loadNotifications = async () => {
        if (!resolvedUserId) return;
        setNotifLoading(true);
        try {
            const res = await notificationApi.get(`/notifications/users/${resolvedUserId}/recent`, { params: { limit: 5 } });
            const baseRows = Array.isArray(res.data) ? res.data : [];
            const borrowRes = await borrowingApi.get(`/borrows/user/${resolvedUserId}`);
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
            const enriched = enrichRows(baseRows, recordToBook, booksById);
            setNotifRows(enriched);
            return enriched;
        } catch {
            setNotifRows([]);
            return [];
        } finally {
            setNotifLoading(false);
        }
    };

    useEffect(() => {
        if (!resolvedUserId) return;
        const key = `notif_last_seen_marker_${resolvedUserId}`;
        const checkUnread = async () => {
            try {
                const res = await notificationApi.get(`/notifications/users/${resolvedUserId}/recent`, { params: { limit: 1 } });
                const latest = Array.isArray(res.data) ? res.data[0] : null;
                if (!latest) {
                    setHasUnread(false);
                    return;
                }
                const latestMarker = getNotificationMarker(latest);
                const lastSeenMarker = localStorage.getItem(key);

                if (!lastSeenMarker) {
                    setHasUnread(true);
                    return;
                }

                if (latestMarker && latestMarker !== lastSeenMarker) {
                    const latestTs = toEpoch(latest.createdAt);
                    const lastSeenCreatedAt = lastSeenMarker.split('|')[1] || '';
                    const seenTs = toEpoch(lastSeenCreatedAt);
                    if (Number.isNaN(latestTs) || Number.isNaN(seenTs)) {
                        setHasUnread(true);
                    } else {
                        setHasUnread(latestTs > seenTs);
                    }
                    return;
                }

                setHasUnread(false);
            } catch {
                // keep prior badge state on transient errors
            }
        };
        checkUnread();
        const id = setInterval(checkUnread, 15000);
        return () => clearInterval(id);
    }, [resolvedUserId]);

    const toggleNotifications = async () => {
        const next = !notifOpen;
        setNotifOpen(next);
        if (next) {
            const loadedRows = await loadNotifications();
            if (resolvedUserId) {
                const latest = loadedRows?.[0] || null;
                const marker = getNotificationMarker(latest);
                if (marker) {
                    localStorage.setItem(`notif_last_seen_marker_${resolvedUserId}`, marker);
                }
            }
            setHasUnread(false);
        }
    };

    if (showGuestNav) {
        return (
            <header style={styles.header}>
                <h1 style={styles.brand}>
                    <Link to="/login" style={styles.brandLink}>
                        E-Library
                    </Link>
                </h1>
                <div style={styles.flex} />
                <nav style={styles.nav}>
                    <NavLink to="/login" style={navLinkStyle} end>
                        Log In
                    </NavLink>
                    <NavLink to="/register" style={navLinkStyle} end>
                        Register
                    </NavLink>
                </nav>
            </header>
        );
    }

    return (
        <header style={styles.header}>
            <h1 style={styles.brand}>
                <Link to="/dashboard" style={styles.brandLink}>
                    E-Library
                </Link>
            </h1>
            <nav style={styles.navMain}>
                <NavLink to="/dashboard" style={navLinkStyle} end>
                    Dashboard
                </NavLink>
                <NavLink to="/books" style={navLinkStyle} end>
                    Browse books
                </NavLink>
                {user?.role === 'ADMIN' && (
                    <>
                        <NavLink to="/admin/fines" style={navLinkStyle} end>
                            Fines
                        </NavLink>
                        <NavLink to="/admin/testing" style={navLinkStyle} end>
                            Testing
                        </NavLink>
                    </>
                )}
                {user?.role !== 'ADMIN' && (
                    <NavLink to="/borrows" style={navLinkStyle} end>
                        Borrows
                    </NavLink>
                )}
            </nav>
            <div style={styles.userInfo}>
                {!showGuestNav && (
                    <div style={styles.bellWrap} ref={menuRef}>
                        <button type="button" onClick={toggleNotifications} style={styles.bellBtn} title="Notifications">
                            <BellIcon />
                            {hasUnread && <span style={styles.notifDot} />}
                        </button>
                        {notifOpen && (
                            <div style={styles.notifMenu}>
                                <div style={styles.notifHead}>Recent notifications</div>
                                {notifLoading ? (
                                    <div style={styles.notifMuted}>Loading…</div>
                                ) : notifRows.length === 0 ? (
                                    <div style={styles.notifMuted}>No notifications</div>
                                ) : (
                                    notifRows.map((n) => (
                                        <div key={n.notificationId} style={styles.notifItem}>
                                            <div style={styles.notifType}>{typeLabel(n.type)}</div>
                                            <div style={styles.notifMsg}>{n.displayMessage || n.message || '—'}</div>
                                        </div>
                                    ))
                                )}
                                <Link to="/notifications" onClick={() => setNotifOpen(false)} style={styles.notifMore}>
                                    View all notifications →
                                </Link>
                            </div>
                        )}
                    </div>
                )}
                {user && (
                    <>
                        <Link to="/account" style={styles.nameLink}>
                            <span style={styles.name}>{user.name}</span>
                        </Link>
                        <span style={styles.role}>{user.role}</span>
                    </>
                )}
                <button type="button" onClick={logout} style={styles.logoutBtn}>
                    Log Out
                </button>
            </div>
        </header>
    );
}

const styles = {
    header: {
        background: '#fff',
        padding: '16px 32px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        flexWrap: 'wrap',
        gap: '12px',
    },
    brand: { fontSize: '20px', margin: 0, fontWeight: '600' },
    brandLink: { color: 'inherit', textDecoration: 'none' },
    flex: { flex: 1 },
    nav: { display: 'flex', alignItems: 'center', gap: '20px' },
    navMain: { display: 'flex', alignItems: 'center', gap: '16px', flex: 1, marginLeft: '24px' },
    navLink: { fontSize: '14px', color: '#666', cursor: 'pointer', textDecoration: 'none' },
    navActive: { color: '#2563eb', fontWeight: '600', cursor: 'default' },
    userInfo: { display: 'flex', alignItems: 'center', gap: '16px' },
    nameLink: { textDecoration: 'none' },
    bellWrap: { position: 'relative' },
    bellBtn: {
        border: '1px solid #ddd',
        background: '#fff',
        borderRadius: '999px',
        width: '34px',
        height: '34px',
        cursor: 'pointer',
        display: 'grid',
        placeItems: 'center',
        color: '#666',
        lineHeight: 0,
        padding: 0,
        position: 'relative',
    },
    notifDot: {
        position: 'absolute',
        top: '4px',
        right: '4px',
        width: '8px',
        height: '8px',
        borderRadius: '999px',
        background: '#dc2626',
        border: '1px solid #fff',
    },
    notifMenu: {
        position: 'absolute',
        right: 0,
        top: '40px',
        width: '320px',
        maxWidth: 'calc(100vw - 32px)',
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: '10px',
        boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
        zIndex: 20,
        overflow: 'hidden',
    },
    notifHead: { padding: '10px 12px', fontSize: '12px', fontWeight: '700', color: '#374151', borderBottom: '1px solid #f1f5f9' },
    notifMuted: { padding: '12px', fontSize: '12px', color: '#666' },
    notifItem: { padding: '10px 12px', borderBottom: '1px solid #f8fafc' },
    notifType: { fontSize: '11px', color: '#1d4ed8', fontWeight: '700', marginBottom: '3px' },
    notifMsg: { fontSize: '12px', color: '#111', lineHeight: 1.35 },
    notifMore: { display: 'block', padding: '10px 12px', fontSize: '12px', fontWeight: '600', color: '#2563eb', textDecoration: 'none' },
    name: { fontSize: '14px', color: '#333' },
    role: {
        fontSize: '11px',
        background: '#dbeafe',
        color: '#2563eb',
        padding: '3px 8px',
        borderRadius: '12px',
        fontWeight: '600',
    },
    logoutBtn: {
        padding: '8px 16px',
        background: 'none',
        border: '1px solid #ddd',
        borderRadius: '6px',
        cursor: 'pointer',
        fontSize: '13px',
        color: '#666',
    },
};

export default AppNav;
