package org.figuramc.figura.lua.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.ChatLineAccessor;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.mixin.LivingEntityAccessor;
import org.figuramc.figura.mixin.gui.GuiChatAccessor;
import org.figuramc.figura.mixin.gui.GuiIngameAccessor;
import org.figuramc.figura.mixin.gui.GuiNewChatAccessor;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.Pair;
import org.figuramc.figura.utils.TextUtils;
import org.luaj.vm2.LuaError;

import java.awt.image.BufferedImage;
import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "HostAPI",
        value = "host"
)
public class HostAPI {

    private final Avatar owner;
    private final boolean isHost;
    private final Minecraft minecraft;

    @LuaWhitelist
    @LuaFieldDoc("host.unlock_cursor")
    public boolean unlockCursor = false;
    public Integer chatColor;

    public HostAPI(Avatar owner) {
        this.owner = owner;
        this.minecraft = Minecraft.getMinecraft();
        this.isHost = owner.isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_host")
    public boolean isHost() {
        return isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "host.is_cursor_unlocked")
    public boolean isCursorUnlocked() {
        return unlockCursor;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "boolean"
            ),
            value = "host.set_unlock_cursor")
    public HostAPI setUnlockCursor(boolean bool) {
        unlockCursor = bool;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "timesData"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class},
                            argumentNames = {"fadeInTime", "stayTime", "fadeOutTime"}
                    )
            },
            aliases = "titleTimes",
            value = "host.set_title_times"
    )
    public HostAPI setTitleTimes(Object x, Double y, Double z) {
        if (!isHost()) return this;
        FiguraVec3 times = LuaUtils.parseVec3("setTitleTimes", x, y, z);
        if (times.x >= 0)
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitleFadeInTime((int) times.x);
        if (times.y >= 0)
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitleStayTime((int) times.y);
        if (times.z >= 0)
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitleFadeOutTime((int) times.y);
        if (((GuiIngameAccessor)this.minecraft.ingameGUI).getTitleTime() > 0)
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitleTime(((GuiIngameAccessor)this.minecraft.ingameGUI).getTitleFadeInTime() + ((GuiIngameAccessor)this.minecraft.ingameGUI).getTitleStayTime() + ((GuiIngameAccessor)this.minecraft.ingameGUI).getTitleFadeOutTime());
        return this;
    }

    @LuaWhitelist
    public HostAPI titleTimes(Object x, Double y, Double z) {
        return setTitleTimes(x, y, z);
    }
//TODO Fix wrong GUI Mixin, subtitle points to the title on 1.16
    @LuaWhitelist
    @LuaMethodDoc("host.clear_title")
    public HostAPI clearTitle() {
        if (isHost()) {
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitle(null);
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setSubtitle(null);
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitleTime(0);
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "title",
            value = "host.set_title"
    )
    public HostAPI setTitle(@LuaNotNil String text) {
        if (isHost())
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setTitle(TextUtils.tryParseJson(text).getUnformattedText());
        return this;
    }

    @LuaWhitelist
    public HostAPI title(@LuaNotNil String text) {
        return setTitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "subtitle",
            value = "host.set_subtitle"
    )
    public HostAPI setSubtitle(@LuaNotNil String text) {
        if (isHost())
            ((GuiIngameAccessor)this.minecraft.ingameGUI).setSubtitle(TextUtils.tryParseJson(text).getUnformattedText());
        return this;
    }

    @LuaWhitelist
    public HostAPI subtitle(@LuaNotNil String text) {
        return setSubtitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "text"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, boolean.class},
                            argumentNames = {"text", "animated"}
                    )
            },
            aliases = "actionbar",
            value = "host.set_actionbar"
    )
    public HostAPI setActionbar(@LuaNotNil String text, boolean animated) {
        if (isHost())
            this.minecraft.ingameGUI.setOverlayMessage(TextUtils.tryParseJson(text), animated);
        return this;
    }

    @LuaWhitelist
    public HostAPI actionbar(@LuaNotNil String text, boolean animated) {
        return setActionbar(text, animated);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.send_chat_message"
    )
    public HostAPI sendChatMessage(@LuaNotNil String message) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        EntityPlayerSP player = this.minecraft.player;
        if (player != null) player.sendChatMessage(message.startsWith("/") ? message.substring(1) : message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "command"
            ),
            value = "host.send_chat_command"
    )
    public HostAPI sendChatCommand(@LuaNotNil String command) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        EntityPlayerSP player = this.minecraft.player;
        if (player != null) player.sendChatMessage(command.startsWith("/") ? command : "/" + command);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.append_chat_history"
    )
    public HostAPI appendChatHistory(@LuaNotNil String message) {
        if (isHost())
            this.minecraft.ingameGUI.getChatGUI().addToSentMessages(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Integer.class,
                    argumentNames = "index"
            ),
            value = "host.get_chat_message"
    )
    public Map<String, Object> getChatMessage(int index) {
        if (!isHost())
            return null;

        index--;
        List<ChatLine> messages = ((GuiNewChatAccessor) this.minecraft.ingameGUI.getChatGUI()).getAllMessages();
        if (index < 0 || index >= messages.size())
            return null;

        ChatLine message = messages.get(index);
        Map<String, Object> map = new HashMap<>();

        map.put("addedTime", message.getUpdatedCounter());
        map.put("message", message.getChatComponent().getUnformattedText());
        map.put("json", message.getChatComponent());
        map.put("backgroundColor", ((ChatLineAccessor) message).figura$getColor());

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "index"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, String.class},
                            argumentNames = {"index", "newMessage"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, String.class, FiguraVec3.class},
                            argumentNames = {"index", "newMessage", "backgroundColor"}
                    )
            },
            value = "host.set_chat_message")
    public HostAPI setChatMessage(int index, String newMessage, FiguraVec3 backgroundColor) {
        if (!isHost()) return this;

        index--;
        List<ChatLine> messages = ((GuiNewChatAccessor) this.minecraft.ingameGUI.getChatGUI()).getAllMessages();
        if (index < 0 || index >= messages.size())
            return this;

        if (newMessage == null)
            messages.remove(index);
        else {
            ChatLine old = messages.get(index);
            ChatLine neww = new ChatLine(this.minecraft.ingameGUI.getUpdateCounter(), TextUtils.tryParseJson(newMessage), old.getChatLineID());
            messages.set(index, neww);
            ((ChatLineAccessor) neww).figura$setColor(backgroundColor != null ? ColorUtils.rgbToInt(backgroundColor) : ((ChatLineAccessor) old).figura$getColor());
        }

        this.minecraft.ingameGUI.getChatGUI().refreshChat();
        return this;
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
            value = "host.swing_arm"
    )
    public HostAPI swingArm(boolean offhand) {
        if (isHost() && this.minecraft.player != null)
            this.minecraft.player.swingArm(offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "slot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "slot"
                    )
            },
            value = "host.get_slot"
    )
    public ItemStackAPI getSlot(@LuaNotNil Object slot) {
        if (!isHost()) return null;
        Entity e = this.owner.luaRuntime.getUser();
        if (e == null || !e.isEntityAlive())
            return ItemStackAPI.verify(ItemStack.EMPTY);
        if (e instanceof IInventory)
            return ItemStackAPI.verify(((IInventory) e).getStackInSlot(LuaUtils.parseSlot(slot, null)));
        else if (e instanceof EntityPlayer)
            ItemStackAPI.verify(((EntityPlayer) e).inventory.getStackInSlot(LuaUtils.parseSlot(slot, null)));

        return ItemStackAPI.verify(ItemStack.EMPTY);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "slot"),
                    @LuaMethodOverload(argumentTypes = Integer.class, argumentNames = "slot"),
                    @LuaMethodOverload(argumentTypes = {String.class, String.class}, argumentNames = {"slot", "item"}),
                    @LuaMethodOverload(argumentTypes = {Integer.class, ItemStackAPI.class}, argumentNames = {"slot", "item"})
            },
            value = "host.set_slot"
    )
    public HostAPI setSlot(@LuaNotNil Object slot, Object item) {
        if (!isHost() || (slot == null && item == null) || this.minecraft.playerController == null || this.minecraft.player == null || !this.minecraft.playerController.getCurrentGameType().isCreative())
            return this;

        InventoryPlayer inventory = this.minecraft.player.inventory;

        int index = LuaUtils.parseSlot(slot, inventory);
        ItemStack stack = LuaUtils.parseItemStack("setSlot", item);

        inventory.setInventorySlotContents(index, stack);
        this.minecraft.playerController.sendSlotPacket(stack, index + 36);

        return this;
    }

    @LuaWhitelist
    public HostAPI setBadge(int index, boolean value, boolean pride) {
        if (!isHost()) return this;
        if (!FiguraMod.debugModeEnabled())
            throw new LuaError("Congrats, you found this debug easter egg!");

        Pair<BitSet, BitSet> badges = AvatarManager.getBadges(owner.owner);
        if (badges == null)
            return this;

        BitSet set = pride ? badges.getFirst() : badges.getSecond();
        set.set(index, value);
        return this;
    }

    @LuaWhitelist
    public HostAPI badge(int index, boolean value, boolean pride) {
        return setBadge(index, value, pride);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_color")
    public Integer getChatColor() {
        return isHost() ? this.chatColor : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "color"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            aliases = "chatColor",
            value = "host.set_chat_color"
    )
    public HostAPI setChatColor(Object x, Double y, Double z) {
        if (isHost()) this.chatColor = x == null ? null : ColorUtils.rgbToInt(LuaUtils.parseVec3("setChatColor", x, y, z));
        return this;
    }

    @LuaWhitelist
    public HostAPI chatColor(Object x, Double y, Double z) {
        return setChatColor(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_text")
    public String getChatText() {
        if (isHost() && this.minecraft.currentScreen instanceof GuiChat) {
            GuiChat chat = (GuiChat) this.minecraft.currentScreen;
            return ((GuiChatAccessor) chat).getInput().getText();
        }

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "chatText",
            value = "host.set_chat_text"
    )
    public HostAPI setChatText(@LuaNotNil String text) {
        if (isHost() && Configs.CHAT_MESSAGES.value && this.minecraft.currentScreen instanceof GuiChat) {
            GuiChat chat = (GuiChat) this.minecraft.currentScreen;
            ((GuiChatAccessor) chat).getInput().setText(text);
        }
        return this;
    }

    @LuaWhitelist
    public HostAPI chatText(@LuaNotNil String text) {
        return setChatText(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen")
    public String getScreen() {
        if (!isHost() || this.minecraft.currentScreen == null)
            return null;
        return this.minecraft.currentScreen.getClass().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen_slot_count")
    public Integer getScreenSlotCount() {
        if (isHost() && this.minecraft.currentScreen instanceof GuiContainer) {
            GuiContainer screen = (GuiContainer) this.minecraft.currentScreen;
            return screen.inventorySlots.inventorySlots.size();
        }
        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(overloads = {
            @LuaMethodOverload(argumentTypes = String.class, argumentNames = "slot"),
            @LuaMethodOverload(argumentTypes = Integer.class, argumentNames = "slot")
    }, value = "host.get_screen_slot")
    public ItemStackAPI getScreenSlot(@LuaNotNil Object slot) {
        if (!isHost() || !(this.minecraft.currentScreen instanceof GuiContainer))
            return null;
        GuiContainer screen = (GuiContainer) this.minecraft.currentScreen;

        List<Slot> slots = screen.inventorySlots.inventorySlots;
        int index = LuaUtils.parseSlot(slot, null);
        if (index < 0 || index >= slots.size())
            return null;
        return ItemStackAPI.verify(slots.get(index).getStack());
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_open")
    public boolean isChatOpen() {
        return isHost() && this.minecraft.currentScreen instanceof GuiChat;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_container_open")
    public boolean isContainerOpen() {
        return isHost() && this.minecraft.currentScreen instanceof GuiContainer;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            value = "host.screenshot")
    public FiguraTexture screenshot(@LuaNotNil String name) {
        if (!isHost())
            return null;

        BufferedImage img = ScreenShotHelper.createScreenshot(this.minecraft.displayWidth, this.minecraft.displayHeight, this.minecraft.getFramebuffer());
        return owner.luaRuntime.texture.register(name, img, true);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_avatar_uploaded")
    public boolean isAvatarUploaded() {
        return isHost() && AvatarManager.localUploaded;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_status_effects")
    public List<Map<String, Object>> getStatusEffects() {
        List<Map<String, Object>> list = new ArrayList<>();

        EntityPlayerSP player = this.minecraft.player;
        if (!isHost() || player == null)
            return list;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", effect.getEffectName());
            map.put("amplifier", effect.getAmplifier());
            map.put("duration", effect.getDuration());
            map.put("visible", effect.doesShowParticles());

            list.add(map);
        }

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_clipboard")
    public String getClipboard() {
        return isHost() ? GuiScreen.getClipboardString() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "clipboard",
            value = "host.set_clipboard")
    public HostAPI setClipboard(@LuaNotNil String text) {
        if (isHost()) GuiScreen.setClipboardString(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI clipboard(@LuaNotNil String text) {
        return setClipboard(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_attack_charge")
    public float getAttackCharge() {
        EntityPlayerSP player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getCooledAttackStrength(0f);
        return 0f;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_jumping")
    public boolean isJumping() {
        EntityPlayerSP player = this.minecraft.player;
        if (isHost() && player != null)
            return ((LivingEntityAccessor) player).isJumping();
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_flying")
    public boolean isFlying() {
        EntityPlayerSP player = this.minecraft.player;
        if (isHost() && player != null)
            return player.capabilities.isFlying;
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_reach_distance")
    public double getReachDistance() {
        return this.minecraft.playerController == null ? 0 : this.minecraft.playerController.getBlockReachDistance();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_air")
    public int getAir() {
        EntityPlayerSP player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAir();
        return 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_pick_block")
    public Object[] getPickBlock() {
        return isHost() ? LuaUtils.parseBlockHitResult(minecraft.objectMouseOver) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_pick_entity")
    public EntityAPI<?> getPickEntity() {
        return isHost() && minecraft.pointedEntity != null ? EntityAPI.wrap(minecraft.pointedEntity) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_verified")
    public boolean isChatVerified() {
        return false;
    }

    public Object __index(String arg) {
        if ("unlockCursor".equals(arg))
            return unlockCursor;
        return null;
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, Object value) {
        if ("unlockCursor".equals(key))
            unlockCursor = (Boolean) value;
        else throw new LuaError("Cannot assign value on key \"" + key + "\"");
    }

    @Override
    public String toString() {
        return "HostAPI";
    }
}
