package com.orientechnologies.orient.core.exception;

import com.orientechnologies.orient.core.storage.index.sbtreebonsai.local.OSBTreeBonsaiLocal;

/**
 * @since 10/2/2015
 */
public class OSBTreeBonsaiLocalException extends YTDurableComponentException {

  public OSBTreeBonsaiLocalException(OSBTreeBonsaiLocalException exception) {
    super(exception);
  }

  public OSBTreeBonsaiLocalException(String message, OSBTreeBonsaiLocal component) {
    super(message, component);
  }
}
