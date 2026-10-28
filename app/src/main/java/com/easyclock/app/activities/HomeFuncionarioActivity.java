package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.BuildConfig;
import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HomeFuncionarioActivity extends AppCompatActivity {

    private TextView tvHoraAtual;
    private TextView tvDataAtual;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    private boolean clockSincronizado = false;
    private long baseServerTimeMs = 0;
    private long baseMonotonicTimeMs = 0;
    private long ultimaSincronizacaoMonotonicMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_funcionario);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();

        inicializarComponentes();
        configurarClockLoop();
    }

    private void inicializarComponentes() {
        tvHoraAtual = findViewById(R.id.tvHoraAtual);
        tvDataAtual = findViewById(R.id.tvDataAtual);

        ImageButton btnPerfilHeader = findViewById(R.id.btnPerfilHeader);
        if (btnPerfilHeader != null) {
            btnPerfilHeader.setOnClickListener(v -> abrirPerfilCorrespondente());
        }

        MaterialButton btnBaterPonto = findViewById(R.id.btnBaterPonto);
        if (btnBaterPonto != null) {
            btnBaterPonto.setOnClickListener(v ->
                    startActivity(new Intent(HomeFuncionarioActivity.this, FaceIdActivity.class))
            );
        }
    }

    private void configurarClockLoop() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                atualizarDisplayClock();
                clockHandler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        sincronizarHorarioComServidor();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
            clockHandler.post(clockRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    private void sincronizarHorarioComServidor() {
        long t0 = SystemClock.elapsedRealtime();

        if (BuildConfig.DEBUG) {
            Log.d("HomeFuncionarioActivity", "Iniciando sincronização de horário com o servidor...");
        }

        autenticacaoService.obterHorarioServidor()
                .addOnSuccessListener(result -> {
                    long t1 = SystemClock.elapsedRealtime();
                    long latencyMs = (t1 - t0) / 2;

                    if (result != null && result.getData() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.getData();
                        Object tsVal = data.get("timestampServerMs");
                        if (tsVal instanceof Number) {
                            long serverTs = ((Number) tsVal).longValue();
                            baseServerTimeMs = serverTs + latencyMs;
                            baseMonotonicTimeMs = t1;
                            clockSincronizado = true;
                            ultimaSincronizacaoMonotonicMs = t1;

                            if (BuildConfig.DEBUG) {
                                Log.d("HomeFuncionarioActivity", "Horário sincronizado com sucesso! Latência estimada: " + latencyMs + "ms");
                            }

                            atualizarDisplayClock();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (BuildConfig.DEBUG) {
                        Log.e("HomeFuncionarioActivity", "Falha na sincronização do horário com o servidor: " + e.getMessage());
                    }
                    if (!clockSincronizado && tvHoraAtual != null) {
                        tvHoraAtual.setText(R.string.sincronizando_horario);
                    }
                });
    }

    private void atualizarDisplayClock() {
        if (!clockSincronizado) {
            if (tvHoraAtual != null) {
                tvHoraAtual.setText(R.string.sincronizando_horario);
            }
            return;
        }

        long elapsedMonotonicMs = SystemClock.elapsedRealtime() - baseMonotonicTimeMs;
        long currentServerMs = baseServerTimeMs + elapsedMonotonicMs;

        Date now = new Date(currentServerMs);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", new Locale("pt", "BR"));
        timeFormat.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

        String horaFormatted = timeFormat.format(now);
        String dataFormatted = dateFormat.format(now);

        if (!dataFormatted.isEmpty()) {
            dataFormatted = dataFormatted.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + dataFormatted.substring(1);
        }

        if (tvHoraAtual != null) {
            tvHoraAtual.setText(horaFormatted);
        }

        if (tvDataAtual != null) {
            tvDataAtual.setText(dataFormatted);
        }

        // Re-sincronizar periodicamente se se passaram mais de 5 minutos (300.000 ms)
        long tempoDesdeUltimaSincMs = SystemClock.elapsedRealtime() - ultimaSincronizacaoMonotonicMs;
        if (tempoDesdeUltimaSincMs > 300000) {
            sincronizarHorarioComServidor();
        }
    }

    private void abrirPerfilCorrespondente() {
        String uid = autenticacaoService.getUidAtual();
        if (uid != null) {
            funcionarioDAO.buscarPorUid(uid)
                    .addOnSuccessListener(func -> {
                        if (func != null && "RH".equalsIgnoreCase(func.getPerfil())) {
                            startActivity(new Intent(HomeFuncionarioActivity.this, PerfilRhActivity.class));
                        } else {
                            startActivity(new Intent(HomeFuncionarioActivity.this, PerfilFuncionarioActivity.class));
                        }
                    })
                    .addOnFailureListener(e -> startActivity(new Intent(HomeFuncionarioActivity.this, PerfilFuncionarioActivity.class)));
        } else {
            startActivity(new Intent(HomeFuncionarioActivity.this, PerfilFuncionarioActivity.class));
        }
    }
}
