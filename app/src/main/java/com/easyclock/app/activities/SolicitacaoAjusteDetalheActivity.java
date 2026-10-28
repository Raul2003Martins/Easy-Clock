package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class SolicitacaoAjusteDetalheActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitacao_ajuste_detalhe);

        MaterialToolbar toolbarSolicitacaoAjuste = findViewById(R.id.toolbarSolicitacaoAjuste);
        if (toolbarSolicitacaoAjuste != null) {
            toolbarSolicitacaoAjuste.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnConfirmarAjuste = findViewById(R.id.btnConfirmarAjuste);
        if (btnConfirmarAjuste != null) {
            btnConfirmarAjuste.setOnClickListener(v ->
                    Toast.makeText(this, "Ajuste de Ponto em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnNaoConcederAjuste = findViewById(R.id.btnNaoConcederAjuste);
        if (btnNaoConcederAjuste != null) {
            btnNaoConcederAjuste.setOnClickListener(v ->
                    Toast.makeText(this, "Recusa de Ajuste em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
