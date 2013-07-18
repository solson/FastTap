package usask.hci.fastdraw;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    private DrawView mDrawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawView = new DrawView(this);
        setContentView(mDrawView);
    }
}
