import type { CarCategory } from "./car";

export interface SearchValues {
  locationId: string;
  /** datetime-local value, e.g. "2026-10-14T10:00" */
  pickup: string;
  dropoff: string;
  category: CarCategory | "any";
}
