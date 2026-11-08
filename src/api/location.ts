import { apiFetch } from "@/api/client";
import type { LocationDto } from "@/api/dto";
import { toLocation } from "@/api/mappers/location";
import type { Location } from "@/types/location";

export async function fetchLocations(): Promise<Location[]> {
  const dtos = await apiFetch<LocationDto[]>("/api/v1/locations");
  return dtos.map(toLocation);
}
