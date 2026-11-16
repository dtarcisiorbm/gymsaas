import type { Metadata } from "next";
import "./globals.css";
import { SessionProvider } from "@/components/session/SessionProvider";
import { ThemeProvider } from "@/core/theme/ThemeProvider";

export const metadata: Metadata = {
  title: "GymSaaS — Gestão de academias",
  description: "Plataforma multi-tenant para gestão de academias.",
};

const themeInitScript = `
(function() {
  try {
    var t = localStorage.getItem('gymsaas_theme');
    document.documentElement.dataset.theme = t === 'light' ? 'light' : 'dark';
  } catch (e) {
    document.documentElement.dataset.theme = 'dark';
  }
})();
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body>
        <ThemeProvider>
          <SessionProvider>{children}</SessionProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
