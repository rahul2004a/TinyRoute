"use client";

import { useSession } from "../auth/use-session";
import { LandingHeader } from "./landing-header";
import { LANDING_ACCOUNT_LINKS } from "./landing-navigation";

const signedInLinks = [
  { href: "/links", label: "Your links" },
  { href: "/settings", label: "Account settings" },
] as const;

const unconfirmedLinks = [{ href: "/links", label: "Your links" }] as const;

export function LandingSessionHeader() {
  const session = useSession();
  const accountLinks = session.data?.authenticated
    ? signedInLinks
    : session.data?.authenticated === false
      ? LANDING_ACCOUNT_LINKS
      : unconfirmedLinks;

  return <LandingHeader accountLinks={accountLinks} />;
}
