package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

public class CadastroEmpresaActivity extends AppCompatActivity {

    private TextInputEditText edtNomeEmpresa;
    private TextInputEditText edtCnpjEmpresa;
    private TextInputEditText edtNomeResponsavel;
    private TextInputEditText edtEnderecoEmpresa;
    private TextInputEditText edtEmailEmpresa;
    private TextInputEditText edtSenhaEmpresa;
    private TextInputEditText edtConfirmarSenhaEmpresa;
    private MaterialButton btnCadastrarEmpresa;
    private TextView tvJaTemEmpresa;

    private AutenticacaoService autenticacaoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_empresa);

        autenticacaoService = new AutenticacaoService();

        inicializarComponentes();
        configurarListeners();
    }

    private void inicializarComponentes() {
        edtNomeEmpresa = findViewById(R.id.edtNomeEmpresa);
        edtCnpjEmpresa = findViewById(R.id.edtCnpjEmpresa);
        edtNomeResponsavel = findViewById(R.id.edtNomeResponsavel);
        edtEnderecoEmpresa = findViewById(R.id.edtEnderecoEmpresa);
        edtEmailEmpresa = findViewById(R.id.edtEmailEmpresa);
        edtSenhaEmpresa = findViewById(R.id.edtSenhaEmpresa);
        edtConfirmarSenhaEmpresa = findViewById(R.id.edtConfirmarSenhaEmpresa);
        btnCadastrarEmpresa = findViewById(R.id.btnCadastrarEmpresa);
        tvJaTemEmpresa = findViewById(R.id.tvJaTemEmpresa);
    }

    private void configurarListeners() {
        if (btnCadastrarEmpresa != null) {
            btnCadastrarEmpresa.setOnClickListener(v -> realizarCadastroEmpresa());
        }

        if (tvJaTemEmpresa != null) {
            tvJaTemEmpresa.setOnClickListener(this::irParaLogin);
        }
    }

    private void realizarCadastroEmpresa() {
        if (edtNomeEmpresa == null || edtCnpjEmpresa == null ||
                edtNomeResponsavel == null || edtEnderecoEmpresa == null ||
                edtEmailEmpresa == null || edtSenhaEmpresa == null ||
                edtConfirmarSenhaEmpresa == null) {
            return;
        }

        String nomeEmpresa = edtNomeEmpresa.getText() != null ? edtNomeEmpresa.getText().toString().trim() : "";
        String cnpj = edtCnpjEmpresa.getText() != null ? edtCnpjEmpresa.getText().toString().trim() : "";
        String nomeResponsavel = edtNomeResponsavel.getText() != null ? edtNomeResponsavel.getText().toString().trim() : "";
        String endereco = edtEnderecoEmpresa.getText() != null ? edtEnderecoEmpresa.getText().toString().trim() : "";
        String emailResponsavel = edtEmailEmpresa.getText() != null ? edtEmailEmpresa.getText().toString().trim() : "";
        String senha = edtSenhaEmpresa.getText() != null ? edtSenhaEmpresa.getText().toString() : "";
        String confirmarSenha = edtConfirmarSenhaEmpresa.getText() != null ? edtConfirmarSenhaEmpresa.getText().toString() : "";

        if (nomeEmpresa.isEmpty()) {
            edtNomeEmpresa.setError("Informe o nome da empresa");
            edtNomeEmpresa.requestFocus();
            return;
        }

        if (cnpj.isEmpty()) {
            edtCnpjEmpresa.setError("Informe o CNPJ da empresa");
            edtCnpjEmpresa.requestFocus();
            return;
        }

        String cnpjDigitos = cnpj.replaceAll("[^0-9]", "");
        if (cnpjDigitos.length() != 14) {
            edtCnpjEmpresa.setError("CNPJ deve conter 14 dígitos válidos");
            edtCnpjEmpresa.requestFocus();
            return;
        }

        if (nomeResponsavel.isEmpty()) {
            edtNomeResponsavel.setError("Informe o nome do responsável");
            edtNomeResponsavel.requestFocus();
            return;
        }

        if (endereco.isEmpty()) {
            edtEnderecoEmpresa.setError("Informe o endereço completo da empresa");
            edtEnderecoEmpresa.requestFocus();
            return;
        }

        if (emailResponsavel.isEmpty()) {
            edtEmailEmpresa.setError("Informe o e-mail do responsável");
            edtEmailEmpresa.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailResponsavel).matches()) {
            edtEmailEmpresa.setError("E-mail inválido");
            edtEmailEmpresa.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            edtSenhaEmpresa.setError("Informe a senha");
            edtSenhaEmpresa.requestFocus();
            return;
        }

        String erroSenha = validarComplexidadeSenha(senha);
        if (erroSenha != null) {
            edtSenhaEmpresa.setError(erroSenha);
            edtSenhaEmpresa.requestFocus();
            return;
        }

        if (confirmarSenha.isEmpty()) {
            edtConfirmarSenhaEmpresa.setError("Confirme a senha");
            edtConfirmarSenhaEmpresa.requestFocus();
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            edtConfirmarSenhaEmpresa.setError("As senhas não coincidem");
            edtConfirmarSenhaEmpresa.requestFocus();
            return;
        }

        btnCadastrarEmpresa.setEnabled(false);

        autenticacaoService.cadastrarEmpresa(nomeEmpresa, cnpj, endereco, nomeResponsavel, emailResponsavel, senha, confirmarSenha)
                .addOnSuccessListener(result -> {
                    btnCadastrarEmpresa.setEnabled(true);
                    new AlertDialog.Builder(this)
                            .setTitle("Empresa Cadastrada")
                            .setMessage("Sua empresa e sua conta de responsável foram cadastradas com sucesso!\n\nSua conta já possui perfil administrativo (RH). Faça o login para acessar o painel.")
                            .setPositiveButton("OK", (dialog, which) -> irParaLogin(null))
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    btnCadastrarEmpresa.setEnabled(true);
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro ao cadastrar empresa.";
                    new AlertDialog.Builder(this)
                            .setTitle("Falha no Cadastro")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private String validarComplexidadeSenha(String senha) {
        if (senha.length() < 8) {
            return "A senha deve ter no mínimo 8 caracteres";
        }
        if (!Pattern.compile("[A-Z]").matcher(senha).find()) {
            return "A senha deve conter pelo menos uma letra maiúscula";
        }
        if (!Pattern.compile("[a-z]").matcher(senha).find()) {
            return "A senha deve conter pelo menos uma letra minúscula";
        }
        if (!Pattern.compile("[0-9]").matcher(senha).find()) {
            return "A senha deve conter pelo menos um número";
        }
        if (!Pattern.compile("[^a-zA-Z0-9]").matcher(senha).find()) {
            return "A senha deve conter pelo menos um caractere especial";
        }
        return null;
    }

    public void irParaLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
