"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import type { AccessContextResponse, UpdateProfileRequest, UserProfile } from "../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";

type Problem = { code: string; message: string; traceId?: string | null };
type AccessState = "loading" | "ready" | "empty" | "selection" | "denied" | "session-expired" | "error" | "profile-conflict";

const stateCopy: Record<Exclude<AccessState, "ready" | "selection">, { title: string; body: string }> = {
  loading: { title: "Checking your access", body: "Nexora is confirming your current session and organization membership." },
  empty: { title: "Welcome to Nexora", body: "Your account is signed in, but it does not have an active organization yet." },
  denied: { title: "Access denied", body: "That organization is not available to your current account." },
  "session-expired": { title: "Session expired", body: "Sign in again to refresh your secure session. No authorization decision is made in the browser." },
  error: { title: "Nexora is unavailable", body: "We could not confirm access. Retry when the connection is available." },
  "profile-conflict": { title: "Profile changed elsewhere", body: "Your saved profile is stale. Reload it before trying to save again." },
};

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

export function AccountAccess() {
  const [access, setAccess] = useState<AccessContextResponse | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [state, setState] = useState<AccessState>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [selected, setSelected] = useState<string>("");
  const [saving, setSaving] = useState(false);
  const statusHeading = useRef<HTMLHeadingElement>(null);

  const load = useCallback(async () => {
    setState("loading"); setProblem(null);
    try {
      const context = await readJson<AccessContextResponse>("/api/bff/access-context");
      setAccess(context);
      if (context.memberships.length === 0) { setState("empty"); return; }
      setSelected((current) => current || (context.tenantSelectionRequired ? "" : context.memberships[0].organizationId));
      setState(context.tenantSelectionRequired ? "selection" : "ready");
      try { setProfile(await readJson<UserProfile>("/api/bff/profile")); } catch (error) { handleProblem(error); }
    } catch (error) { handleProblem(error); }
  }, []);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { statusHeading.current?.focus(); }, [state, problem]);

  function handleProblem(error: unknown) {
    const next = (error && typeof error === "object" ? error : {}) as Problem;
    setProblem(next);
    setState(next.code === "AUTHENTICATION_REQUIRED" || next.code === "SESSION_EXPIRED" ? "session-expired" : next.code === "PERMISSION_DENIED" ? "denied" : "error");
  }

  async function switchOrganization() {
    if (!selected) return;
    setSaving(true); setProblem(null);
    try { await readJson("/api/bff/tenant-context", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ organizationId: selected }) }); setState("ready"); }
    catch (error) { handleProblem(error); }
    finally { setSaving(false); }
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!profile) return; setSaving(true); setProblem(null);
    const form = new FormData(event.currentTarget);
    const body: UpdateProfileRequest = { displayName: String(form.get("displayName") ?? ""), locale: String(form.get("locale") ?? ""), reducedMotion: form.get("reducedMotion") === "on", highContrast: form.get("highContrast") === "on", expectedVersion: profile.version };
    try { setProfile(await readJson<UserProfile>("/api/bff/profile", { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) })); }
    catch (error) { const next = error as Problem; if (next?.code === "VERSION_CONFLICT") { setProblem(next); setState("profile-conflict"); } else handleProblem(error); }
    finally { setSaving(false); }
  }

  const copy = state === "selection" ? stateCopy.loading : state === "ready" ? null : stateCopy[state];
  return <AppShell><PageGrid><header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label="Account navigation"><Link href="/">Public</Link><Link aria-current="page" href="/account">Account</Link></nav></header><main id="main-content" className="nx-account-page">
    <p className="nx-eyebrow">Identity and organization access</p>
    <h1 ref={statusHeading} tabIndex={-1}>{state === "selection" ? "Choose your organization" : state === "ready" ? "Your account" : copy?.title}</h1>
    <p className="nx-lede" aria-live="polite">{state === "selection" ? "Select an active membership. The server validates this choice before any tenant work." : state === "ready" ? "Your profile is personal data; organization access is always confirmed by the Nexora BFF." : copy?.body}</p>
    {state === "loading" && <div className="nx-access-card" role="status"><StatusLabel kind="loading" /> Loading secure account context…</div>}
    {state === "empty" && <section className="nx-access-card" aria-labelledby="onboarding-title"><StatusLabel kind="planned" /><h2 id="onboarding-title">Start onboarding</h2><p>Ask an organization owner for an invitation. There is no browser-only organization creation flow.</p><Link className="nx-action-button nx-action-button--secondary" href="/">Return to public home</Link></section>}
    {state === "selection" && access && <section className="nx-access-card" aria-labelledby="org-title"><h2 id="org-title">Active memberships</h2><fieldset className="nx-org-options"><legend className="nx-visually-hidden">Organization</legend>{access.memberships.map((membership) => <label className="nx-org-option" key={membership.membershipId}><input type="radio" name="organization" value={membership.organizationId} checked={selected === membership.organizationId} onChange={(event) => setSelected(event.target.value)} /><span><strong>Organization {membership.organizationId.slice(0, 8)}</strong><small>{membership.role} · membership version {membership.membershipVersion}</small></span></label>)}</fieldset><ActionButton loading={saving} disabled={!selected} onClick={() => void switchOrganization()}>Continue</ActionButton></section>}
    {state === "ready" && <div className="nx-account-grid"><section className="nx-access-card" aria-labelledby="membership-title"><StatusLabel kind="fixture" /><h2 id="membership-title">Organization access</h2><p>Current membership: <strong>{access?.memberships.find((item) => item.organizationId === selected)?.role ?? "confirmed"}</strong></p><p className="nx-provenance">Every domain request revalidates membership freshness server-side.</p></section>{profile && <form className="nx-access-card nx-profile-form" onSubmit={(event) => void saveProfile(event)} aria-labelledby="profile-title"><StatusLabel kind="fixture" /><h2 id="profile-title">Profile</h2><label htmlFor="displayName">Display name<input id="displayName" name="displayName" defaultValue={profile.displayName} maxLength={120} required /></label><label htmlFor="locale">Locale<input id="locale" name="locale" defaultValue={profile.locale} pattern="[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})?" required /></label><label><input type="checkbox" name="reducedMotion" defaultChecked={profile.reducedMotion} /> Reduce motion</label><label><input type="checkbox" name="highContrast" defaultChecked={profile.highContrast} /> High contrast</label><p className="nx-field-help">Version {profile.version}; updates use optimistic concurrency.</p><ActionButton loading={saving} type="submit">Save profile</ActionButton></form>}</div>}
    {(state === "denied" || state === "session-expired" || state === "error" || state === "profile-conflict") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message ?? copy?.body}</p>{state === "profile-conflict" && <ActionButton tone="secondary" onClick={() => void load()}>Reload profile</ActionButton>}{problem?.traceId && <p className="nx-field-help">Reference: {problem.traceId}</p>}<div className="nx-hero-actions">{state !== "profile-conflict" && <ActionButton tone="secondary" onClick={() => void load()}>Retry</ActionButton>}{state === "session-expired" && <Link className="nx-action-button nx-action-button--primary" href="/auth/callback">Sign in again</Link>}</div></section>}
  </main></PageGrid></AppShell>;
}
