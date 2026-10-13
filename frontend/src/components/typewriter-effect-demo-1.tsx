"use client";

import { ArrowRight } from "lucide-react";
import Link from "next/link";

import { TypewriterEffectSmooth } from "@/components/ui/typewriter-effect";

const words = [
  { text: "Short" },
  { text: "links." },
  { text: "Big" },
  { text: "possibilities.", className: "text-(--primary)" },
];

export default function TypewriterEffectSmoothDemo() {
  return (
    <>
      <p>Your next short link starts here.</p>
      <h2 id="closing-title">
        <TypewriterEffectSmooth words={words} />
      </h2>
      <div className="landing-actions">
        <Link className="landing-action" href="/register" prefetch={false}>
          Get started <ArrowRight aria-hidden="true" size={18} />
        </Link>
        <Link className="landing-secondary" href="/login" prefetch={false}>
          Sign in
        </Link>
      </div>
    </>
  );
}
