package com.easyclock.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easyclock.app.R;
import com.easyclock.app.model.Funcionario;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SolicitacaoCadastroAdapter extends RecyclerView.Adapter<SolicitacaoCadastroAdapter.SolicitacaoViewHolder> {

    public interface OnSolicitacaoAcaoListener {
        void onAprovar(Funcionario funcionario);
        void onRecusar(Funcionario funcionario);
    }

    private final List<Funcionario> listaCompleta = new ArrayList<>();
    private final List<Funcionario> listaFiltrada = new ArrayList<>();
    private final OnSolicitacaoAcaoListener listener;

    public SolicitacaoCadastroAdapter(OnSolicitacaoAcaoListener listener) {
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
    public SolicitacaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitacao_cadastro, parent, false);
        return new SolicitacaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SolicitacaoViewHolder holder, int position) {
        Funcionario f = listaFiltrada.get(position);
        holder.bind(f, listener);
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    static class SolicitacaoViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvNome;
        private final TextView tvEmail;
        private final MaterialButton btnAprovar;
        private final MaterialButton btnRecusar;

        public SolicitacaoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvNomeSolicitante);
            tvEmail = itemView.findViewById(R.id.tvEmailSolicitante);
            btnAprovar = itemView.findViewById(R.id.btnAprovarSolicitacao);
            btnRecusar = itemView.findViewById(R.id.btnRecusarSolicitacao);
        }

        public void bind(Funcionario f, OnSolicitacaoAcaoListener listener) {
            if (tvNome != null) {
                tvNome.setText(f.getNome() != null ? f.getNome() : "Sem nome");
            }

            if (tvEmail != null) {
                tvEmail.setText(f.getEmail() != null ? f.getEmail() : "");
            }

            if (btnAprovar != null) {
                btnAprovar.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAprovar(f);
                    }
                });
            }

            if (btnRecusar != null) {
                btnRecusar.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRecusar(f);
                    }
                });
            }
        }
    }
}
