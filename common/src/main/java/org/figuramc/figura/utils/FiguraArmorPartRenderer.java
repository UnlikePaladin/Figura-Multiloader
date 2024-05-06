package org.figuramc.figura.utils;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import org.figuramc.figura.model.ParentType;

public interface FiguraArmorPartRenderer<T extends ModelBase> {
    void renderArmorPart(T model, EntityLivingBase entity, ItemStack itemStack, EntityEquipmentSlot armorSlot, ItemArmor armorItem, ParentType parentType);
}

