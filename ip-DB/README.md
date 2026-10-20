# Local City database

The operator-provided `dbip-city-lite-2026-10.mmdb` is DB-IP City Lite,
October 2026. The database is kept outside Git and application build artifacts.

- Source: [DB-IP City Lite downloads](https://db-ip.com/db/download/ip-to-city-lite).
- Attribution: [IP Geolocation by DB-IP](https://db-ip.com).
- License: [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
- Unmodified source SHA-1: `df17bd24390108ed1d24dcb7718dc60f6b9abd4f`.

Set `ANALYTICS_GEO_DATABASE_PATH` in the ignored root `.env` to the absolute
path of the downloaded file. Export the environment before starting Spring Boot;
Spring does not load `.env` automatically. Restart the backend after changes.
See [analytics operations](../docs/spec/short-link-analytics/operations.md).

The analytics overview and detail footer include the provider's required link.
Keep this attribution while using DB-IP Lite. Operators obtain monthly updates
and replace the file atomically, then restart the backend. Production needs its
own read-only mount and environment path; never commit the database or bake it
into the backend image. Lookups are approximate and local/private addresses
remain Unknown.
