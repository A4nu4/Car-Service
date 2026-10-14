import { Link } from "react-router-dom";

interface AuthActionsProps {
  variant?: "dark" | "light";
  onNavigate?: () => void;
}

export function AuthActions({
  variant = "light",
  onNavigate,
}: AuthActionsProps) {
  const ghost = variant === "dark" ? "btn--ghost-on-dark" : "btn--ghost";

  return (
    <>
      <Link to="/login" className={`btn ${ghost}`} onClick={onNavigate}>
        Log in
      </Link>
      <Link to="/signup" className="btn" onClick={onNavigate}>
        Sign Up
      </Link>
    </>
  );
}
