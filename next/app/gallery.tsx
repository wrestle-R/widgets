"use client";

import { useState, type CSSProperties } from "react";
import { BatteryCharging, CalendarDays, LockKeyholeOpen, Plus, SlidersHorizontal, Sun, Moon } from "lucide-react";
import widgets from "../../shared/widgets.json";

type Widget = (typeof widgets)[number];
const icons = { unlocks: LockKeyholeOpen, battery: BatteryCharging, date: CalendarDays, counter: Plus };
const settings = [
  {key:"width", name:"Width", min:2, max:4, unit:" cells"},
  {key:"height", name:"Height", min:2, max:3, unit:" cells"},
  {key:"opacity", name:"Background opacity", min:60, max:100, unit:"%"},
] as const;
const defaultSize = {width:2, height:2, opacity:100, light:false};

export function Tile({ widget, value = widget.sample, opacity = 100, light = false }: { widget: Widget; value?: string; opacity?: number; light?: boolean }) {
  const Icon = icons[widget.id as keyof typeof icons];
  return <div className="tile" data-light={light} style={{"--tile-opacity": opacity / 100, "--tile-color": widget.color} as CSSProperties}>
    <span className="tile-icon"><Icon size={18} strokeWidth={2}/></span>
    <div><strong>{value}</strong><small>{widget.label}</small></div>
  </div>;
}

export function Inspector() {
  const [selected, select] = useState(widgets[0]);
  const [sizes, setSizes] = useState<Record<string, typeof defaultSize>>({});
  const [count, setCount] = useState(12);
  const size = sizes[selected.id] ?? defaultSize;

  return <div className="studio">
    <div className="widget-list" aria-label="Choose a widget">
      {widgets.map(widget => {
        const Icon = icons[widget.id as keyof typeof icons];
        return <button key={widget.id} className="widget-choice" aria-pressed={selected.id === widget.id}
          onClick={() => select(widget)}><Icon size={18}/> {widget.title}</button>;
      })}
    </div>
    <div className="workbench">
      <div className="resize-stage">
        <span className="stage-label">Sample data</span>
        <div className="sized" style={{width:`${size.width * 24}%`, height:`${size.height * 67}px`}}>
          {selected.id === "counter" ?
            <button className="interactive-tile" aria-label={`Count is ${count}. Add one`} onClick={() => setCount(n => n + 1)}>
              <Tile widget={selected} opacity={size.opacity} light={size.light} value={String(count)} />
            </button> :
            <Tile widget={selected} opacity={size.opacity} light={size.light} />}
        </div>
      </div>
      <div className="inspector">
        <div className="inspector-title"><SlidersHorizontal size={17}/><h3>{selected.title}</h3></div>
        <p>{selected.description}</p>
        <div className="size-controls">
          {settings.map(({key, name, min, max, unit}) => <div key={key}>
            <label htmlFor={key}>{name}</label><output htmlFor={key}>{size[key]}{unit}</output>
            <input id={key} type="range" min={min} max={max} value={size[key]} onChange={e => {
              const value = Number(e.currentTarget.value);
              setSizes(previous => ({...previous, [selected.id]: {...size, [key]: value}}));
            }}/>
          </div>)}
        </div>
        <div className="theme-control" role="group" aria-label="Widget theme">
          <button aria-pressed={!size.light} onClick={() => setSizes(previous => ({...previous, [selected.id]: {...size, light:false}}))}><Moon size={15}/> Dark</button>
          <button aria-pressed={size.light} onClick={() => setSizes(previous => ({...previous, [selected.id]: {...size, light:true}}))}><Sun size={15}/> Light</button>
        </div>
        <p className="hint">Cell dimensions depend on your Android launcher.</p>
      </div>
    </div>
  </div>;
}
