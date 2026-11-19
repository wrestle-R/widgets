"use client";

import { useEffect, useState } from "react";
import { ArrowUpRight, Download } from "lucide-react";
import { latestApk, repository } from "../links";

type Release = {
  tag_name: string;
  html_url: string;
  published_at: string;
  prerelease: boolean;
  draft: boolean;
  assets: {name: string; browser_download_url: string}[];
};

export function ReleaseHistory() {
  const [history, setHistory] = useState<Release[] | "error">();
  useEffect(() => {
    const controller = new AbortController();
    fetch("https://api.github.com/repos/wrestle-R/widgets/releases", { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error("Release history unavailable");
        return response.json() as Promise<Release[]>;
      })
      .then(releases => setHistory(releases.filter(release => !release.draft && !release.prerelease)))
      .catch(() => { if (!controller.signal.aborted) setHistory("error"); });
    return () => controller.abort();
  }, []);

  const latest = Array.isArray(history)
    ? history.find(release => release.assets.some(asset => asset.name === "widgets.apk"))
    : undefined;

  return <>
    <header className="release-header">
      <div>
        <h1>Releases.</h1>
        <p>Download Widgets for Android 9 and up.</p>
        <div className="actions">
          <a className="button primary" href={latestApk}><Download size={18}/> Download latest APK</a>
          <a className="text-link" href={`${repository}/releases`}>View on GitHub <ArrowUpRight size={15}/></a>
        </div>
      </div>
      <div className="release-feature">
        <span>Latest release</span>
        {latest && <strong>{latest.tag_name}</strong>}
      </div>
    </header>
    <section className="release-history" aria-labelledby="history-heading">
    <h2 id="history-heading">Version history</h2>
    {history === undefined && <p role="status">Loading releases…</p>}
    {history === "error" && <p>Open <a href={`${repository}/releases`}>GitHub releases</a> for version history.</p>}
    {Array.isArray(history) && (history.length === 0 ? <p>No published releases yet.</p> :
      <ul>{history.map(release => {
        const apk = release.assets.find(asset => asset.name === "widgets.apk");
        const date = new Intl.DateTimeFormat(undefined, {dateStyle:"medium"}).format(new Date(release.published_at));
        return <li key={release.tag_name}>
          <div><h3>{release.tag_name}</h3><time dateTime={release.published_at}>{date}</time></div>
          <div className="actions">
            <a className="button secondary" href={release.html_url}>Release notes <ArrowUpRight size={15}/></a>
            {apk && <a className="button primary" href={apk.browser_download_url}><Download size={15}/> APK</a>}
          </div>
        </li>;
      })}</ul>)}
  </section></>;
}
