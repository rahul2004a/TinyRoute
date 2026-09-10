# ADR 0001: Frontend technology stack

- Status: Accepted
- Date: 2026-09-10
- Owners: TinyRoute maintainers

## Context

TinyRoute needs an accessible, responsive frontend for authentication, link
management, and analytics without creating a second application backend. The
Spring Boot API owns validation, authentication, authorization, rate limits,
transactions, redirects, and persistence. The frontend needs a coherent toolset
for forms, remote data, charts, themes, and tests that remains manageable for a
single developer.

## Decision

Use the following baseline for all frontend work:

| Area | Decision |
| --- | --- |
| Runtime and framework | Node.js 24 LTS, Next.js App Router, and React |
| Language | TypeScript with strict mode enabled |
| Styling | Tailwind CSS |
| Components | shadcn/ui configured for Radix UI primitives |
| Icons | Lucide React only |
| Forms and validation | React Hook Form, Zod, and `@hookform/resolvers/zod` |
| Remote data | TanStack Query |
| Charts | Recharts |
| Theme | next-themes with light, dark, and system modes on `data-theme` |
| Unit/component tests | Vitest and React Testing Library |
| Browser tests | Playwright |
| Quality tools | ESLint and Prettier |
| Package manager | pnpm with a committed lockfile |

Use Server Components by default for layouts and static structure. Add Client
Components only where browser state or browser APIs are required: interactive
forms, TanStack Query consumers, Recharts visualizations, and theme controls.
Keep the theme provider as a small client boundary.

The browser calls the Spring Boot JSON API through one shared typed HTTP client,
including credentials and required CSRF data. Next.js route handlers and Server
Actions are not an application backend and must not proxy ordinary API calls or
own product policy. The public short-code redirect remains a Spring Boot route.

TanStack Query owns remote server state, caching, retries, invalidation, and
loading/error transitions. React and React Hook Form own local interaction and
form state. Zod provides early UI feedback and validates frontend boundaries;
it does not replace authoritative Spring Boot validation. Analytics charts must
respect reduced-motion preferences and expose the same values in an accessible
table.

Dependency families are architectural decisions; exact versions are bootstrap
decisions recorded in `package.json` and `pnpm-lock.yaml`. The same Node.js 24
LTS release line is used locally, in CI, and in the frontend container.

## Consequences

- The component source stays locally owned and customizable while Radix supplies
  accessible interaction primitives.
- Client JavaScript must remain deliberate because forms, querying, charts, and
  theming require client boundaries.
- Form schemas may mirror backend constraints for usability, but API responses
  remain the source of truth and must always be handled.
- Vitest covers synchronous utilities, hooks, components, form behavior, and
  query states. Playwright covers async Server Components and critical browser
  journeys such as sign-in, create, manage, and analytics.
- Recharts is sufficient for MVP analytics; another charting system requires a
  new decision rather than being introduced alongside it.
- shadcn/ui must be initialized with its Radix option. Its default primitive
  backend must not be accepted implicitly.

## Alternatives considered

- **Next.js Pages Router:** rejected because new work should use the App Router
  and its Server/Client Component model.
- **A custom component library:** rejected because it adds accessibility and
  maintenance work without improving TinyRoute's product scope.
- **Base UI primitives for shadcn/ui:** viable, but rejected to honor the chosen
  Radix ecosystem and avoid two primitive systems.
- **Ad hoc fetch state:** rejected because owner lists and analytics need shared
  caching, invalidation, retry, loading, and error behavior.
- **Jest or Cypress:** viable, but rejected to keep one fast unit/component
  runner and one cross-browser end-to-end runner.

## References

- [Next.js App Router](https://nextjs.org/docs/app)
- [Next.js TypeScript configuration](https://nextjs.org/docs/app/api-reference/config/typescript)
- [Tailwind CSS with Next.js](https://tailwindcss.com/docs/installation/framework-guides/nextjs)
- [shadcn/ui configuration](https://ui.shadcn.com/docs/components-json)
- [shadcn/ui primitive selection](https://ui.shadcn.com/docs/changelog/2026-07-base-ui-default)
- [React Hook Form resolvers](https://github.com/react-hook-form/resolvers)
- [Zod](https://zod.dev/)
- [TanStack Query for React](https://tanstack.com/query/latest/docs/framework/react/overview)
- [Recharts](https://recharts.github.io/en-US/guide/)
- [next-themes](https://github.com/pacocoursey/next-themes/blob/main/next-themes/README.md)
- [Next.js Vitest guide](https://nextjs.org/docs/app/guides/testing/vitest)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Playwright test guidance](https://playwright.dev/docs/next/writing-tests)
- [pnpm installation and compatibility](https://pnpm.io/installation)
