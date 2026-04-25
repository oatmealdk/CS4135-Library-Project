import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import BookDetail from '../pages/BookDetail';
import borrowingApi from '../services/borrowingApi';
import bookApi from '../services/bookApi';

jest.mock('../services/borrowingApi');
jest.mock('../services/bookApi');

const MOCK_BOOK = {
    bookId: 1,
    isbn: '978-0-13-468599-1',
    title: 'Clean Architecture',
    author: 'Robert C. Martin',
    publisher: 'Prentice Hall',
    publishYear: 2017,
    description: 'A guide to software structure and design.',
    totalCopies: 3,
    availableCopies: 2,
    status: 'AVAILABLE',
    categoryIds: [],
};

const MOCK_BORROW_RESPONSE = {
    recordId: 42,
    userId: 7,
    bookId: 1,
    status: 'ACTIVE',
    borrowDate: [2026, 4, 25],
    dueDate: [2026, 5, 9],
    returnDate: null,
    renewCount: 0,
    fine: null,
};

function setLoggedInUser(user) {
    localStorage.setItem('token', 'fake-jwt');
    localStorage.setItem('user', JSON.stringify(user));
}

function renderBookDetail() {
    return render(
        <MemoryRouter initialEntries={['/books/1']}>
            <Routes>
                <Route path="/books/:bookId" element={<BookDetail onLogout={jest.fn()} />} />
            </Routes>
        </MemoryRouter>
    );
}

afterEach(() => {
    localStorage.clear();
    jest.restoreAllMocks();
});

describe('Borrow flow integration', () => {
    it('allows a patron to borrow a book and shows a success message', async () => {
        setLoggedInUser({ id: 7, name: 'Test Patron', email: 'patron@test.com', role: 'PATRON' });

        bookApi.get.mockImplementation((url) => {
            if (url === '/books/1') return Promise.resolve({ data: MOCK_BOOK });
            return Promise.reject(new Error('unexpected'));
        });
        borrowingApi.get.mockResolvedValue({ data: [] });
        borrowingApi.post.mockResolvedValue({ data: MOCK_BORROW_RESPONSE });

        renderBookDetail();

        await waitFor(() => {
            expect(screen.getByText('Clean Architecture')).toBeInTheDocument();
        });

        const borrowButton = await screen.findByRole('button', { name: /borrow this book/i });
        await userEvent.click(borrowButton);

        await waitFor(() => {
            expect(screen.getByText(/book borrowed/i)).toBeInTheDocument();
        });

        expect(borrowingApi.post).toHaveBeenCalledWith('/borrows', { userId: 7, bookId: 1 });
    });

    it('shows an error when the borrow request fails (e.g. quota exceeded)', async () => {
        setLoggedInUser({ id: 7, name: 'Test Patron', email: 'patron@test.com', role: 'PATRON' });

        bookApi.get.mockImplementation((url) => {
            if (url === '/books/1') return Promise.resolve({ data: MOCK_BOOK });
            return Promise.reject(new Error('unexpected'));
        });
        borrowingApi.get.mockResolvedValue({ data: [] });
        borrowingApi.post.mockRejectedValue({
            response: { status: 422, data: 'Borrow limit reached for this user.' },
        });

        renderBookDetail();

        await waitFor(() => {
            expect(screen.getByText('Clean Architecture')).toBeInTheDocument();
        });

        const borrowButton = await screen.findByRole('button', { name: /borrow this book/i });
        await userEvent.click(borrowButton);

        await waitFor(() => {
            expect(screen.getByText(/borrow limit reached/i)).toBeInTheDocument();
        });
    });

    it('shows unavailable message when no copies are left', async () => {
        setLoggedInUser({ id: 7, name: 'Test Patron', email: 'patron@test.com', role: 'PATRON' });

        const nocopies = { ...MOCK_BOOK, availableCopies: 0, status: 'BORROWED' };
        bookApi.get.mockImplementation((url) => {
            if (url === '/books/1') return Promise.resolve({ data: nocopies });
            return Promise.reject(new Error('unexpected'));
        });
        borrowingApi.get.mockResolvedValue({ data: [] });

        renderBookDetail();

        await waitFor(() => {
            expect(screen.getByText('Clean Architecture')).toBeInTheDocument();
        });

        expect(screen.getByText(/no copies are currently available/i)).toBeInTheDocument();
    });
});
