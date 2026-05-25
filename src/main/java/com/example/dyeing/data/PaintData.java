package com.example.dyeing.data;

import net.minecraft.nbt.CompoundTag;

public record PaintData(
        int argb,
        float scale,
        float offsetX,
        float offsetY,
        float offsetZ,
        long startGameTime,
        boolean hasScaleAnimation,
        float scaleFrom,
        float scaleTo,
        float scaleAlphaFrom,
        float scaleAlphaTo,
        int scalePeriod,
        int scalePlayCount,
        AnimationEndAction scaleEndAction,
        boolean hasColorAnimation,
        int colorFromArgb,
        int colorToArgb,
        int colorPeriod,
        int colorPlayCount,
        AnimationEndAction colorEndAction
) {
    public static PaintData staticPaint(int argb, float scale) {
        return staticPaint(argb, scale, 0.0F, 0.0F, 0.0F, 0L);
    }

    public static PaintData staticPaint(int argb, float scale, float offsetX, float offsetY, float offsetZ) {
        return staticPaint(argb, scale, offsetX, offsetY, offsetZ, 0L);
    }

    public static PaintData staticPaint(int argb, float scale, float offsetX, float offsetY, float offsetZ, long startGameTime) {
        return new PaintData(
                argb,
                scale,
                offsetX,
                offsetY,
                offsetZ,
                startGameTime,
                false,
                scale,
                scale,
                1.0F,
                1.0F,
                1,
                -1,
                AnimationEndAction.END,
                false,
                argb,
                argb,
                1,
                -1,
                AnimationEndAction.END
        );
    }

    public static PaintData withScaleAnimation(
            int argb,
            float scaleFrom,
            float scaleTo,
            float scaleAlphaFrom,
            float scaleAlphaTo,
            int scalePeriod,
            float offsetX,
            float offsetY,
            float offsetZ,
            long startGameTime,
            int scalePlayCount,
            AnimationEndAction scaleEndAction
    ) {
        return new PaintData(
                argb,
                scaleFrom,
                offsetX,
                offsetY,
                offsetZ,
                startGameTime,
                true,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                scalePlayCount,
                scaleEndAction,
                false,
                argb,
                argb,
                1,
                -1,
                AnimationEndAction.END
        );
    }

    public static PaintData withColorAnimation(
            int colorFromArgb,
            int colorToArgb,
            float scale,
            int colorPeriod,
            float offsetX,
            float offsetY,
            float offsetZ,
            long startGameTime,
            int colorPlayCount,
            AnimationEndAction colorEndAction
    ) {
        return new PaintData(
                colorFromArgb,
                scale,
                offsetX,
                offsetY,
                offsetZ,
                startGameTime,
                false,
                scale,
                scale,
                1.0F,
                1.0F,
                1,
                -1,
                AnimationEndAction.END,
                true,
                colorFromArgb,
                colorToArgb,
                colorPeriod,
                colorPlayCount,
                colorEndAction
        );
    }

    public static PaintData withCombinedAnimation(
            int colorFromArgb,
            int colorToArgb,
            float scaleFrom,
            float scaleTo,
            float scaleAlphaFrom,
            float scaleAlphaTo,
            int scalePeriod,
            int colorPeriod,
            float offsetX,
            float offsetY,
            float offsetZ,
            long startGameTime,
            int scalePlayCount,
            AnimationEndAction scaleEndAction,
            int colorPlayCount,
            AnimationEndAction colorEndAction
    ) {
        return new PaintData(
                colorFromArgb,
                scaleFrom,
                offsetX,
                offsetY,
                offsetZ,
                startGameTime,
                true,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                scalePlayCount,
                scaleEndAction,
                true,
                colorFromArgb,
                colorToArgb,
                colorPeriod,
                colorPlayCount,
                colorEndAction
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", this.argb);
        tag.putFloat("Scale", this.scale);
        tag.putFloat("OffsetX", this.offsetX);
        tag.putFloat("OffsetY", this.offsetY);
        tag.putFloat("OffsetZ", this.offsetZ);
        tag.putLong("StartGameTime", this.startGameTime);
        tag.putBoolean("HasScaleAnimation", this.hasScaleAnimation);
        tag.putFloat("ScaleFrom", this.scaleFrom);
        tag.putFloat("ScaleTo", this.scaleTo);
        tag.putFloat("ScaleAlphaFrom", this.scaleAlphaFrom);
        tag.putFloat("ScaleAlphaTo", this.scaleAlphaTo);
        tag.putInt("ScalePeriod", this.scalePeriod);
        tag.putInt("ScalePlayCount", this.scalePlayCount);
        tag.putString("ScaleEndAction", this.scaleEndAction.name());
        tag.putBoolean("HasColorAnimation", this.hasColorAnimation);
        tag.putInt("ColorFrom", this.colorFromArgb);
        tag.putInt("ColorTo", this.colorToArgb);
        tag.putInt("ColorPeriod", this.colorPeriod);
        tag.putInt("ColorPlayCount", this.colorPlayCount);
        tag.putString("ColorEndAction", this.colorEndAction.name());
        return tag;
    }

    public static PaintData load(CompoundTag tag) {
        float savedScale = tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F;
        float offsetX = tag.contains("OffsetX") ? tag.getFloat("OffsetX") : 0.0F;
        float offsetY = tag.contains("OffsetY") ? tag.getFloat("OffsetY") : 0.0F;
        float offsetZ = tag.contains("OffsetZ") ? tag.getFloat("OffsetZ") : 0.0F;
        long startGameTime = tag.contains("StartGameTime") ? tag.getLong("StartGameTime") : 0L;
        int savedColor = tag.getInt("Color");
        boolean hasScaleAnimation = tag.getBoolean("HasScaleAnimation");
        float scaleFrom = tag.contains("ScaleFrom") ? tag.getFloat("ScaleFrom") : savedScale;
        float scaleTo = tag.contains("ScaleTo") ? tag.getFloat("ScaleTo") : savedScale;
        float scaleAlphaFrom = tag.contains("ScaleAlphaFrom") ? tag.getFloat("ScaleAlphaFrom") : 1.0F;
        float scaleAlphaTo = tag.contains("ScaleAlphaTo") ? tag.getFloat("ScaleAlphaTo") : 1.0F;
        int scalePeriod = Math.max(1, tag.contains("ScalePeriod") ? tag.getInt("ScalePeriod") : 1);
        int scalePlayCount = tag.contains("ScalePlayCount") ? tag.getInt("ScalePlayCount") : -1;
        AnimationEndAction scaleEndAction = tag.contains("ScaleEndAction")
                ? parseEndAction(tag.getString("ScaleEndAction"))
                : AnimationEndAction.END;
        boolean hasColorAnimation = tag.getBoolean("HasColorAnimation");
        int colorFrom = tag.contains("ColorFrom") ? tag.getInt("ColorFrom") : savedColor;
        int colorTo = tag.contains("ColorTo") ? tag.getInt("ColorTo") : savedColor;
        int colorPeriod = Math.max(1, tag.contains("ColorPeriod") ? tag.getInt("ColorPeriod") : 1);
        int colorPlayCount = tag.contains("ColorPlayCount") ? tag.getInt("ColorPlayCount") : -1;
        AnimationEndAction colorEndAction = tag.contains("ColorEndAction")
                ? parseEndAction(tag.getString("ColorEndAction"))
                : AnimationEndAction.END;
        return new PaintData(
                savedColor,
                savedScale,
                offsetX,
                offsetY,
                offsetZ,
                startGameTime,
                hasScaleAnimation,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                scalePlayCount,
                scaleEndAction,
                hasColorAnimation,
                colorFrom,
                colorTo,
                colorPeriod,
                colorPlayCount,
                colorEndAction
        );
    }

    public PaintRenderState resolve(float currentGameTime) {
        float animationTime = currentGameTime - this.startGameTime;
        float scaleProgress = this.hasScaleAnimation
                ? resolveProgress(animationTime, this.scalePeriod, this.scalePlayCount, this.scaleEndAction)
                : 0.0F;
        float colorProgress = this.hasColorAnimation
                ? resolveProgress(animationTime, this.colorPeriod, this.colorPlayCount, this.colorEndAction)
                : 0.0F;

        float resolvedScale = this.hasScaleAnimation
                ? lerp(scaleProgress, this.scaleFrom, this.scaleTo)
                : this.scale;
        int resolvedColor = this.hasColorAnimation
                ? lerpArgb(colorProgress, this.colorFromArgb, this.colorToArgb)
                : this.argb;
        float alphaFactor = this.hasScaleAnimation
                ? lerp(scaleProgress, this.scaleAlphaFrom, this.scaleAlphaTo)
                : 1.0F;

        float red = ((resolvedColor >> 16) & 0xFF) / 255.0F;
        float green = ((resolvedColor >> 8) & 0xFF) / 255.0F;
        float blue = (resolvedColor & 0xFF) / 255.0F;
        float alpha = (((resolvedColor >>> 24) & 0xFF) / 255.0F) * alphaFactor;
        return new PaintRenderState(red, green, blue, clamp01(alpha), resolvedScale, this.offsetX, this.offsetY, this.offsetZ);
    }

    public boolean shouldAutoRemove(long currentGameTime) {
        float animationTime = currentGameTime - this.startGameTime;
        return shouldRemoveTrack(animationTime, this.hasScaleAnimation, this.scalePeriod, this.scalePlayCount, this.scaleEndAction)
                || shouldRemoveTrack(animationTime, this.hasColorAnimation, this.colorPeriod, this.colorPlayCount, this.colorEndAction);
    }

    private static boolean shouldRemoveTrack(float animationTime, boolean hasAnimation, int period, int playCount, AnimationEndAction endAction) {
        if (!hasAnimation || endAction != AnimationEndAction.REMOVE || playCount < 0) {
            return false;
        }
        return animationTime >= totalDuration(period, playCount);
    }

    private static float resolveProgress(float animationTime, int period, int playCount, AnimationEndAction endAction) {
        if (playCount < 0) {
            return cycleProgress(animationTime, period);
        }

        float duration = totalDuration(period, playCount);
        if (animationTime >= duration) {
            return switch (endAction) {
                case START -> 0.0F;
                case END, REMOVE -> 1.0F;
            };
        }

        return cycleProgress(animationTime, period);
    }

    private static float totalDuration(int period, int playCount) {
        return Math.max(1, period) * Math.max(0, playCount);
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

    private static AnimationEndAction parseEndAction(String name) {
        try {
            return AnimationEndAction.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return AnimationEndAction.END;
        }
    }
}
