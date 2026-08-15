import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { TraceCard, type RagTraceItem } from "../../../packages/ui-ai/src/trace-card";
import { EvaluationViewer, type EvaluationReportData } from "../app/admin/rag/evaluation-viewer";
import { FeedbackModeration, type FeedbackItem } from "../app/admin/rag/feedback-moderation";

describe("RAG Quality Dashboard UI Tests", () => {
  it("renders safe redacted trace card with metrics", () => {
    const trace: RagTraceItem = {
      id: "550e8400-e29b-41d4-a716-446655440000",
      queryHash: "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      modelId: "deepseek-v4-flash",
      modelRevision: "v1-prod",
      candidateCount: 10,
      selectedCitationCount: 3,
      latencyMs: 142,
      outcome: "ANSWERED",
      createdAt: new Date().toISOString(),
    };

    render(<TraceCard trace={trace} />);

    expect(screen.getByText(/sha256:e3b0c442/i)).toBeInTheDocument();
    expect(screen.getByText(/142 ms/i)).toBeInTheDocument();
    expect(screen.getByText(/ANSWERED/i)).toBeInTheDocument();
  });

  it("renders evaluation metrics and data provenance", () => {
    const report: EvaluationReportData = {
      datasetId: "eval-rag-v1",
      datasetChecksum: "sha256:7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
      totalQueries: 50,
      recallAtK: 0.88,
      citationPrecision: 0.94,
      noAnswerRate: 0.05,
      averageLatencyMs: 120,
      modelId: "deepseek-v4-flash",
      modelRevision: "v1-prod",
      rerankerEnabled: true,
      evaluatedAt: new Date().toISOString(),
      notes: "Deterministic seed fixtures + live tenant history",
    };

    render(<EvaluationViewer report={report} loading={false} onRefresh={vi.fn()} />);

    expect(screen.getByText("88.0%")).toBeInTheDocument();
    expect(screen.getByText("94.0%")).toBeInTheDocument();
    expect(screen.getByText("120 ms")).toBeInTheDocument();
    expect(screen.getByText(/eval-rag-v1/i)).toBeInTheDocument();
  });

  it("renders user feedback and triggers moderation delete action", () => {
    const feedbackList: FeedbackItem[] = [
      {
        id: "fb-1",
        runId: "550e8400-e29b-41d4-a716-446655440000",
        rating: "UP",
        comment: "Accurate citation and concise response.",
        createdAt: new Date().toISOString(),
      },
    ];

    const handleDelete = vi.fn();
    render(
      <FeedbackModeration
        feedbackList={feedbackList}
        loading={false}
        onDelete={handleDelete}
        onRefresh={vi.fn()}
      />
    );

    expect(screen.getByText(/Helpful \(UP\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Accurate citation and concise response/i)).toBeInTheDocument();
  });
});
