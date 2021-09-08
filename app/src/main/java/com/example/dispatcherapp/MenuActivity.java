package com.example.dispatcherapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MenuActivity extends AppCompatActivity {

    Button btnMap, btnMes, btnWork, btnRep;

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_DISP_ID = "disp_id";

    SharedPreferences sharedPreferences;

    private static final String url_check_messages_disp = "http://a0534961.xsph.ru/check_messages_disp.php";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        //открытие файла настроек
        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        btnMap = findViewById(R.id.btnMap);
        btnMes = findViewById(R.id.btnMessages);
        btnWork = findViewById(R.id.btnWorks);
        btnRep = findViewById(R.id.btnReport);

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, MapActivity.class));
            }
        });

        btnMes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, MessagesActivity.class));
            }
        });

        btnWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, WorksActivity.class));

            }
        });

        btnRep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, ReportsActivity.class));

            }
        });

        //проверка новых сообщений
        Timer myTimer = new Timer(); // Создаем таймер
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
                new CheckMessages().execute();
            };
        }, 0L, 10L * 1000); // интервал - 10000 миллисекунд, 0 миллисекунд до первого запуска.

    }

    //здесь будет проверка наличия новых сообщений
    class CheckMessages extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        protected String doInBackground(String... args){
            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody =
                        new FormBody.Builder()
                                .add(TAG_DISP_ID, sharedPreferences.getString("id", "0"))
                                .build();
                Request request =
                        new Request.Builder()
                                .url(url_check_messages_disp)
                                .post(formBody)
                                .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                JSONObject json = new JSONObject(response.body().string());

                int success = json.getInt(TAG_SUCCESS);
                JSONArray messages = json.getJSONArray("messages");

                if(success >= 1){
                    for (int i = 0; i < success; i++)
                    {
                        JSONObject message = messages.getJSONObject(i);

                        String text = message.getString("text");
                        String driv_name = message.getString("driv_name").split(" ")[0]
                                + " " + message.getString("driv_name").split(" ")[1];
                        int id_msg = message.getInt("id_msg");
                        String driv_id = message.getString("driv_id");
                        String s_driv_id = driv_name + " [" + message.getString("driv_id");


                        //работа в основном потоке
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                Intent notificationIntent = new Intent(MenuActivity.this, DriverDialogActivity.class);
                                notificationIntent.putExtra("driver", s_driv_id);
                                PendingIntent contentIntent = PendingIntent.getActivity(MenuActivity.this,
                                        0, notificationIntent,
                                        PendingIntent.FLAG_CANCEL_CURRENT);

                                NotificationManager notificationManager = (NotificationManager) MenuActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
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
                                
                                NotificationCompat.Builder builder =
                                        new NotificationCompat.Builder(MenuActivity.this, CHANNEL_ID)
                                                .setContentTitle("Сообщение от " + driv_name)
                                                .setContentText(text)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                .setContentIntent(contentIntent)
                                                .setDefaults(Notification.DEFAULT_ALL)
                                                .setSmallIcon(R.drawable.msg_icon)
                                                .setAutoCancel(true);

                                //notificationManager = NotificationManagerCompat.from(MenuActivity.this);
                                //notificationManager.cancel(driv_id);
                                notificationManager.notify(id_msg, builder.build());
                            }
                        });
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url){
        }
    }

}