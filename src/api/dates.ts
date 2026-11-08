/**
 * Converts a datetime-local value ("2026-12-01T09:00") or any ISO string
 * to the UTC ISO format the API expects ("2026-12-01T08:00:00.000Z").
 * Throws RangeError on an invalid date, so validate form input first.
 */
export function toUtcIso(value: string): string {
  return new Date(value).toISOString();
}
