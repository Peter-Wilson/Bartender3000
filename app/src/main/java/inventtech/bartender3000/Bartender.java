package inventtech.bartender3000;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class Bartender extends Activity {

    private boolean cupScanned;
    static final String cup = "CUP_THERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null || savedInstanceState.getBoolean(cup))
            setContentView(R.layout.activity_cup_scan);
        else
            setContentView(R.layout.activity_drink_selector);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putBoolean(cup, cupScanned);
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cup_scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void newPage(View view)
    {
        this.cupScanned = true;
        setContentView(R.layout.activity_drink_selector);
    }
}
