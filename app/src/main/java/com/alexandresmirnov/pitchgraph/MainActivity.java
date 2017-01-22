package com.alexandresmirnov.pitchgraph;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    LineDataSet dataSet;
    double timestamp = 0;
    float pitch = 150;

    Thread t;
    Thread audioDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRecordAudioPermission();

        // in this example, a LineChart is initialized from xml
        LineChart chart = (LineChart) findViewById(R.id.chart);

        List<Entry> entries = new ArrayList<Entry>();

        entries.add(new Entry(6, (float) (Math.log(150)/Math.log(2))));

        dataSet = new LineDataSet(entries, "Label");
        LineData lineData = new LineData(dataSet);
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

        startCapturingAudio();

        Handler h = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == 0){
                    updateUI();
                }else{
                    showErrorDialog();
                }
            }
        };


        t = new Thread() {

            LineChart chart = (LineChart) findViewById(R.id.chart);

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {

                        Log.d("interrupted", ""+ isAlive());

                        Thread.sleep(10);

                        double transformedPitch = Math.log(pitch) / Math.log(2);

                        Log.d("entry", "" + timestamp + ", " + transformedPitch);
                        dataSet.addEntry(new Entry((float) timestamp, (float) transformedPitch));
                        //dataSet.addEntry(new Entry((float) timestamp, 7));
                        chart.notifyDataSetChanged();
                        chart.invalidate();

                        XAxis bottomAxis = chart.getXAxis();
                        if (timestamp > 5) {

                            //bottomAxis.setAxisMinimum((float) timestamp - 5);
                            bottomAxis.setAxisMaximum((float) timestamp);

                            chart.moveViewToX((float) timestamp - 5);
                        }



                    }
                } catch (InterruptedException e) {
                    Log.d("thread", "interrupted");
                }
            }
        };

        t.start();


        Button stop = (Button) findViewById(R.id.stop);

        stop.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                t.interrupt();
            }
        });

        Button start = (Button) findViewById(R.id.stop);

        start.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                t.run();
            }
        });

        Button clear = (Button) findViewById(R.id.stop);

        clear.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                LineChart chart = (LineChart) findViewById(R.id.chart);

                List<Entry> entries = new ArrayList<Entry>();

                dataSet = new LineDataSet(entries, "Label");

                XAxis bottomAxis = chart.getXAxis();
                bottomAxis.setAxisMinimum(0);
                bottomAxis.setAxisMaximum(5);
                chart.notifyDataSetChanged();
                chart.invalidate();
            }
        });
    }

    protected void startCapturingAudio(){

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);
        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024,
            new PitchDetectionHandler() {

                @Override
                public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {

                    final float pitchInHz = pitchDetectionResult.getPitch();
                    final double audioEventTimeStamp = audioEvent.getTimeStamp();
                    runOnUiThread(
                        new Runnable() {



                            LineChart chart = (LineChart) findViewById(R.id.chart);

                            @Override
                            public void run() {



                                /*
                                TextView timestampView = (TextView) findViewById(R.id.timestamp);
                                TextView pitchView = (TextView) findViewById(R.id.pitch);
                                timestampView.setText("" + audioEventTimeStamp);
                                pitchView.setText("" + pitchInHz);
                                */
                                timestamp = audioEventTimeStamp;
                                pitch = pitchInHz;




                            }
                        });

                }
            }));

        audioDispatcher = new Thread(dispatcher,"Audio Dispatcher");
        audioDispatcher.start();



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
