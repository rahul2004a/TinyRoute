# Reproducing the local performance run

The complete local protocol passed on 2026-10-10. Its
[raw numeric report](link-performance.json.gz) and
[verification summary](verification.md) establish local AC-10 evidence only.
The earlier [failed report](failed-local-load.json.gz) retains its separate
[failed-run metadata](failed-performance-environment.json).

This is disposable verification infrastructure for NFR-PER-01–03 and FR-ABS-01/03.
It is not a deployment definition. The measured runtime images and hardware are
recorded in [performance-environment.json](performance-environment.json).

## Preconditions

Use Node 24, Java 21, Docker Desktop and the existing trusted local TLS files.
Run the full backend verification first so `backend/target/test-classes`,
`backend/target/classes`, Maven dependencies and Failsafe XML reports exist.
The application entry point is `com.tinyroute.LinkVerificationApplication` on the
test classpath; the application JAR inside the runtime image is never executed.
The private directory below and all credentials stay ignored by Git.

The recorded Java runtime can be recreated from the pinned `runtime-base` target
in `backend/Dockerfile`; the Node container runs only the TCP relay. The measured
local image IDs identify the exact existing images used for this report. Retain
those images for an identical local rerun, or record replacement runtime versions
and digests with any new report.

## Prepare private files

From the repository root, run this Python setup. It derives the classpath from
successful integration-test output and generates a fresh token. It never reads
the owner's Compose database or production credentials.

```python
from pathlib import Path
import os
import secrets
import xml.etree.ElementTree as ET

root = Path.cwd()
private = root / ".local-verification/perf-rerun"
private.mkdir(parents=True, exist_ok=True, mode=0o700)
report = next((root / "backend/target/failsafe-reports").glob("TEST-*.xml"))
properties = {
    item.attrib["name"]: item.attrib["value"]
    for item in ET.parse(report).getroot().findall("./properties/property")
}
classpath = properties.get("surefire.test.class.path") or properties["java.class.path"]
token = secrets.token_urlsafe(48)
values = {
    "SPRING_PROFILES_ACTIVE": "dev",
    "DOCKER_HOST": "unix:///var/run/docker.sock",
    "TESTCONTAINERS_HOST_OVERRIDE": "host.docker.internal",
    "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE": "/var/run/docker.sock",
    "TINYROUTE_VERIFICATION_TOKEN": token,
    "TINYROUTE_VERIFICATION_PORT": "8444",
    "MAIL_PORT": "1026",
    "ALLOWED_FRONTEND_ORIGINS": "https://localhost:3001",
    "PASSWORD_RESET_CONFIRMATION_URI": "https://localhost:3001/password-reset/confirm",
    "DEV_TLS_CERTIFICATE": f"file:{root}/.local-certs/localhost.pem",
    "DEV_TLS_PRIVATE_KEY": f"file:{root}/.local-certs/localhost-key.pem",
}
for name, contents in {
    "classpath.txt": classpath,
    "backend.env": "".join(f"{key}={value}\n" for key, value in values.items()),
    "driver.env": f"TINYROUTE_VERIFICATION_TOKEN={token}\n",
}.items():
    file = private / name
    file.write_text(contents)
    file.chmod(0o600)
```

Save this exact relay source as
`.local-verification/perf-rerun/tcp-proxy.mjs`. It handles byte streams only;
Java terminates TLS. Its fixed upstream bind preserves the fixture's existing
`127.0.0.2/32` trusted-proxy entry. It never adds or edits a header.

```javascript
import net from "node:net";
const server = net.createServer((client) => {
  const upstream = net.connect({
    host: "127.0.0.1",
    port: 8444,
    localAddress: "127.0.0.2",
  });
  client.on("error", () => upstream.destroy());
  upstream.on("error", () => client.destroy());
  client.pipe(upstream);
  upstream.pipe(client);
  client.on("close", () => upstream.destroy());
  upstream.on("close", () => client.destroy());
});
server.listen(9444, "0.0.0.0", () =>
  console.log("Local TLS pass-through proxy listening"),
);
```

## Launch the recorded topology

This Python block uses the recorded runtime images and structured Docker arguments.
It mounts the current project and Maven cache read-only at their original absolute
paths, which must match the derived classpath. The Docker socket is used solely
to create the disposable PostgreSQL 17.2 and Redis 7.4.2 Testcontainers. Root in
this verification container allows that local socket; no production runtime user
or permissions are changed. No backend or Redis service is exposed publicly.

```python
from pathlib import Path
import json
import os
import secrets
import subprocess

root = Path.cwd()
private = root / ".local-verification/perf-rerun"
metadata = json.loads((root / "docs/spec/link-creation-and-redirection/performance-environment.json").read_text())
backend = "tinyroute-link-perf-backend-" + secrets.token_hex(4)
proxy = backend.replace("backend", "proxy")
maven = Path.home() / ".m2/repository"
subprocess.run([
    "docker", "run", "-d", "--name", backend,
    "--label", "com.tinyroute.verification=link-performance",
    "--user", "0:0", "--network", "bridge",
    "--publish", "127.0.0.1:8444:9444", "--entrypoint", "java",
    "--workdir", str(root / "backend"),
    "--env-file", str(private / "backend.env"),
    "--mount", f"type=bind,source={root},target={root},readonly",
    "--mount", f"type=bind,source={maven},target={maven},readonly",
    "--mount", "type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock",
    metadata["backendRuntimeImage"], "-cp",
    (private / "classpath.txt").read_text().strip(),
    "com.tinyroute.LinkVerificationApplication",
], check=True)
subprocess.run([
    "docker", "run", "-d", "--name", proxy,
    "--label", "com.tinyroute.verification=link-performance",
    "--network", "container:" + backend,
    "--user", f"{os.getuid()}:{os.getgid()}", "--entrypoint", "node",
    "--mount", f"type=bind,source={private}/tcp-proxy.mjs,target=/work/tcp-proxy.mjs,readonly",
    metadata["proxyRuntimeImage"], "/work/tcp-proxy.mjs",
], check=True)
(private / "container-names.json").write_text(json.dumps([backend, proxy]))
```

Wait for Java startup, then require a verified HTTPS `200 UP` health response.
Also require `200` from `/__verification/bootstrap` with the private
`X-Verification-Token` header. That access succeeds only when the unchanged
fixture guard accepts the relay's actual source. Do not print the bootstrap
body, which contains disposable login credentials. A cold Redis health connection
may need a subsequent check; do not change its timeout or start load before UP.

## Run and retain evidence

Load only the token from the private driver file into the native Node process's
environment. Set `NODE_EXTRA_CA_CERTS` to the existing mkcert `rootCA.pem` path.
On the measured Mac it is under the standard mkcert application-support directory.
Keep certificate and hostname verification enabled. The host driver reaches the
local proxy on 8444; the proxy reaches the Java TLS listener on the same port in
its separate Linux namespace. There is no TLS termination or redirect following.

```sh
node --test tools/link-performance.test.mjs
node tools/link-performance.mjs --via-local-proxy --api-base https://localhost:8444 --rate 100 --duration 600 --warmup 30 --create-samples 200 --clients 100 --output .local-verification/perf-rerun/link-performance.json
```

On macOS the measured process was wrapped with `caffeinate -i` to prevent idle
sleep. Keep the laptop on AC power with its **lid open** throughout setup and
measurement. `caffeinate -i` did not prevent lid-closed sleep: an earlier retry
recorded `Clamshell Sleep` for about 40 seconds and failed strict issuance.
The passing attempt had no overlapping sleep events in macOS power history.
Creation samples use three fixture accounts, omit aliases, obtain fresh
CSRF and never retry POST. Redirects use 100 codes and 100 independent forwarded
client identities, with default caps enabled. Preserve every server/client sample,
status, failure and issuance count. Do not reuse consumed creation quotas: stop
only these named containers and restart fresh disposable data before rerunning.

The 30-second warm-up is excluded. Cache entries have their normal five-second
maximum lifetime, so the 600-second measurement includes repeated cache refills.
The timing collector wraps the complete Spring chain using `System.nanoTime`;
client round trips include the relay hop and are reported separately. A passing
run requires all 60,000 requests issued/completed, no dropped/excessively late
arrivals, complete server samples, strict latency/error targets, and creation
p95 below 500 ms. No application limits or proxy trust entries are relaxed.

Afterward, stop and remove only the backend/proxy names recorded above.
Testcontainers/Ryuk owns cleanup of that backend's disposable datastores. Keep
the report private until its contents are checked for identifiers; the report
contains counts, durations and fixed protocol metadata only. Commit sanitized
numeric evidence and runtime metadata alongside the human verification record.
