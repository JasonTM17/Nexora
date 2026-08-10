"use client";

import { ConfigProvider } from "antd";
import type { ReactNode } from "react";
import { nexoraTokens } from "../../design-tokens/src/index";

/** The only Ant Design provider boundary for Studio-owned routes. */
export function NexoraStudioProvider({ children }: { children: ReactNode }) {
  return (
    <ConfigProvider
      theme={{
        token: {
          borderRadius: nexoraTokens.borderRadius,
          colorBgBase: nexoraTokens.colorBgBase,
          colorBgContainer: nexoraTokens.colorBgContainer,
          colorBorder: nexoraTokens.colorBorder,
          colorPrimary: nexoraTokens.colorPrimary,
          colorText: nexoraTokens.colorText,
          colorTextSecondary: nexoraTokens.colorTextSecondary,
          controlHeight: nexoraTokens.controlHeight,
          fontFamily: "var(--nx-font-ui)",
        },
      }}
    >
      {children}
    </ConfigProvider>
  );
}
