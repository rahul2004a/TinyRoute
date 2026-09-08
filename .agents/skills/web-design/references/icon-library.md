# Icon selection reference

Use the icon family already installed by TinyRoute. The names below use Lucide
terminology only as a searchable concept index; do not add `lucide-react` unless
the current task authorizes it.

## Common concepts

| Purpose | Typical icon names |
|---|---|
| Menu and close | `Menu`, `AlignJustify`, `X`, `XCircle` |
| Back and forward | `ArrowLeft`, `ArrowRight`, `ChevronLeft`, `ChevronRight` |
| Expand and collapse | `ChevronUp`, `ChevronDown`, `Plus`, `Minus` |
| Home and external navigation | `Home`, `ExternalLink`, `ArrowUpRight` |
| Search and filter | `Search`, `Filter`, `SlidersHorizontal` |
| Link actions | `Link`, `Link2`, `Copy`, `Clipboard`, `Share2` |
| Edit and delete | `Pencil`, `PenLine`, `Trash2` |
| Account and users | `User`, `UserCircle`, `Users`, `CircleUser` |
| Time and location | `Calendar`, `Clock`, `MapPin` |
| Code and infrastructure | `Code2`, `Terminal`, `Database`, `Server`, `Cloud` |
| Settings and security | `Settings`, `Wrench`, `Shield`, `Lock` |
| Success | `Check`, `CheckCircle` |
| Warning and error | `AlertTriangle`, `AlertCircle`, `XCircle` |
| Information and loading | `Info`, `Loader2` |
| Theme | `Sun`, `Moon` |
| Analytics | `ChartLine`, `ChartBar`, `TrendingUp`, `MousePointerClick` |

## React example

Adapt imports to the package already present in the project:

```tsx
import { ArrowUpRight, Copy } from "lucide-react";

<button type="button" className="action-button">
  <Copy aria-hidden="true" size={16} strokeWidth={1.75} />
  <span>Copy link</span>
</button>

<a href={destination} target="_blank" rel="noreferrer">
  <span>Open destination</span>
  <ArrowUpRight aria-hidden="true" size={16} strokeWidth={1.75} />
</a>
```

## Rules

1. Use one icon family within a component tree.
2. Keep a consistent size and stroke scale.
3. Pair ambiguous icons with visible labels.
4. Mark decorative icons `aria-hidden="true"`; give icon-only buttons an
   accessible name.
5. Do not use emoji as functional controls.
6. Do not hand-copy proprietary brand marks. Use approved brand assets when
   identity matters.
7. Check `package.json` before importing any library.
