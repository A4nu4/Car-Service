export type CarCategoryDto = "ECONOMY" | "SUV" | "ELECTRIC" | "VAN";
export type TransmissionDto = "AUTOMATIC" | "MANUAL";
export type FuelTypeDto = "PETROL" | "DIESEL" | "HYBRID" | "ELECTRIC";

export interface LocationSummaryDto {
  id: string;
  name: string;
  city: string;
}

export interface LocationDto {
  id: string;
  name: string;
  addressLine: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  openingHours: string;
  active: boolean;
}

export interface CarDto {
  id: string;
  name: string;
  brand: string;
  model: string;
  year: number;
  category: CarCategoryDto;
  transmission: TransmissionDto;
  fuelType: FuelTypeDto;
  seats: number;
  pricePerDay: number;
  imageUrl: string;
  location: LocationSummaryDto;
}

export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface QuoteDto {
  carId: string;
  pickupAt: string;
  dropoffAt: string;
  available: boolean;
  currency: string;
  dailyRate: number;
  rentalDays: number;
  totalPrice: number;
}
