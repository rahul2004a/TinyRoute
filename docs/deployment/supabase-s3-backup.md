# Automated Supabase backups to Amazon S3

## Target and implementation boundary

The deployment plan requires an automated daily export of TinyRoute's Supabase
PostgreSQL data to a private Amazon S3 bucket. The job runs on the Hostinger VPS,
independently of the Spring application and release workflow. Vercel frontend,
Supabase database, and private VPS Redis remain unchanged. S3 supplies backup
storage only; no application hosting or database service moves to AWS.

This document defines the automation contract. The repository does not yet
contain an installed scheduler or backup script, and no live Supabase/S3 backup
has been verified. Runtime implementation and installation must satisfy the
checks below before release (NFR-BAK-01..05, NFR-SEC-03/11/12, NFR-PRV-04).

## Schedule and export

- Use a dedicated host `systemd` oneshot service and timer, outside the backend
  container. Run **twice daily at 02:00 and 14:00 UTC** (07:30 and 19:30
  Asia/Kolkata), with persistent catch-up after downtime. This exceeds the
  at-least-daily requirement and leaves margin before the 24-hour RPO deadline.
  Prevent overlapping runs with a lock. Bound each run, including retries, to
  one hour; verify this budget against actual database size/upload speed before
  release. Bound temporary disk use. A failed run must not advance the
  last-success timestamp.
- Connect from the approved VPS egress address using direct PostgreSQL or the
  session pooler on port 5432, `sslmode=verify-full`, and the matching CA. Use a
  dedicated database backup role with sufficient read access to every application
  table and sequence, including future objects. Supply backup secrets through
  the service's externally managed runtime environment, separate from the
  backend. Create a transient mode-0600 libpq password file for the run and
  remove it during cleanup; never put passwords in command arguments, source
  control, or logs (NFR-SEC-03/11).
- Use a supported `pg_dump` client compatible with the selected Supabase server
  and one consistent custom-format export containing application schema/data,
  sequences, constraints, and `flyway_schema_history`. The current application
  uses `public`; review the schema allowlist whenever migrations add another
  schema. Exclude Supabase-managed schemas. Record server/client versions and
  migration version in the backup manifest. Maintain role/grant and extension
  rebuild instructions separately; schema-only exports do not capture global
  roles or external dependencies.
- Inspect the archive with the matching `pg_restore` client before encryption.
  Client-side encrypt the archive and its detailed manifest for an operator-held
  recovery key; the job needs only its encryption recipient. Keep the recovery
  private key in separately secured custody outside both VPS and S3. Use private
  temporary files and clean them up after either success or failure. Do not log
  exported personal data, credentials, connection strings, or manifest contents.

See [PostgreSQL dump semantics](https://www.postgresql.org/docs/current/app-pgdump.html),
[password-file handling](https://www.postgresql.org/docs/current/libpq-pgpass.html),
and [systemd timer behavior](https://manpages.debian.org/bookworm/systemd/systemd.timer.5.en.html).

## S3 upload and access

- Provision a dedicated private bucket in a recorded AWS region, with Block
  Public Access enabled, bucket-owner-enforced ownership, default SSE-S3
  encryption, and a policy denying non-TLS access. Client-side encryption remains
  required so bucket access alone cannot decrypt database data.
- Upload through AWS CLI v2 using HTTPS and checksums, under a prefix such as
  `tinyroute/prod/supabase/YYYY/MM/DD/`, with a unique UTC timestamp/run ID.
  Use `s3api` conditional writes and a bucket policy enforcing `If-None-Match: *`
  for artifact and completion-record creation, including multipart completion.
  Exempt multipart preparation/part operations as required by S3's policy
  model; a plain high-level upload without the required condition is insufficient.
  Reject attempts to replace retained objects even when made by the backup
  identity. Treat a key collision as a failed run, not a reason to overwrite.
  Publish a completion record only after every required encrypted artifact has
  uploaded successfully. An incomplete prefix is not a usable recovery point.
  Treat the S3 ETag as an identifier, not a universal file checksum.
- Give the backup identity only the required upload/multipart permissions on
  that prefix. It must not delete backups, change bucket settings, or access
  application hosting resources. Use separate restricted operator credentials
  for listing, retrieving, and checking backups during recovery. Scope policies
  to the selected bucket/prefix and restrict the job's source where practical.
- Use short-lived credentials when the chosen VPS credential provider supports
  them. A Hostinger VPS has no EC2 instance role. If a dedicated IAM access key
  is necessary for this baseline, supply it only to the backup service's runtime
  environment from a protected host environment file, rotate it, and never
  reuse personal/admin or release credentials. The
  backend container, frontend, and pull-request jobs receive no AWS credentials.

See [S3 security guidance](https://docs.aws.amazon.com/AmazonS3/latest/userguide/security-best-practices.html),
[SSE-S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingServerSideEncryption.html),
[AWS CLI uploads](https://docs.aws.amazon.com/cli/latest/reference/s3/cp.html),
[conditional writes](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html),
[bucket policy enforcement](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes-enforce.html),
[S3 permission mapping](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-policy-actions.html),
and [IAM credential practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html).

## Retention, failures, and recovery

Use S3 lifecycle rules on the backup prefix to retain at least seven days of
complete daily backups, then expire them. Abort abandoned multipart uploads.
If versioning is enabled, also expire noncurrent versions and remove expired
delete markers so deleted personal data does not remain indefinitely. Account
for lifecycle deletion timing and verify actual retained objects; the baseline
is seven days, not a promise that deletion occurs at an exact hour. Do not use
Object Lock retention that conflicts with the selected privacy policy.
See [S3 expiration behavior](https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-expire-general-considerations.html).

Record sanitized run status, backup snapshot time, uploaded keys, size/checksum,
and last complete backup time. Check freshness at least every 15 minutes from
a process independent of the VPS so host failure cannot silence it. Notify the
owner immediately on a failed run, and when the latest complete snapshot is
18 hours old, allowing time for recovery before the 24-hour deadline. Configure
and test the alert recipient/channel and operator response before installation.
Backups during VPS downtime, failed exports, or failed uploads cannot be assumed
to satisfy the RPO; retries and operator response must restore freshness. The
independent check is specific to backup freshness; broader external monitoring
keeps its existing V1 scope. Keep export load within database connection/resource
limits and measure its effect on application latency.

Before release, configure the real bucket/region, dedicated backup identity,
database role/password file, trusted CA, encryption recipient, and operator
recovery key. Verify one complete encrypted upload, independent retrieval,
checksum verification, and decryption. Verify existing-key writes are denied,
including completion records. Verify the timer, missed-run catch-up, one-hour
run budget, failure status, no-overlap behavior, and independent freshness/alert
delivery, including when the VPS is unavailable. Never test against
production credentials from PR CI.

Restore into a clean compatible Supabase project using the operator's recovery
identity/key and recorded rebuild instructions. Confirm schema/data, constraints,
sequences, and Flyway history before startup migrations. Follow the
[deployment recovery runbook](hostinger-vps.md#persistence-backups-and-restore),
including closed traffic, deletion/security reconciliation, surviving Redis
cache/session invalidation, and old access-token invalidation. A recorded clean
restore drill remains NFR-BAK-04 V1 acceptance; the 24-hour RPO and four-hour RTO
remain unchanged.

Supabase's own plan-specific backups remain supplemental. A logical PostgreSQL
export does not include Supabase Storage object files; TinyRoute currently uses
Supabase only for PostgreSQL. Adding object storage would require its own backup
scope. See [Supabase backup coverage](https://supabase.com/docs/guides/platform/backups).
