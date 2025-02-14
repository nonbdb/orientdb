/* Generated By:JJTree: Do not edit this line. SQLRevokeStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;
import java.util.Objects;

public class SQLRevokeStatement extends SQLSimpleExecStatement {

  protected SQLPermission permission;
  protected boolean revokePolicy = false;
  protected SQLSecurityResourceSegment securityResource;
  protected SQLIdentifier actor;

  public SQLRevokeStatement(int id) {
    super(id);
  }

  public SQLRevokeStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    DatabaseSessionInternal db = ctx.getDatabase();
    Role role = db.getMetadata().getSecurity().getRole(actor.getStringValue());
    if (role == null) {
      throw new CommandExecutionException("Invalid role: " + actor.getStringValue());
    }

    String resourcePath = securityResource.toString();
    if (permission != null) {
      role.revoke(db, resourcePath, toPrivilege(permission.permission));
      role.save(db);
    } else {
      SecurityInternal security = db.getSharedContext().getSecurity();
      security.removeSecurityPolicy(db, role, resourcePath);
    }

    ResultInternal result = new ResultInternal(db);
    result.setProperty("operation", "grant");
    result.setProperty("role", actor.getStringValue());
    if (permission != null) {
      result.setProperty("permission", permission.toString());
    }
    result.setProperty("resource", resourcePath);
    return ExecutionStream.singleton(result);
  }

  protected int toPrivilege(String privilegeName) {
    int privilege;
    if ("CREATE".equals(privilegeName)) {
      privilege = Role.PERMISSION_CREATE;
    } else if ("READ".equals(privilegeName)) {
      privilege = Role.PERMISSION_READ;
    } else if ("UPDATE".equals(privilegeName)) {
      privilege = Role.PERMISSION_UPDATE;
    } else if ("DELETE".equals(privilegeName)) {
      privilege = Role.PERMISSION_DELETE;
    } else if ("EXECUTE".equals(privilegeName)) {
      privilege = Role.PERMISSION_EXECUTE;
    } else if ("ALL".equals(privilegeName)) {
      privilege = Role.PERMISSION_ALL;
    } else if ("NONE".equals(privilegeName)) {
      privilege = Role.PERMISSION_NONE;
    } else {
      throw new CommandExecutionException("Unrecognized privilege '" + privilegeName + "'");
    }
    return privilege;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("REVOKE ");
    if (revokePolicy) {
      builder.append("POLICY");
    } else {
      permission.toString(params, builder);
    }
    builder.append(" ON ");
    this.securityResource.toString(params, builder);
    builder.append(" FROM ");
    actor.toString(params, builder);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("REVOKE ");
    if (revokePolicy) {
      builder.append("POLICY");
    } else {
      permission.toGenericStatement(builder);
    }
    builder.append(" ON ");
    this.securityResource.toGenericStatement(builder);
    builder.append(" FROM ");
    actor.toGenericStatement(builder);
  }

  @Override
  public SQLRevokeStatement copy() {
    SQLRevokeStatement result = new SQLRevokeStatement(-1);
    result.permission = permission == null ? null : permission.copy();
    result.securityResource = securityResource == null ? null : securityResource.copy();
    result.revokePolicy = revokePolicy;
    result.actor = actor == null ? null : actor.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SQLRevokeStatement that = (SQLRevokeStatement) o;
    return revokePolicy == that.revokePolicy
        && Objects.equals(permission, that.permission)
        && Objects.equals(securityResource, that.securityResource)
        && Objects.equals(actor, that.actor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permission, revokePolicy, securityResource, actor);
  }
}
/* JavaCC - OriginalChecksum=d483850d10e1562c1b942fcc249278eb (do not edit this line) */
