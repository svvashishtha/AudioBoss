package app.audioboss;

import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // 1024 lines, with 4 points per line (2 space, 2 texture) each consisting of x and y,
    // so 8 floats per line.
    protected float[] mPointData = new float[1024 * 8];
    Visualizer visualizer;
    Button start, stop;
    TextView textView;
    GenericWaveRs genericWaveRs;
    RecordingSampler recordingSampler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        textView = findViewById(R.id.textView);
//        visualizer = new Visualizer(0);
        setOnClickListeners();

        recordingSampler = new RecordingSampler();
        recordingSampler.setSamplingInterval(100); // voice sampling interval
        recordingSampler.setAudioSampledDataReceiver(new RecordingSampler.AudioSampledDataReceiver() {
            @Override
            public void maxNumber(final float number) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(String.format("%f", number));
                    }
                });

            }
        });

    }

    private void setOnClickListeners() {
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* if (genericWaveRs == null) {
                    genericWaveRs = new GenericWaveRs();
                }
                genericWaveRs.start();*/
                recordingSampler.startRecording();
                start.setEnabled(false);
                stop.setEnabled(true);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                genericWaveRs.stop();
                recordingSampler.stopRecording();

                start.setEnabled(true);
                stop.setEnabled(false);
            }
        });
    }


    @Override
    protected void onDestroy() {
        /*if (genericWaveRs != null) {
            genericWaveRs.release();
        }*/
        if (recordingSampler != null) {
            recordingSampler.release();
        }
        super.onDestroy();
    }


}
