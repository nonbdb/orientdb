/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.server.network.protocol.http.command;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.SecurityAccessException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.common.concur.lock.LockException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityUserImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;
import com.jetbrains.youtrack.db.internal.server.OTokenHandler;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpRequest;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpRequestException;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpResponse;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpSession;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpUtils;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Database based authenticated command. Authenticates against the database taken as second
 * parameter of the URL. The URL must be in this format:
 *
 * <p>
 *
 * <pre>
 * <command>/<database>[/...]
 * </pre>
 */
public abstract class ServerCommandAuthenticatedDbAbstract extends ServerCommandAbstract {

  public static final char DBNAME_DIR_SEPARATOR = '$';
  public static final String SESSIONID_UNAUTHORIZED = "-";
  public static final String SESSIONID_LOGOUT = "!";
  private volatile OTokenHandler tokenHandler;

  @Override
  public boolean beforeExecute(final HttpRequest iRequest, HttpResponse iResponse)
      throws IOException {
    super.beforeExecute(iRequest, iResponse);

    try {
      init();

      final String[] urlParts = iRequest.getUrl().substring(1).split("/");
      if (urlParts.length < 2) {
        throw new HttpRequestException(
            "Syntax error in URL. Expected is: <command>/<database>[/...]");
      }

      iRequest.setDatabaseName(URLDecoder.decode(urlParts[1], StandardCharsets.UTF_8));
      if (iRequest.getBearerTokenRaw() != null) {
        // Bearer authentication
        try {
          iRequest.setBearerToken(
              tokenHandler.parseOnlyWebToken(iRequest.getBearerTokenRaw().getBytes()));
        } catch (Exception e) {
          // TODO: Catch all expected exceptions correctly!
          LogManager.instance().warn(this, "Bearer token parsing failed", e);
        }

        if (iRequest.getBearerToken() == null
            || !iRequest.getBearerToken().getToken().getIsVerified()) {
          // Token parsing or verification failed - for now fail silently.
          sendAuthorizationRequest(iRequest, iResponse, iRequest.getDatabaseName());
          return false;
        }

        // CHECK THE REQUEST VALIDITY
        tokenHandler.validateToken(iRequest.getBearerToken(), urlParts[0], urlParts[1]);
        if (!iRequest.getBearerToken().getToken().getIsValid()) {

          // SECURITY PROBLEM: CROSS DATABASE REQUEST!
          LogManager.instance()
              .warn(
                  this,
                  "Token '%s' is not valid for database '%s'",
                  iRequest.getBearerTokenRaw(),
                  iRequest.getDatabaseName());
          sendAuthorizationRequest(iRequest, iResponse, iRequest.getDatabaseName());
          return false;
        }

        return iRequest.getBearerToken().getToken().getIsValid();
      } else {
        // HTTP basic authentication
        final List<String> authenticationParts =
            iRequest.getAuthorization() != null
                ? StringSerializerHelper.split(iRequest.getAuthorization(), ':')
                : null;

        HttpSession currentSession;
        if (iRequest.getSessionId() != null && iRequest.getSessionId().length() > 1) {
          currentSession = server.getHttpSessionManager().getSession(iRequest.getSessionId());
          if (currentSession != null && authenticationParts != null) {
            if (!currentSession.getUserName().equals(authenticationParts.get(0))) {
              // CHANGED USER, INVALIDATE THE SESSION
              currentSession = null;
            }
          }
        } else {
          currentSession = null;
        }

        if (currentSession == null) {
          // NO SESSION
          if (iRequest.getAuthorization() == null
              || SESSIONID_LOGOUT.equals(iRequest.getSessionId())) {
            iResponse.setSessionId(SESSIONID_UNAUTHORIZED);
            sendAuthorizationRequest(iRequest, iResponse, iRequest.getDatabaseName());
            return false;
          } else {
            return authenticate(
                iRequest, iResponse, authenticationParts, iRequest.getDatabaseName());
          }

        } else {
          // CHECK THE SESSION VALIDITY
          if (!currentSession.getDatabaseName().equals(iRequest.getDatabaseName())) {

            // SECURITY PROBLEM: CROSS DATABASE REQUEST!
            LogManager.instance()
                .warn(
                    this,
                    "Session %s is trying to access to the database '%s', but has been"
                        + " authenticated against the database '%s'",
                    iRequest.getSessionId(),
                    iRequest.getDatabaseName(),
                    currentSession.getDatabaseName());
            server.getHttpSessionManager().removeSession(iRequest.getSessionId());
            sendAuthorizationRequest(iRequest, iResponse, iRequest.getDatabaseName());
            return false;

          } else if (authenticationParts != null
              && !currentSession.getUserName().equals(authenticationParts.get(0))) {

            // SECURITY PROBLEM: CROSS DATABASE REQUEST!
            LogManager.instance()
                .warn(
                    this,
                    "Session %s is trying to access to the database '%s' with user '%s', but has"
                        + " been authenticated with user '%s'",
                    iRequest.getSessionId(),
                    iRequest.getDatabaseName(),
                    authenticationParts.get(0),
                    currentSession.getUserName());
            server.getHttpSessionManager().removeSession(iRequest.getSessionId());
            sendAuthorizationRequest(iRequest, iResponse, iRequest.getDatabaseName());
            return false;
          }

          return true;
        }
      }
    } catch (Exception e) {
      throw BaseException.wrapException(new HttpRequestException("Error on authentication"), e);
    } finally {
      // clear local cache to ensure that zomby records will not pile up in cache.
      try {
        if (iRequest.getDatabaseName() != null) {
          DatabaseSessionInternal db = getProfiledDatabaseInstance(iRequest);
          if (db != null && !db.getTransaction().isActive()) {
            db.activateOnCurrentThread();
            db.getLocalCache().clear();
          }
        }
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Override
  public boolean afterExecute(final HttpRequest iRequest, HttpResponse iResponse)
      throws IOException {
    DatabaseRecordThreadLocal.instance().remove();
    iRequest.getExecutor().setDatabase(null);
    return true;
  }

  protected boolean authenticate(
      final HttpRequest iRequest,
      final HttpResponse iResponse,
      final List<String> iAuthenticationParts,
      final String iDatabaseName)
      throws IOException {
    DatabaseSessionInternal db = null;
    try {
      db =
          server.openDatabase(
              iDatabaseName, iAuthenticationParts.get(0), iAuthenticationParts.get(1));
      // if (db.geCurrentUser() == null)
      // // MAYBE A PREVIOUS ROOT REALM? UN AUTHORIZE
      // return false;

      // Set user rid after authentication
      iRequest.getData().currentUserId =
          db.geCurrentUser() == null ? "<server user>"
              : db.geCurrentUser().getIdentity(db).toString();

      // AUTHENTICATED: CREATE THE SESSION
      iRequest.setSessionId(
          server
              .getHttpSessionManager()
              .createSession(
                  iDatabaseName, iAuthenticationParts.get(0), iAuthenticationParts.get(1)));
      iResponse.setSessionId(iRequest.getSessionId());
      return true;

    } catch (SecurityAccessException e) {
      // WRONG USER/PASSWD
    } catch (LockException e) {
      LogManager.instance()
          .error(this, "Cannot access to the database '" + iDatabaseName + "'", e);
    } finally {
      if (db == null) {
        // WRONG USER/PASSWD
        sendAuthorizationRequest(iRequest, iResponse, iDatabaseName);
      } else {
        db.close();
      }
    }
    return false;
  }

  protected void sendAuthorizationRequest(
      final HttpRequest iRequest, final HttpResponse iResponse, final String iDatabaseName)
      throws IOException {
    // UNAUTHORIZED
    iRequest.setSessionId(SESSIONID_UNAUTHORIZED);

    String header = null;
    String xRequestedWithHeader = iRequest.getHeader("X-Requested-With");
    if (xRequestedWithHeader == null || !xRequestedWithHeader.equals("XMLHttpRequest")) {
      // Defaults to "WWW-Authenticate: Basic" if not an AJAX Request.
      header = server.getSecurity().getAuthenticationHeader(iDatabaseName);

      Map<String, String> headers = server.getSecurity().getAuthenticationHeaders(iDatabaseName);
      headers.entrySet().forEach(s -> iResponse.addHeader(s.getKey(), s.getValue()));
    }

    if (isJsonResponse(iResponse)) {
      sendJsonError(
          iResponse,
          HttpUtils.STATUS_AUTH_CODE,
          HttpUtils.STATUS_AUTH_DESCRIPTION,
          HttpUtils.CONTENT_TEXT_PLAIN,
          "401 Unauthorized.",
          header);
    } else {
      iResponse.send(
          HttpUtils.STATUS_AUTH_CODE,
          HttpUtils.STATUS_AUTH_DESCRIPTION,
          HttpUtils.CONTENT_TEXT_PLAIN,
          "401 Unauthorized.",
          header);
    }
  }

  protected DatabaseSessionInternal getProfiledDatabaseInstance(final HttpRequest iRequest)
      throws InterruptedException {
    if (iRequest.getBearerToken() != null) {
      return getProfiledDatabaseInstanceToken(iRequest);
    } else {
      return getProfiledDatabaseInstanceBasic(iRequest);
    }
  }

  protected DatabaseSessionInternal getProfiledDatabaseInstanceToken(final HttpRequest iRequest)
      throws InterruptedException {
    // after authentication, if current login user is different compare with current DB user, reset
    // DB user to login user
    DatabaseSessionInternal localDatabase = DatabaseRecordThreadLocal.instance().getIfDefined();
    if (localDatabase == null) {
      localDatabase = server.openDatabase(iRequest.getDatabaseName(), iRequest.getBearerToken());
    } else {
      RID currentUserId = iRequest.getBearerToken().getToken().getUserId();
      if (currentUserId != null && localDatabase.geCurrentUser() != null) {
        if (!currentUserId.equals(
            localDatabase.geCurrentUser().getIdentity(localDatabase).getIdentity())) {
          EntityImpl userDoc = localDatabase.load(currentUserId);
          localDatabase.setUser(new SecurityUserImpl(localDatabase, userDoc));
        }
      }
    }

    iRequest.getData().lastDatabase = localDatabase.getName();
    iRequest.getData().lastUser =
        localDatabase.geCurrentUser() != null ? localDatabase.geCurrentUser().getName(localDatabase)
            : null;
    return localDatabase.getDatabaseOwner();
  }

  protected DatabaseSessionInternal getProfiledDatabaseInstanceBasic(
      final HttpRequest iRequest) {
    final HttpSession session = server.getHttpSessionManager().getSession(iRequest.getSessionId());

    if (session == null) {
      throw new SecurityAccessException(iRequest.getDatabaseName(), "No session active");
    }

    // after authentication, if current login user is different compare with current DB user, reset
    // DB user to login user
    DatabaseSessionInternal localDatabase = DatabaseRecordThreadLocal.instance().getIfDefined();

    if (localDatabase == null) {
      localDatabase =
          server.openDatabase(
              iRequest.getDatabaseName(), session.getUserName(), session.getUserPassword());
    } else {
      String currentUserId = iRequest.getData().currentUserId;
      if (currentUserId != null && !currentUserId.isEmpty()
          && localDatabase.geCurrentUser() != null) {
        if (!currentUserId.equals(
            localDatabase.geCurrentUser().getIdentity(localDatabase).toString())) {
          EntityImpl userDoc = localDatabase.load(new RecordId(currentUserId));
          localDatabase.setUser(new SecurityUserImpl(localDatabase, userDoc));
        }
      }
    }

    iRequest.getData().lastDatabase = localDatabase.getName();
    iRequest.getData().lastUser =
        localDatabase.geCurrentUser() != null ? localDatabase.geCurrentUser().getName(localDatabase)
            : null;
    iRequest.getExecutor().setDatabase(localDatabase);
    return localDatabase.getDatabaseOwner();
  }

  private void init() {
    if (tokenHandler == null
        && server
        .getContextConfiguration()
        .getValueAsBoolean(GlobalConfiguration.NETWORK_HTTP_USE_TOKEN)) {
      tokenHandler = server.getTokenHandler();
    }
  }
}
