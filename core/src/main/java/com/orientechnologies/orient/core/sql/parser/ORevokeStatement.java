/* Generated By:JJTree: Do not edit this line. ORevokeStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class ORevokeStatement extends OSimpleExecStatement {

  protected OPermission permission;
  protected boolean revokePolicy = false;
  protected OSecurityResourceSegment securityResource;
  protected OIdentifier actor;

  public ORevokeStatement(int id) {
    super(id);
  }

  public ORevokeStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    ODatabaseSessionInternal db = ctx.getDatabase();
    ORole role = db.getMetadata().getSecurity().getRole(actor.getStringValue());
    if (role == null) {
      throw new OCommandExecutionException("Invalid role: " + actor.getStringValue());
    }

    String resourcePath = securityResource.toString();
    if (permission != null) {
      role.revoke(db, resourcePath, toPrivilege(permission.permission));
      role.save(db);
    } else {
      OSecurityInternal security = db.getSharedContext().getSecurity();
      security.removeSecurityPolicy(db, role, resourcePath);
    }

    OResultInternal result = new OResultInternal(db);
    result.setProperty("operation", "grant");
    result.setProperty("role", actor.getStringValue());
    if (permission != null) {
      result.setProperty("permission", permission.toString());
    }
    result.setProperty("resource", resourcePath);
    return OExecutionStream.singleton(result);
  }

  protected int toPrivilege(String privilegeName) {
    int privilege;
    if ("CREATE".equals(privilegeName)) {
      privilege = ORole.PERMISSION_CREATE;
    } else if ("READ".equals(privilegeName)) {
      privilege = ORole.PERMISSION_READ;
    } else if ("UPDATE".equals(privilegeName)) {
      privilege = ORole.PERMISSION_UPDATE;
    } else if ("DELETE".equals(privilegeName)) {
      privilege = ORole.PERMISSION_DELETE;
    } else if ("EXECUTE".equals(privilegeName)) {
      privilege = ORole.PERMISSION_EXECUTE;
    } else if ("ALL".equals(privilegeName)) {
      privilege = ORole.PERMISSION_ALL;
    } else if ("NONE".equals(privilegeName)) {
      privilege = ORole.PERMISSION_NONE;
    } else {
      throw new OCommandExecutionException("Unrecognized privilege '" + privilegeName + "'");
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
  public ORevokeStatement copy() {
    ORevokeStatement result = new ORevokeStatement(-1);
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
    ORevokeStatement that = (ORevokeStatement) o;
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
