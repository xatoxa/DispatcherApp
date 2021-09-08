package com.example.dispatcherapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController;

import com.google.android.material.snackbar.Snackbar;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.LinearRing;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polygon;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.AnimatedImageProvider;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.ui_view.ViewProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MapActivity extends AppCompatActivity {
    private final Point CAMERA_TARGET = new Point(51.663067, 39.200270);

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_DRIVERS = "drivers";
    private static final String TAG_DISP_ID = "disp_id";

    private static final String url_messages = "http://a0534961.xsph.ru/load_drivers_buses.php";

    SharedPreferences sharedPreferences;


    private MapView mapView;
    private MapObjectCollection mapObjects;
    Handler animationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        setContentView(R.layout.activity_map);
        mapView = (MapView)findViewById(R.id.mapView);
        mapView.getMap().move(
                new CameraPosition(CAMERA_TARGET, 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        mapObjects = mapView.getMap().getMapObjects().addCollection();
        animationHandler = new Handler();

        //загрузка местоположения автобусов
        Timer myTimer = new Timer(); // Создаем таймер
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
                new LoadBusesLocation().execute();
            };
        }, 0L, 30L * 1000); // интервал - 10000 миллисекунд, 0 миллисекунд до первого запуска.

    }

    //завершение отображения карты, когда юзер закрывает это активити
    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
    }

    //начало отображения карты, когда юзер видит это активити
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        MapKitFactory.getInstance().onStart();
    }

    // Strong reference to the listener.
    final private MapObjectTapListener circleMapObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject mapObject, Point point) {
            if (mapObject instanceof CircleMapObject) {
                CircleMapObject circle = (CircleMapObject)mapObject;

                float randomRadius = 100.0f + 50.0f * new Random().nextFloat();

                Circle curGeometry = circle.getGeometry();
                Circle newGeometry = new Circle(curGeometry.getCenter(), randomRadius);
                //circle.setGeometry(newGeometry);

                Object userData = circle.getUserData();
                if (userData instanceof CircleMapObjectUserData) {
                    CircleMapObjectUserData circleUserData = (CircleMapObjectUserData)userData;

                    LayoutInflater li = LayoutInflater.from(MapActivity.this);
                    View driverOnMapView = li.inflate(R.layout.dialog_driver_on_map, null);
                    //Создаем AlertDialog
                    AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(MapActivity.this);

                    //Настраиваем prompt.xml для нашего AlertDialog:
                    mDialogBuilder.setView(driverOnMapView);

                    //Настраиваем отображение поля для ввода текста в открытом диалоге:
                    final TextView txtDriv = (TextView) driverOnMapView.findViewById(R.id.txtDriverOnMap);
                    txtDriv.setText(circleUserData.description);

                    //Настраиваем сообщение в диалоговом окне:
                    mDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton("Написать сообщение",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int id) {
                                            Intent i = new Intent(MapActivity.this,
                                                    DriverDialogActivity.class);
                                            String driver = circleUserData.description.split(" ")[0]
                                                    + " " + circleUserData.description.split(" ")[1]
                                                    + " [" + Integer.toString(circleUserData.id);
                                            i.putExtra("driver", driver);
                                            startActivity(i);
                                        }
                                    })
                            .setNegativeButton("Отмена",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int id) {
                                            dialog.cancel();
                                        }
                                    });

                    //Создаем AlertDialog:
                    AlertDialog alertDialog = mDialogBuilder.create();

                    //и отображаем его:
                    alertDialog.show();
                }
            }
            return true;
        }
    };

    private class CircleMapObjectUserData {
        final int id;
        final String description;

        CircleMapObjectUserData(int id, String description) {
            this.id = id;
            this.description = description;
        }
    }

    private void createTappableCircle(int id, String description, Point bus_location) {
        CircleMapObject circle = mapObjects.addCircle(
                new Circle(bus_location, 25), Color.GREEN, 2, Color.RED);
        circle.setZIndex(100.0f);
        circle.setUserData(new CircleMapObjectUserData(id, description));

        // Client code must retain strong reference to the listener.
        circle.addTapListener(circleMapObjectTapListener);
    }

    class LoadBusesLocation extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            Snackbar.make(findViewById(R.id.MapActivity), "Поиск в базе данных..", Snackbar.LENGTH_SHORT).show();
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

                int success = json.getInt(TAG_SUCCESS);
                JSONArray drivers = json.getJSONArray(TAG_DRIVERS);

                if(success >= 1){
                    for (int i = 0; i < success; i++)
                    {
                        JSONObject driver = drivers.getJSONObject(i);
                        int driv_id = driver.getInt("driv_id");
                        String driv_name = driver.getString("driv_name");
                        String[] s_driv_name = driv_name.split(" ");
                        String reg_num = driver.getString("bus_reg");
                        String description = s_driv_name[0] + " " + s_driv_name[1] + " " + reg_num;
                        String[] s_loc = driver.getString("location").split(", ");
                        Point bus_location = new Point(Double.parseDouble(s_loc[0])
                                , Double.parseDouble(s_loc[1]));

                        //создание объектов в основном потоке
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                createTappableCircle(driv_id, description, bus_location);
                            }
                        });
                        //Snackbar.make(findViewById(R.id.MapActivity), "Загружено.", Snackbar.LENGTH_SHORT).show();
                    }
                }else
                {
                    //not found
                    Snackbar.make(findViewById(R.id.MapActivity), "Не найдено.", Snackbar.LENGTH_SHORT).show();
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