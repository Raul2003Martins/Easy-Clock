package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.dao.JornadaTrabalhoDAO;
import com.easyclock.app.dao.PontoDAO;
import com.easyclock.app.model.JornadaTrabalho;
import com.easyclock.app.model.Ponto;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RegistroPontoActivity extends AppCompatActivity {

    private ProgressBar progressBarPonto;
    private TextView tvSemJornadaAviso;

    private TextView tvNomeFuncionarioPonto;
    private TextView tvDataAtualPonto;
    private TextView tvNomeJornadaPonto;
    private TextView tvHorariosPrevistosPonto;

    private TextView tvMarcacaoEntrada;
    private TextView tvMarcacaoSaidaAlmoco;
    private TextView tvMarcacaoRetornoAlmoco;
    private TextView tvMarcacaoSaida;

    private TextView tvProximaMarcacao;
    private MaterialButton btnConfirmarPonto;
    private MaterialButton btnVerHistoricoPonto;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private JornadaTrabalhoDAO jornadaDAO;
    private PontoDAO pontoDAO;

    private String uidFuncionario;
    private String empresaIdFuncionario;
    private String jornadaIdFuncionario;
    private String nomeFuncionario;
    private JornadaTrabalho jornadaAtual;

    private String dataReferenciaHoje;
    private String dataReferenciaAtiva;
    private String proximaMarcacaoTipo = "ENTRADA";
    private boolean jornadaConcluidaHoje = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_ponto);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        jornadaDAO = new JornadaTrabalhoDAO();
        pontoDAO = new PontoDAO();

        inicializarComponentes();
        verificarECarregarDados();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarRegistroPonto);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        progressBarPonto = findViewById(R.id.progressBarPonto);
        tvSemJornadaAviso = findViewById(R.id.tvSemJornadaAviso);

        tvNomeFuncionarioPonto = findViewById(R.id.tvNomeFuncionarioPonto);
        tvDataAtualPonto = findViewById(R.id.tvDataAtualPonto);
        tvNomeJornadaPonto = findViewById(R.id.tvNomeJornadaPonto);
        tvHorariosPrevistosPonto = findViewById(R.id.tvHorariosPrevistosPonto);

        tvMarcacaoEntrada = findViewById(R.id.tvMarcacaoEntrada);
        tvMarcacaoSaidaAlmoco = findViewById(R.id.tvMarcacaoSaidaAlmoco);
        tvMarcacaoRetornoAlmoco = findViewById(R.id.tvMarcacaoRetornoAlmoco);
        tvMarcacaoSaida = findViewById(R.id.tvMarcacaoSaida);

        tvProximaMarcacao = findViewById(R.id.tvProximaMarcacao);
        btnConfirmarPonto = findViewById(R.id.btnConfirmarPonto);
        btnVerHistoricoPonto = findViewById(R.id.btnVerHistoricoPonto);

        SimpleDateFormat sdfDataRef = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
        sdfDataRef.setTimeZone(TimeZone.getTimeZone("UTC"));
        dataReferenciaHoje = sdfDataRef.format(new Date());
        dataReferenciaAtiva = dataReferenciaHoje;

        SimpleDateFormat sdfDataExib = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
        sdfDataExib.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        String dataExibStr = sdfDataExib.format(new Date());
        if (!dataExibStr.isEmpty()) {
            dataExibStr = dataExibStr.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + dataExibStr.substring(1);
        }
        if (tvDataAtualPonto != null) tvDataAtualPonto.setText("Data: " + dataExibStr);

        if (btnConfirmarPonto != null) {
            btnConfirmarPonto.setOnClickListener(v -> realizarRegistroPonto());
        }

        if (btnVerHistoricoPonto != null) {
            btnVerHistoricoPonto.setOnClickListener(v -> {
                Intent intent = new Intent(RegistroPontoActivity.this, HistoricoPontoActivity.class);
                startActivity(intent);
            });
        }
    }

    private void verificarECarregarDados() {
        uidFuncionario = autenticacaoService.getUidAtual();
        if (uidFuncionario == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarPonto != null) progressBarPonto.setVisibility(View.VISIBLE);

        funcionarioDAO.buscarPorUid(uidFuncionario)
                .addOnSuccessListener(func -> {
                    if (func == null || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Apenas funcionários ativos podem registrar ponto.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    nomeFuncionario = func.getNome();
                    empresaIdFuncionario = func.getEmpresaId();
                    jornadaIdFuncionario = func.getJornadaId();

                    if (tvNomeFuncionarioPonto != null) {
                        tvNomeFuncionarioPonto.setText("Funcionário: " + (nomeFuncionario != null ? nomeFuncionario : "--"));
                    }

                    if (jornadaIdFuncionario == null || jornadaIdFuncionario.trim().isEmpty()) {
                        exibirAvisoSemJornada();
                    } else {
                        carregarJornadaEJornadasDoDia();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar dados do usuário: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void exibirAvisoSemJornada() {
        if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
        if (tvSemJornadaAviso != null) tvSemJornadaAviso.setVisibility(View.VISIBLE);
        if (tvNomeJornadaPonto != null) tvNomeJornadaPonto.setText("Jornada: Não atribuída");
        if (tvHorariosPrevistosPonto != null) tvHorariosPrevistosPonto.setText("Horários Previstos: Sem jornada");
        if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(false);
        if (tvProximaMarcacao != null) tvProximaMarcacao.setText("Ponto bloqueado (sem jornada)");
    }

    private void carregarJornadaEJornadasDoDia() {
        jornadaDAO.buscarPorId(jornadaIdFuncionario)
                .addOnSuccessListener(j -> {
                    if (j == null || !j.isAtivo()) {
                        exibirAvisoSemJornada();
                        return;
                    }

                    jornadaAtual = j;
                    if (tvSemJornadaAviso != null) tvSemJornadaAviso.setVisibility(View.GONE);

                    if (tvNomeJornadaPonto != null) {
                        tvNomeJornadaPonto.setText("Jornada: " + j.getNome());
                    }

                    String prevStr = String.format(Locale.getDefault(), "Previsto: %s - %s | %s - %s",
                            j.getEntrada(), j.getSaidaAlmoco(), j.getRetornoAlmoco(), j.getSaida());
                    if (tvHorariosPrevistosPonto != null) {
                        tvHorariosPrevistosPonto.setText(prevStr);
                    }

                    carregarPontosHoje();
                })
                .addOnFailureListener(e -> {
                    if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar jornada: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarPontosHoje() {
        pontoDAO.buscarControleExpedienteAtivo(uidFuncionario)
                .addOnSuccessListener(ctrlDoc -> {
                    if (ctrlDoc != null && ctrlDoc.exists() && "ABERTO".equalsIgnoreCase(ctrlDoc.getString("estado"))) {
                        String dataRefAtiva = ctrlDoc.getString("dataReferencia");
                        if (dataRefAtiva != null && !dataRefAtiva.trim().isEmpty()) {
                            dataReferenciaAtiva = dataRefAtiva.trim();
                        } else {
                            dataReferenciaAtiva = dataReferenciaHoje;
                        }
                    } else {
                        dataReferenciaAtiva = dataReferenciaHoje;
                    }

                    pontoDAO.listarPontosDoDia(uidFuncionario, dataReferenciaAtiva)
                            .addOnSuccessListener(listaPontos -> {
                                if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);

                                atualizarStatusMarcacoesEProximoTipo(listaPontos);
                            })
                            .addOnFailureListener(e -> {
                                if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                                Toast.makeText(this, "Erro ao consultar pontos de hoje: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    dataReferenciaAtiva = dataReferenciaHoje;
                    pontoDAO.listarPontosDoDia(uidFuncionario, dataReferenciaAtiva)
                            .addOnSuccessListener(listaPontos -> {
                                if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                                atualizarStatusMarcacoesEProximoTipo(listaPontos);
                            })
                            .addOnFailureListener(err -> {
                                if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                                Toast.makeText(this, "Erro ao consultar pontos: " + err.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void atualizarStatusMarcacoesEProximoTipo(List<Ponto> pontosHoje) {
        boolean temEntrada = false;
        boolean temSaidaAlmoco = false;
        boolean temRetornoAlmoco = false;
        boolean temSaida = false;

        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", new Locale("pt", "BR"));
        sdfTime.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

        if (pontosHoje != null) {
            for (Ponto p : pontosHoje) {
                if (p == null || p.getTipoPonto() == null) continue;

                String horaFormatted = p.getDataHora() != null ? sdfTime.format(p.getDataHora().toDate()) : "Registrado";

                if ("ENTRADA".equalsIgnoreCase(p.getTipoPonto())) {
                    temEntrada = true;
                    if (tvMarcacaoEntrada != null) tvMarcacaoEntrada.setText("1. Entrada: " + horaFormatted);
                } else if ("SAIDA_ALMOCO".equalsIgnoreCase(p.getTipoPonto())) {
                    temSaidaAlmoco = true;
                    if (tvMarcacaoSaidaAlmoco != null) tvMarcacaoSaidaAlmoco.setText("2. Saída Almoço: " + horaFormatted);
                } else if ("RETORNO_ALMOCO".equalsIgnoreCase(p.getTipoPonto())) {
                    temRetornoAlmoco = true;
                    if (tvMarcacaoRetornoAlmoco != null) tvMarcacaoRetornoAlmoco.setText("3. Retorno Almoço: " + horaFormatted);
                } else if ("SAIDA".equalsIgnoreCase(p.getTipoPonto())) {
                    temSaida = true;
                    if (tvMarcacaoSaida != null) tvMarcacaoSaida.setText("4. Saída Final: " + horaFormatted);
                }
            }
        }

        if (!temEntrada) {
            if (tvMarcacaoEntrada != null) tvMarcacaoEntrada.setText("1. Entrada: Não registrada");
        }
        if (!temSaidaAlmoco) {
            if (tvMarcacaoSaidaAlmoco != null) tvMarcacaoSaidaAlmoco.setText("2. Saída Almoço: Não registrada");
        }
        if (!temRetornoAlmoco) {
            if (tvMarcacaoRetornoAlmoco != null) tvMarcacaoRetornoAlmoco.setText("3. Retorno Almoço: Não registrada");
        }
        if (!temSaida) {
            if (tvMarcacaoSaida != null) tvMarcacaoSaida.setText("4. Saída Final: Não registrada");
        }

        jornadaConcluidaHoje = false;
        if (!temEntrada) {
            proximaMarcacaoTipo = "ENTRADA";
        } else if (!temSaidaAlmoco) {
            proximaMarcacaoTipo = "SAIDA_ALMOCO";
        } else if (!temRetornoAlmoco) {
            proximaMarcacaoTipo = "RETORNO_ALMOCO";
        } else if (!temSaida) {
            proximaMarcacaoTipo = "SAIDA";
        } else {
            jornadaConcluidaHoje = true;
        }

        if (jornadaConcluidaHoje) {
            if (tvProximaMarcacao != null) tvProximaMarcacao.setText("Jornada do dia concluída!");
            if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(false);
        } else {
            if (tvProximaMarcacao != null) {
                tvProximaMarcacao.setText("Próxima marcação: " + formatarTipoPonto(proximaMarcacaoTipo));
            }
            if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
        }
    }

    private String formatarTipoPonto(String tipo) {
        if ("ENTRADA".equalsIgnoreCase(tipo)) return "ENTRADA";
        if ("SAIDA_ALMOCO".equalsIgnoreCase(tipo)) return "SAÍDA ALMOÇO";
        if ("RETORNO_ALMOCO".equalsIgnoreCase(tipo)) return "RETORNO ALMOÇO";
        if ("SAIDA".equalsIgnoreCase(tipo)) return "SAÍDA FINAL";
        return tipo;
    }

    private void realizarRegistroPonto() {
        if (jornadaConcluidaHoje || jornadaAtual == null) return;

        if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(false);
        if (progressBarPonto != null) progressBarPonto.setVisibility(View.VISIBLE);

        if ("ENTRADA".equalsIgnoreCase(proximaMarcacaoTipo)) {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("entrada", jornadaAtual.getEntrada());
            snapshot.put("saidaAlmoco", jornadaAtual.getSaidaAlmoco());
            snapshot.put("retornoAlmoco", jornadaAtual.getRetornoAlmoco());
            snapshot.put("saida", jornadaAtual.getSaida());
            snapshot.put("cargaHorariaMinutos", jornadaAtual.getCargaHorariaMinutos());
            snapshot.put("toleranciaMinutos", jornadaAtual.getToleranciaMinutos());

            Ponto p = new Ponto();
            p.setEmpresaId(empresaIdFuncionario);
            p.setFuncionarioId(uidFuncionario);
            p.setJornadaId(jornadaIdFuncionario);
            p.setDataReferencia(dataReferenciaHoje);
            p.setTipoPonto(proximaMarcacaoTipo);
            p.setJornadaSnapshot(snapshot);

            pontoDAO.registrarPontoComExpediente(p, snapshot, "SAIDA_ALMOCO")
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Ponto ENTRADA registrado com sucesso!", Toast.LENGTH_SHORT).show();
                        carregarPontosHoje();
                    })
                    .addOnFailureListener(e -> {
                        if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                        if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
                        Toast.makeText(this, "Falha ao registrar ponto: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            pontoDAO.buscarExpedienteDoDia(uidFuncionario, dataReferenciaAtiva)
                    .addOnSuccessListener(expDoc -> {
                        if (expDoc == null || !expDoc.exists() || expDoc.get("jornadaSnapshot") == null) {
                            if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                            if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
                            Toast.makeText(this, "Erro ao carregar o expediente congelado do dia.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, Object> frozenSnapshot = (Map<String, Object>) expDoc.get("jornadaSnapshot");

                        String proximaDep = "SAIDA_ALMOCO";
                        if ("SAIDA_ALMOCO".equalsIgnoreCase(proximaMarcacaoTipo)) proximaDep = "RETORNO_ALMOCO";
                        else if ("RETORNO_ALMOCO".equalsIgnoreCase(proximaMarcacaoTipo)) proximaDep = "SAIDA";
                        else if ("SAIDA".equalsIgnoreCase(proximaMarcacaoTipo)) proximaDep = "CONCLUIDO";

                        Ponto p = new Ponto();
                        p.setEmpresaId(empresaIdFuncionario);
                        p.setFuncionarioId(uidFuncionario);
                        p.setJornadaId(jornadaIdFuncionario);
                        p.setDataReferencia(dataReferenciaAtiva);
                        p.setTipoPonto(proximaMarcacaoTipo);
                        p.setJornadaSnapshot(frozenSnapshot);

                        final String proximaDepFinal = proximaDep;

                        if ("SAIDA".equalsIgnoreCase(proximaMarcacaoTipo)) {
                            pontoDAO.listarPontosDoDia(uidFuncionario, dataReferenciaAtiva)
                                    .addOnSuccessListener(pontosDoDia -> {
                                        com.google.firebase.Timestamp tEntrada = null;
                                        com.google.firebase.Timestamp tSaidaAlmoco = null;
                                        com.google.firebase.Timestamp tRetornoAlmoco = null;
                                        com.google.firebase.Timestamp tSaida = com.google.firebase.Timestamp.now();

                                        if (pontosDoDia != null) {
                                            for (Ponto pontoItem : pontosDoDia) {
                                                if (pontoItem == null) continue;
                                                if ("ENTRADA".equalsIgnoreCase(pontoItem.getTipoPonto())) tEntrada = pontoItem.getDataHora();
                                                else if ("SAIDA_ALMOCO".equalsIgnoreCase(pontoItem.getTipoPonto())) tSaidaAlmoco = pontoItem.getDataHora();
                                                else if ("RETORNO_ALMOCO".equalsIgnoreCase(pontoItem.getTipoPonto())) tRetornoAlmoco = pontoItem.getDataHora();
                                            }
                                        }

                                        long cargaPrevista = 480;
                                        long tolerancia = 5;
                                        if (frozenSnapshot != null) {
                                            Object cVal = frozenSnapshot.get("cargaHorariaMinutos");
                                            if (cVal instanceof Number) cargaPrevista = ((Number) cVal).longValue();
                                            Object tVal = frozenSnapshot.get("toleranciaMinutos");
                                            if (tVal instanceof Number) tolerancia = ((Number) tVal).longValue();
                                        }

                                        long minsTrabalhados = com.easyclock.app.service.BancoHorasService.calcularMinutosTrabalhados(tEntrada, tSaidaAlmoco, tRetornoAlmoco, tSaida);
                                        long minsSaldo = com.easyclock.app.service.BancoHorasService.calcularSaldoMinutos(minsTrabalhados, cargaPrevista, tolerancia);

                                        pontoDAO.registrarPontoComExpedienteEBancoHoras(p, frozenSnapshot, proximaDepFinal, minsTrabalhados, cargaPrevista, minsSaldo)
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(this, "Ponto " + formatarTipoPonto(proximaMarcacaoTipo) + " registrado e saldo calculado!", Toast.LENGTH_SHORT).show();
                                                    carregarPontosHoje();
                                                })
                                                .addOnFailureListener(e -> {
                                                    if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                                                    if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
                                                    Toast.makeText(this, "Falha ao registrar ponto: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                                        if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
                                        Toast.makeText(this, "Falha ao calcular banco de horas: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            pontoDAO.registrarPontoComExpediente(p, frozenSnapshot, proximaDepFinal)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Ponto " + formatarTipoPonto(proximaMarcacaoTipo) + " registrado com sucesso!", Toast.LENGTH_SHORT).show();
                                        carregarPontosHoje();
                                    })
                                    .addOnFailureListener(e -> {
                                        if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                                        if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
                                        Toast.makeText(this, "Falha ao registrar ponto: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (progressBarPonto != null) progressBarPonto.setVisibility(View.GONE);
                        if (btnConfirmarPonto != null) btnConfirmarPonto.setEnabled(true);
                        Toast.makeText(this, "Falha ao consultar expediente: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }
}
