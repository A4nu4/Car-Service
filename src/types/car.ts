export type CarCategory = "economy" | "suv" | "electric" | "van";
export type Transmission = "automatic" | "manual";
export type CategoryFilter = CarCategory | "all";

export interface Car {
  id: string;
  name: string;
  category: CarCategory;
  transmission: Transmission;
  seats: number;
  pricePerDay: number;
  imageUrl?: string;
}

export interface FilterOption {
  value: CategoryFilter;
  label: string;
}
