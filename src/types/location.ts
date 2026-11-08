export interface LocationSummary {
  id: string;
  name: string;
  city: string;
}

export interface Location {
  id: string;
  name: string;
  addressLine: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  openingHours: string;
  active: boolean;
}
