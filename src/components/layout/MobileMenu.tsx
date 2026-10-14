import React, { useEffect, useRef } from "react";
import { CloseIcon } from "@/components/ui/icons";
import { NavLink } from "react-router-dom";
import type { NavItem } from "./navLinks";
import { AuthActions } from "./AuthActions";
import "./MobileMenu.css";

interface MobileMenuProps {
  open: boolean;
  onClose: () => void;
  links: readonly NavItem[];
}

export function MobileMenu({ open, onClose, links }: MobileMenuProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      className="mobile-menu"
      aria-label="Main menu"
      onClose={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div className="mobile-menu__header">
        <span className="navbar__logo">Kerbside</span>
        <button
          type="button"
          className="icon-button"
          aria-label="Close menu"
          onClick={onClose}
        >
          <CloseIcon />
        </button>
      </div>

      <nav aria-label="Mobile">
        <ul className="mobile-menu__list">
          {links.map(({ to, label }) => (
            <li key={to}>
              <NavLink to={to} className="mobile-menu__link" onClick={onClose}>
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="mobile-menu__actions">
        <AuthActions onNavigate={onClose} />
      </div>
    </dialog>
  );
}
