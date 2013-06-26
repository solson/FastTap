package usask.hci.fastdraw;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	private DrawView mDrawView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDrawView = new DrawView(this);
		setContentView(mDrawView);
		loadPreferences();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.action_settings:
	            startActivityForResult(new Intent(this, Preferences.class), 0);
	            break;
        }
 
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	loadPreferences();
    }
    
    private void loadPreferences() {
    	mDrawView.loadPreferences(PreferenceManager.getDefaultSharedPreferences(this));
    }
}
