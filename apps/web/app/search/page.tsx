"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import type { AccessContextResponse } from "../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

interface SearchResult {
  id: string;
  sourceType: string;
  title: string;
  snippet: string | null;
  score: number;
}

interface SearchResults {
  items: SearchResult[];
  nextCursor: string | null;
  totalCount: number;
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "Search failed." };
}

export default function SearchPage() {
  const searchParams = useSearchParams();
  const query = searchParams.get("q") || "";
  const [organizationId, setOrganizationId] = useState("");
  const [results, setResults] = useState<SearchResults | null>(null);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = asProblem(error);
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, []);

  const search = useCallback(async () => {
    if (!query.trim()) { setResults(null); setState("empty"); return; }
    setState("loading"); setProblem(null);
    try {
      const access = await readJson<AccessContextResponse>("/api/bff/access-context");
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) { setResults(null); setState("empty"); return; }
      const encodedQuery = encodeURIComponent(query.trim());
      const result = await readJson<SearchResults>(`/api/bff/search?organizationId=${encodeURIComponent(selected)}&query=${encodedQuery}`);
      setResults(result); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, query, setFailure]);

  useEffect(() => { void search(); }, [search]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">Search</p>
    <h1 ref={heading} tabIndex={-1}>Global search</h1>
    <p className="nx-lede">Authorized hybrid search over pages and knowledge.</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> Searching…</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>Enter a query</h2><p>Search across published pages and active knowledge.</p></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void search()}>Retry</ActionButton></section>}

    {state === "ready" && results && <section className="nx-access-card">
      <div className="nx-card-heading"><div><h2>Results</h2><p className="nx-field-help">{results.totalCount} matches</p></div><StatusLabel kind="fixture" /></div>
      {results.items.length === 0 ? <p className="nx-empty-copy">No results for "{query}".</p> :
        <ul className="nx-search-results">{results.items.map((item) => <li key={item.id}>
          <div className="nx-search-result-header">
            <span className="nx-badge">{item.sourceType}</span>
            <strong>{item.title}</strong>
          </div>
          {item.snippet && <p className="nx-search-snippet">{item.snippet}</p>}
        </li>)}</ul>}
    </section>}
  </main></PageGrid></AppShell>;
}
