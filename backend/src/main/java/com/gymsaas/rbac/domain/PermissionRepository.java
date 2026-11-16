package com.gymsaas.rbac.domain;

import java.util.List;

public interface PermissionRepository {

    List<Permission> findAll();
}
