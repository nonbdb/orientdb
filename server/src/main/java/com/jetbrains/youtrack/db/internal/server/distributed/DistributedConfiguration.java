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
package com.jetbrains.youtrack.db.internal.server.distributed;

import com.jetbrains.youtrack.db.api.exception.ConfigurationException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immutable Distributed configuration. It uses an EntityImpl object to store the configuration.
 * Every changes must be done by obtaining a modifiable verson of the object through the method
 * `modify()`.
 */
public class DistributedConfiguration {

  public static final String NEW_NODE_TAG = "<NEW_NODE>";
  public static final String ALL_WILDCARD = "*";

  protected static final String SERVERS = "servers";
  protected static final String DCS = "dataCenters";
  protected static final String OWNER = "owner";
  protected static final String CLUSTERS = "clusters";
  protected static final String VERSION = "version";

  protected static final String AUTO_DEPLOY = "autoDeploy";
  protected static final String READ_QUORUM = "readQuorum";
  protected static final String WRITE_QUORUM = "writeQuorum";
  public static final String QUORUM_MAJORITY = "majority";
  public static final String QUORUM_ALL = "all";
  public static final String QUORUM_LOCAL_DC = "localDataCenter";
  public static final Integer DEFAULT_READ_QUORUM = 1;
  public static final String DEFAULT_WRITE_QUORUM = QUORUM_MAJORITY;

  protected static final String NEW_NODE_STRATEGY = "newNodeStrategy";
  protected static final String READ_YOUR_WRITES = "readYourWrites";
  protected static final String EXECUTION_MODE = "executionMode";
  protected static final String EXECUTION_MODE_SYNCHRONOUS = "synchronous";

  protected final EntityImpl configuration;
  protected static final List<String> DEFAULT_CLUSTER_NAME =
      Collections.singletonList(ALL_WILDCARD);
  private static final ThreadLocal<Integer> overwriteWriteQuorum = new ThreadLocal<Integer>();

  public enum ROLES {
    MASTER,
    REPLICA
  }

  public enum NEW_NODE_STRATEGIES {
    DYNAMIC,
    STATIC
  }

  public DistributedConfiguration(final EntityImpl iConfiguration) {
    configuration = iConfiguration;
    configuration.setTrackingChanges(false);
  }

  public ModifiableDistributedConfiguration modify() {
    return new ModifiableDistributedConfiguration(configuration.copy());
  }

  /**
   * Returns true if the replication is active, otherwise false.
   *
   * @param iClusterName Cluster name, or null for *
   */
  public boolean isReplicationActive(final String iClusterName, final String iLocalNode) {
    final Collection<String> servers = getClusterConfiguration(iClusterName).field(SERVERS);
    return servers != null && !servers.isEmpty();
  }

  /**
   * Returns true if the configuration per data centers is specified.
   */
  public boolean hasDataCenterConfiguration() {
    return configuration.field(DCS) != null;
  }

  public boolean isAutoDeploy() {
    return configuration.field(AUTO_DEPLOY) != null
        ? (Boolean) configuration.field(AUTO_DEPLOY)
        : true;
  }

  /**
   * Returns the new node strategy between "dynamic" and "static". If static, the node is registered
   * under the "server" tag.
   *
   * @return NEW_NODE_STRATEGIES enum
   */
  public NEW_NODE_STRATEGIES getNewNodeStrategy() {

    final String value = configuration.field(NEW_NODE_STRATEGY);
    if (value != null) {
      return NEW_NODE_STRATEGIES.valueOf(value.toUpperCase(Locale.ENGLISH));
    }

    return NEW_NODE_STRATEGIES.STATIC;
  }

  /**
   * Returns the execution mode if synchronous.
   *
   * @param iClusterName Cluster name, or null for *
   * @return true = synchronous, false = asynchronous, null = undefined
   */
  public Boolean isExecutionModeSynchronous(final String iClusterName) {
    Object value = getClusterConfiguration(iClusterName).field(EXECUTION_MODE);
    if (value == null) {
      value = configuration.field(EXECUTION_MODE);
      if (value == null) {
        return null;
      }
    }

    if (value.toString().equalsIgnoreCase("undefined")) {
      return null;
    }

    return value.toString().equalsIgnoreCase(EXECUTION_MODE_SYNCHRONOUS);
  }

  /**
   * Reads your writes.
   *
   * @param iClusterName Cluster name, or null for *
   */
  public Boolean isReadYourWrites(final String iClusterName) {
    Object value = getClusterConfiguration(iClusterName).field(READ_YOUR_WRITES);
    if (value == null) {
      value = configuration.field(READ_YOUR_WRITES);
      if (value == null) {
        LogManager.instance()
            .warn(
                this,
                "%s setting not found for cluster=%s in distributed-config.json",
                READ_YOUR_WRITES,
                iClusterName);
        return true;
      }
    }
    return (Boolean) value;
  }

  /**
   * Returns the list of servers that can manage a list of clusters. The algorithm makes its best to
   * involve the less servers as it can.
   *
   * @param iClusterNames Set of cluster names to find
   * @param iLocalNode    Local node name
   */
  public Map<String, Collection<String>> getServerClusterMap(
      Collection<String> iClusterNames,
      final String iLocalNode,
      final boolean optimizeForLocalOnly) {
    if (iClusterNames == null || iClusterNames.isEmpty()) {
      iClusterNames = DEFAULT_CLUSTER_NAME;
    }

    final Map<String, Collection<String>> servers =
        new HashMap<String, Collection<String>>(iClusterNames.size());

    // TRY TO SEE IF IT CAN BE EXECUTED ON LOCAL NODE ONLY
    boolean canUseLocalNode = true;
    for (String p : iClusterNames) {
      final List<String> serverList = getClusterConfiguration(p).field(SERVERS);
      if (serverList != null && !serverList.contains(iLocalNode)) {
        canUseLocalNode = false;
        break;
      }
    }

    if (optimizeForLocalOnly && canUseLocalNode) {
      // USE LOCAL NODE ONLY (MUCH FASTER)
      servers.put(iLocalNode, iClusterNames);
      return servers;
    }

    // GROUP BY SERVER WITH THE NUMBER OF CLUSTERS
    final Map<String, Collection<String>> serverMap = new HashMap<String, Collection<String>>();
    for (String p : iClusterNames) {
      final List<String> serverList = getClusterConfiguration(p).field(SERVERS);
      for (String s : serverList) {
        if (NEW_NODE_TAG.equalsIgnoreCase(s)) {
          continue;
        }

        Collection<String> clustersInServer = serverMap.get(s);
        if (clustersInServer == null) {
          clustersInServer = new HashSet<String>();
          serverMap.put(s, clustersInServer);
        }
        clustersInServer.add(p);
      }
    }

    if (serverMap.size() == 1)
    // RETURN THE ONLY SERVER INVOLVED
    {
      return serverMap;
    }

    if (!optimizeForLocalOnly) {
      return serverMap;
    }

    // ORDER BY NUMBER OF CLUSTERS
    final List<String> orderedServers = new ArrayList<String>(serverMap.keySet());
    Collections.sort(
        orderedServers,
        new Comparator<String>() {
          @Override
          public int compare(final String o1, final String o2) {
            return ((Integer) serverMap.get(o2).size()).compareTo(serverMap.get(o1).size());
          }
        });

    // BROWSER ORDERED SERVER MAP PUTTING THE MINIMUM SERVER TO COVER ALL THE CLUSTERS
    final Set<String> remainingClusters =
        new HashSet<String>(iClusterNames); // KEEPS THE REMAINING CLUSTER TO ADD IN FINAL
    // RESULT
    final Set<String> includedClusters =
        new HashSet<String>(iClusterNames.size()); // KEEPS THE COLLECTION OF ALREADY INCLUDED
    // CLUSTERS
    for (String s : orderedServers) {
      final Collection<String> clusters = serverMap.get(s);

      if (!servers.isEmpty()) {
        // FILTER CLUSTER LIST AVOIDING TO REPEAT CLUSTERS ALREADY INCLUDED ON PREVIOUS NODES
        clusters.removeAll(includedClusters);
      }

      servers.put(s, clusters);
      remainingClusters.removeAll(clusters);
      includedClusters.addAll(clusters);

      if (remainingClusters.isEmpty())
      // FOUND ALL CLUSTERS
      {
        break;
      }
    }

    return servers;
  }

  /**
   * Returns the clusters where a server is owner. This is used when a cluster must be selected:
   * locality is always the best choice.
   *
   * @param iClusterNames Set of cluster names
   * @param iNode         Node
   */
  public List<String> getOwnedClustersByServer(
      Collection<String> iClusterNames, final String iNode) {
    if (iClusterNames == null || iClusterNames.isEmpty()) {
      iClusterNames = DEFAULT_CLUSTER_NAME;
    }

    final List<String> notDefinedClusters = new ArrayList<String>(5);
    final List<String> candidates = new ArrayList<String>(5);

    for (String p : iClusterNames) {
      if (p == null) {
        continue;
      }

      final String ownerServer = getClusterOwner(p);
      if (ownerServer == null) {
        notDefinedClusters.add(p);
      } else if (iNode.equals(ownerServer)) {
        // COLLECT AS CANDIDATE
        candidates.add(p);
      }
    }

    if (!candidates.isEmpty())
    // RETURN THE FIRST ONE
    {
      return candidates;
    }

    final String owner = getClusterOwner(ALL_WILDCARD);
    if (iNode.equals(owner))
    // CURRENT SERVER IS MASTER OF DEFAULT: RETURN ALL THE NON CONFIGURED CLUSTERS
    {
      return notDefinedClusters;
    }

    // NO MASTER FOUND, RETURN EMPTY LIST
    return candidates;
  }

  /**
   * Returns the set of server names involved on the passed cluster collection.
   *
   * @param iClusterNames Collection of cluster names to find
   */
  public Set<String> getServers(Collection<String> iClusterNames) {
    if (iClusterNames == null || iClusterNames.isEmpty()) {
      return getAllConfiguredServers();
    }

    final Set<String> partitions = new HashSet<String>(iClusterNames.size());
    for (String p : iClusterNames) {
      final List<String> serverList = getClusterConfiguration(p).field(SERVERS);
      if (serverList != null) {
        for (String s : serverList) {
          if (!s.equals(NEW_NODE_TAG)) {
            partitions.add(s);
          }
        }
      }
    }
    return partitions;
  }

  /**
   * Returns true if the local server has all the requested clusters.
   *
   * @param server   Server name
   * @param clusters Collection of cluster names to find
   */
  public boolean isServerContainingAllClusters(final String server, Collection<String> clusters) {

    if (clusters == null || clusters.isEmpty()) {
      clusters = DEFAULT_CLUSTER_NAME;
    }

    for (String cluster : clusters) {
      final List<String> serverList = getClusterConfiguration(cluster).field(SERVERS);
      if (serverList != null) {
        if (!serverList.contains(server)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns true if the local server has the requested cluster.
   *
   * @param server  Server name
   * @param cluster cluster names to find
   */
  public boolean isServerContainingCluster(final String server, String cluster) {
    if (cluster == null) {
      cluster = ALL_WILDCARD;
    }

    final List<String> serverList = getClusterConfiguration(cluster).field(SERVERS);
    if (serverList != null) {
      return serverList.contains(server);
    }
    return true;
  }

  /**
   * Returns the server list for the requested cluster cluster excluding any tags like <NEW_NODES>
   * and iExclude if any.
   *
   * @param iClusterName Cluster name, or null for *
   * @param iExclude     Node to exclude
   */
  public List<String> getServers(final String iClusterName, final String iExclude) {

    final List<String> serverList = getClusterConfiguration(iClusterName).field(SERVERS);
    if (serverList != null) {
      // COPY AND REMOVE ANY NEW_NODE_TAG
      List<String> filteredServerList = new ArrayList<String>(serverList.size());
      for (String s : serverList) {
        if (!s.equals(NEW_NODE_TAG) && (iExclude == null || !iExclude.equals(s))) {
          filteredServerList.add(s);
        }
      }
      return filteredServerList;
    }
    return Collections.EMPTY_LIST;
  }

  /**
   * Returns an ordered list of master server. The first in the list is the first found in
   * configuration. This is used to determine the cluster leader.
   */
  public List<String> getMasterServers() {
    final List<String> serverList = getClusterConfiguration(null).field(SERVERS);
    if (serverList != null) {
      // COPY AND REMOVE ANY NEW_NODE_TAG
      List<String> masters = new ArrayList<String>(serverList.size());
      for (String s : serverList) {
        if (!s.equals(NEW_NODE_TAG)) {
          masters.add(s);
        }
      }

      final ROLES defRole = getDefaultServerRole();

      final EntityImpl servers = configuration.field(SERVERS);
      if (servers != null) {
        for (Iterator<String> it = masters.iterator(); it.hasNext(); ) {
          final String server = it.next();
          final String roleAsString = servers.field(server);
          final ROLES role =
              roleAsString != null
                  ? ROLES.valueOf(roleAsString.toUpperCase(Locale.ENGLISH))
                  : defRole;
          if (role != ROLES.MASTER) {
            it.remove();
          }
        }
      }

      return masters;
    }

    return Collections.EMPTY_LIST;
  }

  /**
   * Returns the complete list of servers found in configuration.
   */
  public Set<String> getAllConfiguredServers() {

    final Set<String> servers = new HashSet<String>();

    for (String p : getClusterNames()) {
      final List<String> serverList = getClusterConfiguration(p).field(SERVERS);
      if (serverList != null) {
        for (String s : serverList) {
          if (!s.equals(NEW_NODE_TAG)) {
            servers.add(s);
          }
        }
      }
    }
    return servers;
  }

  /**
   * Returns the set of clusters managed by a server.
   *
   * @param iNodeName Server name
   */
  public Set<String> getClustersOnServer(final String iNodeName) {
    final Set<String> clusters = new HashSet<String>();
    for (String cl : getClusterNames()) {
      final List<String> servers = getServers(cl, null);
      if (servers.contains(iNodeName)) {
        clusters.add(cl);
      }
    }
    return clusters;
  }

  /**
   * Returns the set of clusters where server is the owner.
   *
   * @param iNodeName Server name
   */
  public Set<String> getClustersOwnedByServer(final String iNodeName) {
    final Set<String> clusters = new HashSet<String>();
    for (String cl : getClusterNames()) {
      if (iNodeName.equals(getClusterOwner(cl))) {
        clusters.add(cl);
      }
    }
    return clusters;
  }

  /**
   * Returns the owner server for the given cluster excluding the passed node. The Owner server is
   * the first in server list.
   *
   * @param iClusterName Cluster name, or null for *
   */
  public String getClusterOwner(final String iClusterName) {
    String owner;

    final EntityImpl clusters = getConfiguredClusters();

    // GET THE CLUSTER CFG
    final EntityImpl cfg =
        iClusterName != null ? (EntityImpl) clusters.field(iClusterName) : null;

    if (cfg != null) {
      owner = cfg.field(OWNER);
      if (owner != null) {
        return owner;
      }

      final List<String> serverList = cfg.field(SERVERS);
      if (serverList != null && !serverList.isEmpty()) {
        // RETURN THE FIRST ONE
        owner = serverList.get(0);
        if (NEW_NODE_TAG.equals(owner) && serverList.size() > 1)
        // DON'T RETURN <NEW_NODE>
        {
          owner = serverList.get(1);
        }
      }
    } else
    // RETURN THE OWNER OF *
    {
      return getClusterOwner(ALL_WILDCARD);
    }

    return owner;
  }

  /**
   * Returns the static owner server for the given cluster.
   *
   * @param iClusterName Cluster name, or null for *
   */
  public String getConfiguredClusterOwner(final String iClusterName) {

    String owner = null;

    final EntityImpl clusters = getConfiguredClusters();

    // GET THE CLUSTER CFG
    final EntityImpl cfg = clusters.field(iClusterName);
    if (cfg != null) {
      owner = cfg.field(OWNER);
    }

    return owner;
  }

  /**
   * Returns the configured server list for the requested cluster.
   *
   * @param iClusterName Cluster name, or null for *
   */
  public List<String> getConfiguredServers(final String iClusterName) {

    final Collection<? extends String> list = getClusterConfiguration(iClusterName).field(SERVERS);
    return list != null ? new ArrayList<String>(list) : null;
  }

  /**
   * Returns the array of configured clusters
   */
  public String[] getClusterNames() {
    final EntityImpl clusters = configuration.field(CLUSTERS);
    return clusters.fieldNames();
  }

  /**
   * Returns the default server role between MASTER (default) and REPLICA.
   */
  public ROLES getDefaultServerRole() {

    final EntityImpl servers = configuration.field(SERVERS);
    if (servers == null)
    // DEFAULT: MASTER
    {
      return ROLES.MASTER;
    }

    final String role = servers.field(ALL_WILDCARD);
    if (role == null)
    // DEFAULT: MASTER
    {
      return ROLES.MASTER;
    }

    return ROLES.valueOf(role.toUpperCase(Locale.ENGLISH));
  }

  /**
   * Returns the server role between MASTER (default) and REPLICA.
   */
  public ROLES getServerRole(final String iServerName) {

    final EntityImpl servers = configuration.field(SERVERS);
    if (servers == null)
    // DEFAULT: MASTER
    {
      return ROLES.MASTER;
    }

    String role = servers.field(iServerName);
    if (role == null) {
      // DEFAULT: MASTER
      role = servers.field(ALL_WILDCARD);
      if (role == null)
      // DEFAULT: MASTER
      {
        return ROLES.MASTER;
      }
    }

    return ROLES.valueOf(role.toUpperCase(Locale.ENGLISH));
  }

  /**
   * Returns the registered servers.
   */
  public Set<String> getRegisteredServers() {

    final EntityImpl servers = configuration.field(SERVERS);
    final Set<String> result = new HashSet<String>();
    if (servers != null) {
      Collections.addAll(result, servers.fieldNames());
    }
    return result;
  }

  public EntityImpl getDocument() {
    return configuration;
  }

  /**
   * Returns all the configured data centers' names, if any.
   */
  public Set<String> getDataCenters() {

    final EntityImpl dcs = configuration.field(DCS);
    if (dcs == null) {
      return Collections.EMPTY_SET;
    }

    final Set<String> result = new HashSet<String>();
    Collections.addAll(result, dcs.fieldNames());
    return result;
  }

  /**
   * Returns the data center write quorum.
   *
   * @param dataCenter Data center name
   */
  public int getDataCenterWriteQuorum(final String dataCenter) {

    final EntityImpl dc = getDataCenterConfiguration(dataCenter);

    Object wq = dc.field(WRITE_QUORUM);
    if (wq instanceof String) {
      if (wq.toString().equalsIgnoreCase(DistributedConfiguration.QUORUM_MAJORITY)) {
        final List<String> servers = dc.field(SERVERS);
        wq = servers.size() / 2 + 1;
      } else if (wq.toString().equalsIgnoreCase(DistributedConfiguration.QUORUM_ALL)) {
        final List<String> servers = dc.field(SERVERS);
        wq = servers.size();
      }
    }

    return (Integer) wq;
  }

  /**
   * Returns true if the database is sharded across servers. False if it's completely replicated.
   */
  public boolean isSharded() {

    final EntityImpl allCluster = getClusterConfiguration(ALL_WILDCARD);
    if (allCluster != null) {
      final List<String> allServers = allCluster.field(SERVERS);
      if (allServers != null && !allServers.isEmpty()) {
        for (String cl : getClusterNames()) {
          final List<String> servers = getServers(cl, null);
          if (servers != null && !servers.isEmpty() && !allServers.containsAll(servers)) {
            return false;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns the list of servers in a data center.
   *
   * @param dataCenter Data center name
   * @throws ConfigurationException if the list of servers is not found in data center
   *                                 configuration
   */
  public List<String> getDataCenterServers(final String dataCenter) {

    final EntityImpl dc = getDataCenterConfiguration(dataCenter);

    final List<String> servers = dc.field(SERVERS);
    if (servers == null || servers.isEmpty()) {
      throw new ConfigurationException(
          "Data center '"
              + dataCenter
              + "' does not contain any server in distributed database configuration");
    }

    return new ArrayList<String>(servers);
  }

  /**
   * Returns the data center where the server belongs.
   *
   * @param server Server name
   */
  public String getDataCenterOfServer(final String server) {

    final EntityImpl dcs = configuration.field(DCS);
    if (dcs != null) {
      for (String dc : dcs.fieldNames()) {
        final EntityImpl dcConfig = dcs.field(dc);
        if (dcConfig != null) {
          final List<String> dcServers = dcConfig.field("servers");
          if (dcServers != null && !dcServers.isEmpty()) {
            if (dcServers.contains(server))
            // FOUND
            {
              return dc;
            }
          }
        }
      }
    }

    // NOT FOUND
    return null;
  }

  public int getVersion() {
    final Integer v = configuration.field(VERSION);
    if (v == null) {
      return 1;
    }
    return v;
  }

  /**
   * Returns true if the global write quorum is "localDataCenter".
   */
  public boolean isLocalDataCenterWriteQuorum() {
    return QUORUM_LOCAL_DC.equals(configuration.field(WRITE_QUORUM));
  }

  /**
   * Returns the global read quorum.
   *
   * @param iClusterName Cluster name, or null for *
   */
  public Object getGlobalReadQuorum(final String iClusterName) {
    Object value = getClusterConfiguration(iClusterName).field(READ_QUORUM);
    if (value == null) {
      value = configuration.field(READ_QUORUM);
    }
    return value;
  }

  /**
   * Returns the read quorum.
   *
   * @param clusterName            Cluster name, or null for *
   * @param totalConfiguredServers Total node available
   */
  public int getReadQuorum(
      final String clusterName, final int totalConfiguredServers, final String server) {
    return getQuorum(
        "readQuorum", clusterName, totalConfiguredServers, DEFAULT_READ_QUORUM, server);
  }

  /**
   * Returns the write quorum.
   *
   * @param clusterName                  Cluster name, or null for *
   * @param totalConfiguredMasterServers Total node available
   */
  public int getWriteQuorum(
      final String clusterName, final int totalConfiguredMasterServers, final String server) {
    Integer overWrite = overwriteWriteQuorum.get();
    if (overWrite != null) {
      return overWrite.intValue();
    } else {
      return getQuorum(
          "writeQuorum", clusterName, totalConfiguredMasterServers, DEFAULT_WRITE_QUORUM, server);
    }
  }

  private EntityImpl getConfiguredClusters() {
    final EntityImpl clusters = configuration.field(CLUSTERS);
    if (clusters == null) {
      throw new ConfigurationException(
          "Cannot find '" + CLUSTERS + "' in distributed database configuration");
    }
    return clusters;
  }

  @Override
  public String toString() {
    return configuration.toString();
  }

  /**
   * Gets the document representing the cluster configuration.
   *
   * @param iClusterName Cluster name, or null for *
   * @return Always a EntityImpl
   * @throws ConfigurationException in case "clusters" field is not found in configuration
   */
  protected EntityImpl getClusterConfiguration(String iClusterName) {
    final EntityImpl clusters = getConfiguredClusters();

    if (iClusterName == null) {
      iClusterName = ALL_WILDCARD;
    }

    final EntityImpl cfg;
    if (!clusters.containsField(iClusterName))
    // NO CLUSTER IN CFG: GET THE DEFAULT ONE
    {
      cfg = clusters.field(ALL_WILDCARD);
    } else
    // GET THE CLUSTER CFG
    {
      cfg = clusters.field(iClusterName);
    }

    if (cfg == null) {
      return new EntityImpl();
    }

    return cfg;
  }

  /**
   * Gets the document representing the dc configuration.
   *
   * @param dataCenter Data center name
   * @return Always a EntityImpl
   * @throws ConfigurationException if the data center configuration is not found
   */
  private EntityImpl getDataCenterConfiguration(final String dataCenter) {
    final EntityImpl dcs = configuration.field(DCS);
    if (dcs != null) {
      return dcs.field(dataCenter);
    }
    throw new ConfigurationException(
        "Cannot find the data center '" + dataCenter + "' in distributed database configuration");
  }

  public void forceWriteQuorum(int quorum) {
    overwriteWriteQuorum.set(quorum);
  }

  public void clearForceWriteQuorum() {
    overwriteWriteQuorum.remove();
  }

  /**
   * Returns the read quorum.
   *
   * @param iClusterName Cluster name, or null for *
   * @param totalServers Total nodes available
   */
  private int getQuorum(
      final String quorumSetting,
      final String iClusterName,
      final int totalServers,
      final Object defaultValue,
      final String server) {
    Object value = getClusterConfiguration(iClusterName).field(quorumSetting);
    if (value == null) {
      value = configuration.field(quorumSetting);
      if (value == null) {
        LogManager.instance()
            .warn(
                this,
                "%s setting not found for cluster=%s in distributed-config.json",
                quorumSetting,
                iClusterName);
        value = defaultValue;
      }
    }

    if (value instanceof String) {
      if (value.toString().equalsIgnoreCase(QUORUM_MAJORITY)) {
        value = totalServers / 2 + 1;
      } else if (value.toString().equalsIgnoreCase(QUORUM_ALL)) {
        value = totalServers;
      } else if (value.toString().equalsIgnoreCase(QUORUM_LOCAL_DC)) {
        final String dc = getDataCenterOfServer(server);
        if (dc == null) {
          throw new ConfigurationException(
              "Data center not specified for server '" + server + "' in distributed configuration");
        }
        value = getDataCenterWriteQuorum(dc);
      } else {
        throw new ConfigurationException(
            "The value '"
                + value
                + "' is not supported for "
                + quorumSetting
                + " in distributed configuration");
      }
    }

    return (Integer) value;
  }
}
