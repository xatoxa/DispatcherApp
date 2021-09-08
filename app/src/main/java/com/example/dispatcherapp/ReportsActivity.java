package com.example.dispatcherapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class ReportsActivity extends AppCompatActivity {

    ListView listReports;
    ArrayList<HashMap<String, String>> arrayReports;
    HashMap<String, String> hashMap;
    SimpleAdapter adapter;
    SharedPreferences sharedPreferences;
    Button btnSearch;
    Spinner spinner;
    String[] head, subhead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        //кнопка "назад
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        btnSearch = findViewById(R.id.btnSearchRep);
        spinner = findViewById(R.id.spinnerType);

        head = getResources().getStringArray(R.array.head);
        subhead = getResources().getStringArray(R.array.subhead);


        arrayReports = new ArrayList<>();
        adapter = new SimpleAdapter(ReportsActivity.this,
                arrayReports,
                R.layout.list_item_reports,
                new String[]{"head", "subhead"},
                new int[]{R.id.textview_item_head,
                        R.id.textview_item_subhead});
        listReports = findViewById(R.id.listViewRep);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrayReports.clear();
                if (spinner.getSelectedItem().toString().equals("Автобус")) {
                    for (int i = 0; i < 3; i++) {
                        hashMap = new HashMap<>();

                        hashMap.put("head", head[i]);
                        hashMap.put("subhead", subhead[i]);
                        arrayReports.add(hashMap);
                    }
                }
                listReports.setAdapter(adapter);
            }
        });
    }
}