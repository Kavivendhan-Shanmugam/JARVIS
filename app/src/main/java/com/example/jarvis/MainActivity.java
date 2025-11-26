package com.example.jarvis;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SpeechRecognizer recognizer;
    private TextView textView;
    private TextToSpeech tts;

    private static final String CLIENT_ID = "4ce5e4b58cd049dbb5539a6328f73ec0";
    private static final String REDIRECT_URI = "com.example.jarvis://callback";
    private SpotifyAppRemote spotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewByIdAndSetupRecognizer();
        initTextToSpeech();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote appRemote) {
                        spotifyAppRemote = appRemote;
                        Log.d(TAG, "Connected to Spotify!");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(TAG, "Failed to connect to Spotify", throwable);
                    }
                });
    }

    private void initTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if (tts.getEngines().size() == 0) {
                    Toast.makeText(MainActivity.this, "No TTS engine found", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Voice selection logic...
                // FIX: Replaced call to non-existent 'Functions.wishMe()' with a direct greeting.
                speak("System online. I am ready to assist.");
            } else {
                Toast.makeText(MainActivity.this, "TTS Initialization failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void speak(String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void findViewByIdAndSetupRecognizer() {
        textView = findViewById(R.id.textView);
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                // ... (listener methods)
                 @Override
                public void onResults(Bundle results) {
                    ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (result != null && !result.isEmpty()) {
                        Toast.makeText(MainActivity.this, result.get(0), Toast.LENGTH_SHORT).show();
                        textView.setText(result.get(0));
                        response(result.get(0));
                    }
                }
                 // ... (other listener methods)
                @Override
                public void onBeginningOfSpeech() {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {}
                @Override
                public void onError(int error) {}
                @Override
                public void onEvent(int eventType, Bundle params) {}
                @Override
                public void onPartialResults(Bundle partialResults) {}
                @Override
                public void onReadyForSpeech(Bundle params) {}
                @Override
                public void onRmsChanged(float rmsdB) {}
            });
        }
    }

    private void response(String msg) {
        String lowerCaseMsg = msg.toLowerCase(Locale.ROOT);
        if (lowerCaseMsg.contains("hi")) {
            speak("Hello master , Jarvis at your command , what can I help with");
        }
        if (lowerCaseMsg.contains("time")) {
            Date date = new Date();
            String time = DateUtils.formatDateTime(this, date.getTime(), DateUtils.FORMAT_SHOW_TIME);
            speak("The current time is " + time);
        }
        if (lowerCaseMsg.contains("date")) {
            SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            String date = dt.format(cal.getTime());
            speak("The current date is " + date);
        }
        if (lowerCaseMsg.contains("google")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            startActivity(intent);
        }
        if (lowerCaseMsg.contains("youtube")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
            startActivity(intent);
        }
        if (lowerCaseMsg.contains("search")) {
            String searchQuery = lowerCaseMsg.replace("search", "").trim();
            if (!searchQuery.isEmpty()) {
                speak("Searching for " + searchQuery);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(searchQuery)));
                startActivity(intent);
            }
        }
        if (lowerCaseMsg.contains("remember")) {
            speak("okay master i'll remember that");
            writetofile(lowerCaseMsg.replace("remember", "").trim());
        }
        if (lowerCaseMsg.contains("know")) {
            String data = readfromfile();
            speak("Yes master , you told me to remember that " + data);
        }
        if (lowerCaseMsg.contains("play")) {
            String songName = lowerCaseMsg.replace("play", "").trim();
            if (!songName.isEmpty()) {
                if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
                    speak("Playing " + songName);
                    spotifyAppRemote.getPlayerApi().play("spotify:search:" + songName);
                } else {
                    speak("I'm not connected to Spotify yet. Please try again in a moment.");
                }
            } else {
                speak("What song would you like to play?");
            }
        }
    }

    private String readfromfile() {
        String ret = "";
        try {
            InputStream inputStream = openFileInput("data.txt");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("Exception", "file not found: " + e.toString());
        } catch (IOException e) {
            Log.e("Exception", "can not read file: " + e.toString());
        }
        return ret;
    }

    private void writetofile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("data.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void startRecording(View view) {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.RECORD_AUDIO)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                        recognizer.startListening(intent);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(spotifyAppRemote);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
