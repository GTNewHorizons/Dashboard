# Dashboard textures

Everything here lives in `assets/dashboard/textures/gui/`.

Each PNG listed in `themes.json` is one bar theme. The file name is the theme id (`simple.png` is the theme `simple`).
`crosshairs.png` is the crosshair sheet, not a theme. The `images` folder holds pack images.
Every theme uses the same layout, shown below and in [theme_template.png](theme_template.png).

## themes.json

Themes are listed in `themes.json`, next to the PNGs: theme id on the left, the name players see on the right.

```json
{
  "simple": "Simple",
  "super_funky": "Super Funky Theme"
}
```

- A resource pack adds themes with its own `assets/dashboard/textures/gui/themes.json` and PNGs. The lists of
  all packs are merged, so a pack only needs to list its own themes.
- A theme whose PNG is missing is skipped (and noted in the log).
- Use lowercase file names without spaces.

## Crosshairs

The `"crosshairs": 34` entry in `themes.json` is not a theme: it says how many crosshairs `crosshairs.png` holds.
The sheet is 256 x 256 with one 16 x 16 cell per crosshair, filled left to right, then top to bottom (up to 256).
A resource pack that replaces `crosshairs.png` sets its own count the same way. The top pack's count is used.

## Pack images

Pictures a resource pack adds to the HUD, like a logo or a frame around the hotbar. Each one is its own element that
players can move, resize and hide. They are listed in `images.json`, next to `themes.json`, and each PNG goes in the
`images` folder, named after its entry (`logo` is `images/logo.png`).

```json
{
  "logo": {
    "name": "Server Logo",
    "width": 64,
    "height": 32,
    "anchor": "top_left",
    "x": 4,
    "y": 4
  },
  "flame": {
    "name": "Flame",
    "width": 16,
    "height": 16,
    "anchor": "bottom_right",
    "opacity": 0.8,
    "frames": 4,
    "frametime": 2
  }
}
```

| Setting     | Meaning                                                                                       | Default    |
|-------------|-----------------------------------------------------------------------------------------------|------------|
| `name`      | The name shown in the editor (a lang key works too)                                           | the entry  |
| `width`     | Size on screen, in GUI pixels. The picture is stretched to it, so larger PNGs look sharper    | 32         |
| `height`    |                                                                                               | 32         |
| `anchor`    | `top_left`, `top_center`, `top_right`, `center_left`, `center`, `center_right`, `bottom_left`, `bottom_center` or `bottom_right` | `top_left` |
| `x`, `y`    | Moves it from the anchor, right and down (negative for left and up)                           | 0          |
| `opacity`   | From 0 (invisible) to 1 (solid)                                                               | 1          |
| `visible`   | `false` keeps it hidden until the player turns it on in the editor                            | `true`     |
| `frames`    | For animations: the number of frames, stacked from top to bottom in the PNG                   | 1          |
| `frametime` | How long each frame shows, in game ticks (20 per second)                                      | 1          |

- The lists of all packs are merged, like `themes.json`. An entry whose PNG is missing is skipped (and noted in the log).
- Transparent pixels stay transparent, also partly see-through ones.
- An image's name is read once, so a renamed image shows its new name after a restart.
- Use lowercase file names without spaces.

## Sheet

- Size: **512 x 512**
- Every slot is the bar plus **8 px of space on each side**. The bar starts 8 px in from the slot's top left corner.
- Wide slots (XP, jump, boss): **198 x 21**, bar **182 x 5**
- Row slots (everything else): **97 x 25**, bar **81 x 9**
- Each bar has 4 slots: **empty**, **full**, **decoration** and **decoration active**.

## Slots

Coordinates are the top left corner of the slot, in pixels (u across, v down).

Wide bars, 2 x 2 each:

| v | u 0 | u 198 |
|---|---|---|
| **0** | XP empty | XP full |
| **21** | XP decoration | XP decoration active |
| **42** | Jump empty | Jump full |
| **63** | Jump decoration | Jump decoration active |
| **84** | Boss empty | Boss full |
| **105** | Boss decoration | Boss decoration active |

Row bars, one row each:

| v | u 0 | u 97 | u 194 | u 291 |
|---|---|---|---|---|
| **126** | Health empty | Health full | Health decoration | Health decoration active |
| **151** | Poisoned empty | Poisoned full | Poisoned decoration | Poisoned decoration active |
| **176** | Withered empty | Withered full | Withered decoration | Withered decoration active |
| **201** | Hardcore empty | Hardcore full | Hardcore decoration | Hardcore decoration active |
| **226** | Flash empty | Flash full | Flash decoration | Flash decoration active |
| **251** | Flash poisoned (full) | Flash withered (full) | | |
| **276** | Absorption empty | Absorption full | Absorption decoration | Absorption decoration active |
| **301** | Armor empty | Armor full | Armor decoration | Armor decoration active |
| **326** | Food empty | Food full | Food decoration | Food decoration active |
| **351** | Hunger empty | Hunger full | Hunger decoration | Hunger decoration active |
| **376** | Air empty | Air full | Air decoration | Air decoration active |
| **401** | Mount health empty | Mount health full | Mount health decoration | Mount health decoration active |
| **426** | Regen shine *(u 0, 25 x 25)*, Air pop *(u 25, 25 x 25)* | AppleCore saturation (full) | AppleCore exhaustion (full) | |

## How the slots are used

- Every slot is drawn exactly as painted. A transparent slot draws nothing, so state rows (poisoned, withered, flash,
  hunger) need their empty bar and decorations painted too, not only the fill.
- **Empty** slots and **Decorations** are drawn whole, including their 8 px space. Paint outlines, glow and frames there.
- **Full** slots are cut to how full the bar is, and only the bar area is used. Their 8 px space is ignored.
- Flair that reaches into the bar area of an empty slot gets covered by the fill. decorations are drawn after the fill.
- **Decoration active** replaces the decoration while the bar is completely full.
- Paint every bar **filling from the left**, with its decoration at the right end. Bars that fill from the right (food,
  air and mount health by default, or any bar a player flips) are drawn mirrored.
- **Poisoned** and **withered** replace the health row while the effect lasts. Withered wins over poisoned.
- **Hunger** replaces the food row during the Hunger effect.
- **Absorption** is its own bar, drawn above health.
- **Health flash** is used for half a second after taking damage: the flash empty bar replaces the normal one, and
  the flash full bar shows the health just lost. While poisoned or withered, the flash poisoned or flash withered
  fill shows it instead.
- **Hardcore** is drawn on top of the health bar in Hardcore worlds, whatever state it is in: its empty over the empty
  bar, its full over the fill. Its decorations replace the health decorations. Leave it transparent where the bar should
  show through.
- **Health regen shine** is a 9 x 9 piece (25 x 25 with its 8 px space), drawn whole on top of the health bar during
  Regeneration. It steps along the bar one heart width (8 px) per tick, like vanilla's heart wave.
- **Air pop** is a 9 x 9 piece (25 x 25 with its 8 px space), drawn whole just past the end of the air fill for a
  moment whenever a bubble runs out, like vanilla's popping bubble.
- **AppleCore saturation** and **AppleCore exhaustion** are only used when AppleCore is installed. Both are full bars
  cut to how full they are and drawn over the food fill, exhaustion first, then saturation. Make them see-through,
  so the food fill shows under them.
