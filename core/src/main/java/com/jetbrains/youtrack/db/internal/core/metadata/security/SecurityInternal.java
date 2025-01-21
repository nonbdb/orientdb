package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Record;
import com.jetbrains.youtrack.db.api.security.SecurityUser;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;
import com.jetbrains.youtrack.db.internal.core.metadata.security.auth.AuthenticationInfo;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SecurityInternal {

  boolean isAllowed(
      DatabaseSessionInternal session, Set<Identifiable> iAllowAll,
      Set<Identifiable> iAllowOperation);

  @Deprecated
  Identifiable allowUser(
      DatabaseSession session,
      EntityImpl entity,
      RestrictedOperation iOperationType,
      String iUserName);

  @Deprecated
  Identifiable allowRole(
      DatabaseSession session,
      EntityImpl entity,
      RestrictedOperation iOperationType,
      String iRoleName);

  @Deprecated
  Identifiable denyUser(
      DatabaseSessionInternal session,
      EntityImpl entity,
      RestrictedOperation iOperationType,
      String iUserName);

  @Deprecated
  Identifiable denyRole(
      DatabaseSessionInternal session,
      EntityImpl entity,
      RestrictedOperation iOperationType,
      String iRoleName);

  @Deprecated
  Identifiable allowIdentity(
      DatabaseSession session, EntityImpl entity, String iAllowFieldName,
      Identifiable iId);

  @Deprecated
  Identifiable disallowIdentity(
      DatabaseSessionInternal session, EntityImpl entity, String iAllowFieldName,
      Identifiable iId);

  SecurityUserImpl authenticate(DatabaseSessionInternal session, String iUsername,
      String iUserPassword);

  SecurityUserImpl createUser(
      DatabaseSessionInternal session, String iUserName, String iUserPassword, String[] iRoles);

  SecurityUserImpl createUser(
      DatabaseSessionInternal session, String iUserName, String iUserPassword, Role[] iRoles);

  SecurityUserImpl authenticate(DatabaseSessionInternal session, Token authToken);

  Role createRole(
      DatabaseSessionInternal session,
      String iRoleName,
      Role iParent);

  Role createRole(
      DatabaseSessionInternal session, String iRoleName);

  SecurityUserImpl getUser(DatabaseSession session, String iUserName);

  SecurityUserImpl getUser(DatabaseSession session, RID userId);

  Role getRole(DatabaseSession session, String iRoleName);

  Role getRole(DatabaseSession session, Identifiable iRoleRid);

  List<EntityImpl> getAllUsers(DatabaseSession session);

  List<EntityImpl> getAllRoles(DatabaseSession session);

  Map<String, ? extends SecurityPolicy> getSecurityPolicies(DatabaseSession session,
      SecurityRole role);

  /**
   * Returns the security policy policy assigned to a role for a specific resource (not recursive on
   * superclasses, nor on role hierarchy)
   *
   * @param session  an active DB session
   * @param role     the role
   * @param resource the string representation of the security resource, eg.
   *                 "database.class.Person"
   * @return
   */
  SecurityPolicy getSecurityPolicy(DatabaseSession session, SecurityRole role, String resource);

  /**
   * Sets a security policy for a specific resource on a role
   *
   * @param session  a valid db session to perform the operation (that has permissions to do it)
   * @param role     The role
   * @param resource the string representation of the security resource, eg.
   *                 "database.class.Person"
   * @param policy   The security policy
   */
  void setSecurityPolicy(
      DatabaseSessionInternal session, SecurityRole role, String resource,
      SecurityPolicyImpl policy);

  /**
   * creates and saves an empty security policy
   *
   * @param session the session to a DB where the policy has to be created
   * @param name    the policy name
   * @return
   */
  SecurityPolicyImpl createSecurityPolicy(DatabaseSession session, String name);

  SecurityPolicyImpl getSecurityPolicy(DatabaseSession session, String name);

  void saveSecurityPolicy(DatabaseSession session, SecurityPolicyImpl policy);

  void deleteSecurityPolicy(DatabaseSession session, String name);

  /**
   * Removes security policy bound to a role for a specific resource
   *
   * @param session  A valid db session to perform the operation
   * @param role     the role
   * @param resource the string representation of the security resource, eg.
   *                 "database.class.Person"
   */
  void removeSecurityPolicy(DatabaseSession session, Role role, String resource);

  boolean dropUser(DatabaseSession session, String iUserName);

  boolean dropRole(DatabaseSession session, String iRoleName);

  void createClassTrigger(DatabaseSessionInternal session);

  long getVersion(DatabaseSession session);

  void incrementVersion(DatabaseSession session);

  SecurityUserImpl create(DatabaseSessionInternal session);

  void load(DatabaseSessionInternal session);

  void close();

  /**
   * For property-level security. Returns the list of the properties that are hidden (ie. not
   * allowed to be read) for current session, regarding a specific entity
   *
   * @param session the db session
   * @param entity  the entity to filter
   * @return the list of the properties that are hidden (ie. not allowed to be read) on current
   * entity for current session
   */
  Set<String> getFilteredProperties(DatabaseSessionInternal session, EntityImpl entity);

  /**
   * For property-level security
   *
   * @param session
   * @param entity       current entity to check for proeprty-level security
   * @param propertyName the property to check for write access
   * @return
   */
  boolean isAllowedWrite(DatabaseSessionInternal session, EntityImpl entity,
      String propertyName);

  boolean canCreate(DatabaseSessionInternal session, Record record);

  boolean canRead(DatabaseSessionInternal session, Record record);

  boolean canUpdate(DatabaseSessionInternal session, Record record);

  boolean canDelete(DatabaseSessionInternal session, Record record);

  boolean canExecute(DatabaseSessionInternal session, Function function);

  /**
   * checks if for current session a resource is restricted by security resources (ie. READ policies
   * exist, with predicate different from "TRUE", to access the given resource
   *
   * @param session  The session to check for the existece of policies
   * @param resource a resource string, eg. "database.class.Person"
   * @return true if a restriction of any type exists for this session and this resource. False
   * otherwise
   */
  boolean isReadRestrictedBySecurityPolicy(DatabaseSession session, String resource);

  /**
   * returns the list of all the filtered properties (for any role defined in the db)
   *
   * @param database
   * @return
   */
  Set<SecurityResourceProperty> getAllFilteredProperties(DatabaseSessionInternal database);

  SecurityUser securityAuthenticate(DatabaseSessionInternal session, String userName,
      String password);

  SecurityUser securityAuthenticate(
      DatabaseSessionInternal session, AuthenticationInfo authenticationInfo);
}
