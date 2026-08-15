import { RagAdminDashboard } from "./rag-admin-dashboard";

export const metadata = {
  title: "RAG Quality & Observability | Nexora Admin",
  description: "Inspect retrieval traces, review RAG quality benchmarks, and moderate feedback with tenant isolation.",
};

export default function RagAdminPage() {
  return <RagAdminDashboard />;
}
