package com.easyclock.app.service;

import com.easyclock.app.EasyClockApplication;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutenticacaoService {

    private final FirebaseAuth auth;
    private final FirebaseFunctions functions;
    private final FirebaseFirestore db;

    public AutenticacaoService() {
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance("southamerica-east1");
        db = FirebaseFirestore.getInstance();
    }

    // --------------------------------------------------
    // CADASTRO DE EMPRESA
    // --------------------------------------------------

    @SuppressWarnings("unchecked")
    public Task<Map<String, Object>> cadastrarEmpresa(
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

            return erro("Preencha todos os campos obrigatórios.");
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(emailResponsavel.trim())
                .matches()) {

            return erro("O e-mail informado é inválido.");
        }

        if (senha == null || senha.length() < 6) {
            return erro("A senha deve possuir pelo menos 6 caracteres.");
        }

        if (!senha.equals(confirmarSenha)) {
            return erro("As senhas informadas não coincidem.");
        }

        if (EasyClockApplication.USE_FIREBASE_EMULATORS) {
            Map<String, Object> dados = new HashMap<>();
            dados.put("nomeEmpresa", nomeEmpresa.trim());
            dados.put("cnpj", cnpj.trim());
            dados.put("endereco", endereco.trim());
            dados.put("nomeResponsavel", nomeResponsavel.trim());
            dados.put("emailResponsavel", emailResponsavel.trim());
            dados.put("senha", senha);
            dados.put("confirmarSenha", confirmarSenha);

            return functions
                    .getHttpsCallable("cadastrarEmpresa")
                    .call(dados)
                    .continueWith(task -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            throw task.getException() != null ? task.getException() : new Exception("Erro ao cadastrar empresa.");
                        }
                        return (Map<String, Object>) task.getResult().getData();
                    });
        } else {
            return cadastrarEmpresaSpark(nomeEmpresa, cnpj, endereco, nomeResponsavel, emailResponsavel, senha);
        }
    }

    private Task<Map<String, Object>> cadastrarEmpresaSpark(
            String nomeEmpresa,
            String cnpj,
            String endereco,
            String nomeResponsavel,
            String emailResponsavel,
            String senha) {

        String cnpjClean = cnpj.replaceAll("[^0-9]", "");
        String emailNorm = emailResponsavel.trim().toLowerCase();

        return auth.createUserWithEmailAndPassword(emailNorm, senha)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                        throw task.getException() != null ? task.getException() : new Exception("Falha ao criar conta no Firebase Auth.");
                    }

                    String uid = task.getResult().getUser().getUid();
                    String empresaId = db.collection("empresa").document().getId();
                    String rawInviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
                    String codigoHash = gerarSha256(rawInviteCode);

                    WriteBatch batch = db.batch();

                    Map<String, Object> cnpjData = new HashMap<>();
                    cnpjData.put("empresaId", empresaId);
                    cnpjData.put("criadoEm", FieldValue.serverTimestamp());
                    batch.set(db.collection("cnpjs").document(cnpjClean), cnpjData);

                    Map<String, Object> empresaData = new HashMap<>();
                    empresaData.put("nome", nomeEmpresa.trim());
                    empresaData.put("cnpj", cnpjClean);
                    empresaData.put("endereco", endereco.trim());
                    empresaData.put("latitude", 0.0);
                    empresaData.put("longitude", 0.0);
                    empresaData.put("raioPermitido", 0.0);
                    empresaData.put("responsavelId", uid);
                    empresaData.put("status", "ATIVA");
                    empresaData.put("criadoEm", FieldValue.serverTimestamp());
                    batch.set(db.collection("empresa").document(empresaId), empresaData);

                    Map<String, Object> funcData = new HashMap<>();
                    funcData.put("uid", uid);
                    funcData.put("empresaId", empresaId);
                    funcData.put("nome", nomeResponsavel.trim());
                    funcData.put("email", emailNorm);
                    funcData.put("perfil", "RH");
                    funcData.put("status", "ATIVO");
                    funcData.put("cargo", "");
                    funcData.put("setorId", "");
                    funcData.put("matricula", "");
                    funcData.put("criadoEm", FieldValue.serverTimestamp());
                    batch.set(db.collection("funcionario").document(uid), funcData);

                    String conviteId = db.collection("convitesEmpresa").document().getId();
                    Map<String, Object> conviteData = new HashMap<>();
                    conviteData.put("empresaId", empresaId);
                    conviteData.put("codigoHash", codigoHash);
                    conviteData.put("ativo", true);
                    conviteData.put("quantidadeUsos", 0);
                    conviteData.put("criadoEm", FieldValue.serverTimestamp());
                    batch.set(db.collection("convitesEmpresa").document(conviteId), conviteData);
                    batch.set(db.collection("convitesLookup").document(codigoHash), conviteData);

                    return batch.commit().continueWith(bTask -> {
                        if (!bTask.isSuccessful()) {
                            if (auth.getCurrentUser() != null) {
                                auth.getCurrentUser().delete();
                            }
                            throw bTask.getException() != null ? bTask.getException() : new Exception("Erro ao salvar dados no Firestore.");
                        }

                        Map<String, Object> res = new HashMap<>();
                        res.put("codigoConvite", rawInviteCode);
                        res.put("empresaId", empresaId);
                        res.put("uidResponsavel", uid);
                        return res;
                    });
                });
    }

    // --------------------------------------------------
    // CADASTRO DE FUNCIONÁRIO
    // --------------------------------------------------

    @SuppressWarnings("unchecked")
    public Task<Map<String, Object>> cadastrarFuncionario(
            String nome,
            String email,
            String codigoConvite,
            String senha,
            String confirmarSenha) {

        if (campoVazio(nome) ||
                campoVazio(email) ||
                campoVazio(codigoConvite)) {

            return erro("Preencha todos os campos obrigatórios.");
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim())
                .matches()) {

            return erro("O e-mail informado é inválido.");
        }

        if (senha == null || senha.length() < 6) {
            return erro("A senha deve possuir pelo menos 6 caracteres.");
        }

        if (!senha.equals(confirmarSenha)) {
            return erro("As senhas informadas não coincidem.");
        }

        if (EasyClockApplication.USE_FIREBASE_EMULATORS) {
            Map<String, Object> dados = new HashMap<>();
            dados.put("nome", nome.trim());
            dados.put("email", email.trim());
            dados.put("codigoConvite", codigoConvite.trim());
            dados.put("senha", senha);
            dados.put("confirmarSenha", confirmarSenha);

            return functions
                    .getHttpsCallable("cadastrarFuncionario")
                    .call(dados)
                    .continueWith(task -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            throw task.getException() != null ? task.getException() : new Exception("Erro ao cadastrar funcionário.");
                        }
                        return (Map<String, Object>) task.getResult().getData();
                    });
        } else {
            return cadastrarFuncionarioSpark(nome, email, codigoConvite, senha);
        }
    }

    private Task<Map<String, Object>> cadastrarFuncionarioSpark(
            String nome,
            String email,
            String codigoConvite,
            String senha) {

        String emailNorm = email.trim().toLowerCase();
        String codigoHash = gerarSha256(codigoConvite.trim());

        return db.collection("convitesLookup").document(codigoHash).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        throw new Exception("Código de convite inválido ou não encontrado.");
                    }

                    Boolean ativo = task.getResult().getBoolean("ativo");
                    if (ativo == null || !ativo) {
                        throw new Exception("Código de convite desativado.");
                    }

                    String empresaId = task.getResult().getString("empresaId");
                    if (empresaId == null || empresaId.trim().isEmpty()) {
                        throw new Exception("Empresa do convite não encontrada.");
                    }

                    return auth.createUserWithEmailAndPassword(emailNorm, senha)
                            .continueWithTask(authTask -> {
                                if (!authTask.isSuccessful() || authTask.getResult() == null || authTask.getResult().getUser() == null) {
                                    throw authTask.getException() != null ? authTask.getException() : new Exception("Erro ao criar conta no Firebase Auth.");
                                }

                                String uid = authTask.getResult().getUser().getUid();

                                Map<String, Object> funcData = new HashMap<>();
                                funcData.put("uid", uid);
                                funcData.put("empresaId", empresaId);
                                funcData.put("nome", nome.trim());
                                funcData.put("email", emailNorm);
                                funcData.put("perfil", "FUNCIONARIO");
                                funcData.put("status", "PENDENTE_APROVACAO");
                                funcData.put("codigoHash", codigoHash);
                                funcData.put("cargo", "");
                                funcData.put("setorId", "");
                                funcData.put("matricula", "");
                                funcData.put("criadoEm", FieldValue.serverTimestamp());

                                WriteBatch batch = db.batch();
                                batch.set(db.collection("funcionario").document(uid), funcData);
                                batch.update(db.collection("convitesLookup").document(codigoHash), "quantidadeUsos", FieldValue.increment(1));

                                return batch.commit().continueWith(bTask -> {
                                    if (!bTask.isSuccessful()) {
                                        if (auth.getCurrentUser() != null) {
                                            auth.getCurrentUser().delete();
                                        }
                                        throw bTask.getException() != null ? bTask.getException() : new Exception("Erro ao salvar dados do funcionário.");
                                    }

                                    auth.signOut();

                                    Map<String, Object> res = new HashMap<>();
                                    res.put("uid", uid);
                                    res.put("empresaId", empresaId);
                                    return res;
                                });
                            });
                });
    }

    // --------------------------------------------------
    // GERAR CONVITE EMPRESA (RH)
    // --------------------------------------------------

    @SuppressWarnings("unchecked")
    public Task<Map<String, Object>> gerarConviteEmpresa() {
        if (!estaAutenticado()) {
            return erro("Usuário não autenticado.");
        }

        if (EasyClockApplication.USE_FIREBASE_EMULATORS) {
            return functions
                    .getHttpsCallable("gerarConviteEmpresa")
                    .call()
                    .continueWith(task -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            throw task.getException() != null ? task.getException() : new Exception("Erro ao gerar convite.");
                        }
                        return (Map<String, Object>) task.getResult().getData();
                    });
        } else {
            return gerarConviteEmpresaSpark();
        }
    }

    private Task<Map<String, Object>> gerarConviteEmpresaSpark() {
        String uid = getUidAtual();
        if (uid == null) {
            return erro("Usuário não autenticado.");
        }

        return db.collection("funcionario").document(uid).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        throw new Exception("Cadastro do usuário RH não encontrado.");
                    }

                    String perfil = task.getResult().getString("perfil");
                    String status = task.getResult().getString("status");
                    String empresaId = task.getResult().getString("empresaId");

                    if (!"RH".equalsIgnoreCase(perfil) || !"ATIVO".equalsIgnoreCase(status) || empresaId == null) {
                        throw new Exception("Apenas RH ativo pode gerar convites.");
                    }

                    String rawInviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
                    String codigoHash = gerarSha256(rawInviteCode);

                    String conviteId = db.collection("convitesEmpresa").document().getId();
                    Map<String, Object> conviteData = new HashMap<>();
                    conviteData.put("empresaId", empresaId);
                    conviteData.put("codigoHash", codigoHash);
                    conviteData.put("ativo", true);
                    conviteData.put("quantidadeUsos", 0);
                    conviteData.put("criadoEm", FieldValue.serverTimestamp());

                    WriteBatch batch = db.batch();
                    batch.set(db.collection("convitesEmpresa").document(conviteId), conviteData);
                    batch.set(db.collection("convitesLookup").document(codigoHash), conviteData);

                    return batch.commit().continueWith(bTask -> {
                        if (!bTask.isSuccessful()) {
                            throw bTask.getException() != null ? bTask.getException() : new Exception("Erro ao salvar convite.");
                        }

                        Map<String, Object> res = new HashMap<>();
                        res.put("codigoConvite", rawInviteCode);
                        return res;
                    });
                });
    }

    // --------------------------------------------------
    // APROVAR E RECUSAR FUNCIONÁRIO (RH)
    // --------------------------------------------------

    public Task<Void> aprovarFuncionario(String funcionarioUid) {
        if (!estaAutenticado()) {
            return erro("Usuário não autenticado.");
        }
        if (campoVazio(funcionarioUid)) {
            return erro("UID do funcionário inválido.");
        }

        if (EasyClockApplication.USE_FIREBASE_EMULATORS) {
            Map<String, Object> dados = new HashMap<>();
            dados.put("funcionarioUid", funcionarioUid.trim());
            return functions
                    .getHttpsCallable("aprovarFuncionario")
                    .call(dados)
                    .continueWith(task -> null);
        } else {
            return aprovarFuncionarioSpark(funcionarioUid.trim());
        }
    }

    private Task<Void> aprovarFuncionarioSpark(String funcionarioUid) {
        String rhUid = getUidAtual();
        return db.collection("funcionario").document(rhUid).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        throw new Exception("Cadastro do RH não encontrado.");
                    }

                    String rhEmpresaId = task.getResult().getString("empresaId");
                    String rhPerfil = task.getResult().getString("perfil");
                    String rhStatus = task.getResult().getString("status");

                    if (!"RH".equalsIgnoreCase(rhPerfil) || !"ATIVO".equalsIgnoreCase(rhStatus)) {
                        throw new Exception("Apenas usuários RH ativos podem aprovar funcionários.");
                    }

                    WriteBatch batch = db.batch();
                    batch.update(db.collection("funcionario").document(funcionarioUid), "status", "ATIVO");

                    Map<String, Object> auditData = new HashMap<>();
                    auditData.put("rhUid", rhUid);
                    auditData.put("funcionarioUid", funcionarioUid);
                    auditData.put("empresaId", rhEmpresaId);
                    auditData.put("acao", "APROVAR");
                    auditData.put("criadoEm", FieldValue.serverTimestamp());
                    batch.set(db.collection("auditoriaAprovacoes").document(), auditData);

                    return batch.commit();
                });
    }

    public Task<Void> recusarFuncionario(String funcionarioUid) {
        if (!estaAutenticado()) {
            return erro("Usuário não autenticado.");
        }
        if (campoVazio(funcionarioUid)) {
            return erro("UID do funcionário inválido.");
        }

        if (EasyClockApplication.USE_FIREBASE_EMULATORS) {
            Map<String, Object> dados = new HashMap<>();
            dados.put("funcionarioUid", funcionarioUid.trim());
            return functions
                    .getHttpsCallable("recusarFuncionario")
                    .call(dados)
                    .continueWith(task -> null);
        } else {
            return recusarFuncionarioSpark(funcionarioUid.trim());
        }
    }

    private Task<Void> recusarFuncionarioSpark(String funcionarioUid) {
        String rhUid = getUidAtual();
        return db.collection("funcionario").document(rhUid).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        throw new Exception("Cadastro do RH não encontrado.");
                    }

                    String rhEmpresaId = task.getResult().getString("empresaId");
                    String rhPerfil = task.getResult().getString("perfil");
                    String rhStatus = task.getResult().getString("status");

                    if (!"RH".equalsIgnoreCase(rhPerfil) || !"ATIVO".equalsIgnoreCase(rhStatus)) {
                        throw new Exception("Apenas usuários RH ativos podem recusar funcionários.");
                    }

                    WriteBatch batch = db.batch();
                    batch.update(db.collection("funcionario").document(funcionarioUid), "status", "RECUSADO");

                    Map<String, Object> auditData = new HashMap<>();
                    auditData.put("rhUid", rhUid);
                    auditData.put("funcionarioUid", funcionarioUid);
                    auditData.put("empresaId", rhEmpresaId);
                    auditData.put("acao", "RECUSAR");
                    auditData.put("criadoEm", FieldValue.serverTimestamp());
                    batch.set(db.collection("auditoriaAprovacoes").document(), auditData);

                    return batch.commit();
                });
    }

    // --------------------------------------------------
    // HORÁRIO DO SERVIDOR (SINCRONIZAÇÃO)
    // --------------------------------------------------

    @SuppressWarnings("unchecked")
    public Task<Map<String, Object>> obterHorarioServidor() {
        if (EasyClockApplication.USE_FIREBASE_EMULATORS) {
            return functions
                    .getHttpsCallable("obterHorarioServidor")
                    .call()
                    .continueWith(task -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            throw task.getException() != null ? task.getException() : new Exception("Erro ao obter horário do servidor.");
                        }
                        return (Map<String, Object>) task.getResult().getData();
                    });
        } else {
            Map<String, Object> res = new HashMap<>();
            res.put("timestampServerMs", System.currentTimeMillis());
            return Tasks.forResult(res);
        }
    }

    // --------------------------------------------------
    // LOGIN
    // --------------------------------------------------

    public Task<AuthResult> login(
            String email,
            String senha) {

        if (campoVazio(email) || campoVazio(senha)) {
            return erro("Informe o e-mail e a senha.");
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
            return erro("Informe o e-mail para recuperação.");
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim())
                .matches()) {

            return erro("O e-mail informado é inválido.");
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

    private String gerarSha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return texto;
        }
    }
}
