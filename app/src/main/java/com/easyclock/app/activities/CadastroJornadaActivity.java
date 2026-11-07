package com.easyclock.app.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.dao.JornadaTrabalhoDAO;
import com.easyclock.app.model.JornadaTrabalho;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CadastroJornadaActivity extends AppCompatActivity {

    private TextInputEditText etNomeJornadaForm;
    private TextInputEditText etToleranciaMinutos;

    private MaterialButton btnHoraEntrada;
    private MaterialButton btnHoraSaidaAlmoco;
    private MaterialButton btnHoraRetornoAlmoco;
    private MaterialButton btnHoraSaida;

    private CheckBox cbSegunda, cbTerca, cbQuarta, cbQuinta, cbSexta, cbSabado, cbDomingo;

    private TextView tvCargaHorariaCalculada;
    private MaterialButton btnSalvarJornada;

    private String horaEntrada = "08:00";
    private String horaSaidaAlmoco = "12:00";
    private String horaRetornoAlmoco = "13:00";
    private String horaSaida = "17:00";

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private JornadaTrabalhoDAO jornadaDAO;

    private String empresaId;
    private String jornadaIdEdicao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_jornada);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        jornadaDAO = new JornadaTrabalhoDAO();

        empresaId = getIntent().getStringExtra("empresaId");
        jornadaIdEdicao = getIntent().getStringExtra("jornadaId");

        inicializarComponentes();
        verificarPermissaoECarregar();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCadastroJornada);
        if (toolbar != null) {
            if (jornadaIdEdicao != null) {
                toolbar.setTitle("Editar Jornada de Trabalho");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etNomeJornadaForm = findViewById(R.id.etNomeJornadaForm);
        etToleranciaMinutos = findViewById(R.id.etToleranciaMinutos);

        btnHoraEntrada = findViewById(R.id.btnHoraEntrada);
        btnHoraSaidaAlmoco = findViewById(R.id.btnHoraSaidaAlmoco);
        btnHoraRetornoAlmoco = findViewById(R.id.btnHoraRetornoAlmoco);
        btnHoraSaida = findViewById(R.id.btnHoraSaida);

        cbSegunda = findViewById(R.id.cbSegunda);
        cbTerca = findViewById(R.id.cbTerca);
        cbQuarta = findViewById(R.id.cbQuarta);
        cbQuinta = findViewById(R.id.cbQuinta);
        cbSexta = findViewById(R.id.cbSexta);
        cbSabado = findViewById(R.id.cbSabado);
        cbDomingo = findViewById(R.id.cbDomingo);

        tvCargaHorariaCalculada = findViewById(R.id.tvCargaHorariaCalculada);
        btnSalvarJornada = findViewById(R.id.btnSalvarJornada);

        configurarTimePickers();
        atualizarExibicaoHorariosECarga();

        if (btnSalvarJornada != null) {
            btnSalvarJornada.setOnClickListener(v -> salvarJornada());
        }
    }

    private void configurarTimePickers() {
        if (btnHoraEntrada != null) {
            btnHoraEntrada.setOnClickListener(v -> abrirTimePicker(horaEntrada, (h, m) -> {
                horaEntrada = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                atualizarExibicaoHorariosECarga();
            }));
        }

        if (btnHoraSaidaAlmoco != null) {
            btnHoraSaidaAlmoco.setOnClickListener(v -> abrirTimePicker(horaSaidaAlmoco, (h, m) -> {
                horaSaidaAlmoco = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                atualizarExibicaoHorariosECarga();
            }));
        }

        if (btnHoraRetornoAlmoco != null) {
            btnHoraRetornoAlmoco.setOnClickListener(v -> abrirTimePicker(horaRetornoAlmoco, (h, m) -> {
                horaRetornoAlmoco = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                atualizarExibicaoHorariosECarga();
            }));
        }

        if (btnHoraSaida != null) {
            btnHoraSaida.setOnClickListener(v -> abrirTimePicker(horaSaida, (h, m) -> {
                horaSaida = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                atualizarExibicaoHorariosECarga();
            }));
        }
    }

    private interface TimePickerCallback {
        void onTimeSelected(int hour, int minute);
    }

    private void abrirTimePicker(String horaAtual, TimePickerCallback callback) {
        int h = 8, m = 0;
        try {
            String[] partes = horaAtual.split(":");
            h = Integer.parseInt(partes[0]);
            m = Integer.parseInt(partes[1]);
        } catch (Exception e) {
            // Usa valores padrão
        }

        TimePickerDialog tpd = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if (callback != null) callback.onTimeSelected(hourOfDay, minute);
        }, h, m, true);
        tpd.show();
    }

    private void atualizarExibicaoHorariosECarga() {
        if (btnHoraEntrada != null) btnHoraEntrada.setText("Entrada: " + horaEntrada);
        if (btnHoraSaidaAlmoco != null) btnHoraSaidaAlmoco.setText("Saída Almoço: " + horaSaidaAlmoco);
        if (btnHoraRetornoAlmoco != null) btnHoraRetornoAlmoco.setText("Retorno Almoço: " + horaRetornoAlmoco);
        if (btnHoraSaida != null) btnHoraSaida.setText("Saída: " + horaSaida);

        long minutosTrabalhados = calcularCargaHorariaMinutos();
        long horas = minutosTrabalhados / 60;
        long mins = minutosTrabalhados % 60;

        if (tvCargaHorariaCalculada != null) {
            if (minutosTrabalhados > 0) {
                tvCargaHorariaCalculada.setText(String.format(Locale.getDefault(),
                        "Carga Horária Diária: %dh %02dmin (%d minutos)", horas, mins, minutosTrabalhados));
                tvCargaHorariaCalculada.setTextColor(getResources().getColor(R.color.primary));
            } else {
                tvCargaHorariaCalculada.setText("Horários inválidos (A sequência deve ser Entrada < Almoço < Retorno < Saída)");
                tvCargaHorariaCalculada.setTextColor(getResources().getColor(R.color.button_danger));
            }
        }
    }

    private long calcularCargaHorariaMinutos() {
        int mEntrada = timeToMinutes(horaEntrada);
        int mSaidaAlmoco = timeToMinutes(horaSaidaAlmoco);
        int mRetornoAlmoco = timeToMinutes(horaRetornoAlmoco);
        int mSaida = timeToMinutes(horaSaida);

        if (mEntrada < mSaidaAlmoco && mSaidaAlmoco < mRetornoAlmoco && mRetornoAlmoco < mSaida) {
            long turno1 = mSaidaAlmoco - mEntrada;
            long turno2 = mSaida - mRetornoAlmoco;
            return turno1 + turno2;
        }
        return 0;
    }

    private int timeToMinutes(String timeStr) {
        try {
            String[] p = timeStr.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void verificarPermissaoECarregar() {
        String uid = autenticacaoService.getUidAtual();
        if (uid == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        funcionarioDAO.buscarPorUid(uid)
                .addOnSuccessListener(func -> {
                    if (func == null || !"RH".equalsIgnoreCase(func.getPerfil()) || !"ATIVO".equalsIgnoreCase(func.getStatus())) {
                        Toast.makeText(this, "Acesso negado. Apenas RH ativo pode cadastrar jornadas.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (empresaId == null) {
                        empresaId = func.getEmpresaId();
                    }

                    if (jornadaIdEdicao != null) {
                        carregarJornadaParaEdicao();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao validar permissões.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void carregarJornadaParaEdicao() {
        jornadaDAO.buscarPorId(jornadaIdEdicao)
                .addOnSuccessListener(j -> {
                    if (j == null) {
                        Toast.makeText(this, "Jornada não encontrada.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (etNomeJornadaForm != null && j.getNome() != null) {
                        etNomeJornadaForm.setText(j.getNome());
                    }

                    if (j.getEntrada() != null) horaEntrada = j.getEntrada();
                    if (j.getSaidaAlmoco() != null) horaSaidaAlmoco = j.getSaidaAlmoco();
                    if (j.getRetornoAlmoco() != null) horaRetornoAlmoco = j.getRetornoAlmoco();
                    if (j.getSaida() != null) horaSaida = j.getSaida();

                    if (etToleranciaMinutos != null) {
                        etToleranciaMinutos.setText(String.valueOf(j.getToleranciaMinutos()));
                    }

                    List<Long> dias = j.getDiasSemana();
                    if (dias != null) {
                        if (cbSegunda != null) cbSegunda.setChecked(dias.contains(1L));
                        if (cbTerca != null) cbTerca.setChecked(dias.contains(2L));
                        if (cbQuarta != null) cbQuarta.setChecked(dias.contains(3L));
                        if (cbQuinta != null) cbQuinta.setChecked(dias.contains(4L));
                        if (cbSexta != null) cbSexta.setChecked(dias.contains(5L));
                        if (cbSabado != null) cbSabado.setChecked(dias.contains(6L));
                        if (cbDomingo != null) cbDomingo.setChecked(dias.contains(7L));
                    }

                    atualizarExibicaoHorariosECarga();
                });
    }

    private void salvarJornada() {
        String nome = etNomeJornadaForm != null && etNomeJornadaForm.getText() != null ? etNomeJornadaForm.getText().toString().trim() : "";
        if (nome.isEmpty()) {
            if (etNomeJornadaForm != null) etNomeJornadaForm.setError("Informe o nome da jornada");
            return;
        }

        long cargaMinutos = calcularCargaHorariaMinutos();
        if (cargaMinutos <= 0) {
            Toast.makeText(this, "Horários inválidos. Verifique a sequência dos horários.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Long> diasSelecionados = new ArrayList<>();
        if (cbSegunda != null && cbSegunda.isChecked()) diasSelecionados.add(1L);
        if (cbTerca != null && cbTerca.isChecked()) diasSelecionados.add(2L);
        if (cbQuarta != null && cbQuarta.isChecked()) diasSelecionados.add(3L);
        if (cbQuinta != null && cbQuinta.isChecked()) diasSelecionados.add(4L);
        if (cbSexta != null && cbSexta.isChecked()) diasSelecionados.add(5L);
        if (cbSabado != null && cbSabado.isChecked()) diasSelecionados.add(6L);
        if (cbDomingo != null && cbDomingo.isChecked()) diasSelecionados.add(7L);

        if (diasSelecionados.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos um dia da semana.", Toast.LENGTH_SHORT).show();
            return;
        }

        long tolerancia = 5;
        if (etToleranciaMinutos != null && etToleranciaMinutos.getText() != null) {
            try {
                tolerancia = Long.parseLong(etToleranciaMinutos.getText().toString().trim());
            } catch (Exception e) {
                tolerancia = 5;
            }
        }
        if (tolerancia < 0) {
            if (etToleranciaMinutos != null) etToleranciaMinutos.setError("A tolerância não pode ser negativa.");
            return;
        }

        if (btnSalvarJornada != null) btnSalvarJornada.setEnabled(false);

        JornadaTrabalho j = new JornadaTrabalho();
        j.setId(jornadaIdEdicao);
        j.setEmpresaId(empresaId);
        j.setNome(nome);
        j.setEntrada(horaEntrada);
        j.setSaidaAlmoco(horaSaidaAlmoco);
        j.setRetornoAlmoco(horaRetornoAlmoco);
        j.setSaida(horaSaida);
        j.setDiasSemana(diasSelecionados);
        j.setCargaHorariaMinutos(cargaMinutos);
        j.setToleranciaMinutos(tolerancia);
        j.setAtivo(true);

        jornadaDAO.salvarJornada(j)
                .addOnSuccessListener(id -> {
                    Toast.makeText(this, "Jornada salva com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (btnSalvarJornada != null) btnSalvarJornada.setEnabled(true);
                    Toast.makeText(this, "Erro ao salvar jornada: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
