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

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText edtNomeFuncionario;
    private TextInputEditText edtEmailFuncionario;
    private TextInputEditText edtCodigoEmpresa;
    private TextView tvNomeEmpresaPreview;
    private TextInputEditText edtSenhaFuncionario;
    private TextInputEditText edtConfirmarSenhaFuncionario;
    private MaterialButton btnCadastrarFuncionario;
    private TextView tvJaTemConta;

    private AutenticacaoService autenticacaoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        autenticacaoService = new AutenticacaoService();

        inicializarComponentes();
        configurarListeners();
    }

    private void inicializarComponentes() {
        edtNomeFuncionario = findViewById(R.id.edtNomeFuncionario);
        edtEmailFuncionario = findViewById(R.id.edtEmailFuncionario);
        edtCodigoEmpresa = findViewById(R.id.edtCodigoEmpresa);
        tvNomeEmpresaPreview = findViewById(R.id.tvNomeEmpresaPreview);
        edtSenhaFuncionario = findViewById(R.id.edtSenhaFuncionario);
        edtConfirmarSenhaFuncionario = findViewById(R.id.edtConfirmarSenhaFuncionario);
        btnCadastrarFuncionario = findViewById(R.id.btnCadastrarFuncionario);
        tvJaTemConta = findViewById(R.id.tvJaTemConta);

        if (tvNomeEmpresaPreview != null) {
            tvNomeEmpresaPreview.setVisibility(View.GONE);
        }
    }

    private void configurarListeners() {
        if (btnCadastrarFuncionario != null) {
            btnCadastrarFuncionario.setOnClickListener(v -> realizarCadastroFuncionario());
        }

        if (tvJaTemConta != null) {
            tvJaTemConta.setOnClickListener(this::irParaLogin);
        }
    }

    private void realizarCadastroFuncionario() {
        if (edtNomeFuncionario == null || edtEmailFuncionario == null ||
                edtCodigoEmpresa == null || edtSenhaFuncionario == null ||
                edtConfirmarSenhaFuncionario == null) {
            return;
        }

        String nome = edtNomeFuncionario.getText() != null ? edtNomeFuncionario.getText().toString().trim() : "";
        String email = edtEmailFuncionario.getText() != null ? edtEmailFuncionario.getText().toString().trim() : "";
        String codigoConvite = edtCodigoEmpresa.getText() != null ? edtCodigoEmpresa.getText().toString().trim() : "";
        String senha = edtSenhaFuncionario.getText() != null ? edtSenhaFuncionario.getText().toString() : "";
        String confirmarSenha = edtConfirmarSenhaFuncionario.getText() != null ? edtConfirmarSenhaFuncionario.getText().toString() : "";

        if (nome.isEmpty()) {
            edtNomeFuncionario.setError("Informe o nome completo");
            edtNomeFuncionario.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            edtEmailFuncionario.setError("Informe o e-mail");
            edtEmailFuncionario.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmailFuncionario.setError("E-mail inválido");
            edtEmailFuncionario.requestFocus();
            return;
        }

        if (codigoConvite.isEmpty()) {
            edtCodigoEmpresa.setError("Informe o código de convite da empresa");
            edtCodigoEmpresa.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            edtSenhaFuncionario.setError("Informe a senha");
            edtSenhaFuncionario.requestFocus();
            return;
        }

        String erroSenha = validarComplexidadeSenha(senha);
        if (erroSenha != null) {
            edtSenhaFuncionario.setError(erroSenha);
            edtSenhaFuncionario.requestFocus();
            return;
        }

        if (confirmarSenha.isEmpty()) {
            edtConfirmarSenhaFuncionario.setError("Confirme a senha");
            edtConfirmarSenhaFuncionario.requestFocus();
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            edtConfirmarSenhaFuncionario.setError("As senhas não coincidem");
            edtConfirmarSenhaFuncionario.requestFocus();
            return;
        }

        btnCadastrarFuncionario.setEnabled(false);

        autenticacaoService.cadastrarFuncionario(nome, email, codigoConvite, senha, confirmarSenha)
                .addOnSuccessListener(result -> {
                    btnCadastrarFuncionario.setEnabled(true);
                    new AlertDialog.Builder(this)
                            .setTitle("Cadastro Realizado")
                            .setMessage("Seu cadastro foi realizado com sucesso!\n\nSeu perfil está aguardando aprovação do RH da empresa. Por favor, aguarde antes de realizar o login.")
                            .setPositiveButton("OK", (dialog, which) -> irParaLogin(null))
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    btnCadastrarFuncionario.setEnabled(true);
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro ao realizar cadastro.";
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
