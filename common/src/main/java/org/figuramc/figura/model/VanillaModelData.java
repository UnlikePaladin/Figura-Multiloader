package org.figuramc.figura.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import org.figuramc.figura.math.vector.FiguraVec3;

import java.util.HashMap;
import java.util.Map;

public class VanillaModelData {

    public final Map<ParentType, PartData> partMap = new HashMap<ParentType, PartData>() {{
        for (ParentType value : ParentType.values()) {
            if (value.provider != null)
                put(value, new PartData());
        }
    }};

    public void update(RenderLivingBase<?> entityRenderer) {
        for (Map.Entry<ParentType, PartData> entry : partMap.entrySet()) {
            ParentType parent = entry.getKey();

            ModelBase vanillaModel;
            vanillaModel = entityRenderer.getMainModel();

            if (vanillaModel == null)
                continue;

            update(parent, vanillaModel);
        }
    }

    public void update(ParentType parent, ModelBase model) {
        ModelRenderer part = parent.provider.func.apply(model);
        if (part == null)
            return;

        update(parent, part);
    }

    public void update(ParentType parent, ModelRenderer part) {
        PartData data = partMap.get(parent);
        if (data != null)
            data.updateFromPart(part);
    }

    public static class PartData {

        public final FiguraVec3 pos = FiguraVec3.of();
        public final FiguraVec3 rot = FiguraVec3.of();
        public final FiguraVec3 scale = FiguraVec3.of(1, 1, 1);
        public boolean visible = false;

        private void updateFromPart(ModelRenderer model) {
            this.pos.set(model.rotationPointX, model.rotationPointY, -model.rotationPointZ);
            this.rot.set(Math.toDegrees(-model.rotateAngleX), Math.toDegrees(-model.rotateAngleY), Math.toDegrees(model.rotateAngleZ));
            //this.scale.set(model.xScale, model.yScale, model.zScale);
            this.visible = model.showModel;
        }
    }
}
