package com.example.dispatcherapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WorksActivity extends AppCompatActivity {

    //JSON node names
    private static final String TAG_DISP_ID = "disp_id";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_WORKS = "works";
    private static final String TAG_TYPE = "type";


    //url to
    private static final String url_works = "http://a0534961.xsph.ru/load_works_disp.php";

    ListView listWorks;
    ArrayList<HashMap<String, String>> arrayWorks;
    HashMap<String, String> hashMap;
    SimpleAdapter adapter;
    SharedPreferences sharedPreferences;
    Button btnSearch;
    RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_works);

        //кнопка "назад
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        btnSearch = findViewById(R.id.btnSearch);
        radioGroup = findViewById(R.id.radioGroupSearch);

        arrayWorks = new ArrayList<>();
        adapter = new SimpleAdapter(WorksActivity.this,
                arrayWorks,
                R.layout.list_item_works,
                new String[]{"name", "driv", "bus"},
                new int[]{R.id.textview_item_name_trip,
                        R.id.textview_item_driv,
                        R.id.textview_item_bus});
        listWorks = findViewById(R.id.listWorks);

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedRadioId = radioGroup.getCheckedRadioButtonId();
                String type = "true";

                if (checkedRadioId == R.id.radioButtonOpen) type = "false";

                new LoadWorks().execute(type);

            }
        });

    }

    class LoadWorks extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            Snackbar.make(findViewById(R.id.WorksActivity),
                    "Поиск в базе данных..",
                    Snackbar.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... args){
            try {
                arrayWorks.clear();

                OkHttpClient client = new OkHttpClient();
                RequestBody formBody =
                        new FormBody.Builder()
                                .add(TAG_DISP_ID, sharedPreferences.getString("id", "0"))
                                .add(TAG_TYPE, args[0])
                                .build();
                Request request =
                        new Request.Builder()
                                .url(url_works)
                                .post(formBody)
                                .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                JSONObject json = new JSONObject(response.body().string());

                //Log.d("trips", json.toString());
                int success = json.getInt(TAG_SUCCESS);
                JSONArray works = json.getJSONArray(TAG_WORKS);

                if(success >= 1){
                    for (int i = 0; i < success; i++)
                    {
                        hashMap = new HashMap<>();
                        String name, driv, bus;

                        JSONObject work = works.getJSONObject(i);
                        driv = work.optString("driv_name");
                        bus = work.optString("b_model") + " "
                                    + work.optString("b_class") + " "
                                    + work.optString("b_reg_num");
                        name = work.optString("t_name");

                        hashMap.put("name", name);
                        hashMap.put("driv", driv);
                        hashMap.put("bus", bus);
                        arrayWorks.add(hashMap);
                    }
                }else
                {
                    //not found
                    Snackbar.make(findViewById(R.id.WorksActivity),
                            "Не найдено.",
                            Snackbar.LENGTH_SHORT).show();

                }

                //работа с listView в основном потоке
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        listWorks.setAdapter(adapter);
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url){
        }
    }

}