package com.andeditor.uglifier.client;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.Random;
import java.util.random.RandomGenerator;

/** Main Uglifier Mod class */
public class UglifierModClient implements ClientModInitializer {

    // TODO: Add mod configuration soon
    public static final boolean ENABLE = true;
    public static final float SPREAD_SCALE_AMOUNT = 1.5f;
    public static final float SATURATION_ADDITION = 0.15f;
    public static final float HUE_SHIFT_APPLY = 0.3f;
    public static final float NOISE_PERCENTAGE = 0.15f;
    public static final int NOISE_STRENGTH = 25;
    public static final int COLOR_REDUCTION = 16;

    @Override
    public void onInitializeClient() {
        // left it blank in case it need init
    }


    public static boolean shouldUglifySprite(Identifier location) {
        return true; // Just apply all sprite textures
    }

    public static boolean shouldUglify(Identifier location) {
        if (location.getPath().startsWith("textures/effect")) return false;
        if (location.getPath().startsWith("textures/font")) return false;
        if (location.getPath().startsWith("textures/misc")) return false;
        return true;
    }

    // Get called from the mixin injection
    public static void tryUglify(NativeImage image, Identifier location) {
        if (ENABLE && shouldUglify(location)) {
            uglify(image, location);
        }
    }

    // Get called from the mixin injection
    public static void tryUglifySprite(NativeImage image, Identifier location) {
        if (ENABLE && shouldUglifySprite(location)) {
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

        int amount = (int) (Math.sqrt(width * height) * SPREAD_SCALE_AMOUNT);
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
                hsv[1] = Math.min(hsv[1] + SATURATION_ADDITION, 1.0f); // Make it a bit more saturation

                // Shift hue and wrap around if necessary
                hsv[0] = (hsv[0] + shift) % 1f; // Hue stays within [0, 1] range

                // Apply percentage of a new color.
                int newPixel = ARGB.linearLerp(HUE_SHIFT_APPLY, oldPixel, Mth.hsvToArgb(hsv[0], hsv[1], hsv[2], ARGB.alpha(oldPixel)));

                // Apply new pixel value
                image.setPixel(x, y, newPixel);
            }
        }
    }

    /** Make some random noise in a texture */
    public static void makeNoise(NativeImage image, RandomGenerator random) {
        var width = image.getWidth();
        var height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (random.nextFloat() > NOISE_PERCENTAGE)
                    continue;

                int pixel = image.getPixel(x, y);
                if (ARGB.alpha(pixel) < 20)
                    continue;

                Int2IntFunction a = p -> Mth.clamp(p + nextInt(random, -NOISE_STRENGTH, NOISE_STRENGTH), 0, 255);
                int newPixel = ARGB.color(ARGB.alpha(pixel), a.get(ARGB.red(pixel)), a.get(ARGB.green(pixel)), a.get(ARGB.blue(pixel)));
                image.setPixel(x, y, newPixel);
            }
        }
    }

    /** Apply Low resolution colors to a texture */
    public static void lowColor(NativeImage image) {
        var width = image.getWidth();
        var height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int oldPixel = image.getPixel(x, y);
                int newPixel = ARGB.color(ARGB.alpha(oldPixel), ARGB.red(oldPixel) / COLOR_REDUCTION * COLOR_REDUCTION, ARGB.green(oldPixel) / COLOR_REDUCTION * COLOR_REDUCTION, ARGB.blue(oldPixel) / COLOR_REDUCTION * COLOR_REDUCTION);
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
