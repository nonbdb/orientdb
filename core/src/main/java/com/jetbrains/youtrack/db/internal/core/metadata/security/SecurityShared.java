/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.exception.SecurityAccessException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Record;
import com.jetbrains.youtrack.db.api.schema.Property;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaClass.INDEX_TYPE;
import com.jetbrains.youtrack.db.api.security.SecurityUser;
import com.jetbrains.youtrack.db.api.security.SecurityUser.STATUSES;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.SystemDatabase;
import com.jetbrains.youtrack.db.internal.core.db.record.ClassTrigger;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedSet;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.NullOutputListener;
import com.jetbrains.youtrack.db.internal.core.metadata.MetadataDefault;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.PropertyInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaImmutableClass;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule.ResourceGeneric;
import com.jetbrains.youtrack.db.internal.core.metadata.security.auth.AuthenticationInfo;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.Sequence;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.security.GlobalUser;
import com.jetbrains.youtrack.db.internal.core.security.SecuritySystem;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLBooleanExpression;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Shared security class. It's shared by all the database instances that point to the same storage.
 */
public class SecurityShared implements SecurityInternal {

  private static final String DEFAULT_WRITER_ROLE_NAME = "writer";

  private static final String DEFAULT_READER_ROLE_NAME = "reader";

  private final AtomicLong version = new AtomicLong();

  public static final String RESTRICTED_CLASSNAME = "ORestricted";
  public static final String IDENTITY_CLASSNAME = "OIdentity";

  /**
   * role name -> class name -> true: has some rules, ie. it's not all allowed
   */
  protected Map<String, Map<String, Boolean>> roleHasPredicateSecurityForClass;

  // used to avoid updating the above while the security schema is being created
  protected boolean skipRoleHasPredicateSecurityForClassUpdate = false;

  protected Map<String, Map<String, SQLBooleanExpression>> securityPredicateCache =
      new ConcurrentHashMap<>();

  /**
   * set of all the security resources defined on properties (used for optimizations)
   */
  protected Set<SecurityResourceProperty> filteredProperties;

  private final SecuritySystem security;

  /**
   * Uses the RestrictedOperation ENUM instead.
   */
  @Deprecated
  public static final String ALLOW_ALL_FIELD = RestrictedOperation.ALLOW_ALL.getFieldName();

  /**
   * Uses the RestrictedOperation ENUM instead.
   */
  @Deprecated
  public static final String ALLOW_READ_FIELD = RestrictedOperation.ALLOW_READ.getFieldName();

  /**
   * Uses the RestrictedOperation ENUM instead.
   */
  @Deprecated
  public static final String ALLOW_UPDATE_FIELD = RestrictedOperation.ALLOW_UPDATE.getFieldName();

  /**
   * Uses the RestrictedOperation ENUM instead.
   */
  @Deprecated
  public static final String ALLOW_DELETE_FIELD = RestrictedOperation.ALLOW_DELETE.getFieldName();

  public static final String ONCREATE_IDENTITY_TYPE = "onCreate.identityType";
  public static final String ONCREATE_FIELD = "onCreate.fields";

  public static final Set<String> ALLOW_FIELDS =
      Collections.unmodifiableSet(
          new HashSet<>() {
            {
              add(RestrictedOperation.ALLOW_ALL.getFieldName());
              add(RestrictedOperation.ALLOW_READ.getFieldName());
              add(RestrictedOperation.ALLOW_UPDATE.getFieldName());
              add(RestrictedOperation.ALLOW_DELETE.getFieldName());
            }
          });

  public SecurityShared(SecuritySystem security) {
    this.security = security;
  }

  @Override
  public Identifiable allowRole(
      final DatabaseSession session,
      final EntityImpl entity,
      final RestrictedOperation iOperation,
      final String iRoleName) {
    return session.computeInTx(
        () -> {
          final RID role = getRoleRID(session, iRoleName);
          if (role == null) {
            throw new IllegalArgumentException("Role '" + iRoleName + "' not found");
          }

          return allowIdentity(session, entity, iOperation.getFieldName(), role);
        });
  }

  @Override
  public Identifiable allowUser(
      final DatabaseSession session,
      final EntityImpl entity,
      final RestrictedOperation iOperation,
      final String iUserName) {
    return session.computeInTx(
        () -> {
          final RID user = getUserRID(session, iUserName);
          if (user == null) {
            throw new IllegalArgumentException("User '" + iUserName + "' not found");
          }

          return allowIdentity(session, entity, iOperation.getFieldName(), user);
        });
  }

  public Identifiable allowIdentity(
      final DatabaseSession session,
      final EntityImpl entity,
      final String iAllowFieldName,
      final Identifiable iId) {
    return session.computeInTx(
        () -> {
          Set<Identifiable> field = entity.field(iAllowFieldName);
          if (field == null) {
            field = new TrackedSet<>(entity);
            entity.field(iAllowFieldName, field);
          }
          field.add(iId);

          return iId;
        });
  }

  @Override
  public Identifiable denyUser(
      final DatabaseSessionInternal session,
      final EntityImpl entity,
      final RestrictedOperation iOperation,
      final String iUserName) {
    return session.computeInTx(
        () -> {
          final RID user = getUserRID(session, iUserName);
          if (user == null) {
            throw new IllegalArgumentException("User '" + iUserName + "' not found");
          }

          return disallowIdentity(session, entity, iOperation.getFieldName(), user);
        });
  }

  @Override
  public Identifiable denyRole(
      final DatabaseSessionInternal session,
      final EntityImpl entity,
      final RestrictedOperation iOperation,
      final String iRoleName) {
    return session.computeInTx(
        () -> {
          final RID role = getRoleRID(session, iRoleName);
          if (role == null) {
            throw new IllegalArgumentException("Role '" + iRoleName + "' not found");
          }

          return disallowIdentity(session, entity, iOperation.getFieldName(), role);
        });
  }

  public Identifiable disallowIdentity(
      final DatabaseSessionInternal session,
      final EntityImpl entity,
      final String iAllowFieldName,
      final Identifiable iId) {
    Set<Identifiable> field = entity.field(iAllowFieldName);
    if (field != null) {
      field.remove(iId);
    }
    return iId;
  }

  @Override
  public boolean isAllowed(
      final DatabaseSessionInternal session,
      final Set<Identifiable> iAllowAll,
      final Set<Identifiable> iAllowOperation) {
    if ((iAllowAll == null || iAllowAll.isEmpty())
        && (iAllowOperation == null || iAllowOperation.isEmpty()))
    // NO AUTHORIZATION: CAN'T ACCESS
    {
      return false;
    }

    final SecurityUser currentUser = session.geCurrentUser();
    if (currentUser != null) {
      // CHECK IF CURRENT USER IS ENLISTED
      if (iAllowAll == null
          || (iAllowAll != null && !iAllowAll.contains(currentUser.getIdentity()))) {
        // CHECK AGAINST SPECIFIC _ALLOW OPERATION
        if (iAllowOperation != null && iAllowOperation.contains(currentUser.getIdentity())) {
          return true;
        }

        // CHECK IF AT LEAST ONE OF THE USER'S ROLES IS ENLISTED
        for (SecurityRole r : currentUser.getRoles()) {
          // CHECK AGAINST GENERIC _ALLOW
          if (iAllowAll != null && iAllowAll.contains(r.getIdentity())) {
            return true;
          }
          // CHECK AGAINST SPECIFIC _ALLOW OPERATION
          if (iAllowOperation != null && iAllowOperation.contains(r.getIdentity())) {
            return true;
          }
          // CHECK inherited permissions from parent roles, fixes #1980: Record Level Security:
          // permissions don't follow role's
          // inheritance
          SecurityRole parentRole = r.getParentRole();
          while (parentRole != null) {
            if (iAllowAll != null && iAllowAll.contains(parentRole.getIdentity())) {
              return true;
            }
            if (iAllowOperation != null && iAllowOperation.contains(
                parentRole.getIdentity())) {
              return true;
            }
            parentRole = parentRole.getParentRole();
          }
        }
        return false;
      }
    }
    return true;
  }

  @Override
  public SecurityUser securityAuthenticate(
      DatabaseSessionInternal session, AuthenticationInfo authenticationInfo) {
    SecurityUser user = null;
    final String dbName = session.getName();
    assert !session.isRemote();
    user = security.authenticate(session, authenticationInfo);

    if (user != null) {
      if (user.getAccountStatus(session) != SecurityUser.STATUSES.ACTIVE) {
        throw new SecurityAccessException(dbName,
            "User '" + user.getName(session) + "' is not active");
      }
    } else {
      // WAIT A BIT TO AVOID BRUTE FORCE
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }

      throw new SecurityAccessException(
          dbName, "Invalid authentication info for access to the database " + authenticationInfo);
    }
    return user;
  }

  @Override
  public SecurityUser securityAuthenticate(
      DatabaseSessionInternal session, String userName, String password) {
    SecurityUser user;
    final String dbName = session.getName();
    assert !session.isRemote();
    user = security.authenticate(session, userName, password);

    if (user != null) {
      if (user.getAccountStatus(session) != SecurityUser.STATUSES.ACTIVE) {
        throw new SecurityAccessException(dbName,
            "User '" + user.getName(session) + "' is not active");
      }
    } else {
      // WAIT A BIT TO AVOID BRUTE FORCE
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }

      throw new SecurityAccessException(
          dbName,
          "User or password not valid for username: " + userName + ", database: '" + dbName + "'");
    }
    return user;
  }

  @Override
  public SecurityUserImpl authenticate(
      DatabaseSessionInternal session, final String iUsername, final String iUserPassword) {
    return null;
  }

  // Token MUST be validated before being passed to this method.
  public SecurityUserImpl authenticate(final DatabaseSessionInternal session,
      final Token authToken) {
    final String dbName = session.getName();
    if (!authToken.getIsValid()) {
      throw new SecurityAccessException(dbName, "Token not valid");
    }

    SecurityUserImpl user = authToken.getUser(session);
    if (user == null && authToken.getUserName() != null) {
      // Token handler may not support returning an OUser so let's get username (subject) and query:
      user = getUser(session, authToken.getUserName());
    }

    if (user == null) {
      throw new SecurityAccessException(
          dbName, "Authentication failed, could not load user from token");
    }
    if (user.getAccountStatus(session) != STATUSES.ACTIVE) {
      throw new SecurityAccessException(dbName,
          "User '" + user.getName(session) + "' is not active");
    }

    return user;
  }

  public SecurityUserImpl getUser(final DatabaseSession session, final RID iRecordId) {
    if (iRecordId == null) {
      return null;
    }

    EntityImpl result;
    result = session.load(iRecordId);
    if (!result.getClassName().equals(SecurityUserImpl.CLASS_NAME)) {
      result = null;
    }
    return new SecurityUserImpl((DatabaseSessionInternal) session, result);
  }

  public SecurityUserImpl createUser(
      final DatabaseSessionInternal session,
      final String iUserName,
      final String iUserPassword,
      final String... iRoles) {
    final SecurityUserImpl user = new SecurityUserImpl(session, iUserName, iUserPassword);

    if (iRoles != null) {
      for (String r : iRoles) {
        user.addRole(session, r);
      }
    }

    user.save(session);
    return user;
  }

  public SecurityUserImpl createUser(
      final DatabaseSessionInternal session,
      final String userName,
      final String userPassword,
      final Role... roles) {
    final SecurityUserImpl user = new SecurityUserImpl(session, userName, userPassword);

    if (roles != null) {
      for (Role r : roles) {
        user.addRole(session, r);
      }
    }

    user.save(session);
    return user;
  }

  public boolean dropUser(final DatabaseSession session, final String iUserName) {
    final Number removed;
    try (ResultSet res = session.command("delete from OUser where name = ?", iUserName)) {
      removed = res.next().getProperty("count");
    }

    return removed != null && removed.intValue() > 0;
  }

  public Role getRole(final DatabaseSession db, final Identifiable iRole) {
    try {
      final EntityImpl entity = iRole.getRecord(db);
      SchemaImmutableClass clazz = EntityInternalUtils.getImmutableSchemaClass(entity);

      if (clazz != null && clazz.isRole()) {
        return new Role((DatabaseSessionInternal) db, entity);
      }
    } catch (RecordNotFoundException rnf) {
      return null;
    }

    return null;
  }

  public Role getRole(final DatabaseSession session, final String iRoleName) {
    if (iRoleName == null) {
      return null;
    }

    try (final ResultSet result =
        session.query("select from " + Role.CLASS_NAME + " where name = ? limit 1", iRoleName)) {
      if (result.hasNext()) {
        return new Role((DatabaseSessionInternal) session,
            (EntityImpl) result.next().getEntity().get());
      }
    }

    return null;
  }

  public static RID getRoleRID(final DatabaseSession session, final String iRoleName) {
    if (iRoleName == null) {
      return null;
    }

    try (final ResultSet result =
        session.query("select @rid as rid from " + Role.CLASS_NAME + " where name = ? limit 1",
            iRoleName)) {

      if (result.hasNext()) {
        return result.next().getProperty("rid");
      }
    }
    return null;
  }

  public Role createRole(
      final DatabaseSessionInternal session, final String iRoleName) {
    return createRole(session, iRoleName, null);
  }

  @Override
  public Role createRole(
      final DatabaseSessionInternal session,
      final String iRoleName,
      final Role iParent) {
    final Role role = new Role(session, iRoleName, iParent);
    role.save(session);
    return role;
  }

  public boolean dropRole(final DatabaseSession session, final String iRoleName) {
    final Number removed;
    try (ResultSet result =
        session.command("delete from " + Role.CLASS_NAME + " where name = '" + iRoleName + "'")) {
      removed = result.next().getProperty("count");
    }

    return removed != null && removed.intValue() > 0;
  }

  public List<EntityImpl> getAllUsers(final DatabaseSession session) {
    try (ResultSet rs = session.query("select from OUser")) {
      return rs.stream().map((e) -> (EntityImpl) e.getEntity().get())
          .collect(Collectors.toList());
    }
  }

  public List<EntityImpl> getAllRoles(final DatabaseSession session) {
    try (ResultSet rs = session.query("select from " + Role.CLASS_NAME)) {
      return rs.stream().map((e) -> (EntityImpl) e.getEntity().get())
          .collect(Collectors.toList());
    }
  }

  @Override
  public Map<String, ? extends SecurityPolicy> getSecurityPolicies(
      DatabaseSession session, SecurityRole role) {
    var result = role.getPolicies(session);
    return result != null ? result : Collections.emptyMap();
  }

  @Override
  public SecurityPolicy getSecurityPolicy(
      DatabaseSession session, SecurityRole role, String resource) {
    resource = normalizeSecurityResource(session, resource);
    return role.getPolicy(session, resource);
  }

  public void setSecurityPolicyWithBitmask(
      DatabaseSessionInternal session, SecurityRole role, String resource, int legacyPolicy) {
    String policyName = "default_" + legacyPolicy;
    SecurityPolicyImpl policy = getSecurityPolicy(session, policyName);
    if (policy == null) {
      policy = createSecurityPolicy(session, policyName);
      policy.setCreateRule(session,
          (legacyPolicy & Role.PERMISSION_CREATE) > 0 ? "true" : "false");
      policy.setReadRule(session, (legacyPolicy & Role.PERMISSION_READ) > 0 ? "true" : "false");
      policy.setBeforeUpdateRule(session,
          (legacyPolicy & Role.PERMISSION_UPDATE) > 0 ? "true" : "false");
      policy.setAfterUpdateRule(session,
          (legacyPolicy & Role.PERMISSION_UPDATE) > 0 ? "true" : "false");
      policy.setDeleteRule(session,
          (legacyPolicy & Role.PERMISSION_DELETE) > 0 ? "true" : "false");
      policy.setExecuteRule(session,
          (legacyPolicy & Role.PERMISSION_EXECUTE) > 0 ? "true" : "false");
      saveSecurityPolicy(session, policy);
    }
    setSecurityPolicy(session, role, resource, policy);
  }

  @Override
  public void setSecurityPolicy(
      DatabaseSessionInternal session, SecurityRole role, String resource,
      SecurityPolicyImpl policy) {
    var currentResource = normalizeSecurityResource(session, resource);
    Entity roleEntity = session.load(role.getIdentity().getIdentity());
    validatePolicyWithIndexes(session, currentResource);
    Map<String, Identifiable> policies = roleEntity.getProperty("policies");
    if (policies == null) {
      policies = new HashMap<>();
      roleEntity.setProperty("policies", policies);
    }

    policies.put(currentResource, policy.getElement(session));
    session.save(roleEntity);
    if (session.geCurrentUser() != null && session.geCurrentUser()
        .hasRole(session, role.getName(session), true)) {
      session.reloadUser();
    }
    updateAllFilteredProperties(session);
    initPredicateSecurityOptimizations(session);
  }

  private static void validatePolicyWithIndexes(DatabaseSession session, String resource)
      throws IllegalArgumentException {
    SecurityResource res = SecurityResource.getInstance(resource);
    if (res instanceof SecurityResourceProperty) {
      String clazzName = ((SecurityResourceProperty) res).getClassName();
      SchemaClass clazz =
          ((DatabaseSessionInternal) session)
              .getMetadata()
              .getImmutableSchemaSnapshot()
              .getClass(clazzName);
      if (clazz == null) {
        return;
      }
      Set<SchemaClass> allClasses = new HashSet<>();
      allClasses.add(clazz);
      allClasses.addAll(clazz.getAllSubclasses());
      allClasses.addAll(clazz.getAllSuperClasses());
      for (SchemaClass c : allClasses) {
        for (Index index : ((SchemaClassInternal) c).getIndexesInternal(session)) {
          List<String> indexFields = index.getDefinition().getFields();
          if (indexFields.size() > 1
              && indexFields.contains(((SecurityResourceProperty) res).getPropertyName())) {
            throw new IllegalArgumentException(
                "Cannot bind security policy on "
                    + resource
                    + " because of existing composite indexes: "
                    + index.getName());
          }
        }
      }
    }
  }

  @Override
  public SecurityPolicyImpl createSecurityPolicy(DatabaseSession session, String name) {
    Entity elem = session.newEntity(SecurityPolicy.CLASS_NAME);
    elem.setProperty("name", name);
    SecurityPolicyImpl policy = new SecurityPolicyImpl(elem);
    saveSecurityPolicy(session, policy);
    return policy;
  }

  @Override
  public SecurityPolicyImpl getSecurityPolicy(DatabaseSession session, String name) {
    try (ResultSet rs =
        session.query(
            "SELECT FROM " + SecurityPolicy.CLASS_NAME + " WHERE name = ?", name)) {
      if (rs.hasNext()) {
        Result result = rs.next();
        return new SecurityPolicyImpl(result.getEntity().get());
      }
    }
    return null;
  }

  @Override
  public void saveSecurityPolicy(DatabaseSession session, SecurityPolicyImpl policy) {
    var sessionInternal = (DatabaseSessionInternal) session;
    sessionInternal.save(policy.getElement(sessionInternal));
  }

  @Override
  public void deleteSecurityPolicy(DatabaseSession session, String name) {
    session
        .command("DELETE FROM " + SecurityPolicy.CLASS_NAME + " WHERE name = ?", name)
        .close();
  }

  @Override
  public void removeSecurityPolicy(DatabaseSession session, Role role, String resource) {
    var sessionInternal = (DatabaseSessionInternal) session;
    String calculatedResource = normalizeSecurityResource(session, resource);
    final Entity roleEntity = session.load(role.getIdentity().getIdentity());
    Map<String, Identifiable> policies = roleEntity.getProperty("policies");
    if (policies == null) {
      return;
    }
    policies.remove(calculatedResource);

    roleEntity.save();

    updateAllFilteredProperties(sessionInternal);
    initPredicateSecurityOptimizations(sessionInternal);
  }

  private static String normalizeSecurityResource(DatabaseSession session, String resource) {
    return resource; // TODO
  }

  public SecurityUserImpl create(final DatabaseSessionInternal session) {
    if (!session.getMetadata().getSchema().getClasses().isEmpty()) {
      return null;
    }

    skipRoleHasPredicateSecurityForClassUpdate = true;
    SecurityUserImpl adminUser = null;
    try {
      SchemaClass identityClass =
          session.getMetadata().getSchema().getClass(Identity.CLASS_NAME); // SINCE 1.2.0
      if (identityClass == null) {
        identityClass = session.getMetadata().getSchema().createAbstractClass(Identity.CLASS_NAME);
      }

      createOrUpdateOSecurityPolicyClass(session);

      SchemaClass roleClass = createOrUpdateORoleClass(session, identityClass);

      createOrUpdateOUserClass(session, identityClass, roleClass);
      createOrUpdateORestrictedClass(session);

      if (!SystemDatabase.SYSTEM_DB_NAME.equals(session.getName())) {
        // CREATE ROLES AND USERS
        createDefaultRoles(session);
        adminUser = createDefaultUsers(session);
      }

    } finally {
      skipRoleHasPredicateSecurityForClassUpdate = false;
    }
    initPredicateSecurityOptimizations(session);

    return adminUser;
  }

  private void createDefaultRoles(final DatabaseSessionInternal session) {
    session.executeInTx(
        () -> {
          createDefaultAdminRole(session);
          createDefaultReaderRole(session);
          createDefaultWriterRole(session);
        });
  }

  private SecurityUserImpl createDefaultUsers(final DatabaseSessionInternal session) {
    boolean createDefUsers =
        session.getConfiguration().getValueAsBoolean(GlobalConfiguration.CREATE_DEFAULT_USERS);

    SecurityUserImpl adminUser = null;
    // This will return the global value if a local storage context configuration value does not
    // exist.
    if (createDefUsers) {
      session.computeInTx(
          () -> {
            var admin = createUser(session, SecurityUserImpl.ADMIN, SecurityUserImpl.ADMIN,
                Role.ADMIN);
            createUser(session, "reader", "reader", DEFAULT_READER_ROLE_NAME);
            createUser(session, "writer", "writer", DEFAULT_WRITER_ROLE_NAME);
            return admin;
          });
    }

    return adminUser;
  }

  private Role createDefaultWriterRole(final DatabaseSessionInternal session) {
    final Role writerRole =
        createRole(session, DEFAULT_WRITER_ROLE_NAME);
    sedDefaultWriterPermissions(session, writerRole);
    return writerRole;
  }

  private void sedDefaultWriterPermissions(final DatabaseSessionInternal session,
      final Role writerRole) {
    setSecurityPolicyWithBitmask(session, writerRole, "database.class.*.*", Role.PERMISSION_ALL);

    writerRole.addRule(session, ResourceGeneric.DATABASE, null, Role.PERMISSION_READ);
    writerRole.addRule(session,
        ResourceGeneric.SCHEMA,
        null, Role.PERMISSION_READ + Role.PERMISSION_CREATE + Role.PERMISSION_UPDATE);
    writerRole.addRule(session,
        ResourceGeneric.CLUSTER,
        MetadataDefault.CLUSTER_INTERNAL_NAME, Role.PERMISSION_READ);
    writerRole.addRule(session, ResourceGeneric.CLASS, null, Role.PERMISSION_ALL);
    writerRole.addRule(session, ResourceGeneric.CLASS, "OUser", Role.PERMISSION_READ);
    writerRole.addRule(session, ResourceGeneric.CLUSTER, null, Role.PERMISSION_ALL);
    writerRole.addRule(session, ResourceGeneric.COMMAND, null, Role.PERMISSION_ALL);
    writerRole.addRule(session, ResourceGeneric.RECORD_HOOK, null, Role.PERMISSION_ALL);
    writerRole.addRule(session, ResourceGeneric.FUNCTION, null, Role.PERMISSION_READ);
    writerRole.addRule(session, ResourceGeneric.CLASS, Sequence.CLASS_NAME,
        Role.PERMISSION_READ);
    writerRole.addRule(session, ResourceGeneric.CLASS, "OTriggered", Role.PERMISSION_READ);
    writerRole.addRule(session, ResourceGeneric.CLASS, "OSchedule", Role.PERMISSION_READ);
    writerRole.addRule(session,
        ResourceGeneric.CLASS,
        SecurityResource.class.getSimpleName(), Role.PERMISSION_READ);
    writerRole.addRule(session, ResourceGeneric.SYSTEM_CLUSTERS, null, Role.PERMISSION_NONE);
    writerRole.save(session);

    setSecurityPolicyWithBitmask(
        session, writerRole, Rule.ResourceGeneric.DATABASE.getLegacyName(), Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.SCHEMA.getLegacyName(),
        Role.PERMISSION_READ + Role.PERMISSION_CREATE + Role.PERMISSION_UPDATE);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName()
            + "."
            + MetadataDefault.CLUSTER_INTERNAL_NAME,
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName() + "." + Role.CLASS_NAME.toLowerCase(
            Locale.ROOT),
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName() + ".ouser",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLASS.getLegacyName() + ".*",
        Role.PERMISSION_ALL);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLASS.getLegacyName() + ".OUser",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName() + ".*",
        Role.PERMISSION_ALL);
    setSecurityPolicyWithBitmask(
        session, writerRole, Rule.ResourceGeneric.COMMAND.getLegacyName(), Role.PERMISSION_ALL);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.RECORD_HOOK.getLegacyName(),
        Role.PERMISSION_ALL);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.FUNCTION.getLegacyName() + ".*",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.CLASS.getLegacyName() + "." + Sequence.CLASS_NAME,
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.SYSTEM_CLUSTERS.getLegacyName() + ".OTriggered",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.SYSTEM_CLUSTERS.getLegacyName() + ".OSchedule",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        writerRole,
        Rule.ResourceGeneric.SYSTEM_CLUSTERS.getLegacyName(),
        Role.PERMISSION_NONE);
  }

  private void createDefaultReaderRole(final DatabaseSessionInternal session) {
    final Role readerRole =
        createRole(session, DEFAULT_READER_ROLE_NAME);
    setDefaultReaderPermissions(session, readerRole);
  }

  private void setDefaultReaderPermissions(final DatabaseSessionInternal session,
      final Role readerRole) {
    setSecurityPolicyWithBitmask(session, readerRole, "database.class.*.*", Role.PERMISSION_ALL);

    readerRole.addRule(session, ResourceGeneric.DATABASE, null, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.SCHEMA, null, Role.PERMISSION_READ);
    readerRole.addRule(session,
        ResourceGeneric.CLUSTER,
        MetadataDefault.CLUSTER_INTERNAL_NAME, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.CLUSTER, Role.CLASS_NAME.toLowerCase(Locale.ROOT),
        Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.CLUSTER, "ouser", Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.CLASS, null, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.CLASS, "OUser", Role.PERMISSION_NONE);
    readerRole.addRule(session, ResourceGeneric.CLUSTER, null, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.COMMAND, null, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.RECORD_HOOK, null, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.FUNCTION, null, Role.PERMISSION_READ);
    readerRole.addRule(session, ResourceGeneric.SYSTEM_CLUSTERS, null, Role.PERMISSION_NONE);

    readerRole.save(session);

    setSecurityPolicyWithBitmask(
        session, readerRole, Rule.ResourceGeneric.DATABASE.getLegacyName(), Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session, readerRole, Rule.ResourceGeneric.SCHEMA.getLegacyName(), Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName()
            + "."
            + MetadataDefault.CLUSTER_INTERNAL_NAME,
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName() + "." + Role.CLASS_NAME.toLowerCase(
            Locale.ROOT),
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName() + ".ouser",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.CLASS.getLegacyName() + ".*",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.CLASS.getLegacyName() + ".OUser",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.CLUSTER.getLegacyName() + ".*",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session, readerRole, Rule.ResourceGeneric.COMMAND.getLegacyName(), Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.RECORD_HOOK.getLegacyName(),
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.FUNCTION.getLegacyName() + ".*",
        Role.PERMISSION_READ);
    setSecurityPolicyWithBitmask(
        session,
        readerRole,
        Rule.ResourceGeneric.SYSTEM_CLUSTERS.getLegacyName(),
        Role.PERMISSION_NONE);
  }

  private void createDefaultAdminRole(final DatabaseSessionInternal session) {
    Role adminRole;
    adminRole = createRole(session, Role.ADMIN);
    setDefaultAdminPermissions(session, adminRole);
  }

  private void setDefaultAdminPermissions(final DatabaseSessionInternal session,
      Role adminRole) {
    setSecurityPolicyWithBitmask(session, adminRole, "*", Role.PERMISSION_ALL);
    adminRole.addRule(session, ResourceGeneric.BYPASS_RESTRICTED, null, Role.PERMISSION_ALL)
        .save(session);
    adminRole.addRule(session, ResourceGeneric.ALL, null, Role.PERMISSION_ALL).save(session);
    adminRole.addRule(session, ResourceGeneric.CLASS, null, Role.PERMISSION_ALL).save(session);
    adminRole.addRule(session, ResourceGeneric.CLUSTER, null, Role.PERMISSION_ALL).save(session);
    adminRole.addRule(session, ResourceGeneric.SYSTEM_CLUSTERS, null, Role.PERMISSION_ALL)
        .save(session);
    adminRole.addRule(session, ResourceGeneric.DATABASE, null, Role.PERMISSION_ALL).save(session);
    adminRole.addRule(session, ResourceGeneric.SCHEMA, null, Role.PERMISSION_ALL).save(session);
    adminRole.addRule(session, ResourceGeneric.COMMAND, null, Role.PERMISSION_ALL).save(session);
    adminRole.addRule(session, ResourceGeneric.COMMAND_GREMLIN, null, Role.PERMISSION_ALL)
        .save(session);
    adminRole.addRule(session, ResourceGeneric.FUNCTION, null, Role.PERMISSION_ALL).save(session);

    adminRole.save(session);
  }

  private static void createOrUpdateORestrictedClass(final DatabaseSessionInternal database) {
    SchemaClassInternal restrictedClass = database.getMetadata().getSchemaInternal()
        .getClassInternal(RESTRICTED_CLASSNAME);
    boolean unsafe = false;
    if (restrictedClass == null) {
      restrictedClass =
          (SchemaClassInternal) database.getMetadata().getSchemaInternal()
              .createAbstractClass(RESTRICTED_CLASSNAME);
      unsafe = true;
    }
    if (!restrictedClass.existsProperty(ALLOW_ALL_FIELD)) {
      restrictedClass.createProperty(database,
          ALLOW_ALL_FIELD,
          PropertyType.LINKSET,
          database.getMetadata().getSchema().getClass(Identity.CLASS_NAME), unsafe);
    }
    if (!restrictedClass.existsProperty(ALLOW_READ_FIELD)) {
      restrictedClass.createProperty(database,
          ALLOW_READ_FIELD,
          PropertyType.LINKSET,
          database.getMetadata().getSchema().getClass(Identity.CLASS_NAME), unsafe);
    }
    if (!restrictedClass.existsProperty(ALLOW_UPDATE_FIELD)) {
      restrictedClass.createProperty(database,
          ALLOW_UPDATE_FIELD,
          PropertyType.LINKSET,
          database.getMetadata().getSchema().getClass(Identity.CLASS_NAME), unsafe);
    }
    if (!restrictedClass.existsProperty(ALLOW_DELETE_FIELD)) {
      restrictedClass.createProperty(database,
          ALLOW_DELETE_FIELD,
          PropertyType.LINKSET,
          database.getMetadata().getSchema().getClass(Identity.CLASS_NAME), unsafe);
    }
  }

  private static void createOrUpdateOUserClass(
      final DatabaseSessionInternal database, SchemaClass identityClass, SchemaClass roleClass) {
    boolean unsafe = false;
    SchemaClassInternal userClass = database.getMetadata().getSchemaInternal()
        .getClassInternal("OUser");
    if (userClass == null) {
      userClass = (SchemaClassInternal) database.getMetadata().getSchema()
          .createClass("OUser", identityClass);
      unsafe = true;
    } else if (!userClass.getSuperClasses().contains(identityClass))
    // MIGRATE AUTOMATICALLY TO 1.2.0
    {
      userClass.setSuperClasses(database, Collections.singletonList(identityClass));
    }

    if (!userClass.existsProperty("name")) {
      userClass
          .createProperty(database, "name", PropertyType.STRING, (PropertyType) null, unsafe)
          .setMandatory(database, true)
          .setNotNull(database, true)
          .setCollate(database, "ci")
          .setMin(database, "1")
          .setRegexp(database, "\\S+(.*\\S+)*");
      userClass.createIndex(database, "OUser.name", INDEX_TYPE.UNIQUE, NullOutputListener.INSTANCE,
          "name");
    } else {
      final PropertyInternal name = userClass.getPropertyInternal("name");
      if (name.getAllIndexes(database).isEmpty()) {
        userClass.createIndex(database,
            "OUser.name", INDEX_TYPE.UNIQUE, NullOutputListener.INSTANCE, "name");
      }
    }
    if (!userClass.existsProperty(SecurityUserImpl.PASSWORD_PROPERTY)) {
      userClass
          .createProperty(database, SecurityUserImpl.PASSWORD_PROPERTY, PropertyType.STRING,
              (PropertyType) null, unsafe)
          .setMandatory(database, true)
          .setNotNull(database, true);
    }
    if (!userClass.existsProperty("roles")) {
      userClass.createProperty(database, "roles", PropertyType.LINKSET, roleClass, unsafe);
    }
    if (!userClass.existsProperty("status")) {
      userClass
          .createProperty(database, "status", PropertyType.STRING, (PropertyType) null, unsafe)
          .setMandatory(database, true)
          .setNotNull(database, true);
    }
  }

  private static void createOrUpdateOSecurityPolicyClass(
      final DatabaseSessionInternal database) {
    SchemaClassInternal policyClass = database.getMetadata().getSchemaInternal()
        .getClassInternal("OSecurityPolicy");
    boolean unsafe = false;
    if (policyClass == null) {
      policyClass = (SchemaClassInternal) database.getMetadata().getSchema()
          .createClass("OSecurityPolicy");
      unsafe = true;
    }

    if (!policyClass.existsProperty("name")) {
      policyClass
          .createProperty(database, "name", PropertyType.STRING, (PropertyType) null, unsafe)
          .setMandatory(database, true)
          .setNotNull(database, true)
          .setCollate(database, "ci");
      policyClass.createIndex(database,
          "OSecurityPolicy.name", INDEX_TYPE.UNIQUE, NullOutputListener.INSTANCE, "name");
    } else {
      final PropertyInternal name = policyClass.getPropertyInternal("name");
      if (name.getAllIndexes(database).isEmpty()) {
        policyClass.createIndex(database,
            "OSecurityPolicy.name", INDEX_TYPE.UNIQUE, NullOutputListener.INSTANCE, "name");
      }
    }

    if (!policyClass.existsProperty("create")) {
      policyClass.createProperty(database, "create", PropertyType.STRING, (PropertyType) null,
          unsafe);
    }
    if (!policyClass.existsProperty("read")) {
      policyClass.createProperty(database, "read", PropertyType.STRING, (PropertyType) null,
          unsafe);
    }
    if (!policyClass.existsProperty("beforeUpdate")) {
      policyClass.createProperty(database, "beforeUpdate", PropertyType.STRING, (PropertyType) null,
          unsafe);
    }
    if (!policyClass.existsProperty("afterUpdate")) {
      policyClass.createProperty(database, "afterUpdate", PropertyType.STRING, (PropertyType) null,
          unsafe);
    }
    if (!policyClass.existsProperty("delete")) {
      policyClass.createProperty(database, "delete", PropertyType.STRING, (PropertyType) null,
          unsafe);
    }
    if (!policyClass.existsProperty("execute")) {
      policyClass.createProperty(database, "execute", PropertyType.STRING, (PropertyType) null,
          unsafe);
    }

    if (!policyClass.existsProperty("active")) {
      policyClass.createProperty(database, "active", PropertyType.BOOLEAN, (PropertyType) null,
          unsafe);
    }

  }

  private static SchemaClass createOrUpdateORoleClass(final DatabaseSessionInternal database,
      SchemaClass identityClass) {
    SchemaClassInternal roleClass = database.getMetadata().getSchemaInternal()
        .getClassInternal(Role.CLASS_NAME);
    boolean unsafe = false;
    if (roleClass == null) {
      roleClass = (SchemaClassInternal) database.getMetadata().getSchema()
          .createClass(Role.CLASS_NAME, identityClass);
      unsafe = true;
    } else if (!roleClass.getSuperClasses().contains(identityClass))
    // MIGRATE AUTOMATICALLY TO 1.2.0
    {
      roleClass.setSuperClasses(database, Collections.singletonList(identityClass));
    }

    if (!roleClass.existsProperty("name")) {
      roleClass
          .createProperty(database, "name", PropertyType.STRING, (PropertyType) null, unsafe)
          .setMandatory(database, true)
          .setNotNull(database, true)
          .setCollate(database, "ci");
      roleClass.createIndex(database, Role.CLASS_NAME + "." + Role.NAME, INDEX_TYPE.UNIQUE,
          NullOutputListener.INSTANCE,
          "name");
    } else {
      final PropertyInternal name = roleClass.getPropertyInternal("name");
      if (name.getAllIndexes(database).isEmpty()) {
        roleClass.createIndex(database,
            "ORole.name", INDEX_TYPE.UNIQUE, NullOutputListener.INSTANCE, "name");
      }
    }

    if (!roleClass.existsProperty("mode")) {
      roleClass.createProperty(database, "mode", PropertyType.BYTE, (PropertyType) null, unsafe);
    }

    if (!roleClass.existsProperty("rules")) {
      roleClass.createProperty(database, "rules", PropertyType.EMBEDDEDMAP, PropertyType.BYTE,
          unsafe);
    }
    if (!roleClass.existsProperty("inheritedRole")) {
      roleClass.createProperty(database, "inheritedRole", PropertyType.LINK, roleClass, unsafe);
    }

    if (!roleClass.existsProperty("policies")) {
      roleClass.createProperty(database,
          "policies", PropertyType.LINKMAP, database.getClass("OSecurityPolicy"), unsafe);
    }

    return roleClass;
  }

  public void load(DatabaseSessionInternal session) {
    final SchemaClassInternal userClass = session.getMetadata().getSchema()
        .getClassInternal("OUser");
    if (userClass != null) {
      // @COMPATIBILITY <1.3.0
      if (!userClass.existsProperty("status")) {
        userClass.createProperty(session, "status", PropertyType.STRING).setMandatory(session, true)
            .setNotNull(session, true);
      }
      Property p = userClass.getProperty("name");
      if (p == null) {
        p =
            userClass
                .createProperty(session, "name", PropertyType.STRING)
                .setMandatory(session, true)
                .setNotNull(session, true)
                .setMin(session, "1")
                .setRegexp(session, "\\S+(.*\\S+)*");
      }

      if (userClass.getInvolvedIndexes(session, "name") == null) {
        p.createIndex(session, INDEX_TYPE.UNIQUE);
      }

      // ROLE
      Role.generateSchema(session);

      // TODO migrate Role to use security policies
    }

    setupPredicateSecurity(session);
    initPredicateSecurityOptimizations(session);
  }

  private void setupPredicateSecurity(DatabaseSessionInternal session) {
    SchemaClass securityPolicyClass = session.getMetadata().getSchema()
        .getClass(SecurityPolicy.CLASS_NAME);
    if (securityPolicyClass == null) {
      createOrUpdateOSecurityPolicyClass(session);
      Role adminRole = getRole(session, "admin");
      if (adminRole != null) {
        setDefaultAdminPermissions(session, adminRole);
      }
      Role readerRole = getRole(session, DEFAULT_READER_ROLE_NAME);
      if (readerRole != null) {
        setDefaultReaderPermissions(session, readerRole);
      }

      Role writerRole = getRole(session, DEFAULT_WRITER_ROLE_NAME);
      if (writerRole != null) {
        sedDefaultWriterPermissions(session, writerRole);
      }

      incrementVersion(session);
    }
  }

  public void createClassTrigger(DatabaseSessionInternal session) {
    SchemaClass classTrigger = session.getMetadata().getSchema().getClass(ClassTrigger.CLASSNAME);
    if (classTrigger == null) {
      session.getMetadata().getSchema().createAbstractClass(ClassTrigger.CLASSNAME);
    }
  }

  public static SecurityUserImpl getUserInternal(final DatabaseSession session,
      final String iUserName) {
    try (ResultSet result =
        session.query("select from OUser where name = ? limit 1", iUserName)) {
      if (result.hasNext()) {
        return new SecurityUserImpl((DatabaseSessionInternal) session,
            (EntityImpl) result.next().getEntity().get());
      }
    }

    return null;
  }

  @Override
  public SecurityUserImpl getUser(DatabaseSession session, String username) {
    return getUserInternal(session, username);
  }

  public static SecurityRole createRole(GlobalUser serverUser) {

    final SecurityRole role;
    if (serverUser.getResources().equalsIgnoreCase("*")) {
      role = createRoot(serverUser);
    } else {
      Map<ResourceGeneric, Rule> permissions = mapPermission(serverUser);
      role = new ImmutableRole(null, serverUser.getName(), permissions, null);
    }

    return role;
  }

  private static SecurityRole createRoot(GlobalUser serverUser) {
    Map<String, ImmutableSecurityPolicy> policies = createrRootSecurityPolicy("*");
    Map<Rule.ResourceGeneric, Rule> rules = new HashMap<Rule.ResourceGeneric, Rule>();
    for (Rule.ResourceGeneric resource : Rule.ResourceGeneric.values()) {
      Rule rule = new Rule(resource, null, null);
      rule.grantAccess(null, Role.PERMISSION_ALL);
      rules.put(resource, rule);
    }

    return new ImmutableRole(null, serverUser.getName(), rules, policies);
  }

  private static Map<ResourceGeneric, Rule> mapPermission(GlobalUser user) {
    Map<Rule.ResourceGeneric, Rule> rules = new HashMap<Rule.ResourceGeneric, Rule>();
    String[] strings = user.getResources().split(",");

    for (String string : strings) {
      Rule.ResourceGeneric generic = Rule.mapLegacyResourceToGenericResource(string);
      if (generic != null) {
        Rule rule = new Rule(generic, null, null);
        rule.grantAccess(null, Role.PERMISSION_ALL);
        rules.put(generic, rule);
      }
    }
    return rules;
  }

  public static Map<String, ImmutableSecurityPolicy> createrRootSecurityPolicy(String resource) {
    Map<String, ImmutableSecurityPolicy> policies =
        new HashMap<String, ImmutableSecurityPolicy>();
    policies.put(
        resource,
        new ImmutableSecurityPolicy(resource, "true", "true", "true", "true", "true", "true"));
    return policies;
  }

  public static RID getUserRID(final DatabaseSession session, final String userName) {
    try (ResultSet result =
        session.query("select @rid as rid from OUser where name = ? limit 1", userName)) {

      if (result.hasNext()) {
        return result.next().getProperty("rid");
      }
    }

    return null;
  }

  @Override
  public void close() {
  }

  @Override
  public long getVersion(final DatabaseSession session) {
    return version.get();
  }

  @Override
  public void incrementVersion(final DatabaseSession session) {
    var sessionInternal = (DatabaseSessionInternal) session;
    version.incrementAndGet();
    securityPredicateCache.clear();
    updateAllFilteredProperties(sessionInternal);
    initPredicateSecurityOptimizations(sessionInternal);
  }

  protected void initPredicateSecurityOptimizations(DatabaseSessionInternal session) {
    if (skipRoleHasPredicateSecurityForClassUpdate) {
      return;
    }
    SecurityUser user = session.geCurrentUser();
    try {
      if (user != null) {
        session.setUser(null);
      }

      initPredicateSecurityOptimizationsInternal(session);
    } finally {

      if (user != null) {
        session.setUser(user);
      }
    }
  }

  private void initPredicateSecurityOptimizationsInternal(DatabaseSessionInternal session) {
    Map<String, Map<String, Boolean>> result = new HashMap<>();
    Collection<SchemaClass> allClasses = session.getMetadata().getSchema().getClasses();

    if (!session
        .getMetadata()
        .getImmutableSchemaSnapshot()
        .existsClass(Role.CLASS_NAME)) {
      return;
    }

    session.executeInTx(
        () -> {
          synchronized (this) {
            try (ResultSet rs = session.query("select name, policies from " + Role.CLASS_NAME)) {
              while (rs.hasNext()) {
                Result item = rs.next();
                String roleName = item.getProperty("name");

                Map<String, Identifiable> policies = item.getProperty("policies");
                if (policies != null) {
                  for (Map.Entry<String, Identifiable> policyEntry : policies.entrySet()) {
                    SecurityResource res = SecurityResource.getInstance(policyEntry.getKey());
                    try {
                      Entity policy = policyEntry.getValue().getRecord(session);

                      for (SchemaClass clazz : allClasses) {
                        if (isClassInvolved(clazz, res)
                            && !isAllAllowed(
                            session,
                            new ImmutableSecurityPolicy(session,
                                new SecurityPolicyImpl(policy)))) {
                          Map<String, Boolean> roleMap =
                              result.computeIfAbsent(roleName, k -> new HashMap<>());
                          roleMap.put(clazz.getName(), true);
                        }
                      }
                    } catch (RecordNotFoundException rne) {
                      // ignore
                    }
                  }
                }
              }
            }
            this.roleHasPredicateSecurityForClass = result;
          }
        });
  }

  private boolean isAllAllowed(DatabaseSessionInternal db, SecurityPolicy policy) {
    for (SecurityPolicy.Scope scope : SecurityPolicy.Scope.values()) {
      String predicateString = policy.get(scope, db);
      if (predicateString == null) {
        continue;
      }
      SQLBooleanExpression predicate = SecurityEngine.parsePredicate(db, predicateString);
      if (!predicate.isAlwaysTrue()) {
        return false;
      }
    }
    return true;
  }

  private boolean isClassInvolved(SchemaClass clazz, SecurityResource res) {

    if (res instanceof SecurityResourceAll
        || res.equals(SecurityResourceClass.ALL_CLASSES)
        || res.equals(SecurityResourceProperty.ALL_PROPERTIES)) {
      return true;
    }
    if (res instanceof SecurityResourceClass) {
      String resourceClass = ((SecurityResourceClass) res).getClassName();
      return clazz.isSubClassOf(resourceClass);
    } else if (res instanceof SecurityResourceProperty) {
      String resourceClass = ((SecurityResourceProperty) res).getClassName();
      return clazz.isSubClassOf(resourceClass);
    }
    return false;
  }

  @Override
  public Set<String> getFilteredProperties(DatabaseSessionInternal session,
      EntityImpl entity) {
    if (session.geCurrentUser() == null) {
      return Collections.emptySet();
    }
    SchemaImmutableClass clazz = EntityInternalUtils.getImmutableSchemaClass(entity);
    if (clazz == null) {
      return Collections.emptySet();
    }
    if (clazz.isSecurityPolicy()) {
      return Collections.emptySet();
    }

    if (roleHasPredicateSecurityForClass != null) {
      for (SecurityRole role : session.geCurrentUser().getRoles()) {

        Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(role.getName(session));
        if (roleMap == null) {
          return Collections.emptySet(); // TODO hierarchy...?
        }
        Boolean val = roleMap.get(clazz.getName());
        if (!(Boolean.TRUE.equals(val))) {
          return Collections.emptySet(); // TODO hierarchy...?
        }
      }
    }
    var props = entity.getPropertyNamesInternal();
    Set<String> result = new HashSet<>();

    for (String prop : props) {
      SQLBooleanExpression predicate =
          SecurityEngine.getPredicateForSecurityResource(
              session,
              this,
              "database.class.`" + clazz.getName() + "`.`" + prop + "`",
              SecurityPolicy.Scope.READ);
      if (!SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, entity)) {
        result.add(prop);
      }
    }
    return result;
  }

  @Override
  public boolean isAllowedWrite(DatabaseSessionInternal session, EntityImpl entity,
      String propertyName) {

    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return true;
    }

    String className;
    SchemaClass clazz = null;
    if (entity instanceof EntityImpl) {
      className = entity.getClassName();
    } else {
      clazz = entity.getSchemaType().orElse(null);
      className = clazz == null ? null : clazz.getName();
    }
    if (className == null) {
      return true;
    }

    if (roleHasPredicateSecurityForClass != null) {
      for (SecurityRole role : session.geCurrentUser().getRoles()) {
        Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(role.getName(session));
        if (roleMap == null) {
          return true; // TODO hierarchy...?
        }
        Boolean val = roleMap.get(className);
        if (!(Boolean.TRUE.equals(val))) {
          return true; // TODO hierarchy...?
        }
      }
    }

    var sessionInternal = session;
    if (entity.getIdentity().isNew()) {
      SQLBooleanExpression predicate =
          SecurityEngine.getPredicateForSecurityResource(
              sessionInternal,
              this,
              "database.class.`" + className + "`.`" + propertyName + "`",
              SecurityPolicy.Scope.CREATE);
      return SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, entity);
    } else {

      SQLBooleanExpression readPredicate =
          SecurityEngine.getPredicateForSecurityResource(
              sessionInternal,
              this,
              "database.class.`" + className + "`.`" + propertyName + "`",
              SecurityPolicy.Scope.READ);
      if (!SecurityEngine.evaluateSecuirtyPolicyPredicate(session, readPredicate, entity)) {
        return false;
      }

      SQLBooleanExpression beforePredicate =
          SecurityEngine.getPredicateForSecurityResource(
              sessionInternal,
              this,
              "database.class.`" + className + "`.`" + propertyName + "`",
              SecurityPolicy.Scope.BEFORE_UPDATE);
      ResultInternal originalRecord = calculateOriginalValue(entity,
          session);

      if (!SecurityEngine.evaluateSecuirtyPolicyPredicate(
          session, beforePredicate, originalRecord)) {
        return false;
      }

      SQLBooleanExpression predicate =
          SecurityEngine.getPredicateForSecurityResource(
              sessionInternal,
              this,
              "database.class.`" + className + "`.`" + propertyName + "`",
              SecurityPolicy.Scope.AFTER_UPDATE);
      return SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, entity);
    }
  }

  @Override
  public boolean canCreate(DatabaseSessionInternal session, Record record) {
    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return true;
    }

    if (record instanceof Entity) {
      String className;
      if (record instanceof EntityImpl) {
        className = ((EntityImpl) record).getClassName();
      } else {
        className = ((Entity) record).getSchemaType().map(SchemaClass::getName).orElse(null);
      }

      if (roleHasPredicateSecurityForClass != null) {
        for (SecurityRole role : session.geCurrentUser().getRoles()) {
          Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(
              role.getName(session));
          if (roleMap == null) {
            return true; // TODO hierarchy...?
          }
          Boolean val = roleMap.get(className);
          if (!(Boolean.TRUE.equals(val))) {
            return true; // TODO hierarchy...?
          }
        }
      }

      SQLBooleanExpression predicate;
      if (className == null) {
        predicate = null;
      } else {
        predicate =
            SecurityEngine.getPredicateForSecurityResource(
                session, this, "database.class.`" + className + "`",
                SecurityPolicy.Scope.CREATE);
      }
      return SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, record);
    }
    return true;
  }

  @Override
  public boolean canRead(DatabaseSessionInternal session, Record record) {
    // TODO what about server users?
    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return true;
    }

    var sessionInternal = session;
    if (record instanceof Entity) {
      SchemaImmutableClass clazz = EntityInternalUtils.getImmutableSchemaClass((EntityImpl) record);
      if (clazz == null) {
        return true;
      }
      if (clazz.isSecurityPolicy()) {
        return true;
      }

      if (roleHasPredicateSecurityForClass != null) {
        for (SecurityRole role : session.geCurrentUser().getRoles()) {
          Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(
              role.getName(session));
          if (roleMap == null) {
            return true; // TODO hierarchy...?
          }
          Boolean val = roleMap.get(((EntityImpl) record).getClassName());
          if (!(Boolean.TRUE.equals(val))) {
            return true; // TODO hierarchy...?
          }
        }
      }

      SQLBooleanExpression predicate =
          SecurityEngine.getPredicateForSecurityResource(
              sessionInternal,
              this,
              "database.class.`" + ((EntityImpl) record).getClassName() + "`",
              SecurityPolicy.Scope.READ);
      return SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, record);
    }
    return true;
  }

  @Override
  public boolean canUpdate(DatabaseSessionInternal session, Record record) {
    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return true;
    }
    if (record instanceof Entity) {

      String className;
      if (record instanceof EntityImpl) {
        className = ((EntityImpl) record).getClassName();
      } else {
        className = ((Entity) record).getSchemaType().map(x -> x.getName()).orElse(null);
      }

      if (className != null && roleHasPredicateSecurityForClass != null) {
        for (SecurityRole role : session.geCurrentUser().getRoles()) {
          Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(
              role.getName(session));
          if (roleMap == null) {
            return true; // TODO hierarchy...?
          }
          Boolean val = roleMap.get(className);
          if (!(Boolean.TRUE.equals(val))) {
            return true; // TODO hierarchy...?
          }
        }
      }

      var sessionInternal = session;
      SQLBooleanExpression beforePredicate = null;
      if (className != null) {
        beforePredicate =
            SecurityEngine.getPredicateForSecurityResource(
                sessionInternal,
                this,
                "database.class.`" + className + "`",
                SecurityPolicy.Scope.BEFORE_UPDATE);
      }

      // TODO avoid calculating original valueif not needed!!!

      ResultInternal originalRecord = calculateOriginalValue(record, sessionInternal);
      if (!SecurityEngine.evaluateSecuirtyPolicyPredicate(
          session, beforePredicate, originalRecord)) {
        return false;
      }

      SQLBooleanExpression predicate = null;
      if (className != null) {
        predicate =
            SecurityEngine.getPredicateForSecurityResource(
                sessionInternal,
                this,
                "database.class.`" + className + "`",
                SecurityPolicy.Scope.AFTER_UPDATE);
      }
      return SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, record);
    }
    return true;
  }

  private ResultInternal calculateOriginalValue(Record record, DatabaseSessionInternal db) {
    return calculateBefore(record.getRecord(db), db);
  }

  public static ResultInternal calculateBefore(EntityImpl entity,
      DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    for (String prop : entity.getPropertyNamesInternal()) {
      result.setProperty(prop, unboxRidbags(entity.getProperty(prop)));
    }
    result.setProperty("@rid", entity.getIdentity());
    result.setProperty("@class", entity.getClassName());
    result.setProperty("@version", entity.getVersion());
    for (String prop : entity.getDirtyFields()) {
      result.setProperty(prop, convert(entity.getOriginalValue(prop)));
    }
    return result;
  }

  private static Object convert(Object originalValue) {
    if (originalValue instanceof RidBag) {
      Set result = new LinkedHashSet<>();
      ((RidBag) originalValue).iterator().forEachRemaining(result::add);
      return result;
    }
    return originalValue;
  }

  public static Object unboxRidbags(Object value) {
    // TODO move it to some helper class
    if (value instanceof RidBag) {
      List<Identifiable> result = new ArrayList<>(((RidBag) value).size());
      for (Identifiable identifiable : (RidBag) value) {
        result.add(identifiable);
      }

      return result;
    }
    return value;
  }

  @Override
  public boolean canDelete(DatabaseSessionInternal session, Record record) {
    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return true;
    }

    if (record instanceof Entity) {
      String className;
      if (record instanceof EntityImpl) {
        className = ((EntityImpl) record).getClassName();
      } else {
        className = ((Entity) record).getSchemaType().map(SchemaClass::getName).orElse(null);
      }

      if (roleHasPredicateSecurityForClass != null) {
        for (SecurityRole role : session.geCurrentUser().getRoles()) {
          Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(
              role.getName(session));
          if (roleMap == null) {
            return true; // TODO hierarchy...?
          }
          Boolean val = roleMap.get(className);
          if (!(Boolean.TRUE.equals(val))) {
            return true; // TODO hierarchy...?
          }
        }
      }

      SQLBooleanExpression predicate = null;
      if (className != null) {
        predicate =
            SecurityEngine.getPredicateForSecurityResource(
                session, this, "database.class.`" + className + "`",
                SecurityPolicy.Scope.DELETE);
      }

      return SecurityEngine.evaluateSecuirtyPolicyPredicate(session, predicate, record);
    }
    return true;
  }

  @Override
  public boolean canExecute(DatabaseSessionInternal session, Function function) {
    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return true;
    }

    SQLBooleanExpression predicate =
        SecurityEngine.getPredicateForSecurityResource(
            session,
            this,
            "database.function." + function.getName(),
            SecurityPolicy.Scope.EXECUTE);
    if (predicate == null) {
      return true;
    }

    return SecurityEngine.evaluateSecuirtyPolicyPredicate(
        session, predicate, function.getIdentity().getEntity(session));
  }

  protected SQLBooleanExpression getPredicateFromCache(String roleName, String key) {
    Map<String, SQLBooleanExpression> roleMap = this.securityPredicateCache.get(roleName);
    if (roleMap == null) {
      return null;
    }
    SQLBooleanExpression result = roleMap.get(key.toLowerCase(Locale.ENGLISH));
    if (result != null) {
      return result.copy();
    }
    return null;
  }

  protected void putPredicateInCache(DatabaseSessionInternal session, String roleName, String key,
      SQLBooleanExpression predicate) {
    if (predicate.isCacheable(session)) {
      Map<String, SQLBooleanExpression> roleMap = this.securityPredicateCache.get(roleName);
      if (roleMap == null) {
        roleMap = new ConcurrentHashMap<>();
        this.securityPredicateCache.put(roleName, roleMap);
      }

      roleMap.put(key.toLowerCase(Locale.ENGLISH), predicate);
    }
  }

  @Override
  public boolean isReadRestrictedBySecurityPolicy(DatabaseSession session, String resource) {
    if (session.geCurrentUser() == null) {
      // executeNoAuth
      return false;
    }

    var sessionInternal = (DatabaseSessionInternal) session;
    SQLBooleanExpression predicate =
        SecurityEngine.getPredicateForSecurityResource(
            sessionInternal, this, resource, SecurityPolicy.Scope.READ);
    return predicate != null && !SQLBooleanExpression.TRUE.equals(predicate);
  }

  @Override
  public synchronized Set<SecurityResourceProperty> getAllFilteredProperties(
      DatabaseSessionInternal database) {
    if (filteredProperties == null) {
      updateAllFilteredProperties(database);
    }
    if (filteredProperties == null) {
      return Collections.emptySet();
    }
    return new HashSet<>(filteredProperties);
  }

  protected void updateAllFilteredProperties(DatabaseSessionInternal session) {
    Set<SecurityResourceProperty> result;
    if (session.geCurrentUser() == null) {
      result = calculateAllFilteredProperties(session);
      synchronized (this) {
        filteredProperties = result;
      }

    } else {
      synchronized (this) {
        if (filteredProperties == null) {
          filteredProperties = new HashSet<>();
        }
        updateAllFilteredPropertiesInternal(session);
      }
    }
  }

  protected void updateAllFilteredPropertiesInternal(DatabaseSessionInternal session) {
    SecurityUser user = session.geCurrentUser();
    try {
      if (user != null) {
        session.setUser(null);
      }

      synchronized (SecurityShared.this) {
        filteredProperties.clear();
        filteredProperties.addAll(calculateAllFilteredProperties(session));
      }
    } finally {
      if (user != null) {
        session.setUser(user);
      }
    }
  }

  protected Set<SecurityResourceProperty> calculateAllFilteredProperties(
      DatabaseSessionInternal db) {
    Set<SecurityResourceProperty> result = new HashSet<>();
    if (!db
        .getMetadata()
        .getImmutableSchemaSnapshot()
        .existsClass(Role.CLASS_NAME)) {
      return Collections.emptySet();
    }
    try (ResultSet rs = db.query("select policies from " + Role.CLASS_NAME)) {
      while (rs.hasNext()) {
        Result item = rs.next();
        Map<String, Identifiable> policies = item.getProperty("policies");
        if (policies != null) {
          for (Map.Entry<String, Identifiable> policyEntry : policies.entrySet()) {
            try {
              SecurityResource res = SecurityResource.getInstance(policyEntry.getKey());
              if (res instanceof SecurityResourceProperty) {
                final Entity element = policyEntry.getValue().getRecord(db);
                final SecurityPolicy policy =
                    new ImmutableSecurityPolicy(db, new SecurityPolicyImpl(element));
                final String readRule = policy.getReadRule(db);
                if (readRule != null && !readRule.trim().equalsIgnoreCase("true")) {
                  result.add((SecurityResourceProperty) res);
                }
              }
            } catch (RecordNotFoundException e) {
              // ignore
            } catch (Exception e) {
              LogManager.instance().error(this, "Error on loading security policy", e);
            }
          }
        }
      }
    }
    return result;
  }

  public boolean couldHaveActivePredicateSecurityRoles(DatabaseSession session,
      String className) {
    if (session.geCurrentUser() == null) {
      return false;
    }
    if (roleHasPredicateSecurityForClass != null) {
      for (SecurityRole role : session.geCurrentUser().getRoles()) {
        Map<String, Boolean> roleMap = roleHasPredicateSecurityForClass.get(role.getName(session));
        if (roleMap == null) {
          return false; // TODO hierarchy...?
        }
        Boolean val = roleMap.get(className);
        if (Boolean.TRUE.equals(val)) {
          return true; // TODO hierarchy...?
        }
      }

      return false;
    }
    return true;
  }
}
