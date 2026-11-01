import { useEffect, useMemo, useState } from 'react';
import {
    getSubscriptions,
    createSubscription,
    updateSubscription,
    deleteSubscription
} from '../api/subscriptions';
import styles from './Subscriptions.module.css';

const categories = [
    'OTT', 'MUSIC', 'TELECOM', 'SOFTWARE',
    'UTILITIES', 'FITNESS', 'OTHER'
];

const billingCycles = ['MONTHLY', 'QUARTERLY', 'YEARLY'];

const paymentMethods = [
    'UPI_AUTOPAY', 'CARD', 'NETBANKING', 'OTHER'
];

const initialForm = {
    name: '',
    category: 'OTT',
    amount: '',
    billingCycle: 'MONTHLY',
    nextRenewalDate: '',
    paymentMethod: 'UPI_AUTOPAY',
    isTrial: false,
    trialEndDate: '',
    status: 'ACTIVE',
    lastUsedDate: '',
    cancelUrl: ''
};

const formatLabel = (value = '') =>
    value.replaceAll('_', ' ').toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());

const Subscriptions = () => {
    const [subscriptions, setSubscriptions] = useState([]);
    const [form, setForm] = useState(initialForm);
    const [editingId, setEditingId] = useState(null);

    const [search, setSearch] = useState('');
    const [categoryFilter, setCategoryFilter] = useState('ALL');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [sortBy, setSortBy] = useState('renewal');

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const [showForm, setShowForm] = useState(false);

    const loadSubscriptions = async () => {
        try {
            setLoading(true);
            const response = await getSubscriptions();

            if (response.success) {
                setSubscriptions(response.data || []);
            } else {
                setError(response.message || 'Unable to load subscriptions.');
            }
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Failed to load subscriptions.'
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadSubscriptions();
    }, []);

    const handleChange = (event) => {
        const { name, value, type, checked } = event.target;

        setForm(previous => ({
            ...previous,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const resetForm = () => {
        setForm(initialForm);
        setEditingId(null);
        setShowForm(false);
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        setError('');
        setSuccess('');

        if (form.isTrial && !form.trialEndDate) {
            setError('Please select a trial end date.');
            return;
        }

        const payload = {
            ...form,
            amount: Number(form.amount),
            trialEndDate: form.isTrial ? form.trialEndDate : null,
            lastUsedDate: form.lastUsedDate || null,
            cancelUrl: form.cancelUrl.trim() || null
        };

        try {
            setSaving(true);

            if (editingId) {
                await updateSubscription(editingId, payload);
                setSuccess('Subscription updated successfully.');
            } else {
                await createSubscription(payload);
                setSuccess('Subscription added successfully.');
            }

            resetForm();
            await loadSubscriptions();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Unable to save subscription.'
            );
        } finally {
            setSaving(false);
        }
    };

    const handleEdit = (subscription) => {
        setEditingId(subscription.id);

        setForm({
            name: subscription.name || '',
            category: subscription.category,
            amount: subscription.amount,
            billingCycle: subscription.billingCycle,
            nextRenewalDate: subscription.nextRenewalDate || '',
            paymentMethod: subscription.paymentMethod,
            isTrial: subscription.isTrial,
            trialEndDate: subscription.trialEndDate || '',
            status: subscription.status || 'ACTIVE',
            lastUsedDate: subscription.lastUsedDate || '',
            cancelUrl: subscription.cancelUrl || ''
        });

        setShowForm(true);
        setError('');
        setSuccess('');
    };

    const handleDelete = async (id) => {
        const confirmed = window.confirm(
            'Are you sure you want to delete this subscription?'
        );

        if (!confirmed) return;

        try {
            setError('');
            await deleteSubscription(id);
            setSuccess('Subscription deleted successfully.');
            await loadSubscriptions();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Unable to delete subscription.'
            );
        }
    };

    const filteredSubscriptions = useMemo(() => {
        let result = [...subscriptions];

        // Search by subscription name
        result = result.filter(subscription =>
            subscription.name.toLowerCase().includes(search.toLowerCase())
        );

        // Category filter
        if (categoryFilter !== 'ALL') {
            result = result.filter(
                subscription => subscription.category === categoryFilter
            );
        }

        // Status filter
        if (statusFilter !== 'ALL') {
            result = result.filter(
                subscription => subscription.status === statusFilter
            );
        }

        // Sorting
        result.sort((a, b) => {
            if (sortBy === 'amount-low') {
                return Number(a.amount) - Number(b.amount);
            }

            if (sortBy === 'amount-high') {
                return Number(b.amount) - Number(a.amount);
            }

            if (sortBy === 'renewal') {
                return new Date(a.nextRenewalDate) -
                    new Date(b.nextRenewalDate);
            }

            if (sortBy === 'renewal-latest') {
                return new Date(b.nextRenewalDate) -
                    new Date(a.nextRenewalDate);
            }

            return a.name.localeCompare(b.name);
        });

        return result;
    }, [subscriptions, search, categoryFilter, statusFilter, sortBy]);

    const activeCount = subscriptions.filter(
        item => item.status === 'ACTIVE'
    ).length;

    const monthlyTotal = subscriptions
        .filter(item => item.status === 'ACTIVE')
        .reduce((total, item) => {
            const amount = Number(item.amount);

            const monthlyAmount =
                item.billingCycle === 'YEARLY' ? amount / 12 :
                item.billingCycle === 'QUARTERLY' ? amount / 3 :
                amount;

            return total + monthlyAmount;
        }, 0);

    return (
        <div className={styles.page}>
            <div className={styles.header}>
                <div>
                    <p className={styles.eyebrow}>MANAGE YOUR SPENDING</p>
                    <h1>Subscriptions</h1>
                    <p className={styles.subtitle}>
                        Keep track of your recurring payments in one place.
                    </p>
                </div>

                <button
                    className={styles.primaryButton}
                    onClick={() => {
                        resetForm();
                        setShowForm(true);
                    }}
                >
                    + Add Subscription
                </button>
            </div>

            {error && <div className={styles.error}>{error}</div>}
            {success && <div className={styles.success}>{success}</div>}

            <div className={styles.summaryGrid}>
                <div className={styles.summaryCard}>
                    <span>Total subscriptions</span>
                    <strong>{subscriptions.length}</strong>
                </div>

                <div className={styles.summaryCard}>
                    <span>Active subscriptions</span>
                    <strong>{activeCount}</strong>
                </div>

                <div className={styles.summaryCard}>
                    <span>Estimated monthly spending</span>
                    <strong>
                        ₹{monthlyTotal.toLocaleString('en-IN', {
                            maximumFractionDigits: 2
                        })}
                    </strong>
                </div>
            </div>

            {showForm && (
                <form className={styles.formCard} onSubmit={handleSubmit}>
                    <div className={styles.formHeader}>
                        <h2>
                            {editingId ? 'Edit Subscription' : 'Add Subscription'}
                        </h2>

                        <button
                            type="button"
                            className={styles.closeButton}
                            onClick={resetForm}
                        >
                            ✕
                        </button>
                    </div>

                    <div className={styles.formGrid}>
                        <label>
                            Subscription name *
                            <input
                                name="name"
                                value={form.name}
                                onChange={handleChange}
                                placeholder="e.g. Spotify Premium"
                                maxLength={100}
                                required
                            />
                        </label>

                        <label>
                            Amount (₹) *
                            <input
                                type="number"
                                name="amount"
                                value={form.amount}
                                onChange={handleChange}
                                min="0.01"
                                step="0.01"
                                required
                            />
                        </label>

                        <label>
                            Category *
                            <select
                                name="category"
                                value={form.category}
                                onChange={handleChange}
                                required
                            >
                                {categories.map(item => (
                                    <option key={item} value={item}>
                                        {formatLabel(item)}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Billing cycle *
                            <select
                                name="billingCycle"
                                value={form.billingCycle}
                                onChange={handleChange}
                                required
                            >
                                {billingCycles.map(item => (
                                    <option key={item} value={item}>
                                        {formatLabel(item)}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Next renewal date *
                            <input
                                type="date"
                                name="nextRenewalDate"
                                value={form.nextRenewalDate}
                                onChange={handleChange}
                                required
                            />
                        </label>

                        <label>
                            Payment method *
                            <select
                                name="paymentMethod"
                                value={form.paymentMethod}
                                onChange={handleChange}
                                required
                            >
                                {paymentMethods.map(item => (
                                    <option key={item} value={item}>
                                        {formatLabel(item)}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Status
                            <select
                                name="status"
                                value={form.status}
                                onChange={handleChange}
                            >
                                <option value="ACTIVE">Active</option>
                                <option value="CANCELLED">Cancelled</option>
                            </select>
                        </label>

                        <label>
                            Last used date
                            <input
                                type="date"
                                name="lastUsedDate"
                                value={form.lastUsedDate}
                                onChange={handleChange}
                            />
                        </label>

                        <label className={styles.checkboxLabel}>
                            <input
                                type="checkbox"
                                name="isTrial"
                                checked={form.isTrial}
                                onChange={handleChange}
                            />
                            This subscription is in a free trial
                        </label>

                        {form.isTrial && (
                            <label>
                                Trial end date *
                                <input
                                    type="date"
                                    name="trialEndDate"
                                    value={form.trialEndDate}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                        )}

                        <label className={styles.fullWidth}>
                            Cancellation URL
                            <input
                                type="url"
                                name="cancelUrl"
                                value={form.cancelUrl}
                                onChange={handleChange}
                                maxLength={500}
                                placeholder="https://..."
                            />
                        </label>
                    </div>

                    <div className={styles.formActions}>
                        <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={resetForm}
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            className={styles.primaryButton}
                            disabled={saving}
                        >
                            {saving ? 'Saving...' :
                                editingId ? 'Update Subscription' :
                                    'Save Subscription'}
                        </button>
                    </div>
                </form>
            )}

            <div className={styles.filterCard}>
                <input
                    className={styles.searchInput}
                    value={search}
                    onChange={event => setSearch(event.target.value)}
                    placeholder="Search subscriptions..."
                />

                <select
                    value={categoryFilter}
                    onChange={event => setCategoryFilter(event.target.value)}
                >
                    <option value="ALL">All categories</option>
                    {categories.map(item => (
                        <option key={item} value={item}>
                            {formatLabel(item)}
                        </option>
                    ))}
                </select>

                <select
                    value={statusFilter}
                    onChange={event => setStatusFilter(event.target.value)}
                >
                    <option value="ALL">All statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="CANCELLED">Cancelled</option>
                </select>

                <select
                    value={sortBy}
                    onChange={event => setSortBy(event.target.value)}
                >
                    <option value="renewal">Renewal date: earliest</option>
                    <option value="renewal-latest">Renewal date: latest</option>
                    <option value="amount-low">Amount: low to high</option>
                    <option value="amount-high">Amount: high to low</option>
                    <option value="name">Name: A to Z</option>
                </select>

                <button
                    className={styles.resetButton}
                    onClick={() => {
                        setSearch('');
                        setCategoryFilter('ALL');
                        setStatusFilter('ALL');
                        setSortBy('renewal');
                    }}
                >
                    Reset
                </button>
            </div>

            <div className={styles.listHeader}>
                <h2>Your subscriptions</h2>
                <span>{filteredSubscriptions.length} results</span>
            </div>

            {loading ? (
                <div className={styles.emptyState}>Loading subscriptions...</div>
            ) : filteredSubscriptions.length === 0 ? (
                <div className={styles.emptyState}>
                    <h3>No subscriptions found</h3>
                    <p>Try changing your filters or add a new subscription.</p>
                </div>
            ) : (
                <div className={styles.tableWrapper}>
                    <table className={styles.table}>
                        <thead>
                            <tr>
                                <th>Subscription</th>
                                <th>Category</th>
                                <th>Amount</th>
                                <th>Billing</th>
                                <th>Next renewal</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {filteredSubscriptions.map(item => (
                                <tr key={item.id}>
                                    <td className={styles.nameCell}>
                                        {item.name}
                                        {item.isTrial && (
                                            <small>Free trial</small>
                                        )}
                                    </td>

                                    <td>{formatLabel(item.category)}</td>

                                    <td className={styles.amountCell}>
                                        ₹{Number(item.amount).toLocaleString('en-IN')}
                                    </td>

                                    <td>{formatLabel(item.billingCycle)}</td>

                                    <td>{item.nextRenewalDate}</td>

                                    <td>
                                        <span className={
                                            item.status === 'ACTIVE'
                                                ? styles.activeBadge
                                                : styles.cancelledBadge
                                        }>
                                            {formatLabel(item.status)}
                                        </span>
                                    </td>

                                    <td>
                                        <div className={styles.actions}>
                                            <button
                                                onClick={() => handleEdit(item)}
                                                className={styles.editButton}
                                            >
                                                Edit
                                            </button>

                                            <button
                                                onClick={() => handleDelete(item.id)}
                                                className={styles.deleteButton}
                                            >
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default Subscriptions;