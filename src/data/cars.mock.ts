import type { Car } from "@/types/car";

export const CARS: readonly Car[] = [
  {
    id: "toyota-yaris",
    name: "Toyota Yaris",
    category: "economy",
    transmission: "automatic",
    seats: 5,
    pricePerDay: 34,
  },
  {
    id: "skoda-octavia",
    name: "Skoda Octavia",
    category: "economy",
    transmission: "manual",
    seats: 5,
    pricePerDay: 46,
  },
  {
    id: "vw-tiguan",
    name: "VW Tiguan",
    category: "suv",
    transmission: "automatic",
    seats: 5,
    pricePerDay: 59,
  },
  {
    id: "tesla-model-3",
    name: "Tesla Model 3",
    category: "electric",
    transmission: "automatic",
    seats: 5,
    pricePerDay: 79,
  },
  {
    id: "nissan-leaf",
    name: "Nissan Leaf",
    category: "electric",
    transmission: "automatic",
    seats: 5,
    pricePerDay: 52,
  },
  {
    id: "mercedes-vito",
    name: "Mercedes Vito",
    category: "van",
    transmission: "manual",
    seats: 9,
    pricePerDay: 89,
  },
];
