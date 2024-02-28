package org.figuramc.figura.animation;

import net.minecraft.client.Minecraft;

public class TimeController {

    private long lastTime, time, pauseTime;

    public void init() {
        pauseTime = 0L;
        lastTime = time = Minecraft.getSystemTime();
    }

    public void tick() {
        lastTime = time;
        time = Minecraft.getSystemTime();
    }

    public void reset() {
        lastTime = time = pauseTime = 0L;
    }

    public void pause() {
        lastTime = time;
        pauseTime = Minecraft.getSystemTime();
    }

    public void resume() {
        long diff = Minecraft.getSystemTime() - pauseTime;
        lastTime += diff;
        time += diff;
    }

    public float getDiff() {
        return Minecraft.getMinecraft().isGamePaused() ? 0 : (time - lastTime) / 1000f;
    }
}
