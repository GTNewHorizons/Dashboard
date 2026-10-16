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

Press **Numpad 7** to open the editor. You can change the key in **Controls**, under **Dashboard**.

Every part of the HUD gets its own box, even when it is not showing right now, so you can always find and grab it.

| Action                         | How                                                      |
|--------------------------------|----------------------------------------------------------|
| Move an element                | Drag the element (hold **Shift** to stop it snapping)    |
| Hide or show an element        | **Right-click** the element                              |
| Reset an element's position    | **Ctrl + click** the element                             |
| Resize an element              | **Alt + scroll** over the element                        |
| Reset an element's size        | **Alt + click** the element                              |
| See the controls again         | Hover the **?** in the editor menu                       |

Hover an element to see its name, its size and which mod it belongs to. If that mod has its own position setting for
the element, the tooltip also tells you where to find it. Once you move the element in the editor, Dashboard's position
is used instead, until you reset it.

### Editor buttons

- **Grid**: shows a grid and snaps elements to it.
- **Preview**: shows sample content for elements that are not showing right now, so you can see what you are moving.
- **Labels**: shows each element's name inside its box.
- **Position**: shows each element's position next to its box.
- **Mods**: a list of mods with tick boxes. Untick a mod to leave its elements out of the editor and tidy it up.
  They still show in game.
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
- **Tinkers' Construct**: its crosshair setting, and its hearts move with the health bar
- **Galacticraft**: oxygen tanks, oxygen warning, launch countdown, lander warning and rocket altitude
- **GalaxySpace**: atmospheric pressure bar
- **GraviSuite**: status text
- **Waila**: the block info box (also when drawn by ChromaticTooltips)
- **Adventure Backpack**: potion icons and tanks
- **Botania**: mana bar, flight bar, boss bar, wand mode, corporea hint and the info around the crosshair
- **Backhand**: the off-hand slot, separate from the hotbar
- **InGameInfoXML**: each text area (top left, top right, and so on)
- **Draconic Evolution**: the tool and block info box, the armor HUD and the magnet icon

More mods are on the way.

## Settings

Everything you change in the editor and in the Dashboard Customize menu is saved automatically. Some options can also be found in the
Dashboard config, in the **Mods** menu of the main screen.

## License

[![License](https://img.shields.io/badge/License-LGPL--3.0-yellow.svg?style=flat-square)](LICENSE)
