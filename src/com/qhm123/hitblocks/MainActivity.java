package com.qhm123.hitblocks;

import com.qhm123.hitblocks.GameView.GameThread;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	private GameView mGameView;
	private TextView mScore;
	private TextView mLife;
	private TextView mGameMessage;

	private GameThread mGameThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGameView = (GameView) findViewById(R.id.game_view);
		mScore = (TextView) findViewById(R.id.score);
		mLife = (TextView) findViewById(R.id.life);
		mGameMessage = (TextView) findViewById(R.id.game_message);

		mGameThread = mGameView.getThread();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mGameThread.doStart();
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();

		mGameThread.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// mGameThread.unpause();
	}

}
