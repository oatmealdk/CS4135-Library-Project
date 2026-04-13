import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppNav from '../components/AppNav';
import api from '../services/api';
import { clearSession, parseUser, setSession } from '../services/auth';

function UserSettings({ onLogout }) {
    const navigate = useNavigate();
    const user = parseUser();

    const [name, setName] = useState(user?.name || '');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const [deleting, setDeleting] = useState(false);

    const handleSave = async (event) => {
        event.preventDefault();
        setMessage('');
        setError('');

        if (!name.trim() && !password.trim()) {
            setError('Please enter a new full name or password to update.');
            return;
        }

        try {
            setSaving(true);
            const payload = {};
            if (name.trim()) payload.name = name.trim();
            if (password.trim()) payload.password = password;

            const response = await api.put('/users/me', payload);
            setSession(response.data);
            setPassword('');
            setMessage('Your details were updated successfully.');
        } catch (err) {
            if (err.response?.status === 404) {
                setError('Update endpoint not found. Restart user-service and try again.');
            } else {
                setError(err.response?.data?.message || 'Could not update user details.');
            }
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteAccount = async () => {
        const confirmed = window.confirm('Delete your account permanently? This cannot be undone.');
        if (!confirmed) return;

        try {
            setDeleting(true);
            await api.delete('/users/me');
            clearSession();
            if (onLogout) onLogout();
            navigate('/login');
        } catch (err) {
            if (err.response?.status === 404) {
                setError('Delete endpoint not found. Restart user-service and try again.');
            } else {
                setError(err.response?.data?.message || 'Could not delete account.');
            }
        } finally {
            setDeleting(false);
        }
    };

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <div style={styles.container}>
                <form onSubmit={handleSave} style={styles.card}>
                    <h2 style={styles.title}>Change User Details</h2>
                    {message && <p style={styles.success}>{message}</p>}
                    {error && <p style={styles.error}>{error}</p>}

                    <label style={styles.label}>Full Name</label>
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        style={styles.input}
                        placeholder="Enter full name"
                    />

                    <label style={styles.label}>New Password</label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        style={styles.input}
                        placeholder="Leave blank to keep current password"
                        minLength={8}
                    />

                    <button type="submit" style={styles.primaryButton} disabled={saving}>
                        {saving ? 'Saving...' : 'Save Changes'}
                    </button>

                    <button
                        type="button"
                        onClick={handleDeleteAccount}
                        style={styles.deleteButton}
                        disabled={deleting}
                    >
                        {deleting ? 'Deleting...' : 'Delete Account'}
                    </button>
                </form>
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    container: { flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '24px 16px' },
    card: { background: '#fff', padding: '32px', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', width: '100%', maxWidth: '440px' },
    title: { margin: '0 0 20px 0', fontSize: '24px' },
    label: { display: 'block', fontSize: '13px', marginBottom: '6px', color: '#444' },
    input: { width: '100%', padding: '12px', marginBottom: '14px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },
    success: { color: '#047857', fontSize: '13px', marginBottom: '10px' },
    error: { color: '#dc2626', fontSize: '13px', marginBottom: '10px' },
    primaryButton: { width: '100%', padding: '12px', marginTop: '8px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '15px', cursor: 'pointer' },
    deleteButton: { width: '100%', padding: '12px', marginTop: '10px', background: '#fff', color: '#dc2626', border: '1px solid #fca5a5', borderRadius: '6px', fontSize: '15px', cursor: 'pointer' },
};

export default UserSettings;
