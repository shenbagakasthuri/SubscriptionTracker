import React from 'react';
import { NavLink } from 'react-router-dom';
import styles from './Sidebar.module.css';

const navigation = [
  { name: 'Dashboard', path: '/dashboard', icon: '▦' },
  { name: 'Subscriptions', path: '/subscriptions', icon: '◷' },
  { name: 'Transactions', path: '/transactions', icon: '⇄' },
];

const Sidebar = () => {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandMark}>S</div>

        <div className={styles.brandText}>
          <span className={styles.brandName}>Subly</span>
          <span className={styles.brandSubtitle}>SUBSCRIPTION TRACKER</span>
        </div>
      </div>

      <div className={styles.sectionLabel}>WORKSPACE</div>

      <nav className={styles.navList} aria-label="Main navigation">
        {navigation.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `${styles.navLink} ${isActive ? styles.active : ''}`
            }
          >
            <span className={styles.navIcon}>{item.icon}</span>
            <span>{item.name}</span>
            <span className={styles.activeIndicator} />
          </NavLink>
        ))}
      </nav>

      <div className={styles.sidebarFooter}>
        <span className={styles.footerDot} />
        <span>Manage your spending smarter</span>
      </div>
    </aside>
  );
};

export default Sidebar;