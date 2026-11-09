import { useQuery } from "@tanstack/react-query";

import { fetchLocations } from "@/api/location";

export function useLocations() {
  return useQuery({
    queryKey: ["locations"],
    queryFn: () => fetchLocations(),
    staleTime: 5 * 60 * 1000,
  });
}
