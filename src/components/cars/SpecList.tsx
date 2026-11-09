import "./SpecList.css";

import { Card } from "@/components/ui/Card";

export interface Spec {
  label: string;
  value: string;
}

interface SpecListProps {
  specs: readonly Spec[];
}

export function SpecList({ specs }: SpecListProps) {
  return (
    <dl className="spec-list">
      {specs.map((spec) => (
        <Card key={spec.label} className="spec">
          <dt className="spec__label">{spec.label}</dt>
          <dd className="spec__value">{spec.value}</dd>
        </Card>
      ))}
    </dl>
  );
}
