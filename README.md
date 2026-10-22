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

## The HUD Editor

To open the editor, first bind **Open Dashboard HUD Editor** in **Controls**, under **Dashboard** (it has no key by default), then press that key.
Without a key, open it while in a world through **Mod Options**, **Dashboard**, **Config**, then **Open HUD Editor**.

Every part of the HUD gets its own box, even when it is not showing right now, so you can always find and grab it.

| Action                         | How                                                      |
|--------------------------------|----------------------------------------------------------|
| Move an element                | Drag the element (hold **Shift** to stop it snapping)    |
| Hide or show an element        | **Right-click** the element                              |
| Reset an element's position    | **Ctrl + click** the element                             |
| Resize an element              | **Alt + scroll** over the element                        |
| Reset an element's size        | **Alt + click** the element                              |
| Zoom into an image             | **Scroll** over it, **middle-drag** to move around, **middle-click** to reset |
| See the controls again         | Hover the **?** in the editor menu                       |

Hover an element to see its name, its size and which mod it belongs to. If that mod has its own position setting for
the element, the tooltip also tells you where to find it. Once you move the element in the editor, Dashboard's position
is used instead, until you reset it.

### Editor buttons

- **Grid**: shows a grid and snaps elements to it.
- **Preview**: shows sample content for elements that are not showing right now, so you can see what you are moving.
- **Labels**: shows each element's name inside its box.
- **Position**: shows each element's position next to its box.
- **Mods**: a list of mods with tick boxes. Only Minecraft and Dashboard are ticked at first, so the editor starts
  tidy. Tick a mod to arrange its elements too. Unticked mods still show in game.
- **Reset**: puts every element shown in the editor back where it started.
- **Customize**: opens the styling options below.
- **Done**: saves and closes the editor.

You can also drag the editor menu itself out of the way.

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

## Supported Mods

Besides Minecraft's own HUD, Dashboard can move the HUD of these mods:

- **AppleCore**: saturation and exhaustion on the food bar
- **Tinkers' Construct**: its crosshair setting, and its hearts move with the health bar. Dashboard also adds a
  **Belt Hotbar** that shows the items in your Traveller's Belt (hidden by default, turn it on in the editor)
- **Galacticraft**: oxygen tanks, oxygen warning, launch countdown, lander warning and rocket altitude
- **GalaxySpace**: atmospheric pressure bar
- **GraviSuite**: status text
- **Waila**: the block info box (also when drawn by ChromaticTooltips)
- **Adventure Backpack**: potion icons and tanks
- **Botania**: mana bar, flight bar, boss bar, wand mode, corporea hint and the info around the crosshair
- **Backhand**: the off-hand slot, separate from the hotbar
- **InGameInfoXML**: each text area (top left, top right, and so on)
- **Draconic Evolution**: the tool and block info box, the armor HUD and the magnet icon
- **Thaumcraft**: wand vis dial, warp meter, runic shielding, hover harness and research notifications
- **Thaumic Tinkerer**: Dislocation focus box, Kami tool mode text and soul hearts
- **Tainted Magic**: Katana charge runes and Thaumic Disassembler mode text
- **Witchery**: vampire blood meter, the blood of the creature you look at, infusion energy and creature power bars,
  infused brew and bark belt charges (the vampire power bar moves with the hotbar)
- **IndustrialCraft 2**: the Nano and Quantum helmet HUD (armor charge and held item info)
- **Battlegear 2**: shield block bar and quiver slots
- **Nature's Compass**: the biome search text
- **GTNHLib**: the text above the hotbar and titles, which other GTNH mods use
- **Infernal Mobs**: the elite mob health bar, with its name and modifiers
- **ServerUtilities**: its notifications, which move with the GTNHLib text above the hotbar

More mods are on the way.

<!-- need pr
### Mods that support Dashboard themselves

These mods add their own Dashboard support, so their HUD can be moved as soon as both are installed:

- **Bogosorter**: the usage ticker (the hotbar swap column moves with the hotbar)
- **Hardcore Ender Expansion**: the notes in the bottom right and the energy cluster info
- **Et Futurum**: subtitles, the sound captions (when turned on in Et Futurum's config)
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

## License

[![License](https://img.shields.io/badge/License-LGPL--3.0-yellow.svg?style=flat-square)](LICENSE)
