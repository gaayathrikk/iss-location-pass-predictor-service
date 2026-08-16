// interface OfflineToggleProps {
//   isOffline: boolean;
//   onToggle: () => void;
// }

// export function OfflineToggle({ isOffline, onToggle }: OfflineToggleProps) {
//   return (
//     <label className="offline-toggle">
//       <input type="checkbox" checked={isOffline} onChange={onToggle} />
//       Offline mode {isOffline && "(showing mock data)"}
//     </label>
//   );
// }

interface OfflineToggleProps {
  isOffline: boolean;
  onToggle: () => void;
}

export function OfflineToggle({ isOffline, onToggle }: OfflineToggleProps) {
  return (
    <label className="offline-toggle">
      <input type="checkbox" checked={isOffline} onChange={onToggle} />
      <span className="offline-toggle__track">
        <span className="offline-toggle__thumb" />
      </span>
      Offline mode
    </label>
  );
}