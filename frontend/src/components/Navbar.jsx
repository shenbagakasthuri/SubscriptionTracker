
import React from 'react';
import styles from './Navbar.module.css';

const Navbar = () => (
  <header className={styles.navbar} aria-label="Top navigation">
    <div className={styles.brand}>
      <div className={styles.logo}>S</div>

      <div className={styles.brandInfo}>
        <h1 className={styles.title}>Subly</h1>
        <span className={styles.subtitle}>SUBSCRIPTION TRACKER</span>
      </div>
    </div>

    <div className={styles.tagline}>
      Simple subscriptions. Smarter spending.
    </div>
  </header>
);

export default Navbar;
