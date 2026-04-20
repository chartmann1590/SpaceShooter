package com.spaceshooter;

import android.graphics.*;

public class PowerUp {
    float x, y;
    int type;
    float rotation = 0;
    int color;
    String[] names = {"Health", "Multi-Shot", "Shield", "Bomb", "Rapid Fire", "Laser", "Nuke"};

    PowerUp(float x, float y, int type) {
        this.x = x; this.y = y; this.type = type;
        switch (type) {
            case 0: this.color = Color.GREEN; break;
            case 1: this.color = Color.CYAN; break;
            case 2: this.color = Color.BLUE; break;
            case 3: this.color = Color.YELLOW; break;
            case 4: this.color = Color.MAGENTA; break;
            case 5: this.color = Color.rgb(255, 100, 255); break;
            case 6: this.color = Color.rgb(255, 150, 50); break;
            default: this.color = Color.WHITE;
        }
    }
    
    int getColor() { return color; }
}
