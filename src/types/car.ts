import type { LocationSummary } from "@/types/location";

export type CarCategory = "economy" | "suv" | "electric" | "van";
export type Transmission = "automatic" | "manual";
export type FuelType = "petrol" | "diesel" | "hybrid" | "electric";

export type CategoryFilter = CarCategory | "all";

export interface Car {
  id: string;
  name: string;
  brand: string;
  model: string;
  year: number;
  category: CarCategory;
  transmission: Transmission;
  fuelType: FuelType;
  seats: number;
  pricePerDay: number;
  imageUrl: string;
  location: LocationSummary;
}
