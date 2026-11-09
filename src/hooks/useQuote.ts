import { useQuery } from "@tanstack/react-query";

import { fetchQuote } from "@/api/cars";
import { getRentalDays } from "@/utils/rental";

export function useQuote(carId: string, pickup: string, dropoff: string) {
  return useQuery({
    queryKey: ["quote", carId, pickup, dropoff],
    queryFn: () => fetchQuote(carId, pickup, dropoff),
    enabled: getRentalDays(pickup, dropoff) !== null,
  });
}
