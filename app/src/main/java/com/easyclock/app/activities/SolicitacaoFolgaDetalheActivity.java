package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class SolicitacaoFolgaDetalheActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitacao_folga_detalhe);

        MaterialToolbar toolbarSolicitacaoFolga = findViewById(R.id.toolbarSolicitacaoFolga);
        if (toolbarSolicitacaoFolga != null) {
            toolbarSolicitacaoFolga.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnConcederFolga = findViewById(R.id.btnConcederFolga);
        if (btnConcederFolga != null) {
            btnConcederFolga.setOnClickListener(v ->
                    Toast.makeText(this, "Concessão de Folga/Férias em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnNaoConcederFolga = findViewById(R.id.btnNaoConcederFolga);
        if (btnNaoConcederFolga != null) {
            btnNaoConcederFolga.setOnClickListener(v ->
                    Toast.makeText(this, "Recusa de Folga/Férias em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
