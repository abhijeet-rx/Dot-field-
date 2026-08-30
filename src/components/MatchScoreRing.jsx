/**
 * SVG circular progress ring for match score display.
 * Color-coded by match category.
 */
export default function MatchScoreRing({ score = 0, category = '', size = 120 }) {
  const strokeWidth = 8;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (score / 100) * circumference;

  const colorMap = {
    STRONG_MATCH: '#10b981',
    GOOD_MATCH: '#00f2fe',
    MODERATE_MATCH: '#a855f7',
    WEAK_MATCH: '#f43f5e',
  };
  const strokeColor = colorMap[category] || '#a855f7';
  const glowFilter = `drop-shadow(0 0 8px ${strokeColor})`;

  const categoryLabels = {
    STRONG_MATCH: 'Strong Match',
    GOOD_MATCH: 'Good Match',
    MODERATE_MATCH: 'Moderate',
    WEAK_MATCH: 'Weak Match',
  };

  return (
    <div className="match-score-ring" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {/* Background track */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="rgba(255,255,255,0.06)"
          strokeWidth={strokeWidth}
        />
        {/* Progress arc */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={strokeColor}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{
            filter: glowFilter,
            transform: 'rotate(-90deg)',
            transformOrigin: '50% 50%',
            transition: 'stroke-dashoffset 0.8s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
        />
      </svg>
      <div className="match-score-ring__label">
        <span className="match-score-ring__value" style={{ color: strokeColor }}>
          {score}%
        </span>
        <span className="match-score-ring__category">
          {categoryLabels[category] || category}
        </span>
      </div>
    </div>
  );
}
