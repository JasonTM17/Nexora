import type { Metadata } from "next";
import "../../../packages/design-tokens/src/tokens.css";
import "./globals.css";

export const metadata: Metadata = {
  title: "Nexora foundation",
  description: "A deterministic product foundation preview.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
