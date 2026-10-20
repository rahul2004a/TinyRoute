import { expect, test } from "vitest";
import world from "./world-map.json";

// Ray casting against generated SVG rings independently checks the shipped
// outline, rather than trusting a source/profile label on the asset.
function contains(path: string, x: number, y: number) {
  let inside = false;
  for (const ring of path.matchAll(/M([^Z]+)Z/g)) {
    const points = ring[1]
      .split("L")
      .map((point) => point.split(",").map(Number));
    for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
      const [xi, yi] = points[i],
        [xj, yj] = points[j];
      if (yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi)
        inside = !inside;
    }
  }
  return inside;
}

test("the bundled India boundary view includes its northern outline without neighboring overlap", () => {
  const india = world.countries.find((country) => country.code === "IN")!;
  const neighbors = world.countries.filter((country) =>
    ["PK", "CN"].includes(country.code),
  );
  // Fixed Natural Earth projection coordinates of Gilgit and Aksai Chin.
  for (const [x, y] of [
    [659.0, 142.94],
    [672.76, 145.02],
  ]) {
    expect(contains(india.path, x, y)).toBe(true);
    for (const neighbor of neighbors)
      expect(contains(neighbor.path, x, y)).toBe(false);
  }
});
