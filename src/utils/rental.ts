const MS_PER_DAY = 24 * 60 * 60 * 1000;

const shortDate = new Intl.DateTimeFormat("en-GB", {
  day: "numeric",
  month: "short",
});

function parseData(value: string): Date | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function getRentalDays(pickup: string, dropoff: string): number | null {
  const start = parseData(pickup);
  const end = parseData(dropoff);
  if (!start || !end || end.getTime() <= start.getTime()) {
    return null;
  }
  return Math.ceil((end.getTime() - start.getTime()) / MS_PER_DAY);
}

export function formatDateRange(pickup: string, dropoff: string): string {
  const start = parseData(pickup);
  const end = parseData(dropoff);
  if (!start || !end) {
    return "Any dates";
  }
  return `${shortDate.format(start)} - ${shortDate.format(end)}`;
}
