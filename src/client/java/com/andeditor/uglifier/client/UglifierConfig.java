package com.andeditor.uglifier.client;

import eu.midnightdust.lib.config.MidnightConfig;
import eu.midnightdust.lib.config.MidnightConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class UglifierConfig extends MidnightConfig {
    //@Comment(centered = true, name = "Use F3+T to reload the texture") public static Comment note;
    @Entry(name = "Enable")
    public static boolean enable = true;
    @Entry(min = 0, max = 10, name = "Pixel spread amount scale", isSlider = true, precision = 10)
    public static float spreadAmountScale = 1.5f;
    @Entry(min = 0, max = 1, name = "Saturation addition", isSlider = true)
    public static float saturationAddition = 0.15f;
    @Entry(min = 0, max = 1, name = "Hue shift apply", isSlider = true)
    public static float hueShiftApply = 0.3f;
    @Entry(min = 0, max = 1, name = "Noise percentage", isSlider = true)
    public static float noisePercentage = 0.15f;
    @Entry(min = 0, max = 100, name = "Noise strength", isSlider = true)
    public static int noiseStrength = 25;
    @Entry(min = 1, max = 64, name = "Color reduction", isSlider = true)
    public static int colorReduction = 16;

    @Override
    public MidnightConfigScreen getScreen(Screen parent) {
        return new ConfigScreen(parent);
    }
}
