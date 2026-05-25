package com.example.dyeing.data;

import net.minecraft.nbt.CompoundTag;

public record PaintData(
        int argb,
        float scale,
        boolean hasScaleAnimation,
        float scaleFrom,
        float scaleTo,
        float scaleAlphaFrom,
        float scaleAlphaTo,
        int scalePeriod,
        boolean hasColorAnimation,
        int colorFromArgb,
        int colorToArgb,
        int colorPeriod
) {
    public static PaintData staticPaint(int argb, float scale) {
        return new PaintData(argb, scale, false, scale, scale, 1.0F, 1.0F, 1, false, argb, argb, 1);
    }

    public static PaintData withScaleAnimation(
            int argb,
            float scaleFrom,
            float scaleTo,
            float scaleAlphaFrom,
            float scaleAlphaTo,
            int scalePeriod
    ) {
        return new PaintData(argb, scaleFrom, true, scaleFrom, scaleTo, scaleAlphaFrom, scaleAlphaTo, scalePeriod, false, argb, argb, 1);
    }

    public static PaintData withColorAnimation(int colorFromArgb, int colorToArgb, float scale, int colorPeriod) {
        return new PaintData(colorFromArgb, scale, false, scale, scale, 1.0F, 1.0F, 1, true, colorFromArgb, colorToArgb, colorPeriod);
    }

    public static PaintData withCombinedAnimation(
            int colorFromArgb,
            int colorToArgb,
            float scaleFrom,
            float scaleTo,
            float scaleAlphaFrom,
            float scaleAlphaTo,
            int scalePeriod,
            int colorPeriod
    ) {
        return new PaintData(
                colorFromArgb,
                scaleFrom,
                true,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                true,
                colorFromArgb,
                colorToArgb,
                colorPeriod
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", this.argb);
        tag.putFloat("Scale", this.scale);
        tag.putBoolean("HasScaleAnimation", this.hasScaleAnimation);
        tag.putFloat("ScaleFrom", this.scaleFrom);
        tag.putFloat("ScaleTo", this.scaleTo);
        tag.putFloat("ScaleAlphaFrom", this.scaleAlphaFrom);
        tag.putFloat("ScaleAlphaTo", this.scaleAlphaTo);
        tag.putInt("ScalePeriod", this.scalePeriod);
        tag.putBoolean("HasColorAnimation", this.hasColorAnimation);
        tag.putInt("ColorFrom", this.colorFromArgb);
        tag.putInt("ColorTo", this.colorToArgb);
        tag.putInt("ColorPeriod", this.colorPeriod);
        return tag;
    }

    public static PaintData load(CompoundTag tag) {
        float savedScale = tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F;
        int savedColor = tag.getInt("Color");
        boolean hasScaleAnimation = tag.getBoolean("HasScaleAnimation");
        float scaleFrom = tag.contains("ScaleFrom") ? tag.getFloat("ScaleFrom") : savedScale;
        float scaleTo = tag.contains("ScaleTo") ? tag.getFloat("ScaleTo") : savedScale;
        float scaleAlphaFrom = tag.contains("ScaleAlphaFrom") ? tag.getFloat("ScaleAlphaFrom") : 1.0F;
        float scaleAlphaTo = tag.contains("ScaleAlphaTo") ? tag.getFloat("ScaleAlphaTo") : 1.0F;
        int scalePeriod = Math.max(1, tag.contains("ScalePeriod") ? tag.getInt("ScalePeriod") : 1);
        boolean hasColorAnimation = tag.getBoolean("HasColorAnimation");
        int colorFrom = tag.contains("ColorFrom") ? tag.getInt("ColorFrom") : savedColor;
        int colorTo = tag.contains("ColorTo") ? tag.getInt("ColorTo") : savedColor;
        int colorPeriod = Math.max(1, tag.contains("ColorPeriod") ? tag.getInt("ColorPeriod") : 1);
        return new PaintData(
                savedColor,
                savedScale,
                hasScaleAnimation,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                hasColorAnimation,
                colorFrom,
                colorTo,
                colorPeriod
        );
    }

    public PaintRenderState resolve(float animationTime) {
        float resolvedScale = this.hasScaleAnimation
                ? lerp(cycleProgress(animationTime, this.scalePeriod), this.scaleFrom, this.scaleTo)
                : this.scale;
        int resolvedColor = this.hasColorAnimation
                ? lerpArgb(cycleProgress(animationTime, this.colorPeriod), this.colorFromArgb, this.colorToArgb)
                : this.argb;
        float alphaFactor = this.hasScaleAnimation
                ? lerp(cycleProgress(animationTime, this.scalePeriod), this.scaleAlphaFrom, this.scaleAlphaTo)
                : 1.0F;

        float red = ((resolvedColor >> 16) & 0xFF) / 255.0F;
        float green = ((resolvedColor >> 8) & 0xFF) / 255.0F;
        float blue = (resolvedColor & 0xFF) / 255.0F;
        float alpha = (((resolvedColor >>> 24) & 0xFF) / 255.0F) * alphaFactor;
        return new PaintRenderState(red, green, blue, clamp01(alpha), resolvedScale);
    }

    private static float cycleProgress(float animationTime, int period) {
        float safePeriod = Math.max(1, period);
        float progress = animationTime % safePeriod;
        if (progress < 0.0F) {
            progress += safePeriod;
        }
        return progress / safePeriod;
    }

    private static int lerpArgb(float progress, int from, int to) {
        int fromA = (from >>> 24) & 0xFF;
        int fromR = (from >>> 16) & 0xFF;
        int fromG = (from >>> 8) & 0xFF;
        int fromB = from & 0xFF;

        int toA = (to >>> 24) & 0xFF;
        int toR = (to >>> 16) & 0xFF;
        int toG = (to >>> 8) & 0xFF;
        int toB = to & 0xFF;

        int a = Math.round(lerp(progress, fromA, toA));
        int r = Math.round(lerp(progress, fromR, toR));
        int g = Math.round(lerp(progress, fromG, toG));
        int b = Math.round(lerp(progress, fromB, toB));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float lerp(float progress, float from, float to) {
        return from + (to - from) * clamp01(progress);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
