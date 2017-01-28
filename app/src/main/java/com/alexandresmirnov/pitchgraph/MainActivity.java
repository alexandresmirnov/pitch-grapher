package com.alexandresmirnov.pitchgraph;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {

    double timestamp = 0;
    float pitch = 150;

    LineDataSet dataSet;
    LineData lineData;

    Thread t;
    Thread audioDispatcherThread;

    Handler handler;
    Runnable updateChart;

    AudioDispatcher audioDispatcher;

    PitchProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024,
            new PitchDetectionHandler() {

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {

                timestamp = audioEvent.getTimeStamp();
                pitch = pitchDetectionResult.getPitch();

                Log.d("entry", "" + timestamp + pitch);

                float transformedPitch = (float) (Math.log(pitch) / Math.log(2));

                new UpdateChartTask().execute(new Entry((float) timestamp, transformedPitch));

            }
        }
    );

    private class UpdateChartTask extends AsyncTask<Entry, Void, Entry> {
        protected Entry doInBackground(Entry... entries) {
            return entries[0];
        }

        protected void onPostExecute(Entry entry) {
            LineChart chart = (LineChart) findViewById(R.id.chart);

            Log.d("new entry", ""+entry);
            dataSet.addEntry(entry);
            //lineData.addDataSet(dataSet);
            //Log.d("UpdateChartTask", "added entry");
            chart.notifyDataSetChanged();
            chart.invalidate();

        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRecordAudioPermission();

        LineChart chart = (LineChart) findViewById(R.id.chart);

        List<Entry> entries = new ArrayList<Entry>();

        entries.add(new Entry(6, (float) (Math.log(150)/Math.log(2))));

        dataSet = new LineDataSet(entries, "Label");
        lineData = new LineData(dataSet);
        chart.setData(lineData);
        //chart.setTouchEnabled(false);
        chart.setDrawBorders(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum((float) (Math.log(40)/Math.log(2)));
        leftAxis.setAxisMaximum((float) (Math.log(400)/Math.log(2)));
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        XAxis bottomAxis = chart.getXAxis();
        //bottomAxis.setAxisMaximum(5);
        bottomAxis.setDrawGridLines(false);
        bottomAxis.setLabelCount(5, true);
        bottomAxis.setAxisMinimum(0);
        //chart.moveViewToX(5);

        //startCapturingAudio();
        setUpButtons();





        startCapturingAudio();
    }


    protected void startCapturingAudio(){

        audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);
        audioDispatcher.addAudioProcessor(pitchProcessor);

        audioDispatcherThread = new Thread(audioDispatcher,"Audio Dispatcher");
        audioDispatcherThread.start();


    }

    private void setUpButtons() {

        Button stop = (Button) findViewById(R.id.stop);

        stop.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Log.d("buttons", "pressed stop button");
                //audioDispatcher.removeAudioProcessor(pitchProcessor);
                audioDispatcher.stop();
            }
        });

        Button start = (Button) findViewById(R.id.start);

        start.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Log.d("buttons", "pressed start button");
                //audioDispatcher.start();
                startCapturingAudio();
                //audioDispatcher.addAudioProcessor(pitchProcessor);

            }
        });

        Button clear = (Button) findViewById(R.id.clear);

        clear.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Log.d("buttons", "pressed clear button");
                LineChart chart = (LineChart) findViewById(R.id.chart);


                /*
                List<Entry> entries = new ArrayList<Entry>();
                entries.add(new Entry(6, (float) (Math.log(150)/Math.log(2))));

                dataSet = new LineDataSet(entries, "Label");
                lineData = new LineData(dataSet);
                chart.setData(lineData);
                */

                Log.d("oldDataSet", ""+dataSet);

                List<Entry> entries = new ArrayList<Entry>();
                entries.add(new Entry(6, (float) (Math.log(150)/Math.log(2))));

                dataSet = new LineDataSet(entries, "Label");
                lineData = new LineData(dataSet);
                chart.setData(lineData);

                Log.d("newDataSet", ""+dataSet);

                XAxis bottomAxis = chart.getXAxis();
                bottomAxis.setAxisMinimum(0);
                bottomAxis.setAxisMaximum(5);
                chart.notifyDataSetChanged();
                chart.invalidate();
            }
        });
    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
