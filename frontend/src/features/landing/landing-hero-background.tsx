import type { CSSProperties } from "react";

import { LandingHeroLights } from "./landing-hero-lights";

const routePoints = [
  [440, 180],
  [620, 130],
  [800, 80],
  [440, 420],
  [620, 370],
  [800, 320],
];

export function LandingHeroBackground() {
  return (
    <LandingHeroLights>
      <div aria-hidden="true" className="landing-hero-background">
        <svg viewBox="0 0 900 620" fill="none" focusable="false">
          <defs>
            <filter
              id="landing-route-halo"
              x="-200%"
              y="-200%"
              width="500%"
              height="500%"
            >
              <feGaussianBlur stdDeviation="12" />
            </filter>
          </defs>
          <g className="landing-grid-lines" stroke="currentColor">
            {Array.from({ length: 14 }, (_, index) => (
              <path key={`column-${index}`} d={`M${index * 70} 0v620`} />
            ))}
            {Array.from({ length: 13 }, (_, index) => (
              <path key={`row-${index}`} d={`M0 ${index * 60}l900 -250`} />
            ))}
          </g>
          <g className="landing-route-lines" stroke="currentColor">
            <path d="M0 302.22 900 52.22M0 542.22 900 292.22" />
            <path d="M440 0v620M620 0v620M800 0v620" />
          </g>
          {routePoints.map(([x, y], index) => (
            <g key={`${x}-${y}`} transform={`translate(${x} ${y}) rotate(12)`}>
              <ellipse
                className="landing-route-rim"
                rx="17"
                ry="23"
                stroke="currentColor"
                strokeWidth="2"
              />
              <g
                className="landing-route-light"
                style={
                  {
                    "--light-duration": `${8 + index * 1.13}s`,
                    "--light-delay": `${-index * 2.37}s`,
                  } as CSSProperties
                }
              >
                <ellipse
                  className="landing-route-halo"
                  rx="24"
                  ry="32"
                  filter="url(#landing-route-halo)"
                />
                <ellipse className="landing-route-center" rx="8" ry="12" />
              </g>
            </g>
          ))}
        </svg>
      </div>
    </LandingHeroLights>
  );
}
