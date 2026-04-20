package com.spaceshooter;

public class Bullet {
    float x, y;
    float vx, vy;
    boolean isPlayer;

    Bullet(float x, float y, float vx, float vy, boolean isPlayer) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.isPlayer = isPlayer;
    }
}
