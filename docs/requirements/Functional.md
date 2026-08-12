# Functional Requirements

## 1. Scope

TinyRoute is a URL shortener built as a personal portfolio project. It is designed to be small enough for one developer to build, deploy, and maintain, while still behaving like a real production service.

This document defines **what** the system does. It does not define architecture, data models, technologies, or API contracts.

**Confirmed decisions**

- Anyone can follow a short URL; no account is needed to be redirected.
- Creating and managing short URLs requires an account.
- MVP is the lean core: accounts, create, redirect, manage, disable/delete, rate limiting.
- Custom aliases, expiration, analytics, and a public API are deliberately out of MVP.

**Legend**

- **Priority:** `Must` / `Should` / `Could`, within the stated release.
- **Release:** `MVP` (first working deployment), `V1` (the version to show off), `Future` (nice-to-have, only if time allows).

## 2. Actors

- **Visitor** — anyone following a short URL.
- **User** — a registered, signed-in person who owns short URLs.
- **System** — automated behavior such as expiration or rate limiting.

## 3. Accounts

| ID        | Requirement         | Description                                                                                           | Actor   | Priority | Release |
| --------- | ------------------- | ----------------------------------------------------------------------------------------------------- | ------- | -------- | ------- |
| FR-ACC-01 | Register            | A visitor can create an account with an email address and password.                                   | Visitor | Must     | MVP     |
| FR-ACC-02 | Sign in and out     | A registered user can sign in and sign out. After sign-out, protected pages are no longer accessible. | User    | Must     | MVP     |
| FR-ACC-03 | Session persistence | A signed-in user stays signed in across browser restarts until the session expires or they sign out.  | User    | Should   | MVP     |
| FR-ACC-04 | Password reset      | A user can reset a forgotten password through a link sent to their registered email.                  | User    | Should   | V1      |
| FR-ACC-05 | Delete account      | A user can delete their account. All of their short URLs stop redirecting.                            | User    | Could    | V1      |

## 4. URL Creation

| ID        | Requirement              | Description                                                                                                                | Actor   | Priority | Release |
| --------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------------- | ------- | -------- | ------- |
| FR-CRE-01 | Create short URL         | A signed-in user submits a destination URL and receives a short URL that resolves to it.                                   | User    | Must     | MVP     |
| FR-CRE-02 | Authentication required  | Creation attempts by signed-out visitors are rejected and the visitor is prompted to sign in.                              | Visitor | Must     | MVP     |
| FR-CRE-03 | Destination validation   | Only well-formed `https` URLs are accepted. Anything else returns a clear validation message.                              | User    | Must     | MVP     |
| FR-CRE-04 | Unique code              | Every short URL gets a code that is not already in use. A new link never overwrites an existing one.                       | System  | Must     | MVP     |
| FR-CRE-05 | Copyable result          | After creation, the short URL is displayed in a form the user can copy in one click.                                       | User    | Must     | MVP     |
| FR-CRE-06 | Self-reference rejection | Destinations pointing back at TinyRoute's own short-URL domain are rejected to avoid redirect loops.                       | System  | Should   | MVP     |
| FR-CRE-07 | Custom alias             | A user can request a custom code instead of a generated one, if it is available and passes format and reserved-word rules. | User    | Should   | V1      |
| FR-CRE-08 | Expiration               | A user can set an expiry date and time when creating a link.                                                               | User    | Should   | V1      |

## 5. Redirection

| ID        | Requirement          | Description                                                                                       | Actor   | Priority | Release |
| --------- | -------------------- | ------------------------------------------------------------------------------------------------- | ------- | -------- | ------- |
| FR-RED-01 | Resolve link         | Requesting an active short URL redirects the visitor to its destination. No account required.     | Visitor | Must     | MVP     |
| FR-RED-02 | Preserve destination | The redirect preserves the destination's full path, query, and fragment as stored.                | Visitor | Must     | MVP     |
| FR-RED-03 | Unknown code         | An unknown code returns a not-found page and never redirects somewhere else.                      | Visitor | Must     | MVP     |
| FR-RED-04 | Disabled link        | A disabled short URL does not redirect and shows an "unavailable" page with no owner details.     | Visitor | Must     | MVP     |
| FR-RED-05 | Deleted link         | A deleted short URL no longer redirects and reveals nothing about the original link or its owner. | Visitor | Must     | MVP     |
| FR-RED-06 | Case sensitivity     | Codes are case-sensitive. Changing the case of a code does not resolve to a different link.       | Visitor | Must     | MVP     |
| FR-RED-07 | Expired link         | Once past its expiry time, a link stops redirecting without any owner action.                     | System  | Must     | V1      |

## 6. Link Management

| ID        | Requirement           | Description                                                                                    | Actor | Priority | Release |
| --------- | --------------------- | ---------------------------------------------------------------------------------------------- | ----- | -------- | ------- |
| FR-MGT-01 | List own links        | A user sees a paginated list of their links with code, destination, status, and creation date. | User  | Must     | MVP     |
| FR-MGT-02 | Ownership enforcement | A user cannot view or act on another user's links, even by guessing an identifier in the URL.  | User  | Must     | MVP     |
| FR-MGT-03 | Disable link          | A user can disable one of their links; it stops redirecting but stays in their list.           | User  | Must     | MVP     |
| FR-MGT-04 | Re-enable link        | A user can re-enable a disabled link and it resumes redirecting.                               | User  | Must     | MVP     |
| FR-MGT-05 | Delete link           | A user can delete a link after confirming. Deletion is permanent and the code is never reused. | User  | Must     | MVP     |
| FR-MGT-06 | Search                | A user can search or filter their links by code or destination.                                | User  | Should   | V1      |
| FR-MGT-07 | Edit destination      | A user can change a link's destination; future redirects use the new one.                      | User  | Must     | V1      |

## 7. Analytics

| ID        | Requirement                                                                 | Description                                                                                                 | Actor  | Priority | Release |
| --------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- | ------ | -------- | ------- |
| FR-ANA-01 | Count clicks                                                                | Each successful redirect increments a click count for that link, excluding the service's own health checks. | System | Must     | V1      |
| FR-ANA-02 | View click count                                                            | A user sees the total clicks for each of their links.                                                       | User   | Must     | V1      |
| FR-ANA-03 | Click trend                                                                 | A user sees clicks per day for a link over the last 30 days.                                                | User   | Should   | V1      |
| FR-ANA-04 | Referrer, Device, Operating System, Browser and Geography(Country and City) | A user sees aggregated top referrers and countries for a link.                                              | User   | Could    | MVP     |

## 8. Abuse Prevention

| ID        | Requirement         | Description                                                                                                  | Actor  | Priority | Release |
| --------- | ------------------- | ------------------------------------------------------------------------------------------------------------ | ------ | -------- | ------- |
| FR-ABS-01 | Creation rate limit | Link creation is capped per account per hour; requests over the cap are rejected with a clear retry message. | System | Must     | MVP     |
| FR-ABS-02 | Auth rate limit     | Sign-in and registration attempts are capped per client to slow down credential guessing.                    | System | Must     | MVP     |
| FR-ABS-03 | Redirect rate limit | Abnormally high request rates against redirects are throttled without blocking normal visitors.              | System | Should   | MVP     |
| FR-ABS-04 | Blocklist           | Destinations matching a maintained blocklist of domains or patterns are rejected at creation.                | System | Should   | V1      |
| FR-ABS-05 | Safe browsing check | Destinations are checked against a reputation source and rejected if flagged as malicious.                   | System | Could    | Future  |

## 9. API Access

| ID        | Requirement         | Description                                                                                                        | Actor | Priority | Release |
| --------- | ------------------- | ------------------------------------------------------------------------------------------------------------------ | ----- | -------- | ------- |
| FR-API-01 | API keys            | A user can generate and revoke API keys. A revoked key stops working immediately.                                  | User  | Should   | V1      |
| FR-API-02 | Programmatic create | A valid API key can create short URLs owned by its user, under the same validation and rate limits as the web app. | User  | Should   | V1      |
| FR-API-03 | Programmatic manage | A valid API key can list, disable, and delete its user's links.                                                    | User  | Could    | V1      |

## 10. Functional Requirements Summary

**MVP** — email/password accounts, authenticated link creation with validation, public redirection with correct handling of unknown, disabled, and deleted codes, owner-only link listing and management, and rate limiting on creation, auth, and redirects. This is the smallest version that is genuinely deployable.

**V1** — the portfolio version: custom aliases, expiration, click counts and a 30-day trend, search, password reset, a blocklist, and an API-key-based public API.

**Future** — editable destinations, referrer/country analytics, and third-party safe-browsing checks. Only if time allows; nothing else depends on these.

## 11. Assumptions

1. Single-language, single-region web app; no localization or multi-tenancy.
2. Email/password is the only identity method; social login is out of scope.
3. Anonymous link creation is excluded entirely to keep abuse handling simple.
4. There is no admin console. Moderation, if ever needed, happens through direct database or CLI access by the project owner.
5. Codes are never reused after deletion or expiry.
6. Link destinations are immutable in MVP and V1.
7. Analytics are informational only, not billing-grade, and may undercount blocked or cached requests.
8. Traffic is portfolio-scale: a handful of real users plus load tests and demos.

## 12. Open Questions

1. What is the short-URL domain, and is a custom domain worth the cost for a portfolio project?
2. Should password reset ship in MVP, given that email delivery adds a dependency?
3. Are click counts good enough for the demo, or is the daily trend chart the thing worth showing?
4. Is the public API worth building, or is the web app plus a good README more convincing to a reviewer?

## 13. Recommended Next Step

Lock the MVP table as the build scope, then move to non-functional targets in [docs/requirements/Non-Functional.md](docs/requirements/Non-Functional.md). Design work should not begin until both are settled.
