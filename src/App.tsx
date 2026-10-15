import { Route, Routes } from "react-router-dom";
import { RootLayout } from "@/components/layout/RootLayout";
import { HomePage } from "./pages/HomePage";

function Placeholder({ title }: { title: string }) {
  return (
    <main className="container" style={{ paddingBlock: "2rem" }}>
      <h1>{title}</h1>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<RootLayout />}>
        <Route index element={<HomePage />} />
        <Route path="cars" element={<Placeholder title="Cars" />} />
        <Route path="locations" element={<Placeholder title="Locations" />} />
        <Route
          path="how-it-works"
          element={<Placeholder title="How it works" />}
        />
        <Route path="help" element={<Placeholder title="Help" />} />
        <Route path="login" element={<Placeholder title="Log in" />} />
        <Route path="signup" element={<Placeholder title="Sign up" />} />
        <Route path="*" element={<Placeholder title="Page not found" />} />
      </Route>
    </Routes>
  );
}
