import { ArrowRight, LockKeyhole } from "lucide-react";
import Link from "next/link";

import "./account-access-state.css";

export function AccountAccessState({
  title,
  description,
  signInHref,
}: {
  title: string;
  description: string;
  signInHref: string;
}) {
  return (
    <section
      className="account-access-state"
      aria-labelledby="account-access-title"
    >
      <LockKeyhole
        className="account-access-icon"
        size={28}
        strokeWidth={1.5}
        aria-hidden="true"
      />
      <h1 id="account-access-title">{title}</h1>
      <p>{description}</p>
      <Link className="account-access-action" href={signInHref}>
        Sign in
        <ArrowRight size={18} aria-hidden="true" />
      </Link>
    </section>
  );
}
