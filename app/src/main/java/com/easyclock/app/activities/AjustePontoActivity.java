package com.easyclock.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class AjustePontoActivity extends AppCompatActivity {

    private TextInputEditText etDataAjustePonto;
    private TextInputEditText etHorarioAjustePonto;
    private TextInputEditText etMotivoAjustePonto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajuste_ponto);

        inicializarComponentes();
        configurarListeners();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarAjustePonto = findViewById(R.id.toolbarAjustePonto);
        if (toolbarAjustePonto != null) {
            toolbarAjustePonto.setNavigationOnClickListener(v -> finish());
        }

        etDataAjustePonto = findViewById(R.id.etDataAjustePonto);
        etHorarioAjustePonto = findViewById(R.id.etHorarioAjustePonto);
        etMotivoAjustePonto = findViewById(R.id.etMotivoAjustePonto);
    }

    private void configurarListeners() {
        if (etDataAjustePonto != null) {
            etDataAjustePonto.setOnClickListener(v -> exibirDatePicker());
        }

        if (etHorarioAjustePonto != null) {
            etHorarioAjustePonto.setOnClickListener(v -> exibirTimePicker());
        }

        MaterialButton btnEnviarAjustePonto = findViewById(R.id.btnEnviarAjustePonto);
        if (btnEnviarAjustePonto != null) {
            btnEnviarAjustePonto.setOnClickListener(v -> enviarSolicitacaoAjuste());
        }
    }

    private void exibirDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int ano = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int dia = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    String dataFormatada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year);
                    if (etDataAjustePonto != null) {
                        etDataAjustePonto.setText(dataFormatada);
                    }
                }, ano, mes, dia);
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
                    }
                }, hora, minuto, true);
        timePickerDialog.show();
    }

    private void enviarSolicitacaoAjuste() {
        String data = etDataAjustePonto != null && etDataAjustePonto.getText() != null ? etDataAjustePonto.getText().toString().trim() : "";
        String horario = etHorarioAjustePonto != null && etHorarioAjustePonto.getText() != null ? etHorarioAjustePonto.getText().toString().trim() : "";
        String motivo = etMotivoAjustePonto != null && etMotivoAjustePonto.getText() != null ? etMotivoAjustePonto.getText().toString().trim() : "";

        if (data.isEmpty()) {
            if (etDataAjustePonto != null) {
                etDataAjustePonto.setError("Selecione a data do ponto");
                etDataAjustePonto.requestFocus();
            }
            return;
        }

        if (horario.isEmpty()) {
            if (etHorarioAjustePonto != null) {
                etHorarioAjustePonto.setError("Selecione o horário desejado");
                etHorarioAjustePonto.requestFocus();
            }
            return;
        }

        if (motivo.isEmpty()) {
            if (etMotivoAjustePonto != null) {
                etMotivoAjustePonto.setError("Descreva o motivo da solicitação");
                etMotivoAjustePonto.requestFocus();
            }
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Solicitação Enviada")
                .setMessage("Sua solicitação de ajuste de ponto para o dia " + data + " às " + horario + " foi enviada com sucesso!")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
