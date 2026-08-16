import { useState } from "react";
import { usePredictions } from "./hooks/usePredictions";
import { EarthPreview } from "./components/EarthPreview";
import { PositionPanel } from "./components/PositionPanel";
import { PassesPanel } from "./components/PassesPanel";
import { LocationForm } from "./components/LocationForm";
import { OfflineToggle } from "./components/OfflineToggle";
import { AlertBanner } from "./components/AlertBanner";
import "./styles/tokens.css";
import "./styles/components.css";
import "./styles/layout.css";

function App() {
  const { data, isLoading, error, isOffline, toggleOffline, fetchAt } = usePredictions();
  const [hasSubmitted, setHasSubmitted] = useState(false);
  const [lastCoordinates, setLastCoordinates] = useState<{ latitude: number; longitude: number } | null>(null);

  function handleSubmit(coordinates: { latitude: number; longitude: number }) {
    setHasSubmitted(true);
    setLastCoordinates(coordinates);
    fetchAt(coordinates);
  }

  return (
    <div className="layout-app">
      <header className="layout-header">
        <span className="layout-title">ISS pass predictor</span>
        <OfflineToggle isOffline={isOffline} onToggle={toggleOffline} />
      </header>

      <div className="layout-grid">
        <div className="layout-left">
          <LocationForm onSubmit={handleSubmit} isLoading={isLoading} />
          <PositionPanel position={data?.currentPosition ?? null} />
          <PassesPanel passes={data?.upcomingPasses ?? null} />
        </div>

        <div className="layout-right">
          <EarthPreview issPosition={data?.currentPosition} userLocation={lastCoordinates} />
        </div>
      </div>

      {!data && !isLoading && !hasSubmitted && (
        <p className="layout-empty-hint">Enter a location to find when the ISS will be overhead.</p>
      )}

      {(error || (data && data.alerts.length > 0)) && (
        <div className="layout-alert">
          <AlertBanner alerts={data?.alerts ?? []} error={error} />
        </div>
      )}
    </div>
  );
}

export default App;