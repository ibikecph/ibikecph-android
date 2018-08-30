package dk.kk.ibikecphlib;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.util.Config;

/**
 * The activity that lets the user set up the text-to-speech settings of the app.
 * @author jens
 *
 */
public class TTSSettingsActivity extends Activity {
	private Switch enableSwitch;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ttssettings);



		this.getActionBar().setTitle(IBikeApplication.getString("voice"));
		this.enableSwitch = (Switch) findViewById(R.id.ttsEnabledSwitch);
		
		this.enableSwitch.setText(IBikeApplication.getString("voice_option"));
		
		this.enableSwitch.setChecked(Config.TTS_ENABLED);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.ttssettings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		/*
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		*/
		return super.onOptionsItemSelected(item);
	}
	
	public void onEnableClick(View v) {
		Config.TTS_ENABLED = this.enableSwitch.isChecked();
	}
}
