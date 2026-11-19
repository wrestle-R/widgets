import type { Metadata } from "next";
import { ReleaseHistory } from "./release-history";

export const metadata: Metadata = {
  title: "Releases — Widgets",
  description: "Download the latest signed Widgets APK for Android and browse earlier releases.",
};

export default function Releases() {
  return <main id="main" className="wrap release-page"><ReleaseHistory /></main>;
}
