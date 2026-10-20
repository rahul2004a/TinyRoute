"use client";

import { Minus, Plus, RotateCcw } from "lucide-react";
import { useId, useRef, useState } from "react";
import type { CSSProperties, KeyboardEvent, PointerEvent } from "react";
import type { Analytics } from "./analytics-api";
import world from "./data/world-map.json";

type Geography = NonNullable<Analytics["geography"]>;
type View = { zoom: number; x: number; y: number };
const initialView: View = { zoom: 1, x: 0, y: 0 };
const mappedCodes = new Set(world.countries.map((country) => country.code));
const number = new Intl.NumberFormat("en");

function bounded(view: View): View {
  const zoom = Math.max(1, Math.min(8, view.zoom));
  const horizontal = ((zoom - 1) * world.width) / 2;
  const vertical = ((zoom - 1) * world.height) / 2;
  return {
    zoom,
    x: Math.max(-horizontal, Math.min(horizontal, view.x)),
    y: Math.max(-vertical, Math.min(vertical, view.y)),
  };
}

export function AnalyticsCountryMap({ geography }: { geography: Geography }) {
  const id = useId();
  const [view, setView] = useState(initialView);
  const [selectedCode, setSelectedCode] = useState<string | null>(null);
  const drag = useRef<{ id: number; x: number; y: number; view: View } | null>(
    null,
  );
  const counts = new Map(
    geography.countries.map((country) => [country.countryCode, country]),
  );
  const maximum = Math.max(
    1,
    ...geography.countries.map((country) => country.count),
  );
  const unplotted = geography.countries.reduce(
    (sum, country) =>
      sum + (mappedCodes.has(country.countryCode) ? 0 : country.count),
    0,
  );
  const selected = geography.countries.find(
    (country) => country.countryCode === selectedCode,
  );
  const shape = world.countries.find(
    (country) => country.code === selectedCode,
  );

  function zoomBy(amount: number) {
    setView((current) => {
      const zoom = Math.max(1, Math.min(8, current.zoom + amount));
      return bounded({
        zoom,
        x: current.x * (zoom / current.zoom),
        y: current.y * (zoom / current.zoom),
      });
    });
  }
  function keyboard(event: KeyboardEvent<HTMLDivElement>) {
    if (event.ctrlKey || event.metaKey || event.altKey) return;
    if (event.key === "+" || event.key === "=") zoomBy(1);
    else if (event.key === "-") zoomBy(-1);
    else if (event.key === "Home") setView(initialView);
    else if (
      ["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)
    ) {
      setView((current) =>
        bounded({
          ...current,
          x:
            current.x +
            (event.key === "ArrowLeft"
              ? 60
              : event.key === "ArrowRight"
                ? -60
                : 0),
          y:
            current.y +
            (event.key === "ArrowUp"
              ? 60
              : event.key === "ArrowDown"
                ? -60
                : 0),
        }),
      );
    } else return;
    event.preventDefault();
  }
  function startDrag(event: PointerEvent<HTMLDivElement>) {
    if (view.zoom === 1 || !event.isPrimary || event.button !== 0) return;
    drag.current = {
      id: event.pointerId,
      x: event.clientX,
      y: event.clientY,
      view,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  }
  function moveDrag(event: PointerEvent<HTMLDivElement>) {
    const start = drag.current;
    if (!start || start.id !== event.pointerId) return;
    const rectangle = event.currentTarget.getBoundingClientRect();
    const unit = Math.min(
      rectangle.width / world.width,
      rectangle.height / world.height,
    );
    if (unit === 0) return;
    setView(
      bounded({
        ...start.view,
        x: start.view.x + (event.clientX - start.x) / unit,
        y: start.view.y + (event.clientY - start.y) / unit,
      }),
    );
  }
  return (
    <section className="analytics-map" aria-labelledby={`${id}-title`}>
      <div className="analytics-section-heading analytics-map-heading">
        <div>
          <h2 id={`${id}-title`}>Traffic by country</h2>
          <p className="analytics-muted">
            Successful redirects in the selected range · Approximate geography
          </p>
        </div>
        <div
          className="analytics-map-controls"
          role="group"
          aria-label="Map controls"
        >
          <button
            type="button"
            className="settings-button"
            aria-label="Zoom out"
            disabled={view.zoom === 1}
            onClick={() => zoomBy(-1)}
          >
            <Minus size={18} aria-hidden="true" />
          </button>
          <span aria-label="Map zoom" aria-live="polite">
            {view.zoom}×
          </span>
          <button
            type="button"
            className="settings-button"
            aria-label="Zoom in"
            disabled={view.zoom === 8}
            onClick={() => zoomBy(1)}
          >
            <Plus size={18} aria-hidden="true" />
          </button>
          <button
            type="button"
            className="settings-button"
            aria-label="Reset map"
            onClick={() => {
              setView(initialView);
              setSelectedCode(null);
            }}
          >
            <RotateCcw size={16} aria-hidden="true" />
            <span>Reset</span>
          </button>
        </div>
      </div>
      <p id={`${id}-instructions`} className="sr-only">
        Use plus and minus to zoom, arrow keys to pan, and Home to reset. The
        Countries table contains the same counts.
      </p>
      <div
        className="analytics-map-viewport"
        role="group"
        aria-roledescription="interactive map"
        aria-label="World map of redirect countries"
        aria-describedby={`${id}-instructions`}
        tabIndex={0}
        data-zoomed={view.zoom > 1}
        onKeyDown={keyboard}
        onPointerDown={startDrag}
        onPointerMove={moveDrag}
        onPointerUp={() => {
          drag.current = null;
        }}
        onPointerCancel={() => {
          drag.current = null;
        }}
        onLostPointerCapture={() => {
          drag.current = null;
        }}
      >
        <svg
          viewBox={`0 0 ${world.width} ${world.height}`}
          role="group"
          aria-label="Country boundaries"
        >
          <g
            transform={`translate(${world.width / 2 + view.x} ${world.height / 2 + view.y}) scale(${view.zoom}) translate(${-world.width / 2} ${-world.height / 2})`}
          >
            <path
              d={world.graticule}
              className="analytics-map-grid"
              aria-hidden="true"
            />
            {world.countries.map((country) => {
              const count = counts.get(country.code)?.count ?? 0;
              const label = counts.get(country.code)?.label ?? country.label;
              return (
                <path
                  key={country.id}
                  d={country.path}
                  role={country.code ? "button" : undefined}
                  tabIndex={count > 0 ? 0 : undefined}
                  aria-label={
                    country.code
                      ? `${label}: ${number.format(count)} redirects`
                      : undefined
                  }
                  aria-hidden={!country.code || undefined}
                  aria-pressed={
                    country.code ? selectedCode === country.code : undefined
                  }
                  className="analytics-map-country"
                  data-active={count > 0}
                  data-selected={
                    country.code !== "" && selectedCode === country.code
                  }
                  fillRule="evenodd"
                  vectorEffect="non-scaling-stroke"
                  style={
                    {
                      "--map-intensity": `${25 + 70 * Math.sqrt(count / maximum)}%`,
                    } as CSSProperties
                  }
                  onPointerEnter={() => {
                    if (country.code && !drag.current)
                      setSelectedCode(country.code);
                  }}
                  onFocus={() => setSelectedCode(country.code)}
                  onClick={() => {
                    if (country.code) setSelectedCode(country.code);
                  }}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      event.stopPropagation();
                      setSelectedCode(country.code);
                    }
                  }}
                >
                  <title>
                    {label}: {number.format(count)} redirects
                  </title>
                </path>
              );
            })}
          </g>
        </svg>
      </div>
      <div className="analytics-map-caption">
        <p className="analytics-map-selection" role="status">
          {shape
            ? `${selected?.label ?? shape.label} · ${number.format(selected?.count ?? 0)} redirects`
            : "Hover or select a country to see its count."}
        </p>
        <div
          className="analytics-map-legend"
          aria-label="Country shading: stronger intensity means more redirects"
        >
          <span
            className="analytics-map-swatch analytics-map-swatch-empty"
            aria-hidden="true"
          />
          <span>None</span>
          <span className="analytics-map-scale" aria-hidden="true">
            {[25, 42, 60, 77, 95].map((intensity) => (
              <span
                key={intensity}
                style={{ "--map-intensity": `${intensity}%` } as CSSProperties}
              />
            ))}
          </span>
          <span>More</span>
        </div>
      </div>
      {geography.countries.length === 0 ? (
        <p className="analytics-muted analytics-map-notice">
          {geography.unknownClicks
            ? "Country information is unavailable for these redirects."
            : "No redirects in this date range."}
        </p>
      ) : null}
      {geography.unknownClicks > 0 ? (
        <p className="analytics-muted analytics-map-notice">
          Unknown country: {number.format(geography.unknownClicks)} redirects.
        </p>
      ) : null}
      {unplotted > 0 ? (
        <p className="analytics-muted analytics-map-notice">
          {number.format(unplotted)} redirects are from countries not shown on
          this map. See the Countries table.
        </p>
      ) : null}
      <p className="analytics-map-credit analytics-muted">
        Country outlines:{" "}
        <a
          href="https://www.naturalearthdata.com/"
          target="_blank"
          rel="noreferrer"
        >
          Natural Earth
        </a>
        . India boundary view. Drag to pan when zoomed.
      </p>
    </section>
  );
}
