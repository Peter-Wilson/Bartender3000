package inventtech.bartender3000;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.List;
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
    private InputStream in;
    private OutputStream out;
    byte[] readBuffer;
    int readBufferPosition;
    private static final int  REQUEST_ENABLE = 0x1;
    private static final int DISCOVER_DURATION = 300;
    private static final int REQUEST_BLU = 2;
    private ImageButton drink1, drink2, drink3, drink4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cup_scan);
        cupScanned = false;
        initializeBluetooth();
    }

    private void initializeBluetooth() {
        //Get the bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null)
        {
            showAppCloseAlert("Bluetooth Required", "Bluetooth is not supported on this device, application closing");
            return;
        }

        //enable bluetooth if it isn't already
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, REQUEST_ENABLE);
            return;
        }

        //Pair with the Arduino
        arduino = GetArduinoDevice();
        if(arduino == null) {
            showArduinoConnectionAlert();
            return;
        }

        //connect to bluetooth arduino and set up input/output streams
        try {
            arduinoSocket = getDeviceSocket(arduino);
            arduinoSocket.connect();
            in = arduinoSocket.getInputStream();
            out = arduinoSocket.getOutputStream();
        }
        catch(Exception e)
        {
            //cannot connect to the arduino
            showAppCloseAlert("Cannot Connect","Unable to connect to Arduino. Application closing, try Unpairing with the device.");
            System.out.println("Connection was unsuccessful");
            return;
        }

        //start reading inputs
        try
        {
            SetUpInputThread();
        }
        catch (Exception e)
        {
            //cannot connect to the arduino
            showAlert("Cannot Read","Unable to read from the arduino");
            System.out.println("Connection was unsuccessful");
            newCup();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                initializeBluetooth();
            }
            else
            {
                Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enabler, REQUEST_ENABLE);
            }
        }
        else if(requestCode == REQUEST_BLU) {
            //if still not paired, exit
            arduino = GetArduinoDevice();
            if(arduino == null)
            {
                showAppCloseAlert("Not Paired", "Your device is not paired with the arduino. Closing the application now.");
                return;
            }


            //if paired, continue
            initializeBluetooth();
        }
    }

    protected void OpenBluetoothSettings()
    {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivityForResult(intentOpenBluetoothSettings, REQUEST_BLU);
    }

    protected void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    protected void showArduinoConnectionAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Pair with Arduino")
                .setMessage("You must pair with the arduino to continue.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        OpenBluetoothSettings();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    protected void showAppCloseAlert(String title, String message) {
        final Bartender b = this;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        b.finishAffinity();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    //set up the clickListeners for the drink buttons
    private void SetOnClickListeners() {

        drink1 = (ImageButton) findViewById(R.id.drink1);
        drink2 = (ImageButton) findViewById(R.id.drink2);
        drink3 = (ImageButton) findViewById(R.id.drink3);
        drink4 = (ImageButton) findViewById(R.id.drink4);

        drink1.setOnClickListener(new View.OnClickListener()
        { @Override public void onClick(View v) {ButtonSelected(drink1,'a');}});

        drink2.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {ButtonSelected(drink2,'b');}});

        drink3.setOnClickListener(new View.OnClickListener()
        {@Override public void onClick(View v) {ButtonSelected(drink3,'c');}});

        drink4.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {ButtonSelected(drink4,'d');}});
    }

    protected void ButtonSelected(ImageButton button,char choice)
    {
        try {
            //darken all the buttons other than the one selected
            ((ImageView) findViewById(R.id.drink1)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            ((ImageView) findViewById(R.id.drink2)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            ((ImageView) findViewById(R.id.drink3)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            ((ImageView) findViewById(R.id.drink4)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            button.clearColorFilter();

            //send the selection to the arduino
            out.write(choice);
            out.flush();
        } catch (Exception e) {
            Toast.makeText(this,"Selection cannot be sent to the Bartender.", Toast.LENGTH_LONG);
        }
    }

    //create the thread to read from the arduino
    private void SetUpInputThread() {
            final byte delimiter = 10; //This is the ASCII code for a newline character
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            input = new Thread(new Runnable() {
                public void run() {
                    Log.d("CREATION","creating thread");
                    try {
                        Log.d("TRY", "in try");

                        while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                            int bytesAvailable = in.available();

                            Log.d("AVAILABLE", "" + bytesAvailable);
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                in.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        Log.d("INPUT", data);
                                        readBufferPosition = 0;

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //the cup is ready to be filled
                                                if (data.equals("y\r")) {
                                                    newCup();
                                                //the cup is not ready to be filled
                                                } else if (data.equals("n\r")) {
                                                    cupGone();
                                                //the drink has been finished
                                                } else if (data.equals("d\r")) {
                                                    drinkFull();
                                                }
                                            }
                                        });

                                        //The variable data now contains our full command
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        }
                        arduinoSocket.close();
                    }
                    catch(Exception e) {
                        Log.d("CAUGHT",e.getMessage());
                        stopWorker = true;
                        System.out.println("Error occurred while recieving input");
                    }
                }
            });
            input.start();
    }

    //Find the paired arduino
    private BluetoothDevice GetArduinoDevice() {
        Set pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            int i = 0;
            for(BluetoothDevice device : (Set<BluetoothDevice>)pairedDevices)
            {

                if(device.getName().toLowerCase().equals("itead"))
                {
                    return device;

                }
                else
                   i++;
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
            Toast.makeText(this,"Cannot connect to the device", Toast.LENGTH_LONG);
            return null;
        }

    }


    @Override
    protected void onSaveInstanceState (Bundle outState) {
        try {
            arduinoSocket.close();
        }
        catch(Exception e){     }

        //save the cup state
        outState.putBoolean("CupThere", cupScanned);
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

    //a new cup is found
    public void newCup()
    {
        if(!cupScanned) {
            cupScanned = true;
            Toast.makeText(this,"A Cup has been detected, select your drink", Toast.LENGTH_LONG);
            setContentView(R.layout.activity_drink_selector);
            SetOnClickListeners();
        }

    }

    //the cup is removed
    public void cupGone()
    {
        if(cupScanned) {
            cupScanned = false;
            Toast.makeText(this, "The cup has been removed", Toast.LENGTH_LONG);
            setContentView(R.layout.activity_cup_scan);
        }
    }

    //the drink is filled
    public void drinkFull() {
        Toast.makeText(this, "The drink has been filled, you may remove the cup", Toast.LENGTH_LONG);
    }
}
