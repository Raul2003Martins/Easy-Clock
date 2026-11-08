package com.easyclock.app.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.adapters.HistoricoBancoHorasAdapter;
import com.easyclock.app.dao.BancoHorasDAO;
import com.easyclock.app.dao.HistoricoBancoHorasDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.easyclock.app.service.BancoHorasService;
import com.google.android.material.appbar.MaterialToolbar;

public class BancoHorasActivity extends AppCompatActivity {

    private static final String TAG = "BancoHorasActivity";

    private ProgressBar progressBarBancoHoras;
    private TextView tvSaldoAtualConsolidado;
    private TextView tvTotalPositivas;
    private TextView tvTotalNegativas;
    private RecyclerView rvHistoricoBancoHoras;

    private AutenticacaoService autenticacaoService;
    private BancoHorasDAO bancoHorasDAO;
    private HistoricoBancoHorasDAO historicoBancoHorasDAO;

    private String uidFuncionario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banco_horas);

        autenticacaoService = new AutenticacaoService();
        bancoHorasDAO = new BancoHorasDAO();
        historicoBancoHorasDAO = new HistoricoBancoHorasDAO();

        inicializarComponentes();
        carregarDados();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarBancoHoras = findViewById(R.id.toolbarBancoHoras);
        if (toolbarBancoHoras != null) {
            toolbarBancoHoras.setNavigationOnClickListener(v -> finish());
        }

        progressBarBancoHoras = findViewById(R.id.progressBarBancoHoras);
        tvSaldoAtualConsolidado = findViewById(R.id.tvSaldoAtualConsolidado);
        tvTotalPositivas = findViewById(R.id.tvTotalPositivas);
        tvTotalNegativas = findViewById(R.id.tvTotalNegativas);
        rvHistoricoBancoHoras = findViewById(R.id.rvHistoricoBancoHoras);

        if (rvHistoricoBancoHoras != null) {
            rvHistoricoBancoHoras.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void carregarDados() {
        uidFuncionario = autenticacaoService.getUidAtual();
        if (uidFuncionario == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarBancoHoras != null) progressBarBancoHoras.setVisibility(View.VISIBLE);

        bancoHorasDAO.buscarPorFuncionario(uidFuncionario)
                .addOnSuccessListener(bh -> {
                    if (bh != null) {
                        long saldo = bh.getSaldoTotalMinutos();
                        long pos = bh.getTotalPositivasMinutos();
                        long neg = bh.getTotalNegativasMinutos();

                        if (tvSaldoAtualConsolidado != null) {
                            tvSaldoAtualConsolidado.setText(BancoHorasService.formatarMinutosParaHoraString(saldo));
                            if (saldo > 0) {
                                tvSaldoAtualConsolidado.setTextColor(getResources().getColor(R.color.primary));
                            } else if (saldo < 0) {
                                tvSaldoAtualConsolidado.setTextColor(getResources().getColor(R.color.button_danger));
                            } else {
                                tvSaldoAtualConsolidado.setTextColor(getResources().getColor(R.color.text_secondary));
                            }
                        }

                        if (tvTotalPositivas != null) {
                            tvTotalPositivas.setText("+" + BancoHorasService.formatarMinutosParaHoraString(pos).replace("+", "").replace("-", ""));
                        }

                        if (tvTotalNegativas != null) {
                            tvTotalNegativas.setText("-" + BancoHorasService.formatarMinutosParaHoraString(neg).replace("+", "").replace("-", ""));
                        }
                    }

                    carregarHistorico();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao consultar saldo para UID " + uidFuncionario, e);
                    if (progressBarBancoHoras != null) progressBarBancoHoras.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao consultar saldo.", Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarHistorico() {
        historicoBancoHorasDAO.listarPorFuncionario(uidFuncionario)
                .addOnSuccessListener(lista -> {
                    if (progressBarBancoHoras != null) progressBarBancoHoras.setVisibility(View.GONE);

                    if (rvHistoricoBancoHoras != null) {
                        HistoricoBancoHorasAdapter adapter = new HistoricoBancoHorasAdapter(lista);
                        rvHistoricoBancoHoras.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar lançamentos para UID " + uidFuncionario, e);
                    if (progressBarBancoHoras != null) progressBarBancoHoras.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar histórico de lançamentos.", Toast.LENGTH_SHORT).show();
                });
    }
}
