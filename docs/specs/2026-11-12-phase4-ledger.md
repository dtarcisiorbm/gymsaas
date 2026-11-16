# Ledger — Fase 4 (Gestão de Alunos)

Plano: `docs/specs/2026-11-12-phase4-plan.md`
Spec: prompt da Fase 4 + decisões aprovadas em 2026-11-12.
Observação: o workspace não é um repositório git; não há commits. Cada task é registrada aqui com os comandos e resultados reais.

## Pre-flight

- Task 1 produz tabelas/portas → Tasks 4–9 consomem. Sem conflito: nomes de tabelas e colunas definidos em V8/V9.
- Task 2 produz `StudentRegistrationGenerator.next(UUID)` → Task 4 consome. Sem conflito.
- Task 3 produz records/portas → Tasks 4–7 consomem. Sem conflito de assinatura.
- Task 8 produz endpoints → Task 10 (frontend) consome. Contrato em Task 8.
- Task 12 depende do tema global; Task 13 documenta. Sem conflito.

## Execução

- Task 0 (baseline): backend `mvnw verify` 16 ITs verdes, frontend `npm run build` verde (pré-Fase 4).
- Task 1: `V8__students_core.sql`, `V9__student_branches_history_notes_permissions.sql` criadas e aplicadas (Flyway v9, 8 tabelas `student*`).
- Task 2: CPF valid/normalize/mask em `BrDocuments`, `StudentRegistrationGenerator` + adapter Postgres/JDBC; 8 testes unitários verdes.
- Task 3: domínio `student` (records/enums/portas/critérios), 4 testes verdes.
- Task 4: guard/assembler/DTOs e use cases core (create/update/find/list/change status), 10 testes verdes.
- Task 5: use cases de documentos/contatos/endereço, 9 testes verdes.
- Task 6: use cases de vínculos/transferência/histórico/notas, 9 testes verdes.
- Task 7: entidades JPA, repositórios Spring Data e adaptadores para todas as tabelas; compile e unit tests do módulo verdes.
- Task 8: `StudentController` REST, validação, tenant do contexto, `students:read_sensitive` para revelação, handler de `ObjectOptimisticLockingFailureException` → 409 `CONCURRENT_UPDATE`.
- Task 9: `StudentManagementIT`, `StudentIsolationIT`, `StudentConcurrencyIT` + ajuste `RlsIsolationIT` (9 migrations, 17 tabelas com RLS). Todos verdes em execuções direcionadas.
- Task 10-12: frontend MVVM (`students` model/api/viewmodels), páginas `/academy/students`, `/new`, `/[id]`, `/[id]/edit`, item "Alunos" no nav, tema claro/escuro com toggle persistido e overrides globais; `npm run build` verde (13 rotas).
- Task 13: `docs/security/06-student-data-retention.md` e README atualizados.
- Task 14: `mvnw verify` completo e build frontend executados (ver relatório). Ajustes feitos durante validação: versão `@Version` alinhada a `int`, delete com transação/404, detalhe mascarado por padrão, CPFs únicos nos ITs, `demoTenantId` com bypass RLS.
