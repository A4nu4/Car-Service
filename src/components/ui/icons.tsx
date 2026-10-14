interface IconProps {
  size?: number;
}

const svgProps = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  "aria-hidden": true,
} as const;

export function MenuIcon({ size = 22 }: IconProps) {
  return (
    <svg width={size} height={size} {...svgProps}>
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  );
}

export function CloseIcon({ size = 22 }: IconProps) {
  return (
    <svg width={size} height={size} {...svgProps}>
      <path d="M6 6l12 12M18 6L6 18" />
    </svg>
  );
}
