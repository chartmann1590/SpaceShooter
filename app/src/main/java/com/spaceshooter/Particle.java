package com.spaceshooter;

import android.graphics.*;

public class Particle {
    float x, y;
    float vx, vy;
    int color;
    float size = 4;
    float life = 500;
    float maxLife = 500;

    Particle(float x, float y, float vx, float vy, int color, float life) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.color = color; this.life = life; this.maxLife = life;
    }
}
