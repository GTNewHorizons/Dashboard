# Dashboard

**One HUD config manager to rule them all.**

Dashboard lets you rearrange your whole HUD in an in-game editor: move, resize and hide the parts of the screen you
see while playing, both from Minecraft and from other mods. It also lets you restyle bars, the crosshair, the hotbar and
more.

> Dashboard is still a work in progress. Things may change, and more mods will be supported over time.

## Requirements

- Minecraft 1.7.10 with Forge
- [GTNHLib](https://github.com/GTNewHorizons/GTNHLib)

Every other mod listed below is optional. Dashboard only adds its features for a mod when that mod is installed.

### Recommended

- [InGameInfoXML](https://github.com/GTNewHorizons/InGame-Info-XML): add your own text and info to the screen, like
  coordinates, time, biome, light level or held item details, fully customizable. Together with Dashboard, you can
  then move and resize each of its text areas, for a HUD that is completely your own.

## The HUD Editor

To open the editor, first bind **Open Dashboard HUD Editor** in **Controls**, under **Dashboard** (it has no key by default), then press that key.
Without a key, open it while in a world through **Mod Options**, **Dashboard**, **Config**, then **Open HUD Editor**.

Every part of the HUD gets its own box, even when it is not showing right now, so you can always find and grab it.

Hover over an element, then:

| Action                         | How                                                      |
|--------------------------------|----------------------------------------------------------|
| Move it                        | Drag it (hold **Shift** to stop it snapping)             |
| Move it by 1 pixel             | **Arrow keys**                                           |
| Hide or show it                | **Right-click**                                          |
| Reset its position             | **R**                                                    |
| Resize it                      | **Alt + scroll**                                         |
| Reset its size                 | **Alt + R**                                              |
| Make its background see-through | **Ctrl + scroll** (some elements)                       |
| Reset the see-through background | **Ctrl + R**                                           |
| Lock it, so it can't be changed by accident | **L** (again to unlock)                     |
| Attach it to another element   | **A**, then click the other element (**A** again to detach) |
| Zoom into an image             | **Scroll**, **middle-drag** to move around, **middle-click** to reset |

The keys in bold are the defaults. Change them in **Controls**, under **Dashboard Editor**. Hover the **?** in the editor
menu to see the controls with your own keys.

Hover an element to see its name, its size and which mod it belongs to. If that mod has its own position setting for
the element, the tooltip also tells you where to find it. Once you move the element in the editor, Dashboard's position
is used instead, until you reset it.

### Attaching elements

An attached element follows the one it is attached to, so you can keep a bar right above the hotbar wherever the
hotbar goes. Drag it to change how far from the other element it sits, it stays attached. Resetting its position
(**R**) also detaches it. Hover either element to see a line between them.

The XP level number is attached to the XP bar from the start. Detach it with **A** if you want to place it on its own,
**R** attaches it again.

### Editor buttons

- **Grid**: shows a grid and snaps elements to it.
- **Preview**: shows sample content for elements that are not showing right now, so you can see what you are moving.
- **Labels**: shows each element's name inside its box.
- **Position**: shows each element's position next to its box.
- **Elements**: a list of every element in the editor, grouped by mod, with a search box. Hover over a row to light
  up its element, click it to pick the element up (click again to drop it), right-click it to hide or show it. Handy
  for elements that are tiny or covered by others.
- **Mods**: a list of mods with tick boxes. Only Minecraft and Dashboard are ticked at first, so the editor starts
  tidy. Tick a mod to arrange its elements too. Unticked mods still show in game.
- **Reset**: puts every element shown in the editor back where it started.
- **Customize**: opens the styling options below.
- **Done**: saves and closes the editor.

You can also drag the editor menu itself out of the way, or hide it with **D** to reach the elements under it.

### Layouts

Dashboard keeps three separate layouts. Each remembers its own
element positions, sizes, hidden elements, bar themes and bar numbers. To switch, bind **Next HUD Layout** in
**Controls**, under **Dashboard**, then press it in game. The editor always changes the layout you are on, and its **?**
tooltip tells you which one that is. The crosshair, hotbar, status effect, screen overlay and held item settings from
Customize are shared by all three.

## Customize

- **Bars**: change how health, food, armor, air, XP and other bars look, pick a theme, flip which way they fill, and
  add a number next to them (like `18/20` or a percent).
- **Crosshair**: choose a crosshair style and size. With Tinkers' Construct, you can keep its own crosshair for bows
  and other ranged weapons.
- **Hotbar**: show the slot number or bound key on each hotbar slot, and choose where, how big and in which color.
- **Status Effects**: change how potion effects are shown: background, icon, name, time, layout and sorting.
- **Screen Overlays**: turn down the pumpkin, portal, fire and underwater overlays, the vignette, the sleep fade, the
  hurt shake, the nausea wobble and the night vision flicker.
- **Rendering**: pick items that should look like an empty hand when held in first person.

Dashboard also adds an **Inventory** element: your 27 inventory slots on a see-through background, so you can see
what you carry without opening the inventory. It uses the inventory screen's look, so resource packs change it too.
It is hidden by default, turn it on in the editor, and **Ctrl + scroll** over it to change how see-through it is.

There is also an **Item Pickups** element: the items you pick up show for a moment at the side of the screen, like
`+12 Cobblestone`, and more of the same item adds to its row. It works on servers too, and is hidden by default.

## Supported Mods

Besides Minecraft's own HUD, Dashboard can move the HUD of these mods:

- **AppleCore**: saturation and exhaustion on the food bar
- **GraviSuite**: status text
- **IndustrialCraft 2**: the Nano and Quantum helmet HUD (armor charge and held item info)
<!-- TODO: bump version
- **JourneyMap**: the minimap (needs JourneyMap {version} or newer)
-->
- **Thaumcraft**: wand vis dial, warp meter, runic shielding, hover harness and research notifications
- **Tinkers' Construct**: its crosshair setting, and its hearts move with the health bar. Dashboard also adds a
  **Belt Hotbar** that shows the items in your Traveller's Belt (hidden by default, turn it on in the editor)
- **Witchery**: vampire blood meter, the blood of the creature you look at, infusion energy and creature power bars,
  infused brew and bark belt charges (the vampire power bar moves with the hotbar)
<!-- need pr
- **Overloaded Armor Bar**: its armor bar moves, resizes and hides like the vanilla one
-->

More mods are on the way.

<!-- need pr
### Mods using the Dashboard API

These mods use Dashboard's API to make their own HUD movable. This needs a recent version of both the mod and Dashboard:

- **Adventure Backpack**: potion icons and tanks
- **Akashic Tome**: the book hint next to the crosshair
- **Backhand**: the off-hand slot, separate from the hotbar
- **Battlegear 2**: shield block bar and quiver slots
- **Bogosorter**: the usage ticker (the hotbar swap column moves with the hotbar)
- **Botania**: mana bar, flight bar, boss bar, wand mode, corporea hint and the info around the crosshair
- **Draconic Evolution**: the tool and block info box, the armor HUD and the magnet icon
- **EnderIO**: the Conduit Probe modes and the Yeta Wrench mode (its selector and its icons in the corner)
- **Et Futurum**: subtitles, the sound captions (when turned on in Et Futurum's config)
- **Galacticraft**: oxygen tanks, oxygen warning, launch countdown, lander warning and rocket altitude
- **GalaxySpace**: the atmospheric pressure bar
- **GregTech**: the Power Goggles HUD
- **GTNHLib**: the text above the hotbar and titles, which other GTNH mods use
- **Hardcore Ender Expansion**: the notes in the bottom right and the energy cluster info
- **Infernal Mobs**: the elite mob health bar, with its name and modifiers
- **InGameInfoXML**: each text area (top left, top right, and so on)
- **LootGames**: the Minigames info in the top left
- **Nature's Compass**: the biome search text
- **Random Things**: the notification card
- **ServerUtilities**: its notifications, which move with the GTNHLib text above the hotbar
- **Tainted Magic**: Katana charge runes and Thaumic Disassembler mode text
- **TaskNH**: the pinned task list
- **Thaumic Tinkerer**: Dislocation focus box, Kami tool mode text and soul hearts
- **Waila**: the block info box (also when drawn by ChromaticTooltips, with a recent ChromaticTooltipsCompat)
-->

## Settings

Everything you change in the editor and in the Dashboard Customize menu is saved automatically. Some options can also be found in the
Dashboard config, in the **Mods** menu of the main screen.

## Your Own Images

Put PNG pictures in `config/dashboard/images/` (the folder is made on the first start), then restart or press
**F3 + T**. Each picture becomes an element named after its file, which you can move, resize and hide in the editor.
To hide or show all pictures at once, for example for screenshots, bind **Show/Hide Pictures** in **Controls**,
under **Dashboard**. While chat is open, you can also zoom into a picture: **scroll** to zoom, **drag** to move around in it and
**middle-click** to see the whole picture again. Moving and resizing pictures stays in the editor.

To change a picture's starting size, see-through look or animation, add an `images.json` to the same folder, using
the file names without `.png`. It takes the same settings as pack images, see the
[Texture Guide](src/main/resources/assets/dashboard/textures/gui/TextureGuide.md).

## For Resource Pack Makers

A resource pack can change how Dashboard looks and add to it, all from `assets/dashboard/textures/gui/`:

- **Bar themes**: new looks for the health, food, armor, XP and other bars, listed in `themes.json`.
- **Crosshairs**: extra crosshair styles in `crosshairs.png`.
- **Pack images**: pictures shown on the HUD, like a logo or a frame around the hotbar, listed in `images.json`. Each
  one is an element players can move, resize and hide in the editor. Images can be see-through and animated.

The [Texture Guide](src/main/resources/assets/dashboard/textures/gui/TextureGuide.md) explains each one: file names,
sheet layouts and every setting.

## For Mod Developers

Mods can make their own HUD movable, resizable and hideable in the editor, without depending on Dashboard. The
[API guide](docs/API.md) explains how.

## License

[![License](https://img.shields.io/badge/License-LGPL--3.0-yellow.svg?style=flat-square)](LICENSE)
