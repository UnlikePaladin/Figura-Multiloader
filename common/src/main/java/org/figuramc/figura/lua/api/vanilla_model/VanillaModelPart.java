package org.figuramc.figura.lua.api.vanilla_model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.ParentType;

import java.util.function.Function;

@LuaWhitelist
@LuaTypeDoc(
        name = "VanillaModelPart",
        value = "vanilla_model_part"
)
public class VanillaModelPart extends VanillaPart {

    private final ParentType parentType;
    private final Function<ModelBase, ModelRenderer> provider;

    // backup
    private float backupPosX, backupPosY, backupPosZ;
    private float backupRotX, backupRotY, backupRotZ;
    private float backupScaleX, backupScaleY, backupScaleZ;
    private boolean originVisible;
    private boolean saved;

    // part getters
    private final FiguraVec3 originRot = FiguraVec3.of();
    private final FiguraVec3 originPos = FiguraVec3.of();
    private final FiguraVec3 originScale = FiguraVec3.of();

    public VanillaModelPart(Avatar owner, String name, ParentType parentType, Function<ModelBase, ModelRenderer> provider) {
        super(owner, name);
        this.parentType = parentType;
        this.provider = provider;
    }

    private ModelRenderer getPart(ModelBase model) {
        return provider == null ? null : provider.apply(model);
    }

    @Override
    public void save(ModelBase model) {
        saved = false;
        ModelRenderer part = getPart(model);
        if (part == null) return;

        // set getters
        originRot.set(-part.rotateAngleX, -part.rotateAngleY, part.rotateAngleZ);
        originRot.scale(180 / Math.PI);

        FiguraVec3 pivot = parentType.offset.copy();
        pivot.subtract(part.rotationPointX, part.rotationPointY, part.rotationPointZ);
        pivot.multiply(1, -1, -1);
        originPos.set(pivot);

        //originScale.set(part.xScale, part.yScale, part.zScale);

        // save visible
        originVisible = part.showModel;

        // save pos
        backupPosX = part.rotationPointX;
        backupPosY = part.rotationPointY;
        backupPosZ = part.rotationPointZ;

        // save rot
        backupRotX = part.rotateAngleX;
        backupRotY = part.rotateAngleY;
        backupRotZ = part.rotateAngleZ;

        // save scale
        //backupScaleX = part.xScale;
        //backupScaleY = part.yScale;
        //backupScaleZ = part.zScale;

        saved = true;
    }

    @Override
    public void preTransform(ModelBase model) {
        if (!saved) return;

        ModelRenderer part = getPart(model);
        if (part == null) return;

        // pos
        if (pos != null) {
            part.rotationPointX += (float) -pos.x;
            part.rotationPointY += (float) -pos.y;
            part.rotationPointZ += (float) pos.z;
        }

        // rot
        if (rot != null) {
            FiguraVec3 rot = this.rot.toRad();
            part.rotateAngleX = (float) -rot.x;
            part.rotateAngleY = (float) -rot.y;
            part.rotateAngleZ = (float) rot.z;
        }
        if (offsetRot != null) {
            FiguraVec3 rot = offsetRot.toRad();
            part.rotateAngleX += (float) -rot.x;
            part.rotateAngleY += (float) -rot.y;
            part.rotateAngleZ += (float) rot.z;
        }
        // scale
//        if (scale != null) {
//            part.xScale = (float) scale.x;
//            part.yScale = (float) scale.y;
//            part.zScale = (float) scale.z;
//        }
//        if (offsetScale != null)
//            part.offsetScale(offsetScale.asVec3f());
    }

    @Override
    public void posTransform(ModelBase model) {
        if (visible == null)
            return;

        ModelRenderer part = getPart(model);
        if (part != null)
            part.showModel = visible;
    }

    @Override
    public void restore(ModelBase model) {
        ModelRenderer part = getPart(model);
        if (part == null) return;

        // restore visible
        part.showModel = originVisible;

        if (!saved) return;

        // restore pos
        part.rotationPointX = backupPosX;
        part.rotationPointY = backupPosY;
        part.rotationPointZ = backupPosZ;

        // restore rot
        part.rotateAngleX = backupRotX;
        part.rotateAngleY = backupRotY;
        part.rotateAngleZ = backupRotZ;

        // restore scale
//        part.xScale = backupScaleX;
//        part.yScale = backupScaleY;
//        part.zScale = backupScaleZ;
    }

    @LuaWhitelist
    @LuaMethodDoc("vanilla_model_part.get_origin_visible")
    public boolean getOriginVisible() {
        return this.originVisible;
    }

    @LuaWhitelist
    @LuaMethodDoc("vanilla_model_part.get_origin_rot")
    public FiguraVec3 getOriginRot() {
        return this.originRot.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc("vanilla_model_part.get_origin_pos")
    public FiguraVec3 getOriginPos() {
        return this.originPos.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc("vanilla_model_part.get_origin_scale")
    public FiguraVec3 getOriginScale() {
        return this.originScale.copy();
    }

    @Override
    public String toString() {
        return "VanillaModelPart";
    }
}
