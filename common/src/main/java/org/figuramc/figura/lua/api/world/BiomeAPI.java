package org.figuramc.figura.lua.api.world;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.RegistryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@LuaWhitelist
@LuaTypeDoc(
        name = "Biome",
        value = "biome"
)
public class BiomeAPI {

    private final Biome biome;
    private BlockPos pos;

    @LuaWhitelist
    @LuaFieldDoc("biome.id")
    public final String id;

    public BiomeAPI(Biome biome, BlockPos pos) {
        this.biome = biome;
        this.pos = pos;
        this.id = RegistryUtils.getResourceLocationForRegistryObj(Biome.class, biome).toString();
    }

    protected BlockPos getBlockPos() {
        return pos == null ? BlockPos.ORIGIN : pos;
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_pos")
    public FiguraVec3 getPos() {
        return FiguraVec3.fromBlockPos(getBlockPos());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "pos",
            value = "biome.set_pos"
    )
    public BiomeAPI setPos(Object x, Double y, Double z) {
        FiguraVec3 newPos = LuaUtils.parseVec3("setPos", x, y, z);
        pos = newPos.asBlockPos();
        return this;
    }

    @LuaWhitelist
    public BiomeAPI pos(Object x, Double y, Double z) {
        return setPos(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_tags")
    public List<String> getTags() {
        List<String> list = new ArrayList<>();

       /* Registry<Biome> registry = WorldAPI.getCurrentWorld().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        Optional<ResourceKey<Biome>> key = registry.getResourceKey(biome);

        if (key.isPresent())
            return list;

        list.add(biome.getTempCategory().name());*/
        // TODO : see what i can do for biome tags
        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_temperature")
    public float getTemperature() {
        return biome.getDefaultTemperature();
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_precipitation")
    public String getPrecipitation() {
        return biome.getEnableSnow() ? "snow" : biome.canRain() ? "rain" : "none";
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_sky_color")
    public FiguraVec3 getSkyColor() {
        return ColorUtils.intToRGB(biome.getSkyColorByTemp(biome.getTemperature(pos)));
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_foliage_color")
    public FiguraVec3 getFoliageColor() {
        return ColorUtils.intToRGB(biome.getFoliageColorAtPos(pos));
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_grass_color")
    public FiguraVec3 getGrassColor() {
        BlockPos pos = getBlockPos();
        return ColorUtils.intToRGB(biome.getGrassColorAtPos(pos));
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_fog_color")
    public FiguraVec3 getFogColor() {
        ResourceLocation biomeID = RegistryUtils.getResourceLocationForRegistryObj(Biome.class, biome);
        return ColorUtils.intToRGB(biomeID.getResourcePath().contains("hell") || biomeID.getResourcePath().contains("nether") ? 0x330808 : biomeID.getResourcePath().contains("sky") || biomeID.getResourcePath().contains("end") ? 0xA080A0 : 12638463);
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_water_color")
    public FiguraVec3 getWaterColor() {
        return ColorUtils.intToRGB(biome.getWaterColor());
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_water_fog_color")
    public FiguraVec3 getWaterFogColor() {
        return ColorUtils.intToRGB(329011);
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.get_downfall")
    public float getDownfall() {
        return biome.getRainfall();
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.is_hot")
    public boolean isHot() {
        return  biome.getTemperature(getBlockPos()) > 1f;
    }

    @LuaWhitelist
    @LuaMethodDoc("biome.is_cold")
    public boolean isCold() {
        return !(biome.getTemperature(getBlockPos()) >= 0.15f);
    }

    @LuaWhitelist
    public boolean __eq(BiomeAPI other) {
        return this.biome.equals(other.biome);
    }

    @LuaWhitelist
    public Object __index(String arg) {
        return "id".equals(arg) ? id : null;
    }

    @Override
    public String toString() {
        return id + " (Biome)";
    }
}
