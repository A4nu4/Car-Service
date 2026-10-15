import { ButtonLink } from "@/components/ui/Button";

interface AuthActionsProps {
  variant?: "dark" | "light";
  onNavigate?: () => void;
}

export function AuthActions({
  variant = "light",
  onNavigate,
}: AuthActionsProps) {
  return (
    <>
      <ButtonLink
        to="/login"
        variant={variant === "dark" ? "ghost-on-dark" : "ghost"}
        onClick={onNavigate}
      >
        Log in
      </ButtonLink>
      <ButtonLink to="/signup" onClick={onNavigate}>
        Sign up
      </ButtonLink>
    </>
  );
}
