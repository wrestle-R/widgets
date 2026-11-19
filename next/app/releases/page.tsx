import type { Metadata } from "next";
import { ArrowUpRight, Download, PackageCheck } from "lucide-react";
import { latestApk, repository } from "../links";
import { ReleaseHistory } from "./release-history";

export const metadata: Metadata = {
  title: "Releases — Widgets",
  description: "Download the latest signed Widgets APK for Android and browse earlier releases.",
};

export default function Releases() {
  return <main className="wrap release-page">
    <div className="release-header">
      <PackageCheck size={34} strokeWidth={1.5}/>
      <h1>Releases.</h1>
      <p>Download Widgets for Android 9 and up.</p>
      <div className="actions">
        <a className="button primary" href={latestApk}><Download size={17}/> Download latest APK</a>
        <a className="button secondary" href={`${repository}/releases`}>GitHub releases <ArrowUpRight size={16}/></a>
      </div>
    </div>
    <ReleaseHistory />
  </main>;
}
