import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import bookApi from '../services/bookApi';
import AppNav from '../components/AppNav';

function BookDetail({ onLogout }) {
    const { bookId } = useParams();
    const navigate = useNavigate();
    const [book, setBook] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

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

                {book && (
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

                        <div style={styles.timestamps}>
                            <span>Created: {new Date(book.createdAt).toLocaleString()}</span>
                        </div>
                    </div>
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
    muted: { color: '#999', textAlign: 'center', padding: '40px' },

    card: { background: '#fff', padding: '32px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' },

    headerRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
    title: { fontSize: '24px', margin: 0 },
    badge: { padding: '4px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: '600' },

    detailGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' },
    detailItem: { display: 'flex', flexDirection: 'column', gap: '4px' },
    label: { fontSize: '12px', color: '#999', textTransform: 'uppercase', fontWeight: '600' },
    value: { fontSize: '15px', color: '#333' },

    descriptionSection: { marginBottom: '20px' },
    description: { fontSize: '14px', color: '#555', lineHeight: '1.6', margin: '6px 0 0' },

    timestamps: { display: 'flex', gap: '24px', paddingTop: '16px', borderTop: '1px solid #f0f0f0', fontSize: '12px', color: '#999' },
};

export default BookDetail;