package org.figuramc.figura.mixin.font;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import org.figuramc.figura.ducks.extensions.StyleExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.reflect.Type;

@Mixin(Style.Serializer.class)
public class Style$SerializerMixin {
    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/util/text/Style;", at = @At("RETURN"))
    public void deserializeFiguraFont(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext, CallbackInfoReturnable<Style> cir){
        if (cir.getReturnValue() != null) {
            Style style = cir.getReturnValue();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("font")) {
                ((StyleExtension)style).setFont(new ResourceLocation(jsonObject.get("font").getAsString()));
                cir.setReturnValue(style);
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lcom/google/gson/JsonDeserializationContext;deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;)Ljava/lang/Object;"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/text/Style;access$202(Lnet/minecraft/util/text/Style;Ljava/lang/Boolean;)Ljava/lang/Boolean;"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/util/text/Style;access$602(Lnet/minecraft/util/text/Style;Ljava/lang/String;)Ljava/lang/String;")),
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/util/text/Style;", locals = LocalCapture.CAPTURE_FAILHARD)
    public void deserializeFiguraColor(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext, CallbackInfoReturnable<Style> cir, Style style){
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("color") && jsonObject.get("color").getAsString().contains("#")) {
            try {
                int i = Integer.parseInt(jsonObject.get("color").getAsString().substring(1), 16);
                ((StyleExtension)style).setRGBColor(i);
            }catch (NumberFormatException ignored){
            }
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lcom/google/gson/JsonDeserializationContext;deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;)Ljava/lang/Object;"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/text/Style;access$202(Lnet/minecraft/util/text/Style;Ljava/lang/Boolean;)Ljava/lang/Boolean;"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/util/text/Style;access$602(Lnet/minecraft/util/text/Style;Ljava/lang/String;)Ljava/lang/String;")),
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/util/text/Style;")
    public <T> T cancelIfHasFiguraColor(JsonDeserializationContext instance, JsonElement jsonElement, Type type){
        if (jsonElement.getAsString().contains("#")) {
            try {
                Integer.parseInt(jsonElement.getAsString().substring(1), 16);
                return null;
            }catch (NumberFormatException ignored){
                return instance.deserialize(jsonElement, type);
            }
        }
        return instance.deserialize(jsonElement, type);
    }
    @Inject(method = "serialize(Lnet/minecraft/util/text/Style;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/text/Style;access$800(Lnet/minecraft/util/text/Style;)Lnet/minecraft/util/text/event/HoverEvent;"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void setFiguraFont(Style style, Type type, JsonSerializationContext jsonSerializationContext, CallbackInfoReturnable<JsonElement> cir, JsonObject object){
        if (((StyleExtension)style).getFont() != null)
            object.addProperty("font", ((StyleExtension)style).getFont().toString());
        if (((StyleExtension)style).getRGBColor() != null)
            object.addProperty("color", String.format("#%06X", ((StyleExtension)style).getRGBColor()));
    }

}
