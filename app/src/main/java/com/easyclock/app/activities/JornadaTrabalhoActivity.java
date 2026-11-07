package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.adapters.JornadaAdapter;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.dao.JornadaTrabalhoDAO;
import com.easyclock.app.model.JornadaTrabalho;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class JornadaTrabalhoActivity extends AppCompatActivity implements JornadaAdapter.OnJornadaAcaoListener {

    private ProgressBar progressBarJornada;
    private TextView tvSemJornadas;
    private RecyclerView rvJornadas;
    private JornadaAdapter adapter;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private JornadaTrabalhoDAO jornadaDAO;

    private String rhEmpresaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jornada_trabalho);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        jornadaDAO = new JornadaTrabalhoDAO();

        inicializarComponentes();
        verificarECarregarDados();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarJornada);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        progressBarJornada = findViewById(R.id.progressBarJornada);
        tvSemJornadas = findViewById(R.id.tvSemJornadas);
        rvJornadas = findViewById(R.id.rvJornadas);

        rvJornadas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JornadaAdapter(this);
        rvJornadas.setAdapter(adapter);

        MaterialButton btnNovaJornada = findViewById(R.id.btnNovaJornada);
        if (btnNovaJornada != null) {
            btnNovaJornada.setOnClickListener(v -> {
                Intent intent = new Intent(JornadaTrabalhoActivity.this, CadastroJornadaActivity.class);
                intent.putExtra("empresaId", rhEmpresaId);
                startActivity(intent);
            });
        }

        MaterialButton btnVincularFuncionarios = findViewById(R.id.btnVincularFuncionarios);
        if (btnVincularFuncionarios != null) {
            btnVincularFuncionarios.setOnClickListener(v -> {
                Intent intent = new Intent(JornadaTrabalhoActivity.this, VincularJornadaActivity.class);
                intent.putExtra("empresaId", rhEmpresaId);
                startActivity(intent);
            });
        }
    }

    private void verificarECarregarDados() {
        String uid = autenticacaoService.getUidAtual();
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarJornada != null) progressBarJornada.setVisibility(View.VISIBLE);

        funcionarioDAO.buscarPorUid(uid)
                .addOnSuccessListener(func -> {
                    if (func == null || !"RH".equalsIgnoreCase(func.getPerfil()) || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Funcionalidade exclusiva para RH ativo.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    rhEmpresaId = func.getEmpresaId();
                    carregarJornadas();
                })
                .addOnFailureListener(e -> {
                    if (progressBarJornada != null) progressBarJornada.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao validar permissões: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void carregarJornadas() {
        if (rhEmpresaId == null) return;

        if (progressBarJornada != null) progressBarJornada.setVisibility(View.VISIBLE);

        jornadaDAO.listarPorEmpresa(rhEmpresaId)
                .addOnSuccessListener(lista -> {
                    if (progressBarJornada != null) progressBarJornada.setVisibility(View.GONE);

                    if (lista == null || lista.isEmpty()) {
                        if (tvSemJornadas != null) tvSemJornadas.setVisibility(View.VISIBLE);
                        if (rvJornadas != null) rvJornadas.setVisibility(View.GONE);
                    } else {
                        if (tvSemJornadas != null) tvSemJornadas.setVisibility(View.GONE);
                        if (rvJornadas != null) rvJornadas.setVisibility(View.VISIBLE);
                        adapter.setJornadas(lista);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarJornada != null) progressBarJornada.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar jornadas: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rhEmpresaId != null) {
            carregarJornadas();
        }
    }

    @Override
    public void onEditar(JornadaTrabalho jornada) {
        if (jornada == null) return;

        Intent intent = new Intent(JornadaTrabalhoActivity.this, CadastroJornadaActivity.class);
        intent.putExtra("empresaId", rhEmpresaId);
        intent.putExtra("jornadaId", jornada.getId());
        startActivity(intent);
    }

    @Override
    public void onAlternarStatus(JornadaTrabalho jornada) {
        if (jornada == null || jornada.getId() == null) return;

        boolean novoStatus = !jornada.isAtivo();
        if (progressBarJornada != null) progressBarJornada.setVisibility(View.VISIBLE);

        jornadaDAO.alternarStatusJornada(jornada.getId(), novoStatus)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, novoStatus ? "Jornada ativada!" : "Jornada desativada!", Toast.LENGTH_SHORT).show();
                    carregarJornadas();
                })
                .addOnFailureListener(e -> {
                    if (progressBarJornada != null) progressBarJornada.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao alterar status: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
