/* Generated By:JJTree: Do not edit this line. OCreateLinkStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.LinkList;
import com.orientechnologies.orient.core.db.record.LinkSet;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTProperty;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.ODocumentHelper;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.sql.YTCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.executor.YTResultSet;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OCreateLinkStatement extends OSimpleExecStatement {

  protected OIdentifier name;
  protected OIdentifier type;
  protected OIdentifier sourceClass;
  protected OIdentifier sourceField;
  protected ORecordAttribute sourceRecordAttr;
  protected OIdentifier destClass;
  protected OIdentifier destField;
  protected ORecordAttribute destRecordAttr;
  protected boolean inverse = false;

  boolean breakExec = false; // for timeout

  public OCreateLinkStatement(int id) {
    super(id);
  }

  public OCreateLinkStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    Object total = execute(ctx);
    YTResultInternal result = new YTResultInternal(ctx.getDatabase());
    result.setProperty("operation", "create link");
    result.setProperty("name", name.getValue());
    result.setProperty("count", total);
    result.setProperty("fromClass", sourceClass.getStringValue());
    result.setProperty("toClass", destClass.getStringValue());
    return OExecutionStream.singleton(result);
  }

  /**
   * Execute the CREATE LINK.
   */
  private Object execute(OCommandContext ctx) {
    if (destField == null) {
      throw new YTCommandExecutionException(
          "Cannot execute the command because it has not been parsed yet");
    }

    final YTDatabaseSessionInternal database = ctx.getDatabase();
    if (database.getDatabaseOwner() == null) {
      throw new YTCommandSQLParsingException(
          "This command supports only the database type YTDatabaseDocumentTx and type '"
              + database.getClass()
              + "' was found");
    }

    final YTDatabaseSessionInternal db = database.getDatabaseOwner();

    YTClass sourceClass =
        database
            .getMetadata()
            .getImmutableSchemaSnapshot()
            .getClass(this.sourceClass.getStringValue());
    if (sourceClass == null) {
      throw new YTCommandExecutionException(
          "Source class '" + this.sourceClass.getStringValue() + "' not found");
    }

    YTClass destClass =
        database
            .getMetadata()
            .getImmutableSchemaSnapshot()
            .getClass(this.destClass.getStringValue());
    if (destClass == null) {
      throw new YTCommandExecutionException(
          "Destination class '" + this.destClass.getStringValue() + "' not found");
    }

    String cmd = "select from ";
    if (destField != null && !ODocumentHelper.ATTRIBUTE_RID.equals(destField.value)) {
      cmd = "select from " + this.destClass + " where " + destField + " = ";
    }

    long[] total = new long[1];

    String linkName = name == null ? sourceField.getStringValue() : name.getStringValue();

    var documentSourceClass = sourceClass;
    var txCmd = cmd;
    try {
      final boolean[] multipleRelationship = new boolean[1];

      YTType linkType = YTType.valueOf(type.getStringValue().toUpperCase(Locale.ENGLISH));
      if (linkType != null)
      // DETERMINE BASED ON FORCED TYPE
      {
        multipleRelationship[0] = linkType == YTType.LINKSET || linkType == YTType.LINKLIST;
      } else {
        multipleRelationship[0] = false;
      }

      var txLinkType = linkType;
      var txDestClass = destClass;

      List<YTEntityImpl> result;
      Object oldValue;
      YTEntityImpl target;

      // BROWSE ALL THE RECORDS OF THE SOURCE CLASS
      for (YTEntityImpl doc : db.browseClass(documentSourceClass.getName())) {
        if (breakExec) {
          break;
        }
        Object value = doc.getProperty(sourceField.getStringValue());

        if (value != null) {
          if (value instanceof YTEntityImpl || value instanceof YTRID) {
            // ALREADY CONVERTED
          } else if (value instanceof Collection<?>) {
            // TODO
          } else {
            // SEARCH THE DESTINATION RECORD
            target = null;

            if (destField != null
                && !ODocumentHelper.ATTRIBUTE_RID.equals(destField.value)
                && value instanceof String) {
              if (((String) value).length() == 0) {
                value = null;
              } else {
                value = "'" + value + "'";
              }
            }

            try (YTResultSet rs = database.query(txCmd + value)) {
              result = toList(rs);
            }

            if (result == null || result.size() == 0) {
              value = null;
            } else if (result.size() > 1) {
              throw new YTCommandExecutionException(
                  "Cannot create link because multiple records was found in class '"
                      + txDestClass.getName()
                      + "' with value "
                      + value
                      + " in field '"
                      + destField
                      + "'");
            } else {
              target = result.get(0);
              value = target;
            }

            if (target != null && inverse) {
              // INVERSE RELATIONSHIP
              oldValue = target.getProperty(linkName);

              if (oldValue != null) {
                if (!multipleRelationship[0]) {
                  multipleRelationship[0] = true;
                }

                Collection<YTEntityImpl> coll;
                if (oldValue instanceof Collection) {
                  // ADD IT IN THE EXISTENT COLLECTION
                  coll = (Collection<YTEntityImpl>) oldValue;
                  target.setDirty();
                } else {
                  // CREATE A NEW COLLECTION FOR BOTH
                  coll = new ArrayList<YTEntityImpl>(2);
                  target.setProperty(linkName, coll);
                  coll.add((YTEntityImpl) oldValue);
                }
                coll.add(doc);
              } else {
                if (txLinkType != null) {
                  if (txLinkType == YTType.LINKSET) {
                    value = new LinkSet(target);
                    ((Set<YTIdentifiable>) value).add(doc);
                  } else if (txLinkType == YTType.LINKLIST) {
                    value = new LinkList(target);
                    ((LinkList) value).add(doc);
                  } else
                  // IGNORE THE TYPE, SET IT AS LINK
                  {
                    value = doc;
                  }
                } else {
                  value = doc;
                }

                target.setProperty(linkName, value);
              }
              target.save();

            } else {

              // SET THE REFERENCE
              doc.setProperty(linkName, value);
              doc.save();
            }

            total[0]++;
          }
        }
      }

      if (total[0] > 0) {
        if (inverse) {
          // REMOVE THE OLD PROPERTY IF ANY
          YTProperty prop = destClass.getProperty(linkName);
          destClass = db.getMetadata().getSchema().getClass(this.destClass.getStringValue());
          if (prop != null) {
            if (linkType != prop.getType()) {
              throw new YTCommandExecutionException(
                  "Cannot create the link because the property '"
                      + linkName
                      + "' already exists for class "
                      + destClass.getName()
                      + " and has a different type - actual: "
                      + prop.getType()
                      + " expected: "
                      + linkType);
            }
          } else {
            throw new YTCommandExecutionException(
                "Cannot create the link because the property '"
                    + linkName
                    + "' does not exist in class '"
                    + destClass.getName()
                    + "'");
          }
        } else {
          // REMOVE THE OLD PROPERTY IF ANY
          YTProperty prop = sourceClass.getProperty(linkName);
          sourceClass = db.getMetadata().getSchema().getClass(this.destClass.getStringValue());
          if (prop != null) {
            if (prop.getType() != YTType.LINK) {
              throw new YTCommandExecutionException(
                  "Cannot create the link because the property '"
                      + linkName
                      + "' already exists for class "
                      + sourceClass.getName()
                      + " and has a different type - actual: "
                      + prop.getType()
                      + " expected: "
                      + YTType.LINK);
            }
          } else {
            throw new YTCommandExecutionException(
                "Cannot create the link because the property '"
                    + linkName
                    + "' does not exist in class '"
                    + sourceClass.getName()
                    + "'");
          }
        }
      }

    } catch (Exception e) {
      throw YTException.wrapException(
          new YTCommandExecutionException("Error on creation of links"), e);
    }
    return total[0];
  }

  private List<YTEntityImpl> toList(YTResultSet rs) {
    if (!rs.hasNext()) {
      return null;
    }
    List<YTEntityImpl> result = new ArrayList<>();
    while (rs.hasNext()) {
      result.add((YTEntityImpl) rs.next().getEntity().orElse(null));
    }
    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE LINK ");
    name.toString(params, builder);
    builder.append(" TYPE ");
    type.toString(params, builder);
    builder.append(" FROM ");
    sourceClass.toString(params, builder);
    builder.append(".");
    if (sourceField != null) {
      sourceField.toString(params, builder);
    } else {
      sourceRecordAttr.toString(params, builder);
    }
    builder.append(" TO ");
    destClass.toString(params, builder);
    builder.append(".");
    if (destField != null) {
      destField.toString(params, builder);
    } else {
      destRecordAttr.toString(params, builder);
    }
    if (inverse) {
      builder.append(" INVERSE");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CREATE LINK ");
    name.toGenericStatement(builder);
    builder.append(" TYPE ");
    type.toGenericStatement(builder);
    builder.append(" FROM ");
    sourceClass.toGenericStatement(builder);
    builder.append(".");
    if (sourceField != null) {
      sourceField.toGenericStatement(builder);
    } else {
      sourceRecordAttr.toGenericStatement(builder);
    }
    builder.append(" TO ");
    destClass.toGenericStatement(builder);
    builder.append(".");
    if (destField != null) {
      destField.toGenericStatement(builder);
    } else {
      destRecordAttr.toGenericStatement(builder);
    }
    if (inverse) {
      builder.append(" INVERSE");
    }
  }

  @Override
  public OCreateLinkStatement copy() {
    OCreateLinkStatement result = new OCreateLinkStatement(-1);
    result.name = name == null ? null : name.copy();
    result.type = type == null ? null : type.copy();
    result.sourceClass = sourceClass == null ? null : sourceClass.copy();
    result.sourceField = sourceField == null ? null : sourceField.copy();
    result.sourceRecordAttr = sourceRecordAttr == null ? null : sourceRecordAttr.copy();
    result.destClass = destClass == null ? null : destClass.copy();
    result.destField = destField == null ? null : destField.copy();
    result.destRecordAttr = destRecordAttr == null ? null : destRecordAttr.copy();
    result.inverse = inverse;
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

    OCreateLinkStatement that = (OCreateLinkStatement) o;

    if (inverse != that.inverse) {
      return false;
    }
    if (!Objects.equals(name, that.name)) {
      return false;
    }
    if (!Objects.equals(type, that.type)) {
      return false;
    }
    if (!Objects.equals(sourceClass, that.sourceClass)) {
      return false;
    }
    if (!Objects.equals(sourceField, that.sourceField)) {
      return false;
    }
    if (!Objects.equals(sourceRecordAttr, that.sourceRecordAttr)) {
      return false;
    }
    if (!Objects.equals(destClass, that.destClass)) {
      return false;
    }
    if (!Objects.equals(destField, that.destField)) {
      return false;
    }
    return Objects.equals(destRecordAttr, that.destRecordAttr);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (sourceClass != null ? sourceClass.hashCode() : 0);
    result = 31 * result + (sourceField != null ? sourceField.hashCode() : 0);
    result = 31 * result + (sourceRecordAttr != null ? sourceRecordAttr.hashCode() : 0);
    result = 31 * result + (destClass != null ? destClass.hashCode() : 0);
    result = 31 * result + (destField != null ? destField.hashCode() : 0);
    result = 31 * result + (destRecordAttr != null ? destRecordAttr.hashCode() : 0);
    result = 31 * result + (inverse ? 1 : 0);
    return result;
  }

  public OIdentifier getName() {
    return name;
  }

  public OIdentifier getType() {
    return type;
  }

  public OIdentifier getSourceClass() {
    return sourceClass;
  }

  public OIdentifier getSourceField() {
    return sourceField;
  }

  public ORecordAttribute getSourceRecordAttr() {
    return sourceRecordAttr;
  }

  public OIdentifier getDestClass() {
    return destClass;
  }

  public OIdentifier getDestField() {
    return destField;
  }

  public ORecordAttribute getDestRecordAttr() {
    return destRecordAttr;
  }

  public boolean isInverse() {
    return inverse;
  }
}
/* JavaCC - OriginalChecksum=de46c9bdaf3b36691764a78cd89d1c2b (do not edit this line) */
