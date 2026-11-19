import Link from "next/link";
import { ArrowDown, ArrowUpRight, Download, ImagePlus } from "lucide-react";
import widgets from "../../shared/widgets.json";
import { Inspector, Tile } from "./gallery";
import { issueForm, latestApk } from "./links";

export default function Page() {
  const art = `${process.env.NEXT_PUBLIC_BASE_PATH ?? ""}/substrate.webp`;
  return <main>
    <section className="hero" style={{backgroundImage:`url("${art}")`}}>
      <div className="hero-inner wrap">
        <div className="hero-copy" data-enter>
          <p className="platform"><span /> For Android</p>
          <h1>Widgets.<br/>Your way.</h1>
          <p className="lead">A collection of widgets for the things you want on your home screen.</p>
          <div className="actions">
            <a className="button primary" href={latestApk}><Download size={17}/> Download APK</a>
            <a className="button secondary" href="#gallery">Explore widgets <ArrowDown size={16}/></a>
          </div>
          <Link className="android-version" href="/releases/">Android 9 and up <span/> Release notes <ArrowUpRight size={13}/></Link>
        </div>
        <div className="hero-product" aria-hidden="true">
          <div className="phone">
            <div className="phone-camera" />
            <div className="phone-time">09<span>:</span>41</div>
            <div className="phone-grid">{widgets.map(widget => <Tile key={widget.id} widget={widget} />)}</div>
            <div className="phone-home" />
          </div>
        </div>
      </div>
    </section>

    <section id="gallery" className="wrap gallery">
      <div className="section-title">
        <h2>Try the widgets.</h2>
        <p>See how each one looks at different sizes, opacity levels, and themes.</p>
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
