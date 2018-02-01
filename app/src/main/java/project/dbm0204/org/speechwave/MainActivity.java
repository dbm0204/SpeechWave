package project.dbm0204.org.speechwave;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class MainActivity extends AppCompatActivity {
    private WaveView mRealTimeWaveView;
    private RecordingThread mRecordingThread;
    private PlaybackThread mPlaybackThread;
    private static final int REQUEST_RECORD_AUDIO= 10;
    @Override
    public void onRequestPermissionsResult(int requestCode, String [] permissions, int[] grantResults){
        if(requestCode == REQUEST_RECORD_AUDIO && grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
            mRecordingThread.stopRecording();
        }

    }
    private void requestMicrophonePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)){
            // Show that Microphone acess is needed to record audio
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }
    private void startAudioRecordingSafe(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
            mRecordingThread.stopRecording();
        } else{
            requestMicrophonePermission();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id== R.id.action_settings)
            return true;
        return super.onOptionsItemSelected(item);
    }

    private short[] getAudioSample() throws IOException{
        InputStream is = getResources().openRawResource(R.raw.jinglebells);
        byte[] data;
        try {
            data = IOUtils.toByteArray(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] samples = new short[sb.limit()];
        sb.get(samples);
        return samples;
    }
    @Override
    protected void onStop(){
        super.onStop();
        mRecordingThread.stopRecording();
        mPlaybackThread.stopPlayback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRealTimeWaveView = (WaveView) findViewById(R.id.waveformView);
        mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data) {
                mRealTimeWaveView.setSamples(data);
            }
        });
        final WaveView mPlaybackView = (WaveView) findViewById(R.id.playbackWaveformView);
        Button fab = (Button) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mRecordingThread.recording()){
                    startAudioRecordingSafe();
                } else{
                    mRecordingThread.stopRecording();
                }
            }
        });
        short[] samples = null;
        try{
            samples = getAudioSample();

        } catch (IOException e) {
            e.printStackTrace();
        }
        if(samples!=null){
            final Button playBtn = (Button) findViewById(R.id.playFab);
            mPlaybackThread = new PlaybackThread(samples, new PlaybackListener() {
                @Override
                public void onProgress(int progress) {
                    mPlaybackView.setMarkerPosition(progress);
                }

                @Override
                public void onCompletion() {
                    mPlaybackView.setMarkerPosition(mPlaybackView.getAudioLength());
                }
            });
            mPlaybackView.setChannels(1);
            mPlaybackView.setSampleRate(PlaybackThread.SAMPLE_RATE);
            mPlaybackView.setSamples(samples);
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mPlaybackThread.playing()) {
                        mPlaybackThread.startPlayback();
                    } else {
                        mPlaybackThread.stopPlayback();
                    }
                }
            });
        }
    }

}
