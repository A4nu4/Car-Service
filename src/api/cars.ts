import { apiFetch } from "@/api/client";
import { toUtcIso } from "@/api/dates";
import type { CarDto, PageDto, QuoteDto } from "@/api/dto";
import { toCar } from "@/api/mappers/car";
import { CATEGORY_TO_API } from "@/api/mappers/enums";
import { toPage } from "@/api/mappers/page";
import { toQuote } from "@/api/mappers/quote";
import type { Car, CarCategory } from "@/types/car";
import type { Page } from "@/types/page";
import type { Quote } from "@/types/quote";

export interface CarSearchParams {
  locationId?: string;
  pickupAt?: string;
  dropoffAt?: string;
  category?: CarCategory;
  page?: number;
  size?: number;
}

export async function fetchCarsPage(
  params: CarSearchParams = {},
): Promise<Page<Car>> {
  const dto = await apiFetch<PageDto<CarDto>>("/api/v1/cars", {
    locationId: params.locationId,
    pickupAt: params.pickupAt && toUtcIso(params.pickupAt),
    dropoffAt: params.dropoffAt && toUtcIso(params.dropoffAt),
    category: params.category && CATEGORY_TO_API[params.category],
    page: params.page?.toString(),
    size: params.size?.toString(),
  });
  return toPage(dto, toCar);
}

export async function fetchCars(params: CarSearchParams = {}): Promise<Car[]> {
  const page = await fetchCarsPage(params);
  return page.items;
}

export async function fetchCarById(id: string): Promise<Car> {
  const dto = await apiFetch<CarDto>(`/api/v1/cars/${encodeURIComponent(id)}`);
  return toCar(dto);
}

export async function fetchQuote(
  carId: string,
  pickupAt: string,
  dropoffAt: string,
): Promise<Quote> {
  const dto = await apiFetch<QuoteDto>(
    `/api/v1/cars/${encodeURIComponent(carId)}/quote`,
    { pickupAt: toUtcIso(pickupAt), dropoffAt: toUtcIso(dropoffAt) },
  );
  return toQuote(dto);
}
