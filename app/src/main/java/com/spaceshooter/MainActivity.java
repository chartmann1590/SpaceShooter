package com.spaceshooter;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameSurfaceView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        gameView = new GameSurfaceView(this);
        setContentView(gameView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) gameView.stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
