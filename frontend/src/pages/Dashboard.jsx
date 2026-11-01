
import { useCallback, useEffect, useMemo, useState } from 'react';
import SummaryCard from '../components/SummaryCard';
import api from '../api/axiosInstance';
import styles from './Dashboard.module.css';

const currencyFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2
});

const formatAmount = (amount) =>
  currencyFormatter.format(Number(amount) || 0);

const parseDate = (value) => {
  if (!value) return null;

  // Treat YYYY-MM-DD as a local calendar date.
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

  const date = match
    ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
    : new Date(value);

  return Number.isNaN(date.getTime()) ? null : date;
};

const formatDate = (value) => {
  const date = parseDate(value);

  if (!date) return 'Date unavailable';

  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  });
};

const getMonthlyAmount = (subscription) => {
  const amount = Number(subscription.amount) || 0;

  switch (subscription.billingCycle?.toUpperCase()) {
    case 'QUARTERLY':
      return amount / 3;
    case 'YEARLY':
      return amount / 12;
    case 'MONTHLY':
    default:
      return amount;
  }
};

const Dashboard = () => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const fetchDashboardData = useCallback(async () => {
    setLoading(true);
    setError('');
    setNotice('');

    const results = await Promise.allSettled([
      api.get('/api/subscriptions'),
      api.get('/api/sms/imports')
    ]);

    const [subscriptionResult, transactionResult] = results;

    if (subscriptionResult.status === 'fulfilled') {
      const response = subscriptionResult.value.data;

      if (response?.success) {
        setSubscriptions(
          Array.isArray(response.data) ? response.data : []
        );
      } else {
        setSubscriptions([]);
      }
    } else {
      setSubscriptions([]);
    }

    if (transactionResult.status === 'fulfilled') {
      const response = transactionResult.value.data;

      if (response?.success) {
        setTransactions(
          Array.isArray(response.data) ? response.data : []
        );
      } else {
        setTransactions([]);
      }
    } else {
      setTransactions([]);
    }

    const subscriptionsFailed =
      subscriptionResult.status === 'rejected' ||
      (subscriptionResult.status === 'fulfilled' &&
        !subscriptionResult.value.data?.success);

    const transactionsFailed =
      transactionResult.status === 'rejected' ||
      (transactionResult.status === 'fulfilled' &&
        !transactionResult.value.data?.success);

    if (subscriptionsFailed && transactionsFailed) {
      setError('Unable to load dashboard data. Please try again.');
    } else if (subscriptionsFailed || transactionsFailed) {
      setNotice(
        subscriptionsFailed
          ? 'Subscription information could not be loaded.'
          : 'Transaction information could not be loaded.'
      );
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const dashboardData = useMemo(() => {
    const activeSubscriptions = subscriptions.filter(
      (item) => item.status?.toUpperCase() === 'ACTIVE'
    );

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const next30Days = new Date(today);
    next30Days.setDate(next30Days.getDate() + 30);
    next30Days.setHours(23, 59, 59, 999);

    const upcomingRenewals = activeSubscriptions
      .filter((item) => {
        const renewalDate = parseDate(item.nextRenewalDate);

        return renewalDate &&
          renewalDate >= today &&
          renewalDate <= next30Days;
      })
      .sort(
        (a, b) =>
          parseDate(a.nextRenewalDate) -
          parseDate(b.nextRenewalDate)
      );

    const activeTrials = activeSubscriptions.filter(
      (item) => item.isTrial === true
    );

    const categoryTotals = {};

    activeSubscriptions.forEach((item) => {
      const category = item.category || 'OTHER';
      const monthlyAmount = getMonthlyAmount(item);

      categoryTotals[category] =
        (categoryTotals[category] || 0) + monthlyAmount;
    });

    const spendingByCategory = Object.entries(categoryTotals)
      .map(([category, amount]) => ({ category, amount }))
      .sort((a, b) => b.amount - a.amount);

    const monthlySpending = activeSubscriptions.reduce(
      (total, item) => total + getMonthlyAmount(item),
      0
    );

    const recentTransactions = [...transactions]
      .sort((a, b) => {
        const dateA = parseDate(a.transactionDate)?.getTime() || 0;
        const dateB = parseDate(b.transactionDate)?.getTime() || 0;
        return dateB - dateA;
      })
      .slice(0, 5);

    return {
      activeSubscriptions,
      upcomingRenewals,
      activeTrials,
      spendingByCategory,
      monthlySpending,
      recentTransactions
    };
  }, [subscriptions, transactions]);

  const maxCategoryAmount = Math.max(
    ...dashboardData.spendingByCategory.map((item) => item.amount),
    1
  );

  const summaryCards = [
    {
      id: 1,
      title: 'Monthly Spending',
      value: formatAmount(dashboardData.monthlySpending)
    },
    {
      id: 2,
      title: 'Active Subscriptions',
      value: dashboardData.activeSubscriptions.length
    },
    {
      id: 3,
      title: 'Upcoming Renewals',
      value: dashboardData.upcomingRenewals.length
    },
    {
      id: 4,
      title: 'Active Free Trials',
      value: dashboardData.activeTrials.length
    }
  ];

  if (loading) {
    return (
      <div className={styles.loading}>
        <div className={styles.spinner} />
        <p>Loading your dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.errorState}>
        <h2>Dashboard unavailable</h2>
        <p>{error}</p>
        <button onClick={fetchDashboardData}>Try Again</button>
      </div>
    );
  }

  return (
    <div className={styles.dashboard}>
      <header className={styles.header}>
        <div>
          <span className={styles.eyebrow}>YOUR FINANCIAL OVERVIEW</span>
          <h1>Dashboard</h1>
          <p>Track your subscriptions and stay in control of your spending.</p>
        </div>

        <div className={styles.headerDate}>
          <span>Today</span>
          <strong>
            {new Date().toLocaleDateString('en-IN', {
              day: '2-digit',
              month: 'short',
              year: 'numeric'
            })}
          </strong>
        </div>
      </header>

      {notice && (
        <div className={styles.notice} role="status">
          {notice}
          <button onClick={fetchDashboardData}>Refresh</button>
        </div>
      )}

      <section className={styles.summarySection}>
        <div className={styles.sectionHeading}>
          <div>
            <h2>At a glance</h2>
            <p>Your subscription summary</p>
          </div>
        </div>

        <div className={styles.summaryGrid}>
          {summaryCards.map((card, index) => (
            <div className={styles.summaryItem} key={card.id}>
              <div className={`${styles.cardIcon} ${styles[`icon${index}`]}`}>
                {['◈', '↗', '◷', '✓'][index]}
              </div>
              <SummaryCard title={card.title} value={card.value} />
            </div>
          ))}
        </div>
      </section>

      <div className={styles.contentGrid}>
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Spending by category</h2>
              <p>Monthly equivalent of active subscriptions</p>
            </div>
            <span className={styles.panelIcon}>◉</span>
          </div>

          <div className={styles.spendingTotal}>
            <span>Total monthly subscription spending</span>
            <strong>{formatAmount(dashboardData.monthlySpending)}</strong>
          </div>

          {dashboardData.spendingByCategory.length > 0 ? (
            <div className={styles.categoryList}>
              {dashboardData.spendingByCategory.map((item) => (
                <div className={styles.categoryItem} key={item.category}>
                  <div className={styles.categoryInfo}>
                    <span>{item.category.replaceAll('_', ' ')}</span>
                    <strong>{formatAmount(item.amount)}</strong>
                  </div>

                  <div className={styles.progressTrack}>
                    <div
                      className={styles.progressBar}
                      style={{
                        width: `${
                          (item.amount / maxCategoryAmount) * 100
                        }%`
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className={styles.empty}>
              No active subscription spending available yet.
            </p>
          )}
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Upcoming renewals</h2>
              <p>Payments expected in the next 30 days</p>
            </div>
            <span className={styles.panelIcon}>◷</span>
          </div>

          {dashboardData.upcomingRenewals.length > 0 ? (
            <div className={styles.renewalList}>
              {dashboardData.upcomingRenewals.slice(0, 5).map((item) => (
                <div className={styles.renewalItem} key={item.id}>
                  <div className={styles.renewalIcon}>
                    {item.name?.charAt(0)?.toUpperCase() || 'S'}
                  </div>

                  <div className={styles.renewalDetails}>
                    <strong>{item.name}</strong>
                    <span>{item.billingCycle || 'Subscription'}</span>
                  </div>

                  <div className={styles.renewalDate}>
                    <strong>{formatDate(item.nextRenewalDate)}</strong>
                    <span>{formatAmount(item.amount)}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className={styles.emptyState}>
              <span>✓</span>
              <strong>You're all caught up!</strong>
              <p>No renewals in the next 30 days.</p>
            </div>
          )}
        </section>

        <section className={`${styles.panel} ${styles.recentPanel}`}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Recent transactions</h2>
              <p>Your latest imported payment activity</p>
            </div>
            <span className={styles.panelIcon}>↗</span>
          </div>

          {dashboardData.recentTransactions.length > 0 ? (
            <div className={styles.recentList}>
              {dashboardData.recentTransactions.map((item) => {
                const isCredit =
                  item.direction?.toUpperCase() === 'CREDIT';

                return (
                  <div className={styles.recentItem} key={item.id}>
                    <div className={styles.recentIcon}>
                      {item.merchant?.charAt(0)?.toUpperCase() || 'T'}
                    </div>

                    <div className={styles.recentDetails}>
                      <strong>{item.merchant || 'Unknown merchant'}</strong>
                      <span>
                        {formatDate(item.transactionDate)} ·{' '}
                        {item.paymentMethod || 'Payment'}
                      </span>
                    </div>

                    <strong className={
                      isCredit
                        ? styles.recentCredit
                        : styles.recentDebit
                    }>
                      {isCredit ? '+' : '-'} {formatAmount(item.amount)}
                    </strong>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className={styles.empty}>
              No imported transactions available yet.
            </p>
          )}
        </section>
      </div>

      <div className={styles.footerNote}>
        <span>✦</span>
        <p>
          Stay informed, manage recurring payments, and make smarter
          subscription decisions.
        </p>
      </div>
    </div>
  );
};

export default Dashboard;
