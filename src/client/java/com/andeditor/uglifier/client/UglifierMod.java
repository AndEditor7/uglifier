package com.andeditor.uglifier.client;

import com.mojang.blaze3d.platform.NativeImage;
import eu.midnightdust.lib.config.MidnightConfig;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.Random;
import java.util.random.RandomGenerator;

import static com.andeditor.uglifier.client.UglifierConfig.*;

/** Main Uglifier Mod class */
public class UglifierMod implements ClientModInitializer {

    public static final String ID = "uglifier";

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(ID, UglifierConfig.class);
    }

    public static boolean shouldUglifySprite(Identifier location) {
        if (!gui) {
            var path = location.getPath();
            if (path.startsWith("world_list/")) return false;
            if (path.startsWith("world_lists/")) return false;
            if (path.startsWith("widget/")) return false;
            if (path.startsWith("widgets/")) return false;
            if (path.startsWith("transferable_list/")) return false;
            if (path.startsWith("transferable_lists/")) return false;
            if (path.startsWith("tooltip/")) return false;
            if (path.startsWith("tooltips/")) return false;
            if (path.startsWith("toast/")) return false;
            if (path.startsWith("toasts/")) return false;
            if (path.startsWith("statistics/")) return false;
            if (path.startsWith("statistic/")) return false;
            if (path.startsWith("spectator/")) return false;
            if (path.startsWith("social_interactions/")) return false;
            if (path.startsWith("social_interaction/")) return false;
            if (path.startsWith("server_list/")) return false;
            if (path.startsWith("server_lists/")) return false;
            if (path.startsWith("recipe_book/")) return false;
            if (path.startsWith("recipe_books/")) return false;
            if (path.startsWith("realm_status/")) return false;
            if (path.startsWith("popup/")) return false;
            if (path.startsWith("popups/")) return false;
            if (path.startsWith("player_list/")) return false;
            if (path.startsWith("player_lists/")) return false;
            if (path.startsWith("pending_invite/")) return false;
            if (path.startsWith("pending_invites/")) return false;
            if (path.startsWith("notification/")) return false;
            if (path.startsWith("notifications/")) return false;
            if (path.startsWith("icon/")) return false;
            if (path.startsWith("icons/")) return false;
            if (path.startsWith("hud/")) return false;
            if (path.startsWith("gamemode_switcher/")) return false;
            if (path.startsWith("gamemode_switchers/")) return false;
            if (path.startsWith("dialog/")) return false;
            if (path.startsWith("dialogs/")) return false;
            if (path.startsWith("container/")) return false;
            if (path.startsWith("containers/")) return false;
            if (path.startsWith("boss_bar/")) return false;
            if (path.startsWith("boss_bars/")) return false;
            if (path.startsWith("advancements/")) return false;
            if (path.startsWith("advancement/")) return false;
            if (path.startsWith("hanging_signs/")) return false;
            if (path.startsWith("hanging_sign/")) return false;
            if (path.startsWith("sprites/")) return false;
            if (path.startsWith("sprite/")) return false;
            if (path.startsWith("title/")) return false;
            if (path.startsWith("titles/")) return false;
            if (path.startsWith("presets/")) return false;
            if (path.startsWith("preset/")) return false;
            if (path.startsWith("realms/")) return false;
            if (path.startsWith("realm/")) return false;
            if (path.startsWith("gui/")) return false;
            if (path.startsWith("guis/")) return false;
            if (path.startsWith("textures/gui")) return false;
        }
        return true; // Just apply all sprite textures
    }

    public static boolean shouldUglify(Identifier location) {
        var path = location.getPath();
        if (path.startsWith("textures/effect")) return false;
        if (path.startsWith("textures/font")) return false;
        if (path.startsWith("textures/misc")) return false;
        if (!gui && path.startsWith("textures/gui")) return false;
        return true;
    }

    // Get called from the mixin injection
    public static void tryUglify(NativeImage image, Identifier location) {
        if (enable && shouldUglify(location)) {
            uglify(image, location);
        }
    }

    // Get called from the mixin injection
    public static void tryUglifySprite(NativeImage image, Identifier location) {
        if (enable && shouldUglifySprite(location)) {
            uglify(image, location);
        }
    }

    /** Uglify the texture */
    public static void uglify(NativeImage image, Identifier location) {
        var hash = location.hashCode();
        var random = new Random(hash);
        spreadPixels(image, random);
        random.setSeed(hash * 31);
        shiftHue(image, random);
        random.setSeed(hash * 53);
        makeNoise(image, random);
        lowColor(image);
    }

    /** Offset/spread pixels randomly in a texture */
    public static void spreadPixels(NativeImage image, RandomGenerator random) {
        var width = image.getWidth();
        var height = image.getHeight();

        var imageSrc = new Object() {
            final int[] pixels = image.getPixelsABGR();
            int getPixel(int x, int y) {
                return pixels[x + y * width];
            }
        };

        int amount = (int) (Math.sqrt(width * height) * spreadAmountScale);
        for (int i = 0; i < amount; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int p = imageSrc.getPixel(x, y);

            for (int j = 0; j < 5; j++) {
                int xPlot = x + nextInt(random, -2, 2);
                int yPlot = y + nextInt(random, -2, 2);
                if (xPlot < 0 || xPlot >= width) continue;
                if (yPlot < 0 || yPlot >= height) continue;
                if (xPlot == x || yPlot == y) continue;

                int pPlot = imageSrc.getPixel(xPlot, yPlot);
                image.setPixelABGR(xPlot, yPlot, p);
                image.setPixelABGR(x, y, pPlot);
                break;
            }
        }
    }

    /** Shift hue in a texture */
    public static void shiftHue(NativeImage image, RandomGenerator random) {
        var width = image.getWidth();
        var height = image.getHeight();
        float shift = lerp(0.1f, 0.9f, random.nextFloat()); // Convert shift to fraction of full hue rotation
        float[] hsv = new float[3];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int oldPixel = image.getPixel(x, y);
                rgbToHsv(ARGB.red(oldPixel), ARGB.green(oldPixel), ARGB.blue(oldPixel), hsv);
                hsv[1] = Math.min(hsv[1] + saturationAddition, 1.0f); // Make it a bit more saturation

                // Shift hue and wrap around if necessary
                hsv[0] = (hsv[0] + shift) % 1f; // Hue stays within [0, 1] range

                // Apply percentage of a new color.
                int newPixel = ARGB.linearLerp(hueShiftApply, oldPixel, Mth.hsvToArgb(hsv[0], hsv[1], hsv[2], ARGB.alpha(oldPixel)));

                // Apply new pixel value
                image.setPixel(x, y, newPixel);
            }
        }
    }

    /** Make some random noise in a texture */
    public static void makeNoise(NativeImage image, RandomGenerator random) {
        var width = image.getWidth();
        var height = image.getHeight();

        Int2IntFunction a = p -> Mth.clamp(p + nextInt(random, -noiseStrength, noiseStrength), 0, 255);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (random.nextFloat() > noisePercentage)
                    continue;

                int pixel = image.getPixel(x, y);
                if (ARGB.alpha(pixel) < 20)
                    continue;

                int newPixel = ARGB.color(ARGB.alpha(pixel), a.get(ARGB.red(pixel)), a.get(ARGB.green(pixel)), a.get(ARGB.blue(pixel)));
                image.setPixel(x, y, newPixel);
            }
        }
    }

    /** Apply Low resolution colors to a texture */
    public static void lowColor(NativeImage image) {
        var width = image.getWidth();
        var height = image.getHeight();

        Int2IntFunction a = p -> p / colorReduction * colorReduction;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int oldPixel = image.getPixel(x, y);
                int newPixel = ARGB.color(ARGB.alpha(oldPixel), a.get(ARGB.red(oldPixel)), a.get(ARGB.green(oldPixel)), a.get(ARGB.blue(oldPixel)));
                image.setPixel(x, y, newPixel);
            }
        }
    }

    private static int nextInt(RandomGenerator random, int min, int max) {
        return min >= max ? min : random.nextInt(max - min + 1) + min;
    }

    private static float lerp(float fromValue, float toValue, float progress) {
        return fromValue + (toValue - fromValue) * progress;
    }

    /**
     * Converts RGB values to HSV. float array {hue [0-1], saturation [0-1], value [0-1]}
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     */
    public static void rgbToHsv(int r, int g, int b, float[] hsv) {
        // Normalize RGB to [0,1]
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(Math.max(rf, gf), bf);
        float min = Math.min(Math.min(rf, gf), bf);
        float range = max - min;
        if (range == 0) {
            hsv[0] = 0;
        } else if (max == rf) {
            hsv[0] = (60 * (gf - bf) / range + 360) % 360;
        } else if (max == g) {
            hsv[0] = 60 * (bf - rf) / range + 120;
        } else {
            hsv[0] = 60 * (rf - gf) / range + 240;
        }
        hsv[0] /= 360f;

        if (max > 0) {
            hsv[1] = 1 - min / max;
        } else {
            hsv[1] = 0;
        }

        hsv[2] = max;
    }


}
