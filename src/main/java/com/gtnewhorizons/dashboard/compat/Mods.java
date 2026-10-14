package com.gtnewhorizons.dashboard.compat;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import cpw.mods.fml.common.Loader;

/**
 * The other mods Dashboard adds things for, also used as mixin targets. Anything for one of them only runs when it is
 * loaded.
 */
public enum Mods implements IMod, ITargetMod { // ITargetMod for early mixin(+setCoreModClass)

    // spotless:off
    TINKERS("TConstruct", "tinker"),
    APPLECORE("AppleCore"),
    GRAVISUITE("GraviSuite"),
    GALACTICRAFT("GalacticraftCore"),
    GALAXYSPACE("GalaxySpace"),
    WAILA("Waila"),
    ADVENTURE_BACKPACK("adventurebackpack"),
    ;
    // spotless:on

    private final String id;
    private final String resourceDomain;
    private final TargetModBuilder targetBuilder;
    private Boolean loaded;

    Mods(String id) {
        this(id, id.toLowerCase(Locale.ENGLISH));
    }

    Mods(String id, String resourceDomain) {
        this.id = id;
        this.resourceDomain = resourceDomain;
        this.targetBuilder = new TargetModBuilder().setModId(id);
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
