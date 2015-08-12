package inventtech.bartender3000;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

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
    /* request BT enable */
    private static final int  REQUEST_ENABLE      = 0x1;
    /* request BT discover */
    private static final int  REQUEST_DISCOVERABLE  = 0x2;
    private ImageButton drink1, drink2, drink3, drink4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cupScanned = false;


        //Get the bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //enable bluetooth if it isn't already
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, REQUEST_ENABLE);
        }

        arduino = GetArduinoDevice();
        arduinoSocket = getDeviceSocket(arduino);

        try {
            arduinoSocket.connect();
            in = arduinoSocket.getInputStream();
            out = arduinoSocket.getOutputStream();
        }
        catch(Exception e)
        {
            //cannot connect to the arduino
            showAlert("Connection Unsuccessful", "Unable to connect to Arduino. Please restart and try again");
            System.out.println("Connection was unsuccessful");
            return;
        }
        if (savedInstanceState == null || !savedInstanceState.getBoolean("CupThere")) {
            setContentView(R.layout.activity_cup_scan);

            // Execute some code after 2 seconds have passed
            SetUpInputThread();


        } else {
            setContentView(R.layout.activity_drink_selector);
            SetOnClickListeners();
            SetUpInputThread();
        }






       /* try
        {
            in = arduinoSocket.getInputStream();
            out = arduinoSocket.getOutputStream();
        }
        catch(Exception e)
        {
            System.out.println("Cannot establish connection");
            return;
        }*/


        //SetUpInputThread();

    }

    private void pairDevice(BluetoothDevice device) {
        try {
             Log.d("TAG", "Start Pairing...");

            //waitingForBonding = true;

            Method m = device.getClass()
                    .getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

            Log.d("TAG", "Pairing finished.");
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
    }

    private void SetOnClickListeners() {
        drink1 = (ImageButton) findViewById(R.id.drink1);
        drink2 = (ImageButton) findViewById(R.id.drink2);
        drink3 = (ImageButton) findViewById(R.id.drink3);
        drink4 = (ImageButton) findViewById(R.id.drink4);

        drink1.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    ButtonSelected(drink1,'a');
                                                }
                                            }
        );

        drink2.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          ButtonSelected(drink2,'b');
                                      }
                                  }
        );

        drink3.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          ButtonSelected(drink3,'c');
                                      }
                                  }
        );

        drink4.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          ButtonSelected(drink4,'d');
                                      }
                                  }
        );
    }

    protected void ButtonSelected(ImageButton button,char choice)
    {
        try {
            ((ImageView) findViewById(R.id.drink1)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            ((ImageView) findViewById(R.id.drink2)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            ((ImageView) findViewById(R.id.drink3)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            ((ImageView) findViewById(R.id.drink4)).setColorFilter(0x80808000, PorterDuff.Mode.MULTIPLY);
            button.clearColorFilter();

            out.write(choice);
            out.flush();
        } catch (Exception e) {
            showAlert("Cannot Send", "Sorry, the message cannot be sent.");
        }
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


    private void SetUpInputThread() {
            final Handler handler = new Handler();
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
                                                //showAlert("Data",data);
                                                if (data.equals("y\r")) {
                                                    newCup();
                                                } else if (data.equals("n\r")) {
                                                    cupGone();
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

    public void newCup()
    {
        if(!cupScanned) {
            cupScanned = true;
            showAlert("Cup Detected", "A Cup has been detected, select your drink");
            setContentView(R.layout.activity_drink_selector);
            SetOnClickListeners();
        }

    }

    public void cupGone()
    {
        if(cupScanned) {
            cupScanned = false;
            showAlert("Cup Removed","The cup has been removed");
            setContentView(R.layout.activity_cup_scan);
        }
    }

    public void drinkFull() {
        showAlert("Drink Full","The drink has been filled, you may remove the cup");
    }
}
