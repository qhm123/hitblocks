package com.qhm123.hitblocks;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Callback {

	private static final String TAG = GameView.class.getSimpleName();

	class Block {

		public static final int STATE_GOOD = 0;
		public static final int STATE_BROKEN = 3;

		public RectF rectF;
		public int state;

		public void draw(Canvas c, Paint paint) {
			if (state == STATE_GOOD) {
				paint.setColor(Color.BLUE);
				paint.setStyle(Style.FILL);

				c.drawRect(rectF, paint);

				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(1);
				paint.setStyle(Style.STROKE);

				c.drawRect(rectF, paint);
			}
		}
	}

	class GameThread extends Thread {

		private static final float MATH_PI = (float) Math.PI;

		/*
		 * State-tracking constants
		 */
		public static final int STATE_LOSE = 1;
		public static final int STATE_PAUSE = 2;
		public static final int STATE_READY = 3;
		public static final int STATE_RUNNING = 4;
		public static final int STATE_WIN = 5;

		/** Used to figure out elapsed time between frames */
		private long mLastTime;

		private int mMode;

		private float mGuardX;
		private float mGuardY;

		private float mGuardWidth;
		private float mGuardHeight;

		private float mGuardSpeed;

		private Region mGuardRegion;

		private Paint mGuardPaint;

		private float mBallX;
		private float mBallY;

		private float mBallRadius;

		private float mBallSpeed = 300;

		private float mBallDegree;

		private Region mBallRegion;

		private Paint mBallPaint;

		private List<Block> mBlocks = new ArrayList<Block>();

		private Paint mBlockPaint;

		private Paint mBackgroundPaint;

		private boolean mRun = false;

		private int mCanvasWidth;
		private int mCanvasHeight;

		private Region mCanvasRegion;

		private SurfaceHolder mSurfaceHolder;
		private Context mContext;
		private Handler mHandler;

		public GameThread(SurfaceHolder surfaceHolder, Context context,
				Handler handler) {
			mSurfaceHolder = surfaceHolder;
			mContext = context;
			mHandler = handler;

			mGuardPaint = new Paint();
			mGuardPaint.setColor(Color.GREEN);
			mGuardPaint.setStyle(Style.FILL);

			mBallPaint = new Paint();
			mBallPaint.setColor(Color.RED);
			mBallPaint.setStyle(Style.FILL);

			mBlockPaint = new Paint();
			mBlockPaint.setColor(Color.YELLOW);
			mBlockPaint.setStrokeWidth(1);
			mBlockPaint.setStyle(Style.FILL_AND_STROKE);

			mBackgroundPaint = new Paint();
			mBackgroundPaint.setColor(Color.BLACK);
			mBackgroundPaint.setStyle(Style.FILL);
		}

		private void setRunning(boolean b) {
			mRun = b;
		}

		private Rect rectFToRect(RectF rectF) {
			return new Rect((int) rectF.left, (int) rectF.top,
					(int) rectF.right, (int) rectF.bottom);
		}

		private void updatePhysics() {
			long now = System.currentTimeMillis();

			// Log.d(TAG, "now: " + now + ", mLastTime: " + mLastTime);

			// Do nothing if mLastTime is in the future.
			// This allows the game-start to delay the start of the physics
			// by 100ms or whatever.
			if (mLastTime > now)
				return;

			double elapsed = (now - mLastTime) / 1000.0;

			// Log.d(TAG, "elapsed: " + elapsed);

			mBallX += mBallSpeed * elapsed
					* Math.cos(Math.PI * mBallDegree / 180);
			mBallY += mBallSpeed * elapsed
					* Math.sin(Math.PI * mBallDegree / 180);

			// Log.d(TAG, "ball: " + mBallX + ", " + mBallY);

			Path ballPath = new Path();
			ballPath.addCircle(mBallX, mBallY, mBallRadius, Direction.CW);
			mBallRegion = new Region();
			mBallRegion.setPath(ballPath, mCanvasRegion);

			// Log.d(TAG, "mBallRegion bounds: " + mBallRegion.getBounds()
			// + ", mGuardRegion bounds: " + mGuardRegion.getBounds());

			// if (mBallRegion.quickContains(mGuardRegion.getBounds())) {
			float random = 3 * (float) (Math.random() - Math.random());
			// Log.d(TAG, "random: " + random);

			// Log.d(TAG, "before mBallDegree: " + mBallDegree);
			if (mBallRegion.op(mGuardRegion, Op.INTERSECT)
					|| (mBallY - mBallRadius) < 1F
					|| (mBallY + mBallRadius) > (mCanvasHeight - 1F)) {
				Log.d(TAG, "collision: up down");
				mBallDegree = -mBallDegree + random;
			} else if ((mBallX - mBallRadius) < 1F
					|| (mBallX + mBallRadius) > (mCanvasWidth - 1F)) {
				Log.d(TAG, "collision: left right");
				mBallDegree = -mBallDegree + 180 + random;
			} else {
				for (Block block : mBlocks) {
					if (block.state == Block.STATE_BROKEN) {
						continue;
					}
					Rect rect = rectFToRect(block.rectF);
					// Log.d(TAG, "collision: rect, " + rect);
					Region region = new Region(rect);
					// Log.d(TAG, "mBallRegion bounds: " +
					// mBallRegion.getBounds()
					// + ", region bounds: " + region.getBounds());
					mBallRegion.setPath(ballPath, mCanvasRegion);
					if (mBallRegion.op(region, Op.INTERSECT)) {
						Log.d(TAG, "collision: block, " + block.rectF);
						mBallDegree = -mBallDegree + random;

						block.state = Block.STATE_BROKEN;
						break;
					}
				}
			}
			// Log.d(TAG, "after mBallDegree: " + mBallDegree);

			mLastTime = now;
		}

		private void doDraw(Canvas c) {
			c.drawPaint(mBackgroundPaint);

			guardDraw(c);

			blocksDraw(c);

			ballDraw(c);
		}

		private void ballDraw(Canvas c) {
			c.drawCircle(mBallX, mBallY, mBallRadius, mBallPaint);
		}

		private void guardDraw(Canvas c) {
			c.drawRect(mGuardX, mGuardY, mGuardX + mGuardWidth, mGuardY
					+ mGuardHeight, mGuardPaint);
		}

		private void blocksDraw(Canvas c) {
			for (Block block : mBlocks) {
				block.draw(c, mBlockPaint);
			}
		}

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						// Log.d(TAG, "mMode: " + mMode);
						if (mMode == STATE_RUNNING)
							updatePhysics();
						doDraw(c);
					}
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}

		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mCanvasWidth = width;
				mCanvasHeight = height;

				// // don't forget to resize the background image
				// mBackgroundImage = Bitmap.createScaledBitmap(
				// mBackgroundImage, width, height, true);
			}
		}

		public void doStart() {
			synchronized (mSurfaceHolder) {
				// canvas
				mCanvasRegion = new Region(0, 0, mCanvasWidth, mCanvasHeight);

				// guard
				mGuardWidth = mCanvasWidth / 4F;
				mGuardHeight = mCanvasHeight / 15F;

				mGuardX = (mCanvasWidth - mGuardWidth) / 2;
				mGuardY = mCanvasHeight - mGuardHeight;

				Rect guardRect = new Rect((int) mGuardX, (int) mGuardY,
						(int) (mGuardX + mGuardWidth),
						(int) (mGuardY + mGuardHeight));

				mGuardRegion = new Region();
				mGuardRegion.set(guardRect);

				// ball
				mBallRadius = mCanvasWidth / 10F;

				mBallX = mCanvasWidth / 2F;
				mBallY = mCanvasHeight / 2F + mBallRadius;

				Path ballPath = new Path();
				ballPath.addCircle(mBallX, mBallX, mBallRadius, Direction.CW);

				mBallRegion = new Region();
				mBallRegion.setPath(ballPath, mCanvasRegion);

				mBallDegree = 90;

				// blocks
				int lineCount = 3;
				int blockCountEveryLine = 8;
				float blockWidth = mCanvasWidth / blockCountEveryLine;
				float blockHeight = mCanvasHeight / 20F;
				for (int i = 0; i < lineCount; i++) {
					for (int j = 0; j < blockCountEveryLine; j++) {
						Block block = new Block();
						block.state = Block.STATE_GOOD;
						block.rectF = new RectF(j * blockWidth,
								i * blockHeight, (j + 1) * blockWidth - 1,
								(i + 1) * blockHeight - 1);
						mBlocks.add(block);
					}
				}

				// start
				mLastTime = System.currentTimeMillis() + 100;
				setState(STATE_RUNNING);
			}
		}

		public void pause() {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING)
					setState(STATE_PAUSE);
			}
		}

		public void unpause() {
			// Move the real time clock up to now
			synchronized (mSurfaceHolder) {
				mLastTime = System.currentTimeMillis() + 100;
			}
			setState(STATE_RUNNING);
		}

		public void setState(int mode) {
			synchronized (mSurfaceHolder) {
				setState(mode, null);
			}
		}

		public void setState(int mode, CharSequence message) {
			/*
			 * This method optionally can cause a text message to be displayed
			 * to the user when the mode changes. Since the View that actually
			 * renders that text is part of the main View hierarchy and not
			 * owned by this thread, we can't touch the state of that View.
			 * Instead we use a Message + Handler to relay commands to the main
			 * thread, which updates the user-text View.
			 */
			synchronized (mSurfaceHolder) {
				mMode = mode;

				if (mMode == STATE_RUNNING) {
					Message msg = mHandler.obtainMessage();
					Bundle b = new Bundle();
					b.putString("text", "");
					b.putInt("viz", View.INVISIBLE);
					msg.setData(b);
					mHandler.sendMessage(msg);
				} else {
					// mRotating = 0;
					// mEngineFiring = false;
					Resources res = mContext.getResources();
					CharSequence str = "";
					// if (mMode == STATE_READY)
					// str = res.getText(R.string.mode_ready);
					// else if (mMode == STATE_PAUSE)
					// str = res.getText(R.string.mode_pause);
					// else if (mMode == STATE_LOSE)
					// str = res.getText(R.string.mode_lose);
					// else if (mMode == STATE_WIN)
					// str = res.getString(R.string.mode_win_prefix)
					// + mWinsInARow + " "
					// + res.getString(R.string.mode_win_suffix);
					//
					// if (message != null) {
					// str = message + "\n" + str;
					// }
					//
					// if (mMode == STATE_LOSE)
					// mWinsInARow = 0;

					Message msg = mHandler.obtainMessage();
					Bundle b = new Bundle();
					b.putString("text", str.toString());
					b.putInt("viz", View.VISIBLE);
					msg.setData(b);
					mHandler.sendMessage(msg);
				}
			}
		}
	}

	private GameThread thread;

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new GameThread(holder, context, new Handler() {
			@Override
			public void handleMessage(Message m) {
			}
		});

		setFocusable(true); // make sure we get key events
	}

	/**
	 * Fetches the animation thread corresponding to this LunarView.
	 * 
	 * @return the animation thread
	 */
	public GameThread getThread() {
		return thread;
	}

	/**
	 * Standard window-focus override. Notice focus lost so we can pause on
	 * focus lost. e.g. user switches to take a call.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus)
			thread.pause();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		thread.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

}