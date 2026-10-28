package com.flappybird.recreation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.flappybird.recreation.R;

public class GearslogoActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable continueToGame = this::openGame;
    private long remainingMillis = 2800;
    private long resumedAt;
    private boolean resumed, opened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelFont.init(this);
        if (savedInstanceState != null) {
            remainingMillis = savedInstanceState.getLong("intro_remaining", 2800);
        }
        
        setContentView(R.layout.gearslogo);
        findViewById(R.id.opening_screen).setOnClickListener(view -> openGame());
    }

    @Override protected void onResume() {
        super.onResume();

        WindowInsetsControllerCompat insetsController =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
        insetsController.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        resumed = true;
        resumedAt = SystemClock.uptimeMillis();
        handler.postDelayed(continueToGame, remainingMillis);
    }

    @Override protected void onPause() {
        remainingMillis = Math.max(0, remainingMillis - (SystemClock.uptimeMillis() - resumedAt));
        resumed = false;
        handler.removeCallbacks(continueToGame);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        long remaining = resumed ? Math.max(0, remainingMillis - (SystemClock.uptimeMillis() - resumedAt)) : remainingMillis;
        state.putLong("intro_remaining", remaining);
        super.onSaveInstanceState(state);
    }

    private void openGame() {
        if (!resumed || opened || isFinishing()) return;
        opened = true;
        handler.removeCallbacks(continueToGame);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(continueToGame);
        super.onDestroy();
    }
}
