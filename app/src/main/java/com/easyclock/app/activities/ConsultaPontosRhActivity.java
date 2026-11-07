package com.easyclock.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.adapters.HistoricoPontoAdapter;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.dao.PontoDAO;
import com.easyclock.app.model.Funcionario;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ConsultaPontosRhActivity extends AppCompatActivity {

    private Spinner spinnerFuncionarioConsulta;
    private ProgressBar progressBarConsultaRh;
    private TextView tvSemPontosConsultaRh;
    private RecyclerView rvPontosConsultaRh;
    private HistoricoPontoAdapter adapter;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private PontoDAO pontoDAO;

    private String rhEmpresaId;
    private List<Funcionario> funcionariosAtivosList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consulta_pontos_rh);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        pontoDAO = new PontoDAO();
        funcionariosAtivosList = new ArrayList<>();

        inicializarComponentes();
        verificarECarregar();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarConsultaPontosRh);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        spinnerFuncionarioConsulta = findViewById(R.id.spinnerFuncionarioConsulta);
        progressBarConsultaRh = findViewById(R.id.progressBarConsultaRh);
        tvSemPontosConsultaRh = findViewById(R.id.tvSemPontosConsultaRh);
        rvPontosConsultaRh = findViewById(R.id.rvPontosConsultaRh);

        rvPontosConsultaRh.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoricoPontoAdapter();
        rvPontosConsultaRh.setAdapter(adapter);
    }

    private void verificarECarregar() {
        String uid = autenticacaoService.getUidAtual();
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.VISIBLE);

        funcionarioDAO.buscarPorUid(uid)
                .addOnSuccessListener(func -> {
                    if (func == null || !"RH".equalsIgnoreCase(func.getPerfil()) || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Apenas RH ativo pode consultar pontos.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    rhEmpresaId = func.getEmpresaId();
                    carregarFuncionarios();
                })
                .addOnFailureListener(e -> {
                    if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao validar permissões.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void carregarFuncionarios() {
        if (rhEmpresaId == null) return;

        if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.VISIBLE);

        funcionarioDAO.listarPorEmpresa(rhEmpresaId)
                .addOnSuccessListener(todosFuncs -> {
                    if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.GONE);

                    funcionariosAtivosList.clear();
                    List<String> nomesSpinner = new ArrayList<>();

                    if (todosFuncs != null) {
                        for (Funcionario f : todosFuncs) {
                            if ("ATIVO".equalsIgnoreCase(f.getStatus())) {
                                funcionariosAtivosList.add(f);
                                String desc = f.getNome() + " (" + (f.getEmail() != null ? f.getEmail() : "--") + ")";
                                nomesSpinner.add(desc);
                            }
                        }
                    }

                    if (funcionariosAtivosList.isEmpty()) {
                        if (tvSemPontosConsultaRh != null) tvSemPontosConsultaRh.setVisibility(View.VISIBLE);
                        if (rvPontosConsultaRh != null) rvPontosConsultaRh.setVisibility(View.GONE);
                        Toast.makeText(this, "Nenhum funcionário ativo cadastrado na empresa.", Toast.LENGTH_SHORT).show();
                    } else {
                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                nomesSpinner
                        );
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerFuncionarioConsulta.setAdapter(spinnerAdapter);

                        spinnerFuncionarioConsulta.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position >= 0 && position < funcionariosAtivosList.size()) {
                                    Funcionario sel = funcionariosAtivosList.get(position);
                                    carregarPontosDoFuncionario(sel.getUid());
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar funcionários: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarPontosDoFuncionario(String funcionarioUid) {
        if (funcionarioUid == null || rhEmpresaId == null) return;

        if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.VISIBLE);

        pontoDAO.listarPontosPorEmpresaEFuncionario(rhEmpresaId, funcionarioUid)
                .addOnSuccessListener(listaPontos -> {
                    if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.GONE);

                    if (listaPontos == null || listaPontos.isEmpty()) {
                        if (tvSemPontosConsultaRh != null) tvSemPontosConsultaRh.setVisibility(View.VISIBLE);
                        if (rvPontosConsultaRh != null) rvPontosConsultaRh.setVisibility(View.GONE);
                    } else {
                        if (tvSemPontosConsultaRh != null) tvSemPontosConsultaRh.setVisibility(View.GONE);
                        if (rvPontosConsultaRh != null) rvPontosConsultaRh.setVisibility(View.VISIBLE);
                        adapter.setPontos(listaPontos);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarConsultaRh != null) progressBarConsultaRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar pontos do funcionário: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
