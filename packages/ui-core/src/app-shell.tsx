import type { ReactNode } from "react";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="nx-app-shell">
      <a className="nx-skip-link" href="#main-content">Skip to content</a>
      {children}
    </div>
  );
}

export function PageGrid({ children }: { children: ReactNode }) {
  return <div className="nx-page-grid">{children}</div>;
}
