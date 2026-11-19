import type { Metadata } from "next";
import "./style.css";

export const metadata: Metadata = {
  title: "Widgets — Android home-screen gallery",
  description: "Preview, resize and download widgets for your Android home screen.",
};

export default function Layout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><body>{children}</body></html>;
}
