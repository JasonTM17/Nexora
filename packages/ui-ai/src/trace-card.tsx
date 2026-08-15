"use client";

import { StatusLabel } from "../../ui-core/src/status-label";

export interface RagTraceItem {
  readonly id: string;
  readonly sessionId?: string | null;
  readonly queryHash: string;
  readonly corpusVersion: string;
  readonly modelId: string;
  readonly modelRevision: string;
  readonly candidateIds?: ReadonlyArray<string>;
  readonly selectedChunkIds?: ReadonlyArray<string>;
  readonly candidateCount?: number;
  readonly selectedCitationCount?: number;
  readonly outcome: "ANSWERED" | "NO_ANSWER" | "LOW_CONFIDENCE" | "CANCELLED" | "FAILED" | string;
  readonly latencyMs: number;
  readonly tokenCount: number;
  readonly createdAt: string;
}

export function TraceCard({ trace }: { readonly trace: RagTraceItem }) {
  const formattedDate = new Date(trace.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  const isSuccess = trace.outcome === "ANSWERED";

  return (
    <article className="nx-trace-card" aria-label={`Trace ${trace.id}`}>
      <div className="nx-trace-header">
        <div className="nx-trace-title-group">
          <span className="nx-trace-hash" title={trace.queryHash}>
            Query {trace.queryHash.slice(0, 15)}…
          </span>
          <span className="nx-trace-time">{formattedDate}</span>
        </div>
        <StatusLabel kind={isSuccess ? "fixture" : "error"} />
      </div>

      <div className="nx-trace-meta-grid">
        <div className="nx-trace-stat">
          <span className="nx-trace-stat-label">Outcome</span>
          <span className="nx-trace-stat-val">{trace.outcome}</span>
        </div>
        <div className="nx-trace-stat">
          <span className="nx-trace-stat-label">Latency</span>
          <span className="nx-trace-stat-val">{trace.latencyMs} ms</span>
        </div>
        <div className="nx-trace-stat">
          <span className="nx-trace-stat-label">Candidates</span>
          <span className="nx-trace-stat-val">{trace.candidateIds?.length ?? trace.candidateCount ?? 0} chunks</span>
        </div>
        <div className="nx-trace-stat">
          <span className="nx-trace-stat-label">Selected</span>
          <span className="nx-trace-stat-val">{trace.selectedChunkIds?.length ?? trace.selectedCitationCount ?? 0} citations</span>
        </div>
      </div>

      <div className="nx-trace-footer">
        <small className="nx-field-help">
          Model {trace.modelId} ({trace.modelRevision}) · Tokens: {trace.tokenCount} · Corpus: {trace.corpusVersion}
        </small>
      </div>
    </article>
  );
}
