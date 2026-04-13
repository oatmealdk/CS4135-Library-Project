import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import borrowingApi from '../services/borrowingApi';
import AppNav from '../components/AppNav';

function parseUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
        return null;
    }
}

function formatApiDate(val) {
    if (!val) return '—';
    if (Array.isArray(val) && val.length >= 3) {
        const [y, m, d] = val;
        return new Date(y, m - 1, d).toLocaleDateString();
    }
    try {
        const d = new Date(val);
        return Number.isNaN(d.getTime()) ? String(val) : d.toLocaleDateString();
    } catch {
        return String(val);
    }
}

function errMessage(err) {
    const d = err?.response?.data;
    if (typeof d === 'string' && d.trim()) return d;
    if (d?.message) return d.message;
    return err?.message || 'Request failed.';
}

export default function AdminTesting({ onLogout }) {
    const navigate = useNavigate();
    const user = parseUser();

    useEffect(() => {
        if (user?.role !== 'ADMIN') {
            navigate('/dashboard', { replace: true });
        }
    }, [user?.role, navigate]);

    const [overdueMsg, setOverdueMsg] = useState('');
    const [overdueErr, setOverdueErr] = useState('');
    const [overdueBusy, setOverdueBusy] = useState(false);

    const [recordId, setRecordId] = useState('');
    const [dueDate, setDueDate] = useState('');
    const [dueBusy, setDueBusy] = useState(false);
    const [dueErr, setDueErr] = useState('');
    const [dueOk, setDueOk] = useState(null);

    if (user?.role !== 'ADMIN') return null;

    return (
        <div style={styles.page}>
            <AppNav onLogout={onLogout} />
            <div style={styles.body}>
                <p style={styles.back}>
                    <Link to="/dashboard" style={styles.backLink}>← Dashboard</Link>
                </p>
                <h1 style={styles.title}>Borrowing - testing tools</h1>
                <p style={styles.lead}>
                    These actions talk to the real borrowing service and database. They exist so you can
                    reproduce overdue, renewal, and fine scenarios without waiting weeks on the calendar
                    or editing rows by hand. Use a normal patron (student or staff) account to create loans, 
                    note the borrow record ID from <strong>My Borrows</strong> (or your browser's 
                    network tab), then adjust dates and run checks here. Nothing here bypasses business 
                    rules that apply at return time (fines still come from late returns).
                </p>

                <section style={styles.panel} aria-labelledby="overdue-heading">
                    <h2 id="overdue-heading" style={styles.panelTitle}>
                        Run overdue check
                    </h2>
                    <p style={styles.panelDesc}>
                        The production app runs a scheduled job once per day (same logic) to mark loans whose
                        due date is already in the past as <strong>OVERDUE</strong>. Use this button to run
                        that pass immediately after you have moved a due date into the past (see below). It
                        does not create fines - those are calculated when the patron returns the book. This 
                        would run by default every day at 01:00, but we put this here so it can be run manually.
                    </p>
                    <button
                        type="button"
                        disabled={overdueBusy}
                        onClick={async () => {
                            setOverdueMsg('');
                            setOverdueErr('');
                            setOverdueBusy(true);
                            try {
                                const res = await borrowingApi.post('/borrows/maintenance/run-overdue-check');
                                const n = res.data?.recordsMarkedOverdue ?? 0;
                                setOverdueMsg(`Marked ${n} borrow record(s) as overdue.`);
                            } catch (err) {
                                setOverdueErr(errMessage(err));
                            } finally {
                                setOverdueBusy(false);
                            }
                        }}
                        style={styles.primaryBtn}
                    >
                        {overdueBusy ? 'Running…' : 'Run overdue check now'}
                    </button>
                    {overdueMsg && <p style={styles.ok}>{overdueMsg}</p>}
                    {overdueErr && <p style={styles.err}>{overdueErr}</p>}
                </section>

                <section style={styles.panel} aria-labelledby="duedate-heading">
                    <h2 id="duedate-heading" style={styles.panelTitle}>
                        Set due date (active or renewed loan)
                    </h2>
                    <p style={styles.panelDesc}>
                        Enter a <strong>borrow record ID</strong> for a loan that is still{' '}
                        <strong>ACTIVE</strong> or <strong>RENEWED</strong>, and choose a new due date. The
                        change is saved in the borrowing service like any other update - this is not a raw
                        database edit. Typical test flow: set the due date to 2 days or more before today, click 'Run
                        overdue check' above, then sign in as the patron and confirm the loan shows as
                        overdue, or set a future date to clear that path. You cannot change due dates for
                        loans that are already <strong>RETURNED</strong>, <strong>OVERDUE</strong>, or
                        otherwise finished - those states use different rules.
                    </p>
                    <div style={styles.formRow}>
                        <label style={styles.label} htmlFor="test-record-id">
                            Borrow record ID
                        </label>
                        <input
                            id="test-record-id"
                            type="number"
                            min="1"
                            step="1"
                            placeholder="e.g. 12"
                            value={recordId}
                            onChange={(e) => setRecordId(e.target.value)}
                            style={styles.input}
                        />
                    </div>
                    <div style={styles.formRow}>
                        <label style={styles.label} htmlFor="test-due-date">
                            New due date
                        </label>
                        <input
                            id="test-due-date"
                            type="date"
                            value={dueDate}
                            onChange={(e) => setDueDate(e.target.value)}
                            style={styles.inputDate}
                        />
                    </div>
                    <button
                        type="button"
                        disabled={dueBusy || !recordId.trim() || !dueDate}
                        onClick={async () => {
                            setDueErr('');
                            setDueOk(null);
                            setDueBusy(true);
                            try {
                                const id = parseInt(recordId, 10);
                                if (Number.isNaN(id) || id < 1) {
                                    setDueErr('Enter a positive whole number for record ID.');
                                    return;
                                }
                                const res = await borrowingApi.put(
                                    `/borrows/maintenance/${id}/due-date`,
                                    { dueDate }
                                );
                                setDueOk(res.data);
                            } catch (err) {
                                setDueErr(errMessage(err));
                            } finally {
                                setDueBusy(false);
                            }
                        }}
                        style={styles.primaryBtn}
                    >
                        {dueBusy ? 'Saving…' : 'Apply new due date'}
                    </button>
                    {dueErr && <p style={styles.err}>{dueErr}</p>}
                    {dueOk && (
                        <div style={styles.resultBox}>
                            <p style={styles.resultTitle}>Updated borrow record</p>
                            <ul style={styles.resultList}>
                                <li>
                                    <strong>Record ID:</strong> {dueOk.recordId}
                                </li>
                                <li>
                                    <strong>Status:</strong> {dueOk.status}
                                </li>
                                <li>
                                    <strong>Due date:</strong> {formatApiDate(dueOk.dueDate)}
                                </li>
                                <li>
                                    <strong>User ID:</strong> {dueOk.userId} &middot; <strong>Book ID:</strong>{' '}
                                    {dueOk.bookId}
                                </li>
                            </ul>
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}

const styles = {
    page: { minHeight: '100vh', background: '#f5f5f5', display: 'flex', flexDirection: 'column' },
    body: {
        padding: '32px',
        maxWidth: '720px',
        margin: '0 auto',
        flex: 1,
        width: '100%',
        boxSizing: 'border-box',
    },
    back: { margin: '0 0 16px 0' },
    backLink: { fontSize: '13px', color: '#2563eb', textDecoration: 'none', fontWeight: '600' },
    title: { fontSize: '24px', marginBottom: '12px', color: '#111' },
    lead: {
        fontSize: '14px',
        color: '#444',
        lineHeight: 1.65,
        marginBottom: '28px',
    },
    panel: {
        background: '#fff',
        borderRadius: '10px',
        border: '1px solid #e5e7eb',
        padding: '20px 20px 22px',
        marginBottom: '20px',
        boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
    },
    panelTitle: { fontSize: '17px', margin: '0 0 10px 0', color: '#111' },
    panelDesc: {
        fontSize: '13px',
        color: '#555',
        lineHeight: 1.6,
        margin: '0 0 16px 0',
    },
    formRow: { marginBottom: '12px' },
    label: { display: 'block', fontSize: '12px', fontWeight: '600', color: '#374151', marginBottom: '6px' },
    input: {
        width: '100%',
        maxWidth: '280px',
        padding: '8px 10px',
        fontSize: '14px',
        border: '1px solid #d1d5db',
        borderRadius: '6px',
        boxSizing: 'border-box',
    },
    inputDate: {
        width: '100%',
        maxWidth: '220px',
        padding: '8px 10px',
        fontSize: '14px',
        border: '1px solid #d1d5db',
        borderRadius: '6px',
    },
    primaryBtn: {
        padding: '10px 16px',
        fontSize: '14px',
        fontWeight: '600',
        background: '#111827',
        color: '#fff',
        border: 'none',
        borderRadius: '8px',
        cursor: 'pointer',
        marginTop: '4px',
    },
    ok: { fontSize: '13px', color: '#059669', margin: '12px 0 0 0' },
    err: { fontSize: '13px', color: '#dc2626', margin: '12px 0 0 0', lineHeight: 1.45 },
    resultBox: {
        marginTop: '16px',
        padding: '14px 16px',
        background: '#f0fdf4',
        border: '1px solid #bbf7d0',
        borderRadius: '8px',
    },
    resultTitle: { fontSize: '13px', fontWeight: '700', color: '#166534', margin: '0 0 8px 0' },
    resultList: { fontSize: '13px', color: '#14532d', margin: 0, paddingLeft: '18px', lineHeight: 1.55 },
};
