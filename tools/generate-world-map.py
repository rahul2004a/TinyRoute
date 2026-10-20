"""Build bundled SVG paths using the pinned Natural Earth India boundary view.

Usage: uv run --with pyshp==2.3.1 --with shapely==2.1.2 --with numpy==2.5.4 \
    python tools/generate-world-map.py /path/to/ne_10m_admin_0_countries_ind.zip
These tools are generation-only; the application uses just the resulting JSON.
Projection formula adapted from d3-geo 3.1.1; see the accompanying data/NOTICE.md.
"""
import hashlib
import json
import math
from pathlib import Path
import sys
import shapefile
from shapely import coverage_is_valid, coverage_simplify
from shapely.geometry import Point, mapping, shape

SOURCE_SHA256 = "36a54ee97a509325e98eb44fd91bc7e53b0c5139cf10039918042bbd64102192"
WIDTH, HEIGHT, SCALE = 960, 500, 169


def project(longitude, latitude):
    longitude, latitude = math.radians(longitude), math.radians(latitude)
    squared = latitude * latitude
    fourth = squared * squared
    x = longitude * (0.8707 - 0.131979 * squared + fourth * (-0.013791 + fourth * (0.003971 * squared - 0.001529 * fourth)))
    y = latitude * (1.007226 + squared * (0.015085 + fourth * (-0.044475 + 0.028874 * squared - 0.005916 * fourth)))
    return f"{WIDTH / 2 + SCALE * x:.2f},{HEIGHT / 2 - SCALE * y:.2f}"


def line(points, close=False):
    return "M" + "L".join(project(*point) for point in points) + ("Z" if close else "")


source_path = Path(sys.argv[1])
source = source_path.read_bytes()
if hashlib.sha256(source).hexdigest() != SOURCE_SHA256:
    raise SystemExit("Source checksum differs from the reviewed Natural Earth India POV 5.1.1 data.")
with shapefile.Reader(str(source_path)) as reader:
    features = [(item.record.as_dict(), shape(item.shape.__geo_interface__)) for item in reader.iterShapeRecords()]
geometries = [geometry for _, geometry in features]
if not coverage_is_valid(geometries):
    raise SystemExit("Source country boundaries are not a valid shared-edge coverage.")
# Preserve matching international borders while reducing this 1:10m reference
# data for a zoomable 960px SVG. Never simplify neighbors independently.
simplified = coverage_simplify(geometries, tolerance=0.08)
if not coverage_is_valid(simplified):
    raise SystemExit("Simplified country boundaries are not a valid coverage.")
reference_points = [Point(74.3, 35.9), Point(79.8, 35.2), Point(91.86, 27.59)]
grouped_countries = {}
for (properties, _), geometry in zip(features, simplified):
    code = properties["ISO_A2_EH"]
    if code == "IN" and not all(geometry.covers(point) for point in reference_points):
        raise SystemExit("India boundary view lost a required reference location.")
    if code in ("PK", "CN") and any(geometry.covers(point) for point in reference_points):
        raise SystemExit("Neighboring outline overlaps the selected India boundary view.")
    geometry = mapping(geometry)
    polygons = geometry["coordinates"]
    if geometry["type"] == "Polygon":
        polygons = [polygons]
    country = {
        "id": properties["ADM0_A3"],
        "code": code if len(code) == 2 and code.isalpha() else "",
        "label": properties["NAME_EN"],
        "path": "".join(line(ring, close=True) for polygon in polygons for ring in polygon),
    }
    # One interactive shape per country code. Some overseas regions share the
    # parent's code; keep its canonical label instead of the first island name.
    key = country["code"] or country["id"]
    if key in grouped_countries:
        current = grouped_countries[key]
        current["path"] += country["path"]
        if properties["ADM0_A3"] == properties["ISO_A3_EH"]:
            current["id"], current["label"] = country["id"], country["label"]
    else:
        grouped_countries[key] = country
countries = list(grouped_countries.values())
graticule = "".join(line([(longitude, latitude) for latitude in range(-90, 91, 3)]) for longitude in range(-180, 181, 30))
graticule += "".join(line([(longitude, latitude) for longitude in range(-180, 181, 3)]) for latitude in range(-60, 61, 30))
result = {"width": WIDTH, "height": HEIGHT, "boundaryView": "IN", "graticule": graticule, "countries": sorted(countries, key=lambda country: country["id"])}
destination = Path(__file__).resolve().parent.parent / "frontend/src/features/analytics/data/world-map.json"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
print(f"Generated {len(countries)} country shapes ({destination.stat().st_size} bytes).")
