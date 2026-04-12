/** Borrow statuses that mean the user still has the physical copy (not returned). */
export const ON_LOAN_STATUSES = ['ACTIVE', 'RENEWED', 'OVERDUE'];

export function userHasActiveLoanOnBook(borrows, bookId) {
    if (!borrows?.length || bookId == null) return false;
    const id = Number(bookId);
    return borrows.some(
        (b) => Number(b.bookId) === id && ON_LOAN_STATUSES.includes(b.status)
    );
}

/** Records with a fine that has not been paid (typically after return). */
export function recordsWithUnpaidFines(records) {
    if (!records?.length) return [];
    return records.filter((r) => r.fine && !r.fine.isPaid);
}

export function unpaidFinesTotalEuro(records) {
    return recordsWithUnpaidFines(records).reduce((sum, r) => sum + (r.fine?.amount || 0), 0);
}
