import "./SearchBar.css";

import { type FormEvent, useState } from "react";

import { Button } from "@/components/ui/Button";
import { SelectField, TextField } from "@/components/ui/Field";
import { CATEGORY_LABELS } from "@/data/labels";
import type { SearchValues } from "@/types/search";

interface SearchBarProps {
  initialValues?: Partial<SearchValues>;
  onSearch: (values: SearchValues) => void;
}

const EMPTY_VALUES: SearchValues = {
  locationId: "",
  pickup: "",
  dropoff: "",
  category: "any",
};

function isCategoryChoice(value: string): value is SearchValues["category"] {
  return value === "any" || Object.hasOwn(CATEGORY_LABELS, value);
}

export function SearchBar({ initialValues, onSearch }: SearchBarProps) {
  const [values, setValues] = useState<SearchValues>({
    ...EMPTY_VALUES,
    ...initialValues,
  });

  function update<K extends keyof SearchValues>(
    key: K,
    value: SearchValues[K],
  ) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSearch(values);
  }

  return (
    <form
      className="search-bar"
      aria-label="Search cars"
      onSubmit={handleSubmit}
    >
      <TextField
        label="Pick-up location"
        name="location"
        placeholder="City or airport"
        autoComplete="off"
        required
        value={values.locationId}
        onChange={(event) => {
          update("locationId", event.target.value);
        }}
      />
      <TextField
        label="Pick-up date"
        name="pickup"
        type="datetime-local"
        required
        value={values.pickup}
        onChange={(event) => {
          update("pickup", event.target.value);
        }}
      />
      <TextField
        label="Return date"
        name="dropoff"
        type="datetime-local"
        required
        min={values.pickup}
        value={values.dropoff}
        onChange={(event) => {
          update("dropoff", event.target.value);
        }}
      />
      <SelectField
        label="Car type"
        name="category"
        value={values.category}
        onChange={(event) => {
          const next = event.target.value;
          if (isCategoryChoice(next)) {
            update("category", next);
          }
        }}
      >
        <option value="any">Any type</option>
        {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </SelectField>
      <Button type="submit" className="search-bar__submit">
        Search cars
      </Button>
    </form>
  );
}
