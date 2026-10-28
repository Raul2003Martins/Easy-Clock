package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.BuildConfig;
import com.easyclock.app.R;
import com.easyclock.app.adapters.FuncionarioAdapter;
import com.easyclock.app.adapters.SolicitacaoCadastroAdapter;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.model.Funcionario;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ColaboradoresActivity extends AppCompatActivity {

    private MaterialToolbar toolbarColaboradores;
    private TextInputEditText etSearchColaborador;
    private TabLayout tabLayoutColaboradores;
    private RecyclerView rvColaboradores;
    private ProgressBar pbLoadingColaboradores;
    private TextView tvEmptyStateColaboradores;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;

    private FuncionarioAdapter funcionarioAdapter;
    private SolicitacaoCadastroAdapter solicitacaoAdapter;

    private String empresaIdRh;
    private int abaAtual = 0; // 0: Funcionários, 1: Solicitações

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colaboradores);

        if (BuildConfig.DEBUG) {
            Log.d("ColaboradoresActivity", "onCreate: Inicializando ColaboradoresActivity...");
        }

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();

        inicializarComponentes();
        configurarAdapters();
        configurarListeners();
        carregarDadosRh();
    }

    private void inicializarComponentes() {
        toolbarColaboradores = findViewById(R.id.toolbarColaboradores);
        etSearchColaborador = findViewById(R.id.etSearchColaborador);
        tabLayoutColaboradores = findViewById(R.id.tabLayoutColaboradores);
        rvColaboradores = findViewById(R.id.rvColaboradores);
        pbLoadingColaboradores = findViewById(R.id.pbLoadingColaboradores);
        tvEmptyStateColaboradores = findViewById(R.id.tvEmptyStateColaboradores);

        if (rvColaboradores != null) {
            rvColaboradores.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void configurarAdapters() {
        funcionarioAdapter = new FuncionarioAdapter(funcionario -> {
            Intent intent = new Intent(ColaboradoresActivity.this, EspelhoPontoFuncionarioActivity.class);
            intent.putExtra("funcionarioUid", funcionario.getUid());
            intent.putExtra("funcionarioNome", funcionario.getNome());
            startActivity(intent);
        });

        solicitacaoAdapter = new SolicitacaoCadastroAdapter(new SolicitacaoCadastroAdapter.OnSolicitacaoAcaoListener() {
            @Override
            public void onAprovar(Funcionario funcionario) {
                confirmarAprovacao(funcionario);
            }

            @Override
            public void onRecusar(Funcionario funcionario) {
                confirmarRecusa(funcionario);
            }
        });

        if (rvColaboradores != null) {
            rvColaboradores.setAdapter(funcionarioAdapter);
        }
    }

    private void configurarListeners() {
        if (toolbarColaboradores != null) {
            toolbarColaboradores.setNavigationOnClickListener(v -> finish());
        }

        if (tabLayoutColaboradores != null) {
            tabLayoutColaboradores.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    abaAtual = tab.getPosition();
                    if (abaAtual == 0) {
                        if (rvColaboradores != null) rvColaboradores.setAdapter(funcionarioAdapter);
                        carregarFuncionariosAtivos();
                    } else {
                        if (rvColaboradores != null) rvColaboradores.setAdapter(solicitacaoAdapter);
                        carregarSolicitacoesPendentes();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        if (etSearchColaborador != null) {
            etSearchColaborador.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String termo = s != null ? s.toString() : "";
                    if (abaAtual == 0) {
                        funcionarioAdapter.filtrar(termo);
                    } else {
                        solicitacaoAdapter.filtrar(termo);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void carregarDadosRh() {
        String uid = autenticacaoService.getUidAtual();
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        exibirLoading(true);

        funcionarioDAO.buscarPorUid(uid)
                .addOnSuccessListener(rh -> {
                    if (rh == null || !"RH".equalsIgnoreCase(rh.getPerfil()) || !"ATIVO".equalsIgnoreCase(rh.getStatus())) {
                        exibirLoading(false);
                        if (BuildConfig.DEBUG) {
                            Log.d("ColaboradoresActivity", "Acesso restrito: usuário não é RH ativo.");
                        }
                        Toast.makeText(this, "Acesso restrito ao perfil de RH ativo.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    empresaIdRh = rh.getEmpresaId();
                    if (BuildConfig.DEBUG) {
                        Log.d("ColaboradoresActivity", "Perfil RH verificado com sucesso. EmpresaId=" + empresaIdRh);
                    }
                    carregarFuncionariosAtivos();
                })
                .addOnFailureListener(e -> {
                    exibirLoading(false);
                    if (BuildConfig.DEBUG) {
                        Log.e("ColaboradoresActivity", "Erro ao carregar dados do usuário RH: " + e.getMessage());
                    }
                    Toast.makeText(this, "Erro ao carregar dados do usuário RH: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void carregarFuncionariosAtivos() {
        if (empresaIdRh == null) return;

        exibirLoading(true);

        funcionarioDAO.listarPorEmpresa(empresaIdRh)
                .addOnSuccessListener(lista -> {
                    exibirLoading(false);

                    List<Funcionario> ativos = new ArrayList<>();
                    if (lista != null) {
                        for (Funcionario f : lista) {
                            if ("ATIVO".equalsIgnoreCase(f.getStatus())) {
                                ativos.add(f);
                            }
                        }
                    }

                    funcionarioAdapter.setLista(ativos);
                    if (etSearchColaborador != null && etSearchColaborador.getText() != null) {
                        funcionarioAdapter.filtrar(etSearchColaborador.getText().toString());
                    }

                    atualizarEmptyState(ativos.isEmpty());
                })
                .addOnFailureListener(e -> {
                    exibirLoading(false);
                    Toast.makeText(this, "Erro ao carregar funcionários: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarSolicitacoesPendentes() {
        if (empresaIdRh == null) return;

        exibirLoading(true);

        funcionarioDAO.listarPendentes(empresaIdRh)
                .addOnSuccessListener(lista -> {
                    exibirLoading(false);

                    List<Funcionario> pendentes = lista != null ? lista : new ArrayList<>();
                    solicitacaoAdapter.setLista(pendentes);

                    if (etSearchColaborador != null && etSearchColaborador.getText() != null) {
                        solicitacaoAdapter.filtrar(etSearchColaborador.getText().toString());
                    }

                    atualizarEmptyState(pendentes.isEmpty());
                })
                .addOnFailureListener(e -> {
                    exibirLoading(false);
                    Toast.makeText(this, "Erro ao carregar solicitações: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmarAprovacao(Funcionario funcionario) {
        if (funcionario == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Aprovação")
                .setMessage("Deseja aprovar o cadastro de " + (funcionario.getNome() != null ? funcionario.getNome() : "este funcionário") + "?")
                .setPositiveButton("APROVAR", (dialog, which) -> executarAprovacao(funcionario))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void executarAprovacao(Funcionario funcionario) {
        exibirLoading(true);

        autenticacaoService.aprovarFuncionario(funcionario.getUid())
                .addOnSuccessListener(result -> {
                    exibirLoading(false);
                    Toast.makeText(this, "Funcionário aprovado com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarSolicitacoesPendentes();
                })
                .addOnFailureListener(e -> {
                    exibirLoading(false);
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro ao aprovar funcionário.";
                    new AlertDialog.Builder(this)
                            .setTitle("Falha na Aprovação")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private void confirmarRecusa(Funcionario funcionario) {
        if (funcionario == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Recusa")
                .setMessage("Deseja recusar o cadastro de " + (funcionario.getNome() != null ? funcionario.getNome() : "este funcionário") + "?")
                .setPositiveButton("RECUSAR", (dialog, which) -> executarRecusa(funcionario))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void executarRecusa(Funcionario funcionario) {
        exibirLoading(true);

        autenticacaoService.recusarFuncionario(funcionario.getUid())
                .addOnSuccessListener(result -> {
                    exibirLoading(false);
                    Toast.makeText(this, "Cadastro recusado com sucesso.", Toast.LENGTH_SHORT).show();
                    carregarSolicitacoesPendentes();
                })
                .addOnFailureListener(e -> {
                    exibirLoading(false);
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro ao recusar funcionário.";
                    new AlertDialog.Builder(this)
                            .setTitle("Falha na Recusa")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private void exibirLoading(boolean carregando) {
        if (pbLoadingColaboradores != null) {
            pbLoadingColaboradores.setVisibility(carregando ? View.VISIBLE : View.GONE);
        }
        if (rvColaboradores != null) {
            rvColaboradores.setVisibility(carregando ? View.GONE : View.VISIBLE);
        }
        if (carregando && tvEmptyStateColaboradores != null) {
            tvEmptyStateColaboradores.setVisibility(View.GONE);
        }
    }

    private void atualizarEmptyState(boolean vazia) {
        if (tvEmptyStateColaboradores != null) {
            tvEmptyStateColaboradores.setVisibility(vazia ? View.VISIBLE : View.GONE);
        }
    }
}
