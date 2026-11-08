import type { QuoteDto } from "@/api/dto";
import type { Quote } from "@/types/quote";

export function toQuote(dto: QuoteDto): Quote {
  return {
    carId: dto.carId,
    pickupAt: dto.pickupAt,
    dropoffAt: dto.dropoffAt,
    available: dto.available,
    currency: dto.currency,
    dailyRate: dto.dailyRate,
    rentalDays: dto.rentalDays,
    totalPrice: dto.totalPrice,
  };
}
