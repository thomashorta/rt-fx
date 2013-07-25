package com.example.realtimefx;

import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	public final int SAMPLERATE = 44100;
	public int IN_CHANNELCONFIG = AudioFormat.CHANNEL_IN_MONO;
	public int OUT_CHANNELCONFIG = AudioFormat.CHANNEL_OUT_MONO;
	public int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	public ToggleButton button;
	public boolean isRecording = false;
	public AudioManager am;
	public Thread thread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Get the views from the main layout and set them if necessary
		button = (ToggleButton) findViewById(R.id.start);
		
		// Start AudioManager
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public void onClick(View v) {
		if (!am.isWiredHeadsetOn()) {
			button.toggle();
			Toast toast = Toast.makeText(getApplicationContext(), 
					"Please connect the headset to use the app.", Toast.LENGTH_SHORT);
			toast.show();
		}
		else if (!isRecording) {
			isRecording = true;
			thread = new Thread (new Runnable() {
				@Override
				public void run() {
					try { 
						realtimeFX();
					} catch (Throwable e) {
						Log.e("RealTime FX", "Audio Recording/Playing failed." + " | Exception: "+ e);
						runOnUiThread(new Runnable() {
							@Override
							public void run(){
								button.performClick();
								Toast toast = Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_SHORT);
								toast.show();
							}
						});
					}
				}
			});
			thread.start();			
		}
		else {
			isRecording = false;
			try {
				thread.join();
			} catch (Exception e) { Log.e("onClick", e.toString()); }
		}
	}
	
	@SuppressWarnings("deprecation")
	public void realtimeFX() throws Throwable {
			int bufferSize = 5*AudioRecord.getMinBufferSize(SAMPLERATE, IN_CHANNELCONFIG, ENCODING);
			final int bufferSizeShort = bufferSize/2;	// Since getMinBufferSize returns the size in bytes our short
												// buffer size needs to be half of that (short = 2 bytes)
			runOnUiThread(new Runnable(){
				public void run(){
					Toast.makeText(getApplicationContext(), "bufferSizeShort= " + bufferSizeShort, Toast.LENGTH_SHORT).show();
				}
			});
			
			short[] buffer = new short[bufferSizeShort];
			short[] music = new short [bufferSizeShort];
			Arrays.fill(music, (short)0);
			
			Log.i("RealTime FX", "Created buffers");
			
			// Create a new AudioRecord object to record the audio.
			AudioRecord audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC, SAMPLERATE,
					IN_CHANNELCONFIG, ENCODING, bufferSize);
			Log.i("RealTime FX", "Created AudioRecord");
			
			// Create a new AudioTrack object using the same parameters as the AudioRecord
			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					SAMPLERATE, OUT_CHANNELCONFIG,
					ENCODING, bufferSizeShort, AudioTrack.MODE_STREAM);
			Log.i("RealTime FX", "Created Track");
			
			// Start recording and playing
			audioRecord.startRecording();
			audioTrack.play();

			while (isRecording) {
				if (!am.isWiredHeadsetOn()) throw new HeadsetUnpluggedException();
				audioRecord.read(buffer, 0, bufferSizeShort);	// Read the mic
				// Reverb effect processing
					for (int j = 0; j < bufferSizeShort; j++)
						music[j] = (short)((float)buffer[j] + (float)0.35*music[j]);

				audioTrack.write(music, 0, bufferSizeShort);	// Play the sound
			}
			
			audioRecord.stop();
			audioTrack.stop();
			audioRecord.release();
	}
	
	class HeadsetUnpluggedException extends Exception {
		// AUTOMATICALLY GENERATED serialVersionUID
		private static final long serialVersionUID = 5677812739711721193L;
		private String detail;
		
		HeadsetUnpluggedException() {
			this.detail = "Headset has been unplugged";
		}
		
		HeadsetUnpluggedException(String detail) {
			this.detail = detail;
		}
		
		public String toString(){
			return "HeadsetUnpluggedException[" + detail + "]";
		}
	}

}
