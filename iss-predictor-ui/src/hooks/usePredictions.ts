import { useState, useCallback } from "react";
import type { Coordinates, VisibilityAssessment } from "../types/prediction";
import { fetchPredictions, PredictionApiError } from "../api/predictions";
import { getMockAssessment } from "../api/mockData";

interface UsePredictionsState {
  data: VisibilityAssessment | null;
  isLoading: boolean;
  error: string | null;
}

export function usePredictions() {
  const [state, setState] = useState<UsePredictionsState>({
    data: null,
    isLoading: false,
    error: null,
  });
  const [isOffline, setIsOffline] = useState(false);

  const fetchAt = useCallback(
    async (coordinates: Coordinates) => {
      setState((prev) => ({ ...prev, isLoading: true, error: null }));

      if (isOffline) {
        // Simulate latency so the loading state is visible/testable,
        // and so switching modes doesn't feel like a different code path.
        await new Promise((resolve) => setTimeout(resolve, 300));
        setState({ data: getMockAssessment(coordinates), isLoading: false, error: null });
        return;
      }

      try {
        const result = await fetchPredictions(coordinates);
        setState({ data: result, isLoading: false, error: null });
      } catch (err) {
        const message =
          err instanceof PredictionApiError
            ? err.message
            : "Network error — could not reach the prediction service.";
        setState({ data: null, isLoading: false, error: message });
      }
    },
    [isOffline]
  );

  const toggleOffline = useCallback(() => {
    setIsOffline((prev) => !prev);
    setState({ data: null, isLoading: false, error: null });
  }, []);

  return {
    ...state,
    isOffline,
    toggleOffline,
    fetchAt,
  };
}