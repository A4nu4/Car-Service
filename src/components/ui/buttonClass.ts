export type ButtonVariant = "primary" | "ghost" | "dark" | "ghost-on-dark";
export type ButtonSize = "md" | "sm";

export interface ButtonStyleProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  block?: boolean;
}

export function buttonClass({
  variant = "primary",
  size = "md",
  block = false,
  className,
}: ButtonStyleProps & { className?: string }): string {
  return [
    "btn",
    variant !== "primary" && `btn--${variant}`,
    size === "sm" && "btn--sm",
    block && "btn--block",
    className,
  ]
    .filter(Boolean)
    .join(" ");
}
