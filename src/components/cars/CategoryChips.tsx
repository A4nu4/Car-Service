import { Button } from "@/components/ui/Button";
import type { CategoryFilter, FilterOption } from "@/types/car";
import "./CategoryChips.css";

interface CategoryChipsProps {
  label: string;
  options: readonly FilterOption[];
  value: CategoryFilter;
  onChange: (value: CategoryFilter) => void;
}

export function CategoryChips({
  label,
  options,
  value,
  onChange,
}: CategoryChipsProps) {
  return (
    <div className="chips" role="group" aria-label={label}>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Button
            key={option.value}
            size="sm"
            variant={selected ? "dark" : "ghost"}
            aria-pressed={selected}
            onClick={() => {
              onChange(option.value);
            }}
          >
            {option.label}
          </Button>
        );
      })}
    </div>
  );
}
