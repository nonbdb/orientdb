/* Generated By:JJTree: Do not edit this line. OCreateIndexStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.collate.OCollate;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.exception.YTDatabaseException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexDefinition;
import com.orientechnologies.orient.core.index.OIndexDefinitionFactory;
import com.orientechnologies.orient.core.index.OIndexFactory;
import com.orientechnologies.orient.core.index.OIndexes;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.index.YTIndexException;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTClassImpl;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OCreateIndexStatement extends ODDLStatement {

  protected OIndexName name;
  protected OIdentifier className;
  protected List<Property> propertyList = new ArrayList<Property>();
  protected OIdentifier type;
  protected OIdentifier engine;
  protected List<OIdentifier> keyTypes = new ArrayList<OIdentifier>();
  protected OJson metadata;
  protected boolean ifNotExists = false;

  public OCreateIndexStatement(int id) {
    super(id);
  }

  public OCreateIndexStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void addProperty(Property property) {
    this.propertyList.add(property);
  }

  public void addKeyType(OIdentifier identifier) {
    this.keyTypes.add(identifier);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    Object execResult = execute(ctx);
    if (execResult != null) {
      YTResultInternal result = new YTResultInternal(ctx.getDatabase());
      result.setProperty("operation", "create index");
      result.setProperty("name", name.getValue());
      return OExecutionStream.singleton(result);
    } else {
      return OExecutionStream.empty();
    }
  }

  Object execute(OCommandContext ctx) {
    final YTDatabaseSessionInternal database = ctx.getDatabase();

    if (database.getMetadata().getIndexManagerInternal().existsIndex(name.getValue())) {
      if (ifNotExists) {
        return null;
      } else {
        throw new YTCommandExecutionException("Index " + name + " already exists");
      }
    }

    final OIndex idx;
    List<OCollate> collatesList = calculateCollates(ctx);
    String engine =
        this.engine == null ? null : this.engine.getStringValue().toUpperCase(Locale.ENGLISH);
    YTEntityImpl metadataDoc = calculateMetadata(ctx);

    if (propertyList == null || propertyList.size() == 0) {
      OIndexFactory factory = OIndexes.getFactory(type.getStringValue(), engine);

      YTType[] keyTypes = calculateKeyTypes(ctx);

      if (keyTypes != null && keyTypes.length > 0) {
        idx =
            database
                .getMetadata()
                .getIndexManagerInternal()
                .createIndex(
                    database,
                    name.getValue(),
                    type.getStringValue(),
                    new OSimpleKeyIndexDefinition(keyTypes, collatesList),
                    null,
                    null,
                    metadataDoc,
                    engine);
      } else if (keyTypes != null
          && keyTypes.length == 0
          && "LUCENE_CROSS_CLASS".equalsIgnoreCase(engine)) {
        // handle special case of cross class  Lucene index: awful but works
        OIndexDefinition keyDef =
            new OSimpleKeyIndexDefinition(new YTType[]{YTType.STRING}, collatesList);
        idx =
            database
                .getMetadata()
                .getIndexManagerInternal()
                .createIndex(
                    database,
                    name.getValue(),
                    type.getStringValue(),
                    keyDef,
                    null,
                    null,
                    metadataDoc,
                    engine);

      } else if (className == null && keyTypes == null || keyTypes.length == 0) {
        // legacy: create index without specifying property names
        String[] split = name.getValue().split("\\.");
        if (split.length != 2) {
          throw new YTDatabaseException(
              "Impossible to create an index without specify class and property name nor key types:"
                  + " "
                  + this);
        }
        YTClass oClass = database.getClass(split[0]);
        if (oClass == null) {
          throw new YTDatabaseException(
              "Impossible to create an index, class not found: " + split[0]);
        }
        if (oClass.getProperty(split[1]) == null) {
          throw new YTDatabaseException(
              "Impossible to create an index, property not found: " + name.getValue());
        }
        String[] fields = new String[]{split[1]};
        idx = getoIndex(oClass, fields, engine, database, collatesList, metadataDoc);

      } else {
        throw new YTDatabaseException(
            "Impossible to create an index without specify the key type or the associated property:"
                + " "
                + this);
      }
    } else {
      String[] fields = calculateProperties(ctx);
      YTClass oClass = getIndexClass(ctx);
      idx = getoIndex(oClass, fields, engine, database, collatesList, metadataDoc);
    }

    if (idx != null) {
      return database.computeInTx(() -> idx.getInternal().size(database));
    }

    return null;
  }

  private OIndex getoIndex(
      YTClass oClass,
      String[] fields,
      String engine,
      YTDatabaseSessionInternal database,
      List<OCollate> collatesList,
      YTEntityImpl metadataDoc) {
    OIndex idx;
    if ((keyTypes == null || keyTypes.size() == 0) && collatesList == null) {

      idx =
          oClass.createIndex(database,
              name.getValue(), type.getStringValue(), null, metadataDoc, engine, fields);
    } else {
      final List<YTType> fieldTypeList;
      if (keyTypes == null || keyTypes.size() == 0 && fields.length > 0) {
        for (final String fieldName : fields) {
          if (!fieldName.equals("@rid") && !oClass.existsProperty(fieldName)) {
            throw new YTIndexException(
                "Index with name : '"
                    + name.getValue()
                    + "' cannot be created on class : '"
                    + oClass.getName()
                    + "' because field: '"
                    + fieldName
                    + "' is absent in class definition.");
          }
        }
        fieldTypeList = ((YTClassImpl) oClass).extractFieldTypes(fields);
      } else {
        fieldTypeList =
            keyTypes.stream()
                .map(x -> YTType.valueOf(x.getStringValue()))
                .collect(Collectors.toList());
      }

      final OIndexDefinition idxDef =
          OIndexDefinitionFactory.createIndexDefinition(
              oClass,
              Arrays.asList(fields),
              fieldTypeList,
              collatesList,
              type.getStringValue(),
              engine);

      idx =
          database
              .getMetadata()
              .getIndexManagerInternal()
              .createIndex(
                  database,
                  name.getValue(),
                  type.getStringValue(),
                  idxDef,
                  oClass.getPolymorphicClusterIds(),
                  null,
                  metadataDoc,
                  engine);
    }
    return idx;
  }

  /**
   * * returns the list of property names to be indexed
   *
   * @param ctx
   * @return
   */
  private String[] calculateProperties(OCommandContext ctx) {
    if (propertyList == null) {
      return null;
    }
    return propertyList.stream()
        .map(x -> x.getCompleteKey())
        .collect(Collectors.toList())
        .toArray(new String[]{});
  }

  /**
   * calculates the indexed class based on the class name
   */
  private YTClass getIndexClass(OCommandContext ctx) {
    if (className == null) {
      return null;
    }
    YTClass result =
        ctx.getDatabase().getMetadata().getSchema().getClass(className.getStringValue());
    if (result == null) {
      throw new YTCommandExecutionException("Cannot find class " + className);
    }
    return result;
  }

  /**
   * returns index metadata as an ODocuemnt (as expected by Index API)
   */
  private YTEntityImpl calculateMetadata(OCommandContext ctx) {
    if (metadata == null) {
      return null;
    }
    return metadata.toDocument(null, ctx);
  }

  private YTType[] calculateKeyTypes(OCommandContext ctx) {
    if (keyTypes == null) {
      return new YTType[0];
    }
    return keyTypes.stream()
        .map(x -> YTType.valueOf(x.getStringValue()))
        .collect(Collectors.toList())
        .toArray(new YTType[]{});
  }

  private List<OCollate> calculateCollates(OCommandContext ctx) {
    List<OCollate> result = new ArrayList<>();
    boolean found = false;
    for (Property prop : this.propertyList) {
      String collate = prop.collate == null ? null : prop.collate.getStringValue();
      if (collate != null) {
        final OCollate col = OSQLEngine.getCollate(collate);
        result.add(col);
        found = true;
      } else {
        result.add(null);
      }
    }
    if (!found) {
      return null;
    }
    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE INDEX ");
    name.toString(params, builder);
    if (className != null) {
      builder.append(" ON ");
      className.toString(params, builder);
      builder.append(" (");
      boolean first = true;
      for (Property prop : propertyList) {
        if (!first) {
          builder.append(", ");
        }
        if (prop.name != null) {
          prop.name.toString(params, builder);
        } else {
          prop.recordAttribute.toString(params, builder);
        }
        if (prop.byKey) {
          builder.append(" BY KEY");
        } else if (prop.byValue) {
          builder.append(" BY VALUE");
        }
        if (prop.collate != null) {
          builder.append(" COLLATE ");
          prop.collate.toString(params, builder);
        }
        first = false;
      }
      builder.append(")");
    }
    builder.append(" ");
    type.toString(params, builder);
    if (engine != null) {
      builder.append(" ENGINE ");
      engine.toString(params, builder);
    }
    if (keyTypes != null && keyTypes.size() > 0) {
      boolean first = true;
      builder.append(" ");
      for (OIdentifier keyType : keyTypes) {
        if (!first) {
          builder.append(",");
        }
        keyType.toString(params, builder);
        first = false;
      }
    }
    if (metadata != null) {
      builder.append(" METADATA ");
      metadata.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CREATE INDEX ");
    name.toGenericStatement(builder);
    if (className != null) {
      builder.append(" ON ");
      className.toGenericStatement(builder);
      builder.append(" (");
      boolean first = true;
      for (Property prop : propertyList) {
        if (!first) {
          builder.append(", ");
        }
        if (prop.name != null) {
          prop.name.toGenericStatement(builder);
        } else {
          prop.recordAttribute.toGenericStatement(builder);
        }
        if (prop.byKey) {
          builder.append(" BY KEY");
        } else if (prop.byValue) {
          builder.append(" BY VALUE");
        }
        if (prop.collate != null) {
          builder.append(" COLLATE ");
          prop.collate.toGenericStatement(builder);
        }
        first = false;
      }
      builder.append(")");
    }
    builder.append(" ");
    type.toGenericStatement(builder);
    if (engine != null) {
      builder.append(" ENGINE ");
      engine.toGenericStatement(builder);
    }
    if (keyTypes != null && keyTypes.size() > 0) {
      boolean first = true;
      builder.append(" ");
      for (OIdentifier keyType : keyTypes) {
        if (!first) {
          builder.append(",");
        }
        keyType.toGenericStatement(builder);
        first = false;
      }
    }
    if (metadata != null) {
      builder.append(" METADATA ");
      metadata.toGenericStatement(builder);
    }
  }

  @Override
  public OCreateIndexStatement copy() {
    OCreateIndexStatement result = new OCreateIndexStatement(-1);
    result.name = name == null ? null : name.copy();
    result.className = className == null ? null : className.copy();
    result.propertyList =
        propertyList == null
            ? null
            : propertyList.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.type = type == null ? null : type.copy();
    result.engine = engine == null ? null : engine.copy();
    result.keyTypes =
        keyTypes == null ? null : keyTypes.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.metadata = metadata == null ? null : metadata.copy();
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

    OCreateIndexStatement that = (OCreateIndexStatement) o;

    if (!Objects.equals(name, that.name)) {
      return false;
    }
    if (!Objects.equals(className, that.className)) {
      return false;
    }
    if (!Objects.equals(propertyList, that.propertyList)) {
      return false;
    }
    if (!Objects.equals(type, that.type)) {
      return false;
    }
    if (!Objects.equals(engine, that.engine)) {
      return false;
    }
    if (!Objects.equals(keyTypes, that.keyTypes)) {
      return false;
    }
    return Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (className != null ? className.hashCode() : 0);
    result = 31 * result + (propertyList != null ? propertyList.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (engine != null ? engine.hashCode() : 0);
    result = 31 * result + (keyTypes != null ? keyTypes.hashCode() : 0);
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    return result;
  }

  public static class Property {

    protected OIdentifier name;
    protected ORecordAttribute recordAttribute;
    protected boolean byKey = false;
    protected boolean byValue = false;
    protected OIdentifier collate;

    public Property copy() {
      Property result = new Property();
      result.name = name == null ? null : name.copy();
      result.recordAttribute = recordAttribute == null ? null : recordAttribute.copy();
      result.byKey = byKey;
      result.byValue = byValue;
      result.collate = collate == null ? null : collate.copy();
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

      Property property = (Property) o;

      if (byKey != property.byKey) {
        return false;
      }
      if (byValue != property.byValue) {
        return false;
      }
      if (!Objects.equals(name, property.name)) {
        return false;
      }
      if (!Objects.equals(recordAttribute, property.recordAttribute)) {
        return false;
      }
      return Objects.equals(collate, property.collate);
    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (recordAttribute != null ? recordAttribute.hashCode() : 0);
      result = 31 * result + (byKey ? 1 : 0);
      result = 31 * result + (byValue ? 1 : 0);
      result = 31 * result + (collate != null ? collate.hashCode() : 0);
      return result;
    }

    /**
     * returns the complete key to index, eg. property name or "property by key/value"
     */
    public String getCompleteKey() {
      StringBuilder result = new StringBuilder();
      if (name != null) {
        result.append(name.getStringValue());
      } else if (recordAttribute != null) {
        result.append(recordAttribute.getName());
      }

      if (byKey) {
        result.append(" by key");
      }
      if (byValue) {
        result.append(" by value");
      }
      return result.toString();
    }
  }
}
/* JavaCC - OriginalChecksum=bd090e02c4346ad390a6b8c77f1b9dba (do not edit this line) */
