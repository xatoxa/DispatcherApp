package com.example.dispatcherapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessagesActivity extends AppCompatActivity {

    //JSON node names
    private static final String TAG_DISP_ID = "disp_id";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_DRIVERS = "drivers";

    //url to
    private static final String url_messages = "http://a0534961.xsph.ru/load_drivers_buses.php";

    ListView listView;
    ArrayAdapter<String> adapter;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        //кнопка "назад
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //открытие файла настроек
        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        listView = findViewById(R.id.listViewMes);

        //асинхронная загрузка имён водителей
        new LoadMessages().execute();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                TextView textView = (TextView) itemClicked;
                String strText = textView.getText().toString(); // получаем текст нажатого элемента
                String[] sName = strText.split("]");

                Intent i = new Intent(MessagesActivity.this, DriverDialogActivity.class);
                i.putExtra("driver", sName[0]);
                startActivity(i);
            }
        });

    }

    class LoadMessages extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            Snackbar.make(findViewById(R.id.MessagesActivity), "Поиск в базе данных..", Snackbar.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... args){
            List<List<Integer>> listTrips = new ArrayList<List<Integer>>();
            Stack<Integer> stackTrips = new Stack<>();

            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody =
                        new FormBody.Builder()
                                .add(TAG_DISP_ID, sharedPreferences.getString("id", "0"))
                                .build();
                Request request =
                        new Request.Builder()
                                .url(url_messages)
                                .post(formBody)
                                .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                JSONObject json = new JSONObject(response.body().string());

                //Log.d("trips", json.toString());
                int success = json.getInt(TAG_SUCCESS);
                JSONArray drivers = json.getJSONArray(TAG_DRIVERS);

                if(success >= 1){
                    ArrayList<String> lines = new ArrayList<>();

                    for (int i = 0; i < success; i++)
                    {
                        JSONObject driver = drivers.getJSONObject(i);
                        String driv_id = driver.getString("driv_id");
                        String driv_name = driver.getString("driv_name");
                        String[] s_driv_name = driv_name.split(" ");
                        String reg_num = driver.getString("bus_reg");
                        String line = s_driv_name[0] + " " + s_driv_name[1]
                                + " [" + driv_id + "] "
                                + reg_num;
                        lines.add(line);
                    }

                    //Log.d("lines", lines.toString());

                    adapter = new ArrayAdapter<String>(
                            MessagesActivity.this,
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
                    Snackbar.make(findViewById(R.id.MessagesActivity), "Не найдено.", Snackbar.LENGTH_SHORT).show();

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