import "./BookingSummary.css";

import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { TextField } from "@/components/ui/Field";
import { useQuote } from "@/hooks/useQuote";
import type { Car } from "@/types/car";
import { formatPrice } from "@/utils/formatPrice";
import { getRentalDays } from "@/utils/rental";

interface BookingSummaryProps {
  car: Car;
  pickup: string;
  dropoff: string;
  onDatesChange: (pickup: string, dropoff: string) => void;
}

export function BookingSummary({
  car,
  pickup,
  dropoff,
  onDatesChange,
}: BookingSummaryProps) {
  const days = getRentalDays(pickup, dropoff);
  const quote = useQuote(car.id, pickup, dropoff);

  function renderPrice() {
    if (days === null) {
      return (
        <p className="muted booking__message">
          {pickup && dropoff
            ? "The return date must be after pick-up."
            : "Choose pick-up and return dates to see the price."}
        </p>
      );
    }

    if (quote.isPending) {
      return (
        <p className="muted booking__message" role="status">
          Calculating price…
        </p>
      );
    }

    if (quote.isError) {
      return (
        <div className="booking__message">
          <p>Couldn't calculate the price.</p>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              void quote.refetch();
            }}
          >
            Try again
          </Button>
        </div>
      );
    }

    const { available, currency, dailyRate, rentalDays, totalPrice } =
      quote.data;

    if (!available) {
      return (
        <p className="booking__message">
          This car isn't available for the selected dates.
        </p>
      );
    }

    const subtotal = rentalDays * dailyRate;
    const other = totalPrice - subtotal;

    return (
      <dl className="booking__breakdown">
        <div className="booking__row">
          <dt>{`${rentalDays} ${rentalDays === 1 ? "day" : "days"} × ${formatPrice(dailyRate, currency)}`}</dt>
          <dd>{formatPrice(subtotal, currency)}</dd>
        </div>
        {other >= 0.5 ? (
          <div className="booking__row">
            <dt>Other charges</dt>
            <dd>{formatPrice(other, currency)}</dd>
          </div>
        ) : null}
        <div className="booking__row booking__row--total">
          <dt>Total</dt>
          <dd>{formatPrice(totalPrice, currency)}</dd>
        </div>
      </dl>
    );
  }

  return (
    <Card as="aside" className="booking" aria-labelledby="booking-title">
      <h2 id="booking-title" className="booking__title">
        Your booking
      </h2>

      <p className="muted">{`Pick-up: ${car.location.name}`}</p>

      <TextField
        label="Pick-up"
        type="datetime-local"
        value={pickup}
        onChange={(event) => {
          onDatesChange(event.target.value, dropoff);
        }}
      />
      <TextField
        label="Return"
        type="datetime-local"
        min={pickup}
        value={dropoff}
        onChange={(event) => {
          onDatesChange(pickup, event.target.value);
        }}
      />

      {renderPrice()}

      <Button block disabled>
        Reserve now
      </Button>
      <p className="muted booking__note">
        Online reservations aren't available yet.
      </p>
    </Card>
  );
}
