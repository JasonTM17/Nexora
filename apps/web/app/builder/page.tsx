import Link from "next/link";
import { connection } from "next/server";
import { BuilderFrame } from "../../../../packages/ui-builder/src/builder-frame";
import { BuilderSelectionFixture } from "../../../../packages/ui-builder/src/builder-selection-fixture";
import { PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";

export default async function BuilderPage() {
  await connection();
  return <PageGrid><main className="nx-surface-page" id="main-content"><header><Link href="/">← Nexora</Link><p className="nx-eyebrow">Builder surface</p><h1>Clear ownership and selection</h1><p>The compact mobile frame presents navigator, canvas, and inspector sequentially. It does not claim full desktop-canvas editing on a phone.</p><StatusLabel kind="fixture" /></header><BuilderFrame navigator={<><strong>Navigator</strong><p>Landing page</p><p>Selected: Hero</p></>} canvas={<BuilderSelectionFixture />} inspector={<><strong>Inspector</strong><p>Selection: Hero</p><p>Publish state: not connected</p></>} /></main></PageGrid>;
}
