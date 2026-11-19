import type { Metadata } from "next";
import Link from "next/link";
import { ArrowUpRight, Blocks, GitFork } from "lucide-react";
import { issueForm, repository } from "./links";
import "@fontsource-variable/inter";
import "@fontsource-variable/space-grotesk";
import "./style.css";

export const metadata: Metadata = {
  title: "Widgets — App Unlocks",
  other: {"page-type":"software","site-kind":"software"},
  description: "An Android widget gallery. Preview the widgets, make them your own, and download the app.",
};

export default function Layout({ children }: { children: React.ReactNode }) {
  const base = process.env.NEXT_PUBLIC_BASE_PATH ?? "";
  const art = { "--glass": `url("${base}/substrate.webp")`, "--glass-light": `url("${base}/glass-light.webp")` } as React.CSSProperties;
  return <html lang="en" data-type="software"><body style={art}>
    <header className="site-header"><div className="wrap nav">
      <Link className="wordmark" href="/"><Blocks size={22}/> Widgets</Link>
      <nav aria-label="Main">
        <Link href="/#gallery">Gallery</Link>
        <Link href="/releases/">Releases</Link>
        <a href={repository} aria-label="GitHub repository" className="icon-link"><GitFork size={19}/></a>
      </nav>
    </div></header>
    {children}
    <footer className="footer wrap">
      <Link className="wordmark" href="/"><Blocks size={19}/> Widgets</Link>
      <div><a href={issueForm}>Suggest a widget <ArrowUpRight size={15}/></a><a href={repository}>GitHub <ArrowUpRight size={15}/></a></div>
    </footer>
  </body></html>;
}
