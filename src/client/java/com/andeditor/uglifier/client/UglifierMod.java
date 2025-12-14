package com.andeditor.uglifier.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import eu.midnightdust.lib.config.MidnightConfig;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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

    static {
        var b = new ImmutableList.Builder<String>();
        b.add("world_list/", "world_lists/", "widget/", "widgets/");
        b.add("transferable_list/", "transferable_lists/", "tooltip/", "tooltips/");
        b.add("toast/", "toasts/", "statistics/", "statistic/", "realm_status/");
        b.add("spectator/", "social_interactions/", "social_interaction/");
        b.add("server_list/", "server_lists/", "recipe_book/", "recipe_books/");
        b.add("popup/", "popups/", "player_list/", "player_lists/");
        b.add("pending_invite/", "pending_invites/", "notification/", "notifications/");
        b.add("icon/", "icons/", "hud/", "gamemode_switcher/", "gamemode_switchers/");
        b.add("dialog/", "dialogs/", "container/", "containers/");
        b.add("boss_bar/", "boss_bars/", "advancements/", "advancement/");
        b.add("hanging_signs/", "hanging_sign/", "sprites/", "sprite/");
        b.add("title/", "titles/", "presets/", "preset/");
        b.add("realms/", "realm/", "gui/", "guis/", "textures/gui");
        BLOCKED_PREFIXES = b.build();
    }

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(ID, UglifierConfig.class);
    }

    private static final ImmutableList<String> BLOCKED_PREFIXES;

    public static boolean shouldUglifySprite(Identifier location) {
        if (!gui) {
            String path = location.getPath();
            for (String prefix : BLOCKED_PREFIXES) {
                if (path.startsWith(prefix)) return false;
            }
        }
        return true; // Just apply all sprite textures
    }

    public static boolean shouldUglify(Identifier location) {
        String path = location.getPath();
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

        // List of occupy pixels
        var pixels = new Object() {
            final IntOpenHashSet set = new IntOpenHashSet();
            void occupy(int x, int y) {
                set.add(x + y * width);
            }
            boolean contains(int x, int y) {
                return set.contains(x + y * width);
            }
        };

        int amount = (int) (Math.sqrt(width * height) * spreadAmountScale);

        for (int i = 0; i < amount; i++) {
            // Do ten attempts to pick the pixel
            for (int j = 0; j < 10; j++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                if (pixels.contains(x, y)) continue;
                int p = image.getPixel(x, y);

                // Do ten attempts to swap the pixel
                for (int l = 0; l < 10; l++) {
                    int xPlot = x + nextInt(random, -2, 2);
                    int yPlot = y + nextInt(random, -2, 2);
                    if (xPlot < 0 || xPlot >= width) continue;
                    if (yPlot < 0 || yPlot >= height) continue;
                    if (xPlot == x || yPlot == y) continue;
                    if (pixels.contains(xPlot, yPlot)) continue;

                    int pPlot = image.getPixel(xPlot, yPlot);
                    image.setPixel(xPlot, yPlot, p);
                    image.setPixel(x, y, pPlot);
                    pixels.occupy(x, y);
                    pixels.occupy(xPlot, yPlot);
                    break;
                }
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
        } else if (max == gf) {
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
