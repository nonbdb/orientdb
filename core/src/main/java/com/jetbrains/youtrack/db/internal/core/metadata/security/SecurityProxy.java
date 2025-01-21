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

import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.List;
import java.util.Set;

/**
 * Proxy class for user management
 */
public class SecurityProxy implements Security {

  private final DatabaseSessionInternal session;
  private final SecurityInternal security;

  public SecurityProxy(SecurityInternal security, DatabaseSessionInternal session) {
    this.security = security;
    this.session = session;
  }

  @Override
  public boolean isAllowed(
      final Set<Identifiable> iAllowAll, final Set<Identifiable> iAllowOperation) {
    return security.isAllowed(session, iAllowAll, iAllowOperation);
  }

  @Override
  public Identifiable allowUser(
      EntityImpl entity, RestrictedOperation iOperationType, String iUserName) {
    return security.allowUser(session, entity, iOperationType, iUserName);
  }

  @Override
  public Identifiable allowRole(
      EntityImpl entity, RestrictedOperation iOperationType, String iRoleName) {
    return security.allowRole(session, entity, iOperationType, iRoleName);
  }

  @Override
  public Identifiable denyUser(
      EntityImpl entity, RestrictedOperation iOperationType, String iUserName) {
    return security.denyUser(session, entity, iOperationType, iUserName);
  }

  @Override
  public Identifiable denyRole(
      EntityImpl entity, RestrictedOperation iOperationType, String iRoleName) {
    return security.denyRole(session, entity, iOperationType, iRoleName);
  }

  public SecurityUserImpl authenticate(final String iUsername, final String iUserPassword) {
    return security.authenticate(session, iUsername, iUserPassword);
  }

  public SecurityUserImpl authenticate(final Token authToken) {
    return security.authenticate(session, authToken);
  }

  public SecurityUserImpl getUser(final String iUserName) {
    return security.getUser(session, iUserName);
  }

  public SecurityUserImpl getUser(final RID iUserId) {
    return security.getUser(session, iUserId);
  }

  public SecurityUserImpl createUser(
      final String iUserName, final String iUserPassword, final String... iRoles) {
    return security.createUser(session, iUserName, iUserPassword, iRoles);
  }

  public SecurityUserImpl createUser(
      final String iUserName, final String iUserPassword, final Role... iRoles) {
    return security.createUser(session, iUserName, iUserPassword, iRoles);
  }

  public Role getRole(final String iRoleName) {
    return security.getRole(session, iRoleName);
  }

  public Role getRole(final Identifiable iRole) {
    return security.getRole(session, iRole);
  }

  public Role createRole(final String iRoleName) {
    return security.createRole(session, iRoleName);
  }

  public Role createRole(
      final String iRoleName, final Role iParent) {
    return security.createRole(session, iRoleName, iParent);
  }

  public List<EntityImpl> getAllUsers() {
    return security.getAllUsers(session);
  }

  public List<EntityImpl> getAllRoles() {
    return security.getAllRoles(session);
  }

  public String toString() {
    return security.toString();
  }

  public boolean dropUser(final String iUserName) {
    return security.dropUser(session, iUserName);
  }

  public void dropRole(final String iRoleName) {
    security.dropRole(session, iRoleName);
  }
}
