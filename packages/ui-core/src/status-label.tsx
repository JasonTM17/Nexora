export type StatusKind = "fixture" | "planned" | "empty" | "error" | "denied" | "offline" | "loading";

const labels: Record<StatusKind, string> = {
  fixture: "Fixture data",
  planned: "Planned",
  empty: "No data",
  error: "Error",
  denied: "Access denied",
  offline: "Offline",
  loading: "Loading",
};

export function StatusLabel({ kind, children }: { kind: StatusKind; children?: string }) {
  return <span className={`nx-status-label nx-status-label--${kind}`}>{children ?? labels[kind]}</span>;
}
