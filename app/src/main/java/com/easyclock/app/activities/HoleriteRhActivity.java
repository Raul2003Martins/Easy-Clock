package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class HoleriteRhActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holerite_rh);

        MaterialToolbar toolbarHoleriteRh = findViewById(R.id.toolbarHoleriteRh);
        if (toolbarHoleriteRh != null) {
            toolbarHoleriteRh.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnSelecionarArquivo = findViewById(R.id.btnSelecionarArquivo);
        if (btnSelecionarArquivo != null) {
            btnSelecionarArquivo.setOnClickListener(v ->
                    Toast.makeText(this, "Seleção de arquivo PDF em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnAnexarEConfirmarHolerite = findViewById(R.id.btnAnexarEConfirmarHolerite);
        if (btnAnexarEConfirmarHolerite != null) {
            btnAnexarEConfirmarHolerite.setOnClickListener(v ->
                    Toast.makeText(this, "Geração de Holerite em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
