package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class FaceIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id);

        MaterialToolbar toolbarFaceId = findViewById(R.id.toolbarFaceId);
        if (toolbarFaceId != null) {
            toolbarFaceId.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnBaterPontoFaceId = findViewById(R.id.btnBaterPontoFaceId);
        if (btnBaterPontoFaceId != null) {
            btnBaterPontoFaceId.setOnClickListener(v ->
                    Toast.makeText(this, "Reconhecimento Facial (FaceID) em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
