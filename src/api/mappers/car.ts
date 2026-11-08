import type { CarDto } from "@/api/dto";
import type { Car } from "@/types/car";
import {
  categoryFromApi,
  fuelFromApi,
  transmissionFromApi,
} from "@/api/mappers/enums";
import { toLocationSummary } from "./location";

export function toCar(dto: CarDto): Car {
  return {
    id: dto.id,
    name: dto.name,
    brand: dto.brand,
    model: dto.model,
    year: dto.year,
    category: categoryFromApi(dto.category),
    transmission: transmissionFromApi(dto.transmission),
    fuelType: fuelFromApi(dto.fuelType),
    seats: dto.seats,
    pricePerDay: dto.pricePerDay,
    imageUrl: dto.imageUrl,
    location: toLocationSummary(dto.location),
  };
}
