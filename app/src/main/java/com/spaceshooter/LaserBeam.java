package com.spaceshooter;

public class LaserBeam {
    float x;
    float y;
    float speed = 30;
    
    LaserBeam(float x, float y, float targetY) {
        this.x = x;
        this.y = y;
    }
    
    void update(long dt) {
        y += speed * dt * 0.1f;
    }
}