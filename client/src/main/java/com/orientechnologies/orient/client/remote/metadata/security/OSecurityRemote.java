package com.orientechnologies.orient.client.remote.metadata.security;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.OTrackedSet;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.function.OFunction;
import com.orientechnologies.orient.core.metadata.schema.OImmutableClass;
import com.orientechnologies.orient.core.metadata.security.ORestrictedOperation;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityPolicy;
import com.orientechnologies.orient.core.metadata.security.OSecurityPolicyImpl;
import com.orientechnologies.orient.core.metadata.security.OSecurityResourceProperty;
import com.orientechnologies.orient.core.metadata.security.OSecurityRole;
import com.orientechnologies.orient.core.metadata.security.OSecurityRole.ALLOW_MODES;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.metadata.security.OToken;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.metadata.security.auth.OAuthenticationInfo;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OSecurityRemote implements OSecurityInternal {

  public OSecurityRemote() {
  }

  @Override
  public boolean isAllowed(
      ODatabaseSessionInternal session, Set<OIdentifiable> iAllowAll,
      Set<OIdentifiable> iAllowOperation) {
    return true;
  }

  @Override
  public OIdentifiable allowRole(
      final ODatabaseSession session,
      final ODocument iDocument,
      final ORestrictedOperation iOperation,
      final String iRoleName) {
    final ORID role = getRoleRID(session, iRoleName);
    if (role == null) {
      throw new IllegalArgumentException("Role '" + iRoleName + "' not found");
    }

    return allowIdentity(session, iDocument, iOperation.getFieldName(), role);
  }

  @Override
  public OIdentifiable allowUser(
      final ODatabaseSession session,
      final ODocument iDocument,
      final ORestrictedOperation iOperation,
      final String iUserName) {
    final ORID user = getUserRID(session, iUserName);
    if (user == null) {
      throw new IllegalArgumentException("User '" + iUserName + "' not found");
    }

    return allowIdentity(session, iDocument, iOperation.getFieldName(), user);
  }

  @Override
  public OIdentifiable denyUser(
      final ODatabaseSessionInternal session,
      final ODocument iDocument,
      final ORestrictedOperation iOperation,
      final String iUserName) {
    final ORID user = getUserRID(session, iUserName);
    if (user == null) {
      throw new IllegalArgumentException("User '" + iUserName + "' not found");
    }

    return disallowIdentity(session, iDocument, iOperation.getFieldName(), user);
  }

  @Override
  public OIdentifiable denyRole(
      final ODatabaseSessionInternal session,
      final ODocument iDocument,
      final ORestrictedOperation iOperation,
      final String iRoleName) {
    final ORID role = getRoleRID(session, iRoleName);
    if (role == null) {
      throw new IllegalArgumentException("Role '" + iRoleName + "' not found");
    }

    return disallowIdentity(session, iDocument, iOperation.getFieldName(), role);
  }

  @Override
  public OIdentifiable allowIdentity(
      ODatabaseSession session, ODocument iDocument, String iAllowFieldName, OIdentifiable iId) {
    Set<OIdentifiable> field = iDocument.field(iAllowFieldName);
    if (field == null) {
      field = new OTrackedSet<>(iDocument);
      iDocument.field(iAllowFieldName, field);
    }
    field.add(iId);

    return iId;
  }

  public ORID getRoleRID(final ODatabaseSession session, final String iRoleName) {
    if (iRoleName == null) {
      return null;
    }

    try (final OResultSet result =
        session.query("select @rid as rid from ORole where name = ? limit 1", iRoleName)) {

      if (result.hasNext()) {
        return result.next().getProperty("rid");
      }
    }
    return null;
  }

  public ORID getUserRID(final ODatabaseSession session, final String userName) {
    try (OResultSet result =
        session.query("select @rid as rid from OUser where name = ? limit 1", userName)) {

      if (result.hasNext()) {
        return result.next().getProperty("rid");
      }
    }

    return null;
  }

  @Override
  public OIdentifiable disallowIdentity(
      ODatabaseSessionInternal session, ODocument iDocument, String iAllowFieldName,
      OIdentifiable iId) {
    Set<OIdentifiable> field = iDocument.field(iAllowFieldName);
    if (field != null) {
      field.remove(iId);
    }
    return iId;
  }

  @Override
  public OUser authenticate(ODatabaseSessionInternal session, String iUsername,
      String iUserPassword) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OUser createUser(
      final ODatabaseSessionInternal session,
      final String iUserName,
      final String iUserPassword,
      final String... iRoles) {
    final OUser user = new OUser(session, iUserName, iUserPassword);
    if (iRoles != null) {
      for (String r : iRoles) {
        user.addRole(session, r);
      }
    }
    return user.save(session);
  }

  @Override
  public OUser createUser(
      final ODatabaseSessionInternal session,
      final String userName,
      final String userPassword,
      final ORole... roles) {
    final OUser user = new OUser(session, userName, userPassword);

    if (roles != null) {
      for (ORole r : roles) {
        user.addRole(session, r);
      }
    }

    return user.save(session);
  }

  @Override
  public OUser authenticate(ODatabaseSessionInternal session, OToken authToken) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ORole createRole(
      final ODatabaseSessionInternal session, final String iRoleName,
      final ALLOW_MODES iAllowMode) {
    return createRole(session, iRoleName, null, iAllowMode);
  }

  @Override
  public ORole createRole(
      final ODatabaseSessionInternal session,
      final String iRoleName,
      final ORole iParent,
      final ALLOW_MODES iAllowMode) {
    final ORole role = new ORole(session, iRoleName, iParent, iAllowMode);
    return role.save(session);
  }

  @Override
  public OUser getUser(final ODatabaseSession session, final String iUserName) {
    try (OResultSet result = session.query("select from OUser where name = ? limit 1", iUserName)) {
      if (result.hasNext()) {
        return new OUser(session, (ODocument) result.next().getElement().get());
      }
    }
    return null;
  }

  public OUser getUser(final ODatabaseSession session, final ORID iRecordId) {
    if (iRecordId == null) {
      return null;
    }

    ODocument result;
    result = session.load(iRecordId);
    if (!result.getClassName().equals(OUser.CLASS_NAME)) {
      result = null;
    }
    return new OUser(session, result);
  }

  public ORole getRole(final ODatabaseSession session, final OIdentifiable iRole) {
    final ODocument doc = session.load(iRole.getIdentity());
    OImmutableClass clazz = ODocumentInternal.getImmutableSchemaClass(doc);
    if (clazz != null && clazz.isOrole()) {
      return new ORole(session, doc);
    }

    return null;
  }

  public ORole getRole(final ODatabaseSession session, final String iRoleName) {
    if (iRoleName == null) {
      return null;
    }

    try (final OResultSet result =
        session.query("select from ORole where name = ? limit 1", iRoleName)) {
      if (result.hasNext()) {
        return new ORole(session, (ODocument) result.next().getElement().get());
      }
    }

    return null;
  }

  public List<ODocument> getAllUsers(final ODatabaseSession session) {
    try (OResultSet rs = session.query("select from OUser")) {
      return rs.stream().map((e) -> (ODocument) e.getElement().get()).collect(Collectors.toList());
    }
  }

  public List<ODocument> getAllRoles(final ODatabaseSession session) {
    try (OResultSet rs = session.query("select from ORole")) {
      return rs.stream().map((e) -> (ODocument) e.getElement().get()).collect(Collectors.toList());
    }
  }

  @Override
  public Map<String, OSecurityPolicy> getSecurityPolicies(
      ODatabaseSession session, OSecurityRole role) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OSecurityPolicy getSecurityPolicy(
      ODatabaseSession session, OSecurityRole role, String resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSecurityPolicy(
      ODatabaseSessionInternal session, OSecurityRole role, String resource,
      OSecurityPolicyImpl policy) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OSecurityPolicyImpl createSecurityPolicy(ODatabaseSession session, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OSecurityPolicyImpl getSecurityPolicy(ODatabaseSession session, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveSecurityPolicy(ODatabaseSession session, OSecurityPolicyImpl policy) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteSecurityPolicy(ODatabaseSession session, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeSecurityPolicy(ODatabaseSession session, ORole role, String resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean dropUser(final ODatabaseSession session, final String iUserName) {
    final Number removed =
        session.command("delete from OUser where name = ?", iUserName).next().getProperty("count");

    return removed != null && removed.intValue() > 0;
  }

  @Override
  public boolean dropRole(final ODatabaseSession session, final String iRoleName) {
    final Number removed =
        session
            .command("delete from ORole where name = '" + iRoleName + "'")
            .next()
            .getProperty("count");

    return removed != null && removed.intValue() > 0;
  }

  @Override
  public void createClassTrigger(ODatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getVersion(ODatabaseSession session) {
    return 0;
  }

  @Override
  public void incrementVersion(ODatabaseSession session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OUser create(ODatabaseSessionInternal session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void load(ODatabaseSessionInternal session) {
  }

  @Override
  public void close() {
  }

  @Override
  public Set<String> getFilteredProperties(ODatabaseSessionInternal session, ODocument document) {
    return Collections.emptySet();
  }

  @Override
  public boolean isAllowedWrite(ODatabaseSessionInternal session, ODocument document,
      String propertyName) {
    return true;
  }

  @Override
  public boolean canCreate(ODatabaseSessionInternal session, ORecord record) {
    return true;
  }

  @Override
  public boolean canRead(ODatabaseSessionInternal session, ORecord record) {
    return true;
  }

  @Override
  public boolean canUpdate(ODatabaseSessionInternal session, ORecord record) {
    return true;
  }

  @Override
  public boolean canDelete(ODatabaseSessionInternal session, ORecord record) {
    return true;
  }

  @Override
  public boolean canExecute(ODatabaseSessionInternal session, OFunction function) {
    return true;
  }

  @Override
  public boolean isReadRestrictedBySecurityPolicy(ODatabaseSession session, String resource) {
    return false;
  }

  @Override
  public Set<OSecurityResourceProperty> getAllFilteredProperties(
      ODatabaseSessionInternal database) {
    return Collections.EMPTY_SET;
  }

  @Override
  public OSecurityUser securityAuthenticate(
      ODatabaseSessionInternal session, String userName, String password) {
    throw new UnsupportedOperationException();
  }

  @Override
  public OSecurityUser securityAuthenticate(
      ODatabaseSessionInternal session, OAuthenticationInfo authenticationInfo) {
    throw new UnsupportedOperationException();
  }
}
