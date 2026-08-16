export function EarthPreviewFallback() {
  return (
    <svg width="100%" viewBox="0 0 220 220" role="img" aria-label="Simplified globe showing current ISS ground track">
      <circle cx="110" cy="110" r="95" fill="none" stroke="#3a2b52" strokeWidth="1" strokeDasharray="2 4" />
      <circle cx="110" cy="110" r="80" fill="#1c2140" stroke="#4a3d6b" strokeWidth="1" />
      <ellipse cx="110" cy="110" rx="80" ry="30" fill="none" stroke="#2e2a52" strokeWidth="0.75" />
      <ellipse cx="110" cy="110" rx="80" ry="60" fill="none" stroke="#2e2a52" strokeWidth="0.75" />
      <line x1="30" y1="110" x2="190" y2="110" stroke="#2e2a52" strokeWidth="0.75" />
      <path d="M 75 80 Q 95 68 115 78 Q 130 86 122 100 Q 105 108 85 100 Q 72 92 75 80 Z" fill="#2f5942" opacity="0.6" />
      <path d="M 100 130 Q 130 122 150 138 Q 145 155 122 152 Q 105 148 100 130 Z" fill="#2f5942" opacity="0.6" />
      <path d="M 34 96 Q 50 64 90 55 Q 130 50 160 68" fill="none" stroke="#d8ecff" strokeWidth="1" strokeDasharray="1 5" opacity="0.7" />
      <circle cx="160" cy="68" r="4" fill="#d8ecff" />
      <circle cx="160" cy="68" r="8" fill="none" stroke="#d8ecff" strokeWidth="1" opacity="0.4" />
    </svg>
  );
}