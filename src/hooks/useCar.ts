import { useQuery } from "@tanstack/react-query";

import { fetchCarById } from "@/api/cars";

export function useCar(id: string) {
  return useQuery({
    queryKey: ["car", id],
    queryFn: () => fetchCarById(id),
  });
}
