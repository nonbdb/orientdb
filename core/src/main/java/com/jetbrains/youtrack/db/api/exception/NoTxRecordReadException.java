package com.jetbrains.youtrack.db.api.exception;

public class NoTxRecordReadException extends DatabaseException implements HighLevelException {

  public NoTxRecordReadException(String dbName, String message) {
    super(dbName, message);
  }

  public NoTxRecordReadException(String message) {
    super(message);
  }
}
