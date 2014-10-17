package ru.mipt.diht.hatbot.hatdroid;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpResponse;
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
import java.net.URI;
import java.util.Locale;

public class MainActivity extends Activity implements OnInitListener {

    private TextToSpeech tts;
    private String host = "http://93.175.2.4:1752";
    private TextView wordView;
    private TextView explanationView;
    private JSONObject wordId;
    private String word = "";
    private Button good, bad, blame, getWord, getExplanation;
    private Handler h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tts = new TextToSpeech(this, this);
        wordView = (TextView) findViewById(R.id.wordView);
        explanationView = (TextView) findViewById(R.id.explanationView);
        good = (Button) findViewById(R.id.button2);
        bad = (Button) findViewById(R.id.button3);
        blame = (Button) findViewById(R.id.button4);
        getWord = (Button) findViewById(R.id.button5);
        getExplanation = (Button) findViewById(R.id.button);
        good.setClickable(false);
        bad.setClickable(false);
        blame.setClickable(false);
        getWord.setClickable(false);
        getExplanation.setClickable(false);
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Toast.makeText(
                        getApplicationContext(),
                        "Ваш ответ успешно отправлен!",
                        Toast.LENGTH_SHORT).show();
            };
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.main);
    }

    public void showToast(final String toast, final Context context)
    {
        runOnUiThread(new Runnable() {

            @Override
            public void run()
            {
                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean internetCheck() {
        ConnectionDetector connectionDetector = new ConnectionDetector(getApplicationContext());
        final Context context = this;
        if (connectionDetector.isConnectingToInternet()) {
            return true;
        }
        else {
            showToast("Нет подключения к интернету!", context);
            return false;
        }
    }

    private void buttonLock() {
        good.setClickable(false);
        bad.setClickable(false);
        blame.setClickable(false);
        getExplanation.setClickable(true);
    }

    public void generate(View v) {
        wordView.setText("");
        getExplanation.setClickable(true);
        if (internetCheck()) {
            (new TitleTask()).execute(host + "/random_word");
        }
    }

    public void getAnotherExplanation(View v) {
        good.setClickable(false);
        bad.setClickable(false);
        blame.setClickable(false);
        if (getExplanation.isClickable() && internetCheck()) {
            good.setClickable(true);
            bad.setClickable(true);
            blame.setClickable(true);
            (new DefinitionTask()).execute(host + "/explain?word=" + word);
        }
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
        if (getWord.isClickable()) {
            wordView.setText(word);
            good.setClickable(true);
            bad.setClickable(true);
            blame.setClickable(true);
            getWord.setClickable(false);
            getExplanation.setClickable(true);
        }
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
            if (response.getEntity() != null && !response.getEntity().equals("")) {
                h.sendEmptyMessage(0);
            }
        } catch (JSONException | IOException e) {
            Log.wtf("report", e);
        }
    }

    public void sendSuccess(View v) {
        buttonLock();
        if (good.isClickable()) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    report("SUCCESS");
                }

            };
            t.start();
        }
    }

    public void sendFail(View v) {
        buttonLock();
        if (bad.isClickable()) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    report("FAIL");
                }

            };
            t.start();
        }
    }

    public void sendBlame(View v) {
        buttonLock();
        if (blame.isClickable()) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    report("BLAME");
                }

            };
            t.start();
        }
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
                getWord.setClickable(true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }

    }

    public class ConnectionDetector {

        private Context _context;

        public ConnectionDetector(Context context){
            this._context = context;
        }

        public boolean isConnectingToInternet(){
            ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null)
            {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null)
                    for (NetworkInfo anInfo : info)
                        if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                            return true;
                        }

            }
            return false;
        }
    }
}