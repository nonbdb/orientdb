package com.jetbrains.youtrack.db.internal.core.sql.parser;

import org.junit.Test;

public class CreateClusterStatementTest extends ParserTestAbstract {

  @Test
  public void testPlain() {
    checkRightSyntax("CREATE CLUSTER Foo");
    checkRightSyntax("CREATE CLUSTER Foo ID 14");
    checkRightSyntax("create cluster Foo");
    checkRightSyntax("create cluster Foo id 14");
    checkRightSyntax("CREATE BLOB CLUSTER Foo");
    checkRightSyntax("create blob cluster Foo id 14");

    checkRightSyntax("create blob cluster Foo IF NOT EXISTS");
    checkRightSyntax("create blob cluster Foo IF NOT EXISTS id 14");

    checkWrongSyntax("CREATE Cluster");
    checkWrongSyntax("CREATE Cluster foo bar");
    checkWrongSyntax("CREATE Cluster foo.bar");
    checkWrongSyntax("CREATE Cluster foo id bar");

    checkWrongSyntax("create blob cluster Foo IF EXISTS");
    checkWrongSyntax("create blob cluster Foo IF EXISTS id 14");
  }
}
