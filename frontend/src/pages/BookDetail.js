import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import bookApi from '../services/bookApi';
import AppNav from '../components/AppNav';

function parseUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || '{}');
    } catch {
        return {};
    }
}

function BookDetail({ onLogout }) {
    const { bookId } = useParams();
    const navigate = useNavigate();
    const user = parseUser();
    const isAdmin = user.role === 'ADMIN';

    const [book, setBook] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [editing, setEditing] = useState(false);
    const [editForm, setEditForm] = useState({});

    useEffect(() => {
        fetchBook();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [bookId]);

    const fetchBook = async () => {
        setLoading(true);
        setError('');
        try {
            const res = await bookApi.get(`/books/${bookId}`);
            setBook(res.data);
        } catch (err) {
            setError(err.response?.status === 404
                ? 'Book not found.'
                : 'Failed to load book: ' + (err.response?.data?.message || err.message));
        }
        setLoading(false);
    };

    const startEditing = () => {
        setEditForm({
            isbn: book.isbn,
            title: book.title,
            author: book.author,
            publisher: book.publisher,
            publishYear: book.publishYear,
            description: book.description || '',
            totalCopies: book.totalCopies,
            availableCopies: book.availableCopies
        });
        setEditing(true);
        setError('');
        setSuccess('');
    };

    const cancelEditing = () => {
        setEditing(false);
        setError('');
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        try {
            await bookApi.put(`/books/${bookId}`, {
                ...editForm,
                publishYear: parseInt(editForm.publishYear),
                totalCopies: parseInt(editForm.totalCopies),
                availableCopies: parseInt(editForm.availableCopies)
            });
            setSuccess('Book updated successfully!');
            setEditing(false);
            fetchBook();
        } catch (err) {
            setError('Failed to update book: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleRemove = async () => {
        setError('');
        setSuccess('');
        if (!window.confirm('Remove this book from the catalogue?')) return;
        try {
            await bookApi.delete(`/books/${bookId}`);
            setSuccess('Book removed from catalogue');
            fetchBook();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to remove book');
        }
    };

    const statusStyle = (status) => {
        const map = {
            AVAILABLE: { background: '#dbeafe', color: '#2563eb' },
            BORROWED:  { background: '#fee2e2', color: '#dc2626' },
            REMOVED:   { background: '#e5e7eb', color: '#6b7280' },
        };
        return { ...styles.badge, ...(map[status] || map.REMOVED) };
    };

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />

            <div style={styles.body}>
                <button onClick={() => navigate('/books')} style={styles.backBtn}>
                    &larr; Back to Catalogue
                </button>

                {loading && <p style={styles.muted}>Loading...</p>}
                {error && <p style={styles.error}>{error}</p>}
                {success && <p style={styles.success}>{success}</p>}

                {book && !editing && (
                    <div style={styles.card}>
                        <div style={styles.headerRow}>
                            <h2 style={styles.title}>{book.title}</h2>
                            <span style={statusStyle(book.status)}>{book.status}</span>
                        </div>

                        <div style={styles.detailGrid}>
                            <div style={styles.detailItem}>
                                <span style={styles.label}>Author</span>
                                <span style={styles.value}>{book.author}</span>
                            </div>
                            <div style={styles.detailItem}>
                                <span style={styles.label}>ISBN</span>
                                <span style={styles.value}>{book.isbn}</span>
                            </div>
                            <div style={styles.detailItem}>
                                <span style={styles.label}>Publisher</span>
                                <span style={styles.value}>{book.publisher}</span>
                            </div>
                            <div style={styles.detailItem}>
                                <span style={styles.label}>Publish Year</span>
                                <span style={styles.value}>{book.publishYear}</span>
                            </div>
                            <div style={styles.detailItem}>
                                <span style={styles.label}>Available Copies</span>
                                <span style={styles.value}>{book.availableCopies} / {book.totalCopies}</span>
                            </div>
                        </div>

                        {book.description && (
                            <div style={styles.descriptionSection}>
                                <span style={styles.label}>Description</span>
                                <p style={styles.description}>{book.description}</p>
                            </div>
                        )}

                        {book.categoryIds && book.categoryIds.length > 0 && (
                            <div style={styles.descriptionSection}>
                                <span style={styles.label}>Categories</span>
                                <p style={styles.value}>IDs: {book.categoryIds.join(', ')}</p>
                            </div>
                        )}

                        {/* Admin-only actions */}
                        {isAdmin && book.status !== 'REMOVED' && (
                            <div style={styles.adminActions}>
                                <button onClick={startEditing} style={styles.btnPrimary}>Edit Book</button>
                                <button onClick={handleRemove} style={styles.btnDanger}>Remove from Catalogue</button>
                            </div>
                        )}

                        {book.status === 'REMOVED' && (
                            <p style={styles.removedNote}>This book has been removed from the catalogue.</p>
                        )}
                    </div>
                )}

                {/* Edit Form — Admin only */}
                {book && editing && isAdmin && (
                    <form onSubmit={handleSave} style={styles.card}>
                        <h2 style={styles.title}>Edit Book</h2>

                        <div style={styles.formGrid}>
                            <div style={styles.formField}>
                                <label style={styles.label}>ISBN *</label>
                                <input value={editForm.isbn} onChange={(e) => setEditForm({ ...editForm, isbn: e.target.value })} required style={styles.input} />
                            </div>
                            <div style={styles.formField}>
                                <label style={styles.label}>Title *</label>
                                <input value={editForm.title} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} required style={styles.input} />
                            </div>
                            <div style={styles.formField}>
                                <label style={styles.label}>Author *</label>
                                <input value={editForm.author} onChange={(e) => setEditForm({ ...editForm, author: e.target.value })} required style={styles.input} />
                            </div>
                            <div style={styles.formField}>
                                <label style={styles.label}>Publisher *</label>
                                <input value={editForm.publisher} onChange={(e) => setEditForm({ ...editForm, publisher: e.target.value })} required style={styles.input} />
                            </div>
                            <div style={styles.formField}>
                                <label style={styles.label}>Publish Year *</label>
                                <input type="number" value={editForm.publishYear} onChange={(e) => setEditForm({ ...editForm, publishYear: e.target.value })} required style={styles.input} />
                            </div>
                            <div style={styles.formField}>
                                <label style={styles.label}>Total Copies *</label>
                                <input type="number" min="1" value={editForm.totalCopies} onChange={(e) => setEditForm({ ...editForm, totalCopies: e.target.value })} required style={styles.input} />
                            </div>
                            <div style={styles.formField}>
                                <label style={styles.label}>Available Copies *</label>
                                <input type="number" min="0" value={editForm.availableCopies} onChange={(e) => setEditForm({ ...editForm, availableCopies: e.target.value })} required style={styles.input} />
                            </div>
                        </div>

                        <div style={styles.formField}>
                            <label style={styles.label}>Description</label>
                            <textarea value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} style={styles.textarea} />
                        </div>

                        <div style={styles.editActions}>
                            <button type="submit" style={styles.btnSuccess}>Save Changes</button>
                            <button type="button" onClick={cancelEditing} style={styles.btnSecondary}>Cancel</button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    body: { padding: '32px', maxWidth: '900px', margin: '0 auto', flex: 1, width: '100%', boxSizing: 'border-box' },

    backBtn: { padding: '8px 16px', background: 'none', border: '1px solid #ddd', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', color: '#666', marginBottom: '20px' },

    error: { color: '#dc2626', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#fee2e2', borderRadius: '6px' },
    success: { color: '#059669', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#d1fae5', borderRadius: '6px' },
    muted: { color: '#999', textAlign: 'center', padding: '40px' },

    card: { background: '#fff', padding: '32px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' },

    headerRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
    title: { fontSize: '24px', margin: '0 0 24px 0' },
    badge: { padding: '4px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: '600' },

    detailGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' },
    detailItem: { display: 'flex', flexDirection: 'column', gap: '4px' },
    label: { fontSize: '12px', color: '#999', textTransform: 'uppercase', fontWeight: '600' },
    value: { fontSize: '15px', color: '#333' },

    descriptionSection: { marginBottom: '20px' },
    description: { fontSize: '14px', color: '#555', lineHeight: '1.6', margin: '6px 0 0' },

    adminActions: { display: 'flex', gap: '10px', paddingTop: '20px', borderTop: '1px solid #f0f0f0', marginTop: '20px' },
    removedNote: { fontSize: '13px', color: '#6b7280', fontStyle: 'italic', marginTop: '20px', padding: '10px', background: '#f3f4f6', borderRadius: '6px' },

    formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' },
    formField: { display: 'flex', flexDirection: 'column', gap: '6px' },
    input: { padding: '12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },
    textarea: { width: '100%', padding: '12px', marginTop: '6px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box', minHeight: '80px' },

    editActions: { display: 'flex', gap: '10px', marginTop: '20px' },

    btnPrimary: { padding: '10px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSuccess: { padding: '10px 20px', background: '#059669', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnDanger: { padding: '10px 20px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSecondary: { padding: '10px 20px', background: '#fff', color: '#666', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
};

export default BookDetail;