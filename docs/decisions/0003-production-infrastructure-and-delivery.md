# ADR 0003: Production infrastructure and delivery

- Status: Accepted
- Date: 2026-09-14
- Owners: TinyRoute maintainers
- Implementation status: Future work; this record does not provision resources
  or add deployment workflows.

## Context

TinyRoute needs a production topology and delivery contract before application
bootstrap. The solution must remain understandable for one developer, keep the
Spring Boot backend and datastores private, avoid static AWS credentials, and
satisfy NFR-SEC-01/03/11/12 and NFR-DEP-01/05..09. The frontend hosting decision
in this record supersedes only the frontend-container detail in ADR 0001.

## Decision

Host the Next.js frontend on Vercel. A future GitHub Actions workflow will use
the Vercel CLI for preview deployments from trusted pull requests and production
deployments from `main`. `VERCEL_TOKEN` is a GitHub secret; Vercel organization
and project identifiers are non-secret repository variables. Vercel is not
managed by the AWS Terraform configuration.

Host the Spring Boot backend on Amazon ECS Fargate in `ap-south-1`. Package it
as one reproducible Docker image, push immutable commit-addressed images to a
private Amazon ECR repository, and deploy by image digest. ECS deployment
failure detection provides automatic rollback. CloudWatch receives backend
container logs.

Use an existing Route53 hosted zone with three hostnames:

- `app.<zone>` points to Vercel.
- `api.<zone>` points to the AWS Application Load Balancer for Spring APIs.
- `go.<zone>` points to the same load balancer for public `/{code}` redirects.

Vercel terminates TLS for `app.<zone>`. ACM and the load balancer terminate TLS
for `api.<zone>` and `go.<zone>`; HTTP redirects to HTTPS. The browser calls the
API with credentials from the exact allowed `app.<zone>` origin. Authentication
cookies are host-only to `api.<zone>` so public redirect requests do not carry
them. Spring Boot remains authoritative for CORS, CSRF, authentication, and all
product policy.

Provision the AWS platform with Terraform using this structure:

```text
infra/terraform/
├── modules/
│   ├── network/
│   ├── edge/
│   ├── database/
│   ├── cache/
│   └── backend-platform/
└── environments/
    └── prod/
```

The module responsibilities are:

| Module | Responsibility |
| --- | --- |
| `network` | Two-AZ VPC, public load-balancer subnets, private ECS subnets, isolated data subnets, one NAT Gateway, routing, and an S3 gateway endpoint. |
| `edge` | ACM validation, HTTPS load balancer, HTTP-to-HTTPS redirect, Route53 records, target group, and restrictive security groups. |
| `database` | Lean single-AZ encrypted RDS PostgreSQL instance, isolated subnet group, private access from ECS only, daily backups, and managed master credentials. |
| `cache` | Lean single-node encrypted ElastiCache for Redis OSS deployment in isolated subnets, reachable from ECS only. |
| `backend-platform` | ECS cluster, backend ECR repository, task/execution IAM roles, and CloudWatch log groups. Application task definitions and services wait for a real backend image. |

The production root consumes existing infrastructure prerequisites: an
encrypted, versioned S3 state bucket and separate GitHub OIDC roles for planning
and applying. Enable native S3 state locking with `use_lockfile = true`; do not
add deprecated DynamoDB locking or bootstrap these account-level resources in
this repository. Pin Terraform, providers, and GitHub Actions, commit
`.terraform.lock.hcl`, and never put credentials or secret values in Terraform
variables, plans, state, logs, or artifacts.

GitHub Actions will use this delivery policy when implementation begins:

1. Every pull request runs credential-free formatting, initialization without a
   backend, validation, and Terraform tests.
2. A cloud-backed production plan runs only for trusted same-repository pull
   requests. Fork pull requests never receive AWS or Vercel credentials.
3. A merge to `main` produces a fresh plan and requires approval through the
   protected GitHub `production` environment before apply. The apply role is
   separate from the lower-privilege plan role, and AWS access uses OIDC rather
   than stored access keys.
4. When applications exist, separate workflows deploy the backend to ECS and
   the frontend to Vercel. Database migrations run as a one-off ECS task before
   the backend service is updated. Production workflows use concurrency guards
   and do not apply a pull-request plan artifact.
5. Branch protection and GitHub environments are configured with `gh` after the
   corresponding checks exist; GitHub repository settings are not added to
   Terraform state.

## Consequences

- Next.js uses Vercel previews and runtime capabilities without adding a second
  ECS service or frontend image repository.
- Frontend and backend deployments use different platforms but share GitHub
  Actions as the policy and audit boundary.
- Cross-origin API calls require an exact CORS allowlist and credentialed HTTP
  client, while host-only API cookies avoid exposing authentication cookies to
  the public redirect hostname.
- Private ECS tasks can reach Google OAuth and external mail services through
  the single NAT Gateway. That gateway is a deliberate cost and single-AZ
  egress tradeoff for the lean 99.5% best-effort target.
- RDS is durable and backed up; Redis remains operational state rather than the
  system of record. The lean single-node choices trade high availability for
  lower portfolio-project cost.
- Infrastructure and application deployment remain unimplemented until a
  dedicated feature creates the Terraform, workflows, applications, and
  runtime verification.

## Alternatives considered

- **Run Next.js on ECS:** consistent with a single AWS runtime, but rejected in
  favor of Vercel hosting and previews. This supersedes the frontend-container
  part of ADR 0001 and NFR-DEP-05/06/07.
- **Vercel native Git deployment:** simpler and avoids a Vercel token in GitHub,
  but rejected to keep deployment policy in GitHub Actions.
- **VPC endpoints without internet egress:** rejected because Google OAuth and
  external email delivery require outbound internet access.
- **NAT plus interface endpoints:** stronger separation for AWS API traffic but
  rejected because its fixed cost is excessive for this project.
- **High-availability RDS and Redis:** deferred until measured availability or
  load requires the added cost.
- **Terraform-managed state/OIDC bootstrap or GitHub settings:** rejected in
  favor of existing account-level prerequisites and `gh`-managed repository
  controls.

## Requirement drivers

- HTTPS, secrets, and network isolation: NFR-SEC-01, NFR-SEC-03, and
  NFR-SEC-11/12.
- Health-based recovery and logs: NFR-AVL-04 and NFR-OBS-05.
- Repeatable deployment, rollback, immutable backend images, stateless compute,
  and infrastructure as code: NFR-DEP-01 and NFR-DEP-03..09.

## References

- [Terraform S3 backend and native lockfiles](https://developer.hashicorp.com/terraform/language/backend/s3)
- [AWS credentials for GitHub Actions with OIDC](https://github.com/aws-actions/configure-aws-credentials)
- [Vercel deployments from GitHub Actions](https://vercel.com/docs/git/vercel-for-github)
- [Amazon ECS task networking best practices](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/security-network.html)
