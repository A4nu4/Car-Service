import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { fetchCars } from "@/api/cars";
import type { SearchValues } from "@/types/search";

type CarsSearch = Pick<SearchValues, "locationId" | "pickup" | "dropoff">;

export function useCars(search: CarsSearch) {
  return useQuery({
    queryKey: ["cars", search.locationId, search.pickup, search.dropoff],
    queryFn: () =>
      fetchCars({
        locationId: search.locationId || undefined,
        pickupAt: search.pickup || undefined,
        dropoffAt: search.dropoff || undefined,
      }),
    placeholderData: keepPreviousData,
  });
}
