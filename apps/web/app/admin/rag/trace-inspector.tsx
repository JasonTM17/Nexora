"use client";

import type { RagTraceItem } from "../../../../../packages/ui-ai/src/trace-card";
import { TraceCard } from "../../../../../packages/ui-ai/src/trace-card";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";

interface TraceInspectorProps {
  readonly traces: ReadonlyArray<RagTraceItem>;
  readonly loading: boolean;
  readonly onRefresh: () => void;
}

export function TraceInspector({ traces, loading, onRefresh }: TraceInspectorProps) {
  return (
    <section className="nx-dashboard-panel" aria-labelledby="traces-heading">
      <div className="nx-panel-header">
        <div>
          <h2 id="traces-heading">Retrieval Traces</h2>
          <p className="nx-field-help">
            Safe redacted retrieval runs per tenant. Raw prompts and source texts are strictly excluded.
          </p>
        </div>
        <ActionButton tone="secondary" disabled={loading} onClick={onRefresh}>
          {loading ? "Loading..." : "Refresh Traces"}
        </ActionButton>
      </div>

      {traces.length === 0 && !loading && (
        <div className="nx-empty-card">
          <StatusLabel kind="planned" />
          <p>No retrieval traces recorded yet for this organization.</p>
          <small className="nx-field-help">Traces are automatically generated when asking questions at /ai.</small>
        </div>
      )}

      <div className="nx-trace-list">
        {traces.map((trace) => (
          <TraceCard key={trace.id} trace={trace} />
        ))}
      </div>
    </section>
  );
}
