package com.easyclock.app.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class AutenticacaoService {

    private final FirebaseAuth auth;
    private final FirebaseFunctions functions;

    public AutenticacaoService() {
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance();
    }

    // --------------------------------------------------
    // CADASTRO DE EMPRESA
    // --------------------------------------------------

    public Task<HttpsCallableResult> cadastrarEmpresa(
            String nomeEmpresa,
            String cnpj,
            String endereco,
            String nomeResponsavel,
            String emailResponsavel,
            String senha,
            String confirmarSenha) {

        if (campoVazio(nomeEmpresa) ||
                campoVazio(cnpj) ||
                campoVazio(endereco) ||
                campoVazio(nomeResponsavel) ||
                campoVazio(emailResponsavel)) {

            return erro(
                    "Preencha todos os campos obrigatórios."
            );
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(emailResponsavel.trim())
                .matches()) {

            return erro(
                    "O e-mail informado é inválido."
            );
        }

        if (senha == null || senha.length() < 6) {
            return erro(
                    "A senha deve possuir pelo menos 6 caracteres."
            );
        }

        if (!senha.equals(confirmarSenha)) {
            return erro(
                    "As senhas informadas não coincidem."
            );
        }

        Map<String, Object> dados = new HashMap<>();

        dados.put("nomeEmpresa", nomeEmpresa.trim());
        dados.put("cnpj", cnpj.trim());
        dados.put("endereco", endereco.trim());
        dados.put(
                "nomeResponsavel",
                nomeResponsavel.trim()
        );
        dados.put(
                "emailResponsavel",
                emailResponsavel.trim()
        );
        dados.put("senha", senha);

        // O backend cria a empresa e o primeiro RH.
        // Perfil e status não são enviados pelo cliente.
        return functions
                .getHttpsCallable("cadastrarEmpresa")
                .call(dados);
    }

    // --------------------------------------------------
    // CADASTRO DE FUNCIONÁRIO
    // --------------------------------------------------

    public Task<HttpsCallableResult> cadastrarFuncionario(
            String nome,
            String email,
            String codigoConvite,
            String senha,
            String confirmarSenha) {

        if (campoVazio(nome) ||
                campoVazio(email) ||
                campoVazio(codigoConvite)) {

            return erro(
                    "Preencha todos os campos obrigatórios."
            );
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim())
                .matches()) {

            return erro(
                    "O e-mail informado é inválido."
            );
        }

        if (senha == null || senha.length() < 6) {
            return erro(
                    "A senha deve possuir pelo menos 6 caracteres."
            );
        }

        if (!senha.equals(confirmarSenha)) {
            return erro(
                    "As senhas informadas não coincidem."
            );
        }

        Map<String, Object> dados = new HashMap<>();

        dados.put("nome", nome.trim());
        dados.put("email", email.trim());
        dados.put(
                "codigoConvite",
                codigoConvite.trim()
        );
        dados.put("senha", senha);

        // O backend valida o convite, associa a
        // empresa e cria o funcionário pendente.
        return functions
                .getHttpsCallable("cadastrarFuncionario")
                .call(dados);
    }

    // --------------------------------------------------
    // LOGIN
    // --------------------------------------------------

    public Task<AuthResult> login(
            String email,
            String senha) {

        if (campoVazio(email) || campoVazio(senha)) {
            return erro(
                    "Informe o e-mail e a senha."
            );
        }

        return auth.signInWithEmailAndPassword(
                email.trim(),
                senha
        );
    }

    // --------------------------------------------------
    // RECUPERAÇÃO DE SENHA
    // --------------------------------------------------

    public Task<Void> recuperarSenha(String email) {

        if (campoVazio(email)) {
            return erro(
                    "Informe o e-mail para recuperação."
            );
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim())
                .matches()) {

            return erro(
                    "O e-mail informado é inválido."
            );
        }

        return auth.sendPasswordResetEmail(
                email.trim()
        );
    }

    // --------------------------------------------------
    // USUÁRIO ATUAL
    // --------------------------------------------------

    public FirebaseUser getUsuarioAtual() {
        return auth.getCurrentUser();
    }

    public boolean estaAutenticado() {
        return auth.getCurrentUser() != null;
    }

    public String getUidAtual() {

        FirebaseUser usuario = auth.getCurrentUser();

        if (usuario == null) {
            return null;
        }

        return usuario.getUid();
    }

    // --------------------------------------------------
    // LOGOUT
    // --------------------------------------------------

    public void logout() {
        auth.signOut();
    }

    // --------------------------------------------------
    // MÉTODOS AUXILIARES
    // --------------------------------------------------

    private boolean campoVazio(String valor) {
        return valor == null ||
                valor.trim().isEmpty();
    }

    private <T> Task<T> erro(String mensagem) {
        return Tasks.forException(
                new IllegalArgumentException(mensagem)
        );
    }
}
