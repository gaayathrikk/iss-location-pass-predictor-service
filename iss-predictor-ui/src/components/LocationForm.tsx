// // import { useState } from "react";
// // import type { FormEvent } from "react";
// // import type { Coordinates } from "../types/prediction";

// // interface LocationFormProps {
// //   onSubmit: (coordinates: Coordinates) => void;
// //   isLoading: boolean;
// // }

// // export function LocationForm({ onSubmit, isLoading }: LocationFormProps) {
// //   const [latitude, setLatitude] = useState("");
// //   const [longitude, setLongitude] = useState("");
// //   const [geoError, setGeoError] = useState<string | null>(null);

// //   function handleSubmit(event: FormEvent) {
// //     event.preventDefault();
// //     const lat = parseFloat(latitude);
// //     const lon = parseFloat(longitude);
// //     if (Number.isNaN(lat) || Number.isNaN(lon)) return;
// //     onSubmit({ latitude: lat, longitude: lon });
// //   }

// //   function handleUseMyLocation() {
// //     if (!navigator.geolocation) {
// //       setGeoError("Geolocation isn't supported by this browser.");
// //       return;
// //     }
// //     setGeoError(null);
// //     navigator.geolocation.getCurrentPosition(
// //       (position) => {
// //         setLatitude(position.coords.latitude.toString());
// //         setLongitude(position.coords.longitude.toString());
// //       },
// //       (error) => setGeoError(error.message)
// //     );
// //   }

// //   return (
// //     <form onSubmit={handleSubmit}>
// //       <label>
// //         Latitude
// //         <input
// //           type="number"
// //           step="any"
// //           value={latitude}
// //           onChange={(e) => setLatitude(e.target.value)}
// //           required
// //         />
// //       </label>
// //       <label>
// //         Longitude
// //         <input
// //           type="number"
// //           step="any"
// //           value={longitude}
// //           onChange={(e) => setLongitude(e.target.value)}
// //           required
// //         />
// //       </label>
// //       <button type="button" onClick={handleUseMyLocation}>
// //         Use my location
// //       </button>
// //       {geoError && <p role="alert">{geoError}</p>}
// //       <button type="submit" disabled={isLoading}>
// //         {isLoading ? "Loading…" : "Get predictions"}
// //       </button>
// //     </form>
// //   );
// // }

// import { useState } from "react";
// import type { FormEvent } from "react";
// import type { Coordinates } from "../types/prediction";

// interface LocationFormProps {
//   onSubmit: (coordinates: Coordinates) => void;
//   isLoading: boolean;
// }

// export function LocationForm({ onSubmit, isLoading }: LocationFormProps) {
//   const [latitude, setLatitude] = useState("");
//   const [longitude, setLongitude] = useState("");
//   const [geoError, setGeoError] = useState<string | null>(null);

//   function handleSubmit(event: FormEvent) {
//     event.preventDefault();
//     const lat = parseFloat(latitude);
//     const lon = parseFloat(longitude);
//     if (Number.isNaN(lat) || Number.isNaN(lon)) return;
//     onSubmit({ latitude: lat, longitude: lon });
//   }

//   function handleUseMyLocation() {
//     if (!navigator.geolocation) {
//       setGeoError("Geolocation isn't supported by this browser.");
//       return;
//     }
//     setGeoError(null);
//     navigator.geolocation.getCurrentPosition(
//       (position) => {
//         setLatitude(position.coords.latitude.toFixed(4));
//         setLongitude(position.coords.longitude.toFixed(4));
//       },
//       (error) => setGeoError(error.message)
//     );
//   }

//   return (
//     <form className="location-form" onSubmit={handleSubmit}>
//       <div className="location-form__field">
//         <label htmlFor="lat">Latitude</label>
//         <input id="lat" type="number" step="any" value={latitude} onChange={(e) => setLatitude(e.target.value)} required />
//       </div>
//       <div className="location-form__field">
//         <label htmlFor="lon">Longitude</label>
//         <input id="lon" type="number" step="any" value={longitude} onChange={(e) => setLongitude(e.target.value)} required />
//       </div>
//       <button type="button" className="btn" onClick={handleUseMyLocation}>Use my location</button>
//       <button type="submit" className="btn btn--primary" disabled={isLoading}>
//         {isLoading ? "Loading…" : "Find passes"}
//       </button>
//       {geoError && <p role="alert" style={{ color: "#f0a3a3", fontSize: 12 }}>{geoError}</p>}
//     </form>
//   );
// }

import { useState } from "react";
import type { FormEvent } from "react";
import type { Coordinates } from "../types/prediction";

interface LocationFormProps {
  onSubmit: (coordinates: Coordinates) => void;
  isLoading: boolean;
}

export function LocationForm({ onSubmit, isLoading }: LocationFormProps) {
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [geoError, setGeoError] = useState<string | null>(null);
  const [isLocating, setIsLocating] = useState(false);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const lat = parseFloat(latitude);
    const lon = parseFloat(longitude);
    if (Number.isNaN(lat) || Number.isNaN(lon)) {
      setGeoError("Enter valid latitude and longitude values.");
      return;
    }
    setGeoError(null);
    onSubmit({ latitude: lat, longitude: lon });
  }

  function handleUseMyLocation() {
    if (!navigator.geolocation) {
      setGeoError("Geolocation isn't supported by this browser.");
      return;
    }
    setGeoError(null);
    setIsLocating(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude.toFixed(4));
        setLongitude(position.coords.longitude.toFixed(4));
        setIsLocating(false);
      },
      (error) => {
        setGeoError(error.message);
        setIsLocating(false);
      }
    );
  }

  return (
    <form className="location-form--stacked" onSubmit={handleSubmit}>
      <div className="location-form__field">
        <label htmlFor="lat">Latitude</label>
        <input id="lat" type="number" step="any" value={latitude} onChange={(e) => setLatitude(e.target.value)} required />
      </div>
      <div className="location-form__field">
        <label htmlFor="lon">Longitude</label>
        <input id="lon" type="number" step="any" value={longitude} onChange={(e) => setLongitude(e.target.value)} required />
      </div>
      <div className="location-form__actions">
        <button type="button" className="link-btn" onClick={handleUseMyLocation} disabled={isLocating}>
          {isLocating ? "Locating…" : "Use my location"}
        </button>
        <button type="submit" className="link-btn link-btn--primary" disabled={isLoading}>
          {isLoading ? "Loading…" : "Find passes"}
        </button>
      </div>
      {geoError && <p role="alert" style={{ color: "#f0a3a3", fontSize: 12, margin: 0 }}>{geoError}</p>}
    </form>
  );
}