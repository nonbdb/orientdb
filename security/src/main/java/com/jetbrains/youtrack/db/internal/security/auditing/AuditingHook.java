/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.jetbrains.youtrack.db.internal.security.auditing;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.RecordHookAbstract;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.security.SecurityUser;
import com.jetbrains.youtrack.db.api.session.SessionListener;
import com.jetbrains.youtrack.db.internal.common.parser.VariableParser;
import com.jetbrains.youtrack.db.internal.common.parser.VariableParserListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.SystemDatabase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.security.AuditingOperation;
import com.jetbrains.youtrack.db.internal.core.security.SecuritySystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Hook to audit database access.
 */
public class AuditingHook extends RecordHookAbstract implements SessionListener {

  private final Map<String, AuditingClassConfig> classes =
      new HashMap<String, AuditingClassConfig>(20);
  private final AuditingLoggingThread auditingThread;

  private final Map<DatabaseSession, List<Map<String, ?>>> operations = new ConcurrentHashMap<>();
  private volatile LinkedBlockingQueue<Map<String, ?>> auditingQueue;
  private final Set<AuditingCommandConfig> commands = new HashSet<AuditingCommandConfig>();
  private boolean onGlobalCreate;
  private boolean onGlobalRead;
  private boolean onGlobalUpdate;
  private boolean onGlobalDelete;
  private AuditingClassConfig defaultConfig = new AuditingClassConfig();
  private AuditingSchemaConfig schemaConfig;
  private EntityImpl iConfiguration;

  private static class AuditingCommandConfig {

    public String regex;
    public String message;

    public AuditingCommandConfig(final EntityImpl cfg) {
      regex = cfg.field("regex");
      message = cfg.field("message");
    }
  }

  private static class AuditingClassConfig {

    public boolean polymorphic = true;
    public boolean onCreateEnabled = false;
    public String onCreateMessage;
    public boolean onReadEnabled = false;
    public String onReadMessage;
    public boolean onUpdateEnabled = false;
    public String onUpdateMessage;
    public boolean onUpdateChanges = true;
    public boolean onDeleteEnabled = false;
    public String onDeleteMessage;

    public AuditingClassConfig() {
    }

    public AuditingClassConfig(final EntityImpl cfg) {
      if (cfg.containsField("polymorphic")) {
        polymorphic = cfg.field("polymorphic");
      }

      // CREATE
      if (cfg.containsField("onCreateEnabled")) {
        onCreateEnabled = cfg.field("onCreateEnabled");
      }
      if (cfg.containsField("onCreateMessage")) {
        onCreateMessage = cfg.field("onCreateMessage");
      }

      // READ
      if (cfg.containsField("onReadEnabled")) {
        onReadEnabled = cfg.field("onReadEnabled");
      }
      if (cfg.containsField("onReadMessage")) {
        onReadMessage = cfg.field("onReadMessage");
      }

      // UPDATE
      if (cfg.containsField("onUpdateEnabled")) {
        onUpdateEnabled = cfg.field("onUpdateEnabled");
      }
      if (cfg.containsField("onUpdateMessage")) {
        onUpdateMessage = cfg.field("onUpdateMessage");
      }
      if (cfg.containsField("onUpdateChanges")) {
        onUpdateChanges = cfg.field("onUpdateChanges");
      }

      // DELETE
      if (cfg.containsField("onDeleteEnabled")) {
        onDeleteEnabled = cfg.field("onDeleteEnabled");
      }
      if (cfg.containsField("onDeleteMessage")) {
        onDeleteMessage = cfg.field("onDeleteMessage");
      }
    }
  }

  // Handles the auditing-config "schema" configuration.
  private static class AuditingSchemaConfig extends AuditingConfig {
    private boolean onCreateClassEnabled = false;
    private final String onCreateClassMessage;

    private boolean onDropClassEnabled = false;
    private final String onDropClassMessage;

    public AuditingSchemaConfig(final EntityImpl cfg) {
      if (cfg.containsField("onCreateClassEnabled")) {
        onCreateClassEnabled = cfg.field("onCreateClassEnabled");
      }

      onCreateClassMessage = cfg.field("onCreateClassMessage");

      if (cfg.containsField("onDropClassEnabled")) {
        onDropClassEnabled = cfg.field("onDropClassEnabled");
      }

      onDropClassMessage = cfg.field("onDropClassMessage");
    }

    @Override
    public String formatMessage(final AuditingOperation op, final String subject) {
      if (op == AuditingOperation.CREATEDCLASS) {
        return resolveMessage(onCreateClassMessage, "class", subject);
      } else if (op == AuditingOperation.DROPPEDCLASS) {
        return resolveMessage(onDropClassMessage, "class", subject);
      }

      return subject;
    }

    @Override
    public boolean isEnabled(AuditingOperation op) {
      if (op == AuditingOperation.CREATEDCLASS) {
        return onCreateClassEnabled;
      } else if (op == AuditingOperation.DROPPEDCLASS) {
        return onDropClassEnabled;
      }

      return false;
    }
  }

  public AuditingHook(final EntityImpl iConfiguration, final SecuritySystem system) {
    this.iConfiguration = iConfiguration;

    onGlobalCreate = onGlobalRead = onGlobalUpdate = onGlobalDelete = false;

    final EntityImpl classesCfg = iConfiguration.field("classes");
    if (classesCfg != null) {
      for (var c : classesCfg.fieldNames()) {
        final var cfg = new AuditingClassConfig(classesCfg.field(c));
        if (c.equals("*")) {
          defaultConfig = cfg;
        } else {
          classes.put(c, cfg);
        }

        if (cfg.onCreateEnabled) {
          onGlobalCreate = true;
        }
        if (cfg.onReadEnabled) {
          onGlobalRead = true;
        }
        if (cfg.onUpdateEnabled) {
          onGlobalUpdate = true;
        }
        if (cfg.onDeleteEnabled) {
          onGlobalDelete = true;
        }
      }
    }

    final Iterable<EntityImpl> commandCfg = iConfiguration.field("commands");

    if (commandCfg != null) {

      for (var cfg : commandCfg) {
        commands.add(new AuditingCommandConfig(cfg));
      }
    }

    final EntityImpl schemaCfgDoc = iConfiguration.field("schema");
    if (schemaCfgDoc != null) {
      schemaConfig = new AuditingSchemaConfig(schemaCfgDoc);
    }

    auditingQueue = new LinkedBlockingQueue<>();
    auditingThread =
        new AuditingLoggingThread(
            DatabaseRecordThreadLocal.instance().get().getName(),
            auditingQueue,
            system.getContext(),
            system);

    auditingThread.start();
  }

  public AuditingHook(final SecuritySystem server) {
    auditingQueue = new LinkedBlockingQueue<>();
    auditingThread =
        new AuditingLoggingThread(
            SystemDatabase.SYSTEM_DB_NAME, auditingQueue, server.getContext(), server);

    auditingThread.start();
  }

  @Override
  public void onBeforeTxBegin(DatabaseSession iDatabase) {
  }

  @Override
  public void onBeforeTxRollback(DatabaseSession iDatabase) {
  }

  @Override
  public void onAfterTxRollback(DatabaseSession iDatabase) {

    synchronized (operations) {
      operations.remove(iDatabase);
    }
  }

  @Override
  public void onBeforeTxCommit(DatabaseSession iDatabase) {
  }

  @Override
  public void onAfterTxCommit(DatabaseSession iDatabase) {
    List<Map<String, ?>> entries;

    synchronized (operations) {
      entries = operations.remove(iDatabase);
    }
    if (entries != null) {
      for (var oDocument : entries) {
        auditingQueue.offer(oDocument);
      }
    }
  }

  public EntityImpl getConfiguration() {
    return iConfiguration;
  }

  @Override
  public void onRecordAfterCreate(DatabaseSession db, final DBRecord iRecord) {
    if (!onGlobalCreate) {
      return;
    }

    log(db, AuditingOperation.CREATED, iRecord);
  }

  @Override
  public void onRecordAfterRead(DatabaseSession db, final DBRecord iRecord) {
    if (!onGlobalRead) {
      return;
    }

    log(db, AuditingOperation.LOADED, iRecord);
  }

  @Override
  public void onRecordAfterUpdate(DatabaseSession db, final DBRecord iRecord) {

    if (iRecord instanceof EntityImpl entity) {
      var clazz = EntityInternalUtils.getImmutableSchemaClass(
          (DatabaseSessionInternal) db, entity);

      if (clazz.isUser() && Arrays.asList(entity.getDirtyFields()).contains("password")) {
        String name = entity.getProperty("name");
        var message = String.format("The password for user '%s' has been changed", name);
        log(db, AuditingOperation.CHANGED_PWD, db.getName(), db.geCurrentUser(), message);
      }
    }
    if (!onGlobalUpdate) {
      return;
    }

    log(db, AuditingOperation.UPDATED, iRecord);
  }

  @Override
  public void onRecordAfterDelete(DatabaseSession db, final DBRecord iRecord) {
    if (!onGlobalDelete) {
      return;
    }

    log(db, AuditingOperation.DELETED, iRecord);
  }

  @Override
  public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
    return DISTRIBUTED_EXECUTION_MODE.SOURCE_NODE;
  }

  protected void logCommand(final String command) {
    if (auditingQueue == null) {
      return;
    }

    for (var cfg : commands) {
      if (command.matches(cfg.regex)) {
        final var db = DatabaseRecordThreadLocal.instance().get();

        final Map<String, ?> entity =
            createLogEntry(db
                , AuditingOperation.COMMAND,
                db.getName(),
                db.geCurrentUser(), formatCommandNote(command, cfg.message));
        auditingQueue.offer(entity);
      }
    }
  }

  private static String formatCommandNote(final String command, String message) {
    if (message == null || message.isEmpty()) {
      return command;
    }
    return (String)
        VariableParser.resolveVariables(
            message,
            "${",
            "}",
            variable -> {
              if (variable.startsWith("command")) {
                return command;
              }
              return null;
            });
  }

  protected void log(DatabaseSession db, final AuditingOperation operation,
      final DBRecord iRecord) {
    if (auditingQueue == null)
    // LOGGING THREAD INACTIVE, SKIP THE LOG
    {
      return;
    }

    final var cfg = getAuditConfiguration(iRecord);
    if (cfg == null)
    // SKIP
    {
      return;
    }

    EntityImpl changes = null;
    String note = null;

    switch (operation) {
      case CREATED:
        if (!cfg.onCreateEnabled)
        // SKIP
        {
          return;
        }
        note = cfg.onCreateMessage;
        break;
      case UPDATED:
        if (!cfg.onUpdateEnabled)
        // SKIP
        {
          return;
        }
        note = cfg.onUpdateMessage;

        if (iRecord instanceof EntityImpl entity && cfg.onUpdateChanges) {
          changes = new EntityImpl((DatabaseSessionInternal) db);

          for (var f : entity.getDirtyFields()) {
            var fieldChanges = new EntityImpl(null);
            fieldChanges.field("from", entity.getOriginalValue(f));
            fieldChanges.field("to", (Object) entity.rawField(f));
            changes.field(f, fieldChanges, PropertyType.EMBEDDED);
          }
        }
        break;
      case DELETED:
        if (!cfg.onDeleteEnabled)
        // SKIP
        {
          return;
        }
        note = cfg.onDeleteMessage;
        break;
    }

    var entity =
        createLogEntry(db, operation, db.getName(), db.geCurrentUser(),
            formatNote(iRecord, note));
    entity.put("record", iRecord.getIdentity());
    if (changes != null) {
      entity.put("changes", changes);
    }

    if (((DatabaseSessionInternal) db).getTransaction().isActive()) {
      synchronized (operations) {
        var entries = operations.computeIfAbsent(db, k -> new ArrayList<>());
        entries.add(entity);
      }
    } else {
      auditingQueue.offer(entity);
    }
  }

  private static String formatNote(final DBRecord iRecord, final String iNote) {
    if (iNote == null) {
      return null;
    }

    return (String)
        VariableParser.resolveVariables(
            iNote,
            "${",
            "}",
            new VariableParserListener() {
              @Override
              public Object resolve(final String iVariable) {
                if (iVariable.startsWith("field.")) {
                  if (iRecord instanceof EntityImpl) {
                    final var fieldName = iVariable.substring("field.".length());
                    return ((EntityImpl) iRecord).field(fieldName);
                  }
                }
                return null;
              }
            });
  }

  private AuditingClassConfig getAuditConfiguration(final DBRecord iRecord) {
    AuditingClassConfig cfg = null;

    if (iRecord instanceof EntityImpl) {
      var cls = ((EntityImpl) iRecord).getSchemaClass();
      if (cls != null) {

        if (cls.getName().equals(DefaultAuditing.AUDITING_LOG_CLASSNAME))
        // SKIP LOG CLASS
        {
          return null;
        }

        cfg = classes.get(cls.getName());

        // BROWSE SUPER CLASSES UP TO ROOT
        while (cfg == null && cls != null) {
          cls = cls.getSuperClass();
          if (cls != null) {
            cfg = classes.get(cls.getName());
            if (cfg != null && !cfg.polymorphic) {
              // NOT POLYMORPHIC: IGNORE IT AND EXIT FROM THE LOOP
              cfg = null;
              break;
            }
          }
        }
      }
    }

    if (cfg == null)
    // ASSIGN DEFAULT CFG (*)
    {
      cfg = defaultConfig;
    }

    return cfg;
  }

  public void shutdown(final boolean waitForAllLogs) {
    if (auditingThread != null) {
      auditingThread.sendShutdown(waitForAllLogs);
      auditingQueue = null;
    }
  }

  protected void logClass(final AuditingOperation operation, final String note) {
    final var db = DatabaseRecordThreadLocal.instance().get();

    final var user = db.geCurrentUser();

    var entity = createLogEntry(db, operation, db.getName(), user, note);
    auditingQueue.offer(entity);
  }

  protected void logClass(final AuditingOperation operation, final SchemaClass cls) {
    if (schemaConfig != null && schemaConfig.isEnabled(operation)) {
      logClass(operation, schemaConfig.formatMessage(operation, cls.getName()));
    }
  }

  public void onCreateClass(SchemaClass iClass) {
    logClass(AuditingOperation.CREATEDCLASS, iClass);
  }

  public void onDropClass(SchemaClass iClass) {
    logClass(AuditingOperation.DROPPEDCLASS, iClass);
  }

  public void log(
      DatabaseSession db, final AuditingOperation operation,
      final String dbName,
      SecurityUser user,
      final String message) {
    if (auditingQueue != null) {
      auditingQueue.offer(createLogEntry(db, operation, dbName, user, message));
    }
  }

  private static Map<String, Object> createLogEntry(
      DatabaseSession session, final AuditingOperation operation,
      final String dbName,
      SecurityUser user,
      final String message) {
    final var entity = new HashMap<String, Object>();

    entity.put("date", System.currentTimeMillis());
    entity.put("operation", operation.getByte());

    if (user != null) {
      entity.put("user", user.getName((DatabaseSessionInternal) session));
      entity.put("userType", user.getUserType());
    }

    if (message != null) {
      entity.put("note", message);
    }

    if (dbName != null) {
      entity.put("database", dbName);
    }

    return entity;
  }
}
