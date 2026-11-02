
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    getTransactions,
    parseSms,
    importSms,
    deleteTransaction
} from '../api/transactions';
import styles from './Transactions.module.css';

function Transactions() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Delete states
    const [deletingId, setDeletingId] = useState(null);
    const [deleteError, setDeleteError] = useState('');

    // SMS import states
    const [smsText, setSmsText] = useState('');
    const [smsPreview, setSmsPreview] = useState(null);
    const [smsLoading, setSmsLoading] = useState(false);
    const [importLoading, setImportLoading] = useState(false);
    const [smsError, setSmsError] = useState('');
    const [smsSuccess, setSmsSuccess] = useState('');

    // Transaction filter states
    const [search, setSearch] = useState('');
    const [direction, setDirection] = useState('ALL');
    const [paymentMethod, setPaymentMethod] = useState('ALL');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [sortBy, setSortBy] = useState('date-desc');

    // Fetch transactions
    const fetchTransactions = useCallback(async () => {
        try {
            setLoading(true);
            setError('');

            const response = await getTransactions();

            if (response?.success) {
                setTransactions(
                    Array.isArray(response.data) ? response.data : []
                );
            } else {
                setError(
                    response?.message || 'Failed to load transactions.'
                );
            }
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Unable to load transactions. Please try again.'
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchTransactions();
    }, [fetchTransactions]);

    // Get available payment methods
    const paymentMethods = useMemo(() => {
        return [
            ...new Set(
                transactions
                    .map((item) => item.paymentMethod)
                    .filter(Boolean)
            )
        ].sort();
    }, [transactions]);

    // Filter and sort transactions
    const filteredTransactions = useMemo(() => {
        let result = [...transactions];

        const searchText = search.trim().toLowerCase();

        if (searchText) {
            result = result.filter((item) =>
                `${item.merchant || ''} ${item.maskedAccount || ''}`
                    .toLowerCase()
                    .includes(searchText)
            );
        }

        if (direction !== 'ALL') {
            result = result.filter(
                (item) => item.direction?.toUpperCase() === direction
            );
        }

        if (paymentMethod !== 'ALL') {
            result = result.filter(
                (item) => item.paymentMethod === paymentMethod
            );
        }

        if (startDate) {
            result = result.filter((item) => {
                const date = new Date(item.transactionDate);

                return (
                    !Number.isNaN(date.getTime()) &&
                    date >= new Date(`${startDate}T00:00:00`)
                );
            });
        }

        if (endDate) {
            result = result.filter((item) => {
                const date = new Date(item.transactionDate);

                return (
                    !Number.isNaN(date.getTime()) &&
                    date <= new Date(`${endDate}T23:59:59.999`)
                );
            });
        }

        result.sort((a, b) => {
            const amountA = Number(a.amount) || 0;
            const amountB = Number(b.amount) || 0;

            const dateA = new Date(a.transactionDate).getTime() || 0;
            const dateB = new Date(b.transactionDate).getTime() || 0;

            switch (sortBy) {
                case 'date-asc':
                    return dateA - dateB;

                case 'amount-desc':
                    return amountB - amountA;

                case 'amount-asc':
                    return amountA - amountB;

                default:
                    return dateB - dateA;
            }
        });

        return result;
    }, [
        transactions,
        search,
        direction,
        paymentMethod,
        startDate,
        endDate,
        sortBy
    ]);

    // Reset filters
    const resetFilters = () => {
        setSearch('');
        setDirection('ALL');
        setPaymentMethod('ALL');
        setStartDate('');
        setEndDate('');
        setSortBy('date-desc');
    };

    // Format date
    const formatDate = (value) => {
        if (!value) return 'N/A';

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) return 'N/A';

        return date.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
    };

    // Format transaction amount
    const formatAmount = (transaction) => {
        const amount = Number(transaction.amount);

        if (!Number.isFinite(amount)) return 'N/A';

        return `${transaction.currency || ''} ${amount.toFixed(2)}`.trim();
    };

    // Parse SMS
    const handleParseSms = async () => {
        const trimmedSms = smsText.trim();

        setSmsError('');
        setSmsSuccess('');
        setSmsPreview(null);

        if (!trimmedSms) {
            setSmsError('Please paste an SMS message first.');
            return;
        }

        if (trimmedSms.length > 1000) {
            setSmsError('SMS text must not exceed 1000 characters.');
            return;
        }

        try {
            setSmsLoading(true);

            const response = await parseSms(trimmedSms);

            if (!response?.success || !response?.data) {
                setSmsError(
                    response?.message || 'Unable to parse this SMS.'
                );
                return;
            }

            setSmsPreview(response.data);

            if (!response.data.isSupported) {
                setSmsError(
                    response.data.parseMessage ||
                    'This SMS format is not supported.'
                );
            } else if (
                !response.data.isDebit ||
                response.data.direction?.toUpperCase() !== 'DEBIT'
            ) {
                setSmsError(
                    'Only supported debit transactions can be imported.'
                );
            }
        } catch (err) {
            setSmsError(
                err.response?.data?.message ||
                'Unable to parse SMS. Please try again.'
            );
        } finally {
            setSmsLoading(false);
        }
    };

    // Import parsed SMS
    const handleImportSms = async () => {
        if (
            !smsPreview?.isSupported ||
            !smsPreview?.isDebit ||
            smsPreview?.direction?.toUpperCase() !== 'DEBIT'
        ) {
            setSmsError(
                'Please parse a supported debit SMS before importing.'
            );
            return;
        }

        try {
            setImportLoading(true);
            setSmsError('');
            setSmsSuccess('');

            const response = await importSms(smsText.trim());

            if (!response?.success) {
                setSmsError(
                    response?.message || 'Transaction import failed.'
                );
                return;
            }

            setSmsSuccess('Transaction imported successfully!');
            setSmsText('');
            setSmsPreview(null);

            await fetchTransactions();
        } catch (err) {
            setSmsError(
                err.response?.data?.message ||
                'Unable to import transaction. It may already exist.'
            );
        } finally {
            setImportLoading(false);
        }
    };

    // Delete an imported transaction
    const handleDeleteTransaction = async (transaction) => {
        const amount = Number(transaction.amount);

        const confirmed = window.confirm(
            `Are you sure you want to delete this transaction?\n\n` +
            `Merchant: ${transaction.merchant || 'Unknown'}\n` +
            `Amount: ${transaction.currency || 'INR'} ${
                Number.isFinite(amount) ? amount.toFixed(2) : 'N/A'
            }\n` +
            `Date: ${formatDate(transaction.transactionDate)}\n\n` +
            `This action cannot be undone.`
        );

        if (!confirmed) return;

        try {
            setDeletingId(transaction.id);
            setDeleteError('');
            setSmsSuccess('');

            const response = await deleteTransaction(transaction.id);

            if (!response?.success) {
                setDeleteError(
                    response?.message || 'Unable to delete transaction.'
                );
                return;
            }

            setTransactions((previous) =>
                previous.filter((item) => item.id !== transaction.id)
            );

            setSmsSuccess('Transaction deleted successfully!');
        } catch (err) {
            setDeleteError(
                err.response?.data?.message ||
                'Unable to delete transaction. Please try again.'
            );
        } finally {
            setDeletingId(null);
        }
    };

    // Clear SMS input
    const clearSms = () => {
        setSmsText('');
        setSmsPreview(null);
        setSmsError('');
        setSmsSuccess('');
    };

    // Initial loading state
    if (loading) {
        return (
            <div className={styles.message}>
                <div className={styles.spinner} />
                <p>Loading transactions...</p>
            </div>
        );
    }

    // Transaction loading error
    if (error) {
        return (
            <div className={styles.errorState}>
                <h3>Unable to load transactions</h3>
                <p>{error}</p>

                <button onClick={fetchTransactions}>
                    Try Again
                </button>
            </div>
        );
    }

    return (
        <main className={styles.container}>

            {/* Page Header */}
            <div className={styles.header}>
                <div>
                    <span className={styles.eyebrow}>
                        FINANCIAL ACTIVITY
                    </span>

                    <h1>Transactions</h1>

                    <p>
                        Track and manage your imported payment transactions.
                    </p>
                </div>

                <div className={styles.count}>
                    <strong>{filteredTransactions.length}</strong>
                    <span>
                        of {transactions.length} transactions
                    </span>
                </div>
            </div>

            {/* SMS Import Section */}
            <section className={styles.smsPanel}>
                <div className={styles.smsHeader}>
                    <h2>Import Transaction from SMS</h2>

                    <p>
                        Paste a bank transaction SMS to extract its details
                        and review them before importing.
                    </p>
                </div>

                <textarea
                    className={styles.smsTextarea}
                    placeholder="Paste your bank transaction SMS here..."
                    value={smsText}
                    maxLength={1000}
                    onChange={(e) => {
                        setSmsText(e.target.value);
                        setSmsPreview(null);
                        setSmsError('');
                        setSmsSuccess('');
                    }}
                    rows={4}
                    disabled={smsLoading || importLoading}
                    aria-label="Bank transaction SMS"
                />

                <div className={styles.smsActions}>
                    <button
                        type="button"
                        className={styles.smsPrimaryButton}
                        onClick={handleParseSms}
                        disabled={smsLoading || importLoading}
                    >
                        {smsLoading ? 'Parsing...' : 'Parse SMS'}
                    </button>

                    <button
                        type="button"
                        className={styles.smsSecondaryButton}
                        onClick={clearSms}
                        disabled={
                            smsLoading ||
                            importLoading ||
                            (!smsText && !smsPreview && !smsError && !smsSuccess)
                        }
                    >
                        Clear
                    </button>
                </div>

                {smsError && (
                    <p className={styles.smsError} role="alert">
                        {smsError}
                    </p>
                )}

                {smsSuccess && (
                    <p className={styles.smsSuccess} role="status">
                        {smsSuccess}
                    </p>
                )}

                {/* Parsed Transaction Preview */}
                {smsPreview && (
                    <div className={styles.smsPreview}>
                        <h3>Transaction Preview</h3>

                        <div className={styles.smsPreviewGrid}>
                            <div className={styles.smsPreviewItem}>
                                <span>Merchant</span>
                                <strong>
                                    {smsPreview.merchant || 'N/A'}
                                </strong>
                            </div>

                            <div className={styles.smsPreviewItem}>
                                <span>Amount</span>
                                <strong>
                                    {smsPreview.currency || ''}
                                    {' '}
                                    {smsPreview.amount ?? 'N/A'}
                                </strong>
                            </div>

                            <div className={styles.smsPreviewItem}>
                                <span>Date</span>
                                <strong>
                                    {formatDate(smsPreview.transactionDate)}
                                </strong>
                            </div>

                            <div className={styles.smsPreviewItem}>
                                <span>Transaction Type</span>
                                <strong>
                                    {smsPreview.direction || 'N/A'}
                                </strong>
                            </div>

                            <div className={styles.smsPreviewItem}>
                                <span>Payment Method</span>
                                <strong>
                                    {smsPreview.paymentMethod || 'N/A'}
                                </strong>
                            </div>

                            <div className={styles.smsPreviewItem}>
                                <span>Account</span>
                                <strong>
                                    {smsPreview.maskedAccount || 'N/A'}
                                </strong>
                            </div>
                        </div>

                        {smsPreview.confidence && (
                            <span className={styles.smsConfidence}>
                                Confidence: {smsPreview.confidence}
                            </span>
                        )}

                        {smsPreview.parseMessage && (
                            <p>{smsPreview.parseMessage}</p>
                        )}

                        {smsPreview.isSupported &&
                            smsPreview.isDebit &&
                            smsPreview.direction?.toUpperCase() === 'DEBIT' && (
                                <div className={styles.smsActions}>
                                    <button
                                        type="button"
                                        className={styles.smsPrimaryButton}
                                        onClick={handleImportSms}
                                        disabled={importLoading || smsLoading}
                                    >
                                        {importLoading
                                            ? 'Importing...'
                                            : 'Confirm & Import Transaction'}
                                    </button>
                                </div>
                            )}
                    </div>
                )}
            </section>

            {/* Transaction Filters */}
            <section className={styles.filterPanel}>
                <div className={styles.filterHeader}>
                    <div>
                        <h2>All Transactions</h2>
                        <p>
                            Search, filter and sort your activity.
                        </p>
                    </div>

                    <button
                        type="button"
                        className={styles.resetButton}
                        onClick={resetFilters}
                    >
                        Reset filters
                    </button>
                </div>

                <div className={styles.filters}>
                    <div className={styles.searchField}>
                        <label htmlFor="transaction-search">
                            Search
                        </label>

                        <input
                            id="transaction-search"
                            type="search"
                            placeholder="Search merchant or account..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="direction-filter">
                            Transaction type
                        </label>

                        <select
                            id="direction-filter"
                            value={direction}
                            onChange={(e) => setDirection(e.target.value)}
                        >
                            <option value="ALL">All types</option>
                            <option value="DEBIT">Debit</option>
                            <option value="CREDIT">Credit</option>
                        </select>
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="payment-filter">
                            Payment method
                        </label>

                        <select
                            id="payment-filter"
                            value={paymentMethod}
                            onChange={(e) => setPaymentMethod(e.target.value)}
                        >
                            <option value="ALL">All methods</option>

                            {paymentMethods.map((method) => (
                                <option key={method} value={method}>
                                    {method.replaceAll('_', ' ')}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="sort-filter">
                            Sort by
                        </label>

                        <select
                            id="sort-filter"
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value)}
                        >
                            <option value="date-desc">Newest first</option>
                            <option value="date-asc">Oldest first</option>
                            <option value="amount-desc">Highest amount</option>
                            <option value="amount-asc">Lowest amount</option>
                        </select>
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="start-date">
                            From date
                        </label>

                        <input
                            id="start-date"
                            type="date"
                            value={startDate}
                            max={endDate || undefined}
                            onChange={(e) => setStartDate(e.target.value)}
                        />
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="end-date">
                            To date
                        </label>

                        <input
                            id="end-date"
                            type="date"
                            value={endDate}
                            min={startDate || undefined}
                            onChange={(e) => setEndDate(e.target.value)}
                        />
                    </div>
                </div>
            </section>

            {/* Transaction History */}
            <section className={styles.tableSection}>
                {deleteError && (
                    <p className={styles.smsError} role="alert">
                        {deleteError}
                    </p>
                )}

                {filteredTransactions.length === 0 ? (
                    <div className={styles.empty}>
                        <div className={styles.emptyIcon}>↗</div>

                        <h3>
                            {transactions.length === 0
                                ? 'No transactions yet'
                                : 'No matching transactions'}
                        </h3>

                        <p>
                            {transactions.length === 0
                                ? 'Your imported SMS transactions will appear here.'
                                : 'Try changing your search or filters.'}
                        </p>

                        {transactions.length > 0 && (
                            <button onClick={resetFilters}>
                                Clear filters
                            </button>
                        )}
                    </div>
                ) : (
                    <div className={styles.tableWrapper}>
                        <table className={styles.table}>
                            <thead>
                                <tr>
                                    <th>Merchant</th>
                                    <th>Date</th>
                                    <th>Type</th>
                                    <th>Payment Method</th>
                                    <th>Account</th>
                                    <th className={styles.amountColumn}>
                                        Amount
                                    </th>
                                    <th>Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {filteredTransactions.map((transaction) => {
                                    const isCredit =
                                        transaction.direction?.toUpperCase() === 'CREDIT';

                                    return (
                                        <tr key={transaction.id}>
                                            <td>
                                                <span className={styles.merchant}>
                                                    {transaction.merchant ||
                                                        'Unknown merchant'}
                                                </span>
                                            </td>

                                            <td>
                                                {formatDate(
                                                    transaction.transactionDate
                                                )}
                                            </td>

                                            <td>
                                                <span
                                                    className={`${styles.badge} ${
                                                        isCredit
                                                            ? styles.credit
                                                            : styles.debit
                                                    }`}
                                                >
                                                    {transaction.direction || 'N/A'}
                                                </span>
                                            </td>

                                            <td>
                                                {transaction.paymentMethod || 'N/A'}
                                            </td>

                                            <td>
                                                {transaction.maskedAccount || 'N/A'}
                                            </td>

                                            <td
                                                className={`${styles.amount} ${
                                                    isCredit
                                                        ? styles.creditAmount
                                                        : ''
                                                }`}
                                            >
                                                {isCredit ? '+' : '-'}{' '}
                                                {formatAmount(transaction)}
                                            </td>

                                            <td>
                                                <button
                                                    type="button"
                                                    className={styles.deleteButton}
                                                    onClick={() =>
                                                        handleDeleteTransaction(transaction)
                                                    }
                                                    disabled={deletingId !== null}
                                                    aria-label={`Delete ${transaction.merchant} transaction`}
                                                >
                                                    {deletingId === transaction.id
                                                        ? 'Deleting...'
                                                        : 'Delete'}
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </main>
    );
}

export default Transactions;
