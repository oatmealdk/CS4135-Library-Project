import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Books from './pages/Books';
import BookDetail from './pages/BookDetail';
import Borrows from './pages/Borrows';
import AdminFines from './pages/AdminFines';
import AdminTesting from './pages/AdminTesting';
import UserSettings from './pages/UserSettings';
import Notifications from './pages/Notifications';
import api from './services/api';
import { getToken, clearSession } from './services/auth';

function App() {
    const [loggedIn, setLoggedIn] = useState(!!getToken());
    const [authChecked, setAuthChecked] = useState(false);

    useEffect(() => {
        const handleStorage = () => setLoggedIn(!!getToken());
        const handleAuthLogout = () => setLoggedIn(false);
        window.addEventListener('storage', handleStorage);
        window.addEventListener('auth:logout', handleAuthLogout);
        return () => {
            window.removeEventListener('storage', handleStorage);
            window.removeEventListener('auth:logout', handleAuthLogout);
        };
    }, []);

    useEffect(() => {
        const validateSession = async () => {
            if (!getToken()) {
                setLoggedIn(false);
                setAuthChecked(true);
                return;
            }
            try {
                await api.get('/auth/validate');
                setLoggedIn(true);
            } catch {
                clearSession();
                setLoggedIn(false);
            } finally {
                setAuthChecked(true);
            }
        };
        validateSession();
    }, []);

    useEffect(() => {
        if (!loggedIn) {
            return undefined;
        }
        const intervalId = window.setInterval(async () => {
            try {
                await api.get('/auth/validate');
            } catch {
                clearSession();
                setLoggedIn(false);
            }
        }, 30000);
        return () => window.clearInterval(intervalId);
    }, [loggedIn]);

    if (!authChecked) {
        return <div style={{ padding: '40px', textAlign: 'center' }}>Loading...</div>;
    }

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={loggedIn ? <Navigate to="/dashboard" replace /> : <Login onLogin={() => setLoggedIn(true)} onLogout={() => setLoggedIn(false)} />} />
                <Route path="/register" element={loggedIn ? <Navigate to="/dashboard" replace /> : <Register onLogin={() => setLoggedIn(true)} onLogout={() => setLoggedIn(false)} />} />
                <Route path="/dashboard" element={loggedIn ? <Dashboard onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/books" element={loggedIn ? <Books onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/books/:bookId" element={loggedIn ? <BookDetail onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/borrows" element={loggedIn ? <Borrows onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/account" element={loggedIn ? <UserSettings onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/notifications" element={loggedIn ? <Notifications onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/admin/fines" element={loggedIn ? <AdminFines onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/admin/testing" element={loggedIn ? <AdminTesting onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="*" element={<Navigate to="/login" />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;