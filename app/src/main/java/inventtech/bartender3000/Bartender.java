package inventtech.bartender3000;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.net.Socket;
import java.util.Set;
import java.util.UUID;


public class Bartender extends Activity {

    private boolean cupScanned;
    static final String cup = "CUP_THERE";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice arduino;
    private BluetoothSocket arduinoSocket;
    private Thread input;
    private boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get the bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //If not enabled, prompt to enable
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        arduino = GetArduinoDevice();
        arduinoSocket = getDeviceSocket(arduino);

        //cannot connect to the arduino
        if (arduino == null || arduinoSocket == null) {
            System.out.println("Connection was unsuccessful");
            return;
        }

        if (savedInstanceState == null || savedInstanceState.getBoolean(cup)) {
            setContentView(R.layout.activity_cup_scan);
        } else {
            setContentView(R.layout.activity_drink_selector);
        }

        /*
        final Handler handler = new Handler();
        input = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    //Do work
                }
            }
        });
        input.start();
        */
    }

    //Find the paired arduino
    private BluetoothDevice GetArduinoDevice() {
        Set pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : (Set<BluetoothDevice>)pairedDevices)
            {
                if(device.getName().equals("itead")) //Note, you will need to change this to match the name of your device
                {
                    return device;
                }
            }
        }
        return null;
    }

    //return the socket for the arduino
    private BluetoothSocket getDeviceSocket(BluetoothDevice device)
    {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
            BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            return socket;
        }
        catch(Exception e)
        {
            return null;
        }

    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        try {
            arduinoSocket.close();
        }
        catch(Exception e)
        {

        }
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
