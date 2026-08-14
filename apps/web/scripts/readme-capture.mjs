// Captures README evidence from the built web foundation preview.
// Prerequisites: `next build` then `next start -p 3100 -H 127.0.0.1`.
// Run: node apps/web/scripts/readme-capture.mjs
// Outputs: assets/readme/*.png plus assets/readme/nexora-tour.webm
// (convert the webm to GIF with tools/readme-gif.sh).

import { mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../../..");
const outDir = resolve(repoRoot, "assets/readme");
mkdirSync(outDir, { recursive: true });

const BASE = process.env.NEXORA_README_CAPTURE_URL ?? "http://127.0.0.1:3100";
const surfaces = [
  { path: "/", name: "home" },
  { path: "/studio", name: "studio" },
  { path: "/ai", name: "ai" },
  { path: "/builder", name: "builder" },
];

async function waitForApp(page) {
  await page.goto(`${BASE}/`, { waitUntil: "networkidle" });
  await page.waitForSelector(".nx-wordmark", { timeout: 15000 });
}

const browser = await chromium.launch();

// 1. Static, populated screenshots of every deterministic surface.
const desktop = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 2,
});
const page = await desktop.newPage();
await waitForApp(page);
for (const surface of surfaces) {
  await page.goto(`${BASE}${surface.path}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(350);
  await page.screenshot({ path: resolve(outDir, `${surface.name}.png`), fullPage: true });
  console.log(`captured ${surface.name}.png`);
}
await desktop.close();

// The Builder surface is designed around a compact mobile frame.
const mobile = await browser.newContext({
  viewport: { width: 390, height: 844 },
  deviceScaleFactor: 2,
});
const mobilePage = await mobile.newPage();
await mobilePage.goto(`${BASE}/builder`, { waitUntil: "networkidle" });
await mobilePage.waitForTimeout(350);
await mobilePage.screenshot({ path: resolve(outDir, "builder-mobile.png"), fullPage: true });
console.log("captured builder-mobile.png");
await mobile.close();

// 2. Guided tour recording for the README GIF.
const tour = await browser.newContext({
  viewport: { width: 1280, height: 800 },
  recordVideo: { dir: outDir, size: { width: 1280, height: 800 } },
});
const tourPage = await tour.newPage();
await tourPage.goto(`${BASE}/`, { waitUntil: "networkidle" });
await tourPage.waitForTimeout(1400);
// Show the hero actions, then the surface card grid below the fold.
await tourPage.mouse.move(420, 430, { steps: 12 });
await tourPage.mouse.wheel(0, 300);
await tourPage.waitForTimeout(1000);
await tourPage.mouse.wheel(0, -300);
await tourPage.waitForTimeout(600);
for (const surface of surfaces.slice(1)) {
  await tourPage.click(`a[href="${surface.path}"]`);
  await tourPage.waitForLoadState("networkidle");
  await tourPage.waitForTimeout(1600);
  await tourPage.click('a[href="/"]');
  await tourPage.waitForLoadState("networkidle");
  await tourPage.waitForTimeout(900);
}
await tour.close();
console.log("captured nexora-tour.webm");

await browser.close();
