"use client";

import { useSyncExternalStore, type ReactNode } from "react";

const subscribe = () => () => undefined;
const clientSnapshot = () => true;
const serverSnapshot = () => false;

export function LandingHeroLights({
  children,
}: Readonly<{ children: ReactNode }>) {
  const ready = useSyncExternalStore(subscribe, clientSnapshot, serverSnapshot);

  return (
    <div
      className="landing-hero-lights"
      data-ready={ready}
      onAnimationIteration={(event) => {
        if (
          event.animationName === "landing-light-glow" &&
          event.target instanceof SVGElement
        ) {
          const animation = event.target
            .getAnimations()
            .find(
              (animation) =>
                animation instanceof CSSAnimation &&
                animation.animationName === event.animationName,
            );
          if (!animation?.effect) return;

          // Start a complete new cycle at the dim boundary. Changing CSS
          // duration alone would retime the entire elapsed animation.
          animation.effect.updateTiming({
            duration: (8 + Math.random() * 6) * 1000,
            delay: 0,
          });
          animation.cancel();
          animation.play();
        }
      }}
    >
      {children}
    </div>
  );
}
