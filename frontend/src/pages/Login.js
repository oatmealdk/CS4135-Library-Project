import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import AppNav from '../components/AppNav';

function Login({ onLogin, onLogout }) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const res = await api.post('/auth/login', { email, password });
            localStorage.setItem('token', res.data.token);
            localStorage.setItem('user', JSON.stringify(res.data));
            onLogin();
            navigate('/dashboard');
        } catch (err) {
            if (err.response?.data?.message) {
                setError(err.response.data.message);
            } else if (err.request && !err.response) {
                setError('Cannot reach user-service (port 8081). Check Docker: user-service must be running.');
            } else {
                setError('Invalid email or password');
            }
        }
    };

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <div style={styles.container}>
            <form onSubmit={handleSubmit} style={styles.form}>
                <h2 style={styles.title}>Log In</h2>
                {error && <p style={styles.error}>{error}</p>}
                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    style={styles.input}
                    required
                />
                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={styles.input}
                    required
                />
                <button type="submit" style={styles.button}>Log In</button>
                <p style={styles.switch}>
                    Don't have an account? <Link to="/register" style={styles.link}>Register</Link>
                </p>
            </form>
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    container: { flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '24px 16px' },
    form: { background: '#fff', padding: '40px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', width: '100%', maxWidth: '400px' },
    title: { marginBottom: '24px', fontSize: '24px' },
    input: { width: '100%', padding: '12px', marginBottom: '16px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },
    button: { width: '100%', padding: '12px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '16px', cursor: 'pointer' },
    error: { color: '#dc2626', fontSize: '13px', marginBottom: '12px' },
    switch: { marginTop: '16px', textAlign: 'center', fontSize: '14px', color: '#666' },
    link: { color: '#2563eb' }
};

export default Login;