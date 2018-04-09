package cat.flx.plataformes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;

import cat.flx.plataformes.characters.Bonk;
import cat.flx.plataformes.characters.Heart;

public class GameEngine {

    private static final int UPDATE_DELAY = 50;             // 50ms             => 20 physics/sec
    private static final int INVALIDATES_PER_UPDATE = 2;    // 2 * 50ms = 100ms => 10 redraws/sec
    private static final int SCALED_HEIGHT = 16 * 16;       // 16 rows of scene (16px each tile)

    private Context context;
    private GameView gameView;
    private BitmapSet bitmapSet;
    private Audio audio;
    private Handler handler;
    private Scene scene;
    private Bonk bonk;
    //private Heart[] heart;
    private Heart heart1;
    private Heart heart2;
    private Heart heart3;
    private Input input;

    Context getContext() { return context; }
    public Bitmap getBitmap(int index) { return bitmapSet.getBitmap(index); }
    public Scene getScene() { return scene; }
    public Input getInput() { return input; }
    public Bonk getBonk() { return bonk; }
    public Audio getAudio() { return audio; }

    GameEngine(Context context, GameView gameView) {
        // Initialize everything
        this.context = context;
        bitmapSet = new BitmapSet(context);
        audio = new Audio(context);

        // Relate to the game view
        this.gameView = gameView;
        gameView.setGameEngine(this);
        input = new Input();

        // Load Scene
        scene = new Scene(this);
        scene.loadFromFile(R.raw.main_scene);

        // Create Bonk
        bonk = new Bonk(this, 0,160);

        //Create Heart 1
        heart1 = new Heart(this,05,10);
        //Create Heart 2
        heart2 = new Heart(this,45,10);
        //Create Heart 3
        heart3 = new Heart(this,85,10);

        // Program the Handler for engine refresh (physics et al)
        handler = new Handler();
        Runnable runnable = new Runnable() {
            private long last = 0;
            private int count = 0;
            @Override public void run() {
                handler.postDelayed(this, UPDATE_DELAY);
                // Delta time between calls
                if (last == 0) last = System.currentTimeMillis();
                long now = System.currentTimeMillis();
                int delta = (int)(now - last);
                last = now;
                physics(delta);
                if (++count % INVALIDATES_PER_UPDATE == 0) {
                    GameEngine.this.gameView.invalidate();
                    count = 0;
                }
            }
        };
        handler.postDelayed(runnable, UPDATE_DELAY);
    }

    // For activity start
    void start() {
        audio.startMusic();
    }

    // For activity stop
    void stop() {
        audio.stopMusic();
    }

    // For activity pause
    void pause() {
        audio.stopMusic();
    }

    // For activity resume
    void resume() {
        audio.startMusic();
    }

    // Attend user input
    boolean onTouchEvent(MotionEvent motionEvent) {
        if (screenHeight * screenWidth == 0) return true;
        int act = motionEvent.getActionMasked();
        int i = motionEvent.getActionIndex();
        boolean down = (act == MotionEvent.ACTION_DOWN) ||
                (act == MotionEvent.ACTION_POINTER_DOWN);
        boolean touching = (act != MotionEvent.ACTION_UP) &&
                (act != MotionEvent.ACTION_POINTER_UP) &&
                (act != MotionEvent.ACTION_CANCEL);
        int x = (int)(motionEvent.getX(i)) * 100 / screenWidth;
        int y = (int)(motionEvent.getY(i)) * 100 / screenHeight;
        if ((y > 75) && (x < 40)) {
            if (!touching) input.stopLR();
            else if (x < 20) input.goLeft();            // LEFT
            else input.goRight();                       // RIGHT
        }
        else if ((y > 75) && (x > 80) ) {
            if (down) input.jump();                     // JUMP
        }
        else {
            if (down) input.pause();                    // DEAD-ZONE
        }

        //GAME OVER TOUCH TO RESTART
        if (act == MotionEvent.ACTION_DOWN && bonk.getState() == 3) {
            bonk = new Bonk(this, 0,140);
            scene.setRemainingHearts(3);
            scene.setScore(0);
            resume();
            //RECARGAR ESCENA
            scene = new Scene(this);
            scene.loadFromFile(R.raw.main_scene);
        }

        return true;
    }

    // Testing with keyboard
    boolean onKeyEvent(KeyEvent keyEvent) {
        boolean down = (keyEvent.getAction() == KeyEvent.ACTION_DOWN);
        if (!down) return true;
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_Z: input.goLeft(); break;
            case KeyEvent.KEYCODE_X: input.goRight(); break;
            case KeyEvent.KEYCODE_M: input.jump(); audio.coin(); break;
            case KeyEvent.KEYCODE_P: input.pause(); break;
            default: return false;
        }
        input.setKeyboard(true);
        return true;
    }

    private Paint paint, paintKeys, paintScore, paintGameOverRectangle, paintGameOverText, paintGameOverAll;
    private int screenWidth, screenHeight, scaledWidth;
    private float scale;

    // Perform physics on all game objects
    private void physics(int delta) {
        // Player physics
        bonk.physics(delta);

        // Other game objects' physics
        scene.physics(delta);

        // ... and update scrolling
        updateOffsets();
    }

    // Update screen offsets to always have Bonk visible
    private int offsetX = 0, offsetY = 0;
    private void updateOffsets() {
        if (scaledWidth * SCALED_HEIGHT == 0) return;
        int x = bonk.getX();
        int y = bonk.getY();

        // OFFSET X (100 scaled-pixels margin)
        offsetX = Math.max(offsetX, x - scaledWidth + 124);     // 100 + Bonk Width (24)
        offsetX = Math.min(offsetX, scene.getWidth() - scaledWidth - 1);
        offsetX = Math.min(offsetX, x - 100);
        offsetX = Math.max(offsetX, 0);

        // OFFSET Y (50 scaled-pixels margin)
        offsetY = Math.max(offsetY, y - SCALED_HEIGHT + 82);     // 50 + Bonk Height (32)
        offsetY = Math.min(offsetY, scene.getHeight() - SCALED_HEIGHT - 1);
        offsetY = Math.min(offsetY, y - 50);
        offsetY = Math.max(offsetY, 0);
    }

    // Screen redraw
    void draw(Canvas canvas) {
        if (scene == null) return;

        // Create painters on first draw
        if (paint == null) {
            paint = new Paint();
            paint.setColor(Color.GRAY);
            paint.setTextSize(10);
            paintKeys = new Paint();
            paintKeys.setColor(Color.argb(20, 0, 0, 0));
            paintScore = new Paint();
            paintScore.setColor(Color.BLACK);
            paintScore.setTextSize(5);

            //GAME OVER
            paintGameOverText = new Paint(paintScore);
            paintGameOverText.setColor(Color.WHITE);
            paintGameOverText.setTextSize(10);
            paintGameOverRectangle = new Paint(paintKeys);
            paintGameOverRectangle.setColor(Color.rgb(0, 0, 0));
            paintGameOverRectangle.setAlpha(170); //Opacity 0 - 255
            paintGameOverAll = new Paint(paintGameOverText);
        }

        // Refresh scale factor if screen has changed sizes
        if (canvas.getWidth() * canvas.getHeight() != screenWidth * screenHeight) {
            screenWidth = canvas.getWidth();
            screenHeight = canvas.getHeight();
            if (screenWidth * screenHeight == 0) return; // 0 px on screen (not fully loaded)
            // New Scaling factor
            scale = (float) screenHeight / SCALED_HEIGHT;
            scaledWidth = (int) (screenWidth / scale);
        }

        // --- FIRST DRAW ROUND (scaled)
        canvas.save();
        canvas.scale(scale, scale);
        canvas.translate(-offsetX, -offsetY);

        // Background
        scene.draw(canvas, offsetX, offsetY, scaledWidth, SCALED_HEIGHT);

        // Player character
        bonk.draw(canvas);

        // --- SECOND DRAW ROUND (no-scaled)
        canvas.restore();

        // Debugging information on screen
        String text = "OX=" + offsetX + " OY=" + offsetY;
        canvas.drawText(text, 0, 20, paint);

        // Translucent keyboard on top
        canvas.scale(scale * scaledWidth / 100, scale * SCALED_HEIGHT / 100);
        canvas.drawRect(1, 76, 19, 99, paintKeys);
        canvas.drawText("«", 8, 92, paint);
        canvas.drawRect(21, 76, 39, 99, paintKeys);
        canvas.drawText("»", 28, 92, paint);
        canvas.drawRect(81, 76, 99, 99, paintKeys);
        canvas.drawText("^", 88, 92, paint);

        //GAME OVER
        if (bonk.getState() == 3){
            stop();
            String gameOver = "GAME OVER";
            //String tryAgain = "Try Again";
            canvas.drawRect(0, 0, 100, 100, paintGameOverRectangle);
            canvas.drawText(gameOver, 50 - paintGameOverAll.measureText(gameOver) / 2, 55, paintGameOverAll);
            //canvas.drawText(tryAgain, 50 - paintGameOverAll.measureText(tryAgain) / 2, 60, paintGameOverAll);
        }

        //SCORE
        canvas.drawText("Score: " + scene.getScore(),70,5, paintScore);
        // HEARTS
        canvas.scale(scale * scaledWidth / 11000, scale * SCALED_HEIGHT / 5000);
        if(scene.getRemainingHearts() == 3) {
            heart1.draw(canvas);
            heart2.draw(canvas);
            heart3.draw(canvas);
        }else if (scene.getRemainingHearts() == 2){
            heart1.draw(canvas);
            heart2.draw(canvas);
        }else if (scene.getRemainingHearts() == 1){
            heart1.draw(canvas);
        }
    }

}
