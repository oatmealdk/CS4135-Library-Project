/** Borrow statuses that mean the user still has the physical copy (not returned). */
export const ON_LOAN_STATUSES = ['ACTIVE', 'RENEWED', 'OVERDUE'];

export function userHasActiveLoanOnBook(borrows, bookId) {
    if (!borrows?.length || bookId == null) return false;
    const id = Number(bookId);
    return borrows.some(
        (b) => Number(b.bookId) === id && ON_LOAN_STATUSES.includes(b.status)
    );
}
