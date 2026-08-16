import type { Coordinates, VisibilityAssessment } from "../types/prediction";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export class PredictionApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.status = status;
    this.name = "PredictionApiError";
  }
}

export async function fetchPredictions(
  coordinates: Coordinates
): Promise<VisibilityAssessment> {
  const response = await fetch(`${API_BASE_URL}/predictions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(coordinates),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new PredictionApiError(
      body?.messages?.join(", ") ?? `Request failed with status ${response.status}`,
      response.status
    );
  }

  return response.json();
}