package com.easyclock.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.model.JornadaTrabalho;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JornadaAdapter extends RecyclerView.Adapter<JornadaAdapter.JornadaViewHolder> {

    public interface OnJornadaAcaoListener {
        void onEditar(JornadaTrabalho jornada);
        void onAlternarStatus(JornadaTrabalho jornada);
    }

    private final List<JornadaTrabalho> jornadas;
    private final OnJornadaAcaoListener listener;

    public JornadaAdapter(OnJornadaAcaoListener listener) {
        this.jornadas = new ArrayList<>();
        this.listener = listener;
    }

    public void setJornadas(List<JornadaTrabalho> novasJornadas) {
        this.jornadas.clear();
        if (novasJornadas != null) {
            this.jornadas.addAll(novasJornadas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JornadaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jornada_trabalho, parent, false);
        return new JornadaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JornadaViewHolder holder, int position) {
        JornadaTrabalho jornada = jornadas.get(position);
        holder.bind(jornada, listener);
    }

    @Override
    public int getItemCount() {
        return jornadas.size();
    }

    static class JornadaViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNomeJornada;
        private final TextView tvStatusJornada;
        private final TextView tvHorariosJornada;
        private final TextView tvDiasSemanaJornada;
        private final TextView tvCargaHorariaJornada;
        private final MaterialButton btnEditarJornada;
        private final MaterialButton btnAlternarStatusJornada;

        public JornadaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomeJornada = itemView.findViewById(R.id.tvNomeJornada);
            tvStatusJornada = itemView.findViewById(R.id.tvStatusJornada);
            tvHorariosJornada = itemView.findViewById(R.id.tvHorariosJornada);
            tvDiasSemanaJornada = itemView.findViewById(R.id.tvDiasSemanaJornada);
            tvCargaHorariaJornada = itemView.findViewById(R.id.tvCargaHorariaJornada);
            btnEditarJornada = itemView.findViewById(R.id.btnEditarJornada);
            btnAlternarStatusJornada = itemView.findViewById(R.id.btnAlternarStatusJornada);
        }

        public void bind(JornadaTrabalho jornada, OnJornadaAcaoListener listener) {
            if (jornada == null) return;

            tvNomeJornada.setText(jornada.getNome() != null ? jornada.getNome() : "Sem nome");

            if (jornada.isAtivo()) {
                tvStatusJornada.setText("Ativa");
                tvStatusJornada.setTextColor(Color.parseColor("#2E7D32"));
                btnAlternarStatusJornada.setText("Desativar");
                btnAlternarStatusJornada.setTextColor(Color.parseColor("#D32F2F"));
            } else {
                tvStatusJornada.setText("Inativa");
                tvStatusJornada.setTextColor(Color.parseColor("#D32F2F"));
                btnAlternarStatusJornada.setText("Ativar");
                btnAlternarStatusJornada.setTextColor(Color.parseColor("#2E7D32"));
            }

            String horarios = String.format(Locale.getDefault(), "%s - %s | %s - %s",
                    jornada.getEntrada() != null ? jornada.getEntrada() : "--:--",
                    jornada.getSaidaAlmoco() != null ? jornada.getSaidaAlmoco() : "--:--",
                    jornada.getRetornoAlmoco() != null ? jornada.getRetornoAlmoco() : "--:--",
                    jornada.getSaida() != null ? jornada.getSaida() : "--:--");
            tvHorariosJornada.setText(horarios);

            tvDiasSemanaJornada.setText("Dias: " + formatarDiasSemana(jornada.getDiasSemana()));

            long horas = jornada.getCargaHorariaMinutos() / 60;
            long mins = jornada.getCargaHorariaMinutos() % 60;
            String cargaStr = String.format(Locale.getDefault(), "Carga Diária: %dh %02dmin (%d min) | Tol: %d min",
                    horas, mins, jornada.getCargaHorariaMinutos(), jornada.getToleranciaMinutos());
            tvCargaHorariaJornada.setText(cargaStr);

            btnEditarJornada.setOnClickListener(v -> {
                if (listener != null) listener.onEditar(jornada);
            });

            btnAlternarStatusJornada.setOnClickListener(v -> {
                if (listener != null) listener.onAlternarStatus(jornada);
            });
        }

        private String formatarDiasSemana(List<Long> dias) {
            if (dias == null || dias.isEmpty()) return "Nenhum dia selecionado";

            StringBuilder sb = new StringBuilder();
            String[] nomes = {"", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
            for (Long d : dias) {
                if (d != null && d >= 1 && d <= 7) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(nomes[d.intValue()]);
                }
            }
            return sb.toString();
        }
    }
}
