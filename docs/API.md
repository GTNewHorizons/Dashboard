# Dashboard API

How a mod makes its own HUD movable, resizable and hideable in Dashboard's HUD editor, without depending on Dashboard.

Up to date with **Dashboard 1.0.20**. Newer releases may add API not covered here, see the
[version table](#2-minimum-dashboard-version).

The whole API is four classes in
[`com.gtnewhorizons.dashboard.api`](../src/main/java/com/gtnewhorizons/dashboard/api), only for use on the client:

| Class | What it is |
|---|---|
| [`HudElement`](../src/main/java/com/gtnewhorizons/dashboard/api/HudElement.java) | One thing on the HUD: its id, its default box and optional extras like a preview |
| [`HudRegistry`](../src/main/java/com/gtnewhorizons/dashboard/api/HudRegistry.java) | Registering elements and wrapping their drawing |
| [`HudBounds`](../src/main/java/com/gtnewhorizons/dashboard/api/HudBounds.java) | A rectangle in GUI scaled pixels |
| [`HudShift`](../src/main/java/com/gtnewhorizons/dashboard/api/HudShift.java) | Remembers a started move, for code that starts and ends it in different places, like two mixin hooks |

## How it works

The mod tells Dashboard where each element is drawn by default. While drawing, the mod wraps the element in
`beginRender` and `endRender`, which shift and resize everything drawn in between, from the default box to where the
player put it. When the player hid the element, `beginRender` returns false and the mod skips drawing it.

Without Dashboard, none of this runs, so the mod draws exactly as before.

## Setup

### 1. Dependency

In `dependencies.gradle`, compile against the `:api` jar. It is compile only, so it is not shipped with the mod and
players do not need Dashboard:

```groovy
compileOnly("com.github.GTNewHorizons:Dashboard:1.0.2:api")
```

GTNH's buildscript already knows where to find it. Other builds need GTNH's repository in `repositories.gradle`:

```groovy
maven {
    url = "https://nexus.gtnewhorizons.com/repository/public/"
}
```

### 2. Minimum Dashboard version

Declare Dashboard as an optional dependency with the same version as above, so an older Dashboard gives a clear
"update Dashboard" screen instead of a crash:

```java
@Mod(modid = "mymod", dependencies = "after:dashboard@[1.0.2,)")
```

If the mod's `mcmod.info` has `"useDependencyInformation": true`, Forge ignores the `@Mod` string, so the range goes in
its `"dependencies"` list instead: `"dashboard@[1.0.2,)"`. The `fml-client` log shows which one Forge used:
`Using mcmod dependency info` or `Parsed dependency info`.

**Which version:** the lowest Dashboard release that has every API call the mod uses and the lang keys for its element
names. That is 1.0.2 unless one of these applies:

- The mod calls API added later.
- The element names were added to Dashboard's lang file in a later release.
- An older Dashboard still has its own mixin into this mod, and the PR changes the code that mixin targets. Then use the
  first Dashboard release without that mixin, or the old mixin can fail or move things twice. Bogosorter and Backhand
  needed this.

Compiling against that same minimum makes it impossible to use newer API by accident.

| Version | What it means for mods using the API |
|---|---|
| 1.0.0 | First release with the API |
| 1.0.2 | The short constructor `new HudElement(id, bounds)`, and `.description` and `.own_settings` picked up automatically. The usual minimum |
| 1.0.3 to 1.0.7 | No API changes. They add or rename lang keys for some mods, and remove Dashboard's own hooks for mods that now use the API |
| 1.0.8 to 1.0.12 | No API changes. They remove Dashboard's own hooks for more mods that now use the API |
| 1.0.13 | Lang keys for GregTech |
| 1.0.14 | Lang keys for Random Things |
| 1.0.15 to 1.0.18 | No API changes |
| 1.0.19 | Lang keys for EnderIO |
| 1.0.20 | Lang keys for TaskNH, Akashic Tome and LootGames |

### 3. One helper class

Put every use of the API in a single class. Only call it after checking Dashboard is loaded, so that class is never
loaded without Dashboard:

```java
@SideOnly(Side.CLIENT)
public final class DashboardHud {

    private static final HudElement MANA_BAR = new HudElement(
        "mymod:mana_bar",
        (screenWidth, screenHeight) -> new HudBounds(screenWidth / 2 - 50, screenHeight - 40, 100, 5));

    private DashboardHud() {}

    public static void register() {
        MANA_BAR.setPreview(
            (screenWidth, screenHeight) -> HudRegistry
                .render(MANA_BAR, () -> ManaBarRenderer.drawSample(screenWidth, screenHeight)));
        HudRegistry.register(MANA_BAR);
    }

    /** False means the player hid the bar, so skip drawing it and do not call {@link #endManaBar()}. */
    public static boolean beginManaBar(int screenWidth, int screenHeight) {
        return HudRegistry.beginRender(MANA_BAR, screenWidth, screenHeight);
    }

    public static void endManaBar() {
        HudRegistry.endRender();
    }
}
```

### 4. Register

From the client proxy, in `preInit` or `init`, behind a loaded check. Caching the check is worth it, since the drawing
code asks every frame. Keep it **outside** the helper class, like in the proxy or a constants class: reading a field of
the helper loads the helper, and that crashes without Dashboard.

```java
public static final boolean DASHBOARD_LOADED = Loader.isModLoaded("dashboard");
```

```java
if (DASHBOARD_LOADED) {
    DashboardHud.register();
}
```

Dashboard registers its own fallback hooks for some mods in `postInit`. If the mod already registered the same id,
Dashboard's copy steps aside, so nothing moves twice. Registering the same id twice otherwise throws, so a mod
registering in `postInit`, after Dashboard's hook, would crash the game.

### 5. Wrap the drawing

```java
boolean dashboard = DASHBOARD_LOADED;
if (dashboard && !DashboardHud.beginManaBar(width, height)) {
    return;
}
drawManaBar(width, height);
if (dashboard) {
    DashboardHud.endManaBar();
}
```

When the element is hidden, skip the draw calls, but keep any state updates the drawing code does, like timers or
fading messages. Otherwise those freeze while hidden. `HudRegistry.render(element, () -> ...)` does begin, draw and end
in one call, when nothing needs to happen in between.

## A real example: Hardcore Ender Expansion

[Hardcore Ender Expansion #52](https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/pull/52) makes two elements
movable: the notes in the bottom right and the energy cluster info. The excerpts below are trimmed, see the PR for
the full code.

**Dependency and version.** It needs 1.0.3, not 1.0.2, because that is the first release with its lang keys:

```groovy
compileOnly('com.github.GTNewHorizons:Dashboard:1.0.3:api')
```

```java
dependencies = "required-after:gtnhlib@[0.5.22,);after:dashboard@[1.0.3,)"
```

**The loaded check**, in its overlay class, not in the helper. The client proxy calls `OverlayManager.register()`:

```java
private static final boolean DASHBOARD = Loader.isModLoaded("dashboard");

public static void register() {
    if (instance == null) {
        MinecraftForge.EVENT_BUS.register(instance = new OverlayManager());
        if (DASHBOARD) DashboardHud.register();
    }
}
```

**The helper class**, the only one importing the API. The cluster info's drawing was moved into its own method, so
the preview can draw it with sample values:

```java
private static final HudElement NOTIFICATIONS = new HudElement(
        "hardcoreenderexpansion:notifications",
        DashboardHud::getNotificationBounds);
// ENERGY_CLUSTER is made the same way, with a fixed box around the screen middle

static void register() {
    NOTIFICATIONS.setPreview(
            (screenWidth, screenHeight) -> HudRegistry
                    .render(NOTIFICATIONS, () -> drawNoteSample(screenWidth, screenHeight)));
    ENERGY_CLUSTER.setPreview(
            (screenWidth, screenHeight) -> HudRegistry.render(
                    ENERGY_CLUSTER,
                    () -> OverlayManager.drawClusterInfo(screenWidth >> 1, screenHeight >> 1,
                            SAMPLE_ENERGY, SAMPLE_MAX_ENERGY, EnergyClusterHealth.HEALTHY)));
    HudRegistry.register(NOTIFICATIONS);
    HudRegistry.register(ENERGY_CLUSTER);
}

static boolean beginNotifications() {
    return HudRegistry.beginRender(NOTIFICATIONS);
}

static void end() {
    HudRegistry.endRender();
}
```

**The drawing**, in the hotbar's `Post` at normal priority, so the notes do not move with the hotbar. While hidden,
the notes keep floating up and fading, so they do not all show up at once when shown again:

```java
@SubscribeEvent
public void onPostRenderGameOverlay(RenderGameOverlayEvent.Post e) {
    if (e.type == ElementType.HOTBAR) {
        if (!notifications.isEmpty()) {
            if (DASHBOARD && !DashboardHud.beginNotifications()) {
                // Hidden, so only keep the notes floating up and fading
                notifications.removeIf(Notification::update);
            } else {
                // ... draws the notes, which also updates them ...
                if (DASHBOARD) DashboardHud.end();
            }
        }
    }
}
```

## Element ids and names

- **Id:** `modid:name`, like `mymod:mana_bar`. It is saved in the player's layout, so **never rename it**, or
  players lose where they put the element. A mod taking over from Dashboard's own hook must keep Dashboard's old id.
- **Mod name:** taken from the part before `:`, matched to the mod id ignoring case. Use `setModId` only if that part
  is not the mod id.
- **Lang keys usually live in Dashboard's `en_US.lang`.** The short constructor names an element
  `dashboard.element.<id with ':' as '.'>`, like `dashboard.element.mymod.mana_bar`. The editor tooltip also shows
  these when they exist:
  - `<key>.description`: what the element is.
  - `<key>.own_settings`: where the mod's own position setting is, since moving the element in the editor overrides
    it, like `My Mod config > mana bar offset`.

  If the keys do not match the id, pass the name key with `new HudElement(id, translationKey, bounds)`, and the other
  keys with `setDescription` and `setOwnSettings`. Both also take key bindings: each one fills two `%s` in order, the
  bound key and then its name in Controls.
- **Why Dashboard's file:**
  - The names only show in Dashboard's own screens, like the editor tooltip and the Elements list. So they belong with
    the rest of the editor text, written the same way, and translators find all of it in one place.
  - Adding them is also a check: Dashboard looks at the mod's PR before the keys go in, so the element ids and the code
    using the API are confirmed to work before players save layouts with those ids.
- **Adding the keys:** open a PR to Dashboard adding a section for the mod to
  [`en_US.lang`](../src/main/resources/assets/dashboard/lang/en_US.lang). Until a Dashboard release has them, the editor
  shows the raw key, like `dashboard.element.mymod.mana_bar`. So the mod's minimum version is the first release with
  its keys.
- **Keys in the mod instead:** Minecraft merges every mod's lang files, so the same keys in the mod's own `en_US.lang`
  work too, and need no Dashboard release. Fine for a mod that would rather not wait on Dashboard.

## Default bounds

The default bounds are the box the player drags in the editor, and every move and resize is measured from it. They must
match exactly what is drawn when the player has not moved anything, in GUI scaled pixels. A box that is too big or in
the wrong place does not line up with the element in the editor, and the element resizes around the wrong point.

They are asked for often, every frame while drawing and in the editor, so keep them cheap.

When the size changes, like a list of messages or a line of text, return the current size. When the size is only known
while drawing, measure it then and return what the last frame drew, with a small placeholder before the first draw.
Measure before calling `beginRender`, so it is measured also while the player hid the element. To keep the element out
of the editor until it has drawn something, use `setActiveCheck` with "measured at least once".

## Optional extras

All return the element, so they can be chained.

| Method | Use it when |
|---|---|
| `setPreview(renderer)` | The editor should show a sample while the real element is not showing. Draw it through `HudRegistry.render`, so it moves like the real one |
| `setPreviewOnTop()` | The sample should draw over other elements, like a bar drawn over another bar |
| `setActiveCheck(supplier)` | The element should not exist for now, like when its feature is turned off. While it returns false, the element is left out of the editor and `beginRender` returns false, so it is not drawn either |
| `setHiddenByDefault()` | It is new HUD the player never had, which they turn on in the editor |
| `setModNameAddon(text)` | Another mod draws this element, shown after the mod name |

**When the element counts as showing:** `beginRender` and `shouldRender` mark it as drawn, and the editor shows the
real element instead of the preview for half a second after that. So only call them when there is something to draw,
like only while the player has an effect for an effect icon. Otherwise an empty box shows instead of the preview.

## Special cases

**Drawing during a vanilla HUD event.** Dashboard moves Minecraft's own HUD from that element's
`RenderGameOverlayEvent.Pre` to its `Post`. Anything drawn in between moves with that element, which is right for
extras on it and wrong for separate HUD:

| Vanilla elements | The move ends |
|---|---|
| Hotbar, XP bar, jump bar, boss bar | In `Post`, at priority `HIGHEST` |
| Health, armor, food, air, mount health | In `Post`, at priority `LOWEST`, so extras drawn on them in `Post` move too |

So draw separate HUD in the `Post` of the hotbar or XP bar at normal priority, or in another event, and wrap it in
its own `beginRender`. When the player hides a vanilla element, Forge skips its `Post`, so HUD drawn there would
disappear with it. Dashboard still sends it for the hotbar and XP bar, which is why those two are the safe choices.

**Drawing code that resets the matrix itself.** A reset would undo the shift. Call `HudRegistry.shouldRender(element)`
before drawing, and `HudRegistry.translateToPosition(element)` right after the reset. The drawing code must restore
the matrix afterwards, since these do not push it.

**Drawing in another scale.** `HudBounds` and all moves are in GUI scaled pixels. A mod drawing in screen pixels or
with its own scale has to scale around the move: scale down, translate, scale back up.

**Drawing at the end of the frame.** HUD drawn in a render tick handler draws over open screens, including the editor.
Call `HudRegistry.skipFrameEndHandler(this, this::onRenderTick, event)` at the start of the handler, and return when
it returns true, so Dashboard can run it at the right time.

**Drawing while the editor is open.** Most HUD only draws with no screen open. Let it draw when
`HudRegistry.isLayoutScreenOpen()` is true too, so players see the real thing while moving it.

**Mixins into another mod's method.** When start and end are in two different hooks, keep the state in a `HudShift`,
one per element. Its `beginOrHide` draws a hidden element invisibly instead of skipping it. Only use that when the
drawing can not be skipped, like one of several things in the same method.

**Remembering positions.** Code that caches where something is drawn can be told when the player moves, resizes, hides
or resets elements, with `HudRegistry.addLayoutListener`.

## Testing

Test in the mod's dev game with a temporary line, not committed:

```groovy
runtimeOnlyNonPublishable("com.github.GTNewHorizons:Dashboard:<version>:dev")
```

1. **The minimum version:** the element moves, resizes, hides and previews. If Dashboard ever had its own hook for the
   mod, check nothing moves twice.
2. **The newest Dashboard:** the same checks.
3. **Without Dashboard:** the HUD looks and works as before.
4. **One version below the minimum:** Forge stops at its version screen instead of crashing. Worth it when the minimum
   is above 1.0.2.
