package app.audioboss;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.tyorikan.voicerecordingvisualizer.VisualizerView;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Sampling AudioRecord Input
 * This output send to {@link VisualizerView}
 * <p>
 * Created by tyorikan on 2015/06/09.
 */
public class RecordingSampler {

    private static final int RECORDING_SAMPLE_RATE = 44100;
    private static volatile BlockingQueue<DataObject> dataQueue;
    private final File folder;
    String TAG = "RecordingSampler";
    AudioSampledDataReceiver audioSampledDataReceiver;
    INDArray floatBufArray;
    int dataReceived = 0;
    DataObject dataObject;
    volatile FileWriter ostream;
    volatile BufferedWriter out;
    private AudioRecord mAudioRecord;
    private volatile boolean mIsRecording;
    private int mBufSize;
    private int mSamplingInterval = 200;
    private Timer mTimer;
    private List<VisualizerView> mVisualizerViews = new ArrayList<>();
    private FlushDataToFile dataFlushTask;
    private Thread dataFlushTaskWorkerUpper;
    ICsvBeanWriter beanWriter;
    public RecordingSampler() {
        initAudioRecord();
        String path =
                Environment.getExternalStorageDirectory() + File.separator + "audioSamples";
        // Create the folder.
        folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    /**
     * link to VisualizerView
     *
     * @param visualizerView {@link VisualizerView}
     */
    public void link(VisualizerView visualizerView) {
        mVisualizerViews.add(visualizerView);
    }

    public void setAudioSampledDataReceiver(AudioSampledDataReceiver audioSampledDataReceiver) {
        this.audioSampledDataReceiver = audioSampledDataReceiver;
    }

    /**
     * this.audioSampledDataReceiver = audioSampledDataReceiver;
     * setter of samplingInterval
     *
     * @param samplingInterval interval volume sampling
     */
    public void setSamplingInterval(int samplingInterval) {
        mSamplingInterval = samplingInterval;
    }

    /**
     * getter isRecording
     *
     * @return true:recording, false:not recording
     */
    public boolean isRecording() {
        return mIsRecording;
    }

    private void initAudioRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(
                RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            mBufSize = bufferSize;
        }




    }

    /**
     * start AudioRecord.read
     */
    public void startRecording() {
        mTimer = new Timer();
        mAudioRecord.startRecording();
        mIsRecording = true;
        dataQueue = new LinkedBlockingQueue<>();

        File file = new File(folder, "sample.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {

//            ostream = new FileWriter(file);
//            out = new BufferedWriter(ostream);

            beanWriter = new CsvBeanWriter(new FileWriter(file),
                    CsvPreference.STANDARD_PREFERENCE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        runRecording();
        if (dataFlushTask == null) {
            dataFlushTask = new FlushDataToFile();
        }
        if (dataFlushTaskWorkerUpper == null || !dataFlushTaskWorkerUpper.isAlive()) {
            dataFlushTaskWorkerUpper = new Thread(dataFlushTask);
            dataFlushTaskWorkerUpper.start();
        }

    }

    /**
     * stop AudioRecord.read
     */
    public void stopRecording() {
        mIsRecording = false;
        mTimer.cancel();

    }

    private void runRecording() {
        final short buf[] = new short[mBufSize];
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // stop recording
                if (!mIsRecording) {
                    mAudioRecord.stop();
                    return;
                }
                mAudioRecord.read(buf, 0, mBufSize);
//                FloatBuffer fb = ByteBuffer.wrap(buf).asFloatBuffer();
//                fb.get(floatBuf);

                floatBufArray = Nd4j.create(shortToFloat(buf));
                Number max = floatBufArray.maxNumber();
                Number min = floatBufArray.minNumber();

                dataObject = new DataObject(max.doubleValue(), min.doubleValue());
                dataQueue.add(dataObject);
                if (floatBufArray != null) {
                    if (audioSampledDataReceiver != null) {
                        audioSampledDataReceiver.maxNumber(dataReceived++);
                    }
                }


            }
        }, 0, mSamplingInterval);
    }


    /**
     * release member object
     */
    public void release() {
        stopRecording();
        mAudioRecord.release();
        mAudioRecord = null;
        mTimer = null;
    }

    private float[] shortToFloat(short[] audio) {

        float[] converted = new float[audio.length];

        for (int i = 0; i < converted.length; i++) {
            converted[i] = audio[i] / 1f;
        }

        return converted;
    }

    public interface AudioSampledDataReceiver {
        public void maxNumber(float number);
    }

    public class FlushDataToFile implements Runnable {
        boolean firstWrite;
        @Override
        public void run() {
            firstWrite = true;
            while (isRecording() || !dataQueue.isEmpty()) {


                if (firstWrite) {//if the object being written is not the first to be written to
                    // file then headers should be added to file
                    firstWrite = false;
                    try {

//                        out.write(DataObject.getHeader());
                        beanWriter.writeHeader(DataObject.getHeaders());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        DataObject dataObject = dataQueue.poll(200, TimeUnit.MILLISECONDS);
                        if (dataObject!=null) {
                            String stringToWrite = dataObject.toString();
                            Log.d(TAG, stringToWrite);
//                            out.append(stringToWrite);
                            beanWriter.write(dataObject, DataObject.getHeaders(), DataObject.rawObjectProcessor);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                Log.d(TAG, "=====while end=====");

            }
            Log.d(TAG, "================exit while===========");

            try {
                if (beanWriter != null) {
                    beanWriter.close();
                }
//                out.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
