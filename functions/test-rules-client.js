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
  console.log("=== EXECUTANDO SUÍTE INTEGRAL DE AUDITORIA E REGRESSÃO ===");

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

  const empresaAId = "empresa_A_leak_test";
  const empresaBId = "empresa_B_leak_test";
  const rhAUid = "rh_A_leak_uid";
  const rhBUid = "rh_B_leak_uid";
  const funcAUid = "func_A_leak_uid";
  const funcBUid = "func_B_leak_uid";
  const jornadaAId = "jornada_A_leak_id";
  const unknownUid = "uid_desconhecido_999";

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
      nome: "Empresa A Leak Test",
      cnpj: "11111111000100",
      responsavelId: rhAUid,
      status: "ATIVA",
    });
    await adminDb.collection("funcionario").doc(rhAUid).set({
      uid: rhAUid,
      empresaId: empresaAId,
      nome: "Responsável RH A",
      email: "rhA@leak.com",
      perfil: "RH",
      status: "ATIVO",
    });
    await adminDb.collection("funcionario").doc(funcAUid).set({
      uid: funcAUid,
      empresaId: empresaAId,
      nome: "Funcionário A Leak",
      email: "funcA@leak.com",
      perfil: "FUNCIONARIO",
      status: "ATIVO",
      jornadaId: jornadaAId,
    });

    await adminDb.collection("jornadasTrabalho").doc(jornadaAId).set({
      empresaId: empresaAId,
      nome: "Jornada Padrão Audit",
      entrada: "08:00",
      saidaAlmoco: "12:00",
      retornoAlmoco: "13:00",
      saida: "17:00",
      cargaHorariaMinutos: 480,
      toleranciaMinutos: 5,
      ativo: true,
    });

    // Empresa B
    await adminDb.collection("empresa").doc(empresaBId).set({
      nome: "Empresa B Leak Test",
      cnpj: "22222222000100",
      responsavelId: rhBUid,
      status: "ATIVA",
    });
    await adminDb.collection("funcionario").doc(rhBUid).set({
      uid: rhBUid,
      empresaId: empresaBId,
      nome: "Responsável RH B",
      email: "rhB@leak.com",
      perfil: "RH",
      status: "ATIVO",
    });
    await adminDb.collection("funcionario").doc(funcBUid).set({
      uid: funcBUid,
      empresaId: empresaBId,
      nome: "Funcionário B Leak",
      email: "funcB@leak.com",
      perfil: "FUNCIONARIO",
      status: "ATIVO",
    });

    // Documento de saldo existente do Funcionario A
    await adminDb.collection("bancoHoras").doc(funcAUid).set({
      funcionarioId: funcAUid,
      empresaId: empresaAId,
      saldoTotalMinutos: 60,
      totalPositivasMinutos: 60,
      totalNegativasMinutos: 0,
    });

    // Documento de saldo existente do Funcionario B
    await adminDb.collection("bancoHoras").doc(funcBUid).set({
      funcionarioId: funcBUid,
      empresaId: empresaBId,
      saldoTotalMinutos: -30,
      totalPositivasMinutos: 0,
      totalNegativasMinutos: 30,
    });

    // Lançamento de histórico do Funcionario A
    await adminDb.collection("historicoBancoHoras").doc("hist_A_1").set({
      funcionarioId: funcAUid,
      empresaId: empresaAId,
      minutosSaldo: 60,
      tipo: "AUTOMATICO_EXPEDIENTE",
      dataLancamento: new Date(),
    });

    // Lançamento de histórico do Funcionario B
    await adminDb.collection("historicoBancoHoras").doc("hist_B_1").set({
      funcionarioId: funcBUid,
      empresaId: empresaBId,
      minutosSaldo: -30,
      tipo: "AUTOMATICO_EXPEDIENTE",
      dataLancamento: new Date(),
    });
  });

  // -------------------------------------------------------------
  // GRUPO A: TESTES DE CONSULTA DE LEITURA (1 A 12)
  // -------------------------------------------------------------

  await test("1. Funcionário consulta o próprio saldo existente", async () => {
    const funcCtx = testEnv.authenticatedContext(funcAUid);
    const db = funcCtx.firestore();

    const promise = db.collection("bancoHoras").doc(funcAUid).get();
    await assertSucceeds(promise);
  });

  await test("2. Funcionário consulta o próprio saldo inexistente",
      async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
          await context.firestore().collection("funcionario")
              .doc("func_sem_saldo_uid")
              .set({
                uid: "func_sem_saldo_uid",
                empresaId: empresaAId,
                nome: "Sem Saldo",
                perfil: "FUNCIONARIO",
                status: "ATIVO",
              });
        });

        const funcCtx2 = testEnv.authenticatedContext("func_sem_saldo_uid");
        const db = funcCtx2.firestore();

        const promise = db.collection("bancoHoras")
            .doc("func_sem_saldo_uid").get();
        await assertSucceeds(promise);
      });

  await test("3. Funcionário tenta consultar saldo de outro funcionário",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const promise = db.collection("bancoHoras").doc(funcBUid).get();
        await assertFails(promise);
      });

  await test("4. RH consulta saldo existente da própria empresa", async () => {
    const rhACtx = testEnv.authenticatedContext(rhAUid);
    const dbRhA = rhACtx.firestore();

    const promise = dbRhA.collection("bancoHoras").doc(funcAUid).get();
    await assertSucceeds(promise);
  });

  await test("5. RH consulta saldo inexistente da própria empresa",
      async () => {
        const rhACtx = testEnv.authenticatedContext(rhAUid);
        const dbRhA = rhACtx.firestore();

        const promise = dbRhA.collection("bancoHoras")
            .doc("func_sem_saldo_uid").get();
        await assertSucceeds(promise);
      });

  await test("6. RH consulta saldo existente de outra empresa", async () => {
    const rhACtx = testEnv.authenticatedContext(rhAUid);
    const dbRhA = rhACtx.firestore();

    const promise = dbRhA.collection("bancoHoras").doc(funcBUid).get();
    await assertFails(promise);
  });

  await test("7. RH consulta saldo inexistente de outra empresa", async () => {
    const rhACtx = testEnv.authenticatedContext(rhAUid);
    const dbRhA = rhACtx.firestore();

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection("bancoHoras")
          .doc("func_B_sem_saldo").delete();
      await context.firestore().collection("funcionario")
          .doc("func_B_sem_saldo").set({
            uid: "func_B_sem_saldo",
            empresaId: empresaBId,
            nome: "Sem Saldo B",
            perfil: "FUNCIONARIO",
            status: "ATIVO",
          });
    });

    const promise = dbRhA.collection("bancoHoras")
        .doc("func_B_sem_saldo").get();
    await assertFails(promise);
  });

  await test("8. RH consulta UID desconhecido (sem cadastro)",
      async () => {
        const rhACtx = testEnv.authenticatedContext(rhAUid);
        const dbRhA = rhACtx.firestore();

        const promise = dbRhA.collection("bancoHoras").doc(unknownUid).get();
        await assertFails(promise);
      });

  await test("9. Funcionário consulta o próprio histórico", async () => {
    const funcCtx = testEnv.authenticatedContext(funcAUid);
    const db = funcCtx.firestore();

    const promise = db.collection("historicoBancoHoras")
        .where("funcionarioId", "==", funcAUid)
        .orderBy("dataLancamento", "desc")
        .get();

    await assertSucceeds(promise);
  });

  await test("10. RH consulta histórico de colaborador da própria empresa",
      async () => {
        const rhACtx = testEnv.authenticatedContext(rhAUid);
        const dbRhA = rhACtx.firestore();

        const promise = dbRhA.collection("historicoBancoHoras")
            .where("empresaId", "==", empresaAId)
            .where("funcionarioId", "==", funcAUid)
            .orderBy("dataLancamento", "desc")
            .get();

        await assertSucceeds(promise);
      });

  await test("11. RH consulta histórico de funcionário de outra empresa",
      async () => {
        const rhACtx = testEnv.authenticatedContext(rhAUid);
        const dbRhA = rhACtx.firestore();

        const promise = dbRhA.collection("historicoBancoHoras")
            .where("empresaId", "==", empresaBId)
            .where("funcionarioId", "==", funcBUid)
            .orderBy("dataLancamento", "desc")
            .get();

        await assertFails(promise);
      });

  await test("12. Funcionário tenta alterar saldo de outro funcionário",
      async () => {
        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();

        const promise = db.collection("bancoHoras").doc(funcBUid).update({
          saldoTotalMinutos: 9999,
        });

        await assertFails(promise);
      });

  // -------------------------------------------------------------
  // GRUPO B: TESTES DE REGRESSÃO E OPERAÇÕES DO BANCO DE HORAS (13 A 16)
  // -------------------------------------------------------------

  await test("13. Fechamento de expediente lança banco de horas",
      async () => {
        const dataRegressao = "2028-11-20";
        const expId = `${funcAUid}_${dataRegressao}`;

        await testEnv.withSecurityRulesDisabled(async (context) => {
          const adminDb = context.firestore();
          await adminDb.collection("expedientes").doc(expId).set({
            empresaId: empresaAId,
            funcionarioId: funcAUid,
            jornadaId: jornadaAId,
            dataReferencia: dataRegressao,
            jornadaSnapshot: snapshotJornadaReal,
            proximaMarcacao: "SAIDA",
            concluido: false,
          });
          await adminDb.collection("controleExpedienteAtivo")
              .doc(funcAUid).set({
                funcionarioId: funcAUid,
                empresaId: empresaAId,
                expedienteId: expId,
                dataReferencia: dataRegressao,
                estado: "ABERTO",
              });
          await adminDb.collection("ponto")
              .doc(`${funcAUid}_${dataRegressao}_ENTRADA`).set({
                empresaId: empresaAId,
                funcionarioId: funcAUid,
                jornadaId: jornadaAId,
                dataReferencia: dataRegressao,
                tipoPonto: "ENTRADA",
                dataHora: new Date(),
                status: "VALIDO",
                jornadaSnapshot: snapshotJornadaReal,
              });
          await adminDb.collection("ponto")
              .doc(`${funcAUid}_${dataRegressao}_SAIDA_ALMOCO`).set({
                empresaId: empresaAId,
                funcionarioId: funcAUid,
                jornadaId: jornadaAId,
                dataReferencia: dataRegressao,
                tipoPonto: "SAIDA_ALMOCO",
                dataHora: new Date(),
                status: "VALIDO",
                jornadaSnapshot: snapshotJornadaReal,
              });
          await adminDb.collection("ponto")
              .doc(`${funcAUid}_${dataRegressao}_RETORNO_ALMOCO`).set({
                empresaId: empresaAId,
                funcionarioId: funcAUid,
                jornadaId: jornadaAId,
                dataReferencia: dataRegressao,
                tipoPonto: "RETORNO_ALMOCO",
                dataHora: new Date(),
                status: "VALIDO",
                jornadaSnapshot: snapshotJornadaReal,
              });
        });

        const funcCtx = testEnv.authenticatedContext(funcAUid);
        const db = funcCtx.firestore();
        const docHist = db.collection("historicoBancoHoras")
            .doc(`${expId}_BANCO`);

        const batch = db.batch();
        batch.update(db.collection("expedientes").doc(expId), {
          proximaMarcacao: "CONCLUIDO",
          concluido: true,
        });
        batch.set(db.collection("ponto")
            .doc(`${funcAUid}_${dataRegressao}_SAIDA`), {
          empresaId: empresaAId,
          funcionarioId: funcAUid,
          jornadaId: jornadaAId,
          dataReferencia: dataRegressao,
          tipoPonto: "SAIDA",
          dataHora: serverTimestamp(),
          status: "VALIDO",
          jornadaSnapshot: snapshotJornadaReal,
        });
        batch.set(db.collection("controleExpedienteAtivo").doc(funcAUid), {
          funcionarioId: funcAUid,
          empresaId: empresaAId,
          expedienteId: expId,
          dataReferencia: dataRegressao,
          estado: "CONCLUIDO",
        });
        batch.set(docHist, {
          funcionarioId: funcAUid,
          empresaId: empresaAId,
          expedienteId: expId,
          dataReferencia: dataRegressao,
          minutosTrabalhados: 510,
          cargaPrevistaMinutos: 480,
          minutosSaldo: 30,
          tipo: "AUTOMATICO_EXPEDIENTE",
          justificativa: "Fechamento automático " + dataRegressao,
        });
        batch.set(db.collection("bancoHoras").doc(funcAUid), {
          funcionarioId: funcAUid,
          empresaId: empresaAId,
          expedienteId: expId,
          dataReferencia: dataRegressao,
          saldoTotalMinutos: 90,
          totalPositivasMinutos: 90,
          totalNegativasMinutos: 0,
        });

        await assertSucceeds(batch.commit());
      });

  await test("14. Ajuste manual positivo do RH com justificativa", async () => {
    const rhACtx = testEnv.authenticatedContext(rhAUid);
    const dbRhA = rhACtx.firestore();

    const batch = dbRhA.batch();
    batch.set(dbRhA.collection("historicoBancoHoras").doc("ajuste_pos_14"), {
      funcionarioId: funcAUid,
      empresaId: empresaAId,
      expedienteId: "",
      dataReferencia: "",
      minutosTrabalhados: 0,
      cargaPrevistaMinutos: 0,
      minutosSaldo: 45,
      tipo: "AJUSTE_MANUAL_RH",
      justificativa: "Horas extras autorizadas em reunião de projeto.",
      rhUid: rhAUid,
    });
    batch.set(dbRhA.collection("bancoHoras").doc(funcAUid), {
      funcionarioId: funcAUid,
      empresaId: empresaAId,
      saldoTotalMinutos: 135,
      totalPositivasMinutos: 135,
      totalNegativasMinutos: 0,
    });

    await assertSucceeds(batch.commit());
  });

  await test("15. Ajuste manual negativo do RH com justificativa", async () => {
    const rhACtx = testEnv.authenticatedContext(rhAUid);
    const dbRhA = rhACtx.firestore();

    const batch = dbRhA.batch();
    batch.set(dbRhA.collection("historicoBancoHoras").doc("ajuste_neg_15"), {
      funcionarioId: funcAUid,
      empresaId: empresaAId,
      expedienteId: "",
      dataReferencia: "",
      minutosTrabalhados: 0,
      cargaPrevistaMinutos: 0,
      minutosSaldo: -15,
      tipo: "AJUSTE_MANUAL_RH",
      justificativa: "Compensação de folga parcial aprovada.",
      rhUid: rhAUid,
    });
    batch.set(dbRhA.collection("bancoHoras").doc(funcAUid), {
      funcionarioId: funcAUid,
      empresaId: empresaAId,
      saldoTotalMinutos: 120,
      totalPositivasMinutos: 135,
      totalNegativasMinutos: 15,
    });

    await assertSucceeds(batch.commit());
  });

  await test("16. Ajuste manual do RH sem justificativa é bloqueado",
      async () => {
        const rhACtx = testEnv.authenticatedContext(rhAUid);
        const dbRhA = rhACtx.firestore();

        const promise = dbRhA.collection("historicoBancoHoras")
            .doc("ajuste_invalido_16")
            .set({
              funcionarioId: funcAUid,
              empresaId: empresaAId,
              expedienteId: "",
              dataReferencia: "",
              minutosTrabalhados: 0,
              cargaPrevistaMinutos: 0,
              minutosSaldo: 30,
              tipo: "AJUSTE_MANUAL_RH",
              justificativa: "abc",
              rhUid: rhAUid,
            });

        await assertFails(promise);
      });

  // -------------------------------------------------------------
  // GRUPO C: CONFIGURAÇÃO DE ÍNDICES (17 A 20)
  // -------------------------------------------------------------

  await test("17. Validar presença dos 4 índices em firestore.indexes.json",
      async () => {
        const idxPath = path.join(__dirname, "..", "firestore.indexes.json");
        const rawJson = fs.readFileSync(idxPath, "utf8");
        const idxData = JSON.parse(rawJson);

        if (idxData && idxData.indexes && idxData.indexes.length === 4) {
          return true;
        } else {
          throw new Error("Índices no arquivo JSON não equivalem a 4.");
        }
      });

  await test("18. Validar estrutura e campos do quarto índice do RH",
      async () => {
        const idxPath = path.join(__dirname, "..", "firestore.indexes.json");
        const rawJson = fs.readFileSync(idxPath, "utf8");
        const idxData = JSON.parse(rawJson);

        const target = idxData.indexes.find((i) => {
          return i.collectionGroup === "historicoBancoHoras" &&
            i.fields.length === 3;
        });

        if (target &&
            target.fields[0].fieldPath === "empresaId" &&
            target.fields[1].fieldPath === "funcionarioId" &&
            target.fields[2].fieldPath === "dataLancamento") {
          return true;
        } else {
          throw new Error("Estrutura do quarto índice do RH é inválida.");
        }
      });

  await test("19. Validar configuração do firebase.json para firestore",
      async () => {
        const fbPath = path.join(__dirname, "..", "firebase.json");
        const rawJson = fs.readFileSync(fbPath, "utf8");
        const fbData = JSON.parse(rawJson);

        if (fbData && fbData.firestore &&
            fbData.firestore.indexes === "firestore.indexes.json") {
          return true;
        } else {
          throw new Error("firebase.json não aponta para firestore.indexes.");
        }
      });

  await test("20. Validar correspondência do método Java com o índice do RH",
      async () => {
        const daoPath = path.join(__dirname, "..", "app", "src", "main",
            "java", "com", "easyclock", "app", "dao",
            "HistoricoBancoHorasDAO.java");
        const code = fs.readFileSync(daoPath, "utf8");

        const f1 = ".whereEqualTo(\"empresaId\", empresaId.trim())";
        const f2 = ".whereEqualTo(\"funcionarioId\", funcionarioUid.trim())";
        const f3 = ".orderBy(\"dataLancamento\", Query.Direction.DESCENDING)";

        if (code.includes(f1) && code.includes(f2) && code.includes(f3)) {
          return true;
        } else {
          throw new Error("HistóricoDAO Java não corresponde aos campos.");
        }
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
