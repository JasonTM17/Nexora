import type { ReactNode } from "react";

export function InformationCard({
  children,
  title,
  action,
}: {
  children: ReactNode;
  title: string;
  action?: ReactNode;
}) {
  return (
    <section className="nx-information-card" aria-labelledby={`card-${title.replaceAll(" ", "-").toLowerCase()}`}>
      <div className="nx-card-heading">
        <h2 id={`card-${title.replaceAll(" ", "-").toLowerCase()}`}>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}
