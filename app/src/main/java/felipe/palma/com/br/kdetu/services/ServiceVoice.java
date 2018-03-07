package felipe.palma.com.br.kdetu.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by Roberlandio on 08/05/2017.
 */

public class ServiceVoice extends Service {

    protected static AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamSolo;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL_LISTENING = 2;

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());



    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mIsCountDownOn){
            mNoSpeechCountDown.cancel();
        }
        if(mSpeechRecognizer !=null){
            mSpeechRecognizer.destroy();
        }
    }

    protected static class IncomingHandler extends Handler{
        private WeakReference<ServiceVoice> mTarget;

        IncomingHandler(ServiceVoice target){
            this.mTarget=new WeakReference<ServiceVoice>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            final ServiceVoice target = mTarget.get();

            switch (msg.what){
                case MSG_RECOGNIZER_START_LISTENING:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                        if(!mIsStreamSolo){
                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL,true);
                            mIsStreamSolo = true;
                        }
                    }
                    if(!target.mIsListening){
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                    }
                    break;
                case MSG_RECOGNIZER_CANCEL_LISTENING:
                    if(mIsStreamSolo){
                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL,false);
                        mIsStreamSolo = true;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    break;
            }
        }
    }

    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000,5000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            mIsCountDownOn = false;
            Message message = Message.obtain(null,MSG_RECOGNIZER_CANCEL_LISTENING);
            try{
                mServerMessenger.send(message);
                message = Message.obtain(null,MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }catch (RemoteException e){
                Log.e("ERROR",e.getMessage());
            }
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected class SpeechRecognitionListener implements RecognitionListener{

        @Override
        public void onReadyForSpeech(Bundle params) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();
            }
            Log.d("SERVICE_VOICE","onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            if(mIsCountDownOn){
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            Log.d("SERVICE_VOICE","OnBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

            Log.d("SERVICE_VOICE","onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            if(mIsCountDownOn){
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null,MSG_RECOGNIZER_START_LISTENING);
            try{
                mServerMessenger.send(message);
            }catch (RemoteException e){
                Log.d("ERROR",e.getMessage());
            }
        }

        @Override
        public void onResults(Bundle results) {
            Log.d("SERVICE_VOICE","RESULTADO: " + results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0));
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }
}