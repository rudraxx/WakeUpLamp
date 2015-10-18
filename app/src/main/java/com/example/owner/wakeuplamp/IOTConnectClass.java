package com.example.owner.wakeuplamp;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Owner on 10/17/2015.
 */
public class IOTConnectClass extends AsyncTask<String,Void,String > {

//    private Context context;
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
