"use client";

import { useState, type CSSProperties } from "react";
import { Activity, Circle, LockKeyholeOpen, Moon, SlidersHorizontal, Sun, Terminal, Ticket } from "lucide-react";
import designs from "../../shared/widgets.json";

type Design = (typeof designs)[number];
const icons = {classic: LockKeyholeOpen, signal: Activity, orbit: Circle, ticket: Ticket, terminal: Terminal};
const sampleBuckets = [0, 2, 16, 21, 19, 14, 9, 4];
const sampleCount = sampleBuckets.reduce((sum, count) => sum + count, 0);

export function UnlockTile({ design, light = false, opacity = 100, width = 2, height = 2 }: {
  design: Design; light?: boolean; opacity?: number; width?: number; height?: number;
}) {
  const shape = Math.min(width, height) === 1 ? "compact" : width / height >= 1.45 ? "wide" : height / width >= 1.45 ? "tall" : "square";
  return <div className="tile unlock-tile" data-design={design.id} data-light={light} data-shape={shape}
    role="img" aria-label={`${design.title} sample widget, ${sampleCount} phone unlocks today`}
    style={{"--tile-opacity": opacity / 100, "--tile-color": design.color} as CSSProperties}>
    {design.id === "classic" && <span className="tile-icon" aria-hidden="true"><LockKeyholeOpen size={17}/></span>}
    {design.id === "terminal" && <span className="terminal-title" aria-hidden="true">UNLOCKS</span>}
    {design.id === "orbit" && <i className="orbit-ring" aria-hidden="true"/>}
    <div className="tile-reading" aria-hidden="true"><strong>{sampleCount}</strong><small>Unlocks today</small></div>
    {design.id === "signal" && <div className="signal-bars" aria-hidden="true">
      {sampleBuckets.map((value, index) => <i key={index} style={{height:`${Math.max(6, value / 21 * 100)}%`}} />)}
    </div>}
  </div>;
}

export function Inspector() {
  const [selected, select] = useState(designs[0]);
  const [width, setWidth] = useState(2);
  const [height, setHeight] = useState(2);
  const [light, setLight] = useState(false);
  const [opacity, setOpacity] = useState(100);

  return <div className="studio">
    <div className="widget-list" role="group" aria-label="Widget design">
      {designs.map(design => {
        const Icon = icons[design.id as keyof typeof icons];
        return <button key={design.id} className="widget-choice" aria-pressed={selected.id === design.id}
          onClick={() => select(design)}><Icon size={17} aria-hidden="true"/> {design.title}</button>;
      })}
    </div>
    <div className="workbench">
      <div className="resize-stage" data-light={light}>
        <span className="stage-label">Sample preview</span>
        <div className="sized" style={{"--size-width": width, "--size-height": height} as CSSProperties}>
          <UnlockTile design={selected} opacity={opacity} light={light} width={width} height={height}/>
        </div>
      </div>
      <div className="inspector">
        <div className="inspector-title"><SlidersHorizontal size={17}/><h3>{selected.title}</h3></div>
        <p>{selected.description}</p>
        <div className="size-controls">
          <div>
            <label htmlFor="width">Width</label><output htmlFor="width">{width} cells</output>
            <input id="width" type="range" min={1} max={4} value={width}
              onChange={event => setWidth(Number(event.currentTarget.value))} />
          </div>
          <div>
            <label htmlFor="height">Height</label><output htmlFor="height">{height} cells</output>
            <input id="height" type="range" min={1} max={4} value={height}
              onChange={event => setHeight(Number(event.currentTarget.value))} />
          </div>
          <div>
            <label htmlFor="opacity">Background opacity</label><output htmlFor="opacity">{opacity}%</output>
            <input id="opacity" type="range" min={60} max={100} value={opacity}
              onChange={event => setOpacity(Number(event.currentTarget.value))} />
          </div>
        </div>
        <div className="theme-control" role="group" aria-label="Widget theme">
          <button aria-pressed={!light} onClick={() => setLight(false)}><Moon size={15}/> Dark</button>
          <button aria-pressed={light} onClick={() => setLight(true)}><Sun size={15}/> Light</button>
        </div>
        <p className="hint">Drag the widget’s edges to resize it on your Android home screen.</p>
      </div>
    </div>
  </div>;
}
