import { fetchCars } from "@/api/cars";
import type { Car } from "@/types/car";
import { useEffect, useState } from "react";

export function useCars() {
  const [cars, setCars] = useState<Car[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    fetchCars()
      .then((data) => {
        if (!cancelled) setCars(data);
      })
      .catch((err: unknown) => {
        console.error("fetchCars failed:", err);
        if (!cancelled) {
          setError(
            err instanceof Error ? err : new Error("Failed to load cars"),
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);
  return { cars, isLoading, error };
}
