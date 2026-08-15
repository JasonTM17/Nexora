"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { useI18n } from "../../../lib/i18n";
import { PERMISSION_LABELS, type Permission } from "../../../lib/permissions";

interface AccessContextWithPermissions {
  memberships: ReadonlyArray<{ organizationId: string; role: string }>;
  permissions?: string[];
  tenantSelectionRequired?: boolean;
}

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

export default function PermissionsPage() {
  const { t } = useI18n();
  const [organizationId, setOrganizationId] = useState("");
  const [permissions, setPermissions] = useState<ReadonlyArray<Permission>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: t("errors.REQUEST_FAILED") };
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, [t]);

  const load = useCallback(async () => {
    setState("loading"); setProblem(null);
    try {
      const access = await fetch("/api/bff/access-context", { credentials: "same-origin" }).then(r => r.json()) as AccessContextWithPermissions;
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) { setPermissions([]); setState("empty"); return; }
      setPermissions((access.permissions ?? []) as Permission[]);
      setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">{t("permissions.eyebrow")}</p>
    <h1 ref={heading} tabIndex={-1}>{t("permissions.title")}</h1>
    <p className="nx-lede">{t("permissions.lede")}</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> {t("common.loading")}</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>{t("account.selectOrganization")}</h2></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void load()}>{t("common.retry")}</ActionButton></section>}

    {state === "ready" && <section className="nx-access-card">
      <div className="nx-card-heading"><div><h2>{t("permissions.permission")}</h2><p className="nx-field-help">{permissions.length} {t("permissions.granted").toLowerCase()}</p></div><StatusLabel kind="fixture" /></div>
      {permissions.length === 0 ? <p className="nx-empty-copy">{t("permissions.noPermissions")}</p> :
        <ul className="nx-permission-list">{permissions.map((perm) => <li key={perm}>
          <code>{perm}</code>
          <span>{PERMISSION_LABELS[perm] ?? perm}</span>
          <span className="nx-badge nx-badge--on">{t("permissions.granted")}</span>
        </li>)}</ul>}
    </section>}
  </main></PageGrid></AppShell>;
}
