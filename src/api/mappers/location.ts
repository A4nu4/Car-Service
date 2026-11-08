import type { LocationDto, LocationSummaryDto } from "@/api/dto";
import type { Location, LocationSummary } from "@/types/location";

export function toLocationSummary(dto: LocationSummaryDto): LocationSummary {
  return { id: dto.id, name: dto.name, city: dto.city };
}

export function toLocation(dto: LocationDto): Location {
  return {
    id: dto.id,
    name: dto.name,
    addressLine: dto.addressLine,
    city: dto.city,
    postalCode: dto.postalCode,
    country: dto.country,
    phone: dto.phone,
    openingHours: dto.openingHours,
    active: dto.active,
  };
}
