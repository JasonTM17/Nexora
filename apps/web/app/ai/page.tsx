import Link from "next/link";
import { connection } from "next/server";
import { AIResponse } from "../../../../packages/ui-ai/src/ai-response";
import { Citation } from "../../../../packages/ui-ai/src/citation";
import { PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";

export default async function AiPage() {
  await connection();
  return <PageGrid><main className="nx-surface-page" id="main-content"><header><Link href="/">← Nexora</Link><p className="nx-eyebrow">AI and knowledge surface</p><h1>Evidence before assertion</h1><p>This is a deterministic response frame, not an AI invocation or source lookup.</p><StatusLabel kind="fixture" /></header><AIResponse citationState="no-evidence">No answer is generated in this preview. When a connected feature returns a response, it must expose its source and authorization state.</AIResponse><Citation state="denied" title="" /></main></PageGrid>;
}
