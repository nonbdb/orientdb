package com.orientechnologies.orient.core.command.script;

import com.orientechnologies.orient.core.command.script.transformer.OScriptTransformer;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.resultset.OIteratorResultSet;
import java.util.Iterator;

/**
 * Wrapper of OIteratorResultSet Used in script results with conversion to OResult for single
 * iteration
 */
public class OScriptResultSet extends OIteratorResultSet {

  protected OScriptTransformer transformer;

  public OScriptResultSet(ODatabaseSessionInternal db, Iterator iter,
      OScriptTransformer transformer) {
    super(db, iter);
    this.transformer = transformer;
  }

  @Override
  public OResult next() {

    Object next = iterator.next();
    return transformer.toResult(db, next);
  }
}
