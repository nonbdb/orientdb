/* Generated By:JJTree: Do not edit this line. OCreateUserStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OCreateUserStatement extends OSimpleExecStatement {

  protected static final String USER_FIELD_NAME = "name";
  private static final String USER_FIELD_PASSWORD = "password";
  private static final String USER_FIELD_STATUS = "status";
  private static final String USER_FIELD_ROLES = "roles";

  private static final String DEFAULT_STATUS = "ACTIVE";
  private static final String DEFAULT_ROLE = "writer";
  private static final String ROLE_CLASS = "ORole";
  private static final String ROLE_FIELD_NAME = "name";

  public OCreateUserStatement(int id) {
    super(id);
  }

  public OCreateUserStatement(OrientSql p, int id) {
    super(p, id);
  }

  protected OIdentifier name;
  protected OIdentifier passwordIdentifier;
  protected String passwordString;
  protected OInputParameter passwordParam;

  protected List<OIdentifier> roles = new ArrayList<>();

  public void addRole(OIdentifier role) {
    this.roles.add(role);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {

    List<Object> params = new ArrayList<>();
    // INSERT INTO OUser SET
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO OUser SET ");

    sb.append(USER_FIELD_NAME);
    sb.append("=?");
    params.add(this.name.getStringValue());

    // pass=<pass>
    sb.append(',');
    sb.append(USER_FIELD_PASSWORD);
    sb.append("=");
    if (passwordString != null) {
      sb.append(passwordString);
    } else if (passwordIdentifier != null) {
      sb.append("?");
      params.add(passwordIdentifier.getStringValue());
    } else {
      sb.append("?");
      params.add(passwordParam.getValue(ctx.getInputParameters()));
    }

    // status=ACTIVE
    sb.append(',');
    sb.append(USER_FIELD_STATUS);
    sb.append("='");
    sb.append(DEFAULT_STATUS);
    sb.append("'");

    // role=(select from ORole where name in [<input_role || 'writer'>)]
    List<OIdentifier> roles = new ArrayList<>();
    roles.addAll(this.roles);
    if (roles.size() == 0) {
      roles.add(new OIdentifier(DEFAULT_ROLE));
    }

    sb.append(',');
    sb.append(USER_FIELD_ROLES);
    sb.append("=(SELECT FROM ");
    sb.append(ROLE_CLASS);
    sb.append(" WHERE ");
    sb.append(ROLE_FIELD_NAME);
    sb.append(" IN [");
    OSecurity security = ctx.getDatabase().getMetadata().getSecurity();
    for (int i = 0; i < this.roles.size(); ++i) {
      String roleName = this.roles.get(i).getStringValue();
      ORole role = security.getRole(roleName);
      if (role == null) {
        throw new YTCommandExecutionException(
            "Cannot create user " + this.name + ": role " + roleName + " does not exist");
      }
      if (i > 0) {
        sb.append(", ");
      }

      if (roleName.startsWith("'") || roleName.startsWith("\"")) {
        sb.append(roleName);
      } else {
        sb.append("'");
        sb.append(roleName);
        sb.append("'");
      }
    }
    sb.append("])");
    return OExecutionStream.resultIterator(
        ctx.getDatabase().command(sb.toString(), params.toArray()).stream().iterator());
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE USER ");
    name.toString(params, builder);
    builder.append(" IDENTIFIED BY ");
    if (passwordIdentifier != null) {
      passwordIdentifier.toString(params, builder);
    } else if (passwordString != null) {
      builder.append(passwordString);
    } else {
      passwordParam.toString(params, builder);
    }
    if (!roles.isEmpty()) {
      builder.append("ROLE [");
      boolean first = true;
      for (OIdentifier role : roles) {
        if (!first) {
          builder.append(", ");
        }
        role.toString(params, builder);
        first = false;
      }
      builder.append("]");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CREATE USER ");
    name.toGenericStatement(builder);
    builder.append(" IDENTIFIED BY ");
    if (passwordIdentifier != null) {
      passwordIdentifier.toGenericStatement(builder);
    } else if (passwordString != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else {
      passwordParam.toGenericStatement(builder);
    }
    if (!roles.isEmpty()) {
      builder.append("ROLE [");
      boolean first = true;
      for (OIdentifier role : roles) {
        if (!first) {
          builder.append(", ");
        }
        role.toGenericStatement(builder);
        first = false;
      }
      builder.append("]");
    }
  }

  @Override
  public OCreateUserStatement copy() {
    OCreateUserStatement result = new OCreateUserStatement(-1);
    result.name = name == null ? null : name.copy();
    result.passwordIdentifier = passwordIdentifier == null ? null : passwordIdentifier.copy();
    result.passwordString = passwordString;
    result.passwordParam = passwordParam == null ? null : passwordParam.copy();
    roles.forEach(x -> result.roles.add(x.copy()));
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
    OCreateUserStatement that = (OCreateUserStatement) o;
    return Objects.equals(name, that.name)
        && Objects.equals(passwordIdentifier, that.passwordIdentifier)
        && Objects.equals(passwordString, that.passwordString)
        && Objects.equals(passwordParam, that.passwordParam)
        && Objects.equals(roles, that.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, passwordIdentifier, passwordString, passwordParam, roles);
  }
}
/* JavaCC - OriginalChecksum=d1f22e2468eaf740d8ccc90ebbe2c185 (do not edit this line) */
