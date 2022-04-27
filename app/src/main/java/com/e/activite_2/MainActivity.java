package com.e.activite_2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity {

    //private static final String SERVICE_URL = "http://192.168.1.21:8080/RestWebServiceDemo/rest/person";
    private static final String SERVICE_URL = "https://nexio-order-service.herokuapp.com/swagger-ui/api/clients";

    String TAG = "Activite_2";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void retrieveSampleData(View vw) {

        String sampleURL = SERVICE_URL + "/sample";

        WebServiceTask wst = new WebServiceTask(WebServiceTask.GET_TASK, this, "GETting data...");

        wst.execute(sampleURL);

    }

    public void clearControls(View vw) {

        EditText edFirstName = (EditText) findViewById(R.id.first_name);
        EditText edLastName = (EditText) findViewById(R.id.last_name);
        EditText edEmail = (EditText) findViewById(R.id.email);

        edFirstName.setText("");
        edLastName.setText("");
        edEmail.setText("");

    }

    public void postData(View vw) {

        EditText edFirstName = (EditText) findViewById(R.id.first_name);
        EditText edLastName = (EditText) findViewById(R.id.last_name);
        EditText edEmail = (EditText) findViewById(R.id.email);

        String firstName = edFirstName.getText().toString();
        String lastName = edLastName.getText().toString();
        String email = edEmail.getText().toString();

        if (firstName.equals("") || lastName.equals("") || email.equals("")) {
            Toast.makeText(this, "Entrer tous les champs obligatoires SVP.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        WebServiceTask wst = new WebServiceTask(WebServiceTask.POST_TASK, this, "Posting data...");

        wst.addNameValuePair("firstName", firstName);
        wst.addNameValuePair("lastName", lastName);
        wst.addNameValuePair("email", email);

        // the passed String is the URL we will POST to
        wst.execute(SERVICE_URL);

    }

    public void handleResponse(String response) {

        EditText edFirstName = (EditText) findViewById(R.id.first_name);
        EditText edLastName = (EditText) findViewById(R.id.last_name);
        EditText edEmail = (EditText) findViewById(R.id.email);

        edFirstName.setText("");
        edLastName.setText("");
        edEmail.setText("");

        try {

            JSONObject jso = new JSONObject(response);

            String firstName = jso.getString("firstName");
            String lastName = jso.getString("lastName");
            String email = jso.getString("email");

            edFirstName.setText(firstName);
            edLastName.setText(lastName);
            edEmail.setText(email);

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

    }

    private void hideKeyboard() {

        InputMethodManager inputManager = (InputMethodManager) MainActivity.this
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if(MainActivity.this.getCurrentFocus() != null){
            inputManager.hideSoftInputFromWindow(
                    MainActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        }
    }

    @SuppressLint("StaticFieldLeak")
    private class WebServiceTask extends AsyncTask<String, Integer, String> {

        public static final int POST_TASK = 1;
        public static final int GET_TASK = 2;

        private static final String TAG = "WebServiceTask";

        // connection timeout, in milliseconds (waiting to connect)
        private static final int CONN_TIMEOUT = 3000;

        // socket timeout, in milliseconds (waiting for data)
        private static final int SOCKET_TIMEOUT = 5000;

        private int taskType = GET_TASK;
        private Context mContext = null;
        private String processMessage = "Processing...";

        private final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

        private ProgressDialog pDlg = null;

        public WebServiceTask(int taskType, Context mContext, String processMessage) {

            this.taskType = taskType;
            this.mContext = mContext;
            this.processMessage = processMessage;
        }

        public void addNameValuePair(String name, String value) {

            params.add(new BasicNameValuePair(name, value));
        }

        private void showProgressDialog() {

            pDlg = new ProgressDialog(mContext);
            pDlg.setMessage(processMessage);
            pDlg.setProgressDrawable(mContext.getWallpaper());
            pDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDlg.setCancelable(false);
            pDlg.show();

        }

        @Override
        protected void onPreExecute() {

            hideKeyboard();
            showProgressDialog();

        }

        protected String doInBackground(String... urls) {

            String url = urls[0];
            String result = "";

            HttpResponse response = doResponse(url);

            if (response == null) {
                return result;
            } else {

                try {

                    result = inputStreamToString(response.getEntity().getContent());

                } catch (IllegalStateException | IOException e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);

                }

            }

            return result;
        }

        @Override
        protected void onPostExecute(String response) {

            handleResponse(response);
            pDlg.dismiss();

        }

        // Establish connection and socket (data retrieval) timeouts
        private HttpParams getHttpParams() {

            HttpParams htpp = (HttpParams) new BasicHttpParams();

            HttpConnectionParams.setConnectionTimeout(htpp, CONN_TIMEOUT);
            HttpConnectionParams.setSoTimeout(htpp, SOCKET_TIMEOUT);

            return htpp;
        }

        private HttpResponse doResponse(String url) {

            // Use our connection and data timeouts as parameters for our
            // DefaultHttpClient
            HttpClient httpclient = new DefaultHttpClient((ClientConnectionManager) getHttpParams());

            HttpResponse response = null;

            try {
                switch (taskType) {

                    case POST_TASK:
                        HttpPost httppost = new HttpPost(url);
                        // Add parameters
                        httppost.setEntity(new UrlEncodedFormEntity(params));

                        response = httpclient.execute(httppost);
                        break;
                    case GET_TASK:
                        HttpGet httpget = new HttpGet(url);
                        response = httpclient.execute(httpget);
                        break;
                }
            } catch (Exception e) {

                Log.e(TAG, e.getLocalizedMessage(), e);

            }

            return response;
        }

        private String inputStreamToString(InputStream is) {

            String line;
            StringBuilder total = new StringBuilder();

            // Wrap a BufferedReader around the InputStream
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            try {
                // Read response until the end
                while ((line = rd.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }

            // Return full string
            return total.toString();
        }
    }


}