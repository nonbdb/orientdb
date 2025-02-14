package com.jetbrains.youtrack.db.internal.server.token;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityUserIml;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Token;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.JwtPayload;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.TokenHeader;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.YouTrackDBJwtHeader;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;

/**
 *
 */
public class JsonWebToken implements
    com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.JsonWebToken, Token {

  public final TokenHeader header;
  public final JwtPayload payload;
  private boolean isVerified;
  private boolean isValid;

  public JsonWebToken() {
    this(new YouTrackDBJwtHeader(), new YouTrackDBJwtPayload());
  }

  public JsonWebToken(TokenHeader header, JwtPayload payload) {
    isVerified = false;
    isValid = false;
    this.header = header;
    this.payload = payload;
  }

  @Override
  public TokenHeader getHeader() {
    return header;
  }

  @Override
  public JwtPayload getPayload() {
    return payload;
  }

  @Override
  public boolean getIsVerified() {
    return isVerified;
  }

  @Override
  public void setIsVerified(boolean verified) {
    this.isVerified = verified;
  }

  @Override
  public boolean getIsValid() {
    return this.isValid;
  }

  @Override
  public void setIsValid(boolean valid) {
    this.isValid = valid;
  }

  @Override
  public String getUserName() {
    return payload.getUserName();
  }

  @Override
  public String getDatabase() {
    return payload.getDatabase();
  }

  @Override
  public long getExpiry() {
    return payload.getExpiry();
  }

  @Override
  public RID getUserId() {
    return payload.getUserRid();
  }

  @Override
  public String getDatabaseType() {
    return payload.getDatabaseType();
  }

  @Override
  public SecurityUserIml getUser(DatabaseSessionInternal db) {
    RID userRid = payload.getUserRid();
    EntityImpl result;
    result = db.load(userRid);
    if (!EntityInternalUtils.getImmutableSchemaClass(result).isOuser()) {
      result = null;
    }
    return new SecurityUserIml(db, result);
  }

  @Override
  public void setExpiry(long expiry) {
    this.payload.setExpiry(expiry);
  }

  @Override
  public boolean isNowValid() {
    long now = System.currentTimeMillis();
    return getExpiry() > now && payload.getNotBefore() < now;
  }

  @Override
  public boolean isCloseToExpire() {
    long now = System.currentTimeMillis();
    return getExpiry() - 120000 <= now;
  }
}
