# Retenção e proteção de dados de alunos (LGPD)

Este documento registra as decisões da Fase 4 para dados pessoais de alunos.

## Dados tratados
- **Identificação**: nome, nome social, data de nascimento, matrícula, foto (URL), status cadastral.
- **Contato**: e-mail, telefone e contatos vinculados.
- **Endereço**: CEP e dados de localização residencial.
- **Documentos**: CPF e outros documentos (RG, CNH, passaporte etc.), com metadados de emissão/validade.
- **Vínculos e histórico**: unidades frequentadas, transferências, alterações de status e observações administrativas.

## Princípios aplicados
- **Finalidade e minimização**: coleta restrita ao necessário para gestão acadêmica/cadastral.
- **Isolamento multi-tenant**: `tenant_id` em todas as tabelas de aluno, RLS habilitada e tenant obtido exclusivamente do contexto autenticado.
- **Segurança**: controle de acesso por permissões (`students:*`), mascaramento de documentos por padrão e revelação completa apenas com `students:read_sensitive`.
- **Rastreabilidade**: histórico de status, vínculos/transferências e auditoria de operações sensíveis.

## Retenção
- **Identificação do aluno**: mantida durante a vigência do vínculo com a academia e por **5 anos** após o encerramento, para fins administrativos e legais aplicáveis.
- **Dados de contato**: mantidos durante a vigência e por **2 anos** após o encerramento, podendo ser anonimizados antes quando não mais necessários.
- **Documentos**: armazenados enquanto necessários à finalidade; exibidos mascarados por padrão. O CPF é tratado como dado sensível no contexto operacional e nunca é registrado integralmente em logs.
- **Histórico e observações**: preservados para integridade do cadastro e trilha administrativa, respeitando o soft delete do aluno.

## Exclusão e anonimização
- Não há exclusão física indevida do aluno: utiliza-se **soft delete** (`deleted_at`) preservando histórico e integridade referencial.
- Em demandas de titular ou políticas internas de retenção, registros podem ser anonimizados/expurgados conforme procedimento aprovado, desde que observadas obrigações legais e contratuais.

## Restrições desta fase
- Não há armazenamento de biometria nem reconhecimento facial.
- Integração com terminais Control iD fica restrita à preparação lógica futura (fora do escopo da Fase 4).
