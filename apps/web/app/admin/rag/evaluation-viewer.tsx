"use client";

import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";

export interface EvaluationReportData {
  readonly datasetId: string;
  readonly datasetChecksum: string;
  readonly totalQueries: number;
  readonly recallAtK: number;
  readonly citationPrecision: number;
  readonly noAnswerRate: number;
  readonly averageLatencyMs: number;
  readonly modelId: string;
  readonly modelRevision: string;
  readonly rerankerEnabled: boolean;
  readonly evaluatedAt: string;
  readonly notes: string;
}

interface EvaluationViewerProps {
  readonly report: EvaluationReportData | null;
  readonly loading: boolean;
  readonly onRefresh: () => void;
}

export function EvaluationViewer({ report, loading, onRefresh }: EvaluationViewerProps) {
  return (
    <section className="nx-dashboard-panel" aria-labelledby="eval-heading">
      <div className="nx-panel-header">
        <div>
          <h2 id="eval-heading">RAG Quality & Evaluation Benchmarks</h2>
          <p className="nx-field-help">
            Automated quality evaluation metrics computed over tenant query history and deterministic seed fixtures.
          </p>
        </div>
        <ActionButton tone="secondary" disabled={loading} onClick={onRefresh}>
          {loading ? "Calculating..." : "Re-evaluate"}
        </ActionButton>
      </div>

      {report ? (
        <div className="nx-eval-container">
          <div className="nx-metric-grid">
            <div className="nx-metric-card">
              <span className="nx-metric-label">Recall@K (k=10)</span>
              <span className="nx-metric-value">{(report.recallAtK * 100).toFixed(1)}%</span>
              <span className="nx-metric-sub">Target: &gt;= 80.0%</span>
            </div>
            <div className="nx-metric-card">
              <span className="nx-metric-label">Citation Precision</span>
              <span className="nx-metric-value">{(report.citationPrecision * 100).toFixed(1)}%</span>
              <span className="nx-metric-sub">Target: &gt;= 90.0%</span>
            </div>
            <div className="nx-metric-card">
              <span className="nx-metric-label">No-Answer Rate</span>
              <span className="nx-metric-value">{(report.noAnswerRate * 100).toFixed(1)}%</span>
              <span className="nx-metric-sub">Honest bounds</span>
            </div>
            <div className="nx-metric-card">
              <span className="nx-metric-label">Avg Retrieval Latency</span>
              <span className="nx-metric-value">{report.averageLatencyMs} ms</span>
              <span className="nx-metric-sub">Target: &lt; 250ms</span>
            </div>
          </div>

          <div className="nx-eval-provenance">
            <h3>Evaluation Provenance</h3>
            <ul className="nx-eval-meta-list">
              <li>
                <strong>Dataset:</strong> {report.datasetId} ({report.totalQueries} evaluated runs)
              </li>
              <li>
                <strong>Checksum:</strong> <code>{report.datasetChecksum.slice(0, 24)}…</code>
              </li>
              <li>
                <strong>Model:</strong> {report.modelId} (Rev: {report.modelRevision})
              </li>
              <li>
                <strong>Reranker:</strong> {report.rerankerEnabled ? "Active" : "Disabled (RRF Baseline)"}
              </li>
              <li>
                <strong>Timestamp:</strong> {new Date(report.evaluatedAt).toLocaleString()}
              </li>
            </ul>
            <div className="nx-provenance">
              <StatusLabel kind="fixture" /> {report.notes}
            </div>
          </div>
        </div>
      ) : (
        <div className="nx-empty-card">
          <StatusLabel kind="loading" />
          <p>Loading evaluation report metrics...</p>
        </div>
      )}
    </section>
  );
}
