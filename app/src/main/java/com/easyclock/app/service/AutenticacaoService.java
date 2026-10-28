package com.easyclock.app.service;

import android.util.Log;

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
        functions = FirebaseFunctions.getInstance("southamerica-east1");
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

        Log.d("AutenticacaoService", "Chamando cadastrarEmpresa: [nomeEmpresa len=" +
                nomeEmpresa.trim().length() +
                ", cnpj len=" + cnpj.trim().length() +
                ", endereco len=" + endereco.trim().length() +
                ", nomeResponsavel len=" + nomeResponsavel.trim().length() +
                ", emailResponsavel len=" + emailResponsavel.trim().length() +
                ", senhaPresente=" + (senha != null) +
                ", confirmarSenhaPresente=" + (confirmarSenha != null) + "]");

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
        dados.put("confirmarSenha", confirmarSenha);

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

        Log.d("AutenticacaoService", "Chamando cadastrarFuncionario: [nome len=" +
                nome.trim().length() +
                ", email len=" + email.trim().length() +
                ", codigoConvitePresente=" + (!codigoConvite.trim().isEmpty()) +
                ", senhaPresente=" + (senha != null) +
                ", confirmarSenhaPresente=" + (confirmarSenha != null) + "]");

        Map<String, Object> dados = new HashMap<>();

        dados.put("nome", nome.trim());
        dados.put("email", email.trim());
        dados.put(
                "codigoConvite",
                codigoConvite.trim()
        );
        dados.put("senha", senha);
        dados.put("confirmarSenha", confirmarSenha);

        // O backend valida o convite, associa a
        // empresa e cria o funcionário pendente.
        return functions
                .getHttpsCallable("cadastrarFuncionario")
                .call(dados);
    }

    // --------------------------------------------------
    // GERAR CONVITE EMPRESA (RH)
    // --------------------------------------------------

    public Task<HttpsCallableResult> gerarConviteEmpresa() {
        if (!estaAutenticado()) {
            return erro("Usuário não autenticado.");
        }

        return functions
                .getHttpsCallable("gerarConviteEmpresa")
                .call();
    }

    // --------------------------------------------------
    // APROVAR E RECUSAR FUNCIONÁRIO (RH)
    // --------------------------------------------------

    public Task<HttpsCallableResult> aprovarFuncionario(String funcionarioUid) {
        if (!estaAutenticado()) {
            return erro("Usuário não autenticado.");
        }
        if (campoVazio(funcionarioUid)) {
            return erro("UID do funcionário inválido.");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("funcionarioUid", funcionarioUid.trim());
        return functions
                .getHttpsCallable("aprovarFuncionario")
                .call(dados);
    }

    public Task<HttpsCallableResult> recusarFuncionario(String funcionarioUid) {
        if (!estaAutenticado()) {
            return erro("Usuário não autenticado.");
        }
        if (campoVazio(funcionarioUid)) {
            return erro("UID do funcionário inválido.");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("funcionarioUid", funcionarioUid.trim());
        return functions
                .getHttpsCallable("recusarFuncionario")
                .call(dados);
    }

    // --------------------------------------------------
    // HORÁRIO DO SERVIDOR (SINCRONIZAÇÃO)
    // --------------------------------------------------

    public Task<HttpsCallableResult> obterHorarioServidor() {
        return functions
                .getHttpsCallable("obterHorarioServidor")
                .call();
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
