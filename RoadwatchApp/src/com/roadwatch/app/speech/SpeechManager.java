package com.roadwatch.app.speech;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * Allow saying any text using TTS and/or listening for user speech
 */
public class SpeechManager implements RecognitionListener, OnInitListener
{
	private static final String TAG = SpeechManager.class.getSimpleName();

	private String prompt;

	private Context context;
	private SpeechRecognizer recognizer;

	private WordDetectorListener wordsListener;
	private TextToSpeech tts;

	private enum ListenMode
	{
		DONT_LISTEN, LISTEN_ONCE;
	}

	private ListenMode listenMode;

	public SpeechManager(Context context)
	{
		this(context, null);
	}

	public SpeechManager(Context context, WordDetectorListener wordsListener)
	{
		this.context = context;
		this.wordsListener = wordsListener;

		if (!SpeechRecognizer.isRecognitionAvailable(context))
			throw new RuntimeException("Speech recognition service is not available on this device!");
	}

	/**
	 * Text-to-speech initialization callback
	 * 
	 * @param initStatus
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onInit(int initStatus)
	{
		//check for successful instantiation
		if (initStatus == TextToSpeech.SUCCESS)
		{
			if (tts.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
				tts.setLanguage(Locale.US);

			tts.setOnUtteranceCompletedListener(new android.speech.tts.TextToSpeech.OnUtteranceCompletedListener()
			{
				@Override
				public void onUtteranceCompleted(String utteranceId)
				{
					// The TTS is used from within an activity so we can use the context as an activity and
					// use it to invoke the code on the UI thread.(SpeechRecognition can only run on the UI thread)
					if (context instanceof Activity)
					{
						Activity activity = (Activity) context;
						activity.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								// Close the Text to Speech Library
								if (tts != null)
								{
									tts.stop();
									tts.shutdown();
									tts = null;
									Log.d(TAG, "onUtteranceCompleted() - TTS Destroyed");
								}

								if (listenMode == ListenMode.LISTEN_ONCE)
								{
									if (recognizer != null)
										recognizer.startListening(SpeechRecognitionUtil.getRecognizerIntent(context));
									else
										Log.w(TAG, "onUtteranceCompleted() - SpeechManager was stopped before utterance completed");
								}
							}
						});
					}
				}
			});

			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Car number?");
			tts.speak(prompt, TextToSpeech.QUEUE_ADD, params);
		}
		else if (initStatus == TextToSpeech.ERROR)
		{
			Log.e(TAG, "Text To Speech failed to init !");
		}
	}

	public void say(String prompt)
	{
		this.prompt = prompt;
		listenMode = ListenMode.DONT_LISTEN;
		tts = new TextToSpeech(context, this);
	}

	public void sayAndListen(String prompt)
	{
		this.prompt = prompt;
		listenMode = ListenMode.LISTEN_ONCE;

		// We may invoke startListener() multiple times - no need to re-initialize the recognizer
		if (recognizer == null)
		{
			recognizer = SpeechRecognizer.createSpeechRecognizer(context);
			recognizer.setRecognitionListener(this);

			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			audioManager.requestAudioFocus(new AFChangeListener(), AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		}

		tts = new TextToSpeech(context, this);
	}

	public void stop()
	{
		if (tts != null)
		{
			tts.stop();
			tts.shutdown();
			tts = null;
		}

		if (recognizer != null)
		{
			recognizer.stopListening();
			recognizer.cancel();
			recognizer.destroy();
			recognizer = null;
		}
	}

	@Override
	public void onResults(Bundle results)
	{
		Log.d(TAG, "full results");
		receiveResults(results);
	}

	@Override
	public void onPartialResults(Bundle partialResults)
	{
		Log.d(TAG, "partial results");
		receiveResults(partialResults);
	}

	/**
	 * common method to process any results bundle from {@link SpeechRecognizer}
	 */
	private void receiveResults(Bundle results)
	{
		if ((results != null) && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION))
		{
			List<String> heard = SpeechRecognitionUtil.getHeardFromDirectPartial(results);//results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			float[] scores = SpeechRecognitionUtil.getConfidenceFromDirectPartial(results);//results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
			wordsListener.recieveResults(heard, scores);
		}
		else
		{
			wordsListener.recieveResults(null, null);
			Log.d(TAG, "no results");
		}
	}

	@Override
	public void onError(int errorCode)
	{
		if ((errorCode == SpeechRecognizer.ERROR_NO_MATCH) || (errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
		{
			Log.d(TAG, "didn't recognize anything");
			wordsListener.recieveResults(null, null);
		}
		else
		{
			Log.d(TAG, "FAILED " + SpeechRecognitionUtil.diagnoseErrorCode(errorCode));
		}
	}

	// other unused methods from RecognitionListener...

	@Override
	public void onReadyForSpeech(Bundle params)
	{
		Log.d(TAG, "ready for speech " + params);
	}

	@Override
	public void onEndOfSpeech()
	{
	}

	/**
	 * @see android.speech.RecognitionListener#onBeginningOfSpeech()
	 */
	@Override
	public void onBeginningOfSpeech()
	{
	}

	@Override
	public void onBufferReceived(byte[] buffer)
	{
	}

	@Override
	public void onRmsChanged(float rmsdB)
	{
	}

	@Override
	public void onEvent(int eventType, Bundle params)
	{
	}

	class AFChangeListener implements OnAudioFocusChangeListener
	{
		@Override
		public void onAudioFocusChange(int focusChange)
		{
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
			{
				Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			}
			else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
			{
				Log.d(TAG, "AUDIOFOCUS_GAIN");
			}
			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
			{
				Log.d(TAG, "AUDIOFOCUS_LOSS");
			}
		}
	}
}