import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import bookApi from '../services/bookApi';
import borrowingApi from '../services/borrowingApi';
import searchApi from '../services/searchApi';
import AppNav from '../components/AppNav';
import { userHasActiveLoanOnBook } from '../utils/borrowUtils';

function patronBorrowEligible(book) {
    return book.status !== 'REMOVED' && (book.availableCopies ?? 0) > 0;
}

function parseUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || '{}');
    } catch {
        return {};
    }
}

function Books({ onLogout }) {
    const navigate = useNavigate();
    const user = parseUser();
    const isAdmin = user.role === 'ADMIN';
    const canBorrowAsPatron = !isAdmin && user?.id;

    // catalogue data
    const [books, setBooks] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // search fields
    const [searchTitle, setSearchTitle] = useState('');
    const [searchAuthor, setSearchAuthor] = useState('');
    const [searchKeyword, setSearchKeyword] = useState('');
    const [searchCategoryId, setSearchCategoryId] = useState('');
    const [searchStatus, setSearchStatus] = useState('');
    const [searchYearFrom, setSearchYearFrom] = useState('');
    const [searchYearTo, setSearchYearTo] = useState('');
    const [titleSuggestions, setTitleSuggestions] = useState([]);
    const [page, setPage] = useState(0);
    const [pageSize] = useState(20);
    const [totalPages, setTotalPages] = useState(0);
    const [totalResults, setTotalResults] = useState(null);
    const [querySummary, setQuerySummary] = useState('');
    const [searchMode, setSearchMode] = useState('search'); // 'search' | 'direct'

    // form toggles
    const [showAddForm, setShowAddForm] = useState(false);
    const [showCategoryForm, setShowCategoryForm] = useState(false);

    const [newBook, setNewBook] = useState({
        isbn: '', title: '', author: '', publisher: '',
        publishYear: '', description: '', totalCopies: 1
    });
    const [newCategory, setNewCategory] = useState({ name: '', description: '' });
    const [borrowSubmittingId, setBorrowSubmittingId] = useState(null);
    const [myBorrows, setMyBorrows] = useState([]);

    const loadMyBorrows = async () => {
        if (!canBorrowAsPatron || !user?.id) {
            setMyBorrows([]);
            return;
        }
        try {
            const res = await borrowingApi.get(`/borrows/user/${user.id}`);
            setMyBorrows(res.data || []);
        } catch {
            setMyBorrows([]);
        }
    };

    useEffect(() => {
        runSearch(0);
        fetchCategories();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        loadMyBorrows();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canBorrowAsPatron, user?.id]);

    // Search via search-service (/api/search) - supports title, author, keyword, pagination
    const runSearch = async (targetPage = 0) => {
        setLoading(true);
        setError('');
        const params = { page: targetPage, size: pageSize };
        const tTitle = searchTitle.trim();
        const tAuthor = searchAuthor.trim();
        const tKeyword = searchKeyword.trim();
        if (tTitle) params.title = tTitle;
        if (tAuthor) params.author = tAuthor;
        if (tKeyword) params.keyword = tKeyword;
        if (searchCategoryId) params.categoryId = Number(searchCategoryId);
        if (searchStatus) params.status = searchStatus;
        if (searchYearFrom) params.publishYearFrom = Number(searchYearFrom);
        if (searchYearTo) params.publishYearTo = Number(searchYearTo);
        try {
            const res = await searchApi.get('/api/search', { params });
            const data = res.data;
            setBooks(data.results ?? []);
            setTotalPages(data.totalPages ?? 0);
            setTotalResults(data.totalResults ?? 0);
            setQuerySummary(data.querySummary ?? '');
            setPage(targetPage);
            setSearchMode('search');
        } catch (err) {
            // search-service unavailable - fall back to book-service direct listing
            await fetchBooksDirect(searchTitle.trim());
        }
        setLoading(false);
    };

    // Fallback: call book-service directly (title filter only)
    const fetchBooksDirect = async (title = '') => {
        setSearchMode('direct');
        setError('');
        try {
            const t = (title || '').trim();
            const params = t ? { title: t } : {};
            const res = await bookApi.get('/books', { params });
            setBooks(res.data.content || []);
            setTotalPages(0);
            setTotalResults(null);
        } catch (err) {
            setError('Failed to load books: ' + (err.response?.data?.message || err.message));
        }
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
        runSearch(0);
    };

    const handleClearSearch = () => {
        setSearchTitle('');
        setSearchAuthor('');
        setSearchKeyword('');
        setSearchCategoryId('');
        setSearchStatus('');
        setSearchYearFrom('');
        setSearchYearTo('');
        setTitleSuggestions([]);
        setQuerySummary('');
        setTotalResults(null);
        runSearch(0);
    };

    const loadTitleSuggestions = async (query) => {
        const q = (query || '').trim();
        if (!q || q.length < 2) {
            setTitleSuggestions([]);
            return;
        }
        try {
            const res = await searchApi.get('/api/search/suggestions', { params: { q } });
            setTitleSuggestions(Array.isArray(res.data) ? res.data : []);
        } catch {
            setTitleSuggestions([]);
        }
    };

    const handleAddBook = async (e) => {
        e.preventDefault();
        setError(''); setSuccess('');
        try {
            await bookApi.post('/books', {
                ...newBook,
                publishYear: parseInt(newBook.publishYear),
                totalCopies: parseInt(newBook.totalCopies)
            });
            setSuccess('Book added successfully!');
            setShowAddForm(false);
            setNewBook({ isbn: '', title: '', author: '', publisher: '', publishYear: '', description: '', totalCopies: 1 });
            runSearch(page);
        } catch (err) {
            setError('Failed to add book: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleAddCategory = async (e) => {
        e.preventDefault();
        setError(''); setSuccess('');
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

    const handleRemove = async (bookId) => {
        setError(''); setSuccess('');
        if (!window.confirm('Remove this book from the catalogue?')) return;
        try {
            await bookApi.delete(`/books/${bookId}`);
            setSuccess('Book removed from catalogue');
            runSearch(page);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to remove book');
        }
    };

    const handlePatronBorrow = async (bookId) => {
        setError(''); setSuccess('');
        setBorrowSubmittingId(bookId);
        try {
            await borrowingApi.post('/borrows', { userId: user.id, bookId });
            setSuccess('Book borrowed - open My Borrows to manage or return.');
            await loadMyBorrows();
            runSearch(page);
        } catch (err) {
            const msg =
                typeof err.response?.data === 'string'
                    ? err.response.data
                    : err.response?.data?.message || err.message;
            setError(msg || 'Could not borrow this book.');
        } finally {
            setBorrowSubmittingId(null);
        }
    };

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />

            <div style={styles.body}>
                <h2 style={styles.pageTitle}>Book Catalogue</h2>

                {error && <p style={styles.error}>{error}</p>}
                {success && <p style={styles.success}>{success}</p>}

                {/* Search */}
                <form onSubmit={handleSearch} style={styles.searchGrid}>
                    <input
                        type="text"
                        placeholder="Title"
                        value={searchTitle}
                        onChange={(e) => {
                            const nextTitle = e.target.value;
                            setSearchTitle(nextTitle);
                            loadTitleSuggestions(nextTitle);
                        }}
                        list="book-title-suggestions"
                        style={styles.searchInput}
                    />
                    <datalist id="book-title-suggestions">
                        {titleSuggestions.map((title) => (
                            <option key={title} value={title} />
                        ))}
                    </datalist>
                    <input
                        type="text"
                        placeholder="Author"
                        value={searchAuthor}
                        onChange={(e) => setSearchAuthor(e.target.value)}
                        style={styles.searchInput}
                    />
                    <input
                        type="text"
                        placeholder="Keyword"
                        value={searchKeyword}
                        onChange={(e) => setSearchKeyword(e.target.value)}
                        style={styles.searchInput}
                    />
                    <select
                        value={searchCategoryId}
                        onChange={(e) => setSearchCategoryId(e.target.value)}
                        style={styles.searchInput}
                    >
                        <option value="">All categories</option>
                        {categories.map((c) => (
                            <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
                        ))}
                    </select>
                    <select
                        value={searchStatus}
                        onChange={(e) => setSearchStatus(e.target.value)}
                        style={styles.searchInput}
                    >
                        <option value="">Any status</option>
                        <option value="AVAILABLE">AVAILABLE</option>
                        <option value="BORROWED">BORROWED</option>
                        <option value="REMOVED">REMOVED</option>
                    </select>
                    <input
                        type="number"
                        placeholder="Year from"
                        value={searchYearFrom}
                        onChange={(e) => setSearchYearFrom(e.target.value)}
                        style={styles.searchInput}
                    />
                    <input
                        type="number"
                        placeholder="Year to"
                        value={searchYearTo}
                        onChange={(e) => setSearchYearTo(e.target.value)}
                        style={styles.searchInput}
                    />
                    <button type="submit" style={{ ...styles.btnPrimary, ...styles.searchActionBtn }}>Search</button>
                    <button type="button" onClick={handleClearSearch} style={{ ...styles.btnSecondary, ...styles.searchActionBtn }}>Clear</button>
                </form>

                {querySummary && (
                    <p style={styles.searchSummary}>
                        {querySummary} &nbsp;&middot;&nbsp; {totalResults} result{totalResults !== 1 ? 's' : ''}
                        {totalPages > 1 && ` \u00b7 page ${page + 1} / ${totalPages}`}
                    </p>
                )}
                {searchMode === 'direct' && (
                    <p style={styles.fallbackNote}>Search service unavailable &mdash; showing book-service results.</p>
                )}

                {/* Admin-only action buttons */}
                {isAdmin && (
                    <div style={styles.actionRow}>
                        <button onClick={() => setShowAddForm(!showAddForm)} style={styles.btnSuccess}>
                            {showAddForm ? 'Cancel' : '+ Add Book'}
                        </button>
                        <button onClick={() => setShowCategoryForm(!showCategoryForm)} style={styles.btnPrimary}>
                            {showCategoryForm ? 'Cancel' : '+ Add Category'}
                        </button>
                    </div>
                )}

                {/* Add Book Form - Admin only */}
                {isAdmin && showAddForm && (
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

                {/* Add Category Form - Admin only */}
                {isAdmin && showCategoryForm && (
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
                                        <strong style={{...styles.bookTitle, cursor: 'pointer', color: '#2563eb'}} onClick={() => navigate(`/books/${book.bookId}`)}>{book.title}</strong>
                                        <p style={styles.bookMeta}>{book.author} &middot; {book.publisher} &middot; {book.publishYear}</p>
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

                                {/* Admin - catalogue management (borrowing/returns go through patron flows or desk) */}
                                {isAdmin && (
                                    <div style={styles.bookActions}>
                                        <button onClick={() => handleRemove(book.bookId)} disabled={book.status === 'REMOVED'} style={styles.btnSmallDanger}>Remove</button>
                                    </div>
                                )}
                                {/* Student / staff - second entry point: borrow via borrowing-service */}
                                {canBorrowAsPatron && book.status !== 'REMOVED' && (
                                    <div style={styles.bookActions}>
                                        {userHasActiveLoanOnBook(myBorrows, book.bookId) ? (
                                            <span style={styles.onLoanHint}>You have this book on loan.</span>
                                        ) : patronBorrowEligible(book) ? (
                                            <button
                                                type="button"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handlePatronBorrow(book.bookId);
                                                }}
                                                disabled={borrowSubmittingId === book.bookId}
                                                style={styles.btnSmallBorrow}
                                            >
                                                {borrowSubmittingId === book.bookId ? 'Borrowing…' : 'Borrow'}
                                            </button>
                                        ) : null}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {/* Pagination - only shown when using search-service */}
                {searchMode === 'search' && totalPages > 1 && (
                    <div style={styles.pagination}>
                        <button
                            onClick={() => runSearch(page - 1)}
                            disabled={page === 0}
                            style={styles.btnSecondary}
                        >
                            &laquo; Prev
                        </button>
                        <span style={styles.pageInfo}>Page {page + 1} of {totalPages}</span>
                        <button
                            onClick={() => runSearch(page + 1)}
                            disabled={page >= totalPages - 1}
                            style={styles.btnSecondary}
                        >
                            Next &raquo;
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    body: { padding: '32px', maxWidth: '900px', margin: '0 auto', flex: 1, width: '100%', boxSizing: 'border-box' },
    pageTitle: { fontSize: '24px', marginBottom: '24px' },

    error: { color: '#dc2626', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#fee2e2', borderRadius: '6px' },
    success: { color: '#059669', fontSize: '13px', marginBottom: '12px', padding: '10px', background: '#d1fae5', borderRadius: '6px' },
    searchSummary: { fontSize: '13px', color: '#555', marginBottom: '16px' },
    fallbackNote: { fontSize: '12px', color: '#9a3412', background: '#fff7ed', padding: '6px 10px', borderRadius: '4px', marginBottom: '12px' },

    searchGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
        gap: '10px',
        marginBottom: '16px',
        alignItems: 'center'
    },
    searchActionBtn: { width: '100%', minWidth: 0 },
    searchInput: { padding: '10px 12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px' },

    actionRow: { display: 'flex', gap: '10px', marginBottom: '20px' },
    btnPrimary: { padding: '10px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSecondary: { padding: '10px 20px', background: '#fff', color: '#666', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSuccess: { padding: '10px 20px', background: '#059669', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '14px', cursor: 'pointer' },
    btnSmallDanger: { padding: '6px 14px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' },
    btnSmallBorrow: { padding: '6px 14px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '4px', fontSize: '12px', cursor: 'pointer', fontWeight: '600' },
    onLoanHint: { fontSize: '12px', color: '#1e40af', fontWeight: '500' },

    card: { background: '#fff', padding: '24px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', marginBottom: '20px' },
    cardTitle: { fontSize: '18px', marginBottom: '16px' },
    formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' },
    formRow: { display: 'flex', gap: '10px', alignItems: 'center' },
    input: { padding: '12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },
    textarea: { width: '100%', padding: '12px', marginTop: '12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box', minHeight: '60px' },

    categoryLine: { fontSize: '14px', color: '#666', marginBottom: '20px' },
    muted: { color: '#999', textAlign: 'center', padding: '40px' },

    bookCard: { background: '#fff', padding: '16px 20px', borderRadius: '8px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)', marginBottom: '12px' },
    bookInfo: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' },
    bookTitle: { fontSize: '16px' },
    bookMeta: { fontSize: '13px', color: '#666', margin: '4px 0' },
    bookRight: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' },
    badge: { padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: '600' },
    copies: { fontSize: '13px', color: '#666' },
    bookActions: { display: 'flex', gap: '8px', marginTop: '12px', paddingTop: '12px', borderTop: '1px solid #f0f0f0' },

    pagination: { display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '16px', marginTop: '24px' },
    pageInfo: { fontSize: '14px', color: '#555' },
};

export default Books;