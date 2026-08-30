/**
 * Reusable skill chip with matched/missing/preferred variants.
 */
export default function SkillBadge({ name, variant = 'matched' }) {
  const icons = {
    matched: '✓',
    'matched-preferred': '✓',
    missing: '✗',
    'missing-preferred': '⚠',
  };

  return (
    <span className={`skill-badge skill-badge--${variant}`}>
      <span className="skill-badge__icon">{icons[variant] || '•'}</span>
      {name}
    </span>
  );
}
