package com.andeditor.uglifier.mixin;

import com.andeditor.uglifier.client.UglifierMod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureContents.class)
abstract class TextureContentsMixin {
    @ModifyReturnValue(method = "load", at = @At("RETURN"))
    private static TextureContents uglifyTexture(TextureContents original, ResourceManager manager, Identifier id) {
        UglifierMod.tryUglify(original.image(), id);
        return original;
    }
}
