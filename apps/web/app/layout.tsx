import type { Metadata } from "next";
import "../../../packages/design-tokens/src/tokens.css";
import "./globals.css";
import { I18nProvider } from "../lib/i18n";

export const metadata: Metadata = {
  title: "Nexora",
  description: "Tenant-aware CMS and knowledge workspace",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <I18nProvider>{children}</I18nProvider>
      </body>
    </html>
  );
}
