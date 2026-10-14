import { Outlet, useLocation } from "react-router-dom";
import { Navbar } from "./Navbar";

export function RootLayout() {
  const { pathname } = useLocation();

  return (
    <>
      <Navbar variant={pathname === "/" ? "dark" : "light"} />
      <Outlet />
    </>
  );
}
