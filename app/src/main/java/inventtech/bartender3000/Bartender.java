package inventtech.bartender3000;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    /* request BT enable */
    private static final int  REQUEST_ENABLE      = 0x1;
    /* request BT discover */
    private static final int  REQUEST_DISCOVERABLE  = 0x2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        //Get the bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        //enable
        bluetoothAdapter.enable();

        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(enabler, REQUEST_DISCOVERABLE);



        BroadcastReceiver _foundReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                _devices.add(device);
                showDevices();
            }
        };

        BroadcastReceiver _discoveryReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                Log.d("EF-BTBee", ">>unregisterReceiver");
                unregisterReceiver(_foundReceiver);
                unregisterReceiver(this);
                _discoveryFinished = true;
            }
        };


        arduino = GetArduinoDevice();
        arduinoSocket = getDeviceSocket(arduino);

        //cannot connect to the arduino
        if (arduino == null || arduinoSocket == null) {
            System.out.println("Connection was unsuccessful");
            return;
        }

        try {
            arduinoSocket.connect();
            in = arduinoSocket.getInputStream();
            out = arduinoSocket.getOutputStream();
        }
        catch(Exception e)
        {
            return;
        }*/
        if (savedInstanceState == null || !savedInstanceState.getBoolean("CupThere")) {
            setContentView(R.layout.activity_cup_scan);
            // Execute some code after 2 seconds have passed
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    setContentView(R.layout.activity_drink_selector);
                }
            }, 2000);


        } else {
            setContentView(R.layout.activity_drink_selector);
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

    protected void connect(BluetoothDevice device) {
        //BluetoothSocket socket = null;
        try {
            //Create a Socket connection: need the server's UUID number of registered
            arduinoSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666"));

            arduinoSocket.connect();
            Log.d("EF-BTBee", ">>Client connectted");

            InputStream inputStream = arduinoSocket.getInputStream();
            OutputStream outputStream = arduinoSocket.getOutputStream();
            outputStream.write(new byte[]{(byte) 0xa0, 0, 7, 16, 0, 4, 0});

        } catch (IOException e) {
            Log.e("EF-BTBee", "", e);
        } finally {
            if (arduinoSocket != null) {
                try {
                    Log.d("EF-BTBee", ">>Client Close");
                    arduinoSocket.close();
                    finish();
                    return;
                } catch (IOException e) {
                    Log.e("EF-BTBee", "", e);
                }
            }
        }
    }



    private void SetUpInputThread() {
            final Handler handler = new Handler();
            input = new Thread(new Runnable() {
                public void run() {
                    Log.d("CREATION","creating thread");
                    try {
                        Log.d("TRY", "in try");

                        while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                            int bytesAvailable = in.available();

                            Log.d("AVAILABLE", ""+bytesAvailable);
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                in.read(packetBytes);
                                int readBufferPosition = 0;
                                byte[] readBuffer = null;


                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == 10) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        Log.d("INPUT", data);
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                if (data.equals('y')) {
                                                    newCup();
                                                } else {
                                                    cupGone();
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

    public void newPage(View view)
    {
        this.cupScanned = true;
        setContentView(R.layout.activity_drink_selector);

    }

    public void newCup()
    {
        this.cupScanned = true;

        setContentView(R.layout.activity_drink_selector);
    }

    public void cupGone()
    {
        this.cupScanned = false;
        setContentView(R.layout.activity_cup_scan);
    }
}
