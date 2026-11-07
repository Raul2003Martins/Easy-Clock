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
import com.easyclock.app.adapters.VincularJornadaAdapter;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.dao.JornadaTrabalhoDAO;
import com.easyclock.app.model.Funcionario;
import com.easyclock.app.model.JornadaTrabalho;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class VincularJornadaActivity extends AppCompatActivity implements VincularJornadaAdapter.OnSalvarVinculoListener {

    private ProgressBar progressBarVincular;
    private TextView tvSemFuncionarios;
    private RecyclerView rvVincularFuncionarios;
    private VincularJornadaAdapter adapter;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private JornadaTrabalhoDAO jornadaDAO;

    private String empresaId;
    private List<JornadaTrabalho> jornadasAtivasList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_jornada);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        jornadaDAO = new JornadaTrabalhoDAO();
        jornadasAtivasList = new ArrayList<>();

        empresaId = getIntent().getStringExtra("empresaId");

        inicializarComponentes();
        verificarECarregar();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarVincularJornada);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        progressBarVincular = findViewById(R.id.progressBarVincular);
        tvSemFuncionarios = findViewById(R.id.tvSemFuncionarios);
        rvVincularFuncionarios = findViewById(R.id.rvVincularFuncionarios);

        rvVincularFuncionarios.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VincularJornadaAdapter(this);
        rvVincularFuncionarios.setAdapter(adapter);
    }

    private void verificarECarregar() {
        String uid = autenticacaoService.getUidAtual();
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarVincular != null) progressBarVincular.setVisibility(View.VISIBLE);

        funcionarioDAO.buscarPorUid(uid)
                .addOnSuccessListener(func -> {
                    if (func == null || !"RH".equalsIgnoreCase(func.getPerfil()) || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Apenas RH ativo pode vincular jornadas.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (empresaId == null) {
                        empresaId = func.getEmpresaId();
                    }

                    carregarJornadasEFuncionarios();
                })
                .addOnFailureListener(e -> {
                    if (progressBarVincular != null) progressBarVincular.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao validar permissões.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void carregarJornadasEFuncionarios() {
        if (empresaId == null) return;

        if (progressBarVincular != null) progressBarVincular.setVisibility(View.VISIBLE);

        jornadaDAO.listarPorEmpresa(empresaId)
                .addOnSuccessListener(todasJornadas -> {
                    jornadasAtivasList.clear();
                    if (todasJornadas != null) {
                        for (JornadaTrabalho j : todasJornadas) {
                            if (j.isAtivo()) {
                                jornadasAtivasList.add(j);
                            }
                        }
                    }

                    funcionarioDAO.listarPorEmpresa(empresaId)
                            .addOnSuccessListener(todosFuncs -> {
                                if (progressBarVincular != null) progressBarVincular.setVisibility(View.GONE);

                                List<Funcionario> funcsAtivos = new ArrayList<>();
                                if (todosFuncs != null) {
                                    for (Funcionario f : todosFuncs) {
                                        if ("ATIVO".equalsIgnoreCase(f.getStatus())) {
                                            funcsAtivos.add(f);
                                        }
                                    }
                                }

                                if (funcsAtivos.isEmpty()) {
                                    if (tvSemFuncionarios != null) tvSemFuncionarios.setVisibility(View.VISIBLE);
                                    if (rvVincularFuncionarios != null) rvVincularFuncionarios.setVisibility(View.GONE);
                                } else {
                                    if (tvSemFuncionarios != null) tvSemFuncionarios.setVisibility(View.GONE);
                                    if (rvVincularFuncionarios != null) rvVincularFuncionarios.setVisibility(View.VISIBLE);
                                    adapter.setData(funcsAtivos, jornadasAtivasList);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (progressBarVincular != null) progressBarVincular.setVisibility(View.GONE);
                                Toast.makeText(this, "Erro ao carregar funcionários: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressBarVincular != null) progressBarVincular.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar jornadas: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onSalvarVinculo(Funcionario funcionario, JornadaTrabalho jornadaSelecionada) {
        if (funcionario == null || funcionario.getUid() == null) return;

        String jornadaIdSel = jornadaSelecionada != null ? jornadaSelecionada.getId() : "";

        if (progressBarVincular != null) progressBarVincular.setVisibility(View.VISIBLE);

        funcionarioDAO.atualizarCampos(funcionario.getUid(), java.util.Collections.singletonMap("jornadaId", jornadaIdSel))
                .addOnSuccessListener(unused -> {
                    if (progressBarVincular != null) progressBarVincular.setVisibility(View.GONE);
                    String nomeJornada = jornadaSelecionada != null ? jornadaSelecionada.getNome() : "Nenhuma";
                    Toast.makeText(this, "Jornada '" + nomeJornada + "' atribuída a " + funcionario.getNome(), Toast.LENGTH_SHORT).show();
                    carregarJornadasEFuncionarios();
                })
                .addOnFailureListener(e -> {
                    if (progressBarVincular != null) progressBarVincular.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao atualizar jornada do funcionário: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
