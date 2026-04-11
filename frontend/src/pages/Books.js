import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import bookApi from '../services/bookApi';

function Books() {
    const navigate = useNavigate();
    const [books, setBooks] = useState([]);
    const [categories, setCategories] = useState([]);
    const [searchTitle, setSearchTitle] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [showAddForm, setShowAddForm] = useState(false);
    const [showCategoryForm, setShowCategoryForm] = useState(false);

    const [newBook, setNewBook] = useState({
        isbn: '', title: '', author: '', publisher: '',
        publishYear: '', description: '', totalCopies: 1
    });
    const [newCategory, setNewCategory] = useState({ name: '', description: '' });

    useEffect(() => {
        fetchBooks();
        fetchCategories();
    }, []);

    const fetchBooks = async (title = '') => {
        setLoading(true);
        setError('');
        try {
            const params = title ? { title } : {};
            const res = await bookApi.get('/books', { params });
            setBooks(res.data.content || []);
        } catch (err) {
            setError('Failed to load books: ' + (err.response?.data?.message || err.message));
        }
        setLoading(false);
    };

    const fetchCategories = async () => {
        try {
            const res = await bookApi.get('/categories');
            setCategories(res.data || []);
        } catch (err) {
            console.error('Failed to load categories', err);
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        fetchBooks(searchTitle);
    };

    const handleAddBook = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        try {
            await bookApi.post('/books', {
                ...newBook,
                publishYear: parseInt(newBook.publishYear),
                totalCopies: parseInt(newBook.totalCopies)
            });
            setSuccess('Book added successfully!');
            setShowAddForm(false);
            setNewBook({ isbn: '', title: '', author: '', publisher: '', publishYear: '', description: '', totalCopies: 1 });
            fetchBooks();
        } catch (err) {
            setError('Failed to add book: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleAddCategory = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        try {
            await bookApi.post('/categories', newCategory);
            setSuccess('Category added!');
            setShowCategoryForm(false);
            setNewCategory({ name: '', description: '' });
            fetchCategories();
        } catch (err) {
            setError('Failed to add category: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleDecrement = async (bookId) => {
        setError(''); setSuccess('');
        try {
            await bookApi.put(`/books/${bookId}/decrement-copies`);
            setSuccess('Copy borrowed');
            fetchBooks(searchTitle);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to decrement copies');
        }
    };

    const handleIncrement = async (bookId) => {
        setError(''); setSuccess('');
        try {
            await bookApi.put(`/books/${bookId}/increment-copies`);
            setSuccess('Copy returned');
            fetchBooks(searchTitle);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to increment copies');
        }
    };

    const handleRemove = async (bookId) => {
        setError(''); setSuccess('');
        if (!window.confirm('Remove this book from the catalogue?')) return;
        try {
            await bookApi.delete(`/books/${bookId}`);
            setSuccess('Book removed from catalogue');
            fetchBooks(searchTitle);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to remove book');
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login');
    };

    return (
        <div>
            {/* Header - matches Dashboard.js style */}
            <div style={styles.header}>
                <h1 style={styles.headerTitle}>E-Library</h1>
                <div style={styles.nav}>
                    <span style={styles.navLink} onClick={() => navigate('/dashboard')}>Dashboard</span>
                    <span style={{ ...styles.navLink, ...styles.navActive }}>Books</span>
                    <button onClick={logout} style={styles.logoutBtn}>Log Out</button>
                </div>
            </div>

            {/* Content */}
            <div style={styles.body}>
                <h2 style={styles.pageTitle}>Book Catalogue</h2>

                {error && <p style={styles.error}>{error}</p>}
                {success && <p style={styles.success}>{success}</p>}

                {/* Search */}
                <form onSubmit={handleSearch} style={styles.searchRow}>
                    <input
                        type="text"
                        placeholder="Search by title..."
                        value={searchTitle}
                        onChange={(e) => setSearchTitle(e.target.value)}
                        style={styles.searchInput}
                    />
                    <button type="submit" style={styles.btnPrimary}>Search</button>
                    <button type="button" onClick={() => { setSearchTitle(''); fetchBooks(); }} style={styles.btnSecondary}>Clear</button>
                </form>

                {/* Action buttons */}
                <div style={styles.actionRow}>
                    <button onClick={() => setShowAddForm(!showAddForm)} style={styles.btnSuccess}>
                        {showAddForm ? 'Cancel' : '+ Add Book'}
                    </button>
                    <button onClick={() => setShowCategoryForm(!showCategoryForm)} style={styles.btnPrimary}>
                        {showCategoryForm ? 'Cancel' : '+ Add Category'}
                    </button>
                </div>

                {/* Add Book Form */}
                {showAddForm && (
                    <form onSubmit={handleAddBook} style={styles.card}>
                        <h3 style={styles.cardTitle}>Add New Book</h3>
                        <div style={styles.formGrid}>
                            <input placeholder="ISBN *" value={newBook.isbn} onChange={(e) => setNewBook({ ...newBook, isbn: e.target.value })} required style={styles.input} />
                            <input placeholder="Title *" value={newBook.title} onChange={(e) => setNewBook({ ...newBook, title: e.target.value })} required style={styles.input} />
                            <input placeholder="Author *" value={newBook.author} onChange={(e) => setNewBook({ ...newBook, author: e.target.value })} required style={styles.input} />
                            <input placeholder="Publisher *" value={newBook.publisher} onChange={(e) => setNewBook({ ...newBook, publisher: e.target.value })} required style={styles.input} />
                            <input placeholder="Publish Year *" type="number" value={newBook.publishYear} onChange={(e) => setNewBook({ ...newBook, publishYear: e.target.value })} required style={styles.input} />
                            <input placeholder="Total Copies *" type="number" min="1" value={newBook.totalCopies} onChange={(e) => setNewBook({ ...newBook, totalCopies: e.target.value })} required style={styles.input} />
                        </div>
                        <textarea placeholder="Description" value={newBook.description} onChange={(e) => setNewBook({ ...newBook, description: e.target.value })} style={styles.textarea} />
                        <button type="submit" style={{ ...styles.btnSuccess, marginTop: '12px' }}>Add Book</button>
                    </form>
                )}

                {/* Add Category Form */}
                {showCategoryForm && (
                    <form onSubmit={handleAddCategory} style={styles.card}>
                        <h3 style={styles.cardTitle}>Add New Category</h3>
                        <div style={styles.formRow}>
                            <input placeholder="Category Name *" value={newCategory.name} onChange={(e) => setNewCategory({ ...newCategory, name: e.target.value })} required style={styles.input} />
                            <input placeholder="Description" value={newCategory.description} onChange={(e) => setNewCategory({ ...newCategory, description: e.target.value })} style={styles.input} />
                            <button type="submit" style={styles.btnPrimary}>Add</button>
                        </div>
                    </form>
                )}

                {/* Categories */}
                {categories.length > 0 && (
                    <p style={styles.categoryLine}><strong>Categories:</strong> {categories.map(c => c.name).join(', ')}</p>
                )}

                {/* Books List */}
                {loading ? (
                    <p style={styles.muted}>Loading...</p>
                ) : books.length === 0 ? (
                    <p style={styles.muted}>No books found.</p>
                ) : (
                    <div>
                        {books.map(book => (
                            <div key={book.bookId} style={styles.bookCard}>
                                <div style={styles.bookInfo}>
                                    <div>
                                        <strong style={styles.bookTitle}>{book.title}</strong>
                                        <p style={styles.bookMeta}>{book.author} · {book.publisher} · {book.publishYear}</p>
                                        <p style={styles.bookMeta}>ISBN: {book.isbn}</p>
                                    </div>
                                    <div style={styles.bookRight}>
                                        <span style={{
                                            ...styles.badge,
                                            background: book.status === 'AVAILABLE' ? '#dbeafe' : book.status === 'BORROWED' ? '#fee2e2' : '#e5e7eb',
                                            color: book.status === 'AVAILABLE' ? '#2563eb' : book.status === 'BORROWED' ? '#dc2626' : '#6b7280'
                                        }}>
                                            {book.status}
                                        </span>
                                        <span style={styles.copies}>{book.availableCopies} / {book.totalCopies} available</span>
                                    </div>
                                </div>
                                <div style={styles.bookActions}>
                                    <button onClick={() => handleDecrement(book.bookId)} disabled={book.availableCopies <= 0 || book.status === 'REMOVED'} style={styles.btnSmallWarn}>Borrow</button>
                                    <button onClick={() => handleIncrement(book.bookId)} disabled={book.availableCopies >= book.totalCopies || book.status === 'REMOVED'} style={styles.btnSmallSuccess}>Return</button>
                                    <button onClick={() => handleRemove(book.bookId)} disabled={book.status === 'REMOVED'} style={styles.btnSmallDanger}>Remove</button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

const styles = {
    /* Header - matches Dashboard.js */
    header: { background: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' },
    headerTitle: { fontSize: '20px' },
    nav: { display: 'flex', alignItems: 'center', gap: '16px' },
    navLink: { fontSize: '14px', color: '#666', cursor: 'pointer' },
    navActive: { color: '#2563eb', fontWeight: '600' },
    logoutBtn: { padding: '8px 16px', background: 'none', border: '1px solid #ddd', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', color: '#666' },

    /* Body */
    body: { padding: '32px', maxWidth: '900px', margin: '0 auto' },
    pageTitle: { fontSize: '24px', marginBottom: '24px' },

    /* Alerts */
    error: { color: '#dc2626', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#fee2e2', borderRadius: '6px' },
    success: { color: '#059669', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#d1fae5', borderRadius: '6px' },

    /* Search */
    searchRow: { display: 'flex', gap: '10px', marginBottom: '20px' },
    searchInput: { flex: 1, padding: '12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },

    /* Buttons */
    actionRow: { display: 'flex', gap: '10px', marginBottom: '20px' },
    btnPrimary: { padding: '10px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSecondary: { padding: '10px 20px', background: '#fff', color: '#666', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSuccess: { padding: '10px 20px', background: '#059669', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSmallWarn: { padding: '6px 14px', background: '#f59e0b', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' },
    btnSmallSuccess: { padding: '6px 14px', background: '#059669', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' },
    btnSmallDanger: { padding: '6px 14px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' },

    /* Cards & Forms - matches Login.js style */
    card: { background: '#fff', padding: '24px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', marginBottom: '20px' },
    cardTitle: { fontSize: '18px', marginBottom: '16px' },
    formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' },
    formRow: { display: 'flex', gap: '10px', alignItems: 'center' },
    input: { padding: '12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },
    textarea: { width: '100%', padding: '12px', marginTop: '12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box', minHeight: '60px' },

    /* Misc */
    categoryLine: { fontSize: '14px', color: '#666', marginBottom: '20px' },
    muted: { color: '#999', textAlign: 'center', padding: '40px' },

    /* Book cards */
    bookCard: { background: '#fff', padding: '16px 20px', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: '12px' },
    bookInfo: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' },
    bookTitle: { fontSize: '16px' },
    bookMeta: { fontSize: '13px', color: '#666', margin: '4px 0' },
    bookRight: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' },
    badge: { padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: '600' },
    copies: { fontSize: '13px', color: '#666' },
    bookActions: { display: 'flex', gap: '8px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid #f0f0f0' }
};

export default Books;