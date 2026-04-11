import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

function Dashboard({ onLogout }) {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const res = await api.get('/auth/me');
                setUser(res.data);
            } catch (err) {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                if (onLogout) onLogout();
                navigate('/login');
            } finally {
                setLoading(false);
            }
        };
        fetchUser();
    }, [navigate, onLogout]);

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (onLogout) onLogout();
        navigate('/login');
    };

    if (loading) return <div style={styles.body}><p>Loading...</p></div>;

    return (
        <div>
            <div style={styles.header}>
                <h1 style={styles.title}>E-Library</h1>
                <div style={styles.userInfo}>
                    <span style={styles.name}>{user.name}</span>
                    <span style={styles.role}>{user.role}</span>
                    <button onClick={logout} style={styles.logoutBtn}>Log Out</button>
                </div>
            </div>
            <div style={styles.body}>
                <p>Dashboard content goes here.</p>
            </div>
        </div>
    );
}

const styles = {
    header: { background: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' },
    title: { fontSize: '20px' },
    userInfo: { display: 'flex', alignItems: 'center', gap: '16px' },
    name: { fontSize: '14px', color: '#333' },
    role: { fontSize: '11px', background: '#dbeafe', color: '#2563eb', padding: '3px 8px', borderRadius: '12px', fontWeight: '600' },
    logoutBtn: { padding: '8px 16px', background: 'none', border: '1px solid #ddd', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', color: '#666' },
    body: { padding: '40px 32px', textAlign: 'center', color: '#999' }
};

export default Dashboard;