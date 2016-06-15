package com.freshollie.audiofocus;

import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.content.Context;
import android.util.Log;
import anywheresoftware.b4a.BA;

@BA.ShortName("AudioFocus")
@BA.Author("Freshollie")
@BA.Version(1)
@BA.Events(values={"onfocuslost",
		"ontransient",
		"ontransientcanduck",
		"ongain"})


public class AudioFocus{
	
	private AudioManager am;
	private BA ba;
	private String eventName;
	
  
	@SuppressWarnings("static-access")
	public void Initialize(BA ba, String EventName)
	{
		this.ba = ba;
		this.eventName = EventName.toLowerCase(BA.cul);
		this.am = (AudioManager) ba.applicationContext.getSystemService(Context.AUDIO_SERVICE);
		Log.i("B4A", "AudioFocusListener has been initialized.");
	}
  
	public boolean requestFocus(){
		int requestResult = this.am.requestAudioFocus(
                            mAudioFocusListener, AudioManager.STREAM_MUSIC,
                            //AudioManager.AUDIOFOCUS_GAIN);
                            //AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

		if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.d("B4A", "Successfull to request audioFocus listener");
			return true;

			

		}

		else if(requestResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED)
			{
			return false;
			}

		 else {
			 Log.d("B4A", "Failure to request focus listener");
			return false;
			
		}
		
	}
 
	
	public void abandonAudioFocus() {
        //Abandon audio focus
        int result = this.am.abandonAudioFocus(mAudioFocusListener);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("B4A", "Audio focus abandoned");
        } else {
            Log.e("B4A", "Audio focus failed to abandon");
        }

    }
	
	
	private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            // AudioFocus is a new feature: focus updates are made verbose on
            // purpose
			
            switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                AudioFocus.this.ba.raiseEvent(this, AudioFocus.this.eventName + "_onfocuslost");
                Log.d("B4A", "AudioFocus: received AUDIOFOCUS_LOSS");
                Log.d("B4A", AudioFocus.this.eventName + "_onfocuslost");
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                AudioFocus.this.ba.raiseEvent(this, AudioFocus.this.eventName + "_ontransient");
                Log.d("B4A", "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				AudioFocus.this.ba.raiseEvent(this, AudioFocus.this.eventName + "_ontransientcanduck");
                Log.d("B4A",  "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                break;

            case AudioManager.AUDIOFOCUS_GAIN:
                AudioFocus.this.ba.raiseEvent(this, AudioFocus.this.eventName + "_ongain");
                Log.d("B4A", "AudioFocus: received AUDIOFOCUS_GAIN");
                break;

            default:
                Log.e("B4A", "Unknown audio focus change code");
            }

        }
    };
}
