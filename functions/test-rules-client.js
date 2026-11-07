/* eslint-disable no-console */
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const {serverTimestamp} = require("firebase/firestore");
const fs = require("fs");
const path = require("path");

/**
 * Main unit test runner for Firestore Security Rules.
 */
async function main() {
  console.log("=== INICIANDO SUÍTE DE TESTES DO REGISTRO DE PONTO ===");

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

  const empresaAId = "empresa_A_final";
  const empresaBId = "empresa_B_final";
  const rhAUid = "rh_A_final_uid";
  const rhBUid = "rh_B_final_uid";
  const funcAUid = "func_A_final_uid";
  const jornadaAId = "jornada_A_final_id";
  const dataHoje = "2026-11-07";

  const snapshotJornadaReal = {
    entrada: "08:00",
    saidaAlmoco: "12:00",
    retornoAlmoco: "13:00",
    saida: "17:00",
    cargaHorariaMinutos: 480,
    toleranciaMinutos: 5,
  };

  // Setup de documentos
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();

    await adminDb.collection("empresa").doc(empresaAId).set({
      nome: "Empresa A Final",
      cnpj: "11111111000100",
      responsavelId: rhAUid,
      status: "ATIVA",
    });
    await adminDb.collection("funcionario").doc(rhAUid).set({
      uid: rhAUid,
      empresaId: empresaAId,
      nome: "Responsável RH A",
      email: "rhA@final.com",
      perfil: "RH",
      status: "ATIVO",
    });
    await adminDb.collection("funcionario").doc(funcAUid).set({
      uid: funcAUid,
      empresaId: empresaAId,
      nome: "Funcionário A Final",
      email: "funcA@final.com",
      perfil: "FUNCIONARIO",
      status: "ATIVO",
      jornadaId: jornadaAId,
    });

    await adminDb.collection("jornadasTrabalho").doc(jornadaAId).set({
      empresaId: empresaAId,
      nome: "Jornada Padrão Final",
      entrada: "08:00",
      saidaAlmoco: "12:00",
      retornoAlmoco: "13:00",
      saida: "17:00",
      cargaHorariaMinutos: 480,
      toleranciaMinutos: 5,
      ativo: true,
    });

    await adminDb.collection("empresa").doc(empresaBId).set({
      nome: "Empresa B Final",
      cnpj: "22222222000100",
      responsavelId: rhBUid,
      status: "ATIVA",
    });
    await adminDb.collection("funcionario").doc(rhBUid).set({
      uid: rhBUid,
      empresaId: empresaBId,
      nome: "Responsável RH B",
      email: "rhB@final.com",
      perfil: "RH",
      status: "ATIVO",
    });
  });

  const expedienteId = `${funcAUid}_${dataHoje}`;
  const pontoEntradaId = `${funcAUid}_${dataHoje}_ENTRADA`;

  // Teste 1: Funcionário ativo cria expediente com ENTRADA -> SUCESSO
  await test("1. Funcionário ativo cria expediente com ENTRADA válida",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const batch = db.batch();
        batch.set(db.collection("expedientes").doc(expedienteId), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataHoje,
          jornadaSnapshot: snapshotJornadaReal,
          proximaMarcacao: "SAIDA_ALMOCO",
          concluido: false,
        });
        batch.set(db.collection("ponto").doc(pontoEntradaId), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataHoje,
          tipoPonto: "ENTRADA",
          dataHora: serverTimestamp(),
          status: "VALIDO",
          jornadaSnapshot: snapshotJornadaReal,
        });

        await assertSucceeds(batch.commit());
      });

  // Teste 2: Snapshot falsificado na ENTRADA -> FALHA
  const dataOutroDia = "2026-11-08";
  const expDivergId = `${funcAUid}_${dataOutroDia}`;
  const pontoDivergId = `${funcAUid}_${dataOutroDia}_ENTRADA`;

  await test("2. Snapshot falsificado na ENTRADA é bloqueado",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const snapshotFalso = {
          entrada: "00:00",
          saidaAlmoco: "00:00",
          retornoAlmoco: "00:00",
          saida: "00:00",
          cargaHorariaMinutos: 0,
        };

        const batch = db.batch();
        batch.set(db.collection("expedientes").doc(expDivergId), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataOutroDia,
          jornadaSnapshot: snapshotFalso,
          proximaMarcacao: "SAIDA_ALMOCO",
          concluido: false,
        });
        batch.set(db.collection("ponto").doc(pontoDivergId), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataOutroDia,
          tipoPonto: "ENTRADA",
          dataHora: serverTimestamp(),
          status: "VALIDO",
          jornadaSnapshot: snapshotFalso,
        });

        await assertFails(batch.commit());
      });

  // Teste 3: Alterar proximaMarcacao diretamente -> FALHA
  await test("3. Alterar expediente diretamente sem ponto é bloqueado",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const promise = db.collection("expedientes")
            .doc(expedienteId)
            .update({
              proximaMarcacao: "SAIDA",
              concluido: true,
            });

        await assertFails(promise);
      });

  // Teste 4: Alterar campos imutáveis do expediente -> FALHA
  await test("4. Modificar campos imutáveis do expediente é bloqueado",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const promise = db.collection("expedientes")
            .doc(expedienteId)
            .update({
              empresaId: "empresa_hacked_999",
            });

        await assertFails(promise);
      });

  // Teste 5: Registrar SAIDA_ALMOCO mantendo snapshot congelado -> SUCESSO
  const pontoSaidaAlmocoId = `${funcAUid}_${dataHoje}_SAIDA_ALMOCO`;

  await test("5. Registrar SAIDA_ALMOCO mantendo snapshot congelado",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const batch = db.batch();
        batch.update(db.collection("expedientes").doc(expedienteId), {
          proximaMarcacao: "RETORNO_ALMOCO",
        });
        batch.set(db.collection("ponto").doc(pontoSaidaAlmocoId), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataHoje,
          tipoPonto: "SAIDA_ALMOCO",
          dataHora: serverTimestamp(),
          status: "VALIDO",
          jornadaSnapshot: snapshotJornadaReal,
        });

        await assertSucceeds(batch.commit());
      });

  // Teste 6: RH altera/desativa jornada no meio do dia
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("jornadasTrabalho")
        .doc(jornadaAId)
        .update({
          nome: "Jornada Alterada pelo RH",
          ativo: false,
        });
  });

  const pontoRetornoAlmocoId = `${funcAUid}_${dataHoje}_RETORNO_ALMOCO`;

  await test("6. Concluir marcação mesmo se jornada for alterada/desativada",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const batch = db.batch();
        batch.update(db.collection("expedientes").doc(expedienteId), {
          proximaMarcacao: "SAIDA",
        });
        batch.set(db.collection("ponto").doc(pontoRetornoAlmocoId), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataHoje,
          tipoPonto: "RETORNO_ALMOCO",
          dataHora: serverTimestamp(),
          status: "VALIDO",
          jornadaSnapshot: snapshotJornadaReal,
        });

        await assertSucceeds(batch.commit());
      });

  // Teste 7: Concluir o expediente com SAIDA final -> SUCESSO
  const pontoSaidaFinalId = `${funcAUid}_${dataHoje}_SAIDA`;

  await test("7. Concluir o expediente com SAIDA final", async () => {
    const funcCtx = testEnv.authenticatedContext(funcAUid);
    const db = funcCtx.firestore();

    const batch = db.batch();
    batch.update(db.collection("expedientes").doc(expedienteId), {
      proximaMarcacao: "CONCLUIDO",
      concluido: true,
    });
    batch.set(db.collection("ponto").doc(pontoSaidaFinalId), {
      empresaId: empresaAId,
      funcionarioId: funcAUid,
      jornadaId: jornadaAId,
      dataReferencia: dataHoje,
      tipoPonto: "SAIDA",
      dataHora: serverTimestamp(),
      status: "VALIDO",
      jornadaSnapshot: snapshotJornadaReal,
    });

    await assertSucceeds(batch.commit());
  });

  // Teste 8: Tentar reabrir expediente concluído -> FALHA
  await test("8. Tentar reabrir expediente concluído é bloqueado",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const promise = db.collection("expedientes")
            .doc(expedienteId)
            .update({
              concluido: false,
              proximaMarcacao: "SAIDA_ALMOCO",
            });

        await assertFails(promise);
      });

  // Teste 9: Timestamp manual diferente do servidor -> FALHA
  await test("9. Timestamp manual diferente de request.time é bloqueado",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const promise = db.collection("ponto")
            .doc(`${funcAUid}_2026-11-09_ENTRADA`)
            .set({
              empresaId: empresaAId,
              funcionarioId: funcAUid,
              jornadaId: jornadaAId,
              dataReferencia: "2026-11-09",
              tipoPonto: "ENTRADA",
              dataHora: new Date("2020-01-01T00:00:00Z"),
              status: "VALIDO",
              jornadaSnapshot: snapshotJornadaReal,
            });

        await assertFails(promise);
      });

  // Teste 10: RH externo não acessa expedientes/pontos da Empresa A -> FALHA
  await test("10. RH externo não acessa expedientes/pontos", async () => {
    const rhBCtx = testEnv.authenticatedContext(rhBUid);
    const dbRhB = rhBCtx.firestore();

    const promiseGet = dbRhB.collection("expedientes")
        .doc(expedienteId).get();
    await assertFails(promiseGet);

    const promiseGetPonto = dbRhB.collection("ponto")
        .doc(pontoEntradaId).get();
    await assertFails(promiseGetPonto);
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
