package com.example.owner.wakeuplamp;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Owner on 10/17/2015.
 */
public class HelperClass {


    // Method to send data
    public void sendData(OutputStream outStream, Context context, String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            //attempt to place data on the outstream to the BT device
            outStream.write(msgBuffer);
        } catch (IOException e) {
            //if the sending fails this is most likely because device is no longer there
            Toast.makeText(context, "ERROR - Send Data error", Toast.LENGTH_SHORT).show();
//            finish();
        }
    }

    // Function to write to disk.
    // Mode 1 - Record Start Sleep;
    // Mode 2-  Record Wake up ;
    // Mode 3 - Record Difference - Sleep hours;

    public void writeFile(Context context,String fileName, String inData, int mode)
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

                Toast.makeText(context, "Start sleep time recorded", Toast.LENGTH_SHORT).show();

            }
            else if(mode==2){
                outputStream.write(("Wake up time:  " + inData).getBytes()); // Print the current time.
                Toast.makeText(context,"Wake time recorded",Toast.LENGTH_SHORT).show();
                outputStream.write(("   ").getBytes()); // Spacing of 1 tabs

            }
            else if(mode==3){
                outputStream.write(("Hours slept:  " + inData).getBytes()); // Print the current time.
                Toast.makeText(context,"Hours slept = "+inData,Toast.LENGTH_SHORT).show();

            }
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
