# Bundled world map

`world-map.json` contains static SVG paths, not visitor information. No map
provider receives requests. Following the user's boundary correction, the map
uses Natural Earth 1:10m **India POV**, dataset version **5.1.1**:

- [Source and boundary-view description](https://www.naturalearthdata.com/blog/admin-0-countries-point-of-views/)
- [Source archive](https://naciscdn.org/naturalearth/10m/cultural/ne_10m_admin_0_countries_ind.zip)
- Source SHA-256: `36a54ee97a509325e98eb44fd91bc7e53b0c5139cf10039918042bbd64102192`
- [Natural Earth terms](https://www.naturalearthdata.com/about/terms-of-use/): the
  geographic data is in the public domain.

Regenerate from the repository root:

```sh
uv run --with pyshp==2.3.1 --with shapely==2.1.2 --with numpy==2.5.4 python tools/generate-world-map.py /path/to/ne_10m_admin_0_countries_ind.zip
```

These are pinned generation-only tools; none are application dependencies. The
generator checks the archive hash, validates the country polygon coverage,
simplifies shared edges together at tolerance 0.08 degrees, projects and rounds
coordinates to two decimal places, reads codes from `ISO_A2_EH`, and retains
unrecognized boundary shapes as neutral land. Shared-edge simplification keeps
neighboring outlines consistent. The 248 source shapes are grouped into 243
country records by code, using the parent country's canonical label for grouped
overseas regions. The asset records `boundaryView: IN`. Missing geometry is
reported explicitly beside the map and counts remain in the Countries table.

The selected view was compared with the official
[Survey of India outline](https://surveyofindia.gov.in/pages/outline-maps-of-india).
Generator checks and the shipped-geometry regression cover northern reference
locations and ensure neighboring polygons do not overlap them. Geometry is
generalized for analytics. Country codes and counts continue to come from the
existing aggregate API.
No projection or mapping dependency runs in the application.

The Natural Earth projection formula in the generator is adapted from
[d3-geo 3.1.1](https://github.com/d3/d3-geo/blob/v3.1.1/src/projection/naturalEarth1.js)
under this license:

> Copyright 2010-2024 Mike Bostock
>
> Permission to use, copy, modify, and/or distribute this software for any purpose
> with or without fee is hereby granted, provided that the above copyright notice
> and this permission notice appear in all copies.
>
> THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
> REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
> FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
> INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
> OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
> TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
> THIS SOFTWARE.
