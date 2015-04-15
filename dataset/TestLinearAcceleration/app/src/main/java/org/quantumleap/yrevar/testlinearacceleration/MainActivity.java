package org.quantumleap.yrevar.testlinearacceleration;

import android.content.Context;
import android.hardware.Sensor;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;
import java.util.Vector;


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    float x_l = 0, y_l = 0, z_l = 0;
    float integratedX = 0;


    float xt = 0, yt = 0, zt = 0;
    final float _accel_decimal_precision_ = 1000;

    final float _threshold_ = (float) 0.08;
    boolean startRecording = false;
    boolean recordUpdate = false;

    float[] gravSensorVals = new float[]{0,0,0};

    float[] gravSensorAvg = new float[]{0,0,0};

    Vector<Float> v_x = new Vector<Float>();
    Vector<Float> v_y = new Vector<Float>();
    Vector<Float> v_z = new Vector<Float>();

    Vector<Float> vv_x = new Vector<Float>();
    Vector<Float> vv_y = new Vector<Float>();
    Vector<Float> vv_z = new Vector<Float>();


    TextView test;
    TextView patternText;
    TextView displaceMentText;

    double distance[] = new double[]{0,0,0};
    double init_vel[] = new double[]{0,0,0};
    double total_Accel[] = new double[]{0,0,0};
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;

    float time = System.nanoTime();
    float timeOld = System.nanoTime();
    int count = 0;

    //Graph view
    GraphView graphview1 = null;
    GraphView graphview2 = null;
    LineGraphSeries<DataPoint> graph1_series = null;
    LineGraphSeries<DataPoint> graph2_series = null;


    private void resetvalues() {

        Log.i("LinearAccelerationTest", "Reset Values");
        initGraphView();
        v_x.clear();
        v_y.clear();
        v_z.clear();
        xt = 0; yt = 0; zt = 0;
        for(int i = 0; i < 3; i++){
            distance[i] = 0;
            init_vel[i] = 0;
            gravSensorAvg[i] = 0;
        }
        count = 0;
        timestamp = 0;
        recordUpdate = true;
    }

    private void startRecording() {

        v_x.clear();
        v_y.clear();
        v_z.clear();
        startRecording = true;
    }

    private void stopRecording() {

        startRecording = false;
        recordUpdate = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        test = (TextView) findViewById(R.id.textView);
        patternText = (TextView) findViewById(R.id.patternText);
        displaceMentText = (TextView) findViewById(R.id.displacement);
        Button resetButton = (Button) findViewById(R.id.button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                resetvalues();
            }
        });


        Button recordPatternButton = (Button) findViewById(R.id.record_pattern);
        recordPatternButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN ) {

                    startRecording();
                    return true;
                }
                else if (event.getAction() == MotionEvent.ACTION_UP ) {

                    stopRecording();
                    return true;
                }
                return false;
            }
        });


        // Realtime graph view
        graphview1 = (GraphView) findViewById(R.id.graph1);
        graphview2 = (GraphView) findViewById(R.id.graph2);
        initGraphView();
    }

    private void initGraphView() {

        graphview1.removeAllSeries();
        graphview2.removeAllSeries();
        //Hope old series objects get garbage collected
        graph1_series = new LineGraphSeries<DataPoint>();
        graphview1.addSeries(graph1_series);
        graphview1.getViewport().setYAxisBoundsManual(true);
        graphview1.getViewport().setMinY(-10);
        graphview1.getViewport().setMaxY(10);
        graphview1.getViewport().setXAxisBoundsManual(true);
        graphview1.getViewport().setMinX(0);
        graphview1.getViewport().setMaxX(100);

        graph2_series = new LineGraphSeries<DataPoint>();
        graphview2.addSeries(graph2_series);
        graphview2.getViewport().setYAxisBoundsManual(true);
        graphview2.getViewport().setMinY(-10);
        graphview2.getViewport().setMaxY(10);
        graphview2.getViewport().setXAxisBoundsManual(true);
        graphview2.getViewport().setMinX(0);
        graphview2.getViewport().setMaxX(100);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void dblIntegrate(long new_timestamp, float accel_values[]){

        final float dT = (new_timestamp - timestamp) * NS2S;

        double data[] = new double[3];
        double vel[] = new double[3];
        for(int i = 0; i < 3; i++){
            data[i] = (double)accel_values[i];
            total_Accel[i] += data[i];
            vel[i] = init_vel[i] + (total_Accel[i] * dT);
            distance[i] = distance[i] + (vel[i] * dT);
            init_vel[i] = vel[i];
        }
        timestamp = new_timestamp;
    }

    private static float getAccelerationPrecision(final float value, int decimal_precision) {

        if(decimal_precision == -1) {
            return  value;
        }
        else {
            return (float) (Math.floor(value*decimal_precision)/decimal_precision);
        }
    }

    static final float ALPHA = 0.0001f;

    protected float[] lowPass( float[] input, float[] output ) {

        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

//    private float[] highPass(float x, float y, float z) {
//
//        float[] filteredValues = new float[3];
//
//        gravity[0] = ALPHA * gravity[0] + (1 – ALPHA) * x;
//        gravity[1] = ALPHA * gravity[1] + (1 – ALPHA) * y;
//        gravity[2] = ALPHA * gravity[2] + (1 – ALPHA) * z;
//
//        filteredValues[0] = x – gravity[0];
//        filteredValues[1] = y – gravity[1];
//        filteredValues[2] = z – gravity[2];
//
//        return filteredValues;
//
//    }

    //Frequency
//    time = System.nanoTime();
//    double frequency = count++ / ((time - timeOld) / 1000000000.0);


    public float currentMovingAverage(float old_average, int length, float new_value) {

        return (old_average * (length- 1)/length) + (new_value / length);
    }


    public float lowPassFilter(float old_average, float alpha, float new_value) {

        return (old_average * (1 - alpha)) + (new_value * alpha);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {



        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

//            gravSensorVals = lowPass(event.values.clone(), gravSensorVals);


            //Calculate sample frequency
            time = System.nanoTime();
            // The event timestamps are irregular so we average to determine the
            // update frequency instead of measuring deltas.
            double frequency = count++ / ((time - timeOld) / 1000000000.0);

            //Deep copy values from event to vector gravSensorVals
            System.arraycopy(event.values, 0, gravSensorVals, 0,
                    event.values.length);

            for (int i = 0; i < 3; i++) {
                //0.005 seemed perceivably ok, W's the optimum value for the alpha here? And most importantly find out the formula which caculates it.
                gravSensorAvg[i] = lowPassFilter(gravSensorAvg[i], (float) 0.005, gravSensorVals[i]);
            }


            test.setText("x " +  String.format("%.6f", gravSensorVals[0]) + "\ny " + String.format("%.6f", gravSensorVals[1]) + "\nz " + String.format("%.6f", gravSensorVals[2]));

            //Debug filtered data
            //test.setText("x " +  String.format("%.6f", gravSensorAvg[0]) + "\ny " + String.format("%.6f", gravSensorAvg[1]) + "\nz " + String.format("%.6f", gravSensorAvg[2]));

            //Update realtime graph
            graph1_series.appendData(new DataPoint(count, gravSensorVals[0]), true, 100);
            graph2_series.appendData(new DataPoint(count, gravSensorVals[1]), true, 100);

            if(startRecording) {

                //Clamp output to certain threshold
                //Doesn't make any sense to apply here as it's ultimately a loss of information
//                if( Math.abs(event.values[0]) < _threshold_ && Math.abs(event.values[1]) < _threshold_ ) {
//
//                    v_x.add(0.0f);
//                    v_y.add(0.0f);
//                    v_z.add(0.0f);
//                }
//                else {

                    v_x.add(gravSensorVals[0]);
                    v_y.add(gravSensorVals[1]);
                    v_z.add(gravSensorVals[2]);
//                }
            }
            else if(recordUpdate == true) {

                //Debug
                Log.i("LinearAccelerationTest", "X vector " + v_x.toString());
                Log.i("LinearAccelerationTest", "Y vector " + v_y.toString());
                Log.i("LinearAccelerationTest", "Z vector " + v_z.toString());
//                Log.i("LinearAccelerationTest", "Here it goes..." + "\nchar_ascii = \"L-to-R\"" + "\nvec_len = " + v_x.size() + "\nXAccel = np.array(" + v_x.toString() + ")\nYAccel = np.array(" + v_y.toString()
//                + ")\nt = np.linspace(0, 1, vec_len)"
//                + "\nplotAccelerationValues(t, XAccel, char_ascii, 0)"
//                + "\nplotAccelerationValues(t, YAccel, char_ascii, 1)"
//                );

                //Generate Python plot script
//                Log.i("LinearAccelerationTest", "Here it goes..." + "\nchar_ascii = \"U-to-D\"" + "\nFreq = " + frequency + "\nvec_len = " + v_x.size() + "\nXAccel = np.array(" + v_x.toString() + ")\nYAccel = np.array(" + v_y.toString()
//                                + ")\nplotDim2Features(vec_len, XAccel, YAccel, char_ascii)"
//                );

                //Text box debug
                patternText.setText("pattern_x " + v_x.toString() + "\npattern_y " + v_y.toString() + "\npattern_z" + v_z.toString());
                recordUpdate = false;
            }


        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

//            magSensorVals = lowPass(evt.values.clone(), magSensorVals);
        }
    }

    Random mRand = new Random();

    private DataPoint[] generateData(Vector<Float> v_x) {
        int count = 100;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i<count; i++) {
            DataPoint v = null;
            if(i < v_x.size()) {
                v = new DataPoint(i, v_x.elementAt(i));
            }
            else {
                v = new DataPoint(i, 0);
            }
            values[i] = v;
        }
        return values;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
