package com.spaceshooter;

public class Star {
    float x, y;
    float size;
    float speed;
    float brightness = 1.0f;

    Star(float x, float y, float size, float speed) {
        this.x = x; this.y = y; this.size = size; this.speed = speed;
        this.brightness = 0.5f + (float)Math.random() * 0.5f;
    }
}
