import type { ReactNode } from "react";
import { Citation, type CitationState } from "./citation";

export function AIResponse({ children, citationState = "no-evidence" }: { children: ReactNode; citationState?: CitationState }) {
  return <article className="nx-ai-response"><p>{children}</p><Citation state={citationState} title="Architecture note" /></article>;
}
