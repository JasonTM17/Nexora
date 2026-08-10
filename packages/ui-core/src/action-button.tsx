"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";

export type ActionButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode;
  loading?: boolean;
  tone?: "primary" | "secondary" | "tertiary";
};

export function ActionButton({
  children,
  className = "",
  disabled,
  loading = false,
  tone = "primary",
  type = "button",
  ...props
}: ActionButtonProps) {
  return (
    <button
      {...props}
      aria-busy={loading || undefined}
      className={`nx-action-button nx-action-button--${tone} ${className}`}
      disabled={disabled || loading}
      type={type}
    >
      {loading ? "Working…" : children}
    </button>
  );
}
