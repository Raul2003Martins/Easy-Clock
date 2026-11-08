package com.easyclock.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.model.HistoricoBancoHoras;
import com.easyclock.app.service.BancoHorasService;

import java.util.List;

public class HistoricoBancoHorasAdapter extends RecyclerView.Adapter<HistoricoBancoHorasAdapter.ViewHolder> {

    private final List<HistoricoBancoHoras> lista;

    public HistoricoBancoHorasAdapter(List<HistoricoBancoHoras> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historico_banco_horas, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoricoBancoHoras item = lista.get(position);
        if (item == null) return;

        String titulo = "AJUSTE_MANUAL_RH".equalsIgnoreCase(item.getTipo()) ?
                "Ajuste Manual pelo RH" : "Fechamento de Expediente (" + (item.getDataReferencia() != null ? item.getDataReferencia() : "") + ")";

        holder.tvTituloItemBanco.setText(titulo);

        long minsSaldo = item.getMinutosSaldo();
        String saldoFormatted = BancoHorasService.formatarMinutosParaHoraString(minsSaldo);
        holder.tvSaldoItemBanco.setText(saldoFormatted);

        if (minsSaldo > 0) {
            holder.tvSaldoItemBanco.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
        } else if (minsSaldo < 0) {
            holder.tvSaldoItemBanco.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.button_danger));
        } else {
            holder.tvSaldoItemBanco.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
        }

        holder.tvJustificativaItemBanco.setText(item.getJustificativa() != null ? item.getJustificativa() : "--");
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTituloItemBanco;
        TextView tvSaldoItemBanco;
        TextView tvJustificativaItemBanco;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTituloItemBanco = itemView.findViewById(R.id.tvTituloItemBanco);
            tvSaldoItemBanco = itemView.findViewById(R.id.tvSaldoItemBanco);
            tvJustificativaItemBanco = itemView.findViewById(R.id.tvJustificativaItemBanco);
        }
    }
}
