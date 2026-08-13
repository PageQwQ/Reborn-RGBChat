# RGB Chat

Colored chat messages using `&` color codes, just like older multi-player chat plugins.

Type `&6like &6this&r` in chat and links/names/colors appear as classic `§`-style colored text. Works everywhere text renders: chat, command feedback, power tools, signs (via client-side display), and more.

- Client-side only — **no server mod required**, no message tampering
- Configurable in `config/rgbchat.json` (auto-created on first launch)
- Rebuilds the original RGB Chat experience for modern Minecraft

## Usage

Open chat, type with `&` codes:

- `&0`–`&9`, `&a`–`&f`: classic Minecraft colors
- `&k`–`&o` (plus `&r` reset): formats — some legacy formats may fall back to colors
- Example: `&bHello &c&lworld&r!`

## Compatibility

| Loader | Versions |
|---|---|
| Fabric | 26.1.2, 26.2 |
| NeoForge | 26.1.2, 26.2 |

On the dedicated server side this mod does nothing.

## Configuration

`config/rgbchat.json` (created automatically):

- `useFormattingCodes`: enable `&k`/`&o` style formatting codes (default `true`)
- `colorFirst`: color the first character group of a word (default `true`)
- `lenience`: dash/hyphen lenience for non-Chinese text — adjusts how color codes apply across word boundaries (default `"cn-zh"`)

Find the original project and releases on [GitHub](https://github.com/waterfrog68/RGB-Chat-Renewed).