package com.easyclock.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.model.Funcionario;
import com.easyclock.app.model.JornadaTrabalho;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VincularJornadaAdapter extends RecyclerView.Adapter<VincularJornadaAdapter.VincularViewHolder> {

    public interface OnSalvarVinculoListener {
        void onSalvarVinculo(Funcionario funcionario, JornadaTrabalho jornadaSelecionada);
    }

    private final List<Funcionario> funcionarios;
    private final List<JornadaTrabalho> jornadasAtivas;
    private final OnSalvarVinculoListener listener;

    public VincularJornadaAdapter(OnSalvarVinculoListener listener) {
        this.funcionarios = new ArrayList<>();
        this.jornadasAtivas = new ArrayList<>();
        this.listener = listener;
    }

    public void setData(List<Funcionario> novosFuncionarios, List<JornadaTrabalho> novasJornadasAtivas) {
        this.funcionarios.clear();
        if (novosFuncionarios != null) {
            this.funcionarios.addAll(novosFuncionarios);
        }

        this.jornadasAtivas.clear();
        if (novasJornadasAtivas != null) {
            this.jornadasAtivas.addAll(novasJornadasAtivas);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VincularViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vincular_jornada, parent, false);
        return new VincularViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VincularViewHolder holder, int position) {
        Funcionario func = funcionarios.get(position);
        holder.bind(func, jornadasAtivas, listener);
    }

    @Override
    public int getItemCount() {
        return funcionarios.size();
    }

    static class VincularViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNomeFuncionario;
        private final TextView tvEmailFuncionario;
        private final Spinner spinnerJornada;
        private final MaterialButton btnSalvar;

        public VincularViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomeFuncionario = itemView.findViewById(R.id.tvNomeFuncionarioVincular);
            tvEmailFuncionario = itemView.findViewById(R.id.tvEmailFuncionarioVincular);
            spinnerJornada = itemView.findViewById(R.id.spinnerJornadaAtribuida);
            btnSalvar = itemView.findViewById(R.id.btnSalvarVinculoJornada);
        }

        public void bind(Funcionario func, List<JornadaTrabalho> jornadasAtivas, OnSalvarVinculoListener listener) {
            if (func == null) return;

            tvNomeFuncionario.setText(func.getNome() != null ? func.getNome() : "Sem nome");
            tvEmailFuncionario.setText(func.getEmail() != null ? func.getEmail() : "Sem e-mail");

            List<String> nomesJornadas = new ArrayList<>();
            nomesJornadas.add("Nenhuma Jornada Atribuída");
            int selectedIndex = 0;

            if (jornadasAtivas != null) {
                for (int i = 0; i < jornadasAtivas.size(); i++) {
                    JornadaTrabalho j = jornadasAtivas.get(i);
                    nomesJornadas.add(j.getNome());
                    if (func.getJornadaId() != null && func.getJornadaId().equals(j.getId())) {
                        selectedIndex = i + 1;
                    }
                }
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    nomesJornadas
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerJornada.setAdapter(spinnerAdapter);
            spinnerJornada.setSelection(selectedIndex);

            btnSalvar.setOnClickListener(v -> {
                int pos = spinnerJornada.getSelectedItemPosition();
                JornadaTrabalho jornadaSel = null;
                if (pos > 0 && jornadasAtivas != null && (pos - 1) < jornadasAtivas.size()) {
                    jornadaSel = jornadasAtivas.get(pos - 1);
                }
                if (listener != null) {
                    listener.onSalvarVinculo(func, jornadaSel);
                }
            });
        }
    }
}
