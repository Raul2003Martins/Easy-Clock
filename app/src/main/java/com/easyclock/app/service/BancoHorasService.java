package com.easyclock.app.service;

import com.google.firebase.Timestamp;

import java.util.Locale;

public class BancoHorasService {

    public static long calcularMinutosTrabalhados(Timestamp tEntrada, Timestamp tSaidaAlmoco,
                                                  Timestamp tRetornoAlmoco, Timestamp tSaida) {
        if (tEntrada == null || tSaidaAlmoco == null || tRetornoAlmoco == null || tSaida == null) {
            return 0;
        }

        long msTurno1 = tSaidaAlmoco.toDate().getTime() - tEntrada.toDate().getTime();
        long msTurno2 = tSaida.toDate().getTime() - tRetornoAlmoco.toDate().getTime();

        if (msTurno1 < 0) msTurno1 = 0;
        if (msTurno2 < 0) msTurno2 = 0;

        long totalMs = msTurno1 + msTurno2;
        return totalMs / (1000 * 60);
    }

    public static long calcularSaldoMinutos(long minutosTrabalhados, long cargaPrevistaMinutos, long toleranciaMinutos) {
        long diferencaMinutos = minutosTrabalhados - cargaPrevistaMinutos;

        if (toleranciaMinutos > 0 && Math.abs(diferencaMinutos) <= toleranciaMinutos) {
            return 0; // Dentro da margem de tolerância é arredondado a zero
        }

        return diferencaMinutos;
    }

    public static String formatarMinutosParaHoraString(long totalMinutos) {
        long absMinutos = Math.abs(totalMinutos);
        long horas = absMinutos / 60;
        long mins = absMinutos % 60;

        String sinal = "";
        if (totalMinutos > 0) {
            sinal = "+";
        } else if (totalMinutos < 0) {
            sinal = "-";
        }

        return String.format(Locale.getDefault(), "%s%02dh%02dmin", sinal, horas, mins);
    }
}
