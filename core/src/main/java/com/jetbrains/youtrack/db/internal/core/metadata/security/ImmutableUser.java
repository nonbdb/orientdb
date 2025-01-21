package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.api.exception.SecurityAccessException;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.security.SecurityUser;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule.ResourceGeneric;
import com.jetbrains.youtrack.db.internal.core.security.SecurityManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @since 03/11/14
 */
public class ImmutableUser implements SecurityUser {

  private final long version;

  private final String name;
  private final String password;

  private final Set<ImmutableRole> roles = new HashSet<ImmutableRole>();

  private final STATUSES status;
  private final RID rid;
  private final String userType;

  public ImmutableUser(DatabaseSessionInternal session, long version, SecurityUser user) {
    this.version = version;
    this.name = user.getName(session);
    this.password = user.getPassword(session);
    this.status = user.getAccountStatus(session);
    this.rid = user.getIdentity().getIdentity();
    this.userType = user.getUserType();

    for (SecurityRole role : user.getRoles()) {
      roles.add(new ImmutableRole(session, role));
    }
  }

  public ImmutableUser(DatabaseSessionInternal session, String name, String userType) {
    this(session, name, "", userType, null);
  }

  public ImmutableUser(DatabaseSessionInternal session, String name, String password,
      String userType, SecurityRole role) {
    this.version = 0;
    this.name = name;
    this.password = password;
    this.status = STATUSES.ACTIVE;
    this.rid = new RecordId(-1, -1);
    this.userType = userType;
    if (role != null) {
      ImmutableRole immutableRole;
      if (role instanceof ImmutableRole) {
        immutableRole = (ImmutableRole) role;
      } else {
        immutableRole = new ImmutableRole(session, role);
      }
      roles.add(immutableRole);
    }
  }

  public SecurityRole allow(
      DatabaseSessionInternal session, final ResourceGeneric resourceGeneric,
      final String resourceSpecific,
      final int iOperation) {
    if (roles.isEmpty()) {
      throw new SecurityAccessException(name, "User '" + name + "' has no role defined");
    }

    final SecurityRole role = checkIfAllowed(session, resourceGeneric, resourceSpecific,
        iOperation);

    if (role == null) {
      throw new SecurityAccessException(
          name,
          "User '"
              + name
              + "' does not have permission to execute the operation '"
              + Role.permissionToString(iOperation)
              + "' against the resource: "
              + resourceGeneric
              + "."
              + resourceSpecific);
    }

    return role;
  }

  public SecurityRole checkIfAllowed(
      DatabaseSessionInternal session, final ResourceGeneric resourceGeneric,
      final String resourceSpecific,
      final int iOperation) {
    for (ImmutableRole r : roles) {
      if (r == null) {
        LogManager.instance()
            .warn(
                this,
                "User '%s' has a null role, ignoring it.  Consider fixing this user's roles before"
                    + " continuing",
                name);
      } else if (r.allow(resourceGeneric, resourceSpecific, iOperation)) {
        return r;
      }
    }

    return null;
  }

  public boolean isRuleDefined(
      DatabaseSessionInternal session, final ResourceGeneric resourceGeneric,
      String resourceSpecific) {
    for (ImmutableRole r : roles) {
      if (r == null) {
        LogManager.instance()
            .warn(
                this,
                "UseOSecurityAuthenticatorr '%s' has a null role, ignoring it.  Consider fixing"
                    + " this user's roles before continuing",
                name);
      } else if (r.hasRule(resourceGeneric, resourceSpecific)) {
        return true;
      }
    }

    return false;
  }

  @Override
  @Deprecated
  public SecurityRole allow(DatabaseSessionInternal session, String iResource, int iOperation) {
    final String resourceSpecific = Rule.mapLegacyResourceToSpecificResource(iResource);
    final Rule.ResourceGeneric resourceGeneric =
        Rule.mapLegacyResourceToGenericResource(iResource);

    if (resourceSpecific == null || resourceSpecific.equals("*")) {
      return allow(session, resourceGeneric, null, iOperation);
    }

    return allow(session, resourceGeneric, resourceSpecific, iOperation);
  }

  @Override
  @Deprecated
  public SecurityRole checkIfAllowed(DatabaseSessionInternal session, String iResource,
      int iOperation) {
    final String resourceSpecific = Rule.mapLegacyResourceToSpecificResource(iResource);
    final Rule.ResourceGeneric resourceGeneric =
        Rule.mapLegacyResourceToGenericResource(iResource);

    if (resourceSpecific == null || resourceSpecific.equals("*")) {
      return checkIfAllowed(session, resourceGeneric, null, iOperation);
    }

    return checkIfAllowed(session, resourceGeneric, resourceSpecific, iOperation);
  }

  @Override
  @Deprecated
  public boolean isRuleDefined(DatabaseSessionInternal session, String iResource) {
    final String resourceSpecific = Rule.mapLegacyResourceToSpecificResource(iResource);
    final Rule.ResourceGeneric resourceGeneric =
        Rule.mapLegacyResourceToGenericResource(iResource);

    if (resourceSpecific == null || resourceSpecific.equals("*")) {
      return isRuleDefined(session, resourceGeneric, null);
    }

    return isRuleDefined(session, resourceGeneric, resourceSpecific);
  }

  public boolean checkPassword(DatabaseSessionInternal session, final String iPassword) {
    return SecurityManager.checkPassword(iPassword, password);
  }

  public String getName(DatabaseSessionInternal session) {
    return name;
  }

  public SecurityUserImpl setName(DatabaseSessionInternal session, final String iName) {
    throw new UnsupportedOperationException();
  }

  public String getPassword(DatabaseSessionInternal session) {
    return password;
  }

  public SecurityUserImpl setPassword(DatabaseSessionInternal session, final String iPassword) {
    throw new UnsupportedOperationException();
  }

  public STATUSES getAccountStatus(DatabaseSessionInternal session) {
    return status;
  }

  public void setAccountStatus(DatabaseSessionInternal session, STATUSES accountStatus) {
    throw new UnsupportedOperationException();
  }

  public Set<ImmutableRole> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public SecurityUserImpl addRole(DatabaseSessionInternal session, final String iRole) {
    throw new UnsupportedOperationException();
  }

  public SecurityUserImpl addRole(DatabaseSessionInternal session, final SecurityRole iRole) {
    throw new UnsupportedOperationException();
  }

  public boolean removeRole(DatabaseSessionInternal session, final String iRoleName) {
    throw new UnsupportedOperationException();
  }

  public boolean hasRole(DatabaseSessionInternal session, final String iRoleName,
      final boolean iIncludeInherited) {
    for (final SecurityRole role : roles) {
      if (role.getName(session).equals(iRoleName)) {
        return true;
      }

      if (iIncludeInherited) {
        SecurityRole r = role.getParentRole();
        while (r != null) {
          if (r.getName(session).equals(iRoleName)) {
            return true;
          }
          r = r.getParentRole();
        }
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return name;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public Identifiable getIdentity() {
    return rid;
  }

  @Override
  public String getUserType() {
    return userType;
  }
}
