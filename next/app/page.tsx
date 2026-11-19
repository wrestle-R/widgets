import Link from "next/link";
import { ArrowDown, ArrowUpRight, Download, ImagePlus } from "lucide-react";
import designs from "../../shared/widgets.json";
import { Inspector, UnlockTile } from "./gallery";
import { issueForm, latestApk } from "./links";

export default function Page() {
  return <main>
    <section className="hero">
      <div className="hero-inner wrap">
        <div className="hero-copy">
          <p className="platform"><span /> For Android</p>
          <h1>App<br/>Unlocks.</h1>
          <p className="lead">See how often you unlock your phone. One count, five ways to put it on your home screen.</p>
          <div className="actions">
            <a className="button primary" href={latestApk}><Download size={17}/> Download APK</a>
            <a className="button secondary" href="#gallery">Try the designs <ArrowDown size={16}/></a>
          </div>
          <Link className="android-version" href="/releases/">Android 9 and up <span/> Release notes <ArrowUpRight size={13}/></Link>
        </div>
        <div className="hero-product" aria-hidden="true">
          <div className="hero-tiles">
            <div className="hero-tile hero-classic"><UnlockTile design={designs[0]}/></div>
            <div className="hero-tile hero-orbit"><UnlockTile design={designs[2]}/></div>
            <div className="hero-tile hero-signal"><UnlockTile design={designs[1]} width={4} height={2}/></div>
          </div>
        </div>
      </div>
    </section>

    <section id="gallery" className="wrap gallery">
      <div className="section-title">
        <h2>Find your fit.</h2>
        <p>Try each design from 1 × 1 to 4 × 4, with your preferred theme and opacity.</p>
      </div>
      <Inspector />
    </section>

    <section className="wrap suggest" id="suggest">
      <div className="suggest-art" aria-hidden="true"><ImagePlus strokeWidth={1.25}/></div>
      <div>
        <h2>What should I make next?</h2>
        <p>Send me an idea, a sketch, or a screenshot of a widget you’d like on your phone.</p>
        <a className="button primary" href={issueForm}>Suggest a widget <ArrowUpRight size={17}/></a>
      </div>
    </section>
  </main>;
}
