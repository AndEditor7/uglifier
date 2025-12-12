package com.andeditor.uglifier.client;

import eu.midnightdust.lib.config.MidnightConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ConfigScreen extends MidnightConfigScreen {

    private boolean enable, gui;
    private float spreadAmountScale;
    private float saturationAddition;
    private float hueShiftApply;
    private float noisePercentage;
    private int noiseStrength;
    private int colorReduction;

    public ConfigScreen(Screen parent) {
        super(parent, UglifierMod.ID);
    }

    @Override
    public void init() {
        super.init();
        enable = UglifierConfig.enable;
        gui = UglifierConfig.gui;
        spreadAmountScale = UglifierConfig.spreadAmountScale;
        saturationAddition = UglifierConfig.saturationAddition;
        hueShiftApply = UglifierConfig.hueShiftApply;
        noisePercentage = UglifierConfig.noisePercentage;
        noiseStrength = UglifierConfig.noiseStrength;
        colorReduction = UglifierConfig.colorReduction;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (enable != UglifierConfig.enable ||
            gui != UglifierConfig.gui ||
            spreadAmountScale != UglifierConfig.spreadAmountScale ||
            saturationAddition != UglifierConfig.saturationAddition ||
            hueShiftApply != UglifierConfig.hueShiftApply ||
            noisePercentage != UglifierConfig.noisePercentage ||
            noiseStrength != UglifierConfig.noiseStrength ||
            colorReduction != UglifierConfig.colorReduction) {
            Minecraft.getInstance().reloadResourcePacks();
        }
    }
}
