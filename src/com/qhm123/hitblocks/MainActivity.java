package com.qhm123.hitblocks;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.qhm123.hitblocks.GameView.GameMessageHandler;
import com.qhm123.hitblocks.GameView.GameThread;

public class MainActivity extends Activity implements GameMessageHandler {

	private static final String TAG = MainActivity.class.getSimpleName();

	private GameView mGameView;
	private TextView mScore;
	private TextView mLife;
	private TextView mGameMessage;

	private GameThread mGameThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mScore = (TextView) findViewById(R.id.score);
		mLife = (TextView) findViewById(R.id.life);
		mGameMessage = (TextView) findViewById(R.id.game_message);
		mGameView = (GameView) findViewById(R.id.game_view);
		mGameView.setGameMessageHandler(this);

		mGameThread = mGameView.getThread();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			mGameThread.doStart(false);
			break;
		case R.id.menu_hard:
			mGameThread.doStart(true);
			break;
		}
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

	@Override
	public void handleMessage(Message m) {
		Log.d(TAG, "msg: " + m.what);
		switch (m.what) {
		case GameThread.MESSAGE_LIFE:
			int life = m.getData().getInt("life");
			mLife.setText("life: " + life);
			break;
		case GameThread.MESSAGE_SCORE:
			long score = m.getData().getLong("score");
			mScore.setText("score: " + score);
			break;
		case GameThread.MESSAGE_STATE:
			String text = m.getData().getString("text");
			int viz = m.getData().getInt("viz");
			mGameMessage.setText(text);
			mGameMessage.setVisibility(viz);
			break;
		}
	}

}
