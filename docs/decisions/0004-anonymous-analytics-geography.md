# ADR 0004: Anonymous analytics and offline geography

- Status: Accepted through the short-link-analytics design approval
- Date: 2026-10-20
- Requirements: FR-ANA-01..04, NFR-CON-03, NFR-PRV-02/03, NFR-PER-01/03

Successful completed GET redirects are recorded by a dedicated bounded Spring
`@Async` executor. PostgreSQL atomically inserts each normalized event and
increments the link's lifetime count. Reports aggregate events at query time
inside owner-scoped repeatable-read transactions. No broker, visitor identity,
or Redis analytics store is introduced.

Use `com.maxmind.geoip2:geoip2:5.2.0` for local country/city lookup against an
operator-supplied City `.mmdb`. FR-ANA-04 needs a maintained geographic data
reader; implementing the binary database format ourselves would add risk and
maintenance. The [official Java API](https://maxmind.github.io/GeoIP2-java/#database-usage)
supports a reusable local file reader and optional results. The application
never uses its HTTP web-service API, sends visitor addresses to a provider, or
caches geography by visitor address.

The user supplied DB-IP City Lite (October 2026) for the local runtime. Its City
MMDB is compatible with this same reader; reference lookups were verified
without adding a dependency or remote service. The file stays outside Git and
the application image. [DB-IP Lite's CC BY 4.0 terms](https://db-ip.com/db/lite.php)
require a provider link on pages using its results; both report footers supply
that attribution. Operators own database updates and provider licensing.

Only country codes and city names are retained. Client addresses exist
transiently in bounded task memory and are discarded; raw user agents,
referrer paths/queries, coordinates, postal codes, and visitor hashes are never
persisted. Device, OS, and browser use coarse recognized families without an
additional parser dependency. Geography is approximate and unknown data is
explicitly labeled.

An omitted database path gives Unknown geography; an explicitly configured but
invalid file fails startup safely. Operators supply/update the licensed database
as a read-only file, separately from runtime secrets. Builds/tests need no
provider account or production database. The test fixture is MaxMind's synthetic
City database, with provenance recorded beside it.

Alternatives considered: remote geolocation would disclose visitor addresses
and add network latency/failure policy; ingress geography headers are absent
from the selected Caddy topology and must not be trusted from clients; omitting
an implementation entirely would leave FR-ANA-04 unsupported. Precomputed
cubes and a separate durable queue remain outside the portfolio MVP.
