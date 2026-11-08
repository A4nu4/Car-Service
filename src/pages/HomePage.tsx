import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CarCard } from "@/components/cars/CarCard";
import { CategoryChips } from "@/components/cars/CategoryChips";
import { SearchBar } from "@/components/cars/SearchBar";
import { CardMedia } from "@/components/ui/Card";
import { CATEGORY_FILTERS } from "@/data/labels";
import { useCars } from "@/hooks/useCars";
import type { CategoryFilter } from "@/types/car";
import type { SearchValues } from "@/types/search";
import "./HomePage.css";

export function HomePage() {
  const navigate = useNavigate();
  const [category, setCategory] = useState<CategoryFilter>("all");
  const { cars: allCars, isLoading, error } = useCars();

  const cars =
    category === "all"
      ? allCars
      : allCars.filter((car) => car.category === category);

  function handleSearch(values: SearchValues) {
    const params = new URLSearchParams({
      location: values.location,
      pickup: values.pickup,
      dropoff: values.dropoff,
    });
    if (values.category !== "any") {
      params.set("type", values.category);
    }
    void navigate({ pathname: "/cars", search: params.toString() });
  }

  return (
    <>
      <title>Kerbside - Car rental</title>

      <section className="hero">
        <div className="container">
          <div className="hero__grid">
            <div>
              <h1 className="hero__title">
                <span className="hero__title-line">Pick up in minutes.</span>
                <span className="hero__title-line">Drive for days.</span>
              </h1>

              <p className="hero__lead">
                Compare cars, choose your dates and book in three simple steps.
              </p>
            </div>
            <CardMedia
              className="hero__media"
              alt=""
              placeholder="[CAR PHOTO]"
            />
          </div>

          <div className="hero__search">
            <SearchBar onSearch={handleSearch} />
          </div>
        </div>
      </section>

      <section className="popular" aria-labelledby="popular-title">
        <div className="container">
          <div className="popular__chips">
            <CategoryChips
              label="Filter cars by type"
              options={CATEGORY_FILTERS}
              value={category}
              onChange={setCategory}
            />
          </div>

          <h2 id="popular-title" className="popular__title">
            Popular right now
          </h2>

          {isLoading && <p role="status">Loading cars...</p>}

          {error && <p role="alert">Couldn't load cars. Please try again.</p>}

          {!isLoading && !error && cars.length === 0 && (
            <p>No cars in this category.</p>
          )}

          {!isLoading && !error && cars.length > 0 && (
            <ul className="popular__grid">
              {cars.map((car) => (
                <li key={car.id}>
                  <CarCard car={car} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </>
  );
}
