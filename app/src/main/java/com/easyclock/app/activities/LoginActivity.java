package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmailLogin;
    private TextInputEditText etSenhaLogin;
    private MaterialButton btnEntrar;
    private TextView tvEsqueciSenha;
    private TextView tvCadastrarLogin;
    private TextView tvCadastrarEmpresaLogin;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();

        inicializarComponentes();
        configurarListeners();
    }

    private void inicializarComponentes() {
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etSenhaLogin = findViewById(R.id.etSenhaLogin);
        btnEntrar = findViewById(R.id.btnEntrar);
        tvEsqueciSenha = findViewById(R.id.tvEsqueciSenha);
        tvCadastrarLogin = findViewById(R.id.tvCadastrarLogin);
        tvCadastrarEmpresaLogin = findViewById(R.id.tvCadastrarEmpresaLogin);
    }

    private void configurarListeners() {
        if (btnEntrar != null) {
            btnEntrar.setOnClickListener(v -> realizarLogin());
        }

        if (tvEsqueciSenha != null) {
            tvEsqueciSenha.setOnClickListener(v ->
                    Toast.makeText(this, "Funcionalidade disponível em breve", Toast.LENGTH_SHORT).show()
            );
        }

        if (tvCadastrarLogin != null) {
            tvCadastrarLogin.setOnClickListener(this::abrirCadastroConta);
        }

        if (tvCadastrarEmpresaLogin != null) {
            tvCadastrarEmpresaLogin.setOnClickListener(this::abrirCadastroEmpresa);
        }
    }

    private void realizarLogin() {
        if (etEmailLogin == null || etSenhaLogin == null) return;

        String email = etEmailLogin.getText() != null ? etEmailLogin.getText().toString().trim() : "";
        String senha = etSenhaLogin.getText() != null ? etSenhaLogin.getText().toString() : "";

        if (email.isEmpty()) {
            etEmailLogin.setError("Informe o e-mail");
            etEmailLogin.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailLogin.setError("E-mail inválido");
            etEmailLogin.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            etSenhaLogin.setError("Informe a senha");
            etSenhaLogin.requestFocus();
            return;
        }

        btnEntrar.setEnabled(false);

        autenticacaoService.login(email, senha)
                .addOnSuccessListener(authResult -> {
                    if (authResult != null && authResult.getUser() != null) {
                        consultarDadosUsuario(authResult.getUser().getUid());
                    } else {
                        btnEntrar.setEnabled(true);
                        exibirMensagem("Erro", "Não foi possível obter os dados do usuário autenticado.");
                    }
                })
                .addOnFailureListener(e -> {
                    btnEntrar.setEnabled(true);
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Falha ao realizar login.";
                    exibirMensagem("Falha na autenticação", msg);
                });
    }

    private void consultarDadosUsuario(String uid) {
        funcionarioDAO.buscarPorUid(uid)
                .addOnSuccessListener(funcionario -> {
                    btnEntrar.setEnabled(true);

                    if (funcionario == null) {
                        exibirMensagem(
                                "Cadastro incompleto",
                                "Documento do usuário não encontrado. Entre em contato com o suporte ou realize seu cadastro."
                        );
                        return;
                    }

                    String status = funcionario.getStatus();
                    String perfil = funcionario.getPerfil();

                    if ("PENDENTE_APROVACAO".equalsIgnoreCase(status)) {
                        exibirMensagem(
                                "Cadastro Pendente",
                                "Seu cadastro foi realizado e está aguardando aprovação do RH."
                        );
                    } else if ("INATIVO".equalsIgnoreCase(status)) {
                        exibirMensagem(
                                "Acesso Bloqueado",
                                "Seu cadastro está inativo. Entre em contato com a empresa."
                        );
                    } else if ("ATIVO".equalsIgnoreCase(status)) {
                        Intent intent = new Intent(LoginActivity.this, HomeFuncionarioActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        exibirMensagem(
                                "Status não autorizado",
                                "Seu cadastro não está autorizado para acesso. Status atual: " + status
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    btnEntrar.setEnabled(true);
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro ao consultar dados.";
                    exibirMensagem("Erro no Firestore", msg);
                });
    }

    private void exibirMensagem(String titulo, String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton("OK", null)
                .show();
    }

    public void abrirCadastroConta(View view) {
        Intent intent = new Intent(this, CadastroActivity.class);
        startActivity(intent);
    }

    public void abrirCadastroEmpresa(View view) {
        Intent intent = new Intent(this, CadastroEmpresaActivity.class);
        startActivity(intent);
    }
}
