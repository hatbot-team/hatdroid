package ru.mipt.diht.hatbot.hatdroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Locale;

public class MainActivity extends Activity implements OnInitListener {

    private TextToSpeech tts;
    private String host = "http://93.175.2.4:1752";
    private TextView wordView;
    private TextView explanationView;
    private JSONObject wordId;
    private String word = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tts = new TextToSpeech(this, this);
        wordView = (TextView) findViewById(R.id.wordView);
        explanationView = (TextView) findViewById(R.id.explanationView);
    }

    public void generate(View v) {
        (new TitleTask()).execute(host + "/random_word");
    }

    @Override
    public void onInit(int code) {
        if (code == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
        } else {
            tts = null;
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void showWord(View v) {
        wordView.setText(word);
    }

    private void report(String ans) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(host + "/statistics/update");
        try {
            JSONObject holder = new JSONObject();
            holder.put("id", wordId);
            holder.put("result", ans);
            StringEntity se = new StringEntity(holder.toString());
            post.addHeader("content-type", "application/json");
            post.setEntity(se);
            HttpResponse response = client.execute(post);
            Log.d("resp", EntityUtils.toString(response.getEntity()));
        } catch (UnsupportedEncodingException e) {
            Log.wtf("report", e);
        } catch (ClientProtocolException e) {
            Log.wtf("report", e);
        } catch (IOException e) {
            Log.wtf("report", e);
        } catch (JSONException e) {
            Log.wtf("report", e);
        }
    }

    public void sendSuccess(View v) {

        Thread t = new Thread() {

            @Override
            public void run() {
                report("SUCCESS");
            }

        };
        t.start();
    }

    public void sendFail(View v) {

        Thread t = new Thread() {

            @Override
            public void run() {
                report("FAIL");
            }

        };
        t.start();
    }

    public void sendBlame(View v) {

        Thread t = new Thread() {

            @Override
            public void run() {
                report("BLAME");
            }

        };
        t.start();
    }

    private class TitleTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                HttpClient httpclient = new DefaultHttpClient();

                HttpGet request = new HttpGet();
                URI website = new URI(params[0]);
                request.setURI(website);
                HttpResponse response = httpclient.execute(request);

                return new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent())).readLine();
            } catch (Exception e) {
                Log.wtf("except", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            word = result;
            (new DefinitionTask()).execute(host + "/explain?word=" + result);
            super.onPostExecute(result);
        }
    }

    private class DefinitionTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                HttpClient httpclient = new DefaultHttpClient();

                HttpGet request = new HttpGet();
                URI website = new URI(params[0]);
                request.setURI(website);
                HttpResponse response = httpclient.execute(request);

                return new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent())).readLine();
            } catch (Exception e) {
                Log.wtf("except", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                explanationView.setText(json.getString("text"));
                if (tts != null) {
                    if (!tts.isSpeaking()) {
                        tts.speak(json.getString("text"), TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                wordId = json.getJSONObject("id");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }

    }

}