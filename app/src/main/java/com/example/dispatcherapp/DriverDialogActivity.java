package com.example.dispatcherapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DriverDialogActivity extends AppCompatActivity {

    String driv_name_and_id;
    String driv_name, driv_id;

    ListView listView;
    ArrayAdapter<String> adapter;
    ArrayList<String> lines;
    SharedPreferences sharedPreferences;

    EditText eTxtMsg;
    ImageButton btnSend;

    //JSON node names
    private static final String TAG_DISP_ID = "disp_id";
    private static final String TAG_DRIV_ID = "driv_id";
    private static final String TAG_TEXT = "msg_text";
    private static final String TAG_MESSAGES = "messages";
    private static final String TAG_SUCCESS = "success";

    //url to
    private static final String url_load_dialog = "http://a0534961.xsph.ru/load_dialog.php";
    private static final String url_send_message = "http://a0534961.xsph.ru/send_message_from_dispatcher.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dialog);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //открытие файла настроек
        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        listView = findViewById(R.id.listViewDialog);
        eTxtMsg = findViewById(R.id.eTextMessage);
        btnSend = findViewById(R.id.btnSend);

        //получение строки из прошлой активити
        driv_name_and_id = getIntent().getStringExtra("driver");

        String[] sName = driv_name_and_id.split(" \\[");
        lines = new ArrayList<>();

        driv_name = sName[0];
        driv_id = sName[1];
        //Log.d("driv_name", driv_name);

        Timer myTimer = new Timer(); // Создаем таймер
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
                new LoadDialog().execute();
            };
        }, 0L, 10L * 1000); // интервал - 10000 миллисекунд, 0 миллисекунд до первого запуска.


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = eTxtMsg.getText().toString();
                if (!msg.equals(""))
                {
                    new SendMessage().execute(msg);
                    eTxtMsg.setText("");
                }
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    class SendMessage extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //Snackbar.make(findViewById(R.id.MessagesActivity), "Поиск в базе данных..", Snackbar.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... args){
            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody =
                        new FormBody.Builder()
                                .add(TAG_DISP_ID, sharedPreferences.getString("id", "0"))
                                .add(TAG_DRIV_ID, driv_id)
                                .add(TAG_TEXT, args[0])
                                .build();
                Request request =
                        new Request.Builder()
                                .url(url_send_message)
                                .post(formBody)
                                .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);


                Log.d("disp_id", sharedPreferences.getString("id", "0"));
                Log.d("driv_id", driv_id);
                JSONObject json = new JSONObject(response.body().string());

                int success = json.getInt(TAG_SUCCESS);
                Log.d("success", TAG_SUCCESS);

                if(success > 0){
                    lines.add(sharedPreferences.getString("name", "Я").split(" ")[1]
                            + ": " + args[0]);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Stuff that updates the UI
                            listView.setAdapter(adapter);
                        }
                    });
                }



            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url){
        }
    }


    class LoadDialog extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //Snackbar.make(findViewById(R.id.MessagesActivity), "Поиск в базе данных..", Snackbar.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... args){
            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody =
                        new FormBody.Builder()
                                .add(TAG_DISP_ID, sharedPreferences.getString("id", "0"))
                                .add(TAG_DRIV_ID, driv_id)
                                .build();
                Request request =
                        new Request.Builder()
                                .url(url_load_dialog)
                                .post(formBody)
                                .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                JSONObject json = new JSONObject(response.body().string());

                int success = json.getInt("success");
                JSONArray messages = json.getJSONArray("messages");


                if(success >= 1){
                    lines.clear();
                    for (int i = 0; i < success; i++)
                    {
                        JSONObject message = messages.getJSONObject(i);
                        String text = message.getString("msg_text");
                        String line = "";
                        int from = message.getInt("from_id_disp");
                        if (from == 0) {
                            String[] name = driv_name.split(" ");
                            line = name[1];
                        }
                        else {
                            String[] name = sharedPreferences.getString("name", "Я").split(" ");
                            line = name[1];
                        }
                        line += ": " + text;

                        lines.add(line);
                    }

                    //Log.d("lines", lines.toString());

                    adapter = new ArrayAdapter<String>(
                            DriverDialogActivity.this,
                            android.R.layout.simple_list_item_1,
                            lines);

                    //работа с listView в основном потоке
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Stuff that updates the UI
                            listView.setAdapter(adapter);
                        }
                    });
                }else
                {
                    //not found
                    Snackbar.make(findViewById(R.id.DriverDialogActivity), "Не найдено.", Snackbar.LENGTH_SHORT).show();

                }

                //удаление уведомлений из этого чата
                NotificationManager notificationManager = (NotificationManager) DriverDialogActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                String CHANNEL_ID = "DispatcherApp_channel_" + driv_id;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = "DispatcherApp_channel";
                    String Description = "This is DispatcherApp channel";
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                    mChannel.setDescription(Description);
                    mChannel.enableLights(true);
                    mChannel.setLightColor(Color.RED);
                    mChannel.enableVibration(true);
                    mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    mChannel.setShowBadge(false);
                    notificationManager.createNotificationChannel(mChannel);
                }
                notificationManager.cancelAll();
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url){
        }
    }

}