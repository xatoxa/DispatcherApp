package com.example.dispatcherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.yandex.mapkit.MapKitFactory;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    Button btnSignIn;
    MaterialEditText eTxtId, eTxtPwd;
    SharedPreferences sharedPreferences;

    //url to login
    private static final String url_login = "http://a0534961.xsph.ru/dispatcher_login.php";

    //JSON node names
    private static final String TAG_USER_DATA = "user_data";
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_NAME = "disp_name";
    private static final String TAG_PWD = "disp_pwd";
    private static final String TAG_ID = "disp_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapKitFactory.setApiKey("141fb363-3421-4103-a656-ee2152eca6e3");
        MapKitFactory.initialize(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        eTxtId = findViewById(R.id.eTextID);
        eTxtPwd = findViewById(R.id.eTextPwd);
        btnSignIn = findViewById(R.id.btnSignIn);
        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        //проверка разрешений
        checkPermissions();

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SignIn().execute();
            }
        });
    }

    //проверка разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 50 && grantResults[0] == RESULT_OK)
        {
            checkPermissions();
        }
    }

    //запрос разрешений
    private void checkPermissions()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                != PackageManager.PERMISSION_GRANTED )
        {
            requestPermissions(new String[]{Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Manifest.permission.INTERNET}, 50);
        }
    }

    class SignIn extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //Snackbar.make(findViewById(R.id.MainActivity), "Поиск в базе данных..", Snackbar.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... args){
            String disp_id = eTxtId.getText().toString();
            String disp_pwd = eTxtPwd.getText().toString();

            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody =
                        new FormBody.Builder()
                                .add(TAG_ID, disp_id)
                                .add(TAG_PWD, disp_pwd)
                                .build();
                Request request =
                        new Request.Builder()
                                .url(url_login)
                                .post(formBody)
                                .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                JSONObject json = new JSONObject(response.body().string());
                //Log.d("user data", json.toString());

                int success = json.getInt(TAG_SUCCESS);
                Log.d("success", Integer.toString(success));

                if(success == 1){
                    JSONObject user_data = json.getJSONObject(TAG_USER_DATA);
                    String disp_name = user_data.getString(TAG_NAME);
                    Intent i = new Intent(MainActivity.this, MenuActivity.class);

                    i.putExtra(TAG_ID, disp_id);
                    i.putExtra(TAG_NAME, disp_name);
                    i.putExtra(TAG_PWD, disp_pwd);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("id", disp_id);
                    editor.putString("name", disp_name);
                    editor.apply();

                    startActivity(i);
                    finish();
                }else
                {
                    //not found
                    Snackbar.make(findViewById(R.id.MainActivity), "Проверьте правильность ввода.", Snackbar.LENGTH_SHORT).show();

                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String file_url){
            //Snackbar.make(findViewById(R.id.MainActivity), "Готово!", Snackbar.LENGTH_SHORT).show();
        }
    }
}