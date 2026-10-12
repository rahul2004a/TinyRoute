import { Link2 } from "lucide-react";
import Link from "next/link";

export function LandingBrand() {
  return (
    <Link
      className="landing-brand inline-flex min-h-11 min-w-11 items-center gap-2 font-semibold tracking-tight"
      href="/"
      prefetch={false}
    >
      <Link2 aria-hidden="true" size={22} strokeWidth={1.8} />
      TinyRoute
    </Link>
  );
}
