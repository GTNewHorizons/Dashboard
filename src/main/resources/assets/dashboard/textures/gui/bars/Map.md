# Dashboard bar themes

Each PNG in this folder is one bar theme. The file name is the theme id (`simple.png` is the theme `simple`).
Every theme uses the same layout, shown below.

## themes.json

Themes are listed in `themes.json`, next to the PNGs: theme id on the left, the name players see on the right.

```json
{
  "simple": "Simple",
  "super_funky": "Super Funky Theme"
}
```

- A resource pack adds themes with its own `assets/dashboard/textures/gui/bars/themes.json` and PNGs. The lists of
  all packs are merged, so a pack only needs to list its own themes.
- A theme whose PNG is missing is skipped (and noted in the log).
- Use lowercase file names without spaces.

## Sheet

- Size: **512 x 512**
- Every slot is the bar plus **8 px of space on each side**. The bar starts 8 px in from the slot's top left corner.
- Wide slots (XP, jump, boss): **198 x 21**, bar **182 x 5**
- Row slots (everything else): **97 x 25**, bar **81 x 9**

## Slots

Coordinates are the top left corner of the slot, in pixels (u across, v down).

| v \ u | 0 | 97 | 194 | 291 |
|---|---|---|---|---|
| **0** | XP empty | | XP full *(at u 198)* | |
| **21** | Jump empty | | Jump full *(at u 198)* | |
| **42** | Boss empty | | Boss full *(at u 198)* | |
| **63** | Health empty | Health full | Armor empty | Armor full |
| **88** | Food empty | Food full | Air empty | Air full |
| **113** | Mount health empty | Mount health full | | |
| **138** | Health poisoned (full) | Health withered (full) | Absorption (full) | Hunger effect (full) |
| **163** | Health flash (empty) | Health flash (full) | Health flash poisoned (full) | Health flash withered (full) |
| **188** | Health regen shine *(u 0, 25 x 25)*, Air pop *(u 25, 25 x 25)* | Health hardcore overlay | | |

Everything else from v 188 down is free for future slots.

## How the slots are used

- **Empty** slots are drawn whole, including their 8 px space. Paint any flair (outlines, glow, ornaments) there.
- **Full** slots are cut to how full the bar is, and only the bar area is used. Leave their 8 px space transparent.
- Flair that reaches into the bar area of an empty slot gets covered by the fill.
- **States** (poisoned, withered, absorption, hunger effect) are full bars drawn over the normal empty bar.
- **Health flash** is used for half a second after taking damage: the flash empty bar replaces the normal one, and
  the flash full bar shows the health just lost (normal, poisoned or withered).
- **Health regen shine** is a 9 x 9 piece (25 x 25 with its 8 px space), drawn whole on top of the health bar during
  Regeneration. It steps along the bar one heart width (8 px) per tick, like vanilla's heart wave.
- **Health hardcore overlay** is drawn whole (with its 8 px space) on top of the health bar in Hardcore worlds,
  whatever state the bar is in. Leave it transparent where the bar should show through.
- **Air pop** is a 9 x 9 piece (25 x 25 with its 8 px space), drawn whole just past the end of the air fill for a
  moment whenever a bubble runs out, like vanilla's popping bubble.
- Food, air and mount health fill from the right by default, the others from the left. Players can flip any bar.
