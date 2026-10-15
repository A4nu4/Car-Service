import { useId } from "react";
import type { InputHTMLAttributes, SelectHTMLAttributes } from "react";
import { cx } from "@/utils/cx";
import "./Field.css";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
}

export function TextField({ label, className, ...rest }: TextFieldProps) {
  const id = useId();

  return (
    <div className={cx("field", className)}>
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <input id={id} className="field__control" {...rest} />
    </div>
  );
}

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
}

export function SelectField({
  label,
  className,
  children,
  ...rest
}: SelectFieldProps) {
  const id = useId();

  return (
    <div className={cx("field", className)}>
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <select id={id} className="field__control" {...rest}>
        {children}
      </select>
    </div>
  );
}
