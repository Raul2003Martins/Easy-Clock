package com.easyclock.app.activities;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.adapters.HistoricoBancoHorasAdapter;
import com.easyclock.app.dao.BancoHorasDAO;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.dao.HistoricoBancoHorasDAO;
import com.easyclock.app.model.Funcionario;
import com.easyclock.app.service.AutenticacaoService;
import com.easyclock.app.service.BancoHorasService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BancoHorasRhActivity extends AppCompatActivity {

    private ProgressBar progressBarBancoHorasRh;
    private AutoCompleteTextView actvSeletorFuncionarioRh;
    private TextView tvSaldoAtualRh;
    private MaterialButton btnAjustarSaldoRh;
    private RecyclerView rvHistoricoBancoRh;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private BancoHorasDAO bancoHorasDAO;
    private HistoricoBancoHorasDAO historicoBancoHorasDAO;

    private String rhUid;
    private String empresaIdRh;

    private List<Funcionario> listaFuncionarios = new ArrayList<>();
    private Funcionario funcionarioSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banco_horas_rh);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        bancoHorasDAO = new BancoHorasDAO();
        historicoBancoHorasDAO = new HistoricoBancoHorasDAO();

        inicializarComponentes();
        carregarEValidarRh();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarBancoHorasRh = findViewById(R.id.toolbarBancoHorasRh);
        if (toolbarBancoHorasRh != null) {
            toolbarBancoHorasRh.setNavigationOnClickListener(v -> finish());
        }

        progressBarBancoHorasRh = findViewById(R.id.progressBarBancoHorasRh);
        actvSeletorFuncionarioRh = findViewById(R.id.actvSeletorFuncionarioRh);
        tvSaldoAtualRh = findViewById(R.id.tvSaldoAtualRh);
        btnAjustarSaldoRh = findViewById(R.id.btnAjustarSaldoRh);
        rvHistoricoBancoRh = findViewById(R.id.rvHistoricoBancoRh);

        if (rvHistoricoBancoRh != null) {
            rvHistoricoBancoRh.setLayoutManager(new LinearLayoutManager(this));
        }

        if (btnAjustarSaldoRh != null) {
            btnAjustarSaldoRh.setOnClickListener(v -> exibirDialogoAjusteManual());
        }
    }

    private void carregarEValidarRh() {
        rhUid = autenticacaoService.getUidAtual();
        if (rhUid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.VISIBLE);

        funcionarioDAO.buscarPorUid(rhUid)
                .addOnSuccessListener(func -> {
                    if (func == null || !"RH".equalsIgnoreCase(func.getPerfil()) || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Apenas RH ativo pode gerenciar banco de horas.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    empresaIdRh = func.getEmpresaId();
                    carregarListaFuncionarios();
                })
                .addOnFailureListener(e -> {
                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao validar permissões.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void carregarListaFuncionarios() {
        funcionarioDAO.listarPorEmpresa(empresaIdRh)
                .addOnSuccessListener(lista -> {
                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);

                    listaFuncionarios.clear();
                    if (lista != null) {
                        for (Funcionario f : lista) {
                            if (f != null && "ATIVO".equalsIgnoreCase(f.getStatus())) {
                                listaFuncionarios.add(f);
                            }
                        }
                    }

                    if (listaFuncionarios.isEmpty()) {
                        Toast.makeText(this, "Nenhum funcionário ativo encontrado nesta empresa.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> nomes = new ArrayList<>();
                    for (Funcionario f : listaFuncionarios) {
                        nomes.add(f.getNome() != null ? f.getNome() : f.getUid());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nomes);
                    if (actvSeletorFuncionarioRh != null) {
                        actvSeletorFuncionarioRh.setAdapter(adapter);
                        actvSeletorFuncionarioRh.setOnItemClickListener((parent, view, position, id) -> {
                            funcionarioSelecionado = listaFuncionarios.get(position);
                            carregarDadosBancoDoFuncionario();
                        });

                        // Seleciona o primeiro por padrão
                        actvSeletorFuncionarioRh.setText(nomes.get(0), false);
                        funcionarioSelecionado = listaFuncionarios.get(0);
                        carregarDadosBancoDoFuncionario();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar lista de colaboradores.", Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarDadosBancoDoFuncionario() {
        if (funcionarioSelecionado == null) return;

        String fUid = funcionarioSelecionado.getUid();
        if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.VISIBLE);

        bancoHorasDAO.buscarPorFuncionario(fUid)
                .addOnSuccessListener(bh -> {
                    if (bh != null && tvSaldoAtualRh != null) {
                        long saldo = bh.getSaldoTotalMinutos();
                        tvSaldoAtualRh.setText(BancoHorasService.formatarMinutosParaHoraString(saldo));
                        if (saldo > 0) {
                            tvSaldoAtualRh.setTextColor(getResources().getColor(R.color.primary));
                        } else if (saldo < 0) {
                            tvSaldoAtualRh.setTextColor(getResources().getColor(R.color.button_danger));
                        } else {
                            tvSaldoAtualRh.setTextColor(getResources().getColor(R.color.text_secondary));
                        }
                    }

                    carregarHistoricoDoFuncionario(fUid);
                })
                .addOnFailureListener(e -> {
                    Log.e("BancoHorasRhActivity", "Erro ao consultar saldo para UID " + fUid, e);
                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao consultar saldo.", Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarHistoricoDoFuncionario(String fUid) {
        historicoBancoHorasDAO.listarPorEmpresaEFuncionario(empresaIdRh, fUid)
                .addOnSuccessListener(lista -> {
                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);

                    if (rvHistoricoBancoRh != null) {
                        HistoricoBancoHorasAdapter adapter = new HistoricoBancoHorasAdapter(lista);
                        rvHistoricoBancoRh.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BancoHorasRhActivity", "Erro ao carregar lançamentos para empresa " + empresaIdRh + " e UID " + fUid, e);
                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar lançamentos do colaborador.", Toast.LENGTH_SHORT).show();
                });
    }

    private void exibirDialogoAjusteManual() {
        if (funcionarioSelecionado == null) {
            Toast.makeText(this, "Selecione um colaborador.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etMinutosAjuste = new EditText(this);
        etMinutosAjuste.setHint("Minutos de ajuste (ex: +30 ou -45)");
        etMinutosAjuste.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(etMinutosAjuste);

        EditText etJustificativa = new EditText(this);
        etJustificativa.setHint("Justificativa obrigatória do RH");
        etJustificativa.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etJustificativa);

        new AlertDialog.Builder(this)
                .setTitle("Ajustar Saldo de " + funcionarioSelecionado.getNome())
                .setMessage("Informe a quantidade de minutos a adicionar (+) ou descontar (-) e o motivo:")
                .setView(layout)
                .setPositiveButton("CONFIRMAR AJUSTE", (dialog, which) -> {
                    String minStr = etMinutosAjuste.getText().toString().trim();
                    String motivo = etJustificativa.getText().toString().trim();

                    if (minStr.isEmpty()) {
                        Toast.makeText(this, "Informe a quantidade de minutos.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (motivo.isEmpty() || motivo.length() < 5) {
                        Toast.makeText(this, "Informe uma justificativa válida com pelo menos 5 caracteres.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long mins = 0;
                    try {
                        mins = Long.parseLong(minStr);
                    } catch (Exception e) {
                        Toast.makeText(this, "Minutos inválidos.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (mins == 0) {
                        Toast.makeText(this, "Informe um valor diferente de zero.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.VISIBLE);

                    historicoBancoHorasDAO.registrarAjusteManualRh(funcionarioSelecionado.getUid(), empresaIdRh, rhUid, mins, motivo)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Ajuste de banco de horas registrado com sucesso!", Toast.LENGTH_SHORT).show();
                                carregarDadosBancoDoFuncionario();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBarBancoHorasRh != null) progressBarBancoHorasRh.setVisibility(View.GONE);
                                Toast.makeText(this, "Falha ao registrar ajuste: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }
}
