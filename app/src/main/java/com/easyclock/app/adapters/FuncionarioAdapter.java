package com.easyclock.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.model.Funcionario;

import java.util.ArrayList;
import java.util.List;

public class FuncionarioAdapter extends RecyclerView.Adapter<FuncionarioAdapter.FuncionarioViewHolder> {

    public interface OnFuncionarioClickListener {
        void onFuncionarioClick(Funcionario funcionario);
    }

    private final List<Funcionario> listaCompleta = new ArrayList<>();
    private final List<Funcionario> listaFiltrada = new ArrayList<>();
    private final OnFuncionarioClickListener listener;

    public FuncionarioAdapter(OnFuncionarioClickListener listener) {
        this.listener = listener;
    }

    public void setLista(List<Funcionario> novaLista) {
        this.listaCompleta.clear();
        if (novaLista != null) {
            this.listaCompleta.addAll(novaLista);
        }
        filtrar("");
    }

    public void filtrar(String texto) {
        listaFiltrada.clear();
        String termo = texto != null ? texto.toLowerCase().trim() : "";

        if (termo.isEmpty()) {
            listaFiltrada.addAll(listaCompleta);
        } else {
            for (Funcionario f : listaCompleta) {
                if (f.getNome() != null && f.getNome().toLowerCase().contains(termo)) {
                    listaFiltrada.add(f);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FuncionarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_colaborador, parent, false);
        return new FuncionarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FuncionarioViewHolder holder, int position) {
        Funcionario f = listaFiltrada.get(position);
        holder.bind(f, listener);
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    static class FuncionarioViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNome;
        private final TextView tvCargo;
        private final TextView tvSaldo;

        public FuncionarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvNomeColaborador);
            tvCargo = itemView.findViewById(R.id.tvCargoColaborador);
            tvSaldo = itemView.findViewById(R.id.tvSaldoHorasColaborador);
        }

        public void bind(Funcionario f, OnFuncionarioClickListener listener) {
            if (tvNome != null) {
                tvNome.setText(f.getNome() != null ? f.getNome() : "Sem nome");
            }

            if (tvCargo != null) {
                String cargoText = f.getCargo() != null && !f.getCargo().trim().isEmpty()
                        ? f.getCargo() : "Cargo não definido";
                if (f.getPerfil() != null && "RH".equalsIgnoreCase(f.getPerfil())) {
                    cargoText += " (RH)";
                }
                tvCargo.setText(cargoText);
            }

            if (tvSaldo != null) {
                tvSaldo.setText(R.string.banco_horas_indisponivel);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFuncionarioClick(f);
                }
            });
        }
    }
}
