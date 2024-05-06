package org.figuramc.figura.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "figura", useMetadata = true)
public class FiguraModForge {
    // dummy empty mod class, we are client only
    public FiguraModForge() {
        MinecraftForge.EVENT_BUS.addListener(FiguraModClientForge::cancelVanillaOverlays);
        MinecraftForge.EVENT_BUS.addListener(FiguraModClientForge::renderOverlay);
        MinecraftForge.EVENT_BUS.addListener(FiguraModClientForge::renderUnderlay);
        if (FMLEnvironment.dist == Dist.CLIENT)
            FiguraModClientForge.registerResourceListeners();
    }
}
