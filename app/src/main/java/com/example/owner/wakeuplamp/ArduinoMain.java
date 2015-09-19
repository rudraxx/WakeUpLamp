package com.example.owner.wakeuplamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.UUID;


public class ArduinoMain extends ActionBarActivity {

//    //TObeimplemented:
// USe NFC for autoenable
// Use gears for stand
    // sync with sunrise time at any location to ensure the lighting starts at that time.

    //Declare buttons & editText
//    Button disconnect;

//    private EditText editText;

    //Memeber Fields
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    public OutputStream outStream = null;

    // UUID service - This is the type of Bluetooth device that the BT module is
    // It is very likely yours will be the same, if not google UUID for your manufacturer
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module
    public String newAddress = null;

    private static TextView static_textView_Red;
    private static TextView static_textView_Green;
    private static TextView static_textView_Blue;

    TextView showTime,showHours,showMin,showSec;
    Calendar cal;
//    Button buttonTime;
    private int mInterval = 1000; // 5 seconds by default, can be changed later
    private Handler mHandler;

//    Button buttonSound;
    Button buttonStop;
    private MediaPlayer mediaPlayer;

    Button buttonSetHour;
    EditText editsethour;
    Integer rVal = 0;
    Integer gVal = 0;
    Integer bVal = 0;

    Integer intTime = 6;
    Integer intFlag=0;
    private volatile boolean done = false;

    public String fileName = "File_1.txt"; // Make sure the extension is present.
    // variables for calculating the sleep hours;
    public long startTime;
    public long difference;
    public long differenceInSeconds;
    public long differenceInMinutes;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_main);

        //getting the bluetooth adapter value and calling checkBTstate function
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Not sure if I should remove this call to checkBT state or not. Check and validate.
        // The checkBTstate function is being called in original app.
        checkBTState();

        showTime = (TextView) findViewById(R.id.tvTime);
//        showHours = (TextView) findViewById(R.id.tvHour);
//        showMin = (TextView) findViewById(R.id.tvMin);
//        showSec = (TextView) findViewById(R.id.tvSec);

        final Button disconnect = (Button) findViewById(R.id.bDisableBlueTooth);
        final Button buttonSound = (Button) findViewById(R.id.bSound);
        buttonStop = (Button) findViewById(R.id.bStop);
        mediaPlayer = MediaPlayer.create(this, R.raw.birdysong);
        buttonSetHour = (Button) findViewById(R.id.bSethour);
        editsethour = (EditText) findViewById(R.id.eSetHour);
        //Get MAC address from WakeUpLampActivity
        Intent intent = getIntent();
        newAddress = intent.getStringExtra(WakeUpLamp.EXTRA_DEVICE_ADDRESS);

        // Set up a pointer to the remote device using its address.
        BluetoothDevice device = btAdapter.getRemoteDevice(newAddress);

        //Attempt to create a bluetooth socket for comms
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

        } catch (IOException e1) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create Bluetooth socket", Toast.LENGTH_SHORT).show();
        }

        // Establish the connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();        //If IO exception occurs attempt to close socket
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "ERROR - Could not close Bluetooth socket", Toast.LENGTH_SHORT).show();
            }
        }

        // Create a data stream so we can talk to the device
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create bluetooth outstream", Toast.LENGTH_SHORT).show();
        }
        //When activity is resumed, attempt to send a piece of junk data ('x') so that it will fail if not connected
        // i.e don't wait for a user to press button to recognise connection failure
        sendData("x");
        mHandler = new Handler();
        startRepeatingTask();

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    sendData("<R" + 0 + "G" + 0 + "B" + 0 + ">" + "\n");
                    writeFile(fileName, cal.getTime().toString(), 2); // Sending wake up parameters

                    // Calculate the duration of sleep hours
                    difference = System.currentTimeMillis() - startTime;
                    differenceInSeconds = difference/ DateUtils.SECOND_IN_MILLIS;
                    // format difference in time as HH:MM:SS or MM:SS
                    String formatted = DateUtils.formatElapsedTime(differenceInSeconds);

                    differenceInMinutes = differenceInSeconds/60;
                    String strDiff = String.valueOf(differenceInMinutes);
                    // Write the duration to file.
                    writeFile(fileName, formatted, 3); // Sending hours slept parameters.

                    // Update Thingspeak channel.
                    OperationGetRequest request = new OperationGetRequest();
                    request.execute("https://api.thingspeak.com/update.html?key=XB4Y8F458TQXVGN9&field1="+strDiff);

                    done = true;
                    btSocket.close();
                    if (mediaPlayer.isPlaying()){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
//                    btAdapter.disable();
                    Toast.makeText(getBaseContext(), "Done disabling", Toast.LENGTH_SHORT).show();

                } catch (IOException e3) {
                    Toast.makeText(getBaseContext(), "ERROR - Failed to close Bluetooth socket", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mediaPlayer.start();
                    mediaPlayer.setLooping(true);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mediaPlayer.stop();
                    try {
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.seekTo(0);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(ArduinoMain.this,"Unable to close Mediaplayer",Toast.LENGTH_SHORT);
                }
            }
        });

        buttonSetHour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intTime = Integer.valueOf(editsethour.getText().toString());
                Toast.makeText(ArduinoMain.this,"Set Hour: "+intTime,Toast.LENGTH_SHORT).show();
//                fileName = "File_1";//+cal.getTime().toString(); // THis is where the filename to be saved gets assigned.

                Toast.makeText(ArduinoMain.this,"FileName: "+fileName,Toast.LENGTH_SHORT).show();

                startTime = System.currentTimeMillis();
                writeFile(fileName, cal.getTime().toString(), 1);
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
//        startRepeatingTask();

    }

    @Override
    protected void onStop() {
        super.onStop();
//        startRepeatingTask();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Pausing can be the end of an app if the device kills it or the user doesn't open it again
        //close all connections so resources are not wasted
        // But I cant close the socket in pause since the lamp needs to keep getting data
        // even when the app is in the background. Hence, closing it in the onDestroy().
        //Close BT socket to device
        try {
            sendData("<R" + 0 + "G" + 0 + "B" + 0 + ">" + "\n");
            done = true;
            btSocket.close();
            mediaPlayer.release();
        } catch (IOException e2) {
            Toast.makeText(getBaseContext(), "ERROR - Failed to close Bluetooth socket", Toast.LENGTH_SHORT).show();
        }
        mHandler.removeCallbacks(mStatusChecker);

    }

    //takes the UUID and creates a comms socket
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    //same as in device list activity
    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "ERROR - Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    // Method to send data
    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            //attempt to place data on the outstream to the BT device
            outStream.write(msgBuffer);
        } catch (IOException e) {
            //if the sending fails this is most likely because device is no longer there
            Toast.makeText(getBaseContext(), "ERROR - Send Data error", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void startRepeatingTask() {
        if(!done) {
            mStatusChecker.run();
        }
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            mHandler.postDelayed(mStatusChecker,mInterval);
        }
    };


    public void updateStatus(){
        cal = Calendar.getInstance();

//        if (cal.get(Calendar.SECOND)==10){
//            try {
//                mediaPlayer.start();
//            } catch (IllegalStateException e) {
//                e.printStackTrace();
//            }
//        };
//        if (cal.get(Calendar.SECOND)==50){
//            try {
//                mediaPlayer.stop();
//                mediaPlayer.prepare();
//                mediaPlayer.seekTo(0);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        };

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showTime.setText("" + cal.getTime());
//                showHours.setText("" + cal.get(Calendar.HOUR_OF_DAY));
//                showMin.setText("" + cal.get(Calendar.MINUTE));
//                showSec.setText("" + cal.get(Calendar.SECOND));

                // Set the RGB values
                // Red
                static_textView_Red = (TextView) findViewById(R.id.textView_Red);
                static_textView_Red.setText(rVal+"/255");

                // Green
                static_textView_Green = (TextView) findViewById(R.id.textView_Green);
                static_textView_Green.setText(gVal+"/255");

                // Blue
                static_textView_Blue = (TextView) findViewById(R.id.textView_Blue);
                static_textView_Blue.setText(bVal+"/255");

                // Use the value set in edit text editSetHour to set up start time for the LEDs.


                if (cal.get(Calendar.HOUR_OF_DAY)==intTime){

                    if (cal.get(Calendar.MINUTE)<20){
                        rVal = 30+2*cal.get(Calendar.MINUTE);
                        sendData("<R" + rVal + "G" + 0 + "B" + 0 + ">" + "\n");
//                        Toast.makeText(ArduinoMain.this, String.valueOf(cal.get(Calendar.MINUTE)), Toast.LENGTH_SHORT).show();
                    }

                    if (cal.get(Calendar.MINUTE)>20 && cal.get(Calendar.MINUTE)<40){

                        rVal = 40+3*cal.get(Calendar.MINUTE);
                        gVal = 2*cal.get(Calendar.MINUTE)-20;
                        sendData("<R" + rVal + "G" + gVal + "B" + 0 + ">" + "\n");
//                        Toast.makeText(ArduinoMain.this, String.valueOf(cal.get(Calendar.MINUTE)), Toast.LENGTH_SHORT).show();
                    }

                    if (cal.get(Calendar.MINUTE)>40 && cal.get(Calendar.MINUTE)<60){
                        // Start the sound after about 40 mins of light
                        if (intFlag==0){
                            try {
                                mediaPlayer.start();
                                mediaPlayer.setLooping(true);
                                Toast.makeText(ArduinoMain.this, "Media Started",Toast.LENGTH_SHORT).show();
                                intFlag =1;
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }

                        rVal = 60+3*cal.get(Calendar.MINUTE);
                        gVal = 2*cal.get(Calendar.MINUTE);
                        sendData("<R" + rVal + "G" + gVal + "B" + 0 + ">" + "\n");
//                        Toast.makeText(ArduinoMain.this,String.valueOf(cal.get(Calendar.MINUTE)),Toast.LENGTH_SHORT).show();
                    }

                }
                else {
                    sendData("<R" + 0 + "G" + 0 + "B" + 0 + ">" + "\n");
                    try {
                        mediaPlayer.stop();
                        mediaPlayer.prepare();
                        mediaPlayer.seekTo(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
//                sendData("<R" + 4 * cal.get(Calendar.SECOND) + "G" + 2 * cal.get(Calendar.SECOND) + "B" + 3 * cal.get(Calendar.SECOND) + ">" + "\n");



            }
        });
    }

    // Function to write to disk.
    // Mode 1 - Record Start Sleep;
    // Mode 2-  Record Wake up ;
    // Mode 3 - Record Difference - Sleep hours;

    public void writeFile(String fileName, String inData, int mode)
    {
        File file;
        FileOutputStream outputStream;

        try {
//            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileName);
            file = new File(Environment.getExternalStorageDirectory()+"/TestData",fileName);

//            outputStream = new FileOutputStream(file);
            outputStream = new FileOutputStream(file, true); // allow appending to the file.

//            outputStream.write(v.getText().toString().getBytes());
            if (mode==1){ // If recording the sleep start time.
                outputStream.write(System.getProperty("line.separator").getBytes()); // Start on new line;
                outputStream.write(("Sleep start time:  " + inData).getBytes()); // Print the current time.
                outputStream.write(("   ").getBytes()); // Spacing of 1 tabs

                Toast.makeText(this,"Start sleep time recorded",Toast.LENGTH_SHORT).show();

            }
            else if(mode==2){
                outputStream.write(("Wake up time:  " + inData).getBytes()); // Print the current time.
                Toast.makeText(this,"Wake time recorded",Toast.LENGTH_SHORT).show();
                outputStream.write(("   ").getBytes()); // Spacing of 1 tabs

            }
            else if(mode==3){
                outputStream.write(("Hours slept:  " + inData).getBytes()); // Print the current time.
                Toast.makeText(this,"Hours slept = "+inData,Toast.LENGTH_SHORT).show();

            }
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class OperationGetRequest extends AsyncTask<String,Void,String > {


        @Override
        protected String doInBackground(String... strings) {
            URL myURL;
            HttpURLConnection urlConnection = null;
            String response = "";
            try {
                myURL = new URL(strings[0]);
                urlConnection = (HttpURLConnection) myURL.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                response = readStream(in);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (urlConnection!=null) {
                    urlConnection.disconnect();
                }
            }

            return response;
        }

//        @Override
//        protected void onPostExecute(String result) {
//            TextView textOutput = (TextView)findViewById(R.id.tvOutput);
//            textOutput.setText(result);
//        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }

}
