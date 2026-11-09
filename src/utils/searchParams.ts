import { isCarCategory } from "@/data/labels";
import type { SearchValues } from "@/types/search";

export const EMPTY_SEARCH: SearchValues = {
  locationId: "",
  pickup: "",
  dropoff: "",
  category: "any",
};

export function toSearchParams(values: Partial<SearchValues>): URLSearchParams {
  const params = new URLSearchParams();
  if (values.locationId) {
    params.set("locationId", values.locationId);
  }
  if (values.pickup) {
    params.set("pickup", values.pickup);
  }
  if (values.dropoff) {
    params.set("dropoff", values.dropoff);
  }
  if (values.category && values.category !== "any") {
    params.set("type", values.category);
  }
  return params;
}

export function fromSearchParams(params: URLSearchParams): SearchValues {
  const type = params.get("type") ?? "";
  return {
    locationId: params.get("locationId") ?? "",
    pickup: params.get("pickup") ?? "",
    dropoff: params.get("dropoff") ?? "",
    category: isCarCategory(type) ? type : "any",
  };
}
