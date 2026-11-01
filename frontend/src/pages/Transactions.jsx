
import { useCallback, useEffect, useMemo, useState } from 'react';
import { getTransactions } from '../api/transactions';
import styles from './Transactions.module.css';

function Transactions() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [search, setSearch] = useState('');
    const [direction, setDirection] = useState('ALL');
    const [paymentMethod, setPaymentMethod] = useState('ALL');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [sortBy, setSortBy] = useState('date-desc');

    const fetchTransactions = useCallback(async () => {
        try {
            setLoading(true);
            setError('');

            const response = await getTransactions();

            if (response?.success) {
                setTransactions(Array.isArray(response.data) ? response.data : []);
            } else {
                setError(response?.message || 'Failed to load transactions.');
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

    const paymentMethods = useMemo(() => {
        return [...new Set(
            transactions
                .map((item) => item.paymentMethod)
                .filter(Boolean)
        )].sort();
    }, [transactions]);

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
                return !Number.isNaN(date.getTime()) &&
                    date >= new Date(`${startDate}T00:00:00`);
            });
        }

        if (endDate) {
            result = result.filter((item) => {
                const date = new Date(item.transactionDate);
                return !Number.isNaN(date.getTime()) &&
                    date <= new Date(`${endDate}T23:59:59.999`);
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

    const resetFilters = () => {
        setSearch('');
        setDirection('ALL');
        setPaymentMethod('ALL');
        setStartDate('');
        setEndDate('');
        setSortBy('date-desc');
    };

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

    const formatAmount = (transaction) => {
        const amount = Number(transaction.amount);

        if (!Number.isFinite(amount)) return 'N/A';

        return `${transaction.currency || ''} ${amount.toFixed(2)}`.trim();
    };

    if (loading) {
        return (
            <div className={styles.message}>
                <div className={styles.spinner} />
                <p>Loading transactions...</p>
            </div>
        );
    }

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
            <div className={styles.header}>
                <div>
                    <span className={styles.eyebrow}>FINANCIAL ACTIVITY</span>
                    <h1>Transactions</h1>
                    <p>Track and manage your imported payment transactions.</p>
                </div>

                <div className={styles.count}>
                    <strong>{filteredTransactions.length}</strong>
                    <span>of {transactions.length} transactions</span>
                </div>
            </div>

            <section className={styles.filterPanel}>
                <div className={styles.filterHeader}>
                    <div>
                        <h2>All Transactions</h2>
                        <p>Search, filter and sort your activity.</p>
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
                        <label htmlFor="transaction-search">Search</label>
                        <input
                            id="transaction-search"
                            type="search"
                            placeholder="Search merchant or account..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="direction-filter">Transaction type</label>
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
                        <label htmlFor="payment-filter">Payment method</label>
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
                        <label htmlFor="sort-filter">Sort by</label>
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
                        <label htmlFor="start-date">From date</label>
                        <input
                            id="start-date"
                            type="date"
                            value={startDate}
                            max={endDate || undefined}
                            onChange={(e) => setStartDate(e.target.value)}
                        />
                    </div>

                    <div className={styles.filterField}>
                        <label htmlFor="end-date">To date</label>
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

            <section className={styles.tableSection}>
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
                                    <th className={styles.amountColumn}>Amount</th>
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
                                                    {transaction.merchant || 'Unknown merchant'}
                                                </span>
                                            </td>

                                            <td>{formatDate(transaction.transactionDate)}</td>

                                            <td>
                                                <span className={`${styles.badge} ${
                                                    isCredit
                                                        ? styles.credit
                                                        : styles.debit
                                                }`}>
                                                    {transaction.direction || 'N/A'}
                                                </span>
                                            </td>

                                            <td>
                                                {transaction.paymentMethod || 'N/A'}
                                            </td>

                                            <td>
                                                {transaction.maskedAccount || 'N/A'}
                                            </td>

                                            <td className={`${styles.amount} ${
                                                isCredit ? styles.creditAmount : ''
                                            }`}>
                                                {isCredit ? '+' : '-'} {formatAmount(transaction)}
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
