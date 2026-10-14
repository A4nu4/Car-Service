export interface NavItem {
  readonly to: string;
  readonly label: string;
}

export const NAV_LINKS: readonly NavItem[] = [
  { to: "/cars", label: "Cars" },
  { to: "/locations", label: "Locations" },
  { to: "/how-it-works", label: "How it works" },
  { to: "/help", label: "Help" },
];
