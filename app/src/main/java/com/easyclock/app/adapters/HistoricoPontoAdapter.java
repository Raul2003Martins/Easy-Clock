package com.easyclock.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.model.Ponto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import java.util.Map;
import java.util.TimeZone;

public class HistoricoPontoAdapter extends RecyclerView.Adapter<HistoricoPontoAdapter.HistoricoViewHolder> {

    public static class DiaPontoSummary {
        public String dataReferencia;
        public String horaEntrada = "--:--";
        public String horaSaidaAlmoco = "--:--";
        public String horaRetornoAlmoco = "--:--";
        public String horaSaida = "--:--";
        public String statusExpediente = "Em andamento";
        public String infoJornadaSnapshot = "Sem snapshot";
    }

    private final List<DiaPontoSummary> resumosDias;

    public HistoricoPontoAdapter() {
        this.resumosDias = new ArrayList<>();
    }

    public void setPontos(List<Ponto> listaPontos) {
        this.resumosDias.clear();
        if (listaPontos != null && !listaPontos.isEmpty()) {
            Map<String, DiaPontoSummary> mapa = new LinkedHashMap<>();
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));
            sdfTime.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));

            for (Ponto p : listaPontos) {
                if (p == null || p.getDataReferencia() == null) continue;

                String dataRef = p.getDataReferencia();
                DiaPontoSummary sum = mapa.get(dataRef);
                if (sum == null) {
                    sum = new DiaPontoSummary();
                    sum.dataReferencia = dataRef;
                    mapa.put(dataRef, sum);
                }

                String horaFormatted = "--:--";
                if (p.getDataHora() != null) {
                    horaFormatted = sdfTime.format(p.getDataHora().toDate());
                }

                if ("ENTRADA".equalsIgnoreCase(p.getTipoPonto())) {
                    sum.horaEntrada = horaFormatted;
                } else if ("SAIDA_ALMOCO".equalsIgnoreCase(p.getTipoPonto())) {
                    sum.horaSaidaAlmoco = horaFormatted;
                } else if ("RETORNO_ALMOCO".equalsIgnoreCase(p.getTipoPonto())) {
                    sum.horaRetornoAlmoco = horaFormatted;
                } else if ("SAIDA".equalsIgnoreCase(p.getTipoPonto())) {
                    sum.horaSaida = horaFormatted;
                }

                if (p.getJornadaSnapshot() != null && !p.getJornadaSnapshot().isEmpty()) {
                    Object ent = p.getJornadaSnapshot().get("entrada");
                    Object sai = p.getJornadaSnapshot().get("saida");
                    Object carga = p.getJornadaSnapshot().get("cargaHorariaMinutos");
                    sum.infoJornadaSnapshot = String.format("Jornada Prevista: %s - %s (%s min)",
                            ent != null ? ent : "--:--",
                            sai != null ? sai : "--:--",
                            carga != null ? carga : "--");
                }
            }

            for (DiaPontoSummary sum : mapa.values()) {
                boolean temE = !"--:--".equals(sum.horaEntrada);
                boolean temSA = !"--:--".equals(sum.horaSaidaAlmoco);
                boolean temRA = !"--:--".equals(sum.horaRetornoAlmoco);
                boolean temS = !"--:--".equals(sum.horaSaida);

                if (temE && temSA && temRA && temS) {
                    sum.statusExpediente = "Concluído";
                } else if (temE && (temSA || temRA)) {
                    sum.statusExpediente = "Em andamento";
                } else if (temE) {
                    sum.statusExpediente = "Em andamento";
                } else {
                    sum.statusExpediente = "Incompleto";
                }

                resumosDias.add(sum);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historico_ponto_dia, parent, false);
        return new HistoricoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoricoViewHolder holder, int position) {
        DiaPontoSummary sum = resumosDias.get(position);
        holder.bind(sum);
    }

    @Override
    public int getItemCount() {
        return resumosDias.size();
    }

    static class HistoricoViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDataHistorico;
        private final TextView tvSituacaoHistorico;
        private final TextView tvHorariosHistorico;
        private final TextView tvInfoJornadaHistorico;

        public HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDataHistorico = itemView.findViewById(R.id.tvDataHistorico);
            tvSituacaoHistorico = itemView.findViewById(R.id.tvSituacaoHistorico);
            tvHorariosHistorico = itemView.findViewById(R.id.tvHorariosHistorico);
            tvInfoJornadaHistorico = itemView.findViewById(R.id.tvInfoJornadaHistorico);
        }

        public void bind(DiaPontoSummary sum) {
            if (sum == null) return;

            tvDataHistorico.setText(sum.dataReferencia);
            tvSituacaoHistorico.setText(sum.statusExpediente);

            if ("Concluído".equalsIgnoreCase(sum.statusExpediente)) {
                tvSituacaoHistorico.setTextColor(Color.parseColor("#2E7D32"));
            } else if ("Em andamento".equalsIgnoreCase(sum.statusExpediente)) {
                tvSituacaoHistorico.setTextColor(Color.parseColor("#1976D2"));
            } else {
                tvSituacaoHistorico.setTextColor(Color.parseColor("#D32F2F"));
            }

            String horariosText = String.format(Locale.getDefault(),
                    "E: %s | SA: %s | RA: %s | S: %s",
                    sum.horaEntrada, sum.horaSaidaAlmoco, sum.horaRetornoAlmoco, sum.horaSaida);
            tvHorariosHistorico.setText(horariosText);

            tvInfoJornadaHistorico.setText(sum.infoJornadaSnapshot);
        }
    }
}
