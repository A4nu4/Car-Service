export function formatEnum(value: string): string {
  const lower = value.replaceAll("_", " ").toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}
