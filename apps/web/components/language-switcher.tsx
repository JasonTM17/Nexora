"use client";

import { useI18n, SUPPORTED_LOCALES } from "../lib/i18n";

export function LanguageSwitcher() {
  const { locale, setLocale } = useI18n();

  return (
    <div className="nx-language-switcher" role="group" aria-label="Language">
      {SUPPORTED_LOCALES.map((l) => (
        <button
          key={l}
          type="button"
          className={`nx-language-btn ${locale === l ? "nx-language-btn--active" : ""}`}
          aria-pressed={locale === l}
          onClick={() => setLocale(l)}
        >
          {l.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
