package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.dao.AjustePontoDAO;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class SolicitacaoAjusteDetalheActivity extends AppCompatActivity {

    private TextView tvDataAjuste;
    private TextView tvHorarioAjuste;
    private TextView tvJustificativaAjuste;
    private MaterialButton btnConfirmarAjuste;
    private MaterialButton btnNaoConcederAjuste;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private AjustePontoDAO ajustePontoDAO;

    private String solicitacaoId;
    private String rhUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitacao_ajuste_detalhe);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        ajustePontoDAO = new AjustePontoDAO();

        solicitacaoId = getIntent().getStringExtra("solicitacaoId");

        inicializarComponentes();
        carregarEValidarRh();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarSolicitacaoAjuste = findViewById(R.id.toolbarSolicitacaoAjuste);
        if (toolbarSolicitacaoAjuste != null) {
            toolbarSolicitacaoAjuste.setNavigationOnClickListener(v -> finish());
        }

        tvDataAjuste = findViewById(R.id.tvDataAjuste);
        tvHorarioAjuste = findViewById(R.id.tvHorarioAjuste);
        tvJustificativaAjuste = findViewById(R.id.tvJustificativaAjuste);
        btnConfirmarAjuste = findViewById(R.id.btnConfirmarAjuste);
        btnNaoConcederAjuste = findViewById(R.id.btnNaoConcederAjuste);

        if (btnConfirmarAjuste != null) {
            btnConfirmarAjuste.setOnClickListener(v -> responderAjuste("APROVADO"));
        }

        if (btnNaoConcederAjuste != null) {
            btnNaoConcederAjuste.setOnClickListener(v -> responderAjuste("RECUSADO"));
        }
    }

    private void carregarEValidarRh() {
        rhUid = autenticacaoService.getUidAtual();
        if (rhUid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        funcionarioDAO.buscarPorUid(rhUid)
                .addOnSuccessListener(func -> {
                    if (func == null || !"RH".equalsIgnoreCase(func.getPerfil()) || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Apenas RH ativo pode gerenciar solicitações.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (solicitacaoId != null) {
                        carregarSolicitacao();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao validar permissões de RH.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void carregarSolicitacao() {
        ajustePontoDAO.buscarPorId(solicitacaoId)
                .addOnSuccessListener(a -> {
                    if (a == null) {
                        Toast.makeText(this, "Solicitação não encontrada.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (tvDataAjuste != null) {
                        tvDataAjuste.setText("Data: " + (a.getDataExibicao() != null ? a.getDataExibicao() : a.getDataReferencia()));
                    }

                    if (tvHorarioAjuste != null) {
                        String tipoStr = a.getTipoPonto() != null ? a.getTipoPonto() : "PONTO";
                        tvHorarioAjuste.setText("Tipo: " + tipoStr + " | Horário Solicitado: " + a.getHorarioAjustado());
                    }

                    if (tvJustificativaAjuste != null) {
                        tvJustificativaAjuste.setText(a.getMotivo() != null ? a.getMotivo() : "--");
                    }

                    if ("APROVADO".equalsIgnoreCase(a.getStatus()) || "RECUSADO".equalsIgnoreCase(a.getStatus())) {
                        if (btnConfirmarAjuste != null) btnConfirmarAjuste.setEnabled(false);
                        if (btnNaoConcederAjuste != null) btnNaoConcederAjuste.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar detalhes da solicitação.", Toast.LENGTH_SHORT).show());
    }

    private void responderAjuste(String novoStatus) {
        if (solicitacaoId == null || rhUid == null) return;

        if (btnConfirmarAjuste != null) btnConfirmarAjuste.setEnabled(false);
        if (btnNaoConcederAjuste != null) btnNaoConcederAjuste.setEnabled(false);

        String acaoStr = "APROVADO".equalsIgnoreCase(novoStatus) ? "aprovar" : "recusar";

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Ação")
                .setMessage("Deseja realmente " + acaoStr + " esta solicitação de ajuste de ponto?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    ajustePontoDAO.responderSolicitacao(solicitacaoId, rhUid, novoStatus, "Decisão registrada via painel do RH.")
                            .addOnSuccessListener(unused -> {
                                String msg = "APROVADO".equalsIgnoreCase(novoStatus) ?
                                        "Solicitação aprovada com sucesso!" : "Solicitação recusada com sucesso!";
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (btnConfirmarAjuste != null) btnConfirmarAjuste.setEnabled(true);
                                if (btnNaoConcederAjuste != null) btnNaoConcederAjuste.setEnabled(true);
                                Toast.makeText(this, "Falha ao registrar resposta: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Não", (dialog, which) -> {
                    if (btnConfirmarAjuste != null) btnConfirmarAjuste.setEnabled(true);
                    if (btnNaoConcederAjuste != null) btnNaoConcederAjuste.setEnabled(true);
                })
                .setCancelable(false)
                .show();
    }
}
