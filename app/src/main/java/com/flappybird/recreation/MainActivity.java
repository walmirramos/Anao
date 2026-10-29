package com.flappybird.recreation;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Choreographer;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.widget.FrameLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.flappybird.recreation.SettingsActivity.*;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private LoadingView loadingView;
    private final ExecutorService assetLoader = Executors.newSingleThreadExecutor();
    private boolean foreground;

    boolean isForeground() { return foreground; }

    private void startLoading(FrameLayout container) {
        if (loadingView != null) container.removeView(loadingView);
        loadingView = new LoadingView(this);
        container.addView(loadingView);
        assetLoader.submit(() -> {
            try {
                GameAssets assets = new GameAssets(getResources());
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) { assets.recycle(); return; }
                    gameView = new GameView(this, assets, () -> {
                        container.removeView(loadingView);
                        loadingView = null;
                        if (foreground) gameView.resume(); else gameView.pause();
                    });
                    container.addView(gameView, 0);
                    ViewCompat.requestApplyInsets(gameView);
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || loadingView == null) return;
                    loadingView.showError();
                    loadingView.setOnClickListener(view -> startLoading(container));
                });
            }
        });
    }

    @Override protected void onDestroy() {
        assetLoader.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        PixelFont.init(this);
        
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.main);
        FrameLayout gameContainer = findViewById(R.id.game_container);
        startLoading(gameContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        if (gameView != null) gameView.resume();
    }

    @Override
    protected void onPause() {
        foreground = false;
        super.onPause();
        if (gameView != null) gameView.pause();
    }
}

class GameView extends View implements Choreographer.FrameCallback {

    // --- MEDAL ADJUSTMENTS ---
    private final float PLATINUM_OFFSET_X = -1.0f;
    private final float PLATINUM_OFFSET_Y = 1.0f;
    private final float GOLD_OFFSET_X = -1.0f;
    private final float GOLD_OFFSET_Y = 1.0f;

    private enum GameState { HOME, TRANSITION_TO_WAITING, WAITING, PLAYING, GAME_OVER, PANEL_SLIDING }
    private GameState gameState = GameState.HOME;
    private Choreographer choreographer;
    private volatile boolean isRunning = false;
    private boolean isReady = false;
    private long lastFrameTimeNanos = 0;
    private int screenWidth, screenHeight;
    private float scale;
    private int systemBarTop = -1, systemBarBottom = -1;

    // Graphics & Object Pooling
    private Paint pixelPaint, birdPaint;
    private Matrix birdMatrix = new Matrix();
    private Matrix gradientMatrix = new Matrix();

    private Bitmap unscaledBgDay, unscaledBgNight, unscaledLand;
    private Bitmap unscaledPipeGreen;
    private Bitmap[][] unscaledAllBirdBitmaps = new Bitmap[3][3];
    private Bitmap unscaledTextReady, unscaledTextGameOver, unscaledScorePanel, unscaledButtonPlay, unscaledButtonScore, unscaledTitle, unscaledButtonSettings;
    private Bitmap[] unscaledMedalsBitmaps = new Bitmap[4];
    private Bitmap[] unscaledNumberBitmaps = new Bitmap[10];
    
    private Bitmap bgDayBitmap, bgNightBitmap, groundBitmap;
    private Bitmap pipeUpBitmap, pipeDownBitmap;
    private Bitmap[] wallBottoms = new Bitmap[WorldArt.WALL_STYLES], wallTops = new Bitmap[WorldArt.WALL_STYLES];
    private final GameAssets preparedAssets;
    private final Runnable onReady;
    private int sceneVariant;
    private final GroundImpactEffect groundImpact = new GroundImpactEffect();
    private FallingGunfire fallingGunfire;
    private final PowerUpState powerUps = new PowerUpState();
    private final Bitmap bananaBitmap = BonusArt.banana();
    private final Bitmap phoneBitmap = BonusArt.phone();
    private final Bitmap bazookaBitmap = BonusArt.bazooka();
    private final BonusArt bonusArt = new BonusArt();
    private final FactoryTraffic factoryTraffic=new FactoryTraffic(getResources());
    private final FactoryCelebration celebrations=new FactoryCelebration();
    private final Bitmap workbookBitmap=BonusArt.workbook();
    private Pipe workbookPipe;
    private int workbookGeneration;
    private boolean workbookEarned;
    private float workbookX,workbookY;
    private void updateWorkbook() {
        if(workbookEarned || score<20) return;
        if(workbookPipe!=null && workbookPipe.status.generation()!=workbookGeneration) workbookPipe=null;
        if(workbookPipe==null && !powerUps.visible()) {
            float nearest=Float.MAX_VALUE;
            for(Pipe pipe:pipes) {
                float d=(pipe.x+pipe.width/2f-birdX)*(settingReversePipesEnabled?-1:1);
                if(!pipe.status.isDestroyed() && d>50*scale && d<nearest) {workbookPipe=pipe;nearest=d;}
            }
            if(workbookPipe!=null)workbookGeneration=workbookPipe.status.generation();
        }
        if(workbookPipe==null)return;
        workbookX=workbookPipe.x+workbookPipe.width/2f;
        workbookY=(topWallY(workbookPipe)+workbookPipe.height+bottomWallY(workbookPipe))/2f;
        if(birdRect.right>workbookX-12*scale && birdRect.left<workbookX+12*scale &&
                birdRect.bottom>workbookY-14*scale && birdRect.top<workbookY+14*scale) {
            workbookEarned=true;workbookPipe=null;score+=10;
            celebrations.splash(birdX,birdY,scale,10);playSound(soundPoint);
        } else if((workbookX-birdX)*(settingReversePipesEnabled?-1:1)<-40*scale)workbookPipe=null;
    }
    private final WallBlastEffects wallBlasts = new WallBlastEffects();
    private final SkyTraffic skyTraffic = new SkyTraffic();
    private Pipe bonusPipe, rocketTarget;
    private float bonusX, bonusY, rocketX, rocketY, rocketCooldown;
    private int rocketGeneration, bonusGeneration;
    private int soundAura, soundAuraActivate, soundBazooka, auraStream;

    private void stopAuraSound() {
        if (auraStream != 0 && soundPool != null) soundPool.stop(auraStream);
        auraStream = 0;
    }

    private void syncAuraSound() {
        if (!isRunning || !settingSoundEnabled || !powerUps.active(PowerUpState.Kind.SAYAJIN) || gameState != GameState.PLAYING) {
            stopAuraSound();
        } else if (auraStream == 0 && soundPool != null) {
            auraStream = soundPool.play(soundAura, .28f, .28f, 1, -1, 1f);
        }
    }

    private void drawMovingWall(Canvas canvas,Bitmap texture,Pipe pipe,boolean upper) {
        int saved=canvas.save();canvas.clipRect(pipeDestRect);
        float dy=score>=30?pipe.textureOffset():0;
        canvas.translate(0,dy);
        canvas.drawBitmap(texture,null,pipeDestRect,pipe.pipePaint);
        canvas.translate(0,dy>0?-pipe.height:pipe.height);
        canvas.drawBitmap(texture,null,pipeDestRect,pipe.pipePaint);
        canvas.restoreToCount(saved);
        // Fixed steel rim makes the passage boundary explicit and stationary.
        float edge=upper?pipeDestRect.bottom:pipeDestRect.top;
        float thickness=3*scale;
        pixelPaint.setColor(0xffafc5cc);
        canvas.drawRect(pipeDestRect.left,upper?edge-thickness:edge,pipeDestRect.right,upper?edge:edge+thickness,pixelPaint);
        pixelPaint.setColor(Color.WHITE);
    }
    private float topWallY(Pipe pipe) { return pipe.status.top(pipe.getTopPipeY()); }
    private float bottomWallY(Pipe pipe) { return pipe.status.bottom(pipe.getBottomPipeY(pipeGap)); }

    private void updateWallOpenings(float seconds) {
        float extra=Math.min(pipeGap*.5f,64*scale);
        for(Pipe pipe:pipes) {
            float width=pipe.width*settingPipeWidth;
            float left=pipe.x-(width-pipe.width)/2f;
            boolean crossing=birdRect.right+8*scale>left && birdRect.left-8*scale<left+width;
            pipe.status.updateOpening(powerUps.active(PowerUpState.Kind.CELULAR),extra,seconds,crossing);
        }
    }

    private void updatePowerUps() {
        updateWorkbook();
        if (powerUps.shouldSpawn(score)) {
            Pipe target = null;
            float nearest = Float.MAX_VALUE;
            for (Pipe pipe : pipes) {
                float center = pipe.x + pipe.width / 2f;
                float distance = (center - birdX) * (settingReversePipesEnabled ? -1 : 1);
                if (pipe!=workbookPipe && !pipe.status.isDestroyed() && distance > 40 * scale && distance < nearest) { nearest = distance; target = pipe; }
            }
            if (target != null) {
                bonusPipe = target;
                bonusGeneration=target.status.generation();
                powerUps.spawned(score, 4 + random.nextInt(4));
            }
        }
        if(bonusPipe!=null && bonusPipe.status.generation()!=bonusGeneration) {
            powerUps.miss(); bonusPipe=null;
        }
        if (powerUps.visible() && bonusPipe != null) {
            bonusX = bonusPipe.x + bonusPipe.width / 2f;
            bonusY = (topWallY(bonusPipe)+bonusPipe.height+bottomWallY(bonusPipe))/2f;
            float radius = 10 * scale;
            if (birdRect.right > bonusX - radius && birdRect.left < bonusX + radius &&
                    birdRect.bottom > bonusY - radius && birdRect.top < bonusY + radius) {
                if (powerUps.collect()) {
                    playSound(powerUps.active(PowerUpState.Kind.SAYAJIN) ? soundAuraActivate : soundSwooshing);
                    updateWallOpenings(0); syncAuraSound();
                    rocketCooldown=0;
                }
                bonusPipe = null;
            } else if ((bonusX - birdX) * (settingReversePipesEnabled ? -1 : 1) < -30 * scale) {
                powerUps.miss(); bonusPipe = null;
            }
        }
    }

    private void destroyWall(Pipe pipe) {
        if(!pipe.status.destroy()) return;
        float width=pipe.width*settingPipeWidth;
        wallBlasts.explode(pipe.x-(width-pipe.width)/2f,topWallY(pipe)+pipe.height,
                bottomWallY(pipe),width,screenHeight-systemBarBottom-groundHeight,
                pipe.wallStyle,settingReversePipesEnabled);
        playSound(soundBazooka);
        triggerShake(1.8f,.16f);
    }

    private void updateBazooka(float seconds) {
        if(!powerUps.active(PowerUpState.Kind.BAZUCA)) {
            rocketTarget=null; rocketCooldown=0; return;
        }
        float direction=settingReversePipesEnabled ? -1 : 1;
        rocketCooldown=Math.max(0,rocketCooldown-seconds);
        if(rocketTarget!=null && (rocketTarget.status.isDestroyed() ||
                rocketTarget.status.generation()!=rocketGeneration)) {
            rocketTarget=null;powerUps.resolveShot();
        }
        if(rocketTarget==null && rocketCooldown<=0 && powerUps.rounds()>0) {
            float nearest=Float.MAX_VALUE;
            for(Pipe pipe:pipes) {
                float width=pipe.width*settingPipeWidth;
                float center=pipe.x+pipe.width/2f;
                float left=center-width/2f;
                float distance=(center-birdX)*direction;
                if(!pipe.status.isDestroyed() && BazookaTargeting.ahead(birdRect.left,birdRect.right,
                        left,left+width,settingReversePipesEnabled) && distance<screenWidth && distance<nearest) {
                    rocketTarget=pipe; nearest=distance;
                }
            }
            if(rocketTarget!=null && powerUps.fireShot()) {
                rocketGeneration=rocketTarget.status.generation();
                rocketX=birdX+direction*25*scale*settingBirdSize;
                rocketY=birdY+11*scale*settingBirdSize;
                rocketCooldown=.5f;
            }
        }
        if(rocketTarget==null) return;
        float tx=rocketTarget.x+rocketTarget.width/2f;
        float upper=topWallY(rocketTarget)+rocketTarget.height;
        float lower=bottomWallY(rocketTarget);
        float ty=Math.abs(upper-rocketY)<Math.abs(lower-rocketY) ? upper-4*scale : lower+4*scale;
        float dx=tx-rocketX,dy=ty-rocketY;
        float distance=(float)Math.sqrt(dx*dx+dy*dy);
        float step=(Math.abs(PIPE_SPEED_PER_SEC)+480*scale)*seconds;
        float visualWidth=rocketTarget.width*settingPipeWidth;
        float wallLeft=rocketTarget.x-(visualWidth-rocketTarget.width)/2f;
        boolean close=BazookaTargeting.imminent(birdRect.left,birdRect.right,wallLeft,wallLeft+visualWidth,settingReversePipesEnabled,8*scale);
        if(distance<=step+4*scale || close) {
            destroyWall(rocketTarget); rocketTarget=null;powerUps.resolveShot();
        } else {
            rocketX+=dx/distance*step; rocketY+=dy/distance*step;
        }
    }
    private Bitmap backgroundBitmap, currentPipeTopBitmap, currentPipeBottomBitmap;
    private Bitmap[][] scaledAllBirdBitmaps = new Bitmap[3][3];
    private Bitmap[] birdBitmaps = new Bitmap[3];
    private Bitmap getReadyBitmap, gameOverBitmap, scorePanelBitmap, playButtonBitmap, scoreButtonBitmap, titleBitmap, settingsButtonBitmap;
    private Bitmap[] medalBitmaps = new Bitmap[4];
    private Bitmap currentMedalBitmap;
    private Bitmap[] numberBitmaps = new Bitmap[10];
    private Bitmap[] smallNumberBitmaps = new Bitmap[10];

    // Physics variables
    private int birdFrame = 0;
    private float birdX, birdY, birdVelocityY, birdRotation;
    private long lastFlapTimeMillis = 0;
    private Rect birdRect = new Rect();
    private Rect playButtonRect = new Rect(), scoreButtonRect = new Rect(), settingsButtonRect = new Rect();
    private int currentBirdColor;

    private List<Pipe> pipes;
    private int pipeSpacing, pipeGap;

    private float groundX = 0, backgroundX = 0;
    private int groundHeight;

    private int score = 0, highScore = 0;
    private SharedPreferences prefs;

    // Audio & Haptics
    private SoundPool soundPool;
    private RetroAudio retroAudio;
    private final ColorMatrixColorFilter horrorBackground=new ColorMatrixColorFilter(new float[]{
        .38f,.06f,0,0,9, 0,.24f,.03f,0,2, .04f,0,.38f,0,8, 0,0,0,1,0});
    private int soundWing, soundPoint, soundHit, soundDie, soundSwooshing, soundThunder, soundShot;
    private ExecutorService soundExecutor;
    private MediaPlayer rainSoundPlayer, stormSoundPlayer;
    private Vibrator vibrator;

    // Game Logic Triggers
    private boolean hasTier1Triggered = false, hasTier2Triggered = false;
    
    // Physics Constants
    private float BASE_GRAVITY_PER_SEC, BASE_FLAP_VELOCITY_PER_SEC, BASE_PIPE_SPEED_PER_SEC, JETPACK_THRUST_PER_SEC;
    private float BASE_ROTATION_DELAY_THRESHOLD_PER_SEC, BASE_ROTATION_DOWN_SPEED_PER_SEC;
    private float GRAVITY_PER_SEC, FLAP_VELOCITY_PER_SEC, PIPE_SPEED_PER_SEC;
    private float ROTATION_DELAY_THRESHOLD_PER_SEC, ROTATION_DOWN_SPEED_PER_SEC;
    private float TRANSITION_FADE_SPEED_PER_SEC;
    
    private Random random = new Random();
    
    // UI Constants
    private final float UI_MARGIN_HORIZONTAL_PERCENT = 0.025f;
    private final float HOME_BUTTON_GAP_PERCENT = 0.05f;
    private final float SETTINGS_BUTTON_SCALE_MULTIPLIER = 0.75f;
    private final float PIPE_GAP_BIRD_HEIGHT_MULTIPLIER = 2.2f;
    private final float SCORE_PANEL_NUMBER_SCALE_MULTIPLIER = 0.6f;
    private final int SCORE_PANEL_CURRENT_SCORE_Y_OFFSET = 55;
    private final int SCORE_PANEL_HIGH_SCORE_Y_OFFSET = 98;
    private final int SCORE_PANEL_MEDAL_X_OFFSET = 32;
    private final int SCORE_PANEL_MEDAL_Y_OFFSET = 43;
    private static final String PREF_KEY_WARNING_SHOWN = "hasSeenAspectRatioWarning";
    private final float BIRD_HITBOX_PADDING_X = 0.10f;
    private final float BIRD_HITBOX_PADDING_Y = 0.10f;
    private long FLAP_ANIMATION_TIMEOUT_MS = 480;

    // Animation & State Variables
    private float gameOverElementsY;
    private int gameOverElementsTargetY;
    private Rect pressedButtonRect = null;
    
    // Settings Button Animation
    private float settingsButtonScale = 1.0f;
    private float settingsButtonTargetScale = 1.0f;
    private float pressOffsetY = 0;
    
    private boolean pipesAreMoving = false;
    private boolean pipesAreStopping = false;
    private float pipeAnimationCounter = 0f;
    private float pipeAnimationSpeed = 0.03f;
    private float maxPipeMoveRange = 0f;
    private float currentPipeMoveRange = 0f;
    private long pipeMoveStartTime = 0;
    private final long PIPE_MOVE_DURATION_MS = 30_000;
    private final float PIPE_SPAWN_DELAY_SECONDS = 1.5f;
    private final long PANEL_SLIDE_DELAY_MS = 100;
    private final float PANEL_SLIDE_EASING_BASE = 0.95f;
    private final long GAMEOVER_ICON_SLIDE_DELAY_MS = 25;
    private final float GAMEOVER_ICON_EASING_BASE = 0.92f;
    
    private long birdHitGroundTime = 0;
    private float gameOverSettingsIconY = -1;
    private float gameOverSettingsIconTargetY;
    private boolean isGameOverIconAnimationDone = false;
    private long panelFinishedSlidingTime = 0;

    private Paint transitionPaint;
    private float transitionAlpha = 0;
    private boolean isFadingOut = false;
    private boolean isRestarting = false;

    // Flash Effect Logic
    private Paint flashPaint;
    private float flashAlpha = 0;

    private Paint darkenPaint;
    private float uiScaleCorrection = 1.0f;

    // Settings Cache
    private int settingBirdColor, settingBackground, settingPipeColor, settingGameOverOpacity;
    private int settingPipeMoveTier1, settingPipeMoveTier2, settingScoreMultiplier;
    private int settingWeatherEffect, settingGoldenPipeChance, settingGoldenPipeBonus;
    private float settingGameSpeed, settingGravity, settingJumpStrength, settingPipeGap;
    private float settingBirdHangDelay, settingPipeSpacing, settingBirdHitbox, settingPipeVariation;
    private float settingPipeSpeedVariation, settingBirdSize, settingPipeWidth;
    private float settingBgScrollSpeed, settingGroundScrollSpeed, settingDrunkModeStrength;
    private float settingScreenShakeStrength, settingMotionBlurStrength;
    private boolean settingSoundEnabled, settingNoClipEnabled, settingWingAnimationEnabled;
    private final SecretGestureTouch secretGestureTouch = new SecretGestureTouch();
    private boolean waitingTouch;
    private float secretTouchX, secretTouchY, secretTouchTravel;
    private long secretNoticeUntil;
    private final Paint secretStatusText = WorldArt.textPaint(12), secretStatusBackground = new Paint();
    private boolean settingMovingPipesEnabled, settingHideSettingsIcon, settingRainbowBirdEnabled;
    private boolean settingUpsideDownEnabled, settingReversePipesEnabled, settingHapticFeedbackEnabled;
    private boolean settingBirdTrailEnabled, settingGhostModeEnabled, settingRandomPipeColorsEnabled;
    private boolean settingInfiniteFlapEnabled, settingJetpackModeEnabled, settingDrunkModeEnabled;
    private boolean settingDynamicDayNightEnabled, settingScreenShakeEnabled, settingGoldenPipeEnabled;
    private boolean settingMotionBlurEnabled;
    
    // NEW SETTINGS & KEYS
    public static final String PREF_LIGHTNING_SCORES = "pref_lightning_scores";
    public static final String PREF_MEGA_STRIKE_SCORE = "pref_mega_strike_score";
    
    private Set<Integer> settingLightningScores = new HashSet<>();
    private int settingMegaStrikeScore = 350;

    private float rainbowHue = 0f;
    private Deque<TrailParticle> birdTrail;
    private Paint trailPaint;
    private static final int MAX_TRAIL_PARTICLES = 15;

    private int displayedScore = 0;
    private long lastScoreTickTime = 0;
    private final long SCORE_ANIMATION_INTERVAL_MS_FAST = 8;
    private final long SCORE_ANIMATION_INTERVAL_MS_SLOW = 45;
    
    // Allocations moved out of draw
    private RectF groundDestRect = new RectF();
    private RectF playButtonDestRect = new RectF();
    private RectF scoreButtonDestRect = new RectF();
    private RectF gameOverDestRect = new RectF();
    private RectF panelDestRect = new RectF();
    private RectF medalDestRect = new RectF();
    private RectF restartBtnDestRect = new RectF();
    private RectF scoreDigitDestRect = new RectF();
    private RectF centeredBitmapDestRect = new RectF();
    private RectF settingsButtonDestRect = new RectF();
    private RectF pipeDestRect = new RectF();

    private static final float TARGET_FPS = 120.0f;
    private boolean isScreenPressed = false;
    private float baseBirdX;
    private float drunkModePhase = 0f;
    
    // Weather System
    private WeatherSystem weatherSystem;
    private Paint nightBgPaint, goldenPipePaint, motionBlurPaint;
    private Shader goldenPipeShader;
    private float dayNightCycleProgress = 0f;
    private float shakeTimer = 0f;
    private float currentShakeMagnitude = 0f;
    private float goldenPipeHue = 0f;

    // --- BURNT BIRD & CINEMATIC VARIABLES ---
    private boolean isBirdBurnt = false;
    private float burntTimer = 0f;
    
    // Lightning Logic Fixed
    private int lastLightningScore = -1; // To ensure we only strike once per score
    private boolean hasTriggeredMegaStrike = false;
    
    private float pipeElectrificationTimer = 0f;
    
    private List<SmokeParticle> smokeParticles = new ArrayList<>();
    private List<SparkParticle> sparkParticles = new ArrayList<>();
    private Paint sparkPaint;
    private ColorFilter burntColorFilter;
    private Paint electricityPipePaint;

    // Cinematic Engine Variables
    private float timeDilation = 1.0f; 
    private float cameraZoom = 1.0f;
    private float cinematicTimer = 0f;
    private int cinematicPhase = 0;
    
    // --- MEDAL SHINE VARIABLES ---
    private Bitmap sparkleBitmap;
    private float sparkleTimer = 0f;
    private float sparkleXOffset = 0f;
    private float sparkleYOffset = 0f;
    private float sparkleScaleCurrent = 0f;
    private boolean isSparkleAnimating = false;
    private final float SPARKLE_DURATION = 0.6f; 
    private final float SPARKLE_INTERVAL = 2.0f; 
    
    public GameView(Context context, GameAssets assets, Runnable ready) {
        super(context);
        preparedAssets = assets;
        onReady = ready;
        init(context);
    }

    private void init(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);

        loadSettings();

        pixelPaint = new Paint();
        pixelPaint.setFilterBitmap(false);
        pixelPaint.setAntiAlias(false);

        birdPaint = new Paint();
        birdPaint.setFilterBitmap(false);
        birdPaint.setAntiAlias(false);

        ViewCompat.setOnApplyWindowInsetsListener(this, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            systemBarTop = insets.top;
            systemBarBottom = insets.bottom;
            tryInitializeGame();
            return WindowInsetsCompat.CONSUMED;
        });
        choreographer = Choreographer.getInstance();
        extractBitmapsFromAtlas();
        loadSounds();
        retroAudio = new RetroAudio(context);
        soundExecutor = Executors.newSingleThreadExecutor();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        transitionPaint = new Paint();
        transitionPaint.setColor(Color.BLACK);
        transitionPaint.setAlpha(0);

        flashPaint = new Paint();
        flashPaint.setColor(Color.WHITE);
        flashPaint.setAlpha(0);

        darkenPaint = new Paint();
        darkenPaint.setColor(Color.BLACK);
        darkenPaint.setAlpha(0);

        nightBgPaint = new Paint();
        nightBgPaint.setFilterBitmap(false);
        nightBgPaint.setAntiAlias(false);

        goldenPipePaint = new Paint();
        goldenPipePaint.setFilterBitmap(false);
        goldenPipePaint.setAntiAlias(false);
        goldenPipePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        // Pre-create shader for golden pipe
        int[] colors = {Color.argb(0, 255, 215, 0), Color.argb(150, 255, 255, 100), Color.argb(0, 255, 215, 0)};
        float[] positions = {0, 0.5f, 1};
        goldenPipeShader = new LinearGradient(0, 0, 100, 0, colors, positions, Shader.TileMode.MIRROR);
        goldenPipePaint.setShader(goldenPipeShader);

        motionBlurPaint = new Paint();
        motionBlurPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        birdTrail = new ArrayDeque<>(MAX_TRAIL_PARTICLES);
        trailPaint = new Paint();
        
        // --- SMOKE & SPARKS INIT ---
        sparkPaint = new Paint();
        sparkPaint.setStrokeWidth(4);
        sparkPaint.setStyle(Paint.Style.STROKE);
        sparkPaint.setStrokeCap(Paint.Cap.ROUND);
        sparkPaint.setAntiAlias(true);
        sparkPaint.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));
        
        electricityPipePaint = new Paint();
        electricityPipePaint.setColor(Color.CYAN);
        electricityPipePaint.setStyle(Paint.Style.STROKE);
        electricityPipePaint.setStrokeWidth(3);
        electricityPipePaint.setAntiAlias(true);
        electricityPipePaint.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));
        
        burntColorFilter = new PorterDuffColorFilter(Color.rgb(40, 40, 40), PorterDuff.Mode.MULTIPLY);

        weatherSystem = new WeatherSystem();
        
        sparkleBitmap = createSparkleBitmap();
    }
    
    private Bitmap createSparkleBitmap() {
        int size = 64; 
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setAntiAlias(true);
        p.setMaskFilter(new BlurMaskFilter(size / 4f, BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(size / 2f, size / 2f, size / 6f, p);
        p.setMaskFilter(null);
        float cx = size / 2f;
        float cy = size / 2f;
        float halfLen = size / 2.5f;
        float thickness = size / 12f;
        c.drawRect(cx - halfLen, cy - thickness/2, cx + halfLen, cy + thickness/2, p);
        c.drawRect(cx - thickness/2, cy - halfLen, cx + thickness/2, cy + halfLen, p);
        return bmp;
    }

    private void loadSettings() {
        if (prefs == null) return;
        settingBirdColor = prefs.getInt(PREF_BIRD_COLOR, DEFAULT_BIRD_COLOR);
        settingBackground = prefs.getInt(PREF_BACKGROUND, DEFAULT_BACKGROUND);
        settingSoundEnabled = prefs.getBoolean(PREF_SOUND_ENABLED, DEFAULT_SOUND_ENABLED);
        settingWingAnimationEnabled = prefs.getBoolean(PREF_WING_ANIMATION_ENABLED, DEFAULT_WING_ANIMATION_ENABLED);
        settingPipeColor = prefs.getInt(PREF_PIPE_COLOR, DEFAULT_PIPE_COLOR);
        settingHideSettingsIcon = prefs.getBoolean(PREF_HIDE_SETTINGS_ICON, DEFAULT_HIDE_SETTINGS_ICON);
        settingGameOverOpacity = prefs.getInt(PREF_GAMEOVER_OPACITY, DEFAULT_GAMEOVER_OPACITY);
        settingGameSpeed = prefs.getFloat(PREF_GAME_SPEED, DEFAULT_GAME_SPEED);
        settingGravity = prefs.getFloat(PREF_GRAVITY, DEFAULT_GRAVITY);
        settingJumpStrength = prefs.getFloat(PREF_JUMP_STRENGTH, DEFAULT_JUMP_STRENGTH);
        settingPipeGap = prefs.getFloat(PREF_PIPE_GAP, DEFAULT_PIPE_GAP);
        settingBirdHangDelay = prefs.getFloat(PREF_BIRD_HANG_DELAY, DEFAULT_BIRD_HANG_DELAY);
        settingPipeSpacing = prefs.getFloat(PREF_PIPE_SPACING, DEFAULT_PIPE_SPACING);
        settingBirdHitbox = prefs.getFloat(PREF_BIRD_HITBOX, DEFAULT_BIRD_HITBOX);
        settingMovingPipesEnabled = prefs.getBoolean(PREF_MOVING_PIPES_ENABLED, DEFAULT_MOVING_PIPES_ENABLED);
        settingPipeMoveTier1 = prefs.getInt(PREF_PIPE_MOVE_TIER_1_SCORE, DEFAULT_PIPE_MOVE_TIER_1_SCORE);
        settingPipeMoveTier2 = prefs.getInt(PREF_PIPE_MOVE_TIER_2_SCORE, DEFAULT_PIPE_MOVE_TIER_2_SCORE);
        settingNoClipEnabled = SecretInvincibility.isEnabled();
        settingRainbowBirdEnabled = prefs.getBoolean(PREF_RAINBOW_BIRD_ENABLED, DEFAULT_RAINBOW_BIRD_ENABLED);
        settingUpsideDownEnabled = prefs.getBoolean(PREF_UPSIDE_DOWN_ENABLED, DEFAULT_UPSIDE_DOWN_ENABLED);
        settingReversePipesEnabled = prefs.getBoolean(PREF_REVERSE_PIPES_ENABLED, DEFAULT_REVERSE_PIPES_ENABLED);
        settingPipeVariation = prefs.getFloat(PREF_PIPE_VARIATION, DEFAULT_PIPE_VARIATION);
        settingScoreMultiplier = prefs.getInt(PREF_SCORE_MULTIPLIER, DEFAULT_SCORE_MULTIPLIER);
        settingHapticFeedbackEnabled = prefs.getBoolean(PREF_HAPTIC_FEEDBACK_ENABLED, DEFAULT_HAPTIC_FEEDBACK_ENABLED);
        settingBirdTrailEnabled = prefs.getBoolean(PREF_BIRD_TRAIL_ENABLED, DEFAULT_BIRD_TRAIL_ENABLED);
        settingGhostModeEnabled = prefs.getBoolean(PREF_GHOST_MODE_ENABLED, DEFAULT_GHOST_MODE_ENABLED);
        settingPipeSpeedVariation = prefs.getFloat(PREF_PIPE_SPEED_VARIATION, DEFAULT_PIPE_SPEED_VARIATION);
        settingBirdSize = prefs.getFloat(PREF_BIRD_SIZE, DEFAULT_BIRD_SIZE);
        settingPipeWidth = prefs.getFloat(PREF_PIPE_WIDTH, DEFAULT_PIPE_WIDTH);
        settingBgScrollSpeed = prefs.getFloat(PREF_BG_SCROLL_SPEED, DEFAULT_BG_SCROLL_SPEED);
        settingGroundScrollSpeed = prefs.getFloat(PREF_GROUND_SCROLL_SPEED, DEFAULT_GROUND_SCROLL_SPEED);
        settingRandomPipeColorsEnabled = prefs.getBoolean(PREF_RANDOM_PIPE_COLORS_ENABLED, DEFAULT_RANDOM_PIPE_COLORS_ENABLED);
        settingInfiniteFlapEnabled = prefs.getBoolean(PREF_INFINITE_FLAP_ENABLED, DEFAULT_INFINITE_FLAP_ENABLED);
        settingJetpackModeEnabled = prefs.getBoolean(PREF_JETPACK_MODE_ENABLED, DEFAULT_JETPACK_MODE_ENABLED);
        settingDrunkModeEnabled = prefs.getBoolean(PREF_DRUNK_MODE_ENABLED, DEFAULT_DRUNK_MODE_ENABLED);
        settingDrunkModeStrength = prefs.getFloat(PREF_DRUNK_MODE_STRENGTH, DEFAULT_DRUNK_MODE_STRENGTH);
        settingWeatherEffect = prefs.getInt(PREF_WEATHER_EFFECT, DEFAULT_WEATHER_EFFECT);
        settingDynamicDayNightEnabled = prefs.getBoolean(PREF_DYNAMIC_DAY_NIGHT_ENABLED, DEFAULT_DYNAMIC_DAY_NIGHT_ENABLED);
        settingScreenShakeEnabled = prefs.getBoolean(PREF_SCREEN_SHAKE_ENABLED, DEFAULT_SCREEN_SHAKE_ENABLED);
        settingScreenShakeStrength = prefs.getFloat(PREF_SCREEN_SHAKE_STRENGTH, DEFAULT_SCREEN_SHAKE_STRENGTH);
        settingGoldenPipeEnabled = prefs.getBoolean(PREF_GOLDEN_PIPE_ENABLED, DEFAULT_GOLDEN_PIPE_ENABLED);
        settingGoldenPipeChance = prefs.getInt(PREF_GOLDEN_PIPE_CHANCE, DEFAULT_GOLDEN_PIPE_CHANCE);
        settingGoldenPipeBonus = prefs.getInt(PREF_GOLDEN_PIPE_BONUS, DEFAULT_GOLDEN_PIPE_BONUS);
        settingMotionBlurEnabled = prefs.getBoolean(PREF_MOTION_BLUR_ENABLED, DEFAULT_MOTION_BLUR_ENABLED);
        settingMotionBlurStrength = prefs.getFloat(PREF_MOTION_BLUR_STRENGTH, DEFAULT_MOTION_BLUR_STRENGTH);
        
        settingMegaStrikeScore = prefs.getInt(PREF_MEGA_STRIKE_SCORE, 350);
        
        String lightningScoresStr = prefs.getString(PREF_LIGHTNING_SCORES, "11,34,79,210");
        settingLightningScores.clear();
        try {
            if (lightningScoresStr != null && !lightningScoresStr.isEmpty()) {
                String[] parts = lightningScoresStr.split(",");
                for (String p : parts) {
                    settingLightningScores.add(Integer.parseInt(p.trim()));
                }
            }
        } catch (Exception e) {
            // Fallback defaults if parsing fails
            settingLightningScores.addAll(Arrays.asList(11, 34, 79, 210));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0) return;

        screenWidth = w;
        screenHeight = h;
        tryInitializeGame();
    }

    private void tryInitializeGame() {
        if (isReady || screenWidth == 0 || screenHeight == 0 || systemBarTop == -1) {
            return;
        }

        if (!prefs.getBoolean(PREF_KEY_WARNING_SHOWN, false)) {
            float aspectRatio = (float) screenHeight / screenWidth;
            final float minAspectRatioThreshold = 1.7f;
            if (aspectRatio < minAspectRatioThreshold) {
                showAspectRatioWarning();
            }
        }

        scale = (float) getPlayableHeight() / unscaledBgDay.getHeight();
        
        // Base Physics Constants
        BASE_GRAVITY_PER_SEC = (0.10f * scale) * TARGET_FPS * TARGET_FPS;
        BASE_FLAP_VELOCITY_PER_SEC = (-3.2f * scale) * TARGET_FPS;
        BASE_PIPE_SPEED_PER_SEC = (1.1f * scale) * TARGET_FPS;
        JETPACK_THRUST_PER_SEC = BASE_GRAVITY_PER_SEC * 1.3f;
        BASE_ROTATION_DELAY_THRESHOLD_PER_SEC = (2.7f * scale) * TARGET_FPS;
        BASE_ROTATION_DOWN_SPEED_PER_SEC = (2.5f * TARGET_FPS);
        TRANSITION_FADE_SPEED_PER_SEC = 800.0f;
        pressOffsetY = 4 * scale;
        maxPipeMoveRange = (getPlayableHeight() - groundHeight) * 0.08f;

        scaleAllBitmaps();
        groundHeight = groundBitmap.getHeight();
        Pipe.initHitboxDimensions(scale);
        float margin = screenWidth * UI_MARGIN_HORIZONTAL_PERCENT;
        float availableWidth = screenWidth - (2 * margin);
        if (scorePanelBitmap != null && scorePanelBitmap.getWidth() > availableWidth) {
            uiScaleCorrection = availableWidth / scorePanelBitmap.getWidth();
        } else {
            uiScaleCorrection = 1.0f;
        }

        weatherSystem.init(screenWidth, screenHeight, scale);
        
        resetGame();
        gameState = GameState.HOME;

        isReady = true;
        resume();
        post(onReady);
    }

    private int getPlayableHeight() { return screenHeight - systemBarTop - systemBarBottom; }

    private void resetGame() {
        sceneVariant = random.nextInt(4);
        groundImpact.reset();
        if(retroAudio!=null)retroAudio.setNightShift(false);
        powerUps.reset(); celebrations.clear(); workbookPipe=null;workbookEarned=false; bonusPipe = null; rocketTarget=null; rocketCooldown=0;
        wallBlasts.clear(); skyTraffic.reset(); stopAuraSound();
        if (fallingGunfire != null) fallingGunfire.land();
        loadSettings();
        
        // Standard Custom Mode
        updateWeatherSounds();
        weatherSystem.setWeatherType(settingWeatherEffect);
        weatherSystem.setHeavyRain(false);

        float speedVariation = 1.0f + (random.nextFloat() - 0.5f) * 2 * settingPipeSpeedVariation;
        GRAVITY_PER_SEC = BASE_GRAVITY_PER_SEC * settingGravity * (settingUpsideDownEnabled ? -1 : 1);
        FLAP_VELOCITY_PER_SEC = BASE_FLAP_VELOCITY_PER_SEC * settingJumpStrength * (settingUpsideDownEnabled ? -1 : 1);
        PIPE_SPEED_PER_SEC = BASE_PIPE_SPEED_PER_SEC * settingGameSpeed * speedVariation * (settingReversePipesEnabled ? -1 : 1);
        ROTATION_DELAY_THRESHOLD_PER_SEC = BASE_ROTATION_DELAY_THRESHOLD_PER_SEC * settingBirdHangDelay;
        ROTATION_DOWN_SPEED_PER_SEC = BASE_ROTATION_DOWN_SPEED_PER_SEC;

        final int UNSCALED_BIRD_HEIGHT_FOR_GAP = 48;
        pipeGap = (int) (UNSCALED_BIRD_HEIGHT_FOR_GAP * scale * PIPE_GAP_BIRD_HEIGHT_MULTIPLIER * settingPipeGap);

        final float VIRTUAL_SCREEN_WIDTH = unscaledBgDay.getWidth();
        final float VIRTUAL_SPACING_FACTOR = 0.68f;
        pipeSpacing = (int) (VIRTUAL_SCREEN_WIDTH * VIRTUAL_SPACING_FACTOR * scale * settingPipeSpacing);

        baseBirdX = settingReversePipesEnabled ? screenWidth * 2 / 3f : screenWidth / 3f;
        isRestarting = false;
        hasTier1Triggered = false;
        hasTier2Triggered = false;
        pipeMoveStartTime = 0;
        pipesAreMoving = false;
        pipesAreStopping = false;
        pipeAnimationCounter = 0f;
        currentPipeMoveRange = 0f;
        birdHitGroundTime = 0;
        gameOverSettingsIconY = -1;
        isGameOverIconAnimationDone = false;
        panelFinishedSlidingTime = 0;
        rainbowHue = 0f;
        drunkModePhase = 0f;
        dayNightCycleProgress = 0f;
        shakeTimer = 0f;
        flashAlpha = 0f;
        birdTrail.clear();
        
        isBirdBurnt = false;
        burntTimer = 0f;
        pipeElectrificationTimer = 0f;
        hasTriggeredMegaStrike = false;
        lastLightningScore = -1; // Reset lightning trigger state
        
        smokeParticles.clear();
        sparkParticles.clear();
        timeDilation = 1.0f;
        cameraZoom = 1.0f;
        cinematicPhase = 0;
        
        sparkleTimer = 0f;
        isSparkleAnimating = false;

        if (!settingDynamicDayNightEnabled) {
            if (settingBackground == 0) backgroundBitmap = bgDayBitmap;
            else if (settingBackground == 1) backgroundBitmap = bgNightBitmap;
            else backgroundBitmap = random.nextBoolean() ? bgNightBitmap : bgDayBitmap;
        }

        currentPipeTopBitmap = pipeDownBitmap;
        currentPipeBottomBitmap = pipeUpBitmap;

        int birdColor;
        if (settingBirdColor >= 0 && settingBirdColor <= 2) birdColor = settingBirdColor;
        else birdColor = random.nextInt(3);
        this.currentBirdColor = birdColor;
        for (int i = 0; i < 3; i++) birdBitmaps[i] = scaledAllBirdBitmaps[birdColor][i];

        birdX = baseBirdX;
        birdY = systemBarTop + (getPlayableHeight() - groundHeight) / 2f;
        if (settingUpsideDownEnabled) {
            birdY = screenHeight - birdY;
        }
        birdVelocityY = 0; birdRotation = 0; birdFrame = 0;
        pipes = new ArrayList<>();
        int playableAreaHeight = getPlayableHeight() - groundHeight;
        float firstPipeX;
        if (settingReversePipesEnabled) {
            firstPipeX = -pipeUpBitmap.getWidth() - (Math.abs(PIPE_SPEED_PER_SEC) * PIPE_SPAWN_DELAY_SECONDS);
        } else {
            firstPipeX = screenWidth + (Math.abs(PIPE_SPEED_PER_SEC) * PIPE_SPAWN_DELAY_SECONDS);
        }

        final int NUM_PIPES = 5;
        for (int i = 0; i < NUM_PIPES; i++) {
            float pipeX = firstPipeX + (i * pipeSpacing * (settingReversePipesEnabled ? -1 : 1));
            Pipe pipe = new Pipe(pipeX, currentPipeBottomBitmap.getWidth(), currentPipeBottomBitmap.getHeight());
            pipe.resetHeight(pipeGap, playableAreaHeight, systemBarTop, settingPipeVariation);

            int colorToSet = settingPipeColor;
            if (settingRandomPipeColorsEnabled) {
                colorToSet = random.nextInt(9);
            }
            pipe.setColorFilter(createPipeColorFilter(colorToSet));

            if (settingGoldenPipeEnabled && random.nextInt(100) < settingGoldenPipeChance) {
                pipe.isGolden = true;
            }

            pipes.add(pipe);
        }

        score = 0;
        displayedScore = 0;
        currentMedalBitmap = null;
        if (darkenPaint != null) darkenPaint.setAlpha(0);
    }

    private ColorFilter createPipeColorFilter(int pipeColorSetting) {
        switch (pipeColorSetting) {
            case 1: return new PorterDuffColorFilter(0xFFD05050, PorterDuff.Mode.MULTIPLY); // Red
            case 2: return new PorterDuffColorFilter(0xFF6080E0, PorterDuff.Mode.MULTIPLY); // Blue
            case 3: return new PorterDuffColorFilter(0xFFE0E060, PorterDuff.Mode.MULTIPLY); // Yellow
            case 4: return new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_ATOP); // White
            case 5: return new PorterDuffColorFilter(0xFFE879E8, PorterDuff.Mode.MULTIPLY); // Pink
            case 6: return new PorterDuffColorFilter(0xFF505050, PorterDuff.Mode.MULTIPLY); // Black
            case 7: return new PorterDuffColorFilter(0xFF9040D0, PorterDuff.Mode.MULTIPLY); // Purple
            case 8: return new PorterDuffColorFilter(0xFFE89030, PorterDuff.Mode.MULTIPLY); // Orange
            default: return null; // Default Green
        }
    }

    private void update(float deltaTime) {
        float realDeltaTime = deltaTime;
        groundImpact.update(realDeltaTime); 
        wallBlasts.tick(realDeltaTime,gameState==GameState.PLAYING ? PIPE_SPEED_PER_SEC : 0);
        float gameDeltaTime = deltaTime * timeDilation;

        if (flashAlpha > 0) {
            float decaySpeed = 1500f;
            flashAlpha -= decaySpeed * realDeltaTime;
            if (flashAlpha < 0) flashAlpha = 0;
        }

        updateCinematicSequence(realDeltaTime);

        // Background Scrolling (Game Time)
        if (gameState != GameState.GAME_OVER && gameState != GameState.PANEL_SLIDING) {
            float effectivePipeSpeed = PIPE_SPEED_PER_SEC;
            float bgScrollSpeed = effectivePipeSpeed * settingBgScrollSpeed;
            float groundScrollSpeed = effectivePipeSpeed * settingGroundScrollSpeed;

            Bitmap bg = (backgroundBitmap != null) ? backgroundBitmap : bgDayBitmap;
            float bgWidth = bg.getWidth();
            
            backgroundX -= bgScrollSpeed * gameDeltaTime;
            if (bgScrollSpeed > 0 && backgroundX <= -bgWidth) {
                backgroundX += bgWidth;
            } else if (bgScrollSpeed < 0 && backgroundX >= bgWidth) {
                backgroundX -= bgWidth;
            }

            float groundWidth = groundBitmap.getWidth();
            groundX -= groundScrollSpeed * gameDeltaTime;
            if (groundScrollSpeed > 0 && groundX <= -groundWidth) {
                groundX += groundWidth;
            } else if (groundScrollSpeed < 0 && groundX >= groundWidth) {
                groundX -= groundWidth;
            }
        }
        
        if (settingsButtonScale != settingsButtonTargetScale) {
            float diff = settingsButtonTargetScale - settingsButtonScale;
            if (Math.abs(diff) < 0.01f) {
                settingsButtonScale = settingsButtonTargetScale;
            } else {
                settingsButtonScale += diff * (10f * realDeltaTime);
            }
        }

        boolean wingAnimation = settingWingAnimationEnabled && (settingInfiniteFlapEnabled || System.currentTimeMillis() - lastFlapTimeMillis < FLAP_ANIMATION_TIMEOUT_MS);
        if (gameState == GameState.PLAYING) {
            if (wingAnimation || (settingJetpackModeEnabled && isScreenPressed)) {
               birdFrame = (int) ((System.currentTimeMillis() / 75) % birdBitmaps.length);
            }
            else birdFrame = 1;
        } else if (gameState == GameState.HOME || gameState == GameState.TRANSITION_TO_WAITING || gameState == GameState.WAITING) {
            if (settingWingAnimationEnabled) birdFrame = (int) ((System.currentTimeMillis() / 150) % birdBitmaps.length);
            else birdFrame = 1;
        }

        if (settingRainbowBirdEnabled) {
            rainbowHue = (rainbowHue + 150 * gameDeltaTime) % 360;
        }

        goldenPipeHue = (goldenPipeHue + 200 * gameDeltaTime);

        if (settingBirdTrailEnabled && (gameState == GameState.PLAYING || gameState == GameState.GAME_OVER)) {
            if (birdTrail.size() >= MAX_TRAIL_PARTICLES) {
                birdTrail.pollFirst();
            }
            birdTrail.add(new TrailParticle(birdX, birdY, birdRotation, currentBirdColor, birdFrame, settingRainbowBirdEnabled ? rainbowHue : -1));
        }
        for (TrailParticle p : birdTrail) {
            p.update(gameDeltaTime);
        }

        if (shakeTimer > 0) {
            shakeTimer -= realDeltaTime;
        }
        
        if (pipeElectrificationTimer > 0) {
            pipeElectrificationTimer -= gameDeltaTime;
            if (pipeElectrificationTimer < 0) pipeElectrificationTimer = 0;
        }
        
        boolean isApocalypse = (score >= 600 && settingWeatherEffect == 2);
        if (isApocalypse) weatherSystem.setHeavyRain(true);

        if (isBirdBurnt) {
            burntTimer -= gameDeltaTime;
            if (burntTimer <= 0) {
                isBirdBurnt = false;
                sparkParticles.clear();
            } else {
                if (random.nextInt(100) < 50) smokeParticles.add(new SmokeParticle(birdX, birdY, scale));
                if (random.nextInt(100) < 40) sparkParticles.add(new SparkParticle(birdX, birdY, scale, random.nextBoolean() ? 0 : 1));
            }
        }
        
        if (isApocalypse && random.nextInt(100) < 30) {
            sparkParticles.add(new SparkParticle(random.nextFloat() * screenWidth, -50, scale, 1));
        }

        Iterator<SmokeParticle> smokeIter = smokeParticles.iterator();
        while (smokeIter.hasNext()) {
            SmokeParticle p = smokeIter.next();
            p.update(gameDeltaTime, PIPE_SPEED_PER_SEC * 0.5f); 
            if (p.isDead()) smokeIter.remove();
        }
        
        Iterator<SparkParticle> sparkIter = sparkParticles.iterator();
        while(sparkIter.hasNext()) {
            SparkParticle sp = sparkIter.next();
            sp.update(gameDeltaTime, (sp.type == 1) ? PIPE_SPEED_PER_SEC * 0.8f : 0);
            if (sp.isDead()) sparkIter.remove();
        }

        updateBirdRect();

        if (gameState==GameState.PLAYING && settingWeatherEffect > 0) {
            weatherSystem.update(gameDeltaTime, PIPE_SPEED_PER_SEC * 0.1f, birdVelocityY, birdRect);
        }

        if (settingDynamicDayNightEnabled && (gameState == GameState.PLAYING || gameState == GameState.WAITING)) {
            dayNightCycleProgress = (dayNightCycleProgress + gameDeltaTime / 120.0f) % 1.0f;
        }

        switch (gameState) {
            case PLAYING:
                if (settingJetpackModeEnabled) {
                    if (isScreenPressed) {
                        float thrust = JETPACK_THRUST_PER_SEC * settingJumpStrength * (settingUpsideDownEnabled ? -1 : 1);
                        birdVelocityY -= thrust * gameDeltaTime;
                    }
                }

                birdVelocityY += GRAVITY_PER_SEC * gameDeltaTime;
                birdY += birdVelocityY * gameDeltaTime;
                
                if (!settingNoClipEnabled) {
                    float birdHalfHeight = (birdBitmaps[birdFrame].getHeight() * settingBirdSize) / 2f;
                    float ceilingThreshold = systemBarTop - birdHalfHeight - (birdHalfHeight * 0.8f); 
                    
                    if (settingUpsideDownEnabled) {
                        float upsideDownCeiling = screenHeight + birdHalfHeight + (birdHalfHeight * 0.8f);
                        if (birdY > upsideDownCeiling) {
                            birdY = upsideDownCeiling;
                            if (birdVelocityY > 0) birdVelocityY = 0;
                        }
                    } else {
                        if (birdY < ceilingThreshold) {
                            birdY = ceilingThreshold;
                            if (birdVelocityY < 0) birdVelocityY = 0;
                        }
                    }
                }

                if(settingDrunkModeEnabled) {
                    drunkModePhase += gameDeltaTime * 2.0f;
                    float maxDrift = screenWidth * 0.15f * settingDrunkModeStrength;
                    birdX = baseBirdX + ((float)Math.sin(drunkModePhase) * maxDrift);
                } else {
                    birdX = baseBirdX;
                }

                if (settingWeatherEffect == 2) {
                    int lightningChance = (score >= 600) ? 100 : 300;
                    if (random.nextInt(lightningChance) == 0) triggerLightning();
                }
                
                // --- CUSTOM LIGHTNING STRIKE LOGIC (FIXED) ---
                if (settingWeatherEffect == 2) {
                    if (settingLightningScores.contains(score)) {
                        // Only trigger if we haven't triggered for *this specific score* yet
                        if (score != lastLightningScore) {
                            startCinematicStrikeSequence(false); // Normal single bolt
                            lastLightningScore = score;
                        }
                    } else if (score == settingMegaStrikeScore) {
                        if (!hasTriggeredMegaStrike) {
                            startCinematicStrikeSequence(true); // Mega Multi-bolt
                            hasTriggeredMegaStrike = true;
                        }
                    }
                }

                // --- ROTATION LOGIC ---
                final float ROTATION_UP_SPEED = 6.0f * TARGET_FPS;
                if (settingUpsideDownEnabled) {
                    if (birdVelocityY > 0) {
                        birdRotation = Math.min(25f, birdRotation + ROTATION_UP_SPEED * gameDeltaTime);
                    } else if (birdVelocityY < -ROTATION_DELAY_THRESHOLD_PER_SEC) {
                        birdRotation = Math.max(-90f, birdRotation - ROTATION_DOWN_SPEED_PER_SEC * gameDeltaTime);
                    }
                } else {
                    if (birdVelocityY < 0) {
                        birdRotation = Math.max(-25f, birdRotation - ROTATION_UP_SPEED * gameDeltaTime);
                    } else if (birdVelocityY > ROTATION_DELAY_THRESHOLD_PER_SEC) {
                        birdRotation = Math.min(90f, birdRotation + ROTATION_DOWN_SPEED_PER_SEC * gameDeltaTime);
                    }
                }
                break;
            case HOME:
                baseBirdX = screenWidth / 2f;
                birdX = baseBirdX;
                birdY = (systemBarTop + (getPlayableHeight() - groundHeight) / 2f) + (float) (Math.sin(System.currentTimeMillis() / 200.0) * 6 * scale);
                if (settingUpsideDownEnabled) {
                    birdY += getPlayableHeight() * 0.18f;
                    birdY = screenHeight - birdY;
                }
                birdRotation = 0;
                break;
            case TRANSITION_TO_WAITING:
                birdY = (systemBarTop + (getPlayableHeight() - groundHeight) / 2f) + (float) (Math.sin(System.currentTimeMillis() / 200.0) * 6 * scale);
                if (settingUpsideDownEnabled) birdY = screenHeight - birdY;
                birdRotation = 0;
                if (isFadingOut) {
                    transitionAlpha += TRANSITION_FADE_SPEED_PER_SEC * gameDeltaTime;
                    if (transitionAlpha >= 255) {
                        transitionAlpha = 255;
                        isFadingOut = false;
                        baseBirdX = settingReversePipesEnabled ? screenWidth * 2 / 3f : screenWidth / 3f;
                        birdX = baseBirdX;
                    }
                } else {
                    transitionAlpha -= TRANSITION_FADE_SPEED_PER_SEC * gameDeltaTime;
                    if (transitionAlpha <= 0) {
                        transitionAlpha = 0;
                        gameState = GameState.WAITING;
                    }
                }
                break;
            case WAITING:
                birdX = baseBirdX;
                birdY = (systemBarTop + (getPlayableHeight() - groundHeight) / 2f) + (float) (Math.sin(System.currentTimeMillis() / 200.0) * 6 * scale);
                if (settingUpsideDownEnabled) birdY = screenHeight - birdY;
                birdRotation = 0;
                break;
            case GAME_OVER:
                float groundTopY = screenHeight - systemBarBottom - groundHeight;
                boolean isOnGround;
                if(settingUpsideDownEnabled) {
                    isOnGround = birdY - (birdBitmaps[1].getHeight() * settingBirdSize / 2f) <= systemBarTop;
                } else {
                    isOnGround = birdY + (birdBitmaps[1].getHeight() * settingBirdSize / 2f) >= groundTopY;
                }

                if (!isOnGround) {
                    birdVelocityY += (GRAVITY_PER_SEC * 1.5f) * gameDeltaTime;
                    birdY += birdVelocityY * gameDeltaTime;
                    final float DEATH_ROTATION_SPEED = 2.8f * TARGET_FPS;
                    if(settingUpsideDownEnabled) {
                        if (birdRotation > -90) birdRotation -= DEATH_ROTATION_SPEED * gameDeltaTime;
                    } else {
                        if (birdRotation < 90) birdRotation += DEATH_ROTATION_SPEED * gameDeltaTime;
                    }
                    birdHitGroundTime = 0;
                } else {
                    if (settingUpsideDownEnabled) {
                        birdY = systemBarTop + (birdBitmaps[1].getHeight() * settingBirdSize / 2f);
                        birdRotation = -90f;
                    } else {
                        birdY = groundTopY - (birdBitmaps[1].getHeight() * settingBirdSize / 2f);
                        birdRotation = 90f;
                    }
                    birdFrame = 1;
                    if (birdHitGroundTime == 0) {
                        birdHitGroundTime = System.currentTimeMillis();
                        if (fallingGunfire != null) fallingGunfire.land();
                        groundImpact.emit(birdX, settingUpsideDownEnabled ? systemBarTop : groundTopY, scale, settingUpsideDownEnabled);
                        if(settingScreenShakeEnabled) triggerShake(5.0f, 0.4f);
                    }

                    if (System.currentTimeMillis() - birdHitGroundTime > PANEL_SLIDE_DELAY_MS) {
                        float scaledGameOverTextHeight = gameOverBitmap.getHeight() * uiScaleCorrection;
                        float scaledGap = (20 * scale) * uiScaleCorrection;
                        float scaledPanelHeight = scorePanelBitmap.getHeight() * uiScaleCorrection;
                        float totalUiBlockHeight = scaledGameOverTextHeight + scaledGap + scaledPanelHeight;
                        int playableAreaCenterY = systemBarTop + (getPlayableHeight() - groundHeight) / 2;
                        gameOverElementsTargetY = (int) (playableAreaCenterY - (totalUiBlockHeight / 2));
                        gameOverElementsY = screenHeight;
                        gameState = GameState.PANEL_SLIDING;
                        birdHitGroundTime = 0;
                    }
                }
                break;
            case PANEL_SLIDING:
                float deltaMultiplier = TARGET_FPS * realDeltaTime;
                if (gameOverElementsY > gameOverElementsTargetY) {
                    float portionToMove = 1.0f - (float)Math.pow(PANEL_SLIDE_EASING_BASE, deltaMultiplier);
                    gameOverElementsY -= (gameOverElementsY - gameOverElementsTargetY) * portionToMove;
                    if (gameOverElementsY - gameOverElementsTargetY < 1.0f) {
                        gameOverElementsY = gameOverElementsTargetY;
                    }
                } else {
                    gameOverElementsY = gameOverElementsTargetY;
                    
                    if (currentMedalBitmap != null && displayedScore == score) {
                        sparkleTimer += realDeltaTime;

                        if (!isSparkleAnimating) {
                            if (sparkleTimer >= SPARKLE_INTERVAL) {
                                isSparkleAnimating = true;
                                sparkleTimer = 0;
                                sparkleXOffset = 0.2f + random.nextFloat() * 0.6f;
                                sparkleYOffset = 0.2f + random.nextFloat() * 0.6f;
                            }
                        } else {
                            float progress = sparkleTimer / SPARKLE_DURATION;
                            if (progress >= 1.0f) {
                                isSparkleAnimating = false;
                                sparkleTimer = 0;
                                sparkleScaleCurrent = 0;
                            } else {
                                sparkleScaleCurrent = (float) Math.sin(progress * Math.PI);
                            }
                        }
                    }

                    if (lastScoreTickTime == 0) lastScoreTickTime = System.currentTimeMillis();

                    if (displayedScore < score) {
                        long currentTime = System.currentTimeMillis();
                        long scoreDiff = score - displayedScore;
                        long interval; int increment;
                        if (scoreDiff > 100) { interval = SCORE_ANIMATION_INTERVAL_MS_FAST;
                            increment = 11; }
                        else if (scoreDiff > 10) { interval = SCORE_ANIMATION_INTERVAL_MS_FAST;
                            increment = 3; }
                        else { interval = SCORE_ANIMATION_INTERVAL_MS_SLOW;
                            increment = 1; }
                        if (currentTime - lastScoreTickTime > interval) {
                            displayedScore += increment;
                            if (displayedScore > score) { displayedScore = score; }
                            lastScoreTickTime = currentTime;
                        }
                    }

                    if (panelFinishedSlidingTime == 0) {
                        panelFinishedSlidingTime = System.currentTimeMillis();
                        float margin = screenWidth * UI_MARGIN_HORIZONTAL_PERCENT;
                        gameOverSettingsIconTargetY = systemBarTop + margin;
                    }
                    if (!isGameOverIconAnimationDone && System.currentTimeMillis() - panelFinishedSlidingTime > GAMEOVER_ICON_SLIDE_DELAY_MS) {
                        if (gameOverSettingsIconY == -1) gameOverSettingsIconY = -settingsButtonBitmap.getHeight();
                        if (gameOverSettingsIconY < gameOverSettingsIconTargetY) {
                            float iconPortionToMove = 1.0f - (float)Math.pow(GAMEOVER_ICON_EASING_BASE, deltaMultiplier);
                            gameOverSettingsIconY += (gameOverSettingsIconTargetY - gameOverSettingsIconY) * iconPortionToMove;
                            if (gameOverSettingsIconTargetY - gameOverSettingsIconY < 1.0f) {
                                gameOverSettingsIconY = gameOverSettingsIconTargetY;
                                isGameOverIconAnimationDone = true;
                            }
                        } else {
                            gameOverSettingsIconY = gameOverSettingsIconTargetY;
                            isGameOverIconAnimationDone = true;
                        }
                    }
                }

                int maxAlphaFromSettings = (int) (settingGameOverOpacity / 100.0f * 255.0f);
                if (screenHeight > gameOverElementsTargetY && darkenPaint != null) {
                    float progress = 1.0f - ((gameOverElementsY - gameOverElementsTargetY) / (screenHeight - gameOverElementsTargetY));
                    progress = Math.max(0.0f, Math.min(1.0f, progress));
                    darkenPaint.setAlpha((int) (maxAlphaFromSettings * progress));
                }

                if (isRestarting) {
                    transitionAlpha += TRANSITION_FADE_SPEED_PER_SEC * realDeltaTime;
                    if (transitionAlpha >= 255) {
                        transitionAlpha = 255;
                        resetGame();
                        gameState = GameState.TRANSITION_TO_WAITING;
                        isFadingOut = false;
                    }
                }
                break;
        }

        if (gameState == GameState.PLAYING) {
            if (settingMovingPipesEnabled) {
                if (score >= settingPipeMoveTier1 && !hasTier1Triggered) {
                    hasTier1Triggered = true;
                    pipesAreMoving = true; pipesAreStopping = false;
                    pipeAnimationSpeed = 0.03f; pipeMoveStartTime = System.currentTimeMillis();
                }
                if (score >= settingPipeMoveTier2 && !hasTier2Triggered) {
                    hasTier2Triggered = true;
                    pipesAreMoving = true; pipesAreStopping = false;
                    pipeAnimationSpeed = 0.06f; pipeMoveStartTime = System.currentTimeMillis();
                }
                if (pipeMoveStartTime != 0 && !pipesAreStopping && System.currentTimeMillis() - pipeMoveStartTime > PIPE_MOVE_DURATION_MS) {
                    pipesAreStopping = true;
                    pipeMoveStartTime = 0;
                }
                if (pipesAreStopping) {
                    boolean allPipesAtRest = true;
                    for (Pipe pipe : pipes) { if (!pipe.isAtRest()) { allPipesAtRest = false; break;
                    } }
                    if (allPipesAtRest) { pipesAreMoving = false;
                        pipesAreStopping = false; }
                }
                if (pipesAreMoving || pipesAreStopping) {
                    pipeAnimationCounter += (pipeAnimationSpeed * TARGET_FPS) * gameDeltaTime;
                    if (currentPipeMoveRange < maxPipeMoveRange && !pipesAreStopping) {
                        float moveRangeSpeed = (0.15f * scale * TARGET_FPS);
                        currentPipeMoveRange += moveRangeSpeed * gameDeltaTime;
                        currentPipeMoveRange = Math.min(currentPipeMoveRange, maxPipeMoveRange);
                    }
                }
            }
            int playableAreaHeight = getPlayableHeight() - groundHeight;
            for (Pipe pipe : pipes) {
                pipe.x -= PIPE_SPEED_PER_SEC * gameDeltaTime;
                if (score>=30) {
                    float visualWidth=pipe.width*settingPipeWidth;
                    float left=pipe.x-(visualWidth-pipe.width)/2f;
                    boolean near=birdRect.right+28*scale>left && birdRect.left-28*scale<left+visualWidth;
                    pipe.updateFactoryMotion(score,gameDeltaTime,systemBarTop+24*scale,screenHeight-systemBarBottom-groundHeight-24*scale,pipeGap,near,scale);
                } else if (settingMovingPipesEnabled) {
                    pipe.updateAnimation(pipesAreMoving, pipeAnimationCounter, currentPipeMoveRange, pipesAreStopping);
                }

                boolean scored;
                if (settingReversePipesEnabled) {
                    scored = !pipe.isScored && pipe.x > birdX;
                } else {
                    scored = !pipe.isScored && pipe.x + pipe.width < birdX;
                }
                if (scored) {
                    score += settingScoreMultiplier;
                    if(pipe.isGolden) {
                        score += settingGoldenPipeBonus;
                    }
                    playSound(soundPoint); celebrations.splash(birdX,birdY,scale,settingScoreMultiplier+(pipe.isGolden?settingGoldenPipeBonus:0)); pipe.isScored = true;
                }

                boolean recycle;
                if (settingReversePipesEnabled) {
                    recycle = pipe.x - (pipe.width * settingPipeWidth) > screenWidth;
                } else {
                    recycle = pipe.x + (pipe.width * settingPipeWidth) < 0;
                }

                if (recycle) {
                    pipe.x += (pipes.size() * pipeSpacing * (settingReversePipesEnabled ? -1 : 1));
                    pipe.resetHeight(pipeGap, playableAreaHeight, systemBarTop, settingPipeVariation);
                    pipe.isScored = false;
                    if (settingRandomPipeColorsEnabled) {
                        pipe.setColorFilter(createPipeColorFilter(random.nextInt(9)));
                    } else {
                        pipe.setColorFilter(createPipeColorFilter(settingPipeColor));
                    }

                    if (settingGoldenPipeEnabled && random.nextInt(100) < settingGoldenPipeChance) {
                        pipe.isGolden = true;
                    } else {
                        pipe.isGolden = false;
                    }
                }
            }
        }
        
        if (gameState == GameState.PLAYING) {
            updateBirdRect();
            updateWallOpenings(realDeltaTime);
            updatePowerUps();
            updateBazooka(gameDeltaTime);
            checkCollisions();
        }
    }
    
    // --- CINEMATIC ENGINE LOGIC ---
    
    private boolean isMegaStrike = false;
    
    private void startCinematicStrikeSequence(boolean mega) {
        cinematicPhase = 1; // Start Zoom In
        cinematicTimer = 0f;
        timeDilation = 1.0f;
        cameraZoom = 1.0f;
        isMegaStrike = mega;
    }
    
    private void updateCinematicSequence(float realDeltaTime) {
        if (cinematicPhase == 0) return;
        
        cinematicTimer += realDeltaTime;
        
        if (cinematicPhase == 1) { // PHASE 1: ZOOM IN & SLOW MO
            float progress = Math.min(1.0f, cinematicTimer / 0.2f);
            float targetZoom = isMegaStrike ? 1.8f : 1.5f;
            cameraZoom = 1.0f + ((targetZoom - 1.0f) * progress);
            float targetTime = isMegaStrike ? 0.05f : 0.1f;
            timeDilation = 1.0f - ((1.0f - targetTime) * progress);
            
            if (progress >= 1.0f) {
                cinematicPhase = 2;
                cinematicTimer = 0f;
                if (isMegaStrike) {
                    triggerMegaStrike();
                } else {
                    triggerDirectLightningOnBird();
                }
            }
        } 
        else if (cinematicPhase == 2) { // PHASE 2: HOLD
            timeDilation = isMegaStrike ? 0.05f : 0.1f;
            cameraZoom = isMegaStrike ? 1.8f : 1.5f;
            if (cinematicTimer >= (isMegaStrike ? 0.8f : 0.5f)) { 
                cinematicPhase = 3;
                cinematicTimer = 0f;
            }
        } 
        else if (cinematicPhase == 3) { // PHASE 3: ZOOM OUT
            float progress = Math.min(1.0f, cinematicTimer / 0.3f);
            float startZoom = isMegaStrike ? 1.8f : 1.5f;
            float startTime = isMegaStrike ? 0.05f : 0.1f;
            cameraZoom = startZoom - ((startZoom - 1.0f) * progress);
            timeDilation = startTime + ((1.0f - startTime) * progress);
            
            if (progress >= 1.0f) {
                cinematicPhase = 0; // Done
                timeDilation = 1.0f;
                cameraZoom = 1.0f;
                isMegaStrike = false;
            }
        }
    }

    private void triggerLightning() {
        weatherSystem.triggerLightning(screenWidth, getPlayableHeight() + systemBarTop);
        playSound(soundThunder);
        triggerThunderHaptic();
        if (settingScreenShakeEnabled) {
            triggerShake(7.0f, 0.5f); 
        }
    }
    
    private void triggerDirectLightningOnBird() {
        weatherSystem.triggerTargetedLightning(birdX, birdY, screenWidth, getPlayableHeight() + systemBarTop);
        playSound(soundThunder);
        triggerThunderHaptic();
        if (settingScreenShakeEnabled) triggerShake(15.0f, 0.8f); 
        isBirdBurnt = true;
        burntTimer = 10.0f; 
        flashAlpha = 255;
    }
    
    private void triggerMegaStrike() {
        weatherSystem.triggerMultiDirectionalBolts(birdX, birdY, screenWidth, getPlayableHeight() + systemBarTop);
        playSound(soundThunder);
        playSound(soundThunder); 
        triggerThunderHaptic();
        if (settingScreenShakeEnabled) triggerShake(25.0f, 1.5f);
        isBirdBurnt = true;
        burntTimer = 20.0f; 
        pipeElectrificationTimer = 20.0f;
        flashAlpha = 255;
    }

    private void checkCollisions() {
        if (settingNoClipEnabled) return;
        float birdHalfHeight = (birdBitmaps[birdFrame].getHeight() * settingBirdSize) / 2f;
        
        // Collision check with GROUND
        if (settingUpsideDownEnabled) {
            if (birdRect.bottom >= screenHeight - systemBarBottom - groundHeight) {
                gameOver();
                return;
            }
        } else {
            if (birdY + birdHalfHeight >= screenHeight - systemBarBottom - groundHeight) {
                gameOver();
                return;
            }
        }

        if (powerUps.active(PowerUpState.Kind.SAYAJIN)) return;

        for (Pipe pipe : pipes) {
            if(pipe.status.isDestroyed()) continue;
            float visualWidth = pipe.width * settingPipeWidth;
            float wallX = pipe.x - (visualWidth - pipe.width) / 2f;
            boolean[] mask = preparedAssets.wallMasks[pipe.wallStyle];
            if (WallCollision.intersects(mask, birdRect.left, birdRect.top, birdRect.right, birdRect.bottom,
                    wallX, topWallY(pipe), visualWidth, pipe.height, true) ||
                WallCollision.intersects(mask, birdRect.left, birdRect.top, birdRect.right, birdRect.bottom,
                    wallX, bottomWallY(pipe), visualWidth, pipe.height, false)) {
                gameOver(true); return;
            }
        }
    }

    private void triggerShake(float magnitude, float duration) {
        if (!settingScreenShakeEnabled) return;
        this.currentShakeMagnitude = magnitude * scale * settingScreenShakeStrength;
        this.shakeTimer = duration;
    }

    private void triggerThunderHaptic() {
        if (settingHapticFeedbackEnabled && vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 30, 40, 30, 40, 80, 20, 100};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private void gameOver() { gameOver(false); }

    private void gameOver(boolean wallImpact) {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.GAME_OVER;
            powerUps.end(); celebrations.clear();workbookPipe=null; bonusPipe = null; rocketTarget=null; stopAuraSound();
            
            cinematicPhase = 0;
            cameraZoom = 1.0f;
            timeDilation = 1.0f;
            
            flashAlpha = 255f;
            stopWeatherSounds();
            if (wallImpact && fallingGunfire != null) fallingGunfire.begin(settingSoundEnabled && isRunning);
            else playSound(soundHit);
            triggerShake(5.0f, 0.4f);
            postDelayed(() -> { if(gameState==GameState.GAME_OVER || gameState==GameState.PANEL_SLIDING)playSound(soundDie); }, 300);
            if (score > highScore) { highScore = score; prefs.edit().putInt("highScore", highScore).apply();
            }
            
            if (score >= 40) currentMedalBitmap = medalBitmaps[0]; 
            else if (score >= 30) currentMedalBitmap = medalBitmaps[1];
            else if (score >= 20) currentMedalBitmap = medalBitmaps[2];
            else if (score >= 10) currentMedalBitmap = medalBitmaps[3];
            else currentMedalBitmap = null;

            displayedScore = 0;
            lastScoreTickTime = 0;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null || !isReady) return;

        if (settingMotionBlurEnabled) {
            motionBlurPaint.setAlpha((int) (255 / (Math.max(1f, settingMotionBlurStrength) * 1.5f)));
            canvas.drawPaint(motionBlurPaint);
        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }

        canvas.save();

        // --- CAMERA TRANSFORM (Zoom & Shake) ---
        if (shakeTimer > 0) {
            float magnitude = currentShakeMagnitude * (shakeTimer / 0.5f);
            float xOffset = (random.nextFloat() - 0.5f) * 2 * magnitude;
            float yOffset = (random.nextFloat() - 0.5f) * 2 * magnitude;
            canvas.translate(xOffset, yOffset);
        }
        
        if (cameraZoom > 1.0f) {
            canvas.scale(cameraZoom, cameraZoom, birdX, birdY);
        }

        if (settingUpsideDownEnabled) {
            canvas.scale(1, -1, screenWidth / 2f, screenHeight / 2f);
        }

        // --- DRAWING GAME WORLD ---

        pixelPaint.setColorFilter(score>=40 ? horrorBackground : null);
        nightBgPaint.setColorFilter(score>=40 ? horrorBackground : null);
        Bitmap bgToDraw;
        if(settingDynamicDayNightEnabled) {
            bgToDraw = bgDayBitmap;
        } else {
            bgToDraw = (backgroundBitmap != null) ? backgroundBitmap : bgDayBitmap;
        }

        float bgDrawWidth = bgToDraw.getWidth();
        // Robust tiling logic
        float startBgX = backgroundX % bgDrawWidth;
        if (startBgX > 0) startBgX -= bgDrawWidth; 
        
        for (float x = startBgX - bgDrawWidth; x < screenWidth + bgDrawWidth; x += bgDrawWidth) {
            canvas.drawBitmap(bgToDraw, x, 0, pixelPaint);
        }

        if (settingDynamicDayNightEnabled) {
            float sineProgress = (float) (1 - Math.cos(dayNightCycleProgress * 2 * Math.PI)) / 2;
            int alpha = (int) (sineProgress * 255);
            nightBgPaint.setAlpha(alpha);

            for (float x = startBgX - bgDrawWidth; x < screenWidth + bgDrawWidth; x += bgDrawWidth) {
                canvas.drawBitmap(bgNightBitmap, x, 0, nightBgPaint);
            }
        }

        for (float x = startBgX - bgDrawWidth; x < screenWidth + bgDrawWidth; x += bgDrawWidth) {
            canvas.save();
            canvas.translate(x, 0);
            canvas.scale(bgDrawWidth / 288f, screenHeight / 512f);
            canvas.drawBitmap(preparedAssets.environments[StageProgression.environment(score, sceneVariant)], 0, 0, pixelPaint);
            canvas.restore();
        }

        factoryTraffic.draw(canvas,screenWidth,screenHeight,score>=40);
        canvas.save();
        canvas.scale(screenWidth/288f,screenHeight/512f);
        // Indoor factory: no aircraft overlay.
        canvas.restore();

        if (gameState==GameState.PLAYING && settingWeatherEffect > 0) {
             weatherSystem.drawBack(canvas);
        }

        pixelPaint.setColorFilter(null);nightBgPaint.setColorFilter(null);
        // Draw Pipes
        if (gameState != GameState.HOME && !(gameState == GameState.TRANSITION_TO_WAITING && isFadingOut)) {
            for (Pipe pipe : pipes) {
                if(pipe.status.isDestroyed()) continue;
                float visualPipeWidth = pipe.width * settingPipeWidth;
                float xPos = pipe.x - (visualPipeWidth - pipe.width) / 2f;

                pipeDestRect.set(xPos, topWallY(pipe), xPos + visualPipeWidth, topWallY(pipe) + pipe.height);
                drawMovingWall(canvas,wallTops[pipe.wallStyle],pipe,true);
                
                if (pipeElectrificationTimer > 0) {
                     drawPipeElectricity(canvas, xPos, topWallY(pipe), visualPipeWidth, pipe.height);
                }

                pipeDestRect.set(xPos, bottomWallY(pipe), xPos + visualPipeWidth, bottomWallY(pipe) + pipe.height);
                drawMovingWall(canvas,wallBottoms[pipe.wallStyle],pipe,false);

                if (pipeElectrificationTimer > 0) {
                     drawPipeElectricity(canvas, xPos, bottomWallY(pipe), visualPipeWidth, pipe.height);
                }

                if (pipe.isGolden) {
                    gradientMatrix.setTranslate(goldenPipeHue, 0);
                    goldenPipeShader.setLocalMatrix(gradientMatrix);

                    pipeDestRect.set(xPos, topWallY(pipe), xPos + visualPipeWidth, topWallY(pipe) + pipe.height);
                    canvas.drawRect(pipeDestRect, goldenPipePaint);
                    pipeDestRect.set(xPos, bottomWallY(pipe), xPos + visualPipeWidth, bottomWallY(pipe) + pipe.height);
                    canvas.drawRect(pipeDestRect, goldenPipePaint);
                }
            }
        }
        
        float groundDrawWidth = groundBitmap.getWidth();
        float groundTopY = screenHeight - systemBarBottom - groundHeight;
        
        float startGroundX = groundX % groundDrawWidth;
        if (startGroundX > 0) startGroundX -= groundDrawWidth;
        
        for (float x = startGroundX - groundDrawWidth; x < screenWidth + groundDrawWidth; x += groundDrawWidth) {
            groundDestRect.set(x, groundTopY, x + groundDrawWidth, screenHeight);
            canvas.drawBitmap(groundBitmap, null, groundDestRect, pixelPaint);
        }

        // Draw Smoke Particles (Behind bird)
        for (SmokeParticle p : smokeParticles) {
            p.draw(canvas);
        }

        wallBlasts.draw(canvas,pixelPaint);
        if(rocketTarget!=null) bonusArt.rocket(canvas,rocketX,rocketY,scale,settingReversePipesEnabled);

        if (powerUps.visible()) {
            Bitmap item=powerUps.collectibleKind()==PowerUpState.Kind.CELULAR ? phoneBitmap :
                    powerUps.collectibleKind()==PowerUpState.Kind.BAZUCA ? bazookaBitmap : bananaBitmap;
            float itemScale=20*scale/Math.max(item.getWidth(),item.getHeight());
            canvas.save(); canvas.translate(bonusX-item.getWidth()*itemScale/2f,bonusY-item.getHeight()*itemScale/2f);
            canvas.scale(itemScale,itemScale);
            canvas.drawBitmap(item, 0, 0, pixelPaint); canvas.restore();
        }
        if (gameState==GameState.PLAYING && powerUps.active(PowerUpState.Kind.SAYAJIN)) bonusArt.aura(canvas, birdX, birdY, scale * settingBirdSize,
                android.os.SystemClock.uptimeMillis() / 1000.0);
        if(gameState==GameState.PLAYING) drawBirdTrail(canvas);

        birdPaint.setColorFilter(null);
        if(settingRainbowBirdEnabled) {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setRotate(0, rainbowHue);
            colorMatrix.setRotate(1, rainbowHue);
            colorMatrix.setRotate(2, rainbowHue);
            birdPaint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        }
        
        if (isBirdBurnt) {
            birdPaint.setColorFilter(burntColorFilter);
        }
        
        if (settingGhostModeEnabled) {
            birdPaint.setAlpha(128);
        }

        Bitmap currentBirdBitmap = birdBitmaps[birdFrame];
        birdMatrix.reset();
        birdMatrix.postTranslate(-currentBirdBitmap.getWidth() / 2f, -currentBirdBitmap.getHeight() / 2f);
        birdMatrix.postRotate(birdRotation);
        birdMatrix.postScale(settingBirdSize, settingBirdSize, 0, 0);
        birdMatrix.postTranslate(birdX, birdY);
        canvas.drawBitmap(currentBirdBitmap, birdMatrix, birdPaint);
        birdPaint.setAlpha(255);
        birdPaint.setColorFilter(null); // Reset filter

        if(powerUps.active(PowerUpState.Kind.BAZUCA))
            bonusArt.heldBazooka(canvas,birdX,birdY,scale*settingBirdSize,settingReversePipesEnabled);

        if(workbookEarned) {
            canvas.save();canvas.rotate(birdRotation,birdX,birdY);
            bonusArt.crown(canvas,birdX,birdY-currentBirdBitmap.getHeight()*settingBirdSize/2f,scale*settingBirdSize);
            canvas.restore();
        }
        if(gameState==GameState.PLAYING && workbookPipe!=null) {
            canvas.drawBitmap(workbookBitmap,null,new RectF(workbookX-10*scale,workbookY-12*scale,workbookX+10*scale,workbookY+12*scale),pixelPaint);
        }
        celebrations.draw(canvas,scale);
        // Draw Sparks (Over bird)
        for (SparkParticle sp : sparkParticles) {
            sp.draw(canvas, sparkPaint);
        }

        if (gameState==GameState.PLAYING && settingWeatherEffect > 0) {
             weatherSystem.drawFront(canvas);
             weatherSystem.drawLightingOverlay(canvas, screenWidth, screenHeight);
        }

        groundImpact.draw(canvas);

        // --- END CAMERA TRANSFORM ---
        canvas.restore();
        
        // --- UI (DRAWN IN SCREEN SPACE - NO ZOOM) ---

        if (gameState == GameState.HOME) {
            drawHomeScreen(canvas);
        } else if (gameState == GameState.TRANSITION_TO_WAITING) {
            if (isFadingOut) drawHomeScreen(canvas);
            else drawCenteredBitmap(canvas, getReadyBitmap, -(int) (getPlayableHeight() * 0.15));
        } else if (gameState == GameState.WAITING) {
            drawCenteredBitmap(canvas, getReadyBitmap, -(int) (getPlayableHeight() * 0.15));
        } else if (gameState == GameState.PLAYING) {
            WorldArt.coinLabel(canvas, screenWidth / 2f, systemBarTop + 30 * scale, scale);
            drawScoreWithImages(canvas, score, screenWidth / 2, systemBarTop + (int)(49*scale));
            celebrations.drawLabel(canvas,scale,screenWidth-45*scale,systemBarTop+49*scale);
            if (powerUps.active()) {
                Paint bonusLabel = WorldArt.textPaint(11 * scale);
                bonusLabel.setColor(powerUps.active(PowerUpState.Kind.CELULAR) ? 0xff8be6ff : 0xffffdb4f);
                String name=powerUps.active(PowerUpState.Kind.CELULAR) ? "PARAFUSADEIRA / VÃO MAIOR" :
                        powerUps.active(PowerUpState.Kind.BAZUCA) ? "BAZUCA" : "PNEU / PROTEÇÃO";
                String status=powerUps.active(PowerUpState.Kind.BAZUCA)
                        ? "BAZUCA / "+powerUps.rounds()+" TIROS"+(powerUps.rounds()==0?" / EM VOO":"")
                        : name+" / "+(int)Math.ceil(powerUps.remaining())+"s";
                float maxWidth=screenWidth-24*scale;
                if(bonusLabel.measureText(status)>maxWidth)bonusLabel.setTextSize(bonusLabel.getTextSize()*maxWidth/bonusLabel.measureText(status));
                canvas.drawText(status,screenWidth/2f,systemBarTop+15*scale,bonusLabel);
            }
            if (score >= 20) {
                Paint stageLabel = WorldArt.textPaint(8 * scale);
                stageLabel.setColor(Color.rgb(255,226,154));
                canvas.drawText(score >= 40 ? "TURNO MACABRO" : "MONTAGEM / TURNO 2",
                        screenWidth / 2f, systemBarTop + 78 * scale, stageLabel);
            }
        } else if (gameState == GameState.PANEL_SLIDING || gameState == GameState.GAME_OVER) {
            if (darkenPaint.getAlpha() > 0) {
                canvas.drawRect(0, 0, screenWidth, screenHeight, darkenPaint);
            }
            drawGameOverScreen(canvas);
        }

        if (transitionAlpha > 0) {
            transitionPaint.setAlpha((int) transitionAlpha);
            canvas.drawRect(0, 0, screenWidth, screenHeight, transitionPaint);
        }
        
        if (flashAlpha > 0) {
            flashPaint.setAlpha((int) flashAlpha);
            canvas.drawRect(0, 0, screenWidth, screenHeight, flashPaint);
        }
        if (settingNoClipEnabled || android.os.SystemClock.uptimeMillis()<secretNoticeUntil) {
            String status=settingNoClipEnabled ? "INVENCIBILIDADE ATIVADA" : "INVENCIBILIDADE DESATIVADA";
            secretStatusText.setTextSize(11*scale);
            secretStatusText.setColor(settingNoClipEnabled ? 0xffa6ffe1 : 0xffffd889);
            float half=Math.min(screenWidth/2f-6*scale,secretStatusText.measureText(status)/2f+7*scale);
            float y=screenHeight-systemBarBottom-groundHeight+20*scale;
            secretStatusBackground.setColor(0xee102a32);
            canvas.drawRect(screenWidth/2f-half,y-12*scale,screenWidth/2f+half,y+5*scale,secretStatusBackground);
            canvas.drawText(status,screenWidth/2f,y,secretStatusText);
        }
    }
    
    private void drawPipeElectricity(Canvas canvas, float x, float y, float w, float h) {
        Path path = new Path();
        float currentY = y;
        float endY = y + h;
        for(int i=0; i<3; i++) {
            path.reset();
            float startX = x + (random.nextFloat() * w);
            path.moveTo(startX, currentY);
            float cy = currentY;
            float cx = startX;
            while(cy < endY) {
                 cy += random.nextInt(40) + 10;
                 cx += (random.nextInt(30) - 15);
                 if(cx < x) cx = x;
                 if(cx > x + w) cx = x + w;
                 path.lineTo(cx, cy);
            }
            canvas.drawPath(path, electricityPipePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            waitingTouch = gameState == GameState.WAITING;
            secretTouchX = event.getX(); secretTouchY = event.getY();
            secretTouchTravel=0;
        }
        for(int i=0;i<event.getHistorySize();i++) {
            secretTouchTravel=Math.max(secretTouchTravel,Math.max(Math.abs(event.getHistoricalX(i)-secretTouchX),
                    Math.abs(event.getHistoricalY(i)-secretTouchY)));
        }
        secretTouchTravel=Math.max(secretTouchTravel,Math.max(Math.abs(event.getX()-secretTouchX),
                Math.abs(event.getY()-secretTouchY)));
        if(secretGestureTouch.onTouchEvent(event,screenWidth,screenHeight)) {
            settingNoClipEnabled=SecretInvincibility.toggle();
            secretNoticeUntil=android.os.SystemClock.uptimeMillis()+4000;
            isScreenPressed=false; waitingTouch=false; pressedButtonRect=null;
            settingsButtonTargetScale=1f;
            android.widget.Toast.makeText(getContext(),settingNoClipEnabled ? "Invencibilidade ativada" :
                    "Invencibilidade desativada",android.widget.Toast.LENGTH_SHORT).show();
            if(settingHapticFeedbackEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            invalidate();return true;
        }
        if(event.getPointerCount()>1 || action==MotionEvent.ACTION_POINTER_DOWN ||
                action==MotionEvent.ACTION_POINTER_UP || action==MotionEvent.ACTION_CANCEL) {
            isScreenPressed=false; waitingTouch=false; pressedButtonRect=null;
            settingsButtonTargetScale=1f;
            invalidate();return true;
        }
        int touchX = (int) event.getX();
        int touchY = (int) event.getY();
        if (settingUpsideDownEnabled) {
            touchY = screenHeight - touchY;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isScreenPressed = true;
                pressedButtonRect = null;
                settingsButtonTargetScale = 1.0f;
                
                if (gameState == GameState.HOME) {
                    if (playButtonRect.contains(touchX, touchY)) pressedButtonRect = playButtonRect;
                    else if (scoreButtonRect.contains(touchX, touchY)) pressedButtonRect = scoreButtonRect;
                    else if (settingsButtonRect.contains(touchX, touchY)) {
                        pressedButtonRect = settingsButtonRect;
                        settingsButtonTargetScale = 0.85f;
                    }
                } else if (gameState == GameState.WAITING) {
                    // Wait for release so a deliberate secret stroke does not begin a run.
                } else if (gameState == GameState.PLAYING) {
                    if (!settingJetpackModeEnabled) flap();
                } else if (gameState == GameState.PANEL_SLIDING && gameOverElementsY == gameOverElementsTargetY && !isRestarting) {
                    if (displayedScore == score && playButtonRect.contains(touchX, touchY)) {
                        pressedButtonRect = playButtonRect;
                    }
                    if (isGameOverIconAnimationDone && settingsButtonRect.contains(touchX, touchY)) {
                        pressedButtonRect = settingsButtonRect;
                        settingsButtonTargetScale = 0.85f;
                    }
                }
                if (pressedButtonRect != null) invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isScreenPressed = false;
                settingsButtonTargetScale = 1.0f;
                float tapSlop = Math.max(18f, screenWidth * .04f);
                if(secretTouchTravel>=tapSlop) pressedButtonRect=null;
                if (waitingTouch && gameState == GameState.WAITING) {
                    waitingTouch = false;
                    if (secretTouchTravel < tapSlop) {
                        gameState = GameState.PLAYING;
                        updateWeatherSounds();
                        if (!settingJetpackModeEnabled) flap();
                    }
                }
                
                if (pressedButtonRect != null && pressedButtonRect.contains(touchX, touchY)) {
                    if (gameState == GameState.HOME) {
                        if (pressedButtonRect == playButtonRect) {
                            gameState = GameState.TRANSITION_TO_WAITING; isFadingOut = true; playSound(soundSwooshing);
                        } else if (pressedButtonRect == scoreButtonRect) {
                            new MaterialAlertDialogBuilder(getContext())
                                .setTitle("Coins")
                                .setMessage("Seu recorde: " + highScore + " Coins")
                                .setPositiveButton("Voltar", null).show();
                        } else if (pressedButtonRect == settingsButtonRect) {
                            playSound(soundSwooshing);
                            Intent intent = new Intent(getContext(), SettingsActivity.class);
                            getContext().startActivity(intent);
                        }
                    } else if (gameState == GameState.PANEL_SLIDING) {
                        if (pressedButtonRect == playButtonRect) {
                            isRestarting = true;
                            playSound(soundSwooshing);
                        } else if (pressedButtonRect == settingsButtonRect) {
                            playSound(soundSwooshing);
                            Intent intent = new Intent(getContext(), SettingsActivity.class);
                            getContext().startActivity(intent);
                        }
                    }
                }
                if (pressedButtonRect != null) { pressedButtonRect = null; invalidate(); }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedButtonRect != null && !pressedButtonRect.contains(touchX, touchY)) { 
                    pressedButtonRect = null;
                    settingsButtonTargetScale = 1.0f;
                    invalidate(); 
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                isScreenPressed = false;
                settingsButtonTargetScale = 1.0f;
                if (pressedButtonRect != null) { pressedButtonRect = null; invalidate(); }
                break;
        }
        return true;
    }
    private void flap() {
        birdVelocityY = FLAP_VELOCITY_PER_SEC;
        playSound(soundWing);
        if (settingHapticFeedbackEnabled) {
            this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        lastFlapTimeMillis = System.currentTimeMillis();
    }
    private void updateBirdRect() {
        Bitmap currentBirdBitmap = birdBitmaps[birdFrame];
        float baseHitboxWidth = currentBirdBitmap.getWidth() * (1.0f - 2.0f * BIRD_HITBOX_PADDING_X);
        float baseHitboxHeight = currentBirdBitmap.getHeight() * (1.0f - 2.0f * BIRD_HITBOX_PADDING_Y);
        float hitboxMultiplier = Math.max(0.0f, settingBirdHitbox);
        float finalHitboxWidth = baseHitboxWidth * hitboxMultiplier * settingBirdSize;
        float finalHitboxHeight = baseHitboxHeight * hitboxMultiplier * settingBirdSize;
        float left = birdX - finalHitboxWidth / 2f;
        float top = birdY - finalHitboxHeight / 2f;
        float right = birdX + finalHitboxWidth / 2f;
        float bottom = birdY + finalHitboxHeight / 2f;
        birdRect.set((int) left, (int) top, (int) right, (int) bottom);
    }

    private void drawBirdTrail(Canvas canvas) {
        if (!settingBirdTrailEnabled || birdTrail.isEmpty()) return;

        int i = 0;
        for (TrailParticle particle : birdTrail) {
            float progress = (float) i / birdTrail.size();
            int alpha = (int) (progress * 100);

            trailPaint.setColorFilter(null);
            if (particle.rainbowHue != -1) {
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setRotate(0, particle.rainbowHue);
                colorMatrix.setRotate(1, particle.rainbowHue);
                colorMatrix.setRotate(2, particle.rainbowHue);
                trailPaint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            }
            trailPaint.setAlpha(alpha);

            Bitmap trailBitmap = scaledAllBirdBitmaps[particle.colorIndex][particle.frameIndex];
            birdMatrix.reset();
            birdMatrix.postTranslate(-trailBitmap.getWidth() / 2f, -trailBitmap.getHeight() / 2f);
            birdMatrix.postRotate(particle.rotation);
            birdMatrix.postScale(settingBirdSize, settingBirdSize, 0, 0);
            birdMatrix.postTranslate(particle.x, particle.y);
            canvas.drawBitmap(trailBitmap, birdMatrix, trailPaint);
            i++;
        }
    }


    private void drawHomeScreen(Canvas canvas) {
        float titleX = (screenWidth - titleBitmap.getWidth()) / 2f;
        float titleY = getPlayableHeight() * 0.22f + systemBarTop;
        Paint homeHeading=WorldArt.textPaint(22*scale);homeHeading.setColor(0xffedf3f5);
        canvas.drawText("ANÃO DE FIRMA",screenWidth/2f,titleY+35*scale,homeHeading);

        float margin = screenWidth * UI_MARGIN_HORIZONTAL_PERCENT;
        if (settingsButtonBitmap != null) {
            float settingsBtnX = screenWidth - settingsButtonBitmap.getWidth() - margin;
            float settingsBtnY = systemBarTop + margin;
            
            settingsButtonRect.set((int) settingsBtnX, (int) settingsBtnY, 
                (int) (settingsBtnX + settingsButtonBitmap.getWidth()), 
                (int) (settingsBtnY + settingsButtonBitmap.getHeight()));
                
            if (!settingHideSettingsIcon) {
                float centerX = settingsButtonRect.centerX();
                float centerY = settingsButtonRect.centerY();
                canvas.save();
                canvas.scale(settingsButtonScale, settingsButtonScale, centerX, centerY);
                
                if (pressedButtonRect == settingsButtonRect) {
                    pixelPaint.setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY));
                } else {
                    pixelPaint.setColorFilter(null);
                }
                
                canvas.drawBitmap(settingsButtonBitmap, settingsBtnX, settingsBtnY, pixelPaint);
                canvas.restore();
                
                pixelPaint.setColorFilter(null);
            }
        }

        float availableWidth = screenWidth - (2 * margin);
        float originalPlayWidth = playButtonBitmap.getWidth();
        float originalScoreWidth = scoreButtonBitmap.getWidth();
        float originalGap = screenWidth * HOME_BUTTON_GAP_PERCENT;
        float totalIdealWidth = originalPlayWidth + originalScoreWidth + originalGap;

        float scaleCorrection = 1.0f;
        if (totalIdealWidth > availableWidth) scaleCorrection = availableWidth / totalIdealWidth;

        float newPlayWidth = originalPlayWidth * scaleCorrection;
        float newPlayHeight = playButtonBitmap.getHeight() * scaleCorrection;
        float newScoreWidth = originalScoreWidth * scaleCorrection;
        float newScoreHeight = scoreButtonBitmap.getHeight() * scaleCorrection;
        float newGap = originalGap * scaleCorrection;
        float totalNewWidth = newPlayWidth + newScoreWidth + newGap;
        float playBtnX = (screenWidth - totalNewWidth) / 2f;
        float scoreBtnX = playBtnX + newPlayWidth + newGap;
        float buttonsY = titleY + titleBitmap.getHeight() + getPlayableHeight() * 0.18f;
        playButtonRect.set((int) playBtnX, (int) buttonsY, (int) (playBtnX + newPlayWidth), (int) (buttonsY + newPlayHeight));
        scoreButtonRect.set((int) scoreBtnX, (int) buttonsY, (int) (scoreBtnX + newScoreWidth), (int) (buttonsY + newScoreHeight));

        playButtonDestRect.set(playButtonRect);
        scoreButtonDestRect.set(scoreButtonRect);
        float scaledPressOffsetY = pressOffsetY * scaleCorrection;
        if (pressedButtonRect == playButtonRect) playButtonDestRect.offset(0, scaledPressOffsetY);
        if (pressedButtonRect == scoreButtonRect) scoreButtonDestRect.offset(0, scaledPressOffsetY);

        canvas.drawBitmap(playButtonBitmap, null, playButtonDestRect, pixelPaint);
        canvas.drawBitmap(scoreButtonBitmap, null, scoreButtonDestRect, pixelPaint);
        float groundLineY = screenHeight - systemBarBottom - groundHeight;
        WorldArt.brand(canvas, screenWidth / 2f, groundLineY + groundHeight * .46f, scale);
    }

    private void drawGameOverScreen(Canvas canvas) {
        if (gameState == GameState.PANEL_SLIDING || (gameState == GameState.GAME_OVER && birdHitGroundTime > 0)) {
            if (gameState != GameState.PANEL_SLIDING) return;
            float scaleCorrection = this.uiScaleCorrection;

            float gameOverTextWidth = gameOverBitmap.getWidth() * scaleCorrection;
            float gameOverTextHeight = gameOverBitmap.getHeight() * scaleCorrection;
            float gameOverTextX = (screenWidth - gameOverTextWidth) / 2f;
            float gameOverTextY = gameOverElementsY;
            gameOverDestRect.set(gameOverTextX, gameOverTextY, gameOverTextX + gameOverTextWidth, gameOverTextY + gameOverTextHeight);
            canvas.drawBitmap(gameOverBitmap, null, gameOverDestRect, pixelPaint);

            float panelWidth = scorePanelBitmap.getWidth() * scaleCorrection;
            float panelHeight = scorePanelBitmap.getHeight() * scaleCorrection;
            float gap = (20 * scale) * scaleCorrection;
            float panelX = (screenWidth - panelWidth) / 2f;
            float panelY = gameOverTextY + gameOverTextHeight + gap;
            panelDestRect.set(panelX, panelY, panelX + panelWidth, panelY + panelHeight);
            canvas.drawBitmap(scorePanelBitmap, null, panelDestRect, pixelPaint);
            float panelRightEdge = panelX + panelWidth - (22 * scale * scaleCorrection);
            float scoreY = panelY + (SCORE_PANEL_CURRENT_SCORE_Y_OFFSET * scale * scaleCorrection);
            float highScoreY = panelY + (SCORE_PANEL_HIGH_SCORE_Y_OFFSET * scale * scaleCorrection);
            drawScoreWithImagesRightAligned(canvas, displayedScore, panelRightEdge, scoreY, smallNumberBitmaps, scaleCorrection);
            drawScoreWithImagesRightAligned(canvas, highScore, panelRightEdge, highScoreY, smallNumberBitmaps, scaleCorrection);
            
            if (currentMedalBitmap != null && displayedScore == score) {
                float medalWidth = currentMedalBitmap.getWidth() * scaleCorrection;
                float medalHeight = currentMedalBitmap.getHeight() * scaleCorrection;
                float medalX = panelX + (SCORE_PANEL_MEDAL_X_OFFSET * scale * scaleCorrection);
                float medalY = panelY + (SCORE_PANEL_MEDAL_Y_OFFSET * scale * scaleCorrection);
                medalDestRect.set(medalX, medalY, medalX + medalWidth, medalY + medalHeight);
                
                float xOffset = 0;
                float yOffset = 0;

                if (currentMedalBitmap == medalBitmaps[0]) { // Platinum
                    xOffset = PLATINUM_OFFSET_X * scale * scaleCorrection;
                    yOffset = PLATINUM_OFFSET_Y * scale * scaleCorrection;
                } else if (currentMedalBitmap == medalBitmaps[1]) { // Gold
                    xOffset = GOLD_OFFSET_X * scale * scaleCorrection;
                    yOffset = GOLD_OFFSET_Y * scale * scaleCorrection;
                }
                
                medalDestRect.offset(xOffset, yOffset);
                medalX += xOffset;
                medalY += yOffset;

                canvas.drawBitmap(currentMedalBitmap, null, medalDestRect, pixelPaint);
                
                if (isSparkleAnimating && sparkleBitmap != null) {
                    float shineSize = 30 * scale * scaleCorrection * sparkleScaleCurrent; 
                    float centerX = medalX + (medalWidth * sparkleXOffset);
                    float centerY = medalY + (medalHeight * sparkleYOffset);
                    float left = centerX - (shineSize / 2f);
                    float top = centerY - (shineSize / 2f);
                    RectF sparkleDest = new RectF(left, top, left + shineSize, top + shineSize);
                    Paint smoothPaint = new Paint(); 
                    smoothPaint.setFilterBitmap(true);
                    canvas.save();
                    canvas.rotate(sparkleTimer * 100, centerX, centerY);
                    canvas.drawBitmap(sparkleBitmap, null, sparkleDest, smoothPaint);
                    canvas.restore();
                }
            }

            if (displayedScore == score) {
                float playButtonWidth = playButtonBitmap.getWidth() * scaleCorrection;
                float playButtonHeight = playButtonBitmap.getHeight() * scaleCorrection;
                float btnX = (screenWidth - playButtonWidth) / 2f;
                float btnY = panelY + panelHeight + (15 * scale * scaleCorrection);
                playButtonRect.set((int) btnX, (int) btnY, (int) (btnX + playButtonWidth), (int) (btnY + playButtonHeight));

                restartBtnDestRect.set(playButtonRect);
                if (pressedButtonRect == playButtonRect) {
                    restartBtnDestRect.offset(0, pressOffsetY * scaleCorrection);
                }
                canvas.drawBitmap(playButtonBitmap, null, restartBtnDestRect, pixelPaint);
            }

            if (gameOverSettingsIconY != -1 && settingsButtonBitmap != null) {
                float margin = screenWidth * UI_MARGIN_HORIZONTAL_PERCENT;
                float settingsBtnX = screenWidth - settingsButtonBitmap.getWidth() - margin;
                float settingsBtnY = gameOverSettingsIconY;
                settingsButtonRect.set((int) settingsBtnX, (int) settingsBtnY, (int) (settingsBtnX + settingsButtonBitmap.getWidth()), (int) (settingsBtnY + settingsButtonBitmap.getHeight()));
                
                if (!settingHideSettingsIcon) {
                    float centerX = settingsButtonRect.centerX();
                    float centerY = settingsButtonRect.centerY();
                    canvas.save();
                    canvas.scale(settingsButtonScale, settingsButtonScale, centerX, centerY);
                    
                    if (pressedButtonRect == settingsButtonRect) {
                        pixelPaint.setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY));
                    } else {
                        pixelPaint.setColorFilter(null);
                    }
                    
                    canvas.drawBitmap(settingsButtonBitmap, settingsBtnX, settingsBtnY, pixelPaint);
                    canvas.restore();
                    pixelPaint.setColorFilter(null);
                }
            }
        }
    }

    private void drawScoreWithImages(Canvas canvas, int number, int x, int y) {
        if (numberBitmaps[0] == null) return;
        String numStr = String.valueOf(number);
        float totalWidth = 0;
        for (char c : numStr.toCharArray()) {
            int digit = c - '0';
            if (digit >= 0 && digit < 10 && numberBitmaps[digit] != null) {
                totalWidth += numberBitmaps[digit].getWidth();
            }
        }

        float currentX = x - (totalWidth / 2f);
        for (char c : numStr.toCharArray()) {
            int digit = c - '0';
            if (digit >= 0 && digit < 10 && numberBitmaps[digit] != null) {
                Bitmap digitBitmap = numberBitmaps[digit];
                canvas.drawBitmap(digitBitmap, currentX, y - (digitBitmap.getHeight() / 2f), pixelPaint);
                currentX += digitBitmap.getWidth();
            }
        }
    }

    private void drawScoreWithImagesRightAligned(Canvas canvas, int number, float rightX, float y, Bitmap[] numberSet, float scaleCorrection) {
        if (numberSet[0] == null) return;
        float currentX = rightX;
        String numStr = String.valueOf(number);

        for (int i = numStr.length() - 1; i >= 0; i--) {
            int digit = numStr.charAt(i) - '0';
            if (digit >= 0 && digit < 10 && numberSet[digit] != null) {
                Bitmap digitBitmap = numberSet[digit];
                float digitWidth = digitBitmap.getWidth() * scaleCorrection;
                float digitHeight = digitBitmap.getHeight() * scaleCorrection;
                currentX -= digitWidth;
                scoreDigitDestRect.set(currentX, y - digitHeight, currentX + digitWidth, y);
                canvas.drawBitmap(digitBitmap, null, scoreDigitDestRect, pixelPaint);
            }
        }
    }

    private void drawCenteredBitmap(Canvas canvas, Bitmap bitmap, int yOffset) {
        float scaleCorrection = 1.0f;
        float margin = screenWidth * UI_MARGIN_HORIZONTAL_PERCENT;
        if (bitmap.getWidth() > (screenWidth - (2 * margin))) {
            scaleCorrection = (screenWidth - (2 * margin)) / bitmap.getWidth();
        }

        float newWidth = bitmap.getWidth() * scaleCorrection;
        float newHeight = bitmap.getHeight() * scaleCorrection;
        float left = (screenWidth - newWidth) / 2f;
        float groundY = screenHeight - systemBarBottom - groundHeight;
        float top = (systemBarTop + (groundY - systemBarTop) / 2f) - (newHeight / 2f) + (yOffset * scaleCorrection);
        centeredBitmapDestRect.set(left, top, left + newWidth, top + newHeight);
        canvas.drawBitmap(bitmap, null, centeredBitmapDestRect, pixelPaint);
    }

    private void showAspectRatioWarning() {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Sobre esta tela")
                .setMessage("O jogo pode apresentar diferenças visuais nesta proporção de tela. A experiência é melhor em um celular no modo retrato.")
                .setPositiveButton("Continuar", (dialog, which) -> {
                    prefs.edit().putBoolean(PREF_KEY_WARNING_SHOWN, true).apply();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private Bitmap extract(Bitmap source, int x, int y, int w, int h) { return Bitmap.createBitmap(source, x, y, w, h);
    }
    private int loc(double coord, int dim) { return (int) (coord * dim);
    }
    private void extractBitmapsFromAtlas() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        unscaledBgDay = preparedAssets.day;
        unscaledBgNight = preparedAssets.night;
        unscaledLand = preparedAssets.ground;

        unscaledPipeGreen = preparedAssets.walls[0];

        Bitmap[] characters = preparedAssets.characters;
        for (int color = 0; color < 3; color++) {
            for (int frame = 0; frame < 3; frame++) unscaledAllBirdBitmaps[color][frame] = characters[color];
        }
        unscaledTitle = BrandArt.menuLogo(getResources());
        unscaledButtonScore = WorldArt.button("PEÇAS", "RECORDE");
        unscaledTextReady = WorldArt.message(false);
        unscaledTextGameOver = WorldArt.message(true);
        unscaledScorePanel = WorldArt.panel();
        unscaledButtonPlay = WorldArt.button("FIRMA", "PLAY");

        int settingsResId = getResources().getIdentifier("settingsbutton", "drawable", getContext().getPackageName());
        if (settingsResId != 0) unscaledButtonSettings = BitmapFactory.decodeResource(getResources(), settingsResId, options);
        if (unscaledButtonSettings == null) Log.w("GameView", "Could not load 'settingsbutton.png'.");
        
        for (int tier=0; tier<4; tier++) unscaledMedalsBitmaps[tier] = preparedAssets.medals[tier];
        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("number_" + i, "drawable", getContext().getPackageName());
            if (resId != 0) unscaledNumberBitmaps[i] = BitmapFactory.decodeResource(getResources(), resId, options);
            else { Log.e("GameView", "Missing number resource: number_" + i);
                unscaledNumberBitmaps[i] = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); }
        }
    }
    private Bitmap scaleBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int newW = (int) (bitmap.getWidth() * scale), newH = (int) (bitmap.getHeight() * scale);
        if (newW <= 0 || newH <= 0) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        return Bitmap.createScaledBitmap(bitmap, newW, newH, false);
    }
    private void scaleAllBitmaps() {
        float bgScale = (float) screenHeight / unscaledBgDay.getHeight();
        int bgWidth = (int) (unscaledBgDay.getWidth() * bgScale);
        bgDayBitmap = Bitmap.createScaledBitmap(unscaledBgDay, bgWidth, screenHeight, false);
        bgNightBitmap = Bitmap.createScaledBitmap(unscaledBgNight, bgWidth, screenHeight, false);
        groundBitmap = scaleBitmap(unscaledLand);

        pipeUpBitmap = scaleBitmap(unscaledPipeGreen);
        Matrix flipMatrix = new Matrix();
        flipMatrix.setScale(1, -1);
        pipeDownBitmap = Bitmap.createBitmap(pipeUpBitmap, 0, 0, pipeUpBitmap.getWidth(), pipeUpBitmap.getHeight(), flipMatrix, true);
        wallBottoms[0] = pipeUpBitmap;
        wallTops[0] = pipeDownBitmap;
        for (int style = 1; style < WorldArt.WALL_STYLES; style++) {
            Bitmap nativeWall = preparedAssets.walls[style];
            wallBottoms[style] = scaleBitmap(nativeWall);
            wallTops[style] = Bitmap.createBitmap(wallBottoms[style], 0, 0,
                    wallBottoms[style].getWidth(), wallBottoms[style].getHeight(), flipMatrix, false);
        }


        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) scaledAllBirdBitmaps[i][j] = scaleBitmap(unscaledAllBirdBitmaps[i][j]);
        for (int i=0; i<4; i++) medalBitmaps[i] = scaleBitmap(unscaledMedalsBitmaps[i]);
        for (int i = 0; i < 10; i++) numberBitmaps[i] = scaleBitmap(unscaledNumberBitmaps[i]);
        for (int i = 0; i < 10; i++) {
            Bitmap unscaled = unscaledNumberBitmaps[i];
            if (unscaled != null) {
                int newW = (int) (unscaled.getWidth() * scale * SCORE_PANEL_NUMBER_SCALE_MULTIPLIER);
                int newH = (int) (unscaled.getHeight() * scale * SCORE_PANEL_NUMBER_SCALE_MULTIPLIER);
                if (newW > 0 && newH > 0) smallNumberBitmaps[i] = Bitmap.createScaledBitmap(unscaled, newW, newH, false);
                else smallNumberBitmaps[i] = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
        }
        titleBitmap = scaleBitmap(unscaledTitle);
        scoreButtonBitmap = scaleBitmap(unscaledButtonScore); getReadyBitmap = scaleBitmap(unscaledTextReady);
        gameOverBitmap = scaleBitmap(unscaledTextGameOver); scorePanelBitmap = scaleBitmap(unscaledScorePanel);
        playButtonBitmap = scaleBitmap(unscaledButtonPlay);
        if (unscaledButtonSettings != null) {
            int newW = (int) (unscaledButtonSettings.getWidth() * scale * SETTINGS_BUTTON_SCALE_MULTIPLIER);
            int newH = (int) (unscaledButtonSettings.getHeight() * scale * SETTINGS_BUTTON_SCALE_MULTIPLIER);
            if (newW > 0 && newH > 0) settingsButtonBitmap = Bitmap.createScaledBitmap(unscaledButtonSettings, newW, newH, false);
        }
    }
    private void loadSounds() {
        AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        soundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(aa).build();
        soundWing = soundPool.load(getContext(), R.raw.wing, 1);
        soundPoint = soundPool.load(getContext(), R.raw.point, 1);
        soundHit = soundPool.load(getContext(), R.raw.hit, 1);
        soundShot = soundPool.load(getContext(), R.raw.falling_burst, 1);
        fallingGunfire = new FallingGunfire(soundPool, soundShot);
        soundAura = soundPool.load(getContext(), R.raw.aura_loop, 1);
        soundAuraActivate = soundPool.load(getContext(), R.raw.aura_activate, 1);
        soundBazooka = soundPool.load(getContext(), R.raw.wall_shot, 1);
        soundDie = soundPool.load(getContext(), R.raw.die, 1);
        soundSwooshing = soundPool.load(getContext(), R.raw.swooshing, 1);
        soundThunder = soundPool.load(getContext(), R.raw.thunder, 1);

        rainSoundPlayer = MediaPlayer.create(getContext(), R.raw.rain);
        if (rainSoundPlayer != null) {
            rainSoundPlayer.setLooping(true);
        }

        stormSoundPlayer = MediaPlayer.create(getContext(), R.raw.storm_backtrack);
        if (stormSoundPlayer != null) {
            stormSoundPlayer.setLooping(true);
        }
    }

    private void updateWeatherSounds() {
        if (!settingSoundEnabled) {
            stopWeatherSounds();
            return;
        }

        boolean shouldPlayRain = (settingWeatherEffect == 1 || settingWeatherEffect == 2) && (gameState == GameState.PLAYING || gameState == GameState.WAITING);
        boolean shouldPlayStorm = settingWeatherEffect == 2 && (gameState == GameState.PLAYING || gameState == GameState.WAITING);

        try {
            if (shouldPlayRain && rainSoundPlayer != null && !rainSoundPlayer.isPlaying()) {
                rainSoundPlayer.start();
            } else if (!shouldPlayRain && rainSoundPlayer != null && rainSoundPlayer.isPlaying()) {
                rainSoundPlayer.pause();
            }

            if (shouldPlayStorm && stormSoundPlayer != null && !stormSoundPlayer.isPlaying()) {
                stormSoundPlayer.start();
            } else if (!shouldPlayStorm && stormSoundPlayer != null && stormSoundPlayer.isPlaying()) {
                stormSoundPlayer.pause();
            }
        } catch (IllegalStateException e) {
            Log.e("GameView", "MediaPlayer state error in updateWeatherSounds", e);
        }
    }

    private void stopWeatherSounds() {
        try {
            if (rainSoundPlayer != null && rainSoundPlayer.isPlaying()) {
                rainSoundPlayer.pause();
            }
            if (stormSoundPlayer != null && stormSoundPlayer.isPlaying()) {
                stormSoundPlayer.pause();
            }
        } catch (IllegalStateException e) {
            Log.e("GameView", "MediaPlayer state error on stopWeatherSounds", e);
        }
    }

    private void playSound(final int soundID) {
        if (!settingSoundEnabled || !isRunning) return;
        if (soundPool != null && soundID != 0 && soundExecutor != null && !soundExecutor.isShutdown()) {
            final SoundPool activePool = soundPool;
            soundExecutor.submit(() -> {
                if (isRunning && activePool == soundPool) activePool.play(soundID, 1, 1, 0, 0, 1);
            });
        }
    }
    public void pause() {
        secretGestureTouch.cancel(); waitingTouch=false; isScreenPressed=false; pressedButtonRect=null;
        stopAuraSound();
        if (fallingGunfire != null) fallingGunfire.pause();
        if (retroAudio != null) retroAudio.pause();
        isRunning = false;
        choreographer.removeFrameCallback(this);
        stopWeatherSounds();
    }

    public void resume() {
        if (getContext() instanceof MainActivity && !((MainActivity)getContext()).isForeground()) return;
        if (!isRunning && isReady) {
            loadSettings();
            if (gameState == GameState.HOME || gameState == GameState.PANEL_SLIDING) {
                resetGame();
                gameState = GameState.HOME;
            } else {
                updateWeatherSounds();
            }
            isRunning = true;
            syncAuraSound();
            if (fallingGunfire != null) fallingGunfire.resume(settingSoundEnabled);
            if (retroAudio != null) retroAudio.resume(prefs);
            lastFrameTimeNanos = 0;
            choreographer.postFrameCallback(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();
        if (retroAudio != null) { retroAudio.release(); retroAudio = null; }
        if (soundExecutor != null) { soundExecutor.shutdownNow(); soundExecutor = null;
        }
        if (soundPool != null) { soundPool.release(); soundPool = null;
        }
        if (rainSoundPlayer != null) {
            rainSoundPlayer.release();
            rainSoundPlayer = null;
        }
        if (stormSoundPlayer != null) {
            stormSoundPlayer.release();
            stormSoundPlayer = null;
        }
    }
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isRunning) return;
        if (lastFrameTimeNanos == 0) { lastFrameTimeNanos = frameTimeNanos; choreographer.postFrameCallback(this); return;
        }
        float deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0f;
        lastFrameTimeNanos = frameTimeNanos;
        if (gameState == GameState.PLAYING) {
            if(retroAudio!=null)retroAudio.setNightShift(score>=40);
            powerUps.tick(deltaTime);
            celebrations.tick(Math.min(deltaTime,.05f));
            factoryTraffic.tick(Math.min(deltaTime,.05f));
            skyTraffic.tick(deltaTime,settingReversePipesEnabled);
        }
        syncAuraSound();
        if (deltaTime > (1.0f / 30.0f)) deltaTime = (1.0f / 30.0f);
        update(deltaTime); invalidate(); choreographer.postFrameCallback(this);
    }

    // --- INNER CLASSES FOR EFFECTS ---

    static class TrailParticle {
        float x, y, rotation, rainbowHue;
        int colorIndex, frameIndex;
        float lifetime = 0.5f;

        TrailParticle(float x, float y, float rotation, int colorIndex, int frameIndex, float rainbowHue) {
            this.x = x; this.y = y; this.rotation = rotation;
            this.colorIndex = colorIndex; this.frameIndex = frameIndex;
            this.rainbowHue = rainbowHue;
        }
        void update(float deltaTime) {
            lifetime -= deltaTime;
        }
    }

    static class SmokeParticle {
        float x, y, radius, alpha;
        float speedX, speedY;
        float expansionRate;
        float maxLife, life;
        Paint particlePaint;

        SmokeParticle(float startX, float startY, float scale) {
            Random r = new Random();
            x = startX + (r.nextFloat() - 0.5f) * 40 * scale;
            y = startY + (r.nextFloat() - 0.5f) * 40 * scale;
            radius = (10 + r.nextFloat() * 10) * scale;
            alpha = 0.8f; 
            
            speedY = -(30 + r.nextFloat() * 40) * scale; 
            speedX = -(50 + r.nextFloat() * 30) * scale; 
            
            expansionRate = 25 * scale;
            maxLife = 1.0f + r.nextFloat() * 0.5f;
            life = maxLife;
            
            particlePaint = new Paint();
            particlePaint.setAntiAlias(true);
            
            updateShader();
        }
        
        private void updateShader() {
            RadialGradient gradient = new RadialGradient(x, y, radius,
                new int[] { Color.argb((int)(alpha * 255), 60, 60, 60), Color.TRANSPARENT },
                null, Shader.TileMode.CLAMP);
            particlePaint.setShader(gradient);
        }

        void update(float dt, float windSpeed) {
            life -= dt;
            x -= windSpeed * dt; 
            x += speedX * dt;
            y += speedY * dt;
            radius += expansionRate * dt;
            alpha = Math.max(0, life / maxLife);
            updateShader();
        }
        
        void draw(Canvas canvas) {
             canvas.drawCircle(x, y, radius, particlePaint);
        }
        
        boolean isDead() { return life <= 0; }
    }
    
    static class SparkParticle {
        float x, y;
        float vx, vy;
        float life, maxLife;
        int color;
        int type; 
        
        SparkParticle(float startX, float startY, float scale, int type) {
            Random r = new Random();
            this.type = type;
            this.x = startX + (r.nextFloat() - 0.5f) * 60 * scale;
            this.y = startY + (r.nextFloat() - 0.5f) * 60 * scale;
            
            if (type == 0) { // Zap
                this.vx = (r.nextFloat() - 0.5f) * 50 * scale;
                this.vy = (r.nextFloat() - 0.5f) * 50 * scale;
                this.maxLife = 0.1f + r.nextFloat() * 0.1f;
            } else { // Fall
                this.vx = (r.nextFloat() - 0.5f) * 200 * scale;
                this.vy = -(100 + r.nextFloat() * 100) * scale; 
                this.maxLife = 0.5f + r.nextFloat() * 0.5f;
            }
            
            this.life = maxLife;
            this.color = r.nextBoolean() ? Color.WHITE : Color.CYAN;
        }
        
        void update(float dt, float wind) {
            life -= dt;
            x += vx * dt;
            y += vy * dt;
            x -= wind * dt;
            
            if (type == 1) {
                vy += 800f * dt; // Gravity
            }
        }
        
        void draw(Canvas c, Paint p) {
            int alpha = (int)((life / maxLife) * 255);
            p.setColor(color);
            p.setAlpha(alpha);
            c.drawPoint(x, y, p);
        }
        
        boolean isDead() { return life <= 0; }
    }

    class WeatherSystem {
        private WeatherParticle[] frontParticles;
        private WeatherParticle[] backParticles;
        private List<LightningBolt> bolts = new ArrayList<>();
        private Random random = new Random();
        
        private Paint frontPaint, backPaint;
        private Paint lightningPaint, lightningGlowPaint;
        private Paint stormVignettePaint; 
        private Paint flashTintPaint; 
        private Paint vignettePaint;  
        
        private int currentWeatherType = 0;
        private float lightningIntensity = 0f;
        private float lightningDecay = 4.0f; 
        
        private boolean heavyRain = false;
        
        private int width, height;

        public WeatherSystem() {
            frontPaint = new Paint();
            frontPaint.setColor(Color.WHITE);
            frontPaint.setAntiAlias(true);
            
            backPaint = new Paint();
            backPaint.setColor(Color.WHITE);
            backPaint.setAlpha(100);
            backPaint.setAntiAlias(true);
            
            lightningPaint = new Paint();
            lightningPaint.setColor(Color.WHITE);
            lightningPaint.setStyle(Paint.Style.STROKE);
            lightningPaint.setStrokeCap(Paint.Cap.ROUND);
            lightningPaint.setStrokeJoin(Paint.Join.ROUND);
            lightningPaint.setAntiAlias(true);

            lightningGlowPaint = new Paint(lightningPaint);
            lightningGlowPaint.setColor(Color.argb(255, 200, 230, 255));
            lightningGlowPaint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));
            
            stormVignettePaint = new Paint();
            stormVignettePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            
            flashTintPaint = new Paint();
            flashTintPaint.setColor(Color.WHITE);
            flashTintPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            
            vignettePaint = new Paint();
            vignettePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        }

        public void init(int width, int height, float scale) {
            this.width = width;
            this.height = height;
            
            lightningPaint.setStrokeWidth(4 * scale);
            lightningGlowPaint.setStrokeWidth(20 * scale);
            
            int frontCount = 150;
            int backCount = 150;
            
            frontParticles = new WeatherParticle[frontCount];
            backParticles = new WeatherParticle[backCount];
            
            for(int i=0; i<frontCount; i++) frontParticles[i] = new WeatherParticle(width, height, true);
            for(int i=0; i<backCount; i++) backParticles[i] = new WeatherParticle(width, height, false);

            RadialGradient snowVignette = new RadialGradient(
                width / 2f, height / 2f, width * 1.3f,
                new int[]{Color.TRANSPARENT, Color.argb(200, 230, 230, 250)},
                new float[]{0.3f, 1.0f},
                Shader.TileMode.CLAMP
            );
            vignettePaint.setShader(snowVignette);

            RadialGradient stormVignette = new RadialGradient(
                width / 2f, height / 2f, width * 1.3f,
                new int[]{Color.TRANSPARENT, Color.argb(200, 0, 0, 20)},
                new float[]{0.3f, 1.0f},
                Shader.TileMode.CLAMP
            );
            stormVignettePaint.setShader(stormVignette);
        }
        
        public void setHeavyRain(boolean enable) {
            this.heavyRain = enable;
        }
        
        public void setWeatherType(int type) {
            this.currentWeatherType = type;
            if(frontParticles != null) for(WeatherParticle p : frontParticles) p.respawn(width, height, type);
            if(backParticles != null) for(WeatherParticle p : backParticles) p.respawn(width, height, type);
        }

        public void triggerLightning(int width, int height) {
            bolts.add(new LightningBolt(random.nextFloat() * width, -50, width, height * 0.8f, -1, -1));
            lightningIntensity = 1.0f; 
        }
        
        public void triggerTargetedLightning(float targetX, float targetY, int width, int height) {
            bolts.add(new LightningBolt(targetX, -50, width, height, targetX, targetY));
            lightningIntensity = 1.0f;
        }
        
        public void triggerMultiDirectionalBolts(float targetX, float targetY, int width, int height) {
            bolts.add(new LightningBolt(targetX, -50, width, height, targetX, targetY));
            bolts.add(new LightningBolt(0, -50, width, height, targetX, targetY));
            bolts.add(new LightningBolt(width, -50, width, height, targetX, targetY));
            bolts.add(new LightningBolt(-50, height * 0.2f, width, height, targetX, targetY));
            bolts.add(new LightningBolt(width + 50, height * 0.2f, width, height, targetX, targetY));
            lightningIntensity = 1.0f;
        }

        public void update(float deltaTime, float windSpeed, float birdVelocity, Rect birdRect) {
            float backWind = windSpeed * 0.5f; 
            for(WeatherParticle p : frontParticles) p.update(deltaTime, width, height, currentWeatherType, windSpeed, birdRect, heavyRain);
            for(WeatherParticle p : backParticles) p.update(deltaTime, width, height, currentWeatherType, backWind, birdRect, heavyRain);
            
            Iterator<LightningBolt> iter = bolts.iterator();
            while(iter.hasNext()) {
                LightningBolt bolt = iter.next();
                bolt.update(deltaTime);
                if(bolt.isDead()) iter.remove();
            }
            
            if (lightningIntensity > 0) {
                lightningIntensity -= lightningDecay * deltaTime;
                if (lightningIntensity < 0) lightningIntensity = 0;
            }
        }

        public void drawBack(Canvas canvas) {
            for(WeatherParticle p : backParticles) p.draw(canvas, backPaint, currentWeatherType, heavyRain);
        }
        
        public void drawFront(Canvas canvas) {
            for(WeatherParticle p : frontParticles) p.draw(canvas, frontPaint, currentWeatherType, heavyRain);
        }
        
        public void drawLightingOverlay(Canvas canvas, int width, int height) {
            int overscan = 100; 
            if (currentWeatherType == 2) {
                canvas.drawRect(-overscan, -overscan, width + overscan, height + overscan, stormVignettePaint);
            } else if (currentWeatherType == 3) {
                 canvas.drawRect(-overscan, -overscan, width + overscan, height + overscan, vignettePaint);
            }
            
            for(LightningBolt bolt : bolts) {
                bolt.draw(canvas, lightningPaint, lightningGlowPaint);
            }
            
            if (lightningIntensity > 0) {
                int alpha = (int)(lightningIntensity * 100); 
                flashTintPaint.setAlpha(alpha);
                canvas.drawRect(-overscan, -overscan, width + overscan, height + overscan, flashTintPaint);
            }
        }
    }

    class LightningBolt {
        private Path path = new Path();
        private float life = 0.25f;
        private float maxLife = 0.25f;
        private float alpha = 1.0f;

        public LightningBolt(float startX, float startY, float width, float height, float targetX, float targetY) {
            if (targetX != -1) {
                generateTargeted(startX, startY, targetX, targetY);
            } else {
                generate(startX, startY, height);
            }
        }

        private void generate(float x, float y, float targetHeight) {
            path.reset();
            path.moveTo(x, y);
            float currentX = x;
            float currentY = y;
            Random r = new Random();
            
            while(currentY < targetHeight) {
                float segmentLen = 30 + r.nextFloat() * 50;
                currentY += segmentLen;
                currentX += (r.nextFloat() - 0.5f) * 80;
                path.lineTo(currentX, currentY);
                if (r.nextBoolean()) {
                    float branchX = currentX + (r.nextFloat() - 0.5f) * 100;
                    float branchY = currentY + (50 + r.nextFloat() * 50);
                    path.lineTo(branchX, branchY);
                    path.moveTo(currentX, currentY);
                }
            }
        }
        
        private void generateTargeted(float startX, float startY, float endX, float endY) {
            path.reset();
            path.moveTo(startX, startY);
            
            float currentX = startX;
            float currentY = startY;
            float distX = endX - startX;
            float distY = endY - startY;
            int segments = 8;
            float stepX = distX / segments;
            float stepY = distY / segments;
            
            Random r = new Random();
            
            for(int i=0; i<segments; i++) {
                currentX += stepX;
                currentY += stepY;
                float jitterX = (r.nextFloat() - 0.5f) * 100;
                float jitterY = (r.nextFloat() - 0.5f) * 50;
                if (i == segments -1) {
                    currentX = endX;
                    currentY = endY;
                } else {
                    currentX += jitterX;
                    currentY += jitterY;
                }
                path.lineTo(currentX, currentY);
            }
        }

        public void update(float dt) {
            life -= dt;
            alpha = life / maxLife;
        }

        public boolean isDead() { return life <= 0; }

        public void draw(Canvas c, Paint core, Paint glow) {
            int a = (int)(alpha * 255);
            if (a > 255) a = 255; if (a < 0) a = 0;
            glow.setAlpha(a);
            core.setAlpha(a);
            c.drawPath(path, glow);
            c.drawPath(path, core);
        }
    }

    static class WeatherParticle {
        float x, y, z; 
        float ySpeed, size, length;
        float offset; 
        float bounceX = 0, bounceY = 0;
        private static Random random = new Random();

        WeatherParticle(int screenWidth, int screenHeight, boolean isFront) {
            if (isFront) z = 1.0f + random.nextFloat() * 0.5f;
            else z = 0.5f + random.nextFloat() * 0.5f;
            respawn(screenWidth, screenHeight, 0);
            y = random.nextFloat() * screenHeight;
        }

        void respawn(int screenWidth, int screenHeight, int weatherType) {
            x = random.nextFloat() * screenWidth;
            y = -50 - random.nextFloat() * 50;
            bounceX = 0;
            bounceY = 0;
            offset = random.nextFloat() * 100f;
            
            if (weatherType == 3) { // Snow
                size = (2f + random.nextFloat() * 4f) * z;
                ySpeed = (30f + random.nextFloat() * 40f) * z;
            } else { // Rain
                size = (2.0f + random.nextFloat()) * z; 
                length = (30f + random.nextFloat() * 40f) * z; 
                ySpeed = (700f + random.nextFloat() * 300f) * z;
            }
        }

        void update(float deltaTime, int screenWidth, int screenHeight, int weatherType, float windSpeed, Rect birdRect, boolean heavy) {
            float speedMult = heavy ? 2.5f : 1.0f;
            y += ySpeed * speedMult * deltaTime;
            x += bounceX * deltaTime;
            y += bounceY * deltaTime;
            bounceX *= 0.90f; 
            bounceY *= 0.90f; 

            if (weatherType == 3) { // Snow
                float drift = (float)Math.sin(y * 0.01f + offset) * 50f * z;
                x += (windSpeed * 0.1f * z + drift) * deltaTime;
                if (birdRect.contains((int)x, (int)y)) {
                    float dx = x - birdRect.centerX();
                    float dy = y - birdRect.centerY();
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist < 1) dist = 1;
                    bounceX = (dx / dist) * 200f;
                    bounceY = -Math.abs(ySpeed) * 0.8f; 
                    x += (dx/dist) * 5f;
                    y += (dy/dist) * 5f;
                }
            } else { // Rain
                x -= windSpeed * z * deltaTime;
            }

            if (y > screenHeight + length || x < -100 || x > screenWidth + 100) {
                respawn(screenWidth, screenHeight, weatherType);
            }
        }

        void draw(Canvas canvas, Paint paint, int weatherType, boolean heavy) {
            if (weatherType == 3) { // Snow
                int alpha = (int)(180 * z); 
                if (alpha > 255) alpha = 255;
                paint.setAlpha(alpha);
                canvas.drawCircle(x, y, size, paint);
            } else { // Rain
                int alpha = (int)(160 * z); 
                if (alpha > 255) alpha = 255;
                paint.setAlpha(alpha);
                paint.setStrokeWidth(size);
                float run = -20f * z; 
                float rise = length * (heavy ? 1.5f : 1.0f);
                canvas.drawLine(x, y, x + run, y + rise, paint);
                if (heavy) {
                    canvas.drawLine(x + 50, y + 50, x + 50 + run, y + 50 + rise, paint);
                    canvas.drawLine(x - 30, y - 70, x - 30 + run, y - 70 + rise, paint);
                }
            }
        }
    }
}

class Pipe {
    float x; int topPipeY; boolean isScored; int width, height;
    final WallStatus status = new WallStatus();
    private static Random random = new Random();
    public Paint pipePaint;
    public boolean isGolden;
    public int wallStyle;

    private Rect topHeadRect = new Rect(), topBodyRect = new Rect();
    private Rect bottomHeadRect = new Rect(), bottomBodyRect = new Rect();

    private static float pipeHeadWidth, pipeHeadHeight;
    private static float pipeBodyWidth, pipeBodyOffsetX;
    private float currentYOffset = 0;
    private final SafePipeMotion factoryMotion=new SafePipeMotion();
    public Pipe(float x, int width, int height) {
        this.x = x; this.width = width;
        this.height = height; this.isScored = false;
        this.isGolden = false;
        this.pipePaint = new Paint();
        this.pipePaint.setFilterBitmap(false);
        this.pipePaint.setAntiAlias(false);
    }

    public static void initHitboxDimensions(float scale) {
        final int UNSCALED_HEAD_WIDTH = 52;
        final int UNSCALED_HEAD_HEIGHT = 24;
        final int UNSCALED_BODY_WIDTH = 52;

        pipeHeadWidth = UNSCALED_HEAD_WIDTH * scale;
        pipeHeadHeight = UNSCALED_HEAD_HEIGHT * scale;
        pipeBodyWidth = UNSCALED_BODY_WIDTH * scale;
        pipeBodyOffsetX = (pipeHeadWidth - pipeBodyWidth) / 2.0f;
    }

    public void resetHeight(int pipeGap, int playAreaHeight, int topOffset, float variationMultiplier) {
        status.reset();
        wallStyle = random.nextInt(WorldArt.WALL_STYLES);
        float baseMarginPercent = 0.08f;
        float effectiveVariation = Math.max(0.001f, variationMultiplier);
        float marginPercent = Math.max(0.0f, Math.min(0.49f, baseMarginPercent / effectiveVariation));

        int margin = (int) (playAreaHeight * marginPercent);
        int availableRange = playAreaHeight - (2 * margin) - pipeGap;
        if (availableRange <= 0) {
            this.topPipeY = (playAreaHeight / 2) + topOffset - pipeGap / 2;
        } else {
            this.topPipeY = random.nextInt(availableRange) + margin + topOffset;
        }

        this.currentYOffset = 0;factoryMotion.reset();
    }

    void updateFactoryMotion(int score,float dt,float top,float bottom,int gap,boolean near,float scale) {
        currentYOffset=factoryMotion.tick(score,dt,top-topPipeY,bottom-gap-topPipeY,near,scale);
    }
    public void updateAnimation(boolean isMoving, float animationCounter, float moveRange, boolean isStopping) {
        if (isStopping) { currentYOffset *= 0.9f;
            if (Math.abs(currentYOffset) < 0.1f) currentYOffset = 0; }
        else if (isMoving) { float phaseShift = (this.x / 500f) * (float)Math.PI;
            this.currentYOffset = ((float) Math.sin(animationCounter + phaseShift) * moveRange); }
        else { currentYOffset = 0;
        }
    }
    public boolean isAtRest() { return currentYOffset == 0;
    }

    private float getCurrentTopPipeY() { return topPipeY;
    }
    float textureOffset() {return currentYOffset;}
    public float getTopPipeY() { return getCurrentTopPipeY() - this.height;
    }
    public float getBottomPipeY(int pipeGap) { return getCurrentTopPipeY() + pipeGap;
    }

    public void setColorFilter(ColorFilter filter) {
        this.pipePaint.setColorFilter(filter);
    }

    public Rect getTopHeadRect(float widthMultiplier) {
        float top = getCurrentTopPipeY() - pipeHeadHeight;
        float bottom = getCurrentTopPipeY();
        float visualWidth = this.width * widthMultiplier;
        float xPos = x + (this.width - visualWidth) / 2.0f;
        topHeadRect.set((int) xPos, (int) top, (int) (xPos + visualWidth), (int) bottom);
        return topHeadRect;
    }

    public Rect getTopBodyRect(float widthMultiplier) {
        float top = -20000f; 
        float bottom = getCurrentTopPipeY() - pipeHeadHeight;
        float visualWidth = pipeBodyWidth * widthMultiplier;
        float xPos = x + pipeBodyOffsetX + (pipeBodyWidth - visualWidth) / 2.0f;
        topBodyRect.set(
                (int) xPos,
                (int) top,
                (int) (xPos + visualWidth),
                (int) bottom
        );
        return topBodyRect;
    }

    public Rect getBottomHeadRect(int pipeGap, float widthMultiplier) {
        float top = getBottomPipeY(pipeGap);
        float bottom = top + pipeHeadHeight;
        float visualWidth = this.width * widthMultiplier;
        float xPos = x + (this.width - visualWidth) / 2.0f;
        bottomHeadRect.set((int) xPos, (int) top, (int) (xPos + visualWidth), (int) bottom);
        return bottomHeadRect;
    }

    public Rect getBottomBodyRect(int pipeGap, float widthMultiplier) {
        float top = getBottomPipeY(pipeGap) + pipeHeadHeight;
        float bottom = top + (this.height - pipeHeadHeight);
        float visualWidth = pipeBodyWidth * widthMultiplier;
        float xPos = x + pipeBodyOffsetX + (pipeBodyWidth - visualWidth) / 2.0f;
        bottomBodyRect.set(
                (int) xPos,
                (int) top,
                (int) (xPos + visualWidth),
                (int) bottom
        );
        return bottomBodyRect;
    }
}
