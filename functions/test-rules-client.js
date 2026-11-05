/* eslint-disable no-console */
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

/**
 * Computes SHA-256 hex string.
 * @param {string} text Text to hash.
 * @return {string} Hex hash string.
 */
function sha256(text) {
  return crypto.createHash("sha256").update(text).digest("hex");
}

/**
 * Main unit test runner for Firestore Security Rules.
 */
async function main() {
  console.log("=== INICIANDO TESTES COM @firebase/rules-unit-testing ===");

  const rulesPath = path.join(__dirname, "..", "firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");

  const testEnv = await initializeTestEnvironment({
    projectId: "easyclock-f2e8f",
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: rules,
    },
  });

  let passed = 0;
  let failed = 0;

  /**
   * Helper function to execute and log test.
   * @param {string} name Test description.
   * @param {Function} fn Test logic.
   */
  async function test(name, fn) {
    try {
      await fn();
      console.log(`[PASS] ${name}`);
      passed++;
    } catch (err) {
      console.error(`[FAIL] ${name}`);
      console.error(`       Erro: ${err.message}`);
      failed++;
    }
  }

  const rhUid = "rh_valido_uid_100";
  const empresaId = "empresa_nova_id_100";
  const cnpjClean = "11111111000100";

  // Setup inicial: Empresa A e RH A
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("cnpjs").doc(cnpjClean).set({
      empresaId: empresaId,
    });
    await adminDb.collection("empresa").doc(empresaId).set({
      nome: "Empresa Teste A",
      cnpj: cnpjClean,
      responsavelId: rhUid,
      status: "ATIVA",
    });
    await adminDb.collection("funcionario").doc(rhUid).set({
      uid: rhUid,
      empresaId: empresaId,
      nome: "Responsável RH",
      email: "rh@empresa.com",
      perfil: "RH",
      status: "ATIVO",
    });
  });

  const inviteCodeA = "CONVITE_TESTE_A_100";
  const inviteHashA = sha256(inviteCodeA);

  // Criar convite válido para Empresa A
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("convitesLookup").doc(inviteHashA).set({
      empresaId: empresaId,
      codigoHash: inviteHashA,
      ativo: true,
      limiteUsos: 5,
      quantidadeUsos: 0,
    });
  });

  // 1. Bloquear incremento isolado do contador
  await test("1. Bloquear incremento isolado do contador de convite",
      async () => {
        const userCtx = testEnv.authenticatedContext("usuario_comum_1");
        const db = userCtx.firestore();

        const promise = db.collection("convitesLookup").doc(inviteHashA)
            .update({
              quantidadeUsos: 1,
            });

        await assertFails(promise);
      });

  // 2. Bloquear incremento de convite com empresa divergente no cadastro
  await test("2. Bloquear incremento de convite com empresa divergente",
      async () => {
        const funcUid = "func_divergente_2";
        const funcCtx = testEnv.authenticatedContext(funcUid);
        const db = funcCtx.firestore();

        const batch = db.batch();
        batch.set(db.collection("funcionario").doc(funcUid), {
          uid: funcUid,
          empresaId: "outra_empresa_falsa_999",
          nome: "Invasor Empresa",
          email: "invasor2@empresa.com",
          perfil: "FUNCIONARIO",
          status: "PENDENTE_APROVACAO",
          codigoHash: inviteHashA,
        });
        batch.update(db.collection("convitesLookup").doc(inviteHashA), {
          quantidadeUsos: 1,
        });

        await assertFails(batch.commit());
      });

  // 3. Bloquear uso de convite inativo
  const inviteInativoHash = sha256("CONVITE_INATIVO_3");
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("convitesLookup")
        .doc(inviteInativoHash)
        .set({
          empresaId: empresaId,
          codigoHash: inviteInativoHash,
          ativo: false,
          quantidadeUsos: 0,
        });
  });

  await test("3. Bloquear uso de convite inativo", async () => {
    const funcUid = "func_inativo_3";
    const funcCtx = testEnv.authenticatedContext(funcUid);
    const db = funcCtx.firestore();

    const batch = db.batch();
    batch.set(db.collection("funcionario").doc(funcUid), {
      uid: funcUid,
      empresaId: empresaId,
      nome: "Funcionário Inativo",
      email: "inativo3@empresa.com",
      perfil: "FUNCIONARIO",
      status: "PENDENTE_APROVACAO",
      codigoHash: inviteInativoHash,
    });
    batch.update(db.collection("convitesLookup").doc(inviteInativoHash), {
      quantidadeUsos: 1,
    });

    await assertFails(batch.commit());
  });

  // 4. Bloquear uso acima do limite de utilizações
  const inviteEsgotadoHash = sha256("CONVITE_ESGOTADO_4");
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("convitesLookup")
        .doc(inviteEsgotadoHash)
        .set({
          empresaId: empresaId,
          codigoHash: inviteEsgotadoHash,
          ativo: true,
          limiteUsos: 1,
          quantidadeUsos: 1,
        });
  });

  await test("4. Bloquear uso de convite que atingiu o limite", async () => {
    const funcUid = "func_esgotado_4";
    const funcCtx = testEnv.authenticatedContext(funcUid);
    const db = funcCtx.firestore();

    const batch = db.batch();
    batch.set(db.collection("funcionario").doc(funcUid), {
      uid: funcUid,
      empresaId: empresaId,
      nome: "Funcionário Esgotado",
      email: "esgotado4@empresa.com",
      perfil: "FUNCIONARIO",
      status: "PENDENTE_APROVACAO",
      codigoHash: inviteEsgotadoHash,
    });
    batch.update(db.collection("convitesLookup").doc(inviteEsgotadoHash), {
      quantidadeUsos: 2,
    });

    await assertFails(batch.commit());
  });

  // 5. Permitir cadastro legítimo de funcionário com contador
  await test("5. Permitir cadastro legítimo com atualização do contador",
      async () => {
        const funcUid = "func_legitimo_5";
        const funcCtx = testEnv.authenticatedContext(funcUid);
        const db = funcCtx.firestore();

        const batch = db.batch();
        batch.set(db.collection("funcionario").doc(funcUid), {
          uid: funcUid,
          empresaId: empresaId,
          nome: "Funcionário Legítimo",
          email: "legitimo5@empresa.com",
          perfil: "FUNCIONARIO",
          status: "PENDENTE_APROVACAO",
          codigoHash: inviteHashA,
        });
        batch.update(db.collection("convitesLookup").doc(inviteHashA), {
          quantidadeUsos: 1,
        });

        await assertSucceeds(batch.commit());
      });

  // 6. Bloquear reserva de CNPJ isolada sem empresa no mesmo lote
  await test("6. Bloquear reserva isolada de CNPJ sem empresa", async () => {
    const userCtx = testEnv.authenticatedContext("usuario_cnpjs_6");
    const db = userCtx.firestore();

    const promise = db.collection("cnpjs").doc("88888888000100").set({
      empresaId: "empresa_isolada_6",
    });

    await assertFails(promise);
  });

  // 7. Bloquear reserva de CNPJ associada a empresa preexistente
  await test("7. Bloquear reserva de CNPJ para empresa preexistente",
      async () => {
        const userCtx = testEnv.authenticatedContext("usuario_cnpjs_7");
        const db = userCtx.firestore();

        const promise = db.collection("cnpjs").doc("11111111000100").set({
          empresaId: empresaId,
        });

        await assertFails(promise);
      });

  // 8. Permitir cadastro legítimo de nova empresa com reserva de CNPJ
  await test("8. Permitir cadastro legítimo de nova empresa com CNPJ",
      async () => {
        const novoRhUid = "novo_rh_800";
        const novaEmpresaId = "nova_empresa_800";
        const novoCnpj = "99999999000100";

        const userCtx = testEnv.authenticatedContext(novoRhUid);
        const db = userCtx.firestore();

        const batch = db.batch();
        batch.set(db.collection("cnpjs").doc(novoCnpj), {
          empresaId: novaEmpresaId,
        });
        batch.set(db.collection("empresa").doc(novaEmpresaId), {
          nome: "Nova Empresa S.A.",
          cnpj: novoCnpj,
          endereco: "Rua Nova, 800",
          responsavelId: novoRhUid,
          status: "ATIVA",
        });
        batch.set(db.collection("funcionario").doc(novoRhUid), {
          uid: novoRhUid,
          empresaId: novaEmpresaId,
          nome: "Novo RH Responsável",
          email: "novorh800@empresa.com",
          perfil: "RH",
          status: "ATIVO",
        });

        await assertSucceeds(batch.commit());
      });

  // 9. Aprovação por RH legítimo vs bloqueio por RH de outra empresa
  const rhBUid = "rh_b_uid_900";
  const empresaBId = "empresa_b_id_900";

  await test("9. Aprovação por RH legítimo vs bloqueio por RH externo",
      async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
          const adminDb = context.firestore();
          await adminDb.collection("empresa").doc(empresaBId).set({
            nome: "Empresa B",
            cnpj: "77777777000100",
            responsavelId: rhBUid,
            status: "ATIVA",
          });
          await adminDb.collection("funcionario").doc(rhBUid).set({
            uid: rhBUid,
            empresaId: empresaBId,
            perfil: "RH",
            status: "ATIVO",
          });
        });

        const rhBCtx = testEnv.authenticatedContext(rhBUid);
        const dbRhB = rhBCtx.firestore();
        const failAprov = dbRhB.collection("funcionario")
            .doc("func_legitimo_5").update({
              status: "ATIVO",
            });
        await assertFails(failAprov);

        const rhACtx = testEnv.authenticatedContext(rhUid);
        const dbRhA = rhACtx.firestore();
        const succAprov = dbRhA.collection("funcionario")
            .doc("func_legitimo_5").update({
              status: "ATIVO",
            });
        await assertSucceeds(succAprov);
      });

  // 10. Leitura de convite por hash exato antes de autenticação
  await test("10. Consulta de convite exato permitida antes da Auth",
      async () => {
        const unauthCtx = testEnv.unauthenticatedContext();
        const db = unauthCtx.firestore();

        const promise = db.collection("convitesLookup").doc(inviteHashA).get();
        await assertSucceeds(promise);
      });

  console.log("\n=================================================");
  console.log(`  RESULTADO: ${passed} PASSARAM, ${failed} FALHARAM.`);
  console.log("=================================================");

  await testEnv.cleanup();

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error("Erro ao rodar testes:", err);
  process.exit(1);
});
