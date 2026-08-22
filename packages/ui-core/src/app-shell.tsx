import type { ReactNode } from "react";
import { LanguageSwitcher } from "../../../apps/web/components/language-switcher";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="nx-app-shell">
      <a className="nx-skip-link" href="#main-content">Skip to content</a>
      <div className="nx-app-bar" role="region" aria-label="Application preferences">
        <div className="nx-app-bar-inner">
          <span className="nx-app-bar-label">Local preview</span>
          <LanguageSwitcher />
        </div>
      </div>
      {children}
    </div>
  );
}

export function PageGrid({ children }: { children: ReactNode }) {
  return <div className="nx-page-grid">{children}</div>;
}
