import "./CarCard.css";

import { ButtonLink } from "@/components/ui/Button";
import { Card, CardBody, CardMedia } from "@/components/ui/Card";
import { CATEGORY_LABELS, TRANSMISSION_LABELS } from "@/data/labels";
import type { Car } from "@/types/car";
import { formatPrice } from "@/utils/formatPrice";

interface CarCardProps {
  car: Car;
}

export function CarCard({ car }: CarCardProps) {
  const meta = [
    CATEGORY_LABELS[car.category],
    TRANSMISSION_LABELS[car.transmission],
    `${car.seats} seats`,
  ].join(" · ");

  return (
    <Card as="article" className="car-card">
      <CardMedia
        className="car-card__media"
        src={car.imageUrl}
        alt={car.name}
        placeholder="[CAR PHOTO]"
      />

      <CardBody className="car-card__body">
        <h3 className="car-card__name">{car.name}</h3>
        <p className="muted">{meta}</p>

        <div className="car-card__footer">
          <p>
            <b className="car-card__price">{formatPrice(car.pricePerDay)}</b>{" "}
            <span className="muted">/ day</span>
          </p>
          <ButtonLink
            to={`/cars/${car.id}`}
            variant="ghost"
            size="sm"
            aria-label={`Select ${car.name}`}
          >
            Select
          </ButtonLink>
        </div>
      </CardBody>
    </Card>
  );
}
