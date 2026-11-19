import type { Metadata } from "next";
import type { CSSProperties } from "react";
import Link from "next/link";
import { ArrowUpRight, Blocks, Download } from "lucide-react";
import { issueForm, latestApk, repository } from "./links";
import "@fontsource-variable/inter/wght.css";
import "@fontsource/anton/latin-400.css";
import "./style.css";

export const metadata: Metadata = {
  title: "Widgets — Make it yours",
  other: {"page-type":"software","site-kind":"software"},
  description: "Find a widget, try it in light or dark, and make it part of your Android home screen.",
};

export default function Layout({ children }: { children: React.ReactNode }) {
  const base = process.env.NEXT_PUBLIC_BASE_PATH ?? "";
  const art = {
    "--art-dark": `url("${base}/red-glass.webp")`,
    "--art-light": `url("${base}/light-glass.webp")`,
  } as CSSProperties;
  return <html lang="en" data-type="software"><body style={art}>
    <a className="skip-link" href="#main">Skip to content</a>
    <header className="site-header"><div className="wrap nav">
      <Link className="wordmark" href="/"><Blocks size={23}/> Widgets</Link>
      <nav aria-label="Main">
        <Link href="/#gallery">Gallery</Link>
        <Link href="/releases/">Releases</Link>
      </nav>
      <a className="button primary nav-download" aria-label="Download Widgets" href={latestApk}><Download size={16}/><span>Download</span></a>
    </div></header>
    {children}
    <footer className="footer"><div className="wrap footer-inner">
      <Link className="wordmark" href="/"><Blocks size={20}/> Widgets</Link>
      <div><a href={issueForm}>Suggest a widget <ArrowUpRight size={15}/></a><a href={repository}>GitHub <ArrowUpRight size={15}/></a></div>
    </div></footer>
  </body></html>;
}
