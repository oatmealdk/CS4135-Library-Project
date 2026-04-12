import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Books from './pages/Books';
import BookDetail from './pages/BookDetail';

function App() {
    const [loggedIn, setLoggedIn] = useState(!!localStorage.getItem('token'));

    useEffect(() => {
        const handleStorage = () => setLoggedIn(!!localStorage.getItem('token'));
        window.addEventListener('storage', handleStorage);
        return () => window.removeEventListener('storage', handleStorage);
    }, []);

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login onLogin={() => setLoggedIn(true)} onLogout={() => setLoggedIn(false)} />} />
                <Route path="/register" element={<Register onLogin={() => setLoggedIn(true)} onLogout={() => setLoggedIn(false)} />} />
                <Route path="/dashboard" element={loggedIn ? <Dashboard onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/books" element={loggedIn ? <Books onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="/books/:bookId" element={loggedIn ? <BookDetail onLogout={() => setLoggedIn(false)} /> : <Navigate to="/login" />} />
                <Route path="*" element={<Navigate to="/login" />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;