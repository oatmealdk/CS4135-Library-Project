import React from 'react';
import { Link, NavLink, useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';
import { clearSession, parseUser, getToken } from '../services/auth';

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
                    Books
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
