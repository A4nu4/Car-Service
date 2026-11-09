import "./CarDetailPage.css";

import { Link, useParams, useSearchParams } from "react-router-dom";

import { BookingSummary } from "@/components/cars/BookingSummary";
import { type Spec, SpecList } from "@/components/cars/SpecList";
import { Button, ButtonLink } from "@/components/ui/Button";
import { CardMedia } from "@/components/ui/Card";
import { CATEGORY_LABELS, TRANSMISSION_LABELS } from "@/data/labels";
import { useCar } from "@/hooks/useCar";
import { fromSearchParams, toSearchParams } from "@/utils/searchParams";

export function CarDetailPage() {
  const { id = "" } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const search = fromSearchParams(searchParams);
  const { data: car, isPending, isError, refetch } = useCar(id);

  const backTo = { pathname: "/cars", search: searchParams.toString() };

  function handleDatesChange(pickup: string, dropoff: string) {
    setSearchParams(toSearchParams({ ...search, pickup, dropoff }), {
      replace: true,
    });
  }

  if (isPending) {
    return (
      <div className="container detail__status">
        <title>Car – Kerbside</title>
        <p className="muted" role="status">
          Loading car…
        </p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="container detail__status">
        <title>Car – Kerbside</title>
        <div className="detail__message">
          <h1>Couldn't load this car</h1>
          <p className="muted">Check that the API is running and try again.</p>
          <Button
            onClick={() => {
              void refetch();
            }}
          >
            Try again
          </Button>
        </div>
      </div>
    );
  }

  if (!car) {
    return (
      <div className="container detail__status">
        <title>Car not found – Kerbside</title>
        <div className="detail__message">
          <h1>Car not found</h1>
          <p className="muted">
            This car doesn't exist or is no longer listed.
          </p>
          <ButtonLink to={backTo}>Back to results</ButtonLink>
        </div>
      </div>
    );
  }

  const specs: Spec[] = [
    { label: "Seats", value: String(car.seats) },
    { label: "Transmission", value: TRANSMISSION_LABELS[car.transmission] },
    ...(car.fuelType ? [{ label: "Fuel", value: car.fuelType }] : []),
    ...(car.year ? [{ label: "Year", value: String(car.year) }] : []),
  ];

  return (
    <>
      <title>{`${car.name} – Kerbside`}</title>

      <div className="container detail">
        <article className="detail__main">
          <Link className="detail__back" to={backTo}>
            ← Back to results
          </Link>
          <h1 className="detail__title">{car.name}</h1>
          <p className="muted">{CATEGORY_LABELS[car.category]}</p>

          <CardMedia
            className="detail__media"
            src={car.imageUrl}
            alt={car.name}
            placeholder="[CAR PHOTO]"
          />

          <SpecList specs={specs} />
        </article>

        <BookingSummary
          car={car}
          pickup={search.pickup}
          dropoff={search.dropoff}
          onDatesChange={handleDatesChange}
        />
      </div>
    </>
  );
}
