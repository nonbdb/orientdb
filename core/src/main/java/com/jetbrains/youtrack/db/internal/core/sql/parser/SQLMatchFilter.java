/* Generated By:JJTree: Do not edit this line. SQLMatchFilter.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLMatchFilter extends SimpleNode {

  // TODO transform in a map
  protected List<SQLMatchFilterItem> items = new ArrayList<SQLMatchFilterItem>();

  public SQLMatchFilter(int id) {
    super(id);
  }

  public SQLMatchFilter(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public String getAlias() {
    for (SQLMatchFilterItem item : items) {
      if (item.alias != null) {
        return item.alias.getStringValue();
      }
    }
    return null;
  }

  public void setAlias(String alias) {
    boolean found = false;
    for (SQLMatchFilterItem item : items) {
      if (item.alias != null) {
        item.alias = new SQLIdentifier(alias);
        found = true;
        break;
      }
    }
    if (!found) {
      SQLMatchFilterItem newItem = new SQLMatchFilterItem(-1);
      newItem.alias = new SQLIdentifier(alias);
      items.add(newItem);
    }
  }

  public SQLWhereClause getFilter() {
    for (SQLMatchFilterItem item : items) {
      if (item.filter != null) {
        return item.filter;
      }
    }
    return null;
  }

  public void setFilter(SQLWhereClause filter) {
    boolean found = false;
    for (SQLMatchFilterItem item : items) {
      if (item.filter != null) {
        item.filter = filter;
        found = true;
        break;
      }
    }
    if (!found) {
      SQLMatchFilterItem newItem = new SQLMatchFilterItem(-1);
      newItem.filter = filter;
      items.add(newItem);
    }
  }

  public SQLWhereClause getWhileCondition() {
    for (SQLMatchFilterItem item : items) {
      if (item.whileCondition != null) {
        return item.whileCondition;
      }
    }
    return null;
  }

  public String getClassName(CommandContext context) {
    for (SQLMatchFilterItem item : items) {
      if (item.className != null) {
        if (item.className.value instanceof String) {
          return (String) item.className.value;
        } else if (item.className.value instanceof SimpleNode) {
          StringBuilder builder = new StringBuilder();

          ((SimpleNode) item.className.value)
              .toString(context == null ? null : context.getInputParameters(), builder);
          return builder.toString();
        } else if (item.className.isBaseIdentifier()) {
          return item.className.getDefaultAlias().getStringValue();
        } else {
          return item.className.toString();
        }
      }
    }
    return null;
  }

  public String getClusterName(CommandContext context) {
    for (SQLMatchFilterItem item : items) {
      if (item.clusterName != null) {
        return item.clusterName.getStringValue();
      } else if (item.clusterId != null) {
        int cid = item.clusterId.value.intValue();
        String clusterName = context.getDatabase().getClusterNameById(cid);
        if (clusterName != null) {
          return clusterName;
        }
      }
    }
    return null;
  }

  public SQLRid getRid(CommandContext context) {
    for (SQLMatchFilterItem item : items) {
      if (item.rid != null) {
        return item.rid;
      }
    }
    return null;
  }

  public Integer getMaxDepth() {
    for (SQLMatchFilterItem item : items) {
      if (item.maxDepth != null) {
        return item.maxDepth.value.intValue();
      }
    }
    return null;
  }

  public boolean isOptional() {
    for (SQLMatchFilterItem item : items) {
      if (Boolean.TRUE.equals(item.optional)) {
        return true;
      }
    }
    return false;
  }

  public String getDepthAlias() {
    for (SQLMatchFilterItem item : items) {
      if (item.depthAlias != null) {
        return item.depthAlias.getStringValue();
      }
    }
    return null;
  }

  public String getPathAlias() {
    for (SQLMatchFilterItem item : items) {
      if (item.pathAlias != null) {
        return item.pathAlias.getStringValue();
      }
    }
    return null;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("{");
    boolean first = true;
    for (SQLMatchFilterItem item : items) {
      if (!first) {
        builder.append(", ");
      }
      item.toString(params, builder);
      first = false;
    }
    builder.append("}");
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("{");
    boolean first = true;
    for (SQLMatchFilterItem item : items) {
      if (!first) {
        builder.append(", ");
      }
      item.toGenericStatement(builder);
      first = false;
    }
    builder.append("}");
  }

  @Override
  public SQLMatchFilter copy() {
    SQLMatchFilter result = new SQLMatchFilter(-1);
    result.items =
        items == null ? null : items.stream().map(x -> x.copy()).collect(Collectors.toList());
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

    SQLMatchFilter that = (SQLMatchFilter) o;

    return Objects.equals(items, that.items);
  }

  @Override
  public int hashCode() {
    return items != null ? items.hashCode() : 0;
  }

  public void addItem(SQLMatchFilterItem item) {
    this.items.add(item);
  }
}
/* JavaCC - OriginalChecksum=6b099371c69e0d0c1c106fc96b3072de (do not edit this line) */
