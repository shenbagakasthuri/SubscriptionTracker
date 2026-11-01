import React from 'react';
import styles from './SummaryCard.module.css';

const SummaryCard = ({title, value}) => (
  <div className={styles.card} role="region" aria-label={title}>
    <h3 className={styles.title}>{title}</h3>
    <p className={styles.value}>{value}</p>
  </div>
);

export default SummaryCard;
