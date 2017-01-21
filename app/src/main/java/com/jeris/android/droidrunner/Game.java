package com.jeris.android.droidrunner;

import java.util.Random;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;

public class Game {

	// pothole resources
	final int MAX_potholes = 10;
	float MIN_POTHOLE_WIDTH = 100.0f;
	float MAX_POTHOLE_WIDTH = 210.0f;
	Pothole [] potholes;
	
	// keep track of last spawned pothole
	Pothole lastPothole;
	
	long spawnPotholeTime;
	final long SPAWN_POTHOLE_TIME = 750;
	
	// Droid/Player resources
	Droid droid;
	final float groundY = 400;
	final float groundHeight = 20;	
	
	// player input flag
	boolean playerTap;

	// possible game states
	final int GAME_MENU = 0;
	final int GAME_READY = 1;
	final int GAME_PLAY = 2;
	final int GAME_OVER = 3;
	
	int gameState;
	// game menu message

	long tapToStartTime;
	boolean showTapToStart;
	
	// get ready message
	final int SHOW_GET_READY = 0;
	final int SHOW_GO = 1;
	
	long getReadyGoTime;
	int getReadyGoState;
	
	// game over message
	long gameOverTime;	

	// shared paint objects for drawing
	Paint greenPaint;
	Paint clearPaint;
		
	Random rng;
	// display dimensions
	int width;
	int height;
	
	public Game(Context context) {
		// allocate resources needed by game
		greenPaint = new Paint();
		greenPaint.setAntiAlias(true);
		greenPaint.setARGB(255, 0, 255, 0);
		greenPaint.setFakeBoldText(true);		
		greenPaint.setTextSize(42.0f);

		clearPaint = new Paint();
		clearPaint.setARGB(255, 0, 0, 0);
		clearPaint.setAntiAlias(true);
					
		rng = new Random();
		emptyPaint = new Paint();
		
		loadImages(context);
		soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
		loadSounds(context);

		// create game entities
		droid = new Droid(this);
		potholes = new Pothole[MAX_potholes];
		for (int i=0; i<MAX_potholes; i++) {
			potholes[i] = new Pothole(i, this);
		}
		whitePaint = new Paint();
		whitePaint.setAntiAlias(true);
		whitePaint.setARGB(255, 255, 255, 255);
		whitePaint.setFakeBoldText(true);		
		whitePaint.setTextSize(42.0f);
				
		pastry = new Pastry(this);		
		road = new Road(this);
		
		highScore = SCORE_DEFAULT;
		// initialize the game
		resetGame();
	}
	
	public void setScreenSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void run(Canvas canvas) {
		switch (gameState) {
		case GAME_MENU:
			gameMenu(canvas);
			break;
		case GAME_READY:
			gameReady(canvas);
			break;
		case GAME_PLAY:
			gamePlay(canvas);
			break;
		case GAME_OVER:
			gameOver(canvas);
			break;
		case GAME_PAUSE:
			gamePause(canvas);
			break;
		}
	}
	
	public void doTouch() {		
		playerTap = true;
	}

	public void resetGame() {
		tapToStartTime = System.currentTimeMillis();
		showTapToStart = true;		
		playerTap = false;
		droid.reset();
		spawnPotholeTime = System.currentTimeMillis();
		for (Pothole p : potholes) {
			p.reset();
		}
		lastPothole = null;
		gameState = GAME_MENU;
		lastGameState = gameState;
		getReadyGoState = SHOW_GET_READY;
		getReadyGoTime = 0;
		
		curScore = 0;

		pastry.reset();
		spawnPastryTime = System.currentTimeMillis();
		
		road.reset();
	}
	
	public void initGameOver() {
		
		gameState = GAME_OVER;
		gameOverTime = System.currentTimeMillis();
		
		// play droid crash sound
		soundPool.play(droidCrashSnd, 1.0f, 1.0f, 0, 0, 1.0f);
		
		// update high score
		if (curScore > highScore) {
			highScore = curScore;
		}
	}

	private void gameOver(Canvas canvas) {

		// clear screen
		canvas.drawRect(0, 0, width, height, clearPaint);
		
		canvas.drawText("GAME OVER", width/3, height/2, whitePaint);

		long now = System.currentTimeMillis() - gameOverTime;
		if (now > 2000) {
			resetGame();
		}
	}

	private void gamePlay(Canvas canvas) {
		// clear screen
		canvas.drawRect(0, 0, width, height, clearPaint);
		// draw ground
		road.update();
		road.draw(canvas);
		
		for (Pothole p : potholes) {
			if (p.alive) {
				p.update();
				p.draw(canvas);
			}
		}
		
		if (pastry.alive) {
			pastry.update();
			pastry.draw(canvas);
		}
		droid.update();
		droid.draw(canvas);

		spawnPothole();
		spawnPastry();
		
		doScore(canvas);
	}

	private void gameReady(Canvas canvas) {
		
		long now;
		
		// clear screen
		canvas.drawRect(0, 0, width, height, clearPaint);
		
		switch (getReadyGoState) {
		case SHOW_GET_READY:
			canvas.drawText("GET READY", (width/3)-100.0f, height/2, whitePaint);
			now = System.currentTimeMillis() - getReadyGoTime;
			if (now > 3000) {
				getReadyGoTime = System.currentTimeMillis();
				getReadyGoState = SHOW_GO;
			}
			break;
		case SHOW_GO:
			canvas.drawText("GO!", (width/2)-40.0f, height/2, whitePaint);
			now = System.currentTimeMillis() - getReadyGoTime;
			if (now > 500) {				
				gameState = GAME_PLAY;
				
				scoreTime = System.currentTimeMillis();
			}
			break;
		}
		
		// draw blank score
		canvas.drawText("POINTS: 0", 0, 40, whitePaint);
		// draw ground
		road.draw(canvas);
		// draw player
		droid.draw(canvas);					
	}

	private void gameMenu(Canvas canvas) {

		canvas.drawRect(0, 0, width, height, clearPaint);

		canvas.drawText("DROID RUNNER", (width/4)-20.0f, 100.0f, whitePaint);
		
		canvas.drawText("TOP SCORE: " + highScore, (width/4)-50.0f, height/2, whitePaint);

		if (playerTap) {
			gameState = GAME_READY;
			playerTap = false;
			getReadyGoState = SHOW_GET_READY;
			getReadyGoTime = System.currentTimeMillis();

			// spawn 1st chasm so player sees something at start of game
			potholes[0].spawn(0);
			lastPothole = potholes[0];
		}

		long now = System.currentTimeMillis() - tapToStartTime;
		if (now > 550) {
			tapToStartTime = System.currentTimeMillis();
			showTapToStart = !showTapToStart;
		}

		if (showTapToStart) {
			canvas.drawText("TAP TO START", width/4, height-100.0f, whitePaint);
		}			
	}

	public float random(float a) {
		return rng.nextFloat() * a;
	}

	public float random(float a, float b) {		
		return Math.round(a + (rng.nextFloat() * (b - a)));
	}

	void spawnPothole() {
		long now = System.currentTimeMillis() - spawnPotholeTime;

		if (now > SPAWN_POTHOLE_TIME) {

			// randomly determine whether or not to spawn a new pothole
			if ((int)random(10) > 2) {
				// find an available pothole to use
				for (Pothole p : potholes) {					
					if (p.alive) {
						continue;
					}
					// by default all new potholes start just beyond
					// the right side of the display
					
					float xOffset = 0.0f;
					
					/*
					 if the last pothole is alive then use its width to adjust
					 the position of the new pothole if the last pothole
					 is too close to the right of the screen. this is to
					 give the player some breathing room.
					*/
					
					if (lastPothole.alive) {
						
						float tmp = lastPothole.x + lastPothole.w;
						
						if (tmp > width) {
							tmp = tmp - width;
							xOffset = tmp + random(10.0f);
						}
						else {
							tmp = width - tmp;								
							if (tmp < 20.0f) {
								xOffset = tmp + random(10.0f);
							}
						}
					}

					p.spawn(xOffset);						
					lastPothole = p;						
					break;
				}
			}

			spawnPotholeTime = System.currentTimeMillis();
		}
	}
	
	Paint whitePaint;
	Paint emptyPaint;
	
	final int GAME_PAUSE = 4;
	// track time between save games
	long saveGameTime;
	// high score
	int highScore;
	int curScore;
	
	long scoreTime;
	final long SCORE_TIME = 100;
	
	final int SCORE_DEFAULT = 0;
	final int SCORE_INC = 5;
	final int SCORE_PASTRY_BONUS = 200;
	// Pastry
	Pastry pastry;
	long spawnPastryTime;
	final long SPAWN_PASTRY_TIME = 750;
	// the road 
	Road road;
	
	//
	// bitmaps
	//
	Bitmap roadImage;
	Bitmap dividerImage;
	Bitmap pastryImage;
	Bitmap droidJumpImage;
	Bitmap [] droidImages;
	final int MAX_DROID_IMAGES = 4;
	
	//
	// sound
	//
	SoundPool soundPool;
	int droidJumpSnd;
	int droidEatPastrySnd;
	int droidCrashSnd;

	int lastGameState;
	long pauseStartTime;
		
	private void loadSounds(Context context) {
		droidCrashSnd = soundPool.load(context, R.raw.droidcrash, 1);
		droidEatPastrySnd = soundPool.load(context, R.raw.eatpastry, 1);
		droidJumpSnd = soundPool.load(context, R.raw.droidjump, 1);		
	}
	
	private void loadImages(Context context) {
		Resources res = context.getResources();
		
		roadImage = BitmapFactory.decodeResource(res, R.drawable.road);
		dividerImage = BitmapFactory.decodeResource(res, R.drawable.divider);		
		pastryImage = BitmapFactory.decodeResource(res, R.drawable.pastry);		
		droidJumpImage = BitmapFactory.decodeResource(res, R.drawable.droidjump);		
		droidImages = new Bitmap[MAX_DROID_IMAGES];		
		droidImages[0] = BitmapFactory.decodeResource(res, R.drawable.droid0);
		droidImages[1] = BitmapFactory.decodeResource(res, R.drawable.droid1);
		droidImages[2] = BitmapFactory.decodeResource(res, R.drawable.droid2);
		droidImages[3] = BitmapFactory.decodeResource(res, R.drawable.droid3);
	}
	
	private void gamePause(Canvas canvas) {

		// clear screen
		canvas.drawRect(0, 0, width, height, clearPaint);
		
		canvas.drawText("GAME PAUSED", width/4, height/2, whitePaint);
		
		if (playerTap) {			
			playerTap = false;
			gameState = lastGameState;
			
			// determine time elapsed between pause and unpause
			long deltaTime = System.currentTimeMillis() - pauseStartTime;
			
			// adjust timer variables based on elapsed time delta 
			spawnPotholeTime += deltaTime;
			tapToStartTime += deltaTime;
			getReadyGoTime += deltaTime;
			gameOverTime += deltaTime;
			scoreTime += deltaTime;
			spawnPastryTime += deltaTime;
		}
	}
	
	public void pause() {
		
		// if game already paused don't pause it again - otherwise we'll lose the
		// game state and end up in an infinite loop
		if (gameState == GAME_PAUSE) {
			return;
		}
		
		lastGameState = gameState;
		gameState = GAME_PAUSE;
		pauseStartTime = System.currentTimeMillis();
	}
		
	void spawnPastry() {		
		long now = System.currentTimeMillis() - spawnPastryTime;

		if (now > SPAWN_PASTRY_TIME) {
			// randomly determine whether or not to spawn a new pastry
			if ((int)random(10) > 7) {				
				if (!pastry.alive) {				
					pastry.spawn();					
				}				
			}			
			spawnPastryTime = System.currentTimeMillis();
		}
	}
	
	public void doPlayerEatPastry() {
		// play eat pastry sound
		soundPool.play(droidEatPastrySnd, 1.0f, 1.0f, 0, 0, 1.0f);
		
		// increase score
		curScore += SCORE_PASTRY_BONUS;
		
		// reset pastry and spawn time
		pastry.alive = false;
		spawnPastryTime = System.currentTimeMillis();
	}
	
	private void doScore(Canvas canvas) {
		
		// first update current score
		long now = System.currentTimeMillis() - scoreTime;
		
		if (now > SCORE_TIME) {
			curScore += SCORE_INC;
			scoreTime = System.currentTimeMillis();
		}
		
		// now draw it the screen
		StringBuilder buf = new StringBuilder("POINTS: ");
		buf.append(curScore);		
		canvas.drawText(buf.toString(), 0, 40, whitePaint);
	}
	
	public void restore(SharedPreferences savedState) {
		// start restoring game variables
		if (savedState.getInt("game_saved", 0) != 1) {
			return;
		}
		
		SharedPreferences.Editor editor = savedState.edit();
		editor.remove("game_saved");
		editor.commit();				
		
		highScore = savedState.getInt("game_highScore", SCORE_DEFAULT);
		
		int lastPotholeId = savedState.getInt("game_lastPotHole_id", -1);
		
		if (lastPotholeId != -1) {
			lastPothole = potholes[lastPotholeId];
			
		}
		else {
			lastPothole = null;
		}
		
		spawnPotholeTime = savedState.getLong("game_spawnPotholeTicks", 0);
		playerTap = savedState.getBoolean("game_playerTap", false);
		gameState = savedState.getInt("game_gameState", 0);
		tapToStartTime = savedState.getLong("game_tapToStartTime", 0);		
		showTapToStart = savedState.getBoolean("game_showTapToStart", false);
		getReadyGoTime = savedState.getLong("game_getReadyGoTime", 0);
		getReadyGoState = savedState.getInt("game_getReadyGoState", 0);
		gameOverTime = savedState.getLong("game_gameOverTime", 0);
		
		lastGameState = savedState.getInt("game_lastGameState", 1);
		pauseStartTime = savedState.getLong("game_pauseStartTime", 0);
		
		spawnPastryTime = savedState.getLong("game_spawnPastryTime", 0);
		
		scoreTime = savedState.getLong("game_scoreTime", 0);
		curScore = savedState.getInt("game_curScore", 0);
		
		// restore game entities		
		droid.restore(savedState);
		
		for (Pothole p : potholes) {
			p.restore(savedState);
		}
		
		pastry.restore(savedState);
		
		road.restore(savedState);
	}
	
	public void save(SharedPreferences.Editor map) {
		
		if (map == null) {			
			return;
		}
		
		map.putInt("game_saved", 1);

		map.putInt("game_highScore", highScore);		
		
		// save game variables
		if (lastPothole == null) {
			map.putInt("game_lastPotHole_id", -1);
		}
		else {
			map.putInt("game_lastPotHole_id", lastPothole.id);
		}
		
		map.putLong("game_spawnPotholeTicks", spawnPotholeTime);
		map.putBoolean("game_playerTap", playerTap);
		map.putInt("game_gameState", gameState);
		map.putLong("game_tapToStartTime", tapToStartTime);
		map.putBoolean("game_showTapToStart", showTapToStart);
		map.putLong("game_getReadyGoTime", getReadyGoTime);
		map.putInt("game_getReadyGoState", getReadyGoState);
		map.putLong("game_gameOverTime", gameOverTime);

		map.putInt("game_lastGameState", lastGameState);
		map.putLong("game_pauseStartTime", pauseStartTime);
		
		map.putLong("game_spawnPastryTime", spawnPastryTime);
		
		map.putLong("game_scoreTime", scoreTime);
		map.putInt("game_curScore", curScore);
		
		// save game entities		
		droid.save(map);
		
		for (Pothole p : potholes) {
			p.save(map);
		}
		
		pastry.save(map);
		road.save(map);
		// store saved variables
		map.commit();
	}
}