package com.gtnewhorizons.dashboard.compat;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import cpw.mods.fml.common.Loader;

/**
 * Mods Dashboard supports, also used as mixin targets. Their compat only runs when the mod is loaded. Keep it as
 * lambdas, not method references, which would load compat classes even without their mod and crash.
 */
public enum Mods implements IMod, ITargetMod { // ITargetMod for early mixin(+setCoreModClass)

    // spotless:off
    TINKERS("TConstruct", "tinker", () -> TinkersBeltHotbar.register()),
    APPLECORE("AppleCore"),
    GRAVISUITE("GraviSuite", () -> GraviSuiteCompat.register()),
    GALACTICRAFT("GalacticraftCore", () -> GalacticraftCompat.register()),
    GALAXYSPACE("GalaxySpace", () -> GalaxySpaceCompat.register()),
    WAILA("Waila", () -> WailaCompat.register()),
    ADVENTURE_BACKPACK("adventurebackpack", () -> AdventureBackpackCompat.register()),
    BOTANIA("Botania", () -> BotaniaCompat.register()),
    BACKHAND("backhand", () -> BackhandCompat.register()),
    INGAMEINFO("InGameInfoXML", () -> InGameInfoCompat.register()),
    DRACONIC("DraconicEvolution", () -> DraconicCompat.register()),
    THAUMCRAFT("Thaumcraft", () -> ThaumcraftCompat.register()),
    THAUMIC_TINKERER("ThaumicTinkerer", () -> ThaumicTinkererCompat.register()),
    TAINTED_MAGIC("TaintedMagic", () -> TaintedMagicCompat.register()),
    WITCHERY("witchery", () -> WitcheryCompat.register()),
    IC2("IC2", () -> Ic2Compat.register()),
    BATTLEGEAR("battlegear2", () -> BattlegearCompat.register()),
    NATURES_COMPASS("naturescompass", () -> NaturesCompassCompat.register()),
    GTNHLIB("gtnhlib", () -> GtnhLibCompat.register()),
    INFERNAL_MOBS("InfernalMobs", () -> InfernalMobsCompat.register()),
    ET_FUTURUM("etfuturum", () -> EtFuturumCompat.register()),
    HEE("HardcoreEnderExpansion", () -> HeeCompat.register()),
    SERVER_UTILITIES("serverutilities"),
    ;
    // spotless:on

    private final String id;
    private final String resourceDomain;
    private final TargetModBuilder targetBuilder;
    private final Runnable registerCompat;
    private Boolean loaded;

    Mods(String id) {
        this(id, null);
    }

    Mods(String id, Runnable registerCompat) {
        this(id, id.toLowerCase(Locale.ENGLISH), registerCompat);
    }

    Mods(String id, String resourceDomain, Runnable registerCompat) {
        this.id = id;
        this.resourceDomain = resourceDomain;
        this.targetBuilder = new TargetModBuilder().setModId(id);
        this.registerCompat = registerCompat;
    }

    /** Client only, since the additions are HUD. */
    public static void registerLoadedCompat() {
        for (Mods mod : values()) {
            if (mod.registerCompat != null && mod.isModLoaded()) {
                mod.registerCompat.run();
            }
        }
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return targetBuilder;
    }

    @Override
    public boolean isModLoaded() {
        if (loaded == null) {
            loaded = Loader.isModLoaded(id);
        }
        return loaded;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }
}
