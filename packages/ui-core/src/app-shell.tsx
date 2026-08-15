import type { ReactNode } from "react";
import { LanguageSwitcher } from "../../../apps/web/components/language-switcher";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="nx-app-shell">
      <a className="nx-skip-link" href="#main-content">Skip to content</a>
      <div className="nx-app-bar">
        <LanguageSwitcher />
      </div>
      {children}
    </div>
  );
}

export function PageGrid({ children }: { children: ReactNode }) {
  return <div className="nx-page-grid">{children}</div>;
}
