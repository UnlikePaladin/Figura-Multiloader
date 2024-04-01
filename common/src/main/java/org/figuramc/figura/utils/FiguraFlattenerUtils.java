package org.figuramc.figura.utils;

import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.figuramc.figura.mixin.accessors.BlockFlowerAccessor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FiguraFlattenerUtils {

    public static String serializeBlockState(World world, BlockPos pos, IBlockState state) {
        StringBuilder stringBuilder = new StringBuilder(theStateFlattinator2000(world, pos, state).toString());
        if (!state.getProperties().isEmpty()) {
            stringBuilder.append('[');
            boolean addComma = false;
            for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
                if (addComma) {
                    stringBuilder.append(',');
                }
                addProperty(stringBuilder, entry.getKey(), (Comparable)entry.getValue());
                addComma = true;
            }
            stringBuilder.append(']');
        }
        return stringBuilder.toString();
    }

    private static <T extends Comparable<T>> void addProperty(StringBuilder builder, IProperty<T> property, Comparable<T> value) {
        builder.append(property.getName());
        builder.append('=');
        builder.append(property.getName((T) value));
    }

    private static final Map<IBlockState, ResourceLocation> blockStateResourceLocationMap = new HashMap<>();
    public static ResourceLocation theStateFlattinator2000(World world, BlockPos pos, IBlockState state) {
        if (blockStateResourceLocationMap.containsKey(state) && !state.getBlock().hasTileEntity())
            return blockStateResourceLocationMap.get(state);

        ResourceLocation originalLocation = RegistryUtils.getResourceLocationForRegistryObj(Block.class, state.getBlock());

        ResourceLocation flattenedLocation;
        switch (originalLocation.getResourcePath()) {
            case "reeds": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sugar_cane");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "sponge": {
                boolean wet = state.getValue(BlockSponge.WET);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), wet ? "wet_sponge" : "sponge");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "golden_rail": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "powered_rail");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "noteblock": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "note_block");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "web": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "cobweb");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "tallgrass": {
                BlockTallGrass.EnumType type = state.getValue(BlockTallGrass.TYPE);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName().replace("tall_", ""));
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "grass": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "grass_block");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "piston_extension": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "moving_piston");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "flowing_water": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "water");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "flowing_lava": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "lava");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "log": {
                BlockPlanks.EnumType logType = state.getValue(BlockOldLog.VARIANT);
                BlockLog.EnumAxis axis = state.getValue(BlockOldLog.LOG_AXIS);
                String suffix = "log";
                if (axis == BlockLog.EnumAxis.NONE) {
                    suffix = "wood";
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), logType.getName() + "_" + suffix);
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "log2": {
                BlockPlanks.EnumType logType = state.getValue(BlockNewLog.VARIANT);
                BlockLog.EnumAxis axis = state.getValue(BlockNewLog.LOG_AXIS);
                String suffix = "log";
                if (axis == BlockLog.EnumAxis.NONE) {
                    suffix = "wood";
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), logType.getName() + "_" + suffix);
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "leaves": {
                BlockPlanks.EnumType leafType = state.getValue(BlockOldLeaf.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), leafType.getName() + "_leaves");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "leaves2": {
                BlockPlanks.EnumType leafType = state.getValue(BlockNewLeaf.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), leafType.getName() + "_leaves");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "planks": {
                BlockPlanks.EnumType plankType = state.getValue(BlockPlanks.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), plankType.getName() + "_planks");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "stone": {
                BlockStone.EnumType stoneType = state.getValue(BlockStone.VARIANT);
                String stoneName = stoneType.getName();
                if (stoneName.contains("smooth"))
                    stoneName = "polished_" + stoneName.replace("smooth_", "");
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), stoneName);
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "dirt": {
                BlockDirt.DirtType dirtType = state.getValue(BlockDirt.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), dirtType.getName());
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "sapling": {
                BlockPlanks.EnumType saplingType = state.getValue(BlockSapling.TYPE);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), saplingType.getName() + "_sapling");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "bed": {
                TileEntity bedEntity = world.getTileEntity(pos);
                if (bedEntity instanceof TileEntityBed) {
                    flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(),
                            ((TileEntityBed) bedEntity).getColor().getName()
                                    .replace("silver", "light_gray") + "_bed");
                    blockStateResourceLocationMap.put(state, flattenedLocation);
                    return flattenedLocation;
                }
                return new ResourceLocation(originalLocation.getResourceDomain(), "red_bed");
            }
            case "concrete_powder": {
                EnumDyeColor color = state.getValue(BlockConcretePowder.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_concrete_powder");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "concrete": {
                EnumDyeColor color = state.getValue(BlockColored.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_concrete");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "wool": {
                EnumDyeColor color = state.getValue(BlockColored.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_wool");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "stained_hardened_clay": {
                EnumDyeColor color = state.getValue(BlockStainedHardenedClay.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_terracotta");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "hardened_clay": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "terracotta");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "double_plant": {
                BlockDoublePlant.EnumPlantType plantType = state.getValue(BlockDoublePlant.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), plantType.getName()
                        .replace("paeonia", "peony")
                        .replace("double_rose", "rose_bush")
                        .replace("double_fern", "large_fern")
                        .replace("double_grass", "tall_grass")
                        .replace("syringa", "lilac")
                );
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "deadbush": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "dead_bush");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "portal": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_portal");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "sand": {
                BlockSand.EnumType color = state.getValue(BlockSand.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName());
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "sandstone": {
                BlockSandStone.EnumType type = state.getValue(BlockSandStone.TYPE);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName()
                        .replace("smooth", "cut"));
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "yellow_flower": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "dandelion");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "red_flower": {
                BlockFlower.EnumFlowerType type = state.getValue(BlockFlowerAccessor.getType());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName()
                        .replace("houstonia", "azure_bluet"));
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "double_wooden_slab":
            case "wooden_slab": {
                BlockPlanks.EnumType plankType = state.getValue(BlockDoubleWoodSlab.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), plankType.getName() + "_slab");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "double_stone_slab":
            case "stone_slab": {
                BlockStoneSlab.EnumType stoneType = state.getValue(BlockStoneSlab.VARIANT);
                boolean seamless = state.getValue(BlockStoneSlab.SEAMLESS);
                String blockName = stoneType.getName();
                if (seamless) {
                    blockName = "smooth_" + blockName;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(),  blockName + "_slab");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "double_stone_slab2":
            case "stone_slab2": {
                BlockStoneSlabNew.EnumType sandType = state.getValue(BlockStoneSlabNew.VARIANT);
                boolean seamless = state.getValue(BlockStoneSlabNew.SEAMLESS);
                String blockName = sandType.getName();
                if (seamless) {
                    blockName = "smooth_" + blockName;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), blockName + "_slab");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "double_purpur_slab":
            case "purpur_slab": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "purpur_slab");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "brick_block": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "bricks");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "mob_spawner": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "spawner");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "torch": {
                String preFix = "";
                if (!state.getValue(BlockTorch.FACING).getAxis().isHorizontal()) {
                    preFix = "wall_";
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), preFix + "torch");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_furnace":
            case "furnace": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "furnace");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "stone_stairs": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "cobblestone_stairs");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "wooden_pressure_plate": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_pressure_plate");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_redstone_ore":
            case "redstone_ore": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "redstone_ore");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "redstone_torch":
            case "unlit_redstone_torch": {
                String preFix = "";
                if (!state.getValue(BlockTorch.FACING).getAxis().isHorizontal()) {
                    preFix = "wall_";
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "redstone_"+ preFix + "torch");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "snow_layer": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "snow");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "snow": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "snow_block");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "fence": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_fence");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "pumpkin": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "carved_pumpkin");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_pumpkin": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "jack_o_lantern");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "trapdoor": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_trapdoor");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "monster_egg": {
                BlockSilverfish.EnumType stoneType = state.getValue(BlockSilverfish.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "infested_" + stoneType.getName().replace("brick", "bricks"));
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "stonebrick": {
                BlockStoneBrick.EnumType brickType = state.getValue(BlockStoneBrick.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), brickType.getName()
                        .replace("stonebrick", "stone_bricks"));
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "red_mushroom_block":
            case "brown_mushroom_block": {
                BlockHugeMushroom.EnumType shroomType = state.getValue(BlockHugeMushroom.VARIANT);
                if (shroomType == BlockHugeMushroom.EnumType.ALL_STEM || shroomType == BlockHugeMushroom.EnumType.STEM)
                   flattenedLocation= new ResourceLocation(originalLocation.getResourceDomain(), "mushroom_stem");
                else
                    flattenedLocation= originalLocation;

                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "melon_block": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "melon");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "fence_gate": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_fence_gate");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "waterlily": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "lily_pad");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "nether_brick": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_bricks");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "end_bricks": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "end_stone_bricks");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_redstone_lamp":
            case "redstone_lamp": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "redstone_lamp");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "wooden_button": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_button");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "anvil": {
                int damage = state.getValue(BlockAnvil.DAMAGE);
                String name;
                switch (damage) {
                    case 1:
                        name = "chipped_anvil"; break;
                    case 2:
                        name = "damaged_anvil"; break;
                    default:
                        name = "anvil"; break;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), name);
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "daylight_detector":
            case "daylight_detector_inverted": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "daylight_detector");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "quartz_ore": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_quartz_ore");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "quartz_block": {
                BlockQuartz.EnumType quartzType = state.getValue(BlockQuartz.VARIANT);
                String name;
                switch (quartzType) {
                    case CHISELED:
                        name = "chiseled_quartz_block"; break;
                    case DEFAULT:
                        name = "quartz_block"; break;
                    default:
                        name = "quartz_pillar"; break;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), name);
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "carpet": {
                EnumDyeColor color = state.getValue(BlockCarpet.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_carpet");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "slime": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "slime_block");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "stained_glass_pane": {
                EnumDyeColor color = state.getValue(BlockStainedGlassPane.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_stained_glass_pane");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "stained_glass": {
                EnumDyeColor color = state.getValue(BlockStainedGlass.COLOR);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_stained_glass");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "prismarine": {
                BlockPrismarine.EnumType type = state.getValue(BlockPrismarine.VARIANT);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName());
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "red_sandstone": {
                BlockRedSandstone.EnumType type = state.getValue(BlockRedSandstone.TYPE);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName()
                        .replace("smooth", "cut"));
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "magma": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "magma_block");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "red_nether_brick": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "red_nether_bricks");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "silver_shulker_box": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "light_gray_shulker_box");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "silver_banner": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "light_gray_banner");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "silver_glazed_terracotta": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "light_gray_glazed_terracotta");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "wooden_door": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_door");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "powered_repeater":
            case "repeater":
            case "unpowered_repeater": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "repeater");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "powered_comparator":
            case "comparator":
            case "unpowered_comparator": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "comparator");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "flower_pot": {
                BlockFlowerPot.EnumFlowerType type = state.getValue(BlockFlowerPot.CONTENTS);
                if (type != BlockFlowerPot.EnumFlowerType.EMPTY) {
                    flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "potted_" + type.getName()
                            .replace("houstonia", "azure_bluet")
                            .replace("rose", "poppy")
                            .replace("mushroom_red", "red_mushroom")
                            .replace("mushroom_brown", "brown_mushroom"));
                } else {
                    flattenedLocation = originalLocation;
                }
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "skull": {
                int type = 0;
                String name;
                TileEntity entity = world.getTileEntity(pos);
                EnumFacing enumFacing = EnumFacing.NORTH;
                if (entity instanceof TileEntitySkull) {
                    type = ((TileEntitySkull) entity).getSkullType();
                    enumFacing = EnumFacing.getFront(entity.getBlockMetadata() & 7);
                }
                switch (type) {
                    case 0:
                        name = "skeleton_skull"; break;
                    case 1:
                        name = "wither_skeleton_skull"; break;
                    case 2:
                        name = "zombie_head"; break;
                    case 3:
                        name = "player_head"; break;
                    case 4:
                        name = "creeper_head"; break;
                    default:
                        name = "dragon_head"; break;
                }
                if (!enumFacing.getAxis().isVertical()) {
                    name = name.replace("head", "wall_head");
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), name);
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "standing_banner":
            case "banner": {
                TileEntity entity = world.getTileEntity(pos);
                EnumDyeColor color = EnumDyeColor.WHITE;
                if (entity instanceof TileEntityBanner) {
                    color = ((TileEntityBanner) entity).getColorList().get(0);
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_banner");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "wall_banner": {
                TileEntity entity = world.getTileEntity(pos);
                EnumDyeColor color = EnumDyeColor.WHITE;
                if (entity instanceof TileEntityBanner) {
                    color = ((TileEntityBanner) entity).getColorList().get(0);
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_wall_banner");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "chorus_fruit_popped": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "popped_chorus_fruit");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "pumpkin_stem": {
                EnumFacing facing = state.getValue(BlockStem.FACING);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), facing == EnumFacing.UP ? "pumpkin_stem" : "attached_pumpkin_stem");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            case "melon_stem": {
                EnumFacing facing = state.getValue(BlockStem.FACING);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), facing == EnumFacing.UP ? "melon_stem" : "attached_melon_stem");
                blockStateResourceLocationMap.put(state, flattenedLocation);
                return flattenedLocation;
            }
            default:
                if (world != null && state.getBlock().hasTileEntity() && world.getTileEntity(pos) != null)
                    blockStateResourceLocationMap.put(state, originalLocation);
                return originalLocation;
        }
    }


    private static final Map<ResourceLocation, IBlockState> flattenedResourceLocationMap = new HashMap<>();
    public static IBlockState stateUnflattinator2000(ResourceLocation originalLocation) {
        if (flattenedResourceLocationMap.containsKey(originalLocation))
            return flattenedResourceLocationMap.get(originalLocation);

        ResourceLocation unflattenedLocation;
        switch (originalLocation.getResourcePath()) {
            case "sugar_cane": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "reeds");
                break;
            }
            case "wet_sponge": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sponge");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockSponge.WET, true);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "sponge": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sponge");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockSponge.WET, false);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "powered_rail": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "golden_rail");
                break;
            }
            case "note_block": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "noteblock");
                break;
            }
            case "cobweb": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "web");
                break;
            }
            case "large_fern": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "double_plant");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.FERN);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "sunflower": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "double_plant");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.SUNFLOWER);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lilac": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "double_plant");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.SYRINGA);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "tall_grass": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "double_plant");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.GRASS);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "rose_bush": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "double_plant");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.ROSE);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "peony": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "double_plant");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.PAEONIA);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "dead_bush": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "deadbush");
                break;
            }
            case "grass_block": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "grass");
                break;
            }
            case "moving_piston": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "piston_extension");
                break;
            }
            case "acacia_log":
            case "jungle_log":
            case "dark_oak_log":
            case "spruce_log":
            case "birch_log":
            case "oak_log":
            case "acacia_wood":
            case "jungle_wood":
            case "dark_oak_wood":
            case "spruce_wood":
            case "birch_wood":
            case "oak_wood":{
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("_log", "").replace("_wood", "").toUpperCase(Locale.US));
                BlockLog.EnumAxis type = originalLocation.getResourcePath().contains("log") ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.NONE;
                String block = "log";
                if (logType.getMetadata() >= 4) {
                    block = "log2";
                }
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), block);
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                if (block.equals("log2")) {
                    state.withProperty(BlockNewLog.VARIANT, logType);
                } else {
                    state.withProperty(BlockOldLog.VARIANT, logType);
                }
                state.withProperty(BlockLog.LOG_AXIS, type);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "acacia_leaves":
            case "jungle_leaves":
            case "dark_oak_leaves":
            case "spruce_leaves":
            case "birch_leaves":
            case "oak_leaves":{
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("_leaves", "").toUpperCase(Locale.US));
                String block = "leaves";
                if (logType.getMetadata() >= 4) {
                    block = "leaves2";
                }
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), block);
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                if (block.equals("leaves2")) {
                    state.withProperty(BlockNewLeaf.VARIANT, logType);
                } else {
                    state.withProperty(BlockOldLeaf.VARIANT, logType);
                }
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "acacia_planks":
            case "jungle_planks":
            case "dark_oak_planks":
            case "spruce_planks":
            case "birch_planks":
            case "oak_planks":{
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("_planks", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "planks");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockPlanks.VARIANT, logType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "polished_diorite":
            case "polished_granite":
            case "polished_andesite":
            case "andesite":
            case "diorite":
            case "granite":
            case "stone": {
                String suffix = originalLocation.getResourcePath().contains("polished") ? "_smooth" : "";
                BlockStone.EnumType stoneType = BlockStone.EnumType.valueOf((originalLocation.getResourcePath()
                        .replace("polished_", "") + suffix).toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "stone");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStone.VARIANT, stoneType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "dirt":
            case "coarse_dirt":
            case "podzol":{
                BlockDirt.DirtType dirtType = BlockDirt.DirtType.valueOf(originalLocation.getResourcePath().toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "dirt");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDirt.VARIANT, dirtType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "acacia_sapling":
            case "jungle_sapling":
            case "dark_oak_sapling":
            case "spruce_sapling":
            case "birch_sapling":
            case "oak_sapling":{
                BlockPlanks.EnumType saplingType = BlockPlanks.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("_sapling", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sapling");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockSapling.TYPE, saplingType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lime_bed":
            case "orange_bed":
            case "magenta_bed":
            case "light_blue_bed":
            case "yellow_bed":
            case "pink_bed":
            case "gray_bed":
            case "light_gray_bed":
            case "purple_bed":
            case "brown_bed":
            case "black_bed":
            case "cyan_bed":
            case "blue_bed":
            case "green_bed":
            case "white_bed":
            case "red_bed": {
                // MC used to store bed color in the block entity :pain:
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "bed");
                break;
            }
            case "lime_concrete_powder":
            case "orange_concrete_powder":
            case "magenta_concrete_powder":
            case "light_blue_concrete_powder":
            case "yellow_concrete_powder":
            case "pink_concrete_powder":
            case "gray_concrete_powder":
            case "light_gray_concrete_powder":
            case "purple_concrete_powder":
            case "brown_concrete_powder":
            case "black_concrete_powder":
            case "cyan_concrete_powder":
            case "blue_concrete_powder":
            case "green_concrete_powder":
            case "white_concrete_powder":
            case "red_concrete_powder": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_concrete_powder", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "concrete_powder");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockColored.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lime_concrete":
            case "orange_concrete":
            case "magenta_concrete":
            case "light_blue_concrete":
            case "yellow_concrete":
            case "pink_concrete":
            case "gray_concrete":
            case "light_gray_concrete":
            case "purple_concrete":
            case "brown_concrete":
            case "black_concrete":
            case "cyan_concrete":
            case "blue_concrete":
            case "green_concrete":
            case "white_concrete":
            case "red_concrete": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_concrete", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "concrete");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockColored.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lime_wool":
            case "orange_wool":
            case "magenta_wool":
            case "light_blue_wool":
            case "yellow_wool":
            case "pink_wool":
            case "gray_wool":
            case "light_gray_wool":
            case "purple_wool":
            case "brown_wool":
            case "black_wool":
            case "cyan_wool":
            case "blue_wool":
            case "green_wool":
            case "white_wool":
            case "red_wool": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_wool", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "wool");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockColored.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lime_terracotta":
            case "orange_terracotta":
            case "magenta_terracotta":
            case "light_blue_terracotta":
            case "yellow_terracotta":
            case "pink_terracotta":
            case "gray_terracotta":
            case "light_gray_terracotta":
            case "purple_terracotta":
            case "brown_terracotta":
            case "black_terracotta":
            case "cyan_terracotta":
            case "blue_terracotta":
            case "green_terracotta":
            case "white_terracotta":
            case "red_terracotta": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_terracotta", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "stained_hardened_clay");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockColored.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "terracotta": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "hardened_clay");
                break;
            }
            case "nether_portal": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "portal");
                break;
            }
            case "red_sand":
            case "sand": {
                BlockSand.EnumType color = BlockSand.EnumType.valueOf(originalLocation.getResourcePath().toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sand");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockSand.VARIANT, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "sandstone":
            case "chiseled_sandstone":
            case "cut_sandstone":{
                BlockSandStone.EnumType type = originalLocation.getResourcePath().contains("chiseled") ?
                        BlockSandStone.EnumType.CHISELED : originalLocation.getResourcePath().contains("cut") ?
                        BlockSandStone.EnumType.SMOOTH : BlockSandStone.EnumType.DEFAULT;

                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sandstone");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockSandStone.TYPE, type);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "dandelion": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "yellow_flower");
                break;
            }
            case "oxeye_daisy":
            case "pink_tulip":
            case "white_tulip":
            case "orange_tulip":
            case "red_tulip":
            case "azure_bluet":
            case "allium":
            case "blue_orchid":
            case "poppy": {
                BlockFlower.EnumFlowerType type = BlockFlower.EnumFlowerType.valueOf(originalLocation.getResourcePath()
                        .replace("azure_bluet", "houstonia").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "red_flower");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockFlowerAccessor.getType(), type);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "acacia_slab":
            case "jungle_slab":
            case "dark_oak_slab":
            case "spruce_slab":
            case "birch_slab":
            case "oak_slab":{
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("_slab", "").toUpperCase(Locale.US));
                String block = "double_wooden_slab";
                if (logType.getMetadata() >= 4) {
                    block = "wooden_slab";
                }
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), block);
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                if (block.equals("double_wooden_slab")) {
                    state.withProperty(BlockDoubleWoodSlab.VARIANT, logType);
                } else {
                    state.withProperty(BlockHalfWoodSlab.VARIANT, logType);
                }
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "stone_slab":
            case "sandstone_slab":
            case "petrified_oak_slab":
            case "cobblestone_slab":
            case "brick_slab":
            case "stone_brick_slab":
            case "nether_brick_slab":
            case "quartz_slab":
            case "smooth_stone":
            case "smooth_sandstone":
            case "smooth_quartz":{
                BlockDoubleStoneSlab.EnumType stoneType = BlockDoubleStoneSlab.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("petrified_oak", "wood")
                        .replace("sandstone", "sand")
                        .replace("stone_brick", "smoothbrick")
                        .replace("nether_brick", "netherbrick")
                        .replace("smooth_", "")
                        .replace("_slab", "").toUpperCase(Locale.US));
                String block = "double_stone_slab";
                String ogName =  originalLocation.getResourcePath();
                boolean seamless = originalLocation.getResourcePath().contains("smooth");

                if (ogName.contains("stone_brick_slab") || ogName.contains("nether_brick_slab")
                        || ogName.contains("quartz_slab") || ogName.contains("smooth_stone")
                        || ogName.contains("smooth_sandstone") || ogName.contains("smooth_quartz")) {
                    block = "stone_slab";
                }
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), block);
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockDoubleStoneSlab.VARIANT, stoneType);
                state.withProperty(BlockDoubleStoneSlab.SEAMLESS, seamless);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "smooth_red_sandstone":
            case "red_sandstone_slab":{
                BlockStoneSlabNew.EnumType sandType = BlockStoneSlabNew.EnumType.RED_SANDSTONE;
                String block = "double_stone_slab2";
                if (originalLocation.getResourcePath().contains("smooth")) {
                    block = "stone_slab2";
                }
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), block);
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStoneSlabNew.SEAMLESS, !block.contains("double"));
                state.withProperty(BlockStoneSlabNew.VARIANT, sandType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "bricks": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "brick_block");
                break;
            }
            case "spawner": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "mob_spawner");
                break;
            }
            case "cobblestone_stairs": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "stone_stairs");
                break;
            }
            case "acacia_pressure_plate":
            case "jungle_pressure_plate":
            case "dark_oak_pressure_plate":
            case "spruce_pressure_plate":
            case "birch_pressure_plate":
            case "oak_pressure_plate": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "wooden_pressure_plate");
                break;
            }
            case "snow_block": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "snow");
                break;
            }
            case "snow": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "snow_layer");
                break;
            }
            case "acacia_fence_gate":
            case "jungle_fence_gate":
            case "dark_oak_fence_gate":
            case "spruce_fence_gate":
            case "birch_fence_gate":
            case "oak_fence_gate": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "fence_gate");
                break;
            }
            case "acacia_fence":
            case "jungle_fence":
            case "dark_oak_fence":
            case "spruce_fence":
            case "birch_fence":
            case "oak_fence": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "fence");
                break;
            }
            case "carved_pumpkin": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "pumpkin");
                break;
            }
            case "jack_o_lantern": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "lit_pumpkin");
                break;
            }
            case "acacia_trapdoor":
            case "jungle_trapdoor":
            case "dark_oak_trapdoor":
            case "spruce_trapdoor":
            case "birch_trapdoor":
            case "oak_trapdoor": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "trapdoor");
                break;
            }
            case "infested_chiseled_stone_bricks":
            case "infested_cracked_stone_bricks":
            case "infested_mossy_stone_bricks":
            case "infested_stone_bricks":
            case "infested_cobblestone":
            case "infested_stone": {
                BlockSilverfish.EnumType stoneType = BlockSilverfish.EnumType.valueOf(originalLocation.getResourcePath()
                        .replace("infested_", "")
                        .replace("stone_bricks", "stonebrick").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "monster_egg");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockSilverfish.VARIANT, stoneType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "chiseled_stone_bricks":
            case "cracked_stone_bricks":
            case "mossy_stone_bricks":
            case "stone_bricks": {
                String name = originalLocation.getResourcePath().equals("stone_bricks") ? "default" : originalLocation.getResourcePath()
                        .replace("_stone_bricks", "");
                BlockStoneBrick.EnumType stoneType = BlockStoneBrick.EnumType.valueOf(name.toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "stonebrick");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStoneBrick.VARIANT, stoneType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "red_mushroom_block":
            case "brown_mushroom_block":
            case "mushroom_stem":{
                BlockHugeMushroom.EnumType shroomType = originalLocation.getResourcePath().contains("stem") ? BlockHugeMushroom.EnumType.STEM : BlockHugeMushroom.EnumType.CENTER;
                if (originalLocation.getResourcePath().contains("brown_mushroom_block"))
                    unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "brown_mushroom_block");
                else
                    unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "red_mushroom_block");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockHugeMushroom.VARIANT, shroomType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "melon": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "melon_block");
                break;
            }
            case "lily_pad": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "waterlily");
                break;
            }
            case "nether_bricks": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_brick");
                break;
            }
            case "end_stone_bricks": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "end_bricks");
                break;
            }
            case "acacia_button":
            case "jungle_button":
            case "dark_oak_button":
            case "spruce_button":
            case "birch_button":
            case "oak_button": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "wooden_button");
                break;
            }
            case "anvil":
            case "chipped_anvil":
            case "damaged_anvil":{
                int damage = 0;
                String name = originalLocation.getResourcePath();
                switch (name) {
                    case "chipped_anvil":
                        damage = 1; break;
                    case "damaged_anvil":
                        damage = 2; break;
                }
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "anvil");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockAnvil.DAMAGE, damage);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "nether_quartz_ore": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "quartz_ore");
                break;
            }
            case "quartz_pillar":
            case "chiseled_quartz_block":
            case "quartz_block": {
                BlockQuartz.EnumType quartzType = originalLocation.getResourcePath().contains("chiseled") ? BlockQuartz.EnumType.CHISELED : originalLocation.getResourcePath().equals("quartz_block") ? BlockQuartz.EnumType.DEFAULT : BlockQuartz.EnumType.LINES_X;
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "quartz_block");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockQuartz.VARIANT, quartzType);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lime_carpet":
            case "orange_carpet":
            case "magenta_carpet":
            case "light_blue_carpet":
            case "yellow_carpet":
            case "pink_carpet":
            case "gray_carpet":
            case "light_gray_carpet":
            case "purple_carpet":
            case "brown_carpet":
            case "black_carpet":
            case "cyan_carpet":
            case "blue_carpet":
            case "green_carpet":
            case "white_carpet":
            case "red_carpet": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_carpet", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "carpet");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockCarpet.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "slime_block": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "slime");
                break;
            }
            case "lime_stained_glass_pane":
            case "orange_stained_glass_pane":
            case "magenta_stained_glass_pane":
            case "light_blue_stained_glass_pane":
            case "yellow_stained_glass_pane":
            case "pink_stained_glass_pane":
            case "gray_stained_glass_pane":
            case "light_gray_stained_glass_pane":
            case "purple_stained_glass_pane":
            case "brown_stained_glass_pane":
            case "black_stained_glass_pane":
            case "cyan_stained_glass_pane":
            case "blue_stained_glass_pane":
            case "green_stained_glass_pane":
            case "white_stained_glass_pane":
            case "red_stained_glass_pane": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_stained_glass_pane", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "stained_glass_pane");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStainedGlassPane.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "lime_stained_glass":
            case "orange_stained_glass":
            case "magenta_stained_glass":
            case "light_blue_stained_glass":
            case "yellow_stained_glass":
            case "pink_stained_glass":
            case "gray_stained_glass":
            case "light_gray_stained_glass":
            case "purple_stained_glass":
            case "brown_stained_glass":
            case "black_stained_glass":
            case "cyan_stained_glass":
            case "blue_stained_glass":
            case "green_stained_glass":
            case "white_stained_glass":
            case "red_stained_glass": {
                EnumDyeColor color = EnumDyeColor.valueOf(originalLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_stained_glass", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "stained_glass");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStainedGlass.COLOR, color);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "dark_prismarine":
            case "prismarine_bricks":
            case "prismarine": {
                BlockPrismarine.EnumType type = originalLocation.getResourcePath().contains("dark")
                        ? BlockPrismarine.EnumType.DARK : originalLocation.getResourcePath().contains("bricks")
                        ? BlockPrismarine.EnumType.BRICKS : BlockPrismarine.EnumType.ROUGH;
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "prismarine");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockPrismarine.VARIANT, type);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "red_sandstone":
            case "chiseled_red_sandstone":
            case "cut_red_sandstone": {
                BlockRedSandstone.EnumType type = originalLocation.getResourcePath().contains("chiseled")
                        ? BlockRedSandstone.EnumType.CHISELED : originalLocation.getResourcePath().contains("cut")
                        ? BlockRedSandstone.EnumType.SMOOTH : BlockRedSandstone.EnumType.DEFAULT;
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "red_sandstone");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockRedSandstone.TYPE, type);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "magma_block": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "magma");
                break;
            }
            case "red_nether_bricks": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "red_nether_brick");
                break;
            }
            case "light_gray_shulker_box": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "silver_shulker_box");
                break;
            }
            case "light_gray_glazed_terracotta": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "silver_glazed_terracotta");
                break;
            }
            case "acacia_door":
            case "jungle_door":
            case "dark_oak_door":
            case "spruce_door":
            case "birch_door":
            case "oak_door": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "wooden_door");
                break;
            }
            case "potted_poppy":
            case "potted_dandelion":
            case "potted_oak_sapling":
            case "potted_spruce_sapling":
            case "potted_birch_sapling":
            case "potted_jungle_sapling":
            case "potted_red_mushroom":
            case "potted_brown_mushroom":
            case "potted_cactus":
            case "potted_dead_bush":
            case "potted_fern":
            case "potted_acacia_sapling":
            case "potted_dark_oak_sapling":
            case "potted_blue_orchid":
            case "potted_allium":
            case "potted_azure_bluet":
            case "potted_red_tulip":
            case "potted_orange_tulip":
            case "potted_white_tulip":
            case "potted_pink_tulip":
            case "potted_oxeye_daisy":
            case "flower_pot": {
                BlockFlowerPot.EnumFlowerType type;
                if (originalLocation.getResourcePath().contains("potted")) {
                    String name = originalLocation.getResourcePath().replace("potted_", "")
                            .replace("azure_bluet", "houstonia")
                            .replace("red_mushroom", "mushroom_red")
                            .replace("red_mushroom", "mushroom_brown");
                    type = BlockFlowerPot.EnumFlowerType.valueOf(name.toUpperCase(Locale.US));
                }
                else
                    type = BlockFlowerPot.EnumFlowerType.EMPTY;

                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "flower_pot");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockFlowerPot.CONTENTS, type);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "wither_skeleton_skull":
            case "skeleton_skull":
            case "zombie_head":
            case "player_head":
            case "creeper_head":
            case "dragon_head": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "skull");
                break;
            }
            case "lime_banner":
            case "orange_banner":
            case "magenta_banner":
            case "light_blue_banner":
            case "yellow_banner":
            case "pink_banner":
            case "gray_banner":
            case "light_gray_banner":
            case "purple_banner":
            case "brown_banner":
            case "black_banner":
            case "cyan_banner":
            case "blue_banner":
            case "green_banner":
            case "white_banner":
            case "red_banner": {
                // MC used to store banner color in the block entity :pain:
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "banner");
                break;
            }
            case "lime_wall_banner":
            case "orange_wall_banner":
            case "magenta_wall_banner":
            case "light_blue_wall_banner":
            case "yellow_wall_banner":
            case "pink_wall_banner":
            case "gray_wall_banner":
            case "light_gray_wall_banner":
            case "purple_wall_banner":
            case "brown_wall_banner":
            case "black_wall_banner":
            case "cyan_wall_banner":
            case "blue_wall_banner":
            case "green_wall_banner":
            case "white_wall_banner":
            case "red_wall_banner": {
                // MC used to store banner color in the block entity :pain:
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "wall_banner");
                break;
            }
            case "popped_chorus_fruit": {
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "chorus_fruit_popped");
                break;
            }
            case "pumpkin_stem":
            case "attached_pumpkin_stem": {
                EnumFacing facing = originalLocation.getResourcePath().contains("attached") ? EnumFacing.NORTH : EnumFacing.UP;
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "pumpkin_stem");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStem.FACING, facing);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            case "melon_stem":
            case "attached_melon_stem": {
                EnumFacing facing = originalLocation.getResourcePath().contains("attached") ? EnumFacing.NORTH : EnumFacing.UP;
                unflattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "melon_stem");
                IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
                state.withProperty(BlockStem.FACING, facing);
                flattenedResourceLocationMap.put(originalLocation, state);
                return state;
            }
            default:
                unflattenedLocation = originalLocation;
        }
        IBlockState state = RegistryUtils.getRegistryObjFromLocation(Block.class, unflattenedLocation).getDefaultState();
        flattenedResourceLocationMap.put(originalLocation, state);
        return state;
    }


    private static final Map<ItemStack, ResourceLocation> itemStackResourceLocationMap = new HashMap<>();
    public static ResourceLocation theStackFlattinator2000(ItemStack stack) {
        if (itemStackResourceLocationMap.containsKey(stack))
            return itemStackResourceLocationMap.get(stack);

        ResourceLocation originalLocation = RegistryUtils.getResourceLocationForRegistryObj(Item.class, stack.getItem());

        ResourceLocation flattenedLocation;
        switch (originalLocation.getResourcePath()) {
            case "reeds": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sugar_cane");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "sponge": {
                boolean wet = stack.getMetadata() == 1;
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), wet ? "wet_sponge" : "sponge");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "golden_rail": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "powered_rail");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "noteblock": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "note_block");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "web": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "cobweb");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "tallgrass": {
                BlockTallGrass.EnumType type = BlockTallGrass.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName().replace("tall_", ""));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "grass": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "grass_block");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "piston_extension": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "moving_piston");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "flowing_water": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "water");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "flowing_lava": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "lava");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "log": {
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.byMetadata((stack.getMetadata() & 3) % 4);
                BlockLog.EnumAxis axis;
                switch(stack.getMetadata() & 12) {
                    case 0:
                        axis = BlockLog.EnumAxis.Y;
                        break;
                    case 4:
                        axis = BlockLog.EnumAxis.X;
                        break;
                    case 8:
                        axis = BlockLog.EnumAxis.Z;
                        break;
                    default:
                        axis = BlockLog.EnumAxis.NONE;
                }
                String suffix = "log";
                if (axis == BlockLog.EnumAxis.NONE) {
                    suffix = "wood";
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), logType.getName() + "_" + suffix);
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "log2": {
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.byMetadata((stack.getMetadata() & 3) + 4);
                BlockLog.EnumAxis axis;
                switch(stack.getMetadata() & 12) {
                    case 0:
                        axis = BlockLog.EnumAxis.Y;
                        break;
                    case 4:
                        axis = BlockLog.EnumAxis.X;
                        break;
                    case 8:
                        axis = BlockLog.EnumAxis.Z;
                        break;
                    default:
                        axis = BlockLog.EnumAxis.NONE;
                }
                String suffix = "log";
                if (axis == BlockLog.EnumAxis.NONE) {
                    suffix = "wood";
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), logType.getName() + "_" + suffix);
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "leaves": {
                BlockPlanks.EnumType leafType = BlockPlanks.EnumType.byMetadata((stack.getMetadata() & 3) % 4);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), leafType.getName() + "_leaves");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "leaves2": {
                BlockPlanks.EnumType leafType = BlockPlanks.EnumType.byMetadata((stack.getMetadata() & 3) + 4);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), leafType.getName() + "_leaves");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "planks": {
                BlockPlanks.EnumType plankType = BlockPlanks.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), plankType.getName() + "_planks");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "stone": {
                BlockStone.EnumType stoneType = BlockStone.EnumType.byMetadata(stack.getMetadata());
                String stoneName = stoneType.getName();
                if (stoneName.contains("smooth"))
                    stoneName = "polished_" + stoneName.replace("smooth_", "");
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), stoneName);
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "dirt": {
                BlockDirt.DirtType dirtType = BlockDirt.DirtType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), dirtType.getName());
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "sapling": {
                BlockPlanks.EnumType saplingType = BlockPlanks.EnumType.byMetadata(stack.getMetadata() & 7);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), saplingType.getName() + "_sapling");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "bed": {
                EnumDyeColor bedColor = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(),
                        bedColor.getName()
                                .replace("silver", "light_gray") + "_bed");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "concrete_powder": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_concrete_powder");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "concrete": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_concrete");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "wool": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_wool");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "stained_hardened_clay": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_terracotta");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "hardened_clay": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "terracotta");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "double_plant": {
                BlockDoublePlant.EnumPlantType plantType = BlockDoublePlant.EnumPlantType.byMetadata(stack.getMetadata() & 7);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), plantType.getName()
                        .replace("paeonia", "peony")
                        .replace("double_rose", "rose_bush")
                        .replace("double_fern", "large_fern")
                        .replace("double_grass", "tall_grass")
                        .replace("syringa", "lilac")
                );
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "deadbush": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "dead_bush");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "portal": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_portal");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "sand": {
                BlockSand.EnumType color = BlockSand.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName());
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "sandstone": {
                BlockSandStone.EnumType type = BlockSandStone.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName()
                        .replace("smooth", "cut"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "yellow_flower": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "dandelion");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "red_flower": {
                BlockFlower.EnumFlowerType type = BlockFlower.EnumFlowerType.getType(BlockFlower.EnumFlowerColor.RED, stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName()
                        .replace("houstonia", "azure_bluet"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "double_wooden_slab":
            case "wooden_slab": {
                BlockPlanks.EnumType plankType = BlockPlanks.EnumType.byMetadata(stack.getMetadata() & 7);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), plankType.getName() + "_slab");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "double_stone_slab":
            case "stone_slab": {
                BlockStoneSlab.EnumType stoneType = BlockStoneSlab.EnumType.byMetadata(stack.getMetadata() & 7);
                boolean seamless = (stack.getMetadata() & 8) != 0;
                String blockName = stoneType.getName();
                if (seamless) {
                    blockName = "smooth_" + blockName;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(),  blockName + "_slab");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "double_stone_slab2":
            case "stone_slab2": {
                BlockStoneSlabNew.EnumType sandType = BlockStoneSlabNew.EnumType.byMetadata(stack.getMetadata() & 7);
                boolean seamless = (stack.getMetadata() & 8) != 0;
                String blockName = sandType.getName();
                if (seamless) {
                    blockName = "smooth_" + blockName;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), blockName + "_slab");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "double_purpur_slab":
            case "purpur_slab": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "purpur_slab");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "brick_block": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "bricks");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "mob_spawner": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "spawner");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_furnace":
            case "furnace": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "furnace");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "stone_stairs": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "cobblestone_stairs");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "wooden_pressure_plate": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_pressure_plate");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_redstone_ore":
            case "redstone_ore": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "redstone_ore");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "redstone_torch":
            case "unlit_redstone_torch": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "redstone_torch");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "snow_layer": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "snow");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "snow": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "snow_block");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "fence": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_fence");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "pumpkin": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "carved_pumpkin");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_pumpkin": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "jack_o_lantern");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "trapdoor": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_trapdoor");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "monster_egg": {
                BlockSilverfish.EnumType stoneType = BlockSilverfish.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "infested_" + stoneType.getName().replace("brick", "bricks"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "stonebrick": {
                BlockStoneBrick.EnumType brickType = BlockStoneBrick.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), brickType.getName()
                        .replace("stonebrick", "stone_bricks"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "red_mushroom_block":
            case "brown_mushroom_block": {
                BlockHugeMushroom.EnumType shroomType = BlockHugeMushroom.EnumType.byMetadata(stack.getMetadata());
                if (shroomType == BlockHugeMushroom.EnumType.ALL_STEM || shroomType == BlockHugeMushroom.EnumType.STEM)
                    flattenedLocation= new ResourceLocation(originalLocation.getResourceDomain(), "mushroom_stem");
                else
                    flattenedLocation= originalLocation;

                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "melon_block": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "melon");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "fence_gate": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_fence_gate");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "waterlily": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "lily_pad");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "nether_brick": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_bricks");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "end_bricks": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "end_stone_bricks");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "lit_redstone_lamp":
            case "redstone_lamp": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "redstone_lamp");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "wooden_button": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_button");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "anvil": {
                int damage = (stack.getMetadata() & 15) >> 2;
                String name;
                switch (damage) {
                    case 1:
                        name = "chipped_anvil"; break;
                    case 2:
                        name = "damaged_anvil"; break;
                    default:
                        name = "anvil"; break;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), name);
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "daylight_detector":
            case "daylight_detector_inverted": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "daylight_detector");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "quartz_ore": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_quartz_ore");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "quartz_block": {
                BlockQuartz.EnumType quartzType = BlockQuartz.EnumType.byMetadata(stack.getMetadata());
                String name;
                switch (quartzType) {
                    case CHISELED:
                        name = "chiseled_quartz_block"; break;
                    case DEFAULT:
                        name = "quartz_block"; break;
                    default:
                        name = "quartz_pillar"; break;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), name);
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "carpet": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_carpet");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "slime": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "slime_block");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "stained_glass_pane": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_stained_glass_pane");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "stained_glass": {
                EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_stained_glass");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "prismarine": {
                BlockPrismarine.EnumType type = BlockPrismarine.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName());
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "red_sandstone": {
                BlockRedSandstone.EnumType type = BlockRedSandstone.EnumType.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), type.getName()
                        .replace("smooth", "cut"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "magma": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "magma_block");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "red_nether_brick": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "red_nether_bricks");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "silver_shulker_box": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "light_gray_shulker_box");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "silver_glazed_terracotta": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "light_gray_glazed_terracotta");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "wooden_door": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_door");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "powered_repeater":
            case "repeater":
            case "unpowered_repeater": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "repeater");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "powered_comparator":
            case "comparator":
            case "unpowered_comparator": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "comparator");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "flower_pot": {
                itemStackResourceLocationMap.put(stack, originalLocation);
                return originalLocation;
            }
            case "skull": {
                int type = stack.getMetadata();
                String name;
                switch (type) {
                    case 0:
                        name = "skeleton_skull"; break;
                    case 1:
                        name = "wither_skeleton_skull"; break;
                    case 2:
                        name = "zombie_head"; break;
                    case 3:
                        name = "player_head"; break;
                    case 4:
                        name = "creeper_head"; break;
                    default:
                        name = "dragon_head"; break;
                }
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), name);
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "standing_banner":
            case "banner": {
                EnumDyeColor color = TileEntityBanner.getColor(stack);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_banner");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "wall_banner": {
                EnumDyeColor color = TileEntityBanner.getColor(stack);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), color.getName()
                        .replace("silver", "light_gray") + "_wall_banner");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "chorus_fruit_popped": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "popped_chorus_fruit");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "fireworks": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "firework_rocket");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "firework_charge": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "firework_star");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "netherbrick": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "nether_brick");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "speckled_melon": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "glistering_melon_slice");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "melon": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "melon_slice");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "record_11":
            case "record_stal":
            case "record_ward":
            case "record_strad":
            case "record_chirp":
            case "record_far":
            case "record_mall":
            case "record_mellohi":
            case "record_wait":
            case "record_blocks":
            case "record_cat":
            case "record_13": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), originalLocation.getResourcePath().replace("record", "music_disc"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "cooked_fish": {
                ItemFishFood.FishType fishType = ItemFishFood.FishType.byItemStack(stack);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "cooked_" + fishType.getUnlocalizedName().replace("clownfish", "tropical_fish"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "fish": {
                ItemFishFood.FishType fishType = ItemFishFood.FishType.byItemStack(stack);
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), fishType.getUnlocalizedName().replace("clownfish", "tropical_fish"));
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "boat": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "oak_boat");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "standing_sign": {
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), "sign");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "golden_apple": {
                boolean enchanted = stack.getMetadata() == 1;
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), enchanted ? "enchanted_golden_apple" : "golden_apple");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "coal": {
                boolean charred = stack.getMetadata() == 1;
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(), charred ? "charcoal" : "coal");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            case "dye": {
                EnumDyeColor bedColor = EnumDyeColor.byMetadata(stack.getMetadata());
                flattenedLocation = new ResourceLocation(originalLocation.getResourceDomain(),
                        bedColor.getName()
                                .replace("silver", "light_gray") + "_dye");
                itemStackResourceLocationMap.put(stack, flattenedLocation);
                return flattenedLocation;
            }
            default:
                itemStackResourceLocationMap.put(stack, originalLocation);
                return originalLocation;
        }
    }
// TODO: write a flattener for biome ids, entity ids, sound ids, particle ids and blockstate converter

    private static final Map<ResourceLocation, ItemStack> newNameToOldItemStackMap = new HashMap<>();
    public static ItemStack theStackUnflattinator2000(ResourceLocation newLocation) {
        if (newNameToOldItemStackMap.containsKey(newLocation))
            return newNameToOldItemStackMap.get(newLocation);
        
        ResourceLocation unflattenedLocation;
        switch (newLocation.getResourcePath()) {
            case "sugar_cane": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "reeds");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "wet_sponge":
            case "sponge": {
                boolean wet = newLocation.getResourcePath().contains("wet");
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), wet ? "wet_sponge" : "sponge");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(wet ? 1 : 0);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "powered_rail": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "golden_rail");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "note_block": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "noteblock");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "cobweb": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "web");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "large_fern": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "double_plant");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.FERN.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "sunflower": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "double_plant");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.SUNFLOWER.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lilac": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "double_plant");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.SYRINGA.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "tall_grass": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "double_plant");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.GRASS.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "rose_bush": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "double_plant");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.ROSE.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "peony": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "double_plant");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.PAEONIA.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "grass_block": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "grass");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.PAEONIA.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "grass": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "tallgrass");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.PAEONIA.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "moving_piston": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "piston_extension");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(BlockDoublePlant.EnumPlantType.PAEONIA.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_log":
            case "jungle_log":
            case "dark_oak_log":
            case "spruce_log":
            case "birch_log":
            case "oak_log":
            case "acacia_wood":
            case "jungle_wood":
            case "dark_oak_wood":
            case "spruce_wood":
            case "birch_wood":
            case "oak_wood":{
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("_log", "").replace("_wood", "").toUpperCase(Locale.US));
                BlockLog.EnumAxis type = newLocation.getResourcePath().contains("log") ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.NONE;
                String block = "log";
                if (logType.getMetadata() >= 4) {
                    block = "log2";
                }
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), block);
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                int meta;
                if (block.equals("log2")) {
                    meta = logType.getMetadata()-4;
                } else {
                    meta = logType.getMetadata();
                }
                if (type == BlockLog.EnumAxis.NONE)
                    meta |= 12;

                stack.setItemDamage(meta);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_leaves":
            case "jungle_leaves":
            case "dark_oak_leaves":
            case "spruce_leaves":
            case "birch_leaves":
            case "oak_leaves": {
                BlockPlanks.EnumType leavesType = BlockPlanks.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("_leaves", "").toUpperCase(Locale.US));
                String block = "leaves";
                if (leavesType.getMetadata() >= 4) {
                    block = "leaves2";
                }
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), block);
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                int meta;
                if (block.equals("leaves2")) {
                    meta = leavesType.getMetadata()-4;
                } else {
                    meta = leavesType.getMetadata();
                }

                stack.setItemDamage(meta);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_planks":
            case "jungle_planks":
            case "dark_oak_planks":
            case "spruce_planks":
            case "birch_planks":
            case "oak_planks": {
                BlockPlanks.EnumType plankType = BlockPlanks.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("_planks", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "planks");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                int meta = plankType.getMetadata();
                stack.setItemDamage(meta);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "polished_diorite":
            case "polished_granite":
            case "polished_andesite":
            case "andesite":
            case "diorite":
            case "granite":
            case "stone": {
                String suffix = newLocation.getResourcePath().contains("polished") ? "_smooth" : "";
                BlockStone.EnumType stoneType = BlockStone.EnumType.valueOf((newLocation.getResourcePath()
                        .replace("polished_", "") + suffix).toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "stone");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(stoneType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "dirt":
            case "coarse_dirt":
            case "podzol":{
                BlockDirt.DirtType dirtType = BlockDirt.DirtType.valueOf(newLocation.getResourcePath().toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "dirt");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(dirtType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_sapling":
            case "jungle_sapling":
            case "dark_oak_sapling":
            case "spruce_sapling":
            case "birch_sapling":
            case "oak_sapling": {
                BlockPlanks.EnumType saplingType = BlockPlanks.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("_sapling", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "sapling");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                int meta = saplingType.getMetadata();
                stack.setItemDamage(meta);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_bed":
            case "orange_bed":
            case "magenta_bed":
            case "light_blue_bed":
            case "yellow_bed":
            case "pink_bed":
            case "gray_bed":
            case "light_gray_bed":
            case "purple_bed":
            case "brown_bed":
            case "black_bed":
            case "cyan_bed":
            case "blue_bed":
            case "green_bed":
            case "white_bed":
            case "red_bed": {
                EnumDyeColor bedColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_bed", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("bed");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(bedColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_concrete_powder":
            case "orange_concrete_powder":
            case "magenta_concrete_powder":
            case "light_blue_concrete_powder":
            case "yellow_concrete_powder":
            case "pink_concrete_powder":
            case "gray_concrete_powder":
            case "light_gray_concrete_powder":
            case "purple_concrete_powder":
            case "brown_concrete_powder":
            case "black_concrete_powder":
            case "cyan_concrete_powder":
            case "blue_concrete_powder":
            case "green_concrete_powder":
            case "white_concrete_powder":
            case "red_concrete_powder": {
                EnumDyeColor powderColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_concrete_powder", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("concrete_powder");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(powderColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_concrete":
            case "orange_concrete":
            case "magenta_concrete":
            case "light_blue_concrete":
            case "yellow_concrete":
            case "pink_concrete":
            case "gray_concrete":
            case "light_gray_concrete":
            case "purple_concrete":
            case "brown_concrete":
            case "black_concrete":
            case "cyan_concrete":
            case "blue_concrete":
            case "green_concrete":
            case "white_concrete":
            case "red_concrete": {
                EnumDyeColor concreteColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_concrete", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("concrete");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(concreteColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_wool":
            case "orange_wool":
            case "magenta_wool":
            case "light_blue_wool":
            case "yellow_wool":
            case "pink_wool":
            case "gray_wool":
            case "light_gray_wool":
            case "purple_wool":
            case "brown_wool":
            case "black_wool":
            case "cyan_wool":
            case "blue_wool":
            case "green_wool":
            case "white_wool":
            case "red_wool": {
                EnumDyeColor woolColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_wool", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("wool");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(woolColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_terracotta":
            case "orange_terracotta":
            case "magenta_terracotta":
            case "light_blue_terracotta":
            case "yellow_terracotta":
            case "pink_terracotta":
            case "gray_terracotta":
            case "light_gray_terracotta":
            case "purple_terracotta":
            case "brown_terracotta":
            case "black_terracotta":
            case "cyan_terracotta":
            case "blue_terracotta":
            case "green_terracotta":
            case "white_terracotta":
            case "red_terracotta": {
                EnumDyeColor terracottaColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_terracotta", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("terracotta");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(terracottaColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "dead_bush": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "deadbush");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "nether_portal": {
                newNameToOldItemStackMap.put(newLocation, ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }
            case "red_sand":
            case "sand": {
                BlockSand.EnumType color = BlockSand.EnumType.valueOf(newLocation.getResourcePath().toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "sand");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(color.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "sandstone":
            case "chiseled_sandstone":
            case "cut_sandstone":{
                BlockSandStone.EnumType type = newLocation.getResourcePath().contains("chiseled") ?
                        BlockSandStone.EnumType.CHISELED : newLocation.getResourcePath().contains("cut") ?
                        BlockSandStone.EnumType.SMOOTH : BlockSandStone.EnumType.DEFAULT;

                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "sandstone");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(type.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "dandelion": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "yellow_flower");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "oxeye_daisy":
            case "pink_tulip":
            case "white_tulip":
            case "orange_tulip":
            case "red_tulip":
            case "azure_bluet":
            case "allium":
            case "blue_orchid":
            case "poppy": {
                BlockFlower.EnumFlowerType type = BlockFlower.EnumFlowerType.valueOf(newLocation.getResourcePath()
                        .replace("azure_bluet", "houstonia").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "red_flower");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(type.getMeta());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_slab":
            case "jungle_slab":
            case "dark_oak_slab":
            case "spruce_slab":
            case "birch_slab":
            case "oak_slab":{
                BlockPlanks.EnumType logType = BlockPlanks.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("_slab", "").toUpperCase(Locale.US));
                String block = "double_wooden_slab";
                if (logType.getMetadata() >= 4) {
                    block = "wooden_slab";
                }
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), block);
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                int meta;
                if (block.equals("double_wooden_slab")) {
                    meta = logType.getMetadata()-4;
                } else {
                    meta = logType.getMetadata();
                }
                stack.setItemDamage(meta);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "stone_slab":
            case "sandstone_slab":
            case "petrified_oak_slab":
            case "cobblestone_slab":
            case "brick_slab":
            case "stone_brick_slab":
            case "nether_brick_slab":
            case "quartz_slab":
            case "smooth_stone":
            case "smooth_sandstone":
            case "smooth_quartz":{
                BlockDoubleStoneSlab.EnumType stoneType = BlockDoubleStoneSlab.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("petrified_oak", "wood")
                        .replace("sandstone", "sand")
                        .replace("stone_brick", "smoothbrick")
                        .replace("nether_brick", "netherbrick")
                        .replace("smooth_", "")
                        .replace("_slab", "").toUpperCase(Locale.US));
                String block = "double_stone_slab";
                String ogName =  newLocation.getResourcePath();

                if (ogName.contains("stone_brick_slab") || ogName.contains("nether_brick_slab")
                        || ogName.contains("quartz_slab") || ogName.contains("smooth_stone")
                        || ogName.contains("smooth_sandstone") || ogName.contains("smooth_quartz")) {
                    block = "stone_slab";
                }
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), block);
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(stoneType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "bricks": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "brick_block");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "spawner": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "mob_spawner");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "cobblestone_stairs": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "stone_stairs");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_pressure_plate":
            case "jungle_pressure_plate":
            case "dark_oak_pressure_plate":
            case "spruce_pressure_plate":
            case "birch_pressure_plate":
            case "oak_pressure_plate": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "wooden_pressure_plate");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "snow_block": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "snow");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "snow": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "snow_layer");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_fence_gate":
            case "jungle_fence_gate":
            case "dark_oak_fence_gate":
            case "spruce_fence_gate":
            case "birch_fence_gate":
            case "oak_fence_gate": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "fence_gate");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_fence":
            case "jungle_fence":
            case "dark_oak_fence":
            case "spruce_fence":
            case "birch_fence":
            case "oak_fence": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "fence");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "carved_pumpkin": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "pumpkin");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "jack_o_lantern": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "lit_pumpkin");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_trapdoor":
            case "jungle_trapdoor":
            case "dark_oak_trapdoor":
            case "spruce_trapdoor":
            case "birch_trapdoor":
            case "oak_trapdoor": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "trapdoor");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "infested_chiseled_stone_bricks":
            case "infested_cracked_stone_bricks":
            case "infested_mossy_stone_bricks":
            case "infested_stone_bricks":
            case "infested_cobblestone":
            case "infested_stone": {
                BlockSilverfish.EnumType stoneType = BlockSilverfish.EnumType.valueOf(newLocation.getResourcePath()
                        .replace("infested_", "")
                        .replace("stone_bricks", "stonebrick").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "monster_egg");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(stoneType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "chiseled_stone_bricks":
            case "cracked_stone_bricks":
            case "mossy_stone_bricks":
            case "stone_bricks": {
                String name = newLocation.getResourcePath().equals("stone_bricks") ? "default" : newLocation.getResourcePath()
                        .replace("_stone_bricks", "");
                BlockStoneBrick.EnumType stoneType = BlockStoneBrick.EnumType.valueOf(name.toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "stonebrick");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(stoneType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "red_mushroom_block":
            case "brown_mushroom_block":
            case "mushroom_stem":{
                BlockHugeMushroom.EnumType shroomType = newLocation.getResourcePath().contains("stem") ? BlockHugeMushroom.EnumType.STEM : BlockHugeMushroom.EnumType.CENTER;
                if (newLocation.getResourcePath().contains("brown_mushroom_block"))
                    unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "brown_mushroom_block");
                else
                    unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "red_mushroom_block");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(shroomType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "glistering_melon_slice": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "speckled_melon");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "melon_slice": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "melon");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "melon": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "melon_block");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lily_pad": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "waterlily");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "nether_bricks": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "nether_bricks");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "end_stone_bricks": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "end_bricks");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_button":
            case "jungle_button":
            case "dark_oak_button":
            case "spruce_button":
            case "birch_button":
            case "oak_button": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "wooden_button");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "anvil":
            case "chipped_anvil":
            case "damaged_anvil":{
                int damage = 0;
                String name = newLocation.getResourcePath();
                switch (name) {
                    case "chipped_anvil":
                        damage = 1; break;
                    case "damaged_anvil":
                        damage = 2; break;
                }
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "anvil");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(damage);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "nether_quartz_ore": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "quartz_ore");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "quartz_pillar":
            case "chiseled_quartz_block":
            case "quartz_block": {
                BlockQuartz.EnumType quartzType = newLocation.getResourcePath().contains("chiseled") ? BlockQuartz.EnumType.CHISELED : 
                        newLocation.getResourcePath().equals("quartz_block") ? BlockQuartz.EnumType.DEFAULT : BlockQuartz.EnumType.LINES_X;
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "quartz_block");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(quartzType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_carpet":
            case "orange_carpet":
            case "magenta_carpet":
            case "light_blue_carpet":
            case "yellow_carpet":
            case "pink_carpet":
            case "gray_carpet":
            case "light_gray_carpet":
            case "purple_carpet":
            case "brown_carpet":
            case "black_carpet":
            case "cyan_carpet":
            case "blue_carpet":
            case "green_carpet":
            case "white_carpet":
            case "red_carpet": {
                EnumDyeColor carpetColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_carpet", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("carpet");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(carpetColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "slime_block": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "slime");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_stained_glass_pane":
            case "orange_stained_glass_pane":
            case "magenta_stained_glass_pane":
            case "light_blue_stained_glass_pane":
            case "yellow_stained_glass_pane":
            case "pink_stained_glass_pane":
            case "gray_stained_glass_pane":
            case "light_gray_stained_glass_pane":
            case "purple_stained_glass_pane":
            case "brown_stained_glass_pane":
            case "black_stained_glass_pane":
            case "cyan_stained_glass_pane":
            case "blue_stained_glass_pane":
            case "green_stained_glass_pane":
            case "white_stained_glass_pane":
            case "red_stained_glass_pane": {
                EnumDyeColor stained_glass_paneColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_stained_glass_pane", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("stained_glass_pane");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(stained_glass_paneColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_stained_glass":
            case "orange_stained_glass":
            case "magenta_stained_glass":
            case "light_blue_stained_glass":
            case "yellow_stained_glass":
            case "pink_stained_glass":
            case "gray_stained_glass":
            case "light_gray_stained_glass":
            case "purple_stained_glass":
            case "brown_stained_glass":
            case "black_stained_glass":
            case "cyan_stained_glass":
            case "blue_stained_glass":
            case "green_stained_glass":
            case "white_stained_glass":
            case "red_stained_glass": {
                EnumDyeColor stained_glassColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_stained_glass", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("stained_glass");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(stained_glassColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "dark_prismarine":
            case "prismarine_bricks":
            case "prismarine": {
                BlockPrismarine.EnumType type = newLocation.getResourcePath().contains("dark")
                        ? BlockPrismarine.EnumType.DARK : newLocation.getResourcePath().contains("bricks")
                        ? BlockPrismarine.EnumType.BRICKS : BlockPrismarine.EnumType.ROUGH;
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "prismarine");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(type.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "red_sandstone":
            case "chiseled_red_sandstone":
            case "cut_red_sandstone": {
                BlockRedSandstone.EnumType type = newLocation.getResourcePath().contains("chiseled")
                        ? BlockRedSandstone.EnumType.CHISELED : newLocation.getResourcePath().contains("cut")
                        ? BlockRedSandstone.EnumType.SMOOTH : BlockRedSandstone.EnumType.DEFAULT;
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "red_sandstone");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(type.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "magma_block": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "magma");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "red_nether_bricks": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "red_nether_brick");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "light_gray_shulker_box": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "silver_shulker_box");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "light_gray_glazed_terracotta": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "silver_glazed_terracotta");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_door":
            case "jungle_door":
            case "dark_oak_door":
            case "spruce_door":
            case "birch_door":
            case "oak_door": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "wooden_door");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "wither_skeleton_skull":
            case "skeleton_skull":
            case "zombie_head":
            case "player_head":
            case "creeper_head":
            case "dragon_head": {
                int type = 0;
                switch (newLocation.getResourcePath()) {
                    case "wither_skeleton_skull":
                        type = 1; break;
                    case "zombie_head":
                        type = 2; break;
                    case "player_head":
                        type = 3; break;
                    case "creeper_head":
                        type = 4; break;
                    case "dragon_head":
                        type = 5; break;
                }
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "skull");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(type);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_banner":
            case "orange_banner":
            case "magenta_banner":
            case "light_blue_banner":
            case "yellow_banner":
            case "pink_banner":
            case "gray_banner":
            case "light_gray_banner":
            case "purple_banner":
            case "brown_banner":
            case "black_banner":
            case "cyan_banner":
            case "blue_banner":
            case "green_banner":
            case "white_banner":
            case "red_banner":  {
                EnumDyeColor bannerColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_banner", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("banner");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(bannerColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "popped_chorus_fruit": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "chorus_fruit_popped");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "firework_rocket": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "fireworks");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "firework_star": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "firework_charge");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "nether_brick": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "netherbrick");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "music_disc_11":
            case "music_disc_stal":
            case "music_disc_ward":
            case "music_disc_strad":
            case "music_disc_chirp":
            case "music_disc_far":
            case "music_disc_mall":
            case "music_disc_mellohi":
            case "music_disc_wait":
            case "music_disc_blocks":
            case "music_disc_cat":
            case "music_disc_13": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), newLocation.getResourcePath().replace("music_disc", "record"));
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "cooked_salmon":
            case "cooked_cod": {
                ItemFishFood.FishType fishType = ItemFishFood.FishType.valueOf(newLocation.getResourcePath().replace("cooked_", "").replace("tropical_", "clown").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "cooked_fish");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(fishType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "pufferfish":
            case "tropical_fish":
            case "salmon":
            case "cod": {
                ItemFishFood.FishType fishType = ItemFishFood.FishType.valueOf(newLocation.getResourcePath().replace("tropical_", "clown").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "fish");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(fishType.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "acacia_boat":
            case "jungle_boat":
            case "dark_oak_boat":
            case "spruce_boat":
            case "birch_boat":
            case "oak_boat": {
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "boat");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "enchanted_golden_apple":
            case "golden_apple": {
                boolean enchanted = newLocation.getResourcePath().contains("enchanted");
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "golden_apple");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(enchanted ? 1 : 0);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "coal":
            case "charcoal": {
                boolean charred = newLocation.getResourcePath().contains("char");
                unflattenedLocation = new ResourceLocation(newLocation.getResourceDomain(), "coal");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(charred ? 1 : 0);
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            case "lime_dye":
            case "orange_dye":
            case "magenta_dye":
            case "light_blue_dye":
            case "yellow_dye":
            case "pink_dye":
            case "gray_dye":
            case "light_gray_dye":
            case "purple_dye":
            case "brown_dye":
            case "black_dye":
            case "cyan_dye":
            case "blue_dye":
            case "green_dye":
            case "white_dye":
            case "red_dye":  {
                EnumDyeColor dyeColor = EnumDyeColor.valueOf(newLocation.getResourcePath()
                        .replace("light_gray", "silver")
                        .replace("_dye", "").toUpperCase(Locale.US));
                unflattenedLocation = new ResourceLocation("dye");
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                stack.setItemDamage(dyeColor.getMetadata());
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
            }
            default:
                unflattenedLocation = newLocation;
                ItemStack stack = RegistryUtils.getRegistryObjFromLocation(Item.class, unflattenedLocation).getDefaultInstance();
                newNameToOldItemStackMap.put(newLocation, stack);
                return stack;
        }
    }
}
