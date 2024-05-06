package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LayerArmorBase.class)
public interface HumanoidArmorLayerAccessor {
    @Intrinsic
    @Invoker("isLegSlot")
    boolean usesInnerModel(EntityEquipmentSlot armorSlot);

    @Intrinsic
    @Invoker("getArmorResource")
    ResourceLocation invokeGetArmorLocation(ItemArmor item, boolean legs, @Nullable String overlay);
}
