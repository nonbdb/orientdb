/*
 *
 *  *  Copyright YouTrackDB
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

package com.jetbrains.youtrack.db.internal.core.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.common.io.FileUtils;
import com.jetbrains.youtrack.db.internal.server.ServerMain;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class JournaledTxStreamingTest {

  private static final int ITERATIONS = 1000;

  private File buildDir;
  private Process serverProcess;
  private YouTrackDB ctx;
  private DatabaseSessionInternal db;
  private DataInputStream stream;

  @Before
  public void before() throws Exception {
    var buildDirectory = System.getProperty("buildDirectory", ".");
    buildDirectory += "/" + JournaledTxStreamingTest.class.getSimpleName();

    buildDir = new File(buildDirectory);

    buildDirectory = buildDir.getCanonicalPath();
    buildDir = new File(buildDirectory);

    if (buildDir.exists()) {
      FileUtils.deleteRecursively(buildDir);
    }

    assertThat(buildDir.mkdir()).isTrue();

    spawnServer();

    ctx = new YouTrackDBImpl("remote:localhost:3500", "root", "root",
        YouTrackDBConfig.defaultConfig());
    ctx.execute("create database " + JournaledTxStreamingTest.class.getSimpleName() + " plocal ")
        .close();

    db =
        (DatabaseSessionInternal)
            ctx.open(JournaledTxStreamingTest.class.getSimpleName(), "root", "root");

    final var socket = new Socket();
    socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 3600));
    socket.setSoTimeout(2000);
    stream = new DataInputStream(socket.getInputStream());
  }

  @After
  public void after() throws InterruptedException, IOException {
    db.close();

    System.out.println("Wait for process to destroy");
    serverProcess.destroy();

    serverProcess.waitFor();
    System.out.println("Process was destroyed");

    FileUtils.deleteRecursively(buildDir);
    Assert.assertFalse(buildDir.exists());
  }

  public static final class RemoteDBRunner {

    public static void main(String[] args) throws Exception {
      var server = ServerMain.create(false);
      server.startup(
          RemoteDBRunner.class.getResourceAsStream(
              "/com/jetbrains/youtrack/db/internal/core/db/journaled-tx-streaming-test-server-config.xml"
          )
      );
      server.activate();

      final var mutexFile = System.getProperty("mutexFile");
      final var mutex = new RandomAccessFile(mutexFile, "rw");
      mutex.seek(0);
      mutex.write(1);
      mutex.close();
    }
  }

  private void spawnServer() throws Exception {
    final var mutexFile = new File(buildDir, "mutex.ct");
    final var mutex = new RandomAccessFile(mutexFile, "rw");
    mutex.seek(0);
    mutex.write(0);

    var javaExec = System.getProperty("java.home") + "/bin/java";
    javaExec = new File(javaExec).getCanonicalPath();

    System.setProperty("YOUTRACKDB_HOME", buildDir.getCanonicalPath());

    var processBuilder =
        new ProcessBuilder(
            javaExec,
            "-classpath",
            System.getProperty("java.class.path"),
            "-DYOUTRACKDB_HOME=" + buildDir.getCanonicalPath(),
            "-DmutexFile=" + mutexFile.getCanonicalPath(),
            "-Dstorage.internal.journaled.tx.streaming.port=3600",
            RemoteDBRunner.class.getName());
    processBuilder.inheritIO();

    serverProcess = processBuilder.start();

    System.out.println(JournaledTxStreamingTest.class.getSimpleName() + ": Wait for server start");
    boolean started;
    do {
      Thread.sleep(1000);
      mutex.seek(0);
      started = mutex.read() == 1;
    } while (!started);

    mutex.close();
    assertThat(mutexFile.delete()).isTrue();
    System.out.println(JournaledTxStreamingTest.class.getSimpleName() + ": Server was started");
  }
}
