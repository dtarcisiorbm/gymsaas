-- V5: system roles and granular permissions (global catalog).

INSERT INTO permissions (code, resource, action, description) VALUES
    ('tenant:read',        'tenant',      'read',          'Visualizar academia'),
    ('tenant:update',      'tenant',      'update',        'Editar academia'),
    ('user:create',        'user',        'create',        'Criar usuário'),
    ('user:read',          'user',        'read',          'Visualizar usuários'),
    ('user:update',        'user',        'update',        'Editar usuários'),
    ('user:delete',        'user',        'delete',        'Desativar usuários'),
    ('user:assign_role',   'user',        'assign_role',   'Atribuir papéis a usuários'),
    ('role:read',          'role',        'read',          'Visualizar papéis'),
    ('role:manage',        'role',        'manage',        'Gerenciar papéis e permissões'),
    ('permission:read',    'permission',  'read',          'Visualizar permissões'),
    ('audit:read',         'audit',       'read',          'Consultar auditoria'),
    ('student:create',     'student',     'create',        'Cadastrar alunos'),
    ('student:read',       'student',     'read',          'Visualizar alunos'),
    ('student:update',     'student',     'update',        'Editar alunos'),
    ('student:delete',     'student',     'delete',        'Desativar alunos'),
    ('plan:read',          'plan',        'read',          'Visualizar planos'),
    ('plan:manage',        'plan',        'manage',        'Gerenciar planos'),
    ('payment:read',       'payment',     'read',          'Visualizar pagamentos'),
    ('payment:manage',     'payment',     'manage',        'Gerenciar pagamentos'),
    ('device:read',        'device',      'read',          'Visualizar dispositivos'),
    ('device:manage',      'device',      'manage',        'Gerenciar dispositivos'),
    ('access:read',        'access',      'read',          'Visualizar regras e logs de acesso'),
    ('access:manage',      'access',      'manage',        'Gerenciar regras de acesso'),
    ('report:read',        'report',      'read',          'Visualizar relatórios'),
    ('report:export',      'report',      'export',        'Exportar relatórios');

INSERT INTO roles (tenant_id, code, name, description, is_system) VALUES
    (NULL, 'SAAS_ADMIN',     'Administrador SaaS', 'Acesso global à plataforma', true),
    (NULL, 'TENANT_ADMIN',   'Administrador da Academia', 'Administra a academia', true),
    (NULL, 'BRANCH_MANAGER', 'Gerente de Unidade', 'Gerencia a operação da unidade', true),
    (NULL, 'RECEPTIONIST',   'Recepcionista', 'Atendimento, alunos e pagamentos', true),
    (NULL, 'TRAINER',        'Instrutor', 'Consulta alunos e treinos', true),
    (NULL, 'STUDENT',        'Aluno', 'Acesso aos próprios dados', true);

-- SAAS_ADMIN and TENANT_ADMIN: every permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('SAAS_ADMIN', 'TENANT_ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'tenant:read', 'user:read', 'user:update', 'role:read', 'permission:read',
    'audit:read', 'student:create', 'student:read', 'student:update',
    'plan:read', 'payment:read', 'device:read', 'access:read', 'access:manage',
    'report:read'
)
WHERE r.code = 'BRANCH_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:read', 'student:create', 'student:read', 'student:update',
    'plan:read', 'payment:read', 'payment:manage', 'access:read', 'report:read'
)
WHERE r.code = 'RECEPTIONIST';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('student:read', 'plan:read', 'report:read')
WHERE r.code = 'TRAINER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'student:read'
WHERE r.code = 'STUDENT';
