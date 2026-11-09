import type { CarCategory, FilterOption, Transmission } from "@/types/car";

export const CATEGORY_LABELS: Record<CarCategory, string> = {
  economy: "Economy",
  suv: "SUV",
  electric: "Electric",
  van: "Van",
};

export const TRANSMISSION_LABELS: Record<Transmission, string> = {
  automatic: "Automatic",
  manual: "Manual",
};

export const CATEGORY_FILTERS: readonly FilterOption[] = [
  { value: "all", label: "All" },
  { value: "economy", label: CATEGORY_LABELS.economy },
  { value: "suv", label: CATEGORY_LABELS.suv },
  { value: "electric", label: CATEGORY_LABELS.electric },
  { value: "van", label: CATEGORY_LABELS.van },
];

export function isCarCategory(value: string): value is CarCategory {
  return Object.hasOwn(CATEGORY_LABELS, value);
}

export function isTransmission(value: string): value is Transmission {
  return Object.hasOwn(TRANSMISSION_LABELS, value);
}
