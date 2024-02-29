package org.figuramc.figura.lua.api.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.util.EnumHandSide;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.mixin.EntityPotionAccessor;
import org.figuramc.figura.mixin.LivingEntityAccessor;
import org.figuramc.figura.utils.MathUtils;

@LuaWhitelist
@LuaTypeDoc(
        name = "LivingEntityAPI",
        value = "living_entity"
)
public class LivingEntityAPI<T extends EntityLivingBase> extends EntityAPI<T> {

    public LivingEntityAPI(T entity) {
        super(entity);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Float.class,
                            argumentNames = "delta"
                    )
            },
            value = "living_entity.get_body_yaw"
    )
    public double getBodyYaw(Float delta) {
        checkEntity();
        if (delta == null) delta = 1f;
        return MathUtils.lerp(delta, entity.prevRenderYawOffset, entity.renderYawOffset);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "offhand"
                    )
            },
            value = "living_entity.get_held_item"
    )
    public ItemStackAPI getHeldItem(boolean offhand) {
        checkEntity();
        return ItemStackAPI.verify(offhand ? entity.getHeldItemOffhand() : entity.getHeldItemMainhand());
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_active_item")
    public ItemStackAPI getActiveItem() {
        checkEntity();
        return ItemStackAPI.verify(entity.getActiveItemStack());
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_active_item_time")
    public int getActiveItemTime() {
        checkEntity();
        return entity.getItemInUseMaxCount();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_health")
    public float getHealth() {
        checkEntity();
        return entity.getHealth();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_max_health")
    public float getMaxHealth() {
        checkEntity();
        return entity.getMaxHealth();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_armor")
    public float getArmor() {
        checkEntity();
        return entity.getTotalArmorValue();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_death_time")
    public float getDeathTime() {
        checkEntity();
        return entity.deathTime;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_arrow_count")
    public int getArrowCount() {
        checkEntity();
        return entity.getArrowCountInEntity();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_stinger_count")
    public int getStingerCount() {
        checkEntity();
        return 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_left_handed")
    public boolean isLeftHanded() {
        checkEntity();
        return entity.getPrimaryHand() == EnumHandSide.LEFT;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_using_item")
    public boolean isUsingItem() {
        checkEntity();
        return entity.isHandActive();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_active_hand")
    public String getActiveHand() {
        checkEntity();
        return entity.getActiveHand().name();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_climbing")
    public boolean isClimbing() {
        checkEntity();
        return entity.isOnLadder();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_swing_time")
    public int getSwingTime() {
      checkEntity();
      return entity.swingProgressInt;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_swinging_arm")
    public boolean isSwingingArm() {
      checkEntity();
      return entity.isSwingInProgress;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_swing_arm")
    public String getSwingArm() {
      checkEntity();
      return entity.isSwingInProgress ? entity.swingingHand.name() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_swing_duration")
    public int getSwingDuration() {
      checkEntity();
      return ((LivingEntityAccessor) entity).getSwingDuration();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_absorption_amount")
    public float getAbsorptionAmount() {
        checkEntity();
        return entity.getAbsorptionAmount();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_sensitive_to_water")
    public boolean isSensitiveToWater() {
        checkEntity();
        return EntityPotionAccessor.invokeIsWaterSensitiveEntity(entity);
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.get_entity_category")
    public String getEntityCategory() {
        checkEntity();

        EnumCreatureAttribute mobType = entity.getCreatureAttribute(); // why it is not an enum
        if (mobType == EnumCreatureAttribute.ARTHROPOD)
            return "ARTHROPOD";
        if (mobType == EnumCreatureAttribute.UNDEAD)
            return "UNDEAD";
        if (mobType == EnumCreatureAttribute.ILLAGER)
            return "ILLAGER";

        return "UNDEFINED";
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_gliding")
    public boolean isGliding() {
        checkEntity();
        return entity.isElytraFlying();
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_blocking")
    public boolean isBlocking() {
        checkEntity();
        return entity.isActiveItemStackBlocking();
    }

    // No swimming or riptide in 1.12
    @LuaWhitelist
    @LuaMethodDoc("living_entity.is_visually_swimming")
    public boolean isVisuallySwimming() {
        checkEntity();
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("living_entity.riptide_spinning")
    public boolean riptideSpinning() {
        checkEntity();
        return false;
    }

    @Override
    public String toString() {
        checkEntity();
        return (entity.hasCustomName() ? entity.getCustomNameTag() + " (" + getType() + ")" : getType() ) + " (LivingEntity)";
    }
}
