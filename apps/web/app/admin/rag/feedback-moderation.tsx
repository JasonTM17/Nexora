"use client";

import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";

export interface FeedbackItem {
  readonly id: string;
  readonly runId: string;
  readonly rating: "UP" | "DOWN";
  readonly comment: string;
  readonly createdAt: string;
}

interface FeedbackModerationProps {
  readonly feedbackList: ReadonlyArray<FeedbackItem>;
  readonly loading: boolean;
  readonly onDelete: (id: string) => void;
  readonly onRefresh: () => void;
}

export function FeedbackModeration({ feedbackList, loading, onDelete, onRefresh }: FeedbackModerationProps) {
  return (
    <section className="nx-dashboard-panel" aria-labelledby="feedback-heading">
      <div className="nx-panel-header">
        <div>
          <h2 id="feedback-heading">User Feedback & Moderation</h2>
          <p className="nx-field-help">
            User satisfaction ratings and comments on retrieval answers within the tenant scope.
          </p>
        </div>
        <ActionButton tone="secondary" disabled={loading} onClick={onRefresh}>
          {loading ? "Loading..." : "Refresh Feedback"}
        </ActionButton>
      </div>

      {feedbackList.length === 0 && !loading && (
        <div className="nx-empty-card">
          <StatusLabel kind="planned" />
          <p>No user feedback entries submitted yet.</p>
        </div>
      )}

      <div className="nx-feedback-list">
        {feedbackList.map((item) => {
          const isPositive = item.rating === "UP";
          return (
            <article key={item.id} className="nx-feedback-card" aria-label={`Feedback ${item.id}`}>
              <div className="nx-feedback-header">
                <div className="nx-feedback-rating-wrap">
                  <span className={`nx-rating-tag ${isPositive ? "nx-rating-tag--up" : "nx-rating-tag--down"}`}>
                    {isPositive ? "▲ Helpful (UP)" : "▼ Inaccurate (DOWN)"}
                  </span>
                  <span className="nx-field-help">Trace: {item.runId.slice(0, 8)}…</span>
                </div>
                <button
                  type="button"
                  className="nx-action-button nx-action-button--tertiary nx-btn-danger"
                  onClick={() => onDelete(item.id)}
                  aria-label={`Delete feedback ${item.id}`}
                >
                  Delete
                </button>
              </div>

              {item.comment && (
                <p className="nx-feedback-comment">
                  &ldquo;{item.comment}&rdquo;
                </p>
              )}

              <div className="nx-feedback-meta">
                <small className="nx-field-help">
                  Submitted: {new Date(item.createdAt).toLocaleString()}
                </small>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
