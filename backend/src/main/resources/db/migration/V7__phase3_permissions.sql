-- V7: Phase 3 permissions. Legacy singular permissions are preserved.

INSERT INTO permissions (code, resource, action, description) VALUES
    ('tenants:create',       'tenants',  'create',       'Cadastrar academias'),
    ('tenants:read',         'tenants',  'read',         'Visualizar academias'),
    ('tenants:update',       'tenants',  'update',       'Editar academias'),
    ('tenants:suspend',      'tenants',  'suspend',      'Suspender ou reativar academias'),
    ('branches:create',      'branches', 'create',       'Cadastrar unidades'),
    ('branches:read',        'branches', 'read',         'Visualizar unidades'),
    ('branches:update',      'branches', 'update',       'Editar unidades e configurações'),
    ('branches:deactivate',  'branches', 'deactivate',   'Ativar ou desativar unidades'),
    ('users:create',         'users',    'create',       'Cadastrar usuários'),
    ('users:read',           'users',    'read',         'Visualizar usuários'),
    ('users:update',         'users',    'update',       'Editar usuários'),
    ('users:deactivate',     'users',    'deactivate',   'Ativar ou desativar usuários'),
    ('users:assign_branch',  'users',    'assign_branch', 'Vincular usuários a unidades'),
    ('users:manage_roles',   'users',    'manage_roles', 'Atribuir papéis a usuários')
ON CONFLICT (code) DO NOTHING;

-- SAAS_ADMIN: global tenant administration plus everything else.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SAAS_ADMIN'
ON CONFLICT DO NOTHING;

-- TENANT_ADMIN: own academy, its units and users. No global tenant creation.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'tenants:read',
    'branches:create', 'branches:read', 'branches:update', 'branches:deactivate',
    'users:create', 'users:read', 'users:update', 'users:deactivate',
    'users:assign_branch', 'users:manage_roles'
)
WHERE r.code = 'TENANT_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'branches:read', 'users:read', 'users:update', 'users:assign_branch'
)
WHERE r.code = 'BRANCH_MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('branches:read', 'users:read')
WHERE r.code = 'RECEPTIONIST'
ON CONFLICT DO NOTHING;
