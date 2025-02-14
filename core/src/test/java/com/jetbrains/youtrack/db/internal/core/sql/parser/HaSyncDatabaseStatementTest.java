package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class HaSyncDatabaseStatementTest extends ParserTestAbstract {

  @Test
  public void testPlain() {
    checkRightSyntax("HA SYNC DATABASE");
    checkRightSyntax("ha sync database");
    checkRightSyntax("HA SYNC DATABASE -force");
    checkRightSyntax("HA SYNC DATABASE -full");

    checkWrongSyntax("HA SYNC DATABASE foo");
  }
}
