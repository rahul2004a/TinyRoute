# Lucide-React Icon Quick Reference by Category

The artifact environment includes lucide-react@0.383.0. Import icons directly with `import { IconName } from "lucide-react"`.
All icons are 24x24 SVGs and support the `size`, `color`, and `strokeWidth` properties.

## Navigation
| Purpose | Icon names | Import |
|------|--------|------|
| Menu | Menu, AlignJustify | `import { Menu } from "lucide-react"` |
| Close | X, XCircle | `import { X } from "lucide-react"` |
| Back | ArrowLeft, ChevronLeft | `import { ArrowLeft } from "lucide-react"` |
| Forward | ArrowRight, ChevronRight | `import { ArrowRight } from "lucide-react"` |
| Up | ArrowUp, ChevronUp | `import { ArrowUp } from "lucide-react"` |
| Down | ArrowDown, ChevronDown | `import { ArrowDown } from "lucide-react"` |
| Home | Home | `import { Home } from "lucide-react"` |
| External link | ExternalLink, ArrowUpRight | `import { ExternalLink } from "lucide-react"` |
| Search | Search | `import { Search } from "lucide-react"` |

## Social Media
| Purpose | Icon names | Import |
|------|--------|------|
| GitHub | Github | `import { Github } from "lucide-react"` |
| Twitter/X | Twitter | `import { Twitter } from "lucide-react"` |
| LinkedIn | Linkedin | `import { Linkedin } from "lucide-react"` |
| YouTube | Youtube | `import { Youtube } from "lucide-react"` |
| Instagram | Instagram | `import { Instagram } from "lucide-react"` |
| Email | Mail, MailOpen | `import { Mail } from "lucide-react"` |
| Link | Link, Link2 | `import { Link } from "lucide-react"` |
| Share | Share2 | `import { Share2 } from "lucide-react"` |

## Content / Documents
| Purpose | Icon names |
|------|--------|
| File | File, FileText, FilePlus |
| Folder | Folder, FolderOpen |
| Image | Image, ImagePlus |
| Video | Video, Play, Pause |
| Audio | Music, Volume2 |
| Download | Download |
| Upload | Upload |
| Copy | Copy, Clipboard |
| Edit | Pencil, PenLine, Edit |
| Delete | Trash2 |

## Users / Personal
| Purpose | Icon names |
|------|--------|
| User | User, UserCircle |
| User group | Users |
| Avatar | CircleUser |
| Location | MapPin |
| Phone | Phone |
| Calendar | Calendar |
| Clock | Clock |
| Birthday | Cake |

## Skills / Technology
| Purpose | Icon names |
|------|--------|
| Code | Code, Code2, Terminal |
| Database | Database |
| Server | Server |
| Cloud | Cloud |
| Settings | Settings, Cog |
| Tools | Wrench, Hammer |
| Layers | Layers |
| Components | Component, Puzzle |
| CPU | Cpu |
| Lightning / Fast | Zap |
| Framework | LayoutGrid, Grid3x3 |
| API | Webhook |
| Security | Shield, Lock |

## Status / Feedback
| Purpose | Icon names |
|------|--------|
| Success | Check, CheckCircle |
| Warning | AlertTriangle |
| Error | AlertCircle, XCircle |
| Information | Info |
| Loading | Loader2 (can add `animate-spin`) |
| Star | Star, StarHalf |
| Heart | Heart |
| Like | ThumbsUp |

## Decoration / Layout
| Purpose | Icon names |
|------|--------|
| Sun (light mode) | Sun |
| Moon (dark mode) | Moon |
| Quote | Quote |
| Flame | Flame |
| Rocket | Rocket |
| Trophy | Trophy |
| Target | Target |
| Light bulb | Lightbulb |
| Book | BookOpen |
| Graduation cap | GraduationCap |
| Briefcase | Briefcase |
| Building | Building |

## Usage Examples

```jsx
import { Github, Mail, ArrowUpRight, Moon, Sun } from "lucide-react";

// Basic usage
<Github size={20} />

// Custom color and stroke
<Mail size={24} color="var(--color-primary)" strokeWidth={1.5} />

// Use with a button
<button className="flex items-center gap-2">
  <ArrowUpRight size={16} />
  View Project
</button>

// Theme-toggle icon
{isDark ? <Sun size={20} /> : <Moon size={20} />}
```

## Notes

1. Always import from lucide-react; do not replace icons with hand-written `<svg>` elements or emoji
2. When lucide does not provide a social icon (such as WeChat or Weibo), use an inline SVG. There is no preset in `references/`, so write a simplified SVG path in the code
3. The `size` property accepts a number in pixels. Common values are 16 (inside a small button), 20 (regular), 24 (standard), 32 (large icon), and 48 (decorative)
4. `strokeWidth` defaults to 2; use 1.5 for thin strokes and 2.5 for thick strokes
5. `className` accepts Tailwind classes, such as `className="animate-spin"` for a spinning loader
