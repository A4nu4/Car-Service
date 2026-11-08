import type { CarCategory, FuelType, Transmission } from "@/types/car";
import type { CarCategoryDto, FuelTypeDto, TransmissionDto } from "@/api/dto";

const CATEGORY_FROM_API: Record<CarCategoryDto, CarCategory> = {
  ECONOMY: "economy",
  SUV: "suv",
  ELECTRIC: "electric",
  VAN: "van",
};

const TRANSMISSION_FROM_API: Record<TransmissionDto, Transmission> = {
  AUTOMATIC: "automatic",
  MANUAL: "manual",
};

const FUEL_FROM_API: Record<FuelTypeDto, FuelType> = {
  PETROL: "petrol",
  DIESEL: "diesel",
  HYBRID: "hybrid",
  ELECTRIC: "electric",
};

function invert<K extends string, V extends string>(
  map: Record<K, V>,
): Record<V, K> {
  return Object.fromEntries(
    Object.entries(map).map(([k, v]) => [v, k]),
  ) as Record<V, K>;
}

function lookup<K extends string, V>(
  map: Record<K, V>,
  key: K,
  field: string,
): V {
  const value = map[key] as V | undefined;
  if (value === undefined) {
    throw new Error(`Unknown ${field} from API: "${key}"`);
  }
  return value;
}

export const categoryFromApi = (v: CarCategoryDto) =>
  lookup(CATEGORY_FROM_API, v, "category");
export const transmissionFromApi = (v: TransmissionDto) =>
  lookup(TRANSMISSION_FROM_API, v, "transmission");
export const fuelFromApi = (v: FuelTypeDto) =>
  lookup(FUEL_FROM_API, v, "fuelType");

export const CATEGORY_TO_API = invert(CATEGORY_FROM_API);
export const TRANSMISSION_TO_API = invert(TRANSMISSION_FROM_API);
