package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.extensions.FontExtension;
import org.figuramc.figura.lua.api.ClientAPI;
import org.figuramc.figura.lua.api.nameplate.EntityNameplateCustomization;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(RenderPlayer.class)
public abstract class PlayerRendererMixin extends RenderLivingBase<AbstractClientPlayer> {

    @Shadow public abstract ModelPlayer getMainModel();

    public PlayerRendererMixin(RenderManager dispatcher, ModelBase entityModel, float shadowRadius) {
        super(dispatcher, entityModel, shadowRadius);
    }

    @Unique
    private Avatar avatar;

    @Inject(method = "renderEntityName(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDLjava/lang/String;D)V", at = @At("HEAD"), cancellable = true)
    private void renderNameTag(AbstractClientPlayer player, double ogX, double ogY, double z, String string, double distanceSq, CallbackInfo ci) {
        // return on config or high entity distance
        int config = Configs.ENTITY_NAMEPLATE.value;
        if (config == 0 || AvatarManager.panic || player.getDistanceSq(renderManager.renderViewEntity) > 4096)
            return;

        // get customizations
        Avatar avatar = AvatarManager.getAvatarForPlayer(player.getUniqueID());
        EntityNameplateCustomization custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.ENTITY;

        // customization boolean, which also is the permission check
        boolean hasCustom = custom != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1;
        if (custom != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 0) {
            avatar.noPermissions.add(Permissions.NAMEPLATE_EDIT);
        } else if (avatar != null) {
            avatar.noPermissions.remove(Permissions.NAMEPLATE_EDIT);
        }

        // enabled
        if (hasCustom && !custom.visible) {
            ci.cancel();
            return;
        }

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(player.getName());
        FiguraMod.pushProfiler("nameplate");

        GlStateManager.pushMatrix();

        // pivot
        FiguraMod.pushProfiler("pivot");
        FiguraVec3 pivot;
        float height = player.height - (player.isSneaking() ? 0.25F : 0.0F);

        if (hasCustom && custom.getPivot() != null)
            pivot = FiguraVec3.of(ogX + custom.getPivot().x, ogY + custom.getPivot().y, z + custom.getPivot().z);
        else
            pivot = FiguraVec3.of(ogX, ogY + height + 0.5f, z);

        GlStateManager.translate(pivot.x, pivot.y, pivot.z);
        GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);

        // rotation
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float)(this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // pos
        FiguraMod.popPushProfiler("position");
        if (hasCustom && custom.getPos() != null) {
            FiguraVec3 pos = custom.getPos();
            GlStateManager.translate(pos.x, pos.y, pos.z);
        }

        // scale
        FiguraMod.popPushProfiler("scale");
        float scale = 0.025f;
        FiguraVec3 scaleVec = FiguraVec3.of(-scale, -scale, scale);
        if (hasCustom && custom.getScale() != null)
            scaleVec.multiply(custom.getScale());

        GlStateManager.scale((float) scaleVec.x, (float) scaleVec.y, (float) scaleVec.z);

        // text
        ITextComponent name = new TextComponentString(player.getName());
        FiguraMod.popPushProfiler("text");
        ITextComponent replacement = hasCustom && custom.getJson() != null ? custom.getJson().createCopy() : name;

        // name
        replacement = TextUtils.replaceInText(replacement, "\\$\\{name\\}", name);

        // badges
        FiguraMod.popPushProfiler("badges");
        replacement = Badges.appendBadges(replacement, player.getUniqueID(), config > 1);

        FiguraMod.popPushProfiler("applyName");
        ITextComponent text = new TextComponentString(string);
        text = TextUtils.replaceInText(text, "\\b" + Pattern.quote(player.getName()) + "\\b", replacement);

        // * variables * //
        FiguraMod.popPushProfiler("colors");
        boolean notSneaking = !player.isSneaking();
        boolean deadmau = text.getFormattedText().equals("deadmau5");
// TODO : opacity bg option
        int bgColor = hasCustom && custom.background != null ? custom.background : (int) (0.25f * 0xFF) << 24;
        int outlineColor = hasCustom && custom.outlineColor != null ? custom.outlineColor : 0x202020;

        boolean outline = hasCustom && custom.outline;
        boolean shadow = hasCustom && custom.shadow;

        Integer light = hasCustom && custom.light != null ? custom.light : null;

        // Referenced from EntityRenderer drawNameplate
        FontRenderer font = this.getFontRendererFromRenderManager();
        if (!player.isSneaking()) {
            GlStateManager.disableDepth();
        }
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
//TODO investigate why depthMask is set to false only sometimes
        // render scoreboard
        FiguraMod.popPushProfiler("render");
        FiguraMod.pushProfiler("scoreboard");
        boolean hasScore = false;
        if (distanceSq < 100) {
            // get scoreboard
            Scoreboard scoreboard = player.getWorldScoreboard();
            ScoreObjective scoreboardObjective = scoreboard.getObjectiveInDisplaySlot(2);
            if (scoreboardObjective != null) {
                hasScore = true;

                // render scoreboard
                Score score = scoreboard.getOrCreateScore(player.getName(), scoreboardObjective);

                ITextComponent text1 = new TextComponentString(Integer.toString(score.getScorePoints())).appendText(" ").appendText(scoreboardObjective.getDisplayName());
                float x = -font.getStringWidth(text1.getFormattedText()) / 2f;
                float y = deadmau ? -10f : 0f;

                GlStateManager.disableTexture2D();
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bufferbuilder.pos(x - 1, -1 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
                bufferbuilder.pos(x - 1, 8 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
                bufferbuilder.pos(-x + 1, 8 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
                bufferbuilder.pos(-x + 1, -1 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
                tessellator.draw();
                GlStateManager.enableTexture2D();


                font.drawString(text1.getFormattedText(), x, y, 0x20FFFFFF, false);
                if (notSneaking) {
                    if (outline)
                        ((FontExtension)font).figura$drawInBatch8xOutline(text1, x, y, -1, outlineColor);
                    else {
                        if (shadow) {
                            GlStateManager.pushMatrix();
                            GlStateManager.scale(1, 1, -1);
                        }
                        font.drawString(text1.getFormattedText(), x, y, -1, shadow);
                        if (shadow)
                            GlStateManager.popMatrix();
                    }
                }
            }
        }

        // render name
        FiguraMod.popPushProfiler("name");
        List<ITextComponent> textList = TextUtils.splitText(text, "\n");

        for (int i = 0; i < textList.size(); i++) {
            ITextComponent text1 = textList.get(i);

            if (text1.getFormattedText().isEmpty())
                continue;

            int line = i - textList.size() + (hasScore ? 0 : 1);

            float x = -font.getStringWidth(text1.getFormattedText()) / 2f;
            float y = (deadmau ? -10f : 0f) + (font.FONT_HEIGHT + 1) * line;

            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR); // TODO: Change this so it obeys the color in the nameplate customization
            bufferbuilder.pos(x - 1, -1 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
            bufferbuilder.pos(x - 1, 8 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
            bufferbuilder.pos(-x + 1, 8 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
            bufferbuilder.pos(-x + 1, -1 + y, 0.0).color((float)(bgColor >> 16) / 255.0F, (float)(bgColor >> 8 & 0xFF) / 255.0F, (float)(bgColor & 0xFF) / 255.0F, (float)(bgColor >> 24 & 0xFF) / 255.0f).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();


            font.drawString(text1.getFormattedText(), x, y, 0x20FFFFFF, false);
            if (notSneaking) {
                if (outline)
                    ((FontExtension)font).figura$drawInBatch8xOutline(text1, x, y, -1, outlineColor);
                else {
                    if (shadow) {
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(1, 1, -1);
                    }
                    font.drawString(text1.getFormattedText(), x, y, -1, shadow);
                    if (shadow)
                        GlStateManager.popMatrix();
                }
            }

            // Renders Simple VC icons at the end of the nameplate
            if (ClientAPI.isModLoaded("voicechat") && textList.get(i) == textList.get(textList.size()-1));
               // SimpleVCCompat.renderSimpleVCIcon(player, text1, stack, multiBufferSource, light);
        }
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        FiguraMod.popProfiler(5);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        ci.cancel();
    }

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/model/ModelPlayer;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"), method = "renderLeftArm")
    private void onRenderHandLeft(AbstractClientPlayer player, CallbackInfo ci) {
        avatar = AvatarManager.getAvatarForPlayer(player.getUniqueID());
        if (avatar != null && avatar.luaRuntime != null) {
            VanillaPart part = avatar.luaRuntime.vanilla_model.PLAYER;
            ModelPlayer model = this.getMainModel();

            part.save(model);

            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                part.preTransform(model);
                part.posTransform(model);
            }
        }
    }
    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/model/ModelPlayer;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"), method = "renderRightArm")
    private void onRenderRight(AbstractClientPlayer player, CallbackInfo ci) {
        avatar = AvatarManager.getAvatarForPlayer(player.getUniqueID());
        if (avatar != null && avatar.luaRuntime != null) {
            VanillaPart part = avatar.luaRuntime.vanilla_model.PLAYER;
            ModelPlayer model = this.getMainModel();

            part.save(model);

            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                part.preTransform(model);
                part.posTransform(model);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "renderRightArm")
    private void postRenderHandRight(AbstractClientPlayer player, CallbackInfo ci) {
        if (avatar == null)
            return;

        float delta = Minecraft.getMinecraft().getRenderPartialTicks();
        avatar.firstPersonRender(RenderTypes.FiguraBufferSource.INSTANCE, player, (RenderPlayer) (Object) this, getMainModel().bipedRightArm, player.getBrightnessForRender(), delta);

        if (avatar.luaRuntime != null)
            avatar.luaRuntime.vanilla_model.PLAYER.restore(this.getMainModel());

        avatar = null;
    }

    @Inject(at = @At("RETURN"), method = "renderRightArm")
    private void postRenderHandLeft(AbstractClientPlayer player, CallbackInfo ci) {
        if (avatar == null)
            return;

        float delta = Minecraft.getMinecraft().getRenderPartialTicks();
        avatar.firstPersonRender(RenderTypes.FiguraBufferSource.INSTANCE, player, (RenderPlayer) (Object) this, getMainModel().bipedLeftArm, player.getBrightnessForRender(), delta);

        if (avatar.luaRuntime != null)
            avatar.luaRuntime.vanilla_model.PLAYER.restore(this.getMainModel());

        avatar = null;
    }

    @Inject(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At("HEAD"), cancellable = true)
    private void setupRotations(AbstractClientPlayer entity, float f, float g, float partialTicks, CallbackInfo cir) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(avatar) && !avatar.luaRuntime.renderer.getRootRotationAllowed()) {
            cir.cancel();
        }
    }
}
