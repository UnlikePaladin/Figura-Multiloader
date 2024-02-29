package org.figuramc.figura.lua.api.entity;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.GameType;
import org.figuramc.figura.ducks.FoodStatsAccessor;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.NbtToLua;
import org.figuramc.figura.lua.ReadOnlyLuaTable;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.utils.EntityUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;

import java.util.HashMap;
import java.util.Map;

@LuaWhitelist
@LuaTypeDoc(
        name = "PlayerAPI",
        value = "player"
)
public class PlayerAPI extends LivingEntityAPI<EntityPlayer> {

    private NetworkPlayerInfo playerInfo;

    public PlayerAPI(EntityPlayer entity) {
        super(entity);
    }

    private boolean checkPlayerInfo() {
        if (playerInfo != null)
            return true;

        NetworkPlayerInfo info = EntityUtils.getPlayerInfo(entity.getUniqueID());
        if (info == null)
            return false;

        playerInfo = info;
        return true;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_food")
    public int getFood() {
        checkEntity();
        return entity.getFoodStats().getFoodLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_saturation")
    public float getSaturation() {
        checkEntity();
        return entity.getFoodStats().getSaturationLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_exhaustion")
    public float getExhaustion() {
        checkEntity();
        return ((FoodStatsAccessor)(entity.getFoodStats())).figura$getExhaustionLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_experience_progress")
    public float getExperienceProgress() {
        checkEntity();
        return entity.experience;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_experience_level")
    public int getExperienceLevel() {
        checkEntity();
        return entity.experienceLevel;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_model_type")
    public String getModelType() {
        checkEntity();
        return (checkPlayerInfo() ? playerInfo.getSkinType() : DefaultPlayerSkin.getSkinType(entity.getUniqueID())).toUpperCase();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_gamemode")
    public String getGamemode() {
        checkEntity();
        if (!checkPlayerInfo())
            return null;

        GameType gamemode = playerInfo.getGameType();
        return gamemode == null ? null : gamemode.getName().toUpperCase();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.has_cape")
    public boolean hasCape() {
        checkEntity();
        return checkPlayerInfo() && playerInfo.getLocationCape() != null;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.has_skin")
    public boolean hasSkin() {
        checkEntity();
        return checkPlayerInfo() && playerInfo.hasLocationSkin();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "part"
            ),
            value = "player.is_skin_layer_visible"
    )
    public boolean isSkinLayerVisible(@LuaNotNil String part) {
        checkEntity();
        try {
            if (part.equalsIgnoreCase("left_pants") || part.equalsIgnoreCase("right_pants"))
                part += "_leg";
            return entity.isWearing(EnumPlayerModelParts.valueOf(part.toUpperCase()));
        } catch (Exception ignored) {
            throw new LuaError("Invalid player model part: " + part);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("player.is_fishing")
    public boolean isFishing() {
        checkEntity();
        return entity.fishEntity != null;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_charged_attack_delay")
    public float getChargedAttackDelay() {
        checkEntity();
        return entity.getCooldownPeriod();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "right"
                    )
            },
            value = "player.get_shoulder_entity")
    public LuaTable getShoulderEntity(boolean right) {
        checkEntity();
        return new ReadOnlyLuaTable(NbtToLua.convert(right ? entity.getRightShoulderEntity() : entity.getLeftShoulderEntity()));
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_team_info")
    public Map<String, Object> getTeamInfo() {
        checkEntity();
        if (!checkPlayerInfo())
            return null;

        ScorePlayerTeam team = playerInfo.getPlayerTeam();
        if (team == null)
            return null;

        Map<String, Object> map = new HashMap<>();

        map.put("name", team.getName());
        map.put("display_name", team.getDisplayName());
        map.put("color", team.getColor().name());
        map.put("prefix", team.getPrefix());
        map.put("suffix", team.getSuffix());
        map.put("friendly_fire", team.getAllowFriendlyFire());
        map.put("see_friendly_invisibles", team.getSeeFriendlyInvisiblesEnabled());
        map.put("nametag_visibility", team.getNameTagVisibility().internalName);
        map.put("death_message_visibility", team.getDeathMessageVisibility().internalName);
        map.put("collision_rule", team.getCollisionRule().name);

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {ItemStackAPI.class, Float.class},
                            argumentNames = {"stack", "delta"}
                    ),
            },
            value = "player.get_cooldown_percent"
    )
    public float getCooldownPercent(@LuaNotNil ItemStackAPI stack, Float delta) {
        checkEntity();
        if (delta == null) delta = 0f;
        return this.entity.getCooldownTracker().getCooldown(stack.itemStack.getItem(), delta);
    }

    @Override
    public String toString() {
        checkEntity();
        return entity.getName() + " (Player)";
    }
}
