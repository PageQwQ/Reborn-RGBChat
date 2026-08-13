# RGB Chat

RGB colored chat and text rendering for modern Minecraft, rebuilt from the original 1.12.2 RGB Chat mod.

Type `#RRGGBB` tags in chat and the following text renders in that color — no server mod needed:

- `#FF0000red text` — solid color
- `#FF0000-0000FFgradient text` — per-character gradient between 2+ color stops
- `##` — a literal `#`, and a `#` that isn't a valid tag is shown as-is
- Vanilla `§` codes keep their original meaning and end an active gradient

Works everywhere text renders: chat, signs, anvils, item names, and the input preview before you send (each context has its own config switch).

- Client-side only — **no server mod required**, nothing is sent over the network
- Auto-creates `config/rgbchat.json` with all switches on

## Usage

| Example | Result |
|---|---|
| `#FF5555Hello &fworld` | "Hello" in red, "world" in white |
| `#00FF00-0000FFagradient` | green→blue gradient across the word |

## Compatibility

| Loader | Versions |
|---|---|
| Fabric | 1.21.1, 1.21.2 – 1.21.11, 26.1.2, 26.2 |
| NeoForge | 26.1.2, 26.2 |

On dedicated servers the mod is a harmless no-op.

## Configuration

`config/rgbchat.json` (created on first launch):

- `enabled`: master switch
- `gradient`: when false, gradient tags render as their first color stop
- `chat`, `signs`, `anvil`, `itemNames`, `inputPreview`: per-context switches

Find the source and releases on [GitHub](https://github.com/PageQwQ/Reborn-RGBChat).