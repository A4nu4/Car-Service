import React, { useState } from "react";
import "./Navbar.css";
import assets from "@/assets/assets";
import { MenuIcon } from "../ui/icons";
import { Link, NavLink } from "react-router-dom";
import { NAV_LINKS } from "./navLinks";
import { AuthActions } from "./AuthActions";
import { MobileMenu } from "./MobileMenu";
import { IconButton } from "../ui/Button";

interface NavbarProps {
  variant?: "dark" | "light";
}

export function Navbar({ variant = "light" }: NavbarProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  return (
    <header className={`navbar navbar--${variant}`}>
      <Link to="/" className="navbar__logo">
        Kerbside
      </Link>

      <nav className="navbar__nav" aria-label="Main">
        <ul className="navbar__links">
          {NAV_LINKS.map(({ to, label }) => (
            <li key={to}>
              <NavLink to={to} className="navbar__link">
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="navbar__actions">
        <AuthActions variant={variant} />
      </div>

      <IconButton
        label="Open menu"
        className="navbar__menu-button"
        aria-haspopup="dialog"
        aria-expanded={isSidebarOpen}
        onClick={() => setIsSidebarOpen(true)}
      >
        <MenuIcon />
      </IconButton>

      <MobileMenu
        open={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        links={NAV_LINKS}
      />
    </header>
  );
}
