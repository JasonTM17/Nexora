import Link from "next/link";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";

export const dynamic = "force-dynamic";

export default function AuthCallbackPage() {
  return <AppShell><PageGrid><main id="main-content" className="nx-account-page"><p className="nx-eyebrow">Secure sign-in handoff</p><h1>Returning to Nexora</h1><p className="nx-lede">Your session is handled by the same-origin server boundary. The browser never decides identity, organization membership, or permissions.</p><section className="nx-access-card"><StatusLabel kind="loading" /><p>Continue to your account to confirm access.</p><Link className="nx-action-button nx-action-button--primary" href="/account">Continue</Link></section></main></PageGrid></AppShell>;
}
