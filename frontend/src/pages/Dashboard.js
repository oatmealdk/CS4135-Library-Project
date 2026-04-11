import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import AppNav from '../components/AppNav';

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

    if (loading) {
        return (
            <div style={styles.page}>
                <AppNav onLogout={onLogout} />
                <div style={styles.loadingWrap}><p>Loading...</p></div>
            </div>
        );
    }

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <div style={styles.main}>
                <h2 style={styles.heading}>Welcome back{user?.name ? `, ${user.name}` : ''}</h2>
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    loadingWrap: { padding: '40px 32px', textAlign: 'center', color: '#999', flex: 1 },
    main: { padding: '40px 32px', maxWidth: '560px', margin: '0 auto', flex: 1 },
    heading: { fontSize: '24px', marginBottom: '12px', color: '#111' },
};

export default Dashboard;
