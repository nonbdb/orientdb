package com.orientechnologies.orient.core.security.authenticator;

import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.YTSecurityAccessException;
import com.orientechnologies.orient.core.metadata.security.OSecurityShared;
import com.orientechnologies.orient.core.metadata.security.YTSecurityUser;
import com.orientechnologies.orient.core.metadata.security.YTUser;
import com.orientechnologies.orient.core.metadata.security.auth.OAuthenticationInfo;
import com.orientechnologies.orient.core.metadata.security.auth.OTokenAuthInfo;
import com.orientechnologies.orient.core.metadata.security.auth.OUserPasswordAuthInfo;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.security.OParsedToken;
import com.orientechnologies.orient.core.security.OSecuritySystem;
import com.orientechnologies.orient.core.security.OTokenSign;
import com.orientechnologies.orient.enterprise.channel.binary.YTTokenSecurityException;

public class ODatabaseUserAuthenticator extends OSecurityAuthenticatorAbstract {

  private OTokenSign tokenSign;

  @Override
  public void config(YTDatabaseSessionInternal session, YTEntityImpl jsonConfig,
      OSecuritySystem security) {
    super.config(session, jsonConfig, security);
    tokenSign = security.getTokenSign();
  }

  @Override
  public YTSecurityUser authenticate(YTDatabaseSessionInternal session, OAuthenticationInfo info) {
    if (info instanceof OUserPasswordAuthInfo) {
      return authenticate(
          session,
          ((OUserPasswordAuthInfo) info).getUser(),
          ((OUserPasswordAuthInfo) info).getPassword());
    } else if (info instanceof OTokenAuthInfo) {
      OParsedToken token = ((OTokenAuthInfo) info).getToken();

      if (tokenSign != null && !tokenSign.verifyTokenSign(token)) {
        throw new YTTokenSecurityException("The token provided is expired");
      }
      if (!token.getToken().getIsValid()) {
        throw new YTSecurityAccessException(session.getName(), "Token not valid");
      }

      YTUser user = token.getToken().getUser(session);
      if (user == null && token.getToken().getUserName() != null) {
        OSecurityShared databaseSecurity =
            (OSecurityShared) session.getSharedContext().getSecurity();
        user = OSecurityShared.getUserInternal(session, token.getToken().getUserName());
      }
      return user;
    }
    return super.authenticate(session, info);
  }

  @Override
  public YTSecurityUser authenticate(YTDatabaseSessionInternal session, String username,
      String password) {
    if (session == null) {
      return null;
    }

    String dbName = session.getName();
    YTUser user = OSecurityShared.getUserInternal(session, username);
    if (user == null) {
      return null;
    }
    if (user.getAccountStatus(session) != YTSecurityUser.STATUSES.ACTIVE) {
      throw new YTSecurityAccessException(dbName, "User '" + username + "' is not active");
    }

    // CHECK USER & PASSWORD
    if (!user.checkPassword(session, password)) {
      // WAIT A BIT TO AVOID BRUTE FORCE
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }
      throw new YTSecurityAccessException(
          dbName, "User or password not valid for database: '" + dbName + "'");
    }

    return user;
  }

  @Override
  public YTSecurityUser getUser(String username, YTDatabaseSessionInternal session) {
    return null;
  }

  @Override
  public boolean isAuthorized(YTDatabaseSessionInternal session, String username, String resource) {
    return false;
  }

  @Override
  public boolean isSingleSignOnSupported() {
    return false;
  }
}
