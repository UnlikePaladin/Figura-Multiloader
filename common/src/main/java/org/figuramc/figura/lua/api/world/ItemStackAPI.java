package org.figuramc.figura.lua.api.world;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.NbtToLua;
import org.figuramc.figura.lua.ReadOnlyLuaTable;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.utils.RegistryUtils;
import org.luaj.vm2.LuaTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@LuaWhitelist
@LuaTypeDoc(
        name = "ItemStack",
        value = "itemstack"
)
public class ItemStackAPI {

    public final ItemStack itemStack;

    /**
     * Checks whether the given ItemStack is null, empty. If it is, returns air. If it isn't,
     * returns a new ItemStack for that item.
     * @param itemStack The ItemStack to check if it's a valid stack.
     * @return Null if the stack was invalid, or a wrapper for the stack if it was valid.
     */
    public static ItemStackAPI verify(ItemStack itemStack) {
        if (itemStack == null || itemStack == ItemStack.EMPTY)
            itemStack = Items.AIR.getDefaultInstance();
        return new ItemStackAPI(itemStack);
    }

    @LuaWhitelist
    @LuaFieldDoc("itemstack.id")
    public final String id;
    @LuaWhitelist
    @LuaFieldDoc("itemstack.tag")
    public final LuaTable tag;

    public ItemStackAPI(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.id = RegistryUtils.getResourceLocationForRegistryObj(Item.class, itemStack.getItem()).toString();
        this.tag = new ReadOnlyLuaTable(itemStack.getTagCompound() != null ? NbtToLua.convert(itemStack.getTagCompound()) : new LuaTable());
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_id")
    public String getID() {
        return id;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_tag")
    public LuaTable getTag() {
        return tag;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_count")
    public int getCount() {
        return itemStack.getCount();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_damage")
    public int getDamage() {
        return itemStack.getItemDamage();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_pop_time")
    public int getPopTime() {
        return itemStack.getAnimationsToGo();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.has_glint")
    public boolean hasGlint() {
        return itemStack.hasEffect();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_tags")
    public List<String> getTags() {
        List<String> list = new ArrayList<>();

        Registry<Item> registry = WorldAPI.getCurrentWorld().registryAccess().registryOrThrow(Registry.ITEM_REGISTRY);
        Optional<ResourceKey<Item>> key = registry.getResourceKey(itemStack.getItem());
        if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().getConnection().getTags() == null)
            return list;

        for (ResourceLocation resourceLocation : Minecraft.getMinecraft().getConnection().getTags().getItems().getMatchingTags(itemStack.getItem()))
            list.add(resourceLocation.toString());

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_block_item")
    public boolean isBlockItem() {
        return itemStack.getItem() instanceof ItemBlock;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_food")
    public boolean isFood() {
        return itemStack.getItem() instanceof ItemFood;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_use_action")
    public String getUseAction() {
        return itemStack.getItemUseAction().name();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_name")
    public String getName() {
        return itemStack.getDisplayName();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_max_count")
    public int getMaxCount() {
        return itemStack.getMaxStackSize();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_rarity")
    public String getRarity() {
        return itemStack.getRarity().name();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_enchantable")
    public boolean isEnchantable() {
        return itemStack.isItemEnchantable();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_max_damage")
    public int getMaxDamage() {
        return itemStack.getMaxDamage();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_damageable")
    public boolean isDamageable() {
        return itemStack.isItemStackDamageable();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_stackable")
    public boolean isStackable() {
        return itemStack.isStackable();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_repair_cost")
    public int getRepairCost() {
        return itemStack.getRepairCost();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_use_duration")
    public int getUseDuration() {
        return itemStack.getMaxItemUseDuration();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.to_stack_string")
    public String toStackString() {
        ItemStack stack = itemStack;
        String ret = RegistryUtils.getResourceLocationForRegistryObj(Item.class, stack.getItem()).toString();

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null)
            ret += nbt.toString();

        return ret;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_armor")
    public boolean isArmor() {
        return itemStack.getItem() instanceof ItemArmor;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.is_tool")
    public boolean isTool() {
        return itemStack.getItem() instanceof ItemTool;
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_equipment_slot")
    public String getEquipmentSlot() {
        return EntityMob.getSlotForItemStack(itemStack).name();
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.copy")
    public ItemStackAPI copy() {
        return new ItemStackAPI(itemStack.copy());
    }

    @LuaWhitelist
    @LuaMethodDoc("itemstack.get_blockstate")
    public BlockStateAPI getBlockstate() {
        return itemStack.getItem() instanceof ItemBlock ? new BlockStateAPI(((ItemBlock) itemStack.getItem()).getBlock().getDefaultState(), null) : null;
    }

    @LuaWhitelist
    public boolean __eq(ItemStackAPI other) {
        if (this == other)
            return true;

        ItemStack t = this.itemStack;
        ItemStack o = other.itemStack;
        if (t.getCount() != o.getCount())
            return false;
        if (!(t.getItem() == o.getItem()))
            return false;

        NBTTagCompound tag1 = t.getTagCompound();
        NBTTagCompound tag2 = o.getTagCompound();
        if (tag1 == null && tag2 != null)
            return false;

        return tag1 == null || tag1.equals(tag2);
    }

    @LuaWhitelist
    public Object __index(String arg) {
        if (arg == null) return null;
        switch (arg) {
            case "id":
                return id;
            case "tag":
                return tag;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return id + " x" + getCount() + " (ItemStack)";
    }
}
