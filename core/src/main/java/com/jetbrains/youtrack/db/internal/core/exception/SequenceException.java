package com.jetbrains.youtrack.db.internal.core.exception;

/**
 * @since 2/28/2015
 */
public class SequenceException extends CoreException {

  private static final long serialVersionUID = -2719447287841577672L;

  public SequenceException(SequenceException exception) {
    super(exception);
  }

  public SequenceException(String message) {
    super(message);
  }
}
