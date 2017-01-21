package com.jeris.android.droidrunner;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.RectF;

class Droid {

	float x;
	float y;
	float vy;
	boolean jumping;
	boolean falling;
	
	float w;
	float h;
	
	final float startX = 180.0f;
	final float startY = 352.5f;
	final float initialVelocity = 15.0f;
	
	Game game;
	RectF rect;
	
	int curFrame;
	long curFrameTime = 0;
	public Droid(Game game) {
		this.game = game;
		this.rect = new RectF();
		
		w = game.droidJumpImage.getWidth();
		h = game.droidJumpImage.getHeight();
		reset();
	}

	public void reset() {
		
		jumping = false;
		falling = false;
		
		x = startX;
		y = startY;
		
		rect.left = x;
		rect.top = y;
		rect.bottom = y + h;
		rect.right = x + w;
		
		curFrame = 0;
		curFrameTime = System.currentTimeMillis();
	}

	public void update() {
		// first: handle collision detection with pastry and potholes
		doCollisionDetection();
		// handle falling
		if (falling) {
			doPlayerFall();
		}
		// handle jumping
		if (jumping) {
			doPlayerJump();
		}
		// does player want to jump?
		if (game.playerTap && !jumping && !falling) {
			startPlayerJump();
			game.soundPool.play(game.droidJumpSnd, 1.0f, 1.0f, 0, 0, 1.0f);
		}
		// update animation
		long now = System.currentTimeMillis() - curFrameTime;
		if (now > 250) {
			curFrame++;
			if (curFrame > 3) {
				curFrame = 1;
			}
			curFrameTime = System.currentTimeMillis();
		}
	}

	public void draw(Canvas canvas) {
		if (jumping || falling) {
			canvas.drawBitmap(game.droidJumpImage, x, y, game.clearPaint);
		}
		else {
			canvas.drawBitmap(game.droidImages[curFrame], x, y, game.clearPaint);
		}
	}
	private void doCollisionDetection() {

		float ey = y + h;

		for (Pothole p : game.potholes) {
			if (!p.alive) {
				continue;
			}

			float lx = x;
			float rx = x + w;

			if (
					// am I over the pothole?
					(p.x < lx) 
					
					// am I still inside the pothole?
					&& ((p.x + p.w) > rx) 
					
					// have I fallen into the pothole?
					&& (p.y <= ey)
				
				) {
				
				game.initGameOver();				
			}
		}
		// check for pastry collision
		rect.left = x;
		rect.top = y;
		rect.bottom = y + h;
		rect.right = x + w;
		
		if (game.pastry.alive && rect.intersect(game.pastry.rect)) {
			game.doPlayerEatPastry();			
		}
	}
	
	private void doPlayerFall() {
		vy += 1.0f;
		y += vy;
		float tmpY = y + h;
		if (tmpY > game.groundY) {
			y = startY;
			falling = false;
		}		
	}
	
	private void doPlayerJump() {
		y -= vy;
		vy -= 1.0f;
		if (vy <= 0.0f) {
			jumping = false;
			falling = true;
		}		
	}
	
	private void startPlayerJump() {
		jumping = true;
		game.playerTap = false;
		vy = initialVelocity;		
	}

	public void restore(SharedPreferences savedState) {
		x = savedState.getFloat("droid_x", 0);
		y = savedState.getFloat("droid_y", 0);
		vy = savedState.getFloat("droid_vy", 0);
		jumping = savedState.getBoolean("droid_jumping", false);
		falling = savedState.getBoolean("droid_falling", false);			
		curFrame = savedState.getInt("droid_curFrame", 0);
		curFrameTime = savedState.getLong("droid_curFrameTime",0 );		
	}
	
	public void save(SharedPreferences.Editor map) {
		map.putFloat("droid_x", x);
		map.putFloat("droid_y", y);
		map.putFloat("droid_vy", vy);
		map.putBoolean("droid_jumping", jumping);
		map.putBoolean("droid_falling", falling);
		map.putInt("droid_curFrame", curFrame);
		map.putLong("droid_curFrameTime", curFrameTime);
	}
}
