// R00B product capture writer.
// Captures real deterministic foundation preview surfaces from the built web
// app at 1440px desktop and 375px mobile, records a bounded GIF walkthrough
// and writes the media manifest with runtime tuple + seed/fixture labels.
// Prerequisites: next build + next start on 127.0.0.1:3100.
// Run: node apps/web/scripts/m4-alpha-capture.mjs <productSha>

import { createHash } from "node:crypto";
import { mkdirSync, readFileSync, writeFileSync, existsSync, readdirSync, renameSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const productSha = process.argv[2] ?? "unset";
const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../../..");
const mediaDir = join(repoRoot, "docs/product/media");
const BASE = "http://127.0.0.1:3100";
mkdirSync(mediaDir, { recursive: true });

const sha256 = (buffer) => createHash("sha256").update(buffer).digest("hex");

const surfaces = [
  { path: "/", name: "home" },
  { path: "/studio", name: "studio" },
  { path: "/ai", name: "ai" },
  { path: "/knowledge", name: "knowledge" },
  { path: "/builder", name: "builder" },
];

const captured = [];

const browser = await chromium.launch({
  channel: "msedge",
  headless: true,
});

// Desktop captures at 1440x900 with 2x device scale.
const desktop = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 2,
});
const desktopPage = await desktop.newPage();
await desktopPage.goto(`${BASE}/`, { waitUntil: "networkidle" });
for (const surface of surfaces) {
  await desktopPage.goto(`${BASE}${surface.path}`, { waitUntil: "networkidle" });
  await desktopPage.waitForTimeout(400);
  const file = `${surface.name}-desktop.png`;
  await desktopPage.screenshot({ path: join(mediaDir, file), fullPage: true });
  captured.push({
    file,
    surface: surface.path,
    viewport: "1440x900@2x",
    kind: "desktop-screenshot",
    sha256: sha256(readFileSync(join(mediaDir, file))),
  });
  console.log(`captured ${file}`);
}
await desktop.close();

// Mobile captures at 375x812 with 2x device scale.
const mobile = await browser.newContext({
  viewport: { width: 375, height: 812 },
  deviceScaleFactor: 2,
  isMobile: true,
});
const mobilePage = await mobile.newPage();
for (const surface of surfaces) {
  await mobilePage.goto(`${BASE}${surface.path}`, { waitUntil: "networkidle" });
  await mobilePage.waitForTimeout(400);
  const file = `${surface.name}-mobile.png`;
  await mobilePage.screenshot({ path: join(mediaDir, file), fullPage: true });
  captured.push({
    file,
    surface: surface.path,
    viewport: "375x812@2x",
    kind: "mobile-screenshot",
    sha256: sha256(readFileSync(join(mediaDir, file))),
  });
  console.log(`captured ${file}`);
}
await mobile.close();

// Bounded GIF walkthrough: home -> surfaces -> home.
const tour = await browser.newContext({
  viewport: { width: 1280, height: 800 },
  recordVideo: { dir: mediaDir, size: { width: 1280, height: 800 } },
});
const tourPage = await tour.newPage();
await tourPage.goto(`${BASE}/`, { waitUntil: "networkidle" });
await tourPage.waitForTimeout(1200);
for (const surface of surfaces.slice(1)) {
  await tourPage.goto(`${BASE}${surface.path}`, { waitUntil: "networkidle" });
  await tourPage.waitForTimeout(1100);
}
await tourPage.goto(`${BASE}/`, { waitUntil: "networkidle" });
await tourPage.waitForTimeout(800);
await tour.close();
const webmPath = readdirSync(mediaDir).find((f) => f.endsWith(".webm"));
if (!webmPath) throw new Error("tour recording missing");
const finalWebm = join(mediaDir, "nexora-tour.webm");
renameSync(join(mediaDir, webmPath), finalWebm);
captured.push({
  file: "nexora-tour.webm",
  surface: "/",
  viewport: "1280x800",
  kind: "walkthrough-video",
  sha256: sha256(readFileSync(finalWebm)),
});
console.log("captured nexora-tour.webm");

const manifest = {
  task: "R00B",
  productSha,
  seedManifest: {
    path: "test-fixtures/demo/m4-seed-manifest.json",
    digest: sha256(readFileSync(join(repoRoot, "test-fixtures/demo/m4-seed-manifest.json"))),
    label: "Deterministic M4-S01 synthetic fixture (Nexora University), not live data.",
  },
  runtimeTuple: {
    surface: "apps/web foundation preview (Next.js production build)",
    base: BASE,
    note: "No tenant, provider, repository or live metric is connected; every surface is deterministic fixture content.",
  },
  providerLabel: "fixture-only; no hosted provider or live product call",
  generatedAt: new Date().toISOString(),
  captured,
};

writeFileSync(join(repoRoot, "docs/media-manifest.json"), JSON.stringify(manifest, null, 2) + "\n");
console.log(`recorded ${captured.length} artifacts to docs/media-manifest.json`);

await browser.close();
