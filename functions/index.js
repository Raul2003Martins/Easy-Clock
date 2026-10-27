const {setGlobalOptions} = require("firebase-functions/v2");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({
  region: "southamerica-east1",
  maxInstances: 10,
});

/**
 * Validates if value is a string within allowed length bounds.
 * @param {unknown} val Value to check.
 * @param {number} maxLen Maximum length allowed.
 * @param {number} [minLen=1] Minimum length allowed.
 * @return {boolean} True if valid string.
 */
function validarString(val, maxLen, minLen = 1) {
  if (typeof val !== "string") return false;
  const trimmed = val.trim();
  return trimmed.length >= minLen && trimmed.length <= maxLen;
}

/**
 * Validates Brazilian CNPJ format and check digits.
 * @param {string} cnpj Digits-only CNPJ string.
 * @return {boolean} True if valid.
 */
function validarCNPJ(cnpj) {
  if (!cnpj || cnpj.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(cnpj)) return false;

  let tamanho = cnpj.length - 2;
  let numeros = cnpj.substring(0, tamanho);
  const digitos = cnpj.substring(tamanho);
  let soma = 0;
  let pos = tamanho - 7;

  for (let i = tamanho; i >= 1; i--) {
    soma += parseInt(numeros.charAt(tamanho - i), 10) * pos--;
    if (pos < 2) pos = 9;
  }

  let resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
  if (resultado !== parseInt(digitos.charAt(0), 10)) return false;

  tamanho = tamanho + 1;
  numeros = cnpj.substring(0, tamanho);
  soma = 0;
  pos = tamanho - 7;

  for (let i = tamanho; i >= 1; i--) {
    soma += parseInt(numeros.charAt(tamanho - i), 10) * pos--;
    if (pos < 2) pos = 9;
  }

  resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
  if (resultado !== parseInt(digitos.charAt(1), 10)) return false;

  return true;
}

/**
 * Validates email format and length.
 * @param {string} email Email string to validate.
 * @return {boolean} True if valid.
 */
function validarEmail(email) {
  if (!validarString(email, 254, 5)) return false;
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}

/**
 * Validates password policy (8-128 chars, upper, lower, digit, special).
 * @param {string} senha Password string to validate.
 * @return {boolean} True if password meets policy.
 */
function validarSenha(senha) {
  if (typeof senha !== "string") return false;
  if (senha.length < 8 || senha.length > 128) return false;
  const temMaiuscula = /[A-Z]/.test(senha);
  const temMinuscula = /[a-z]/.test(senha);
  const temNumero = /[0-9]/.test(senha);
  const temEspecial = /[^a-zA-Z0-9]/.test(senha);
  return temMaiuscula && temMinuscula && temNumero && temEspecial;
}

/**
 * Verifies App Check verification status.
 * @param {Object} request Callable request object.
 */
function verificarAppCheck(request) {
  if (process.env.FUNCTIONS_EMULATOR !== "true" && !request.app) {
    throw new HttpsError(
        "unauthenticated",
        "A requisição deve ser enviada por " +
        "um app verificado via App Check.",
    );
  }
}

/**
 * Throttles registration attempts by key (e.g., email hash).
 * @param {string} key Identifier key for rate limiting.
 * @param {number} maxAttempts Max allowed attempts.
 * @param {number} windowMs Window duration in milliseconds.
 */
async function verificarRateLimit(key, maxAttempts = 5, windowMs = 900000) {
  const keyHash = crypto.createHash("sha256").update(key).digest("hex");
  const docRef = db.collection("rateLimits").doc(keyHash);
  const now = Date.now();

  await db.runTransaction(async (transaction) => {
    const docSnap = await transaction.get(docRef);
    if (!docSnap.exists) {
      transaction.set(docRef, {
        attempts: 1,
        resetAt: now + windowMs,
      });
      return;
    }

    const data = docSnap.data();
    if (now > data.resetAt) {
      transaction.set(docRef, {
        attempts: 1,
        resetAt: now + windowMs,
      });
      return;
    }

    if (data.attempts >= maxAttempts) {
      throw new HttpsError(
          "resource-exhausted",
          "Muitas tentativas de cadastro. " +
          "Por favor, aguarde alguns minutos.",
      );
    }

    transaction.update(docRef, {
      attempts: data.attempts + 1,
    });
  });
}

/**
 * Callable function to register a new company and its initial RH user.
 */
exports.cadastrarEmpresa = onCall(async (request) => {
  verificarAppCheck(request);

  const {
    nomeEmpresa,
    cnpj,
    endereco,
    nomeResponsavel,
    emailResponsavel,
    senha,
    confirmarSenha,
  } = request.data || {};

  if (
    !validarString(nomeEmpresa, 100, 2) ||
    !validarString(cnpj, 30, 14) ||
    !validarString(endereco, 250, 5) ||
    !validarString(nomeResponsavel, 100, 2) ||
    !validarString(emailResponsavel, 254, 5) ||
    typeof senha !== "string" ||
    typeof confirmarSenha !== "string"
  ) {
    throw new HttpsError(
        "invalid-argument",
        "Todos os campos são obrigatórios e devem possuir tamanhos válidos.",
    );
  }

  if (senha !== confirmarSenha) {
    throw new HttpsError(
        "invalid-argument",
        "A senha e a confirmação de senha não coincidem.",
    );
  }

  const emailNorm = emailResponsavel.trim().toLowerCase();
  if (!validarEmail(emailNorm)) {
    throw new HttpsError(
        "invalid-argument",
        "O e-mail informado é inválido.",
    );
  }

  const cnpjClean = cnpj.replace(/\D/g, "");
  if (!validarCNPJ(cnpjClean)) {
    throw new HttpsError(
        "invalid-argument",
        "O CNPJ informado é inválido.",
    );
  }

  if (!validarSenha(senha)) {
    throw new HttpsError(
        "invalid-argument",
        "A senha não atende aos requisitos de segurança (mínimo 8 caracteres," +
        " com maiúscula, minúscula, número e caractere especial).",
    );
  }

  await verificarRateLimit(`empresa:${emailNorm}`);

  try {
    await admin.auth().getUserByEmail(emailNorm);
    throw new HttpsError(
        "already-exists",
        "O e-mail informado já está cadastrado.",
    );
  } catch (authCheckErr) {
    if (authCheckErr instanceof HttpsError) {
      throw authCheckErr;
    }
    if (authCheckErr.code !== "auth/user-not-found") {
      logger.error("Erro ao verificar e-mail em Auth:", authCheckErr);
      throw new HttpsError(
          "internal",
          "Erro interno ao validar e-mail do responsável.",
      );
    }
  }

  let userRecord;
  try {
    userRecord = await admin.auth().createUser({
      email: emailNorm,
      password: senha,
      displayName: nomeResponsavel.trim(),
    });
  } catch (createUserErr) {
    if (createUserErr.code === "auth/email-already-exists") {
      throw new HttpsError(
          "already-exists",
          "O e-mail informado já está cadastrado.",
      );
    }
    logger.error("Erro ao criar usuário no Auth:", createUserErr);
    throw new HttpsError(
        "internal",
        "Erro ao criar conta do responsável no serviço de autenticação.",
    );
  }

  const empresaRef = db.collection("empresa").doc();
  const empresaId = empresaRef.id;

  const rawInviteBytes = crypto.randomBytes(16);
  const rawInviteCode = rawInviteBytes.toString("hex").toUpperCase();
  const codigoHash = crypto
      .createHash("sha256")
      .update(rawInviteCode)
      .digest("hex");

  const conviteRef = db.collection("convitesEmpresa").doc();
  const cnpjLockRef = db.collection("cnpjs").doc(cnpjClean);
  const funcionarioRef = db.collection("funcionarios").doc(userRecord.uid);

  try {
    await db.runTransaction(async (transaction) => {
      const cnpjDoc = await transaction.get(cnpjLockRef);
      if (cnpjDoc.exists) {
        throw new HttpsError(
            "already-exists",
            "O CNPJ informado já está cadastrado no sistema.",
        );
      }

      const now = admin.firestore.FieldValue.serverTimestamp();

      transaction.set(cnpjLockRef, {
        empresaId: empresaId,
        criadoEm: now,
      });

      transaction.set(empresaRef, {
        nome: nomeEmpresa.trim(),
        cnpj: cnpjClean,
        endereco: endereco.trim(),
        latitude: 0,
        longitude: 0,
        raioPermitido: 0,
        responsavelId: userRecord.uid,
        status: "ATIVA",
        criadoEm: now,
      });

      transaction.set(funcionarioRef, {
        uid: userRecord.uid,
        empresaId: empresaId,
        nome: nomeResponsavel.trim(),
        email: emailNorm,
        perfil: "RH",
        status: "ATIVO",
        cargo: null,
        setorId: null,
        matricula: null,
        dataAdmissao: null,
        faceId: null,
        criadoEm: now,
      });

      transaction.set(conviteRef, {
        empresaId: empresaId,
        codigoHash: codigoHash,
        ativo: true,
        criadoEm: now,
        expiraEm: null,
        limiteUsos: null,
        quantidadeUsos: 0,
      });
    });
  } catch (txErr) {
    try {
      await admin.auth().deleteUser(userRecord.uid);
    } catch (delErr) {
      logger.error("CRITICAL: Falha ao deletar Auth na compensação:", {
        uid: userRecord.uid,
        error: delErr,
      });
    }

    if (txErr instanceof HttpsError) {
      throw txErr;
    }
    logger.error("Erro na transação Firestore para cadastrarEmpresa:", txErr);
    throw new HttpsError(
        "internal",
        "Erro ao salvar os dados da empresa e do responsável.",
    );
  }

  return {
    sucesso: true,
    mensagem: "Empresa e responsável cadastrados com sucesso.",
    empresaId: empresaId,
    uidResponsavel: userRecord.uid,
    codigoConvite: rawInviteCode,
  };
});

/**
 * Callable function to register a new employee using an invitation code.
 */
exports.cadastrarFuncionario = onCall(async (request) => {
  verificarAppCheck(request);

  const {
    nome,
    email,
    codigoConvite,
    senha,
    confirmarSenha,
  } = request.data || {};

  if (
    !validarString(nome, 100, 2) ||
    !validarString(email, 254, 5) ||
    !validarString(codigoConvite, 100, 4) ||
    typeof senha !== "string" ||
    typeof confirmarSenha !== "string"
  ) {
    throw new HttpsError(
        "invalid-argument",
        "Todos os campos são obrigatórios e devem possuir tamanhos válidos.",
    );
  }

  if (senha !== confirmarSenha) {
    throw new HttpsError(
        "invalid-argument",
        "A senha e a confirmação de senha não coincidem.",
    );
  }

  const emailNorm = email.trim().toLowerCase();
  if (!validarEmail(emailNorm)) {
    throw new HttpsError(
        "invalid-argument",
        "O e-mail informado é inválido.",
    );
  }

  if (!validarSenha(senha)) {
    throw new HttpsError(
        "invalid-argument",
        "A senha não atende aos requisitos de segurança (mínimo 8 caracteres," +
        " com maiúscula, minúscula, número e caractere especial).",
    );
  }

  await verificarRateLimit(`funcionario:${emailNorm}`);

  const codeClean = codigoConvite.trim();
  const hashInput = crypto
      .createHash("sha256")
      .update(codeClean)
      .digest("hex");

  const convitesSnap = await db
      .collection("convitesEmpresa")
      .where("codigoHash", "==", hashInput)
      .limit(1)
      .get();

  if (convitesSnap.empty) {
    throw new HttpsError(
        "not-found",
        "Código de convite inválido ou não encontrado.",
    );
  }

  const conviteDocInitial = convitesSnap.docs[0];
  const conviteDataInitial = conviteDocInitial.data();

  if (!conviteDataInitial.ativo) {
    throw new HttpsError(
        "failed-precondition",
        "Este código de convite não está mais ativo.",
    );
  }

  if (
    conviteDataInitial.expiraEm &&
    conviteDataInitial.expiraEm.toMillis() < Date.now()
  ) {
    throw new HttpsError(
        "failed-precondition",
        "Este código de convite expirou.",
    );
  }

  if (
    conviteDataInitial.limiteUsos !== null &&
    conviteDataInitial.limiteUsos !== undefined &&
    conviteDataInitial.quantidadeUsos >= conviteDataInitial.limiteUsos
  ) {
    throw new HttpsError(
        "failed-precondition",
        "Este código de convite atingiu o limite máximo de utilizações.",
    );
  }

  const targetEmpresaId = conviteDataInitial.empresaId;
  const empresaDocInitial = await db
      .collection("empresa")
      .doc(targetEmpresaId)
      .get();

  if (
    !empresaDocInitial.exists ||
    empresaDocInitial.data().status !== "ATIVA"
  ) {
    throw new HttpsError(
        "failed-precondition",
        "A empresa associada a este convite não está ativa.",
    );
  }

  try {
    await admin.auth().getUserByEmail(emailNorm);
    throw new HttpsError(
        "already-exists",
        "O e-mail informado já está cadastrado.",
    );
  } catch (authCheckErr) {
    if (authCheckErr instanceof HttpsError) {
      throw authCheckErr;
    }
    if (authCheckErr.code !== "auth/user-not-found") {
      logger.error("Erro ao verificar e-mail em Auth:", authCheckErr);
      throw new HttpsError(
          "internal",
          "Erro interno ao validar e-mail do funcionário.",
      );
    }
  }

  let userRecord;
  try {
    userRecord = await admin.auth().createUser({
      email: emailNorm,
      password: senha,
      displayName: nome.trim(),
    });
  } catch (createUserErr) {
    if (createUserErr.code === "auth/email-already-exists") {
      throw new HttpsError(
          "already-exists",
          "O e-mail informado já está cadastrado.",
      );
    }
    logger.error("Erro ao criar funcionário no Auth:", createUserErr);
    throw new HttpsError(
        "internal",
        "Erro ao criar conta de autenticação.",
    );
  }

  const funcionarioRef = db.collection("funcionarios").doc(userRecord.uid);
  const conviteRef = db
      .collection("convitesEmpresa")
      .doc(conviteDocInitial.id);
  const empresaRef = db.collection("empresa").doc(targetEmpresaId);

  try {
    await db.runTransaction(async (transaction) => {
      const latestConvite = await transaction.get(conviteRef);
      if (!latestConvite.exists) {
        throw new HttpsError(
            "not-found",
            "O convite não foi encontrado durante a transação.",
        );
      }

      const latestConviteData = latestConvite.data();
      if (!latestConviteData.ativo) {
        throw new HttpsError(
            "failed-precondition",
            "Este código de convite não está mais ativo.",
        );
      }

      if (
        latestConviteData.expiraEm &&
        latestConviteData.expiraEm.toMillis() < Date.now()
      ) {
        throw new HttpsError(
            "failed-precondition",
            "Este código de convite expirou.",
        );
      }

      if (
        latestConviteData.limiteUsos !== null &&
        latestConviteData.limiteUsos !== undefined &&
        latestConviteData.quantidadeUsos >= latestConviteData.limiteUsos
      ) {
        throw new HttpsError(
            "failed-precondition",
            "Este código de convite atingiu o limite máximo de utilizações.",
        );
      }

      const latestEmpresa = await transaction.get(empresaRef);
      if (
        !latestEmpresa.exists ||
        latestEmpresa.data().status !== "ATIVA"
      ) {
        throw new HttpsError(
            "failed-precondition",
            "A empresa associada a este convite não está ativa.",
        );
      }

      const now = admin.firestore.FieldValue.serverTimestamp();

      transaction.update(conviteRef, {
        quantidadeUsos: admin.firestore.FieldValue.increment(1),
      });

      transaction.set(funcionarioRef, {
        uid: userRecord.uid,
        empresaId: targetEmpresaId,
        nome: nome.trim(),
        email: emailNorm,
        perfil: "FUNCIONARIO",
        status: "PENDENTE_APROVACAO",
        cargo: null,
        setorId: null,
        matricula: null,
        dataAdmissao: null,
        faceId: null,
        criadoEm: now,
      });
    });
  } catch (txErr) {
    try {
      await admin.auth().deleteUser(userRecord.uid);
    } catch (delErr) {
      logger.error("CRITICAL: Falha ao deletar Auth na compensação:", {
        uid: userRecord.uid,
        error: delErr,
      });
    }

    if (txErr instanceof HttpsError) {
      throw txErr;
    }
    logger.error(
        "Erro na transação Firestore para cadastrarFuncionario:",
        txErr,
    );
    throw new HttpsError(
        "internal",
        "Erro ao salvar os dados do funcionário.",
    );
  }

  return {
    sucesso: true,
    mensagem:
      "Cadastro de funcionário realizado com sucesso! Aguarde aprovação do RH.",
    uid: userRecord.uid,
    empresaId: targetEmpresaId,
  };
});
