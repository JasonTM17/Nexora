"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { AccessContextResponse } from "../../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { useI18n } from "../../lib/i18n";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

interface NotificationView {
  id: string;
  notificationType: string;
  priority: string;
  title: string;
  body: string;
  actionUrl: string | null;
  readAt: string | null;
  createdAt: string;
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not load notifications." };
}

export default function NotificationsPage() {
  const { t } = useI18n();
  const [organizationId, setOrganizationId] = useState("");
  const [notifications, setNotifications] = useState<ReadonlyArray<NotificationView>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = asProblem(error);
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, []);

  const load = useCallback(async () => {
    setState("loading"); setProblem(null);
    try {
      const access = await readJson<AccessContextResponse>("/api/bff/access-context");
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) { setNotifications([]); setState("empty"); return; }
      const result = await readJson<ReadonlyArray<NotificationView>>(`/api/bff/notifications?organizationId=${encodeURIComponent(selected)}`);
      setNotifications(result); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  async function markRead(id: string) {
    try {
      await readJson("/api/bff/notifications", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ organizationId, notificationId: id }),
      });
      await load();
    } catch (error) { setFailure(error); }
  }

  const unreadCount = notifications.filter((n) => !n.readAt).length;

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">{t("notifications.eyebrow")}</p>
    <h1 ref={heading} tabIndex={-1}>{t("notifications.title")}</h1>
    <p className="nx-lede">{t("notifications.unread", { count: unreadCount })}</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> {t("common.loading")}</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>{t("account.selectOrganization")}</h2></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void load()}>{t("common.retry")}</ActionButton></section>}

    {state === "ready" && <section className="nx-access-card">
      <div className="nx-card-heading"><div><h2>{t("notifications.inbox")}</h2><p className="nx-field-help">{notifications.length} {t("notifications.inbox").toLowerCase()}</p></div><StatusLabel kind="fixture" /></div>
      {notifications.length === 0 ? <p className="nx-empty-copy">{t("notifications.noNotifications")}</p> :
        <ul className="nx-notification-list">{notifications.map((n) => <li key={n.id} className={n.readAt ? "nx-notification--read" : "nx-notification--unread"}>
          <div className="nx-notification-header">
            <span className={`nx-badge nx-badge--${n.priority}`}>{t(`notifications.priority.${n.priority}`)}</span>
            <strong>{n.title}</strong>
            {!n.readAt && <span className="nx-unread-dot" aria-label={t("notifications.unreadDot")} />}
          </div>
          {n.body && <p className="nx-notification-body">{n.body}</p>}
          <div className="nx-notification-actions">
            {n.actionUrl && <a className="nx-action-button nx-action-button--secondary" href={n.actionUrl}>{t("notifications.open")}</a>}
            {!n.readAt && <ActionButton tone="secondary" onClick={() => void markRead(n.id)}>{t("notifications.markRead")}</ActionButton>}
          </div>
        </li>)}</ul>}
    </section>}
  </main></PageGrid></AppShell>;
}
