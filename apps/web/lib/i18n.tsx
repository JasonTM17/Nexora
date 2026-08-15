"use client";

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import en from "../messages/en.json";
import vi from "../messages/vi.json";

type Locale = "en" | "vi";
type Messages = typeof en;

const messages: Record<Locale, Messages> = { en, vi };

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

const STORAGE_KEY = "nexora-locale";

/** Resolve nested key like "common.loading" */
function resolve(obj: Record<string, unknown>, path: string): string | undefined {
  const parts = path.split(".");
  let current: unknown = obj;
  for (const part of parts) {
    if (current && typeof current === "object" && part in current) {
      current = (current as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return typeof current === "string" ? current : undefined;
}

/** Simple interpolation: {name} → value */
function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (_, key) => String(params[key] ?? `{${key}}`));
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("en");

  useEffect(() => {
    // Load from cookie or localStorage
    const stored = typeof window !== "undefined"
      ? (localStorage.getItem(STORAGE_KEY) || document.cookie.match(/nexora-locale=(\w+)/)?.[1])
      : null;
    if (stored === "vi" || stored === "en") {
      setLocaleState(stored);
    }
  }, []);

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    if (typeof window !== "undefined") {
      localStorage.setItem(STORAGE_KEY, next);
      document.cookie = `nexora-locale=${next}; path=/; max-age=31536000`;
    }
  }, []);

  const t = useCallback((key: string, params?: Record<string, string | number>) => {
    const template = resolve(messages[locale] as unknown as Record<string, unknown>, key);
    if (template === undefined) {
      // Fallback to English
      const fallback = resolve(messages.en as unknown as Record<string, unknown>, key);
      return fallback ? interpolate(fallback, params) : key;
    }
    return interpolate(template, params);
  }, [locale]);

  return <I18nContext.Provider value={{ locale, setLocale, t }}>{children}</I18nContext.Provider>;
}

const defaultI18nValue: I18nContextValue = {
  locale: "en",
  setLocale: () => {},
  t: (key: string, params?: Record<string, string | number>) => {
    const fallback = resolve(messages.en as unknown as Record<string, unknown>, key);
    return fallback ? interpolate(fallback, params) : key;
  },
};

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  return ctx ?? defaultI18nValue;
}

export const SUPPORTED_LOCALES: Locale[] = ["en", "vi"];
