package com.easyclock.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.dao.AjustePontoDAO;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.model.AjustePonto;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AjustePontoActivity extends AppCompatActivity {

    private ProgressBar progressBarAjuste;
    private TextInputEditText etDataAjustePonto;
    private AutoCompleteTextView actvTipoPontoAjuste;
    private TextInputEditText etHorarioAjustePonto;
    private TextInputEditText etMotivoAjustePonto;
    private MaterialButton btnEnviarAjustePonto;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private AjustePontoDAO ajustePontoDAO;

    private String uidFuncionario;
    private String empresaIdFuncionario;
    private String nomeFuncionario;

    private String dataReferenciaTecnica = ""; // YYYY-MM-DD
    private String dataExibicaoSelecionada = ""; // dd/MM/yyyy
    private String tipoPontoCodigo = ""; // ENTRADA, SAIDA_ALMOCO, RETORNO_ALMOCO, SAIDA

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajuste_ponto);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        ajustePontoDAO = new AjustePontoDAO();

        inicializarComponentes();
        carregarDadosUsuario();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarAjustePonto = findViewById(R.id.toolbarAjustePonto);
        if (toolbarAjustePonto != null) {
            toolbarAjustePonto.setNavigationOnClickListener(v -> finish());
        }

        progressBarAjuste = findViewById(R.id.progressBarAjuste);
        etDataAjustePonto = findViewById(R.id.etDataAjustePonto);
        actvTipoPontoAjuste = findViewById(R.id.actvTipoPontoAjuste);
        etHorarioAjustePonto = findViewById(R.id.etHorarioAjustePonto);
        etMotivoAjustePonto = findViewById(R.id.etMotivoAjustePonto);
        btnEnviarAjustePonto = findViewById(R.id.btnEnviarAjustePonto);

        configurarDropdownTipoPonto();
        configurarListeners();
    }

    private void configurarDropdownTipoPonto() {
        if (actvTipoPontoAjuste == null) return;

        String[] opcoes = new String[]{"ENTRADA", "SAÍDA ALMOÇO", "RETORNO ALMOÇO", "SAÍDA FINAL"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, opcoes);
        actvTipoPontoAjuste.setAdapter(adapter);

        actvTipoPontoAjuste.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    tipoPontoCodigo = "ENTRADA";
                    break;
                case 1:
                    tipoPontoCodigo = "SAIDA_ALMOCO";
                    break;
                case 2:
                    tipoPontoCodigo = "RETORNO_ALMOCO";
                    break;
                case 3:
                    tipoPontoCodigo = "SAIDA";
                    break;
                default:
                    tipoPontoCodigo = "";
                    break;
            }
        });
    }

    private void configurarListeners() {
        if (etDataAjustePonto != null) {
            etDataAjustePonto.setOnClickListener(v -> exibirDatePicker());
        }

        if (etHorarioAjustePonto != null) {
            etHorarioAjustePonto.setOnClickListener(v -> exibirTimePicker());
        }

        if (btnEnviarAjustePonto != null) {
            btnEnviarAjustePonto.setOnClickListener(v -> validarEEnviarSolicitacao());
        }
    }

    private void carregarDadosUsuario() {
        uidFuncionario = autenticacaoService.getUidAtual();
        if (uidFuncionario == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.VISIBLE);

        funcionarioDAO.buscarPorUid(uidFuncionario)
                .addOnSuccessListener(func -> {
                    if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.GONE);

                    if (func == null || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Apenas funcionários ativos podem solicitar ajustes.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    nomeFuncionario = func.getNome();
                    empresaIdFuncionario = func.getEmpresaId();
                })
                .addOnFailureListener(e -> {
                    if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void exibirDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int ano = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int dia = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, monthOfYear, dayOfMonth, 0, 0, 0);

                    SimpleDateFormat sdfExib = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat sdfTec = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    dataExibicaoSelecionada = sdfExib.format(sel.getTime());
                    dataReferenciaTecnica = sdfTec.format(sel.getTime());

                    if (etDataAjustePonto != null) {
                        etDataAjustePonto.setText(dataExibicaoSelecionada);
                        etDataAjustePonto.setError(null);
                    }
                }, ano, mes, dia);

        // Bloquear seleção de datas futuras
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void exibirTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hora = calendar.get(Calendar.HOUR_OF_DAY);
        int minuto = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    String horarioFormatado = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                    if (etHorarioAjustePonto != null) {
                        etHorarioAjustePonto.setText(horarioFormatado);
                        etHorarioAjustePonto.setError(null);
                    }
                }, hora, minuto, true);
        timePickerDialog.show();
    }

    private void validarEEnviarSolicitacao() {
        if (etDataAjustePonto == null || etHorarioAjustePonto == null || etMotivoAjustePonto == null) return;

        String dataExib = etDataAjustePonto.getText() != null ? etDataAjustePonto.getText().toString().trim() : "";
        String horario = etHorarioAjustePonto.getText() != null ? etHorarioAjustePonto.getText().toString().trim() : "";
        String motivo = etMotivoAjustePonto.getText() != null ? etMotivoAjustePonto.getText().toString().trim() : "";

        if (dataExib.isEmpty() || dataReferenciaTecnica.isEmpty()) {
            etDataAjustePonto.setError("Selecione a data do ponto.");
            etDataAjustePonto.requestFocus();
            return;
        }

        // Validação estrita de data real
        try {
            SimpleDateFormat sdfCheck = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdfCheck.setLenient(false);
            Date dateCheck = sdfCheck.parse(dataExib);
            if (dateCheck != null && dateCheck.after(new Date())) {
                etDataAjustePonto.setError("Não é possível solicitar ajuste para datas futuras.");
                etDataAjustePonto.requestFocus();
                return;
            }
        } catch (Exception e) {
            etDataAjustePonto.setError("Data inválida. Selecione uma data no formato dd/MM/yyyy.");
            etDataAjustePonto.requestFocus();
            return;
        }

        if (tipoPontoCodigo.isEmpty()) {
            if (actvTipoPontoAjuste != null) {
                actvTipoPontoAjuste.setError("Selecione o tipo de marcação.");
                actvTipoPontoAjuste.requestFocus();
            }
            return;
        }

        if (horario.isEmpty()) {
            etHorarioAjustePonto.setError("Informe o horário do ponto.");
            etHorarioAjustePonto.requestFocus();
            return;
        }

        if (!horario.matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")) {
            etHorarioAjustePonto.setError("Horário inválido. Use o formato HH:mm.");
            etHorarioAjustePonto.requestFocus();
            return;
        }

        if (motivo.isEmpty()) {
            etMotivoAjustePonto.setError("Preencha a justificativa da solicitação.");
            etMotivoAjustePonto.requestFocus();
            return;
        }

        if (motivo.length() < 10) {
            etMotivoAjustePonto.setError("A justificativa deve ter pelo menos 10 caracteres.");
            etMotivoAjustePonto.requestFocus();
            return;
        }

        if (motivo.length() > 300) {
            etMotivoAjustePonto.setError("A justificativa deve ter no máximo 300 caracteres.");
            etMotivoAjustePonto.requestFocus();
            return;
        }

        if (btnEnviarAjustePonto != null) btnEnviarAjustePonto.setEnabled(false);
        if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.VISIBLE);

        // Prevenção de duplicidade: verificar se já existe solicitação pendente idêntica
        ajustePontoDAO.verificarExisteSolicitacaoPendente(uidFuncionario, dataReferenciaTecnica, tipoPontoCodigo)
                .addOnSuccessListener(jaExiste -> {
                    if (jaExiste) {
                        if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.GONE);
                        if (btnEnviarAjustePonto != null) btnEnviarAjustePonto.setEnabled(true);

                        new AlertDialog.Builder(this)
                                .setTitle("Solicitação Duplicada")
                                .setMessage("Você já possui uma solicitação de ajuste pendente para esta marcação nesta data. Aguarde a análise do RH.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    AjustePonto ajuste = new AjustePonto();
                    ajuste.setFuncionarioId(uidFuncionario);
                    ajuste.setEmpresaId(empresaIdFuncionario);
                    ajuste.setNomeFuncionario(nomeFuncionario);
                    ajuste.setDataReferencia(dataReferenciaTecnica);
                    ajuste.setDataExibicao(dataExib);
                    ajuste.setTipoPonto(tipoPontoCodigo);
                    ajuste.setHorarioAjustado(horario);
                    ajuste.setMotivo(motivo);

                    ajustePontoDAO.criarSolicitacao(ajuste)
                            .addOnSuccessListener(id -> {
                                if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.GONE);
                                new AlertDialog.Builder(this)
                                        .setTitle("Solicitação Enviada")
                                        .setMessage("Sua solicitação de ajuste para " + dataExib + " às " + horario + " foi registrada com sucesso!")
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.GONE);
                                if (btnEnviarAjustePonto != null) btnEnviarAjustePonto.setEnabled(true);
                                Toast.makeText(this, "Falha ao enviar solicitação: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressBarAjuste != null) progressBarAjuste.setVisibility(View.GONE);
                    if (btnEnviarAjustePonto != null) btnEnviarAjustePonto.setEnabled(true);
                    Toast.makeText(this, "Erro de conexão: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
