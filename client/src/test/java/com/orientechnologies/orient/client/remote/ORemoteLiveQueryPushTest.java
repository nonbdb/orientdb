package com.orientechnologies.orient.client.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.orient.client.remote.message.OLiveQueryPushRequest;
import com.orientechnologies.orient.client.remote.message.live.OLiveQueryResult;
import com.orientechnologies.orient.core.config.OContextConfiguration;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.OLiveQueryResultListener;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 *
 */
public class ORemoteLiveQueryPushTest {

  private static class MockLiveListener implements OLiveQueryResultListener {

    public int countCreate = 0;
    public int countUpdate = 0;
    public int countDelete = 0;
    public boolean end;

    @Override
    public void onCreate(YTDatabaseSession database, OResult data) {
      countCreate++;
    }

    @Override
    public void onUpdate(YTDatabaseSession database, OResult before, OResult after) {
      countUpdate++;
    }

    @Override
    public void onDelete(YTDatabaseSession database, OResult data) {
      countDelete++;
    }

    @Override
    public void onError(YTDatabaseSession database, YTException exception) {
    }

    @Override
    public void onEnd(YTDatabaseSession database) {
      assertFalse(end);
      end = true;
    }
  }

  private OStorageRemote storage;

  @Mock
  private ORemoteConnectionManager connectionManager;

  @Mock
  private YTDatabaseSessionInternal database;

  @Before
  public void before() throws IOException {
    MockitoAnnotations.initMocks(this);
    storage =
        new OStorageRemote(
            new ORemoteURLs(new String[]{}, new OContextConfiguration()),
            "none",
            null,
            "",
            connectionManager,
            null);
  }

  @Test
  public void testLiveEvents() {
    MockLiveListener mock = new MockLiveListener();
    storage.registerLiveListener(10, new OLiveQueryClientListener(database, mock));
    List<OLiveQueryResult> events = new ArrayList<>();
    events.add(
        new OLiveQueryResult(OLiveQueryResult.CREATE_EVENT, new OResultInternal(database), null));
    events.add(
        new OLiveQueryResult(
            OLiveQueryResult.UPDATE_EVENT, new OResultInternal(database),
            new OResultInternal(database)));
    events.add(
        new OLiveQueryResult(OLiveQueryResult.DELETE_EVENT, new OResultInternal(database), null));

    OLiveQueryPushRequest request =
        new OLiveQueryPushRequest(10, OLiveQueryPushRequest.END, events);
    request.execute(null, storage);
    assertEquals(1, mock.countCreate);
    assertEquals(1, mock.countUpdate);
    assertEquals(1, mock.countDelete);
  }
}
