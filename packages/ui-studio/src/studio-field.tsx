"use client";

import { Alert, Input, Spin } from "antd";

export type StudioFieldState = "ready" | "loading" | "empty" | "error" | "denied";

export function StudioField({ label, state = "ready" }: { label: string; state?: StudioFieldState }) {
  if (state === "loading") return <div className="nx-studio-loading" role="status"><Spin aria-hidden="true" /> <span>{label} loading</span></div>;
  if (state === "error") return <Alert message={`${label} could not load`} type="error" showIcon />;
  if (state === "denied") return <Alert message={`Access denied: ${label}`} type="warning" showIcon />;
  if (state === "empty") return <p className="nx-empty-copy">No {label.toLowerCase()} is available.</p>;
  return <Input aria-label={label} placeholder={`Enter ${label.toLowerCase()}`} />;
}
