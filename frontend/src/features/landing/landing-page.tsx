import {
  ArrowRight,
  BarChart3,
  Clock3,
  Link2,
  ListFilter,
  TextCursorInput,
} from "lucide-react";
import Link from "next/link";

import TypewriterEffectSmoothDemo from "../../components/typewriter-effect-demo-1";
import { LandingBrand } from "./landing-brand";
import { LandingHeader } from "./landing-header";
import { LandingHeroBackground } from "./landing-hero-background";
import {
  LANDING_ACCOUNT_LINKS,
  LANDING_SECTION_LINKS,
} from "./landing-navigation";

function AccountActions() {
  return (
    <div className="landing-actions">
      <Link className="landing-action" href="/register" prefetch={false}>
        Get started <ArrowRight aria-hidden="true" size={18} />
      </Link>
      <Link className="landing-secondary" href="/login" prefetch={false}>
        Sign in
      </Link>
    </div>
  );
}

export function LandingPage() {
  return (
    <div className="landing-page">
      <LandingHeader />
      <main id="main-content" tabIndex={-1}>
        <section aria-labelledby="landing-title" className="landing-hero">
          <div className="landing-hero-content landing-container">
            <div className="landing-hero-copy">
              <h1 id="landing-title">
                Short links.
                <br /> Your control.
              </h1>
              <p>
                Create short links with custom aliases and expiry. Sign in,
                shorten, and share from one place.
              </p>
              <AccountActions />
            </div>
          </div>
          <LandingHeroBackground />
        </section>

        <section
          aria-labelledby="features"
          className="landing-section landing-container"
        >
          <div className="landing-section-heading">
            <h2 id="features" tabIndex={-1}>
              Features
            </h2>
            <p>A little less URL. A little more intention.</p>
          </div>
          <div className="landing-features">
            <article
              aria-labelledby="short-links-title"
              className="landing-feature landing-feature-lead"
            >
              <Link2 aria-hidden="true" size={32} strokeWidth={1.5} />
              <h3 id="short-links-title">Short links</h3>
              <p>
                Turn an HTTPS destination into a short link you can copy and
                share. Create it from your account. Open it from anywhere.
              </p>
            </article>
            <article
              aria-labelledby="aliases-title"
              className="landing-feature"
            >
              <TextCursorInput aria-hidden="true" size={24} strokeWidth={1.5} />
              <h3 id="aliases-title">Custom aliases</h3>
              <p>
                Request a memorable alias when you create a link, subject to
                availability and validation.
              </p>
            </article>
            <article aria-labelledby="expiry-title" className="landing-feature">
              <Clock3 aria-hidden="true" size={24} strokeWidth={1.5} />
              <h3 id="expiry-title">Expiry</h3>
              <p>
                Choose an expiry date and time at creation. Your link stops
                redirecting when it expires.
              </p>
            </article>
            <article
              aria-labelledby="management-title management-status"
              className="landing-feature"
            >
              <ListFilter aria-hidden="true" size={24} strokeWidth={1.5} />
              <div className="landing-feature-title">
                <h3 id="management-title">Link management</h3>
                <span className="landing-status" id="management-status">
                  Planned
                </span>
              </div>
              <p>
                Planned tools will let you list, search and filter your links,
                edit destinations, disable or re-enable links, and delete them.
              </p>
            </article>
            <article
              aria-labelledby="analytics-title analytics-status"
              className="landing-feature"
            >
              <BarChart3 aria-hidden="true" size={24} strokeWidth={1.5} />
              <div className="landing-feature-title">
                <h3 id="analytics-title">Analytics</h3>
                <span className="landing-status" id="analytics-status">
                  Planned
                </span>
              </div>
              <p>
                Planned analytics will show click totals, selectable daily
                trends defaulting to 30 days, and aggregated referrer, device,
                OS, browser, country and city breakdowns. Data will be delayed
                by up to one minute.
              </p>
            </article>
          </div>
        </section>

        <section
          aria-labelledby="how-it-works"
          className="landing-section landing-container"
        >
          <div className="landing-section-heading">
            <h2 id="how-it-works" tabIndex={-1}>
              How it works
            </h2>
            <p>From destination to shared link in three steps.</p>
          </div>
          <ol
            aria-label="Steps to your first short link"
            className="landing-steps"
          >
            <li>
              <span aria-hidden="true" className="landing-step-number">
                01
              </span>
              <h3>Create an account</h3>
              <p>
                Register with email and password or Google. Sign in to start
                creating your links.
              </p>
            </li>
            <li>
              <span aria-hidden="true" className="landing-step-number">
                02
              </span>
              <h3>Shorten your link</h3>
              <p>
                Add an HTTPS destination. Choose an optional custom alias and
                expiry, then create your short link.
              </p>
            </li>
            <li>
              <span aria-hidden="true" className="landing-step-number">
                03
              </span>
              <h3>Share it anywhere</h3>
              <p>
                Copy the result and share it. Recipients need no account to
                follow an active, unexpired link.
              </p>
            </li>
          </ol>
        </section>

        <section
          aria-labelledby="closing-title"
          className="landing-closing landing-container"
        >
          <TypewriterEffectSmoothDemo />
        </section>
      </main>
      <footer className="landing-footer">
        <div className="landing-container landing-footer-content">
          <div>
            <LandingBrand />
            <p>© {new Date().getFullYear()} TinyRoute</p>
          </div>
          <nav aria-label="Footer sections">
            <h2>Pages</h2>
            {LANDING_SECTION_LINKS.map(({ href, label }) => (
              <a className="landing-nav-link" href={href} key={href}>
                {label}
              </a>
            ))}
          </nav>
          <nav aria-label="Footer account">
            <h2>Account</h2>
            {LANDING_ACCOUNT_LINKS.map(({ href, label }) => (
              <Link
                className="landing-nav-link"
                href={href}
                key={href}
                prefetch={false}
              >
                {label}
              </Link>
            ))}
            <Link className="landing-nav-link" href="/links" prefetch={false}>
              Your links
            </Link>
          </nav>
        </div>
        <div
          aria-hidden="true"
          className="landing-footer-wordmark landing-container"
        >
          <div className="landing-footer-wordmark-sharp">TinyRoute</div>
          <div className="landing-footer-wordmark-soft">TinyRoute</div>
        </div>
      </footer>
    </div>
  );
}
