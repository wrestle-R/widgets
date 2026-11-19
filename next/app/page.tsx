import { ArrowDown, ArrowUpRight, Download, ImagePlus } from "lucide-react";
import widgets from "../../shared/widgets.json";
import { Inspector, Tile } from "./gallery";
import { issueForm, latestApk } from "./links";

export default function Page() {
  return <main id="main">
    <section className="hero">
      <div className="hero-inner wrap">
        <div className="hero-copy">
          <h1>Make it<br/>yours.</h1>
          <p className="lead">Widgets for your Android home screen.</p>
          <div className="actions">
            <a className="button primary" href={latestApk}><Download size={18}/> Download APK</a>
            <a className="hero-link" href="#gallery">Explore widgets <ArrowDown size={16}/></a>
          </div>
          <p className="platform">Android 9 and up</p>
        </div>
        <div className="hero-product" aria-hidden="true">
          <div className="phone">
            <div className="phone-camera" />
            <div className="phone-status"><span>09:41</span><span>76%</span></div>
            <div className="phone-grid">{widgets.map(widget => <Tile key={widget.id} widget={widget}/>)}</div>
            <div className="phone-home" />
          </div>
        </div>
      </div>
    </section>

    <section id="gallery" className="wrap gallery">
      <div className="section-title">
        <h2>Try them on.</h2>
        <p>Pick a widget. Adjust the size, theme, and opacity to see how it would look on your phone.</p>
      </div>
      <Inspector />
    </section>

    <section className="suggest" id="suggest"><div className="wrap suggest-inner">
      <div className="suggest-art" aria-hidden="true"><ImagePlus size={50} strokeWidth={1.2}/></div>
      <div>
        <h2>What should I make next?</h2>
        <p>Send me a screenshot, a sketch, or an idea for something you’d like on your home screen.</p>
        <a className="button primary" href={issueForm}>Suggest a widget <ArrowUpRight size={17}/></a>
      </div>
    </div></section>
  </main>;
}
