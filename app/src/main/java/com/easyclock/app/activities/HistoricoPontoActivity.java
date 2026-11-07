package com.easyclock.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.adapters.HistoricoPontoAdapter;
import com.easyclock.app.dao.PontoDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;

public class HistoricoPontoActivity extends AppCompatActivity {

    private ProgressBar progressBarHistorico;
    private TextView tvSemRegistrosPonto;
    private RecyclerView rvHistoricoPonto;
    private HistoricoPontoAdapter adapter;

    private AutenticacaoService autenticacaoService;
    private PontoDAO pontoDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico_ponto);

        autenticacaoService = new AutenticacaoService();
        pontoDAO = new PontoDAO();

        inicializarComponentes();
        carregarHistorico();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarHistoricoPonto);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        progressBarHistorico = findViewById(R.id.progressBarHistorico);
        tvSemRegistrosPonto = findViewById(R.id.tvSemRegistrosPonto);
        rvHistoricoPonto = findViewById(R.id.rvHistoricoPonto);

        rvHistoricoPonto.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoricoPontoAdapter();
        rvHistoricoPonto.setAdapter(adapter);
    }

    private void carregarHistorico() {
        String uid = autenticacaoService.getUidAtual();
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarHistorico != null) progressBarHistorico.setVisibility(View.VISIBLE);

        pontoDAO.listarPontosPorFuncionario(uid)
                .addOnSuccessListener(lista -> {
                    if (progressBarHistorico != null) progressBarHistorico.setVisibility(View.GONE);

                    if (lista == null || lista.isEmpty()) {
                        if (tvSemRegistrosPonto != null) tvSemRegistrosPonto.setVisibility(View.VISIBLE);
                        if (rvHistoricoPonto != null) rvHistoricoPonto.setVisibility(View.GONE);
                    } else {
                        if (tvSemRegistrosPonto != null) tvSemRegistrosPonto.setVisibility(View.GONE);
                        if (rvHistoricoPonto != null) rvHistoricoPonto.setVisibility(View.VISIBLE);
                        adapter.setPontos(lista);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarHistorico != null) progressBarHistorico.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar histórico: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
