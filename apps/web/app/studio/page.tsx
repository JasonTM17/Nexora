import Link from "next/link";
import { PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";
import { NexoraStudioProvider } from "../../../../packages/ui-studio/src/nexora-studio-provider";
import { StudioField } from "../../../../packages/ui-studio/src/studio-field";

export default function StudioPage() {
  return <NexoraStudioProvider><PageGrid><main className="nx-surface-page" id="main-content"><header><Link href="/">← Nexora</Link><p className="nx-eyebrow">Studio surface</p><h1>Workflow foundations</h1><p>Static states demonstrate the wrapper contract. They do not represent a connected workspace.</p><StatusLabel kind="fixture" /></header><div className="nx-studio-stack"><StudioField label="Workspace name" /><StudioField label="Review queue" state="loading" /><StudioField label="Recent activity" state="empty" /><StudioField label="Protected setting" state="denied" /><StudioField label="Saved view" state="error" /></div></main></PageGrid></NexoraStudioProvider>;
}
