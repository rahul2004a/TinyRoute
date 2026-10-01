import { Link2 } from "lucide-react";

export function AuthBrand() {
  return (
    <div className="mb-14 flex items-center gap-2.5 text-sm font-semibold tracking-tight text-(--auth-ink)">
      <span className="grid size-8 place-items-center rounded-md bg-(--auth-ink) text-(--auth-canvas)">
        <Link2 aria-hidden="true" size={17} strokeWidth={2.25} />
      </span>
      TinyRoute
    </div>
  );
}
