interface AlertBannerProps {
  alerts: string[];
  error: string | null;
}

export function AlertBanner({ alerts, error }: AlertBannerProps) {
  if (!error && alerts.length === 0) return null;

  return (
    <div role="alert" className="alert-banner">
      {error && <p className="alert-banner__error">{error}</p>}
      {alerts.map((message) => (
        <p key={message} className="alert-banner__message">
          {message}
        </p>
      ))}
    </div>
  );
}