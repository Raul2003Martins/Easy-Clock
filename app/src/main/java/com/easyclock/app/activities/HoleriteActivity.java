package com.easyclock.app.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;

public class HoleriteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holerite);

        MaterialToolbar toolbarHolerite = findViewById(R.id.toolbarHolerite);
        if (toolbarHolerite != null) {
            toolbarHolerite.setNavigationOnClickListener(v -> finish());
        }
    }
}
