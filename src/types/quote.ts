export interface Quote {
  carId: string;
  pickupAt: string;
  dropoffAt: string;
  available: boolean;
  currency: string;
  dailyRate: number;
  rentalDays: number;
  totalPrice: number;
}
