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

  // 1. Bloquear criação de RH para empresa existente sem criar nova empresa
  await test("1. Bloquear criação de perfil RH para empresa existente",
      async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
          await context.firestore()
              .collection("empresa")
              .doc("empresa_existente_1")
              .set({
                nome: "Empresa Antiga",
                cnpj: "11111111000100",
                responsavelId: "dono_original",
                status: "ATIVA",
              });
        });

        const userCtx = testEnv.authenticatedContext("invasor_uid");
        const db = userCtx.firestore();

        const promise = db.collection("funcionario")
            .doc("invasor_uid")
            .set({
              uid: "invasor_uid",
              empresaId: "empresa_existente_1",
              nome: "Invasor RH",
              email: "invasor@empresa.com",
              perfil: "RH",
              status: "ATIVO",
            });

        await assertFails(promise);
      });

  // 2. Criar empresa + RH no mesmo batch -> SUCESSO
  await test("2. Permitir cadastro de RH com nova empresa no mesmo batch",
      async () => {
        const rhUid = "rh_valido_uid";
        const empresaId = "empresa_nova_id";
        const userCtx = testEnv.authenticatedContext(rhUid);
        const db = userCtx.firestore();

        const batch = db.batch();
        batch.set(db.collection("cnpjs").doc("22222222000100"), {
          empresaId: empresaId,
        });
        batch.set(db.collection("empresa").doc(empresaId), {
          nome: "Empresa Nova LTDA",
          cnpj: "22222222000100",
          endereco: "Rua B, 200",
          responsavelId: rhUid,
          status: "ATIVA",
        });
        batch.set(db.collection("funcionario").doc(rhUid), {
          uid: rhUid,
          empresaId: empresaId,
          nome: "Responsável RH",
          email: "rh2@empresa.com",
          perfil: "RH",
          status: "ATIVO",
        });

        await assertSucceeds(batch.commit());
      });

  // 3. Cadastro de Funcionário por convite -> VALIDA CONVITE vs BLOQUEIA
  await test("3. Permitir funcionário com convite e bloquear sem convite",
      async () => {
        const inviteCode = "CONVITE_VALIDO_ABC";
        const inviteHash = sha256(inviteCode);
        const empresaId = "empresa_nova_id";

        await testEnv.withSecurityRulesDisabled(async (context) => {
          await context.firestore()
              .collection("convitesLookup")
              .doc(inviteHash)
              .set({
                empresaId: empresaId,
                ativo: true,
                quantidadeUsos: 0,
              });
        });

        const funcUid = "func_convidado_uid";
        const funcCtx = testEnv.authenticatedContext(funcUid);
        const db = funcCtx.firestore();

        const batch = db.batch();
        batch.set(db.collection("funcionario").doc(funcUid), {
          uid: funcUid,
          empresaId: empresaId,
          nome: "Funcionário Convidado",
          email: "func@empresa.com",
          perfil: "FUNCIONARIO",
          status: "PENDENTE_APROVACAO",
          codigoHash: inviteHash,
        });
        batch.update(db.collection("convitesLookup").doc(inviteHash), {
          quantidadeUsos: 1,
        });
        await assertSucceeds(batch.commit());

        const invasorCtx = testEnv.authenticatedContext("invasor_uid_3");
        const dbInvasor = invasorCtx.firestore();
        const failPromise = dbInvasor.collection("funcionario")
            .doc("invasor_uid_3")
            .set({
              uid: "invasor_uid_3",
              empresaId: empresaId,
              nome: "Sem Convite",
              email: "semconvite@empresa.com",
              perfil: "FUNCIONARIO",
              status: "PENDENTE_APROVACAO",
              codigoHash: "HASH_INEXISTENTE_999",
            });
        await assertFails(failPromise);
      });

  // 4. Proteger convites: Funcionário comum NÃO pode criar; RH PODE
  await test("4. Impede que funcionário comum crie convites e permite RH",
      async () => {
        const empresaId = "empresa_nova_id";
        const rhUid = "rh_valido_uid";
        const funcUid = "func_convidado_uid";

        await testEnv.withSecurityRulesDisabled(async (context) => {
          await context.firestore()
              .collection("funcionario")
              .doc(funcUid)
              .update({
                status: "ATIVO",
              });
        });

        const funcCtx = testEnv.authenticatedContext(funcUid);
        const dbFunc = funcCtx.firestore();
        const failConvite = dbFunc.collection("convitesEmpresa")
            .doc("convite_fake")
            .set({
              empresaId: empresaId,
              codigoHash: "hash_fake",
              ativo: true,
            });
        await assertFails(failConvite);

        const rhCtx = testEnv.authenticatedContext(rhUid);
        const dbRh = rhCtx.firestore();
        const succConvite = dbRh.collection("convitesEmpresa")
            .doc("convite_rh_ok")
            .set({
              empresaId: empresaId,
              codigoHash: "hash_rh_ok",
              ativo: true,
            });
        await assertSucceeds(succConvite);
      });

  // 5. Aprovação/Recusa: Somente RH ativo da MESMA empresa consegue aprovar
  await test("5. Somente RH ativo da mesma empresa pode aprovar cadastros",
      async () => {
        const empresaA = "empresa_nova_id";
        const empresaB = "empresa_outra_id";
        const rhAUid = "rh_valido_uid";
        const rhBUid = "rh_empresa_b_uid";
        const funcPendUid = "func_pendente_uid";

        await testEnv.withSecurityRulesDisabled(async (context) => {
          const adminDb = context.firestore();
          await adminDb.collection("empresa").doc(empresaB).set({
            nome: "Outra Empresa",
            cnpj: "33333333000100",
            responsavelId: rhBUid,
            status: "ATIVA",
          });
          await adminDb.collection("funcionario").doc(rhBUid).set({
            uid: rhBUid,
            empresaId: empresaB,
            perfil: "RH",
            status: "ATIVO",
          });
          await adminDb.collection("funcionario").doc(funcPendUid).set({
            uid: funcPendUid,
            empresaId: empresaA,
            nome: "Funcionário Pendente A",
            email: "pendenteA@empresa.com",
            perfil: "FUNCIONARIO",
            status: "PENDENTE_APROVACAO",
          });
        });

        const rhBCtx = testEnv.authenticatedContext(rhBUid);
        const dbRhB = rhBCtx.firestore();
        const failAprov = dbRhB.collection("funcionario")
            .doc(funcPendUid)
            .update({
              status: "ATIVO",
            });
        await assertFails(failAprov);

        const rhACtx = testEnv.authenticatedContext(rhAUid);
        const dbRhA = rhACtx.firestore();
        const succAprov = dbRhA.collection("funcionario")
            .doc(funcPendUid)
            .update({
              status: "ATIVO",
            });
        await assertSucceeds(succAprov);
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
