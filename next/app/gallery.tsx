"use client";

import { useState, type FormEvent } from "react";
import widgets from "../../shared/widgets.json";

type Widget = (typeof widgets)[number];

function Tile({ widget, value = widget.sample }: { widget: Widget; value?: string }) {
  return <div className="tile">
    <span className="tile-icon" aria-hidden="true" style={{color: widget.color}}>{widget.icon}</span>
    <div><strong>{value}</strong><small>{widget.label}</small></div>
  </div>;
}

export default function Gallery({ downloadable }: { downloadable: boolean }) {
  const [selected, select] = useState(widgets[0]);
  const [width, setWidth] = useState(2);
  const [height, setHeight] = useState(2);
  const [count, setCount] = useState(12);
  const issueUrl = "https://github.com/wrestle-R/widgets/issues/new";

  function suggest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const title = String(data.get("title")).trim();
    const idea = String(data.get("idea")).trim();
    const field = event.currentTarget.elements.namedItem(!title ? "title" : "idea") as HTMLInputElement | HTMLTextAreaElement;
    if (!title || !idea) {
      field.setCustomValidity("Enter a name and a description.");
      field.reportValidity();
      field.setCustomValidity("");
      return;
    }
    const params = new URLSearchParams({title: `Widget idea: ${title}`, body: idea});
    window.location.assign(`${issueUrl}?${params}`);
  }

  return <>
    <header className="site-header"><div className="shell nav">
      <a href="#" className="brand"><span className="logo" aria-hidden="true"><i/><i/><i/><i/></span>Widgets</a>
      <nav aria-label="Main"><a href="#gallery">Gallery</a><a href="#suggest">Suggest a widget</a></nav>
    </div></header>
    <main className="shell">
      <section className="hero">
        <div className="intro">
          <h1>Widgets for<br/>your home screen.</h1>
          <p>See your phone unlocks, battery and more without opening an app. Try each widget at different sizes before you add it.</p>
          <div className="actions">
            <a className="button primary" href="#gallery">Explore widgets <span aria-hidden="true">↓</span></a>
            {downloadable && <a className="button secondary" href={`${process.env.NEXT_PUBLIC_BASE_PATH ?? ""}/widgets.apk`} download>Download APK <span aria-hidden="true">↗</span></a>}
          </div>
          <p className="platform">Android 9 and up</p>
        </div>
        <div className="wallpaper hero-wallpaper" aria-label="Sample Android home screen">
          <div className="hero-tiles">{widgets.map(widget => <Tile key={widget.id} widget={widget}/>)}</div>
        </div>
      </section>

      <section id="gallery" className="section">
        <div className="section-heading"><h2>Widget gallery</h2><p>Pick a widget to see how it fits.</p></div>
        <div className="catalog">
          <div className="widget-list" aria-label="Widgets">
            {widgets.map(widget => <button key={widget.id} className="widget-choice" aria-pressed={selected.id === widget.id}
              onClick={() => select(widget)}>
              <span className="choice-icon" aria-hidden="true" style={{color:widget.color}}>{widget.icon}</span>
              <span><strong>{widget.title}</strong><small>{widget.category}</small></span>
              <span className="arrow" aria-hidden="true">›</span>
            </button>)}
          </div>
          <div className="inspector">
            <div className="preview-heading"><h3>{selected.title}</h3><span>Sample data</span></div>
            <p>{selected.description}</p>
            <div className="wallpaper resize-stage">
              <div className="sized" style={{width: `${width * 25}%`, height: `${height * 70}px`}}>
                {selected.id === "counter" ?
                  <button className="interactive-tile" aria-label={`Count is ${count}. Add one`} onClick={() => setCount(count + 1)}><Tile widget={selected} value={String(count)}/></button> :
                  <Tile widget={selected}/>}
              </div>
            </div>
            <div className="size-controls">
              <div><label htmlFor="widget-width">Width</label><output htmlFor="widget-width">{width} cells</output><input id="widget-width" type="range" min="2" max="4" value={width} onChange={e => setWidth(Number(e.target.value))}/></div>
              <div><label htmlFor="widget-height">Height</label><output htmlFor="widget-height">{height} cells</output><input id="widget-height" type="range" min="2" max="3" value={height} onChange={e => setHeight(Number(e.target.value))}/></div>
            </div>
            <p className="hint">Actual dimensions depend on your launcher.</p>
          </div>
        </div>
      </section>

      <section id="suggest" className="section suggestions">
        <div className="section-heading"><h2>Suggest a widget</h2><p>What would you like to see on your home screen?</p></div>
        <form onSubmit={suggest}>
          <label>Widget name<input name="title" required maxLength={80} placeholder="e.g. Days until my next trip"/></label>
          <label>What should it do?<textarea name="idea" rows={4} required maxLength={1000}/></label>
          <button className="button primary">Continue to GitHub</button>
          <p className="hint">Review your issue on GitHub and attach a screenshot if you have one.</p>
        </form>
      </section>
    </main>
    <footer className="shell footer"><a href="#" className="brand">Widgets</a><a href="#gallery">Widget gallery ↑</a></footer>
  </>;
}
