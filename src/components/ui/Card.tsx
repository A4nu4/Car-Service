import type { HTMLAttributes } from "react";
import { cx } from "@/utils/cx";
import "./Card.css";

type CardElement = "div" | "article" | "section" | "aside";

interface CardProps extends HTMLAttributes<HTMLElement> {
  as?: CardElement;
}

export function Card({ as: Tag = "div", className, ...rest }: CardProps) {
  return <Tag className={cx("card", className)} {...rest} />;
}

export function CardBody({
  className,
  ...rest
}: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx("card__body", className)} {...rest} />;
}

interface CardMediaProps {
  src?: string;
  alt: string;
  placeholder?: string;
  className?: string;
}

export function CardMedia({
  src,
  alt,
  placeholder = "[PHOTO]",
  className,
}: CardMediaProps) {
  if (src) {
    return (
      <img
        className={cx("media", className)}
        src={src}
        alt={alt}
        loading="lazy"
      />
    );
  }

  return (
    <div className={cx("media", className)} aria-hidden="true">
      {placeholder}
    </div>
  );
}
