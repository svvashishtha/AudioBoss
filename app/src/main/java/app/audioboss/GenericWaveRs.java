package app.audioboss;

import android.os.Handler;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Sampler;

import java.util.Arrays;


public class GenericWaveRs {
    private static final int RSID_STATE = 0;
    private static final int RSID_POINTS = 1;
    private static final int RSID_LINES = 2;
    private static final int RSID_PROGRAMVERTEX = 3;
    private final Handler mHandler = new Handler();
    protected WorldState mWorldState = new WorldState();
    protected Allocation mPointAlloc;
    // 1024 lines, with 4 points per line (2 space, 2 texture) each consisting of x and y,
    // so 8 floats per line.
    protected float[] mPointData = new float[1024 * 8];
    protected AudioCapture mAudioCapture = null;
    protected int[] mVizData = new int[1024];
    private boolean mVisible;
    private final Runnable mDrawCube = new Runnable() {
        public void run() {
            updateWave();
        }
    };
    private int mTexId;
    private Sampler mSampler;
    private Allocation mTexture;


    public GenericWaveRs() {
        mAudioCapture = new AudioCapture(AudioCapture.TYPE_PCM, 1024);
        int outlen = mPointData.length / 8;
        int half = outlen / 2;
        for (int i = 0; i < outlen; i++) {
            mPointData[i * 8] = i - half;          // start point X (Y set later)
            mPointData[i * 8 + 2] = 0;                 // start point S
            mPointData[i * 8 + 3] = 0;                 // start point T
            mPointData[i * 8 + 4] = i - half;        // end point X (Y set later)
            mPointData[i * 8 + 6] = 1.0f;                 // end point S
            mPointData[i * 8 + 7] = 0f;              // end point T
        }
    }

    public void start() {
        mVisible = true;
        if (mAudioCapture != null) {
            mAudioCapture.start();
        }
        SystemClock.sleep(200);
        updateWave();
    }

    public void release() {
        if (mAudioCapture != null) {
            mAudioCapture.release();
        }
    }

    public void stop() {
        mVisible = false;
        if (mAudioCapture != null) {
            mAudioCapture.stop();
        }
        updateWave();
    }

    public void update() {
        if (mAudioCapture != null) {
            mVizData = mAudioCapture.getFormattedData(1, 1);

            System.out.println(mVizData.length);
        } else {
            Arrays.fill(mVizData, 0);
        }


    }

    void updateWave() {
        mHandler.removeCallbacks(mDrawCube);
        if (!mVisible) {
            return;
        }
        mHandler.postDelayed(mDrawCube, 20);
        update();
        mWorldState.waveCounter++;
    }

    protected static class WorldState {
        public float yRotation;
        public int idle;
        public int waveCounter;
        public int width;
    }

}
