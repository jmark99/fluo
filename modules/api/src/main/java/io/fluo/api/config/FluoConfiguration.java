/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.fluo.api.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.fluo.api.client.FluoClient;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SubsetConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration helper class for Fluo. FluoConfiguration extends {@link CompositeConfiguration}.
 */
public class FluoConfiguration extends CompositeConfiguration {

  private static final Logger log = LoggerFactory.getLogger(FluoConfiguration.class);

  public static final String FLUO_PREFIX = "io.fluo";

  // Client properties
  private static final String CLIENT_PREFIX = FLUO_PREFIX + ".client";
  public static final String CLIENT_APPLICATION_NAME_PROP = CLIENT_PREFIX + ".application.name";
  public static final String CLIENT_ACCUMULO_PASSWORD_PROP = CLIENT_PREFIX + ".accumulo.password";
  public static final String CLIENT_ACCUMULO_USER_PROP = CLIENT_PREFIX + ".accumulo.user";
  public static final String CLIENT_ACCUMULO_INSTANCE_PROP = CLIENT_PREFIX + ".accumulo.instance";
  public static final String CLIENT_ACCUMULO_ZOOKEEPERS_PROP = CLIENT_PREFIX
      + ".accumulo.zookeepers";
  public static final String CLIENT_ZOOKEEPER_TIMEOUT_PROP = CLIENT_PREFIX + ".zookeeper.timeout";
  public static final String CLIENT_ZOOKEEPER_CONNECT_PROP = CLIENT_PREFIX + ".zookeeper.connect";
  public static final String CLIENT_RETRY_TIMEOUT_MS_PROP = CLIENT_PREFIX + ".retry.timeout.ms";
  public static final String CLIENT_CLASS_PROP = CLIENT_PREFIX + ".class";
  public static final int CLIENT_ZOOKEEPER_TIMEOUT_DEFAULT = 30000;
  public static final String CLIENT_ACCUMULO_ZOOKEEPERS_DEFAULT = "localhost";
  public static final String CLIENT_ZOOKEEPER_CONNECT_DEFAULT = "localhost/fluo";
  public static final int CLIENT_RETRY_TIMEOUT_MS_DEFAULT = -1;
  public static final String CLIENT_CLASS_DEFAULT = FLUO_PREFIX + ".core.client.FluoClientImpl";

  // Administration
  private static final String ADMIN_PREFIX = FLUO_PREFIX + ".admin";
  public static final String ADMIN_ACCUMULO_TABLE_PROP = ADMIN_PREFIX + ".accumulo.table";
  public static final String ADMIN_ACCUMULO_CLASSPATH_PROP = ADMIN_PREFIX + ".accumulo.classpath";
  public static final String ADMIN_ACCUMULO_CLASSPATH_DEFAULT = "";
  public static final String ADMIN_CLASS_PROP = ADMIN_PREFIX + ".class";
  public static final String ADMIN_CLASS_DEFAULT = FLUO_PREFIX + ".core.client.FluoAdminImpl";

  // Worker
  private static final String WORKER_PREFIX = FLUO_PREFIX + ".worker";
  public static final String WORKER_NUM_THREADS_PROP = WORKER_PREFIX + ".num.threads";
  public static final String WORKER_INSTANCES_PROP = WORKER_PREFIX + ".instances";
  public static final String WORKER_MAX_MEMORY_MB_PROP = WORKER_PREFIX + ".max.memory.mb";
  public static final String WORKER_NUM_CORES_PROP = WORKER_PREFIX + ".num.cores";
  public static final int WORKER_NUM_THREADS_DEFAULT = 10;
  public static final int WORKER_INSTANCES_DEFAULT = 1;
  public static final int WORKER_MAX_MEMORY_MB_DEFAULT = 1024;
  public static final int WORKER_NUM_CORES_DEFAULT = 1;

  // Loader
  private static final String LOADER_PREFIX = FLUO_PREFIX + ".loader";
  public static final String LOADER_NUM_THREADS_PROP = LOADER_PREFIX + ".num.threads";
  public static final String LOADER_QUEUE_SIZE_PROP = LOADER_PREFIX + ".queue.size";
  public static final int LOADER_NUM_THREADS_DEFAULT = 10;
  public static final int LOADER_QUEUE_SIZE_DEFAULT = 10;

  // Oracle
  private static final String ORACLE_PREFIX = FLUO_PREFIX + ".oracle";
  public static final String ORACLE_INSTANCES_PROP = ORACLE_PREFIX + ".instances";
  public static final String ORACLE_MAX_MEMORY_MB_PROP = ORACLE_PREFIX + ".max.memory.mb";
  public static final String ORACLE_NUM_CORES_PROP = ORACLE_PREFIX + ".num.cores";
  public static final int ORACLE_INSTANCES_DEFAULT = 1;
  public static final int ORACLE_MAX_MEMORY_MB_DEFAULT = 512;
  public static final int ORACLE_NUM_CORES_DEFAULT = 1;

  // MiniFluo
  private static final String MINI_PREFIX = FLUO_PREFIX + ".mini";
  public static final String MINI_CLASS_PROP = MINI_PREFIX + ".class";
  public static final String MINI_START_ACCUMULO_PROP = MINI_PREFIX + ".start.accumulo";
  public static final String MINI_DATA_DIR_PROP = MINI_PREFIX + ".data.dir";
  public static final String MINI_CLASS_DEFAULT = FLUO_PREFIX + ".mini.MiniFluoImpl";
  public static final boolean MINI_START_ACCUMULO_DEFAULT = true;
  public static final String MINI_DATA_DIR_DEFAULT = "${env:FLUO_HOME}/mini";

  /** The properties below get loaded into/from Zookeeper */
  // Observer
  public static final String OBSERVER_PREFIX = FLUO_PREFIX + ".observer.";

  // Transaction
  public static final String TRANSACTION_PREFIX = FLUO_PREFIX + ".tx";
  public static final String TRANSACTION_ROLLBACK_TIME_PROP = TRANSACTION_PREFIX + ".rollback.time";
  public static final long TRANSACTION_ROLLBACK_TIME_DEFAULT = 300000;

  // Metrics
  public static final String METRICS_YAML_BASE64 = FLUO_PREFIX + ".metrics.yaml.base64";
  public static final String METRICS_YAML_BASE64_DEFAULT = DatatypeConverter.printBase64Binary(
      "---\nfrequency: 60 seconds\n".getBytes(Charsets.UTF_8)).replace("\n", "");

  // application config
  public static final String APP_PREFIX = FLUO_PREFIX + ".app";

  public FluoConfiguration() {
    super();
    setThrowExceptionOnMissing(true);
    setDelimiterParsingDisabled(true);
  }

  public FluoConfiguration(FluoConfiguration other) {
    this();
    Iterator<String> iter = other.getKeys();
    while (iter.hasNext()) {
      String key = iter.next();
      setProperty(key, other.getProperty(key));
    }
  }

  public FluoConfiguration(Configuration configuration) {
    this();
    if (configuration instanceof AbstractConfiguration) {
      AbstractConfiguration aconf = (AbstractConfiguration) configuration;
      aconf.setDelimiterParsingDisabled(true);
    }

    addConfiguration(configuration);
  }

  public FluoConfiguration(File propertiesFile) {
    this();
    try {
      PropertiesConfiguration config = new PropertiesConfiguration();
      // disabled to prevent accumulo classpath value from being shortened
      config.setDelimiterParsingDisabled(true);
      config.load(propertiesFile);
      addConfiguration(config);
    } catch (ConfigurationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public void validate() {
    // keep in alphabetical order
    getAccumuloClasspath();
    getAccumuloInstance();
    getAccumuloPassword();
    getAccumuloTable();
    getAccumuloUser();
    getAccumuloZookeepers();
    getAdminClass();
    getApplicationName();
    getAppZookeepers();
    getClientClass();
    getClientRetryTimeout();
    getLoaderQueueSize();
    getLoaderThreads();
    getMetricsYaml();
    getMetricsYamlBase64();
    getObserverConfig();
    getOracleInstances();
    getOracleMaxMemory();
    getOracleNumCores();
    getTransactionRollbackTime();
    getWorkerInstances();
    getWorkerMaxMemory();
    getWorkerNumCores();
    getWorkerThreads();
    getZookeeperTimeout();
  }

  public FluoConfiguration setApplicationName(String applicationName) {
    verifyApplicationName(applicationName);
    setProperty(CLIENT_APPLICATION_NAME_PROP, applicationName);
    return this;
  }

  public String getApplicationName() {
    String applicationName = getString(CLIENT_APPLICATION_NAME_PROP);
    verifyApplicationName(applicationName);
    return applicationName;
  }

  /**
   * Verifies application name. Avoids characters that Zookeeper does not like in nodes & Hadoop
   * does not like in HDFS paths.
   * 
   * @param name Application name
   */
  private void verifyApplicationName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Application name cannot be null");
    }
    if (name.length() == 0) {
      throw new IllegalArgumentException("Application name length must be > 0");
    }
    String reason = null;
    char[] chars = name.toCharArray();
    char c;
    for (int i = 0; i < chars.length; i++) {
      c = chars[i];
      if (c == 0) {
        reason = "null character not allowed @" + i;
        break;
      } else if (c == '/' || c == '.' || c == ':') {
        reason = "invalid character '" + c + "'";
        break;
      } else if (c > '\u0000' && c <= '\u001f' || c >= '\u007f' && c <= '\u009F' || c >= '\ud800'
          && c <= '\uf8ff' || c >= '\ufff0' && c <= '\uffff') {
        reason = "invalid charater @" + i;
        break;
      }
    }
    if (reason != null) {
      throw new IllegalArgumentException("Invalid application name \"" + name + "\" caused by "
          + reason);
    }
  }

  public FluoConfiguration setInstanceZookeepers(String zookeepers) {
    return setNonEmptyString(CLIENT_ZOOKEEPER_CONNECT_PROP, zookeepers);
  }

  public String getInstanceZookeepers() {
    return getNonEmptyString(CLIENT_ZOOKEEPER_CONNECT_PROP, CLIENT_ZOOKEEPER_CONNECT_DEFAULT);
  }

  public String getAppZookeepers() {
    return getInstanceZookeepers() + "/" + getApplicationName();
  }

  public FluoConfiguration setZookeeperTimeout(int timeout) {
    return setPositiveInt(CLIENT_ZOOKEEPER_TIMEOUT_PROP, timeout);
  }

  public int getZookeeperTimeout() {
    return getPositiveInt(CLIENT_ZOOKEEPER_TIMEOUT_PROP, CLIENT_ZOOKEEPER_TIMEOUT_DEFAULT);
  }

  public FluoConfiguration setClientRetryTimeout(int timeoutMS) {
    Preconditions.checkArgument(timeoutMS >= -1, CLIENT_RETRY_TIMEOUT_MS_PROP + " must be >= -1");
    setProperty(CLIENT_RETRY_TIMEOUT_MS_PROP, timeoutMS);
    return this;
  }

  public int getClientRetryTimeout() {
    int retval = getInt(CLIENT_RETRY_TIMEOUT_MS_PROP, CLIENT_RETRY_TIMEOUT_MS_DEFAULT);
    Preconditions.checkArgument(retval >= -1, CLIENT_RETRY_TIMEOUT_MS_PROP + " must be >= -1");
    return retval;
  }

  public FluoConfiguration setAccumuloInstance(String accumuloInstance) {
    return setNonEmptyString(CLIENT_ACCUMULO_INSTANCE_PROP, accumuloInstance);
  }

  public String getAccumuloInstance() {
    return getNonEmptyString(CLIENT_ACCUMULO_INSTANCE_PROP);
  }

  public FluoConfiguration setAccumuloUser(String accumuloUser) {
    return setNonEmptyString(CLIENT_ACCUMULO_USER_PROP, accumuloUser);
  }

  public String getAccumuloUser() {
    return getNonEmptyString(CLIENT_ACCUMULO_USER_PROP);
  }

  public FluoConfiguration setAccumuloPassword(String accumuloPassword) {
    setProperty(CLIENT_ACCUMULO_PASSWORD_PROP,
        verifyNotNull(CLIENT_ACCUMULO_PASSWORD_PROP, accumuloPassword));
    return this;
  }

  public String getAccumuloPassword() {
    return verifyNotNull(CLIENT_ACCUMULO_PASSWORD_PROP, getString(CLIENT_ACCUMULO_PASSWORD_PROP));
  }

  public FluoConfiguration setAccumuloZookeepers(String zookeepers) {
    return setNonEmptyString(CLIENT_ACCUMULO_ZOOKEEPERS_PROP, zookeepers);
  }

  public String getAccumuloZookeepers() {
    return getNonEmptyString(CLIENT_ACCUMULO_ZOOKEEPERS_PROP, CLIENT_ACCUMULO_ZOOKEEPERS_DEFAULT);
  }

  public FluoConfiguration setClientClass(String clientClass) {
    return setNonEmptyString(CLIENT_CLASS_PROP, clientClass);
  }

  public String getClientClass() {
    return getNonEmptyString(CLIENT_CLASS_PROP, CLIENT_CLASS_DEFAULT);
  }

  /**
   * Sets Accumulo table. This property only needs to be set for FluoAdmin as it will be stored in
   * retrieved from Zookeeper for clients.
   */
  public FluoConfiguration setAccumuloTable(String table) {
    return setNonEmptyString(ADMIN_ACCUMULO_TABLE_PROP, table);
  }

  public String getAccumuloTable() {
    return getNonEmptyString(ADMIN_ACCUMULO_TABLE_PROP);
  }

  public FluoConfiguration setAccumuloClasspath(String path) {
    setProperty(ADMIN_ACCUMULO_CLASSPATH_PROP, verifyNotNull(ADMIN_ACCUMULO_CLASSPATH_PROP, path));
    return this;
  }

  public String getAccumuloClasspath() {
    return getString(ADMIN_ACCUMULO_CLASSPATH_PROP, ADMIN_ACCUMULO_CLASSPATH_DEFAULT);
  }

  public FluoConfiguration setAdminClass(String adminClass) {
    return setNonEmptyString(ADMIN_CLASS_PROP, adminClass);
  }

  public String getAdminClass() {
    return getNonEmptyString(ADMIN_CLASS_PROP, ADMIN_CLASS_DEFAULT);
  }

  public FluoConfiguration setWorkerThreads(int numThreads) {
    return setPositiveInt(WORKER_NUM_THREADS_PROP, numThreads);
  }

  public int getWorkerThreads() {
    return getPositiveInt(WORKER_NUM_THREADS_PROP, WORKER_NUM_THREADS_DEFAULT);
  }

  public List<ObserverConfiguration> getObserverConfig() {

    List<ObserverConfiguration> configList = new ArrayList<>();
    Iterator<String> iter = getKeys();

    while (iter.hasNext()) {
      String key = iter.next();
      if (key.startsWith(FluoConfiguration.OBSERVER_PREFIX)) {
        String value = getString(key).trim();

        if (value.isEmpty()) {
          throw new IllegalArgumentException(key + " is set to empty value");
        }

        String[] fields = value.split(",");
        if (fields.length == 0) {
          throw new IllegalArgumentException(key + " has bad value: " + value);
        }

        String className = fields[0];
        if (className.isEmpty()) {
          throw new IllegalArgumentException(key + " has empty class name: " + className);
        }
        ObserverConfiguration observerConfig = new ObserverConfiguration(className);

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < fields.length; i++) {
          String[] kv = fields[i].split("=");
          if (kv.length != 2) {
            throw new IllegalArgumentException(key
                + " has invalid param. Expected 'key=value' but encountered '" + fields[i] + "'");
          }
          if (kv[0].isEmpty() || kv[1].isEmpty()) {
            throw new IllegalArgumentException(key + " has empty key or value in param: "
                + fields[i]);
          }
          params.put(kv[0], kv[1]);
        }
        observerConfig.setParameters(params);
        configList.add(observerConfig);
      }
    }
    return configList;
  }

  /**
   * Sets the {@link ObserverConfiguration} for observers
   */
  public FluoConfiguration setObservers(List<ObserverConfiguration> observers) {

    Iterator<String> iter1 = getKeys(OBSERVER_PREFIX);
    while (iter1.hasNext()) {
      String key = iter1.next();
      if (key.substring(OBSERVER_PREFIX.length()).matches("\\d+")) {
        clearProperty(key);
      }
    }

    int count = 0;
    for (ObserverConfiguration oconf : observers) {
      Map<String, String> params = oconf.getParameters();
      StringBuilder paramString = new StringBuilder();
      for (java.util.Map.Entry<String, String> pentry : params.entrySet()) {
        paramString.append(',');
        paramString.append(pentry.getKey());
        paramString.append('=');
        paramString.append(pentry.getValue());
      }
      setProperty(OBSERVER_PREFIX + "" + count, oconf.getClassName() + paramString);
      count++;
    }
    return this;
  }

  public FluoConfiguration setTransactionRollbackTime(long time, TimeUnit tu) {
    return setPositiveLong(TRANSACTION_ROLLBACK_TIME_PROP, tu.toMillis(time));
  }

  public long getTransactionRollbackTime() {
    return getPositiveLong(TRANSACTION_ROLLBACK_TIME_PROP, TRANSACTION_ROLLBACK_TIME_DEFAULT);
  }

  public FluoConfiguration setWorkerInstances(int workerInstances) {
    return setPositiveInt(WORKER_INSTANCES_PROP, workerInstances);
  }

  public int getWorkerInstances() {
    return getPositiveInt(WORKER_INSTANCES_PROP, WORKER_INSTANCES_DEFAULT);
  }

  public FluoConfiguration setWorkerMaxMemory(int maxMemoryMB) {
    return setPositiveInt(WORKER_MAX_MEMORY_MB_PROP, maxMemoryMB);
  }

  public int getWorkerMaxMemory() {
    return getPositiveInt(WORKER_MAX_MEMORY_MB_PROP, WORKER_MAX_MEMORY_MB_DEFAULT);
  }

  public FluoConfiguration setWorkerNumCores(int numCores) {
    return setPositiveInt(WORKER_NUM_CORES_PROP, numCores);
  }

  public int getWorkerNumCores() {
    return getPositiveInt(WORKER_NUM_CORES_PROP, WORKER_NUM_CORES_DEFAULT);
  }

  public FluoConfiguration setLoaderThreads(int numThreads) {
    return setNonNegativeInt(LOADER_NUM_THREADS_PROP, numThreads);
  }

  public int getLoaderThreads() {
    return getNonNegativeInt(LOADER_NUM_THREADS_PROP, LOADER_NUM_THREADS_DEFAULT);
  }

  public FluoConfiguration setLoaderQueueSize(int queueSize) {
    return setNonNegativeInt(LOADER_QUEUE_SIZE_PROP, queueSize);
  }

  public int getLoaderQueueSize() {
    return getNonNegativeInt(LOADER_QUEUE_SIZE_PROP, LOADER_QUEUE_SIZE_DEFAULT);
  }

  public FluoConfiguration setOracleMaxMemory(int oracleMaxMemory) {
    return setPositiveInt(ORACLE_MAX_MEMORY_MB_PROP, oracleMaxMemory);
  }

  public int getOracleMaxMemory() {
    return getPositiveInt(ORACLE_MAX_MEMORY_MB_PROP, ORACLE_MAX_MEMORY_MB_DEFAULT);
  }

  public FluoConfiguration setOracleInstances(int oracleInstances) {
    return setPositiveInt(ORACLE_INSTANCES_PROP, oracleInstances);
  }

  public int getOracleInstances() {
    return getPositiveInt(ORACLE_INSTANCES_PROP, ORACLE_INSTANCES_DEFAULT);
  }

  public FluoConfiguration setOracleNumCores(int numCores) {
    return setPositiveInt(ORACLE_NUM_CORES_PROP, numCores);
  }

  public int getOracleNumCores() {
    return getPositiveInt(ORACLE_NUM_CORES_PROP, ORACLE_NUM_CORES_DEFAULT);
  }

  public FluoConfiguration setMiniClass(String miniClass) {
    return setNonEmptyString(MINI_CLASS_PROP, miniClass);
  }

  public String getMiniClass() {
    return getNonEmptyString(MINI_CLASS_PROP, MINI_CLASS_DEFAULT);
  }

  /**
   * @return A {@link SubsetConfiguration} using the prefix {@value #APP_PREFIX}. Any change made to
   *         subset will be reflected in this configuration, but with the prefix added. This method
   *         is useful for setting application configuration before initialization. For reading
   *         application configration after initialization, see
   *         {@link FluoClient#getAppConfiguration()}
   */
  public Configuration getAppConfiguration() {
    return subset(APP_PREFIX);
  }

  /**
   * Base64 encodes yaml and stores it in metrics yaml base64 property
   * 
   * @param in yaml input
   */
  public void setMetricsYaml(InputStream in) {
    byte[] data = new byte[4096];
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len;
    try {
      while ((len = in.read(data)) > 0) {
        baos.write(data, 0, len);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    setProperty(METRICS_YAML_BASE64, DatatypeConverter.printBase64Binary(baos.toByteArray())
        .replace("\n", ""));
  }

  /**
   * This property must be base64 encoded. If you have raw yaml, then consider using
   * {@link #setMetricsYaml(InputStream)} which will do the base64 encoding for you.
   * 
   * @param base64Yaml A base64 encoded yaml metrics config.
   */
  public FluoConfiguration setMetricsYamlBase64(String base64Yaml) {
    return setNonEmptyString(METRICS_YAML_BASE64, base64Yaml);
  }

  /**
   * Consider using {@link #getMetricsYaml()} wich will automatically decode the base 64 value of
   * this property.
   * 
   * @return base64 encoded yaml metrics config.
   */

  public String getMetricsYamlBase64() {
    return getNonEmptyString(METRICS_YAML_BASE64, METRICS_YAML_BASE64_DEFAULT);
  }

  /**
   * This method will decode the base64 yaml metrics config.
   * 
   * @return stream that can be used to read yaml
   */
  public InputStream getMetricsYaml() {
    return new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(getMetricsYamlBase64()));
  }

  public FluoConfiguration setMiniStartAccumulo(boolean startAccumulo) {
    setProperty(MINI_START_ACCUMULO_PROP, startAccumulo);
    return this;
  }

  public boolean getMiniStartAccumulo() {
    return getBoolean(MINI_START_ACCUMULO_PROP, MINI_START_ACCUMULO_DEFAULT);
  }

  public FluoConfiguration setMiniDataDir(String dataDir) {
    return setNonEmptyString(MINI_DATA_DIR_PROP, dataDir);
  }

  public String getMiniDataDir() {
    return getNonEmptyString(MINI_DATA_DIR_PROP, MINI_DATA_DIR_DEFAULT);
  }

  protected void setDefault(String key, String val) {
    if (getProperty(key) == null) {
      setProperty(key, val);
    }
  }

  /**
   * Logs all properties
   */
  public void print() {
    Iterator<String> iter = getKeys();
    while (iter.hasNext()) {
      String key = iter.next();
      log.info(key + " = " + getProperty(key));
    }
  }

  private boolean verifyStringPropSet(String key) {
    if (containsKey(key) && !getString(key).isEmpty()) {
      return true;
    }
    log.info(key + " is not set");
    return false;
  }

  private boolean verifyStringPropNotSet(String key) {
    if (containsKey(key) && !getString(key).isEmpty()) {
      log.info(key + " should not be set");
      return false;
    }
    return true;
  }

  /**
   * Returns true if required properties for FluoClient are set
   */
  public boolean hasRequiredClientProps() {
    boolean valid = true;
    valid &= verifyStringPropSet(CLIENT_APPLICATION_NAME_PROP);
    valid &= verifyStringPropSet(CLIENT_ACCUMULO_USER_PROP);
    valid &= verifyStringPropSet(CLIENT_ACCUMULO_PASSWORD_PROP);
    valid &= verifyStringPropSet(CLIENT_ACCUMULO_INSTANCE_PROP);
    return valid;
  }

  /**
   * Returns true if required properties for FluoAdmin are set
   */
  public boolean hasRequiredAdminProps() {
    boolean valid = true;
    valid &= hasRequiredClientProps();
    valid &= verifyStringPropSet(ADMIN_ACCUMULO_TABLE_PROP);
    return valid;
  }

  /**
   * Returns true if required properties for Oracle are set
   */
  public boolean hasRequiredOracleProps() {
    boolean valid = true;
    valid &= hasRequiredClientProps();
    return valid;
  }

  /**
   * Returns true if required properties for Worker are set
   */
  public boolean hasRequiredWorkerProps() {
    boolean valid = true;
    valid &= hasRequiredClientProps();
    return valid;
  }

  /**
   * Returns true if required properties for MiniFluo are set
   */
  public boolean hasRequiredMiniFluoProps() {
    boolean valid = true;
    if (getMiniStartAccumulo()) {
      // ensure that client properties are not set since we are using MiniAccumulo
      valid &= verifyStringPropNotSet(CLIENT_ACCUMULO_USER_PROP);
      valid &= verifyStringPropNotSet(CLIENT_ACCUMULO_PASSWORD_PROP);
      valid &= verifyStringPropNotSet(CLIENT_ACCUMULO_INSTANCE_PROP);
      valid &= verifyStringPropNotSet(CLIENT_ACCUMULO_ZOOKEEPERS_PROP);
      valid &= verifyStringPropNotSet(CLIENT_ZOOKEEPER_CONNECT_PROP);
      if (valid == false) {
        log.error("Client properties should not be set in your configuration if MiniFluo is "
            + "configured to start its own accumulo (indicated by "
            + "io.fluo.mini.start.accumulo being set to true)");
      }
    } else {
      valid &= hasRequiredClientProps();
      valid &= hasRequiredAdminProps();
      valid &= hasRequiredOracleProps();
      valid &= hasRequiredWorkerProps();
    }
    return valid;
  }

  public Configuration getClientConfiguration() {
    Configuration clientConfig = new CompositeConfiguration();
    Iterator<String> iter = getKeys();
    while (iter.hasNext()) {
      String key = iter.next();
      if (key.startsWith(CLIENT_PREFIX)) {
        clientConfig.setProperty(key, getProperty(key));
      }
    }
    return clientConfig;
  }

  /**
   * Returns configuration with all Fluo properties set to their default. NOTE - some properties do
   * not have defaults and will not be set.
   */
  public static Configuration getDefaultConfiguration() {
    Configuration config = new CompositeConfiguration();
    setDefaultConfiguration(config);
    return config;
  }

  /**
   * Sets all Fluo properties to their default in the given configuration. NOTE - some properties do
   * not have defaults and will not be set.
   */
  public static void setDefaultConfiguration(Configuration config) {
    config.setProperty(CLIENT_ZOOKEEPER_CONNECT_PROP, CLIENT_ZOOKEEPER_CONNECT_DEFAULT);
    config.setProperty(CLIENT_ZOOKEEPER_TIMEOUT_PROP, CLIENT_ZOOKEEPER_TIMEOUT_DEFAULT);
    config.setProperty(CLIENT_ACCUMULO_ZOOKEEPERS_PROP, CLIENT_ACCUMULO_ZOOKEEPERS_DEFAULT);
    config.setProperty(CLIENT_CLASS_PROP, CLIENT_CLASS_DEFAULT);
    config.setProperty(ADMIN_CLASS_PROP, ADMIN_CLASS_DEFAULT);
    config.setProperty(WORKER_NUM_THREADS_PROP, WORKER_NUM_THREADS_DEFAULT);
    config.setProperty(WORKER_INSTANCES_PROP, WORKER_INSTANCES_DEFAULT);
    config.setProperty(WORKER_MAX_MEMORY_MB_PROP, WORKER_MAX_MEMORY_MB_DEFAULT);
    config.setProperty(WORKER_NUM_CORES_PROP, WORKER_NUM_CORES_DEFAULT);
    config.setProperty(TRANSACTION_ROLLBACK_TIME_PROP, TRANSACTION_ROLLBACK_TIME_DEFAULT);
    config.setProperty(LOADER_NUM_THREADS_PROP, LOADER_NUM_THREADS_DEFAULT);
    config.setProperty(LOADER_QUEUE_SIZE_PROP, LOADER_QUEUE_SIZE_DEFAULT);
    config.setProperty(ORACLE_MAX_MEMORY_MB_PROP, ORACLE_MAX_MEMORY_MB_DEFAULT);
    config.setProperty(ORACLE_NUM_CORES_PROP, ORACLE_NUM_CORES_DEFAULT);
    config.setProperty(MINI_CLASS_PROP, MINI_CLASS_DEFAULT);
    config.setProperty(MINI_START_ACCUMULO_PROP, MINI_START_ACCUMULO_DEFAULT);
    config.setProperty(MINI_DATA_DIR_PROP, MINI_DATA_DIR_DEFAULT);
  }

  private FluoConfiguration setNonNegativeInt(String property, int value) {
    Preconditions.checkArgument(value >= 0, property + " must be non-negative");
    setProperty(property, value);
    return this;
  }

  private int getNonNegativeInt(String property, int defaultValue) {
    int value = getInt(property, defaultValue);
    Preconditions.checkArgument(value >= 0, property + " must be non-negative");
    return value;
  }

  private FluoConfiguration setPositiveInt(String property, int value) {
    Preconditions.checkArgument(value > 0, property + " must be positive");
    setProperty(property, value);
    return this;
  }

  private int getPositiveInt(String property, int defaultValue) {
    int value = getInt(property, defaultValue);
    Preconditions.checkArgument(value > 0, property + " must be positive");
    return value;
  }

  private FluoConfiguration setPositiveLong(String property, long value) {
    Preconditions.checkArgument(value > 0, property + " must be positive");
    setProperty(property, value);
    return this;
  }

  private long getPositiveLong(String property, long defaultValue) {
    long value = getLong(property, defaultValue);
    Preconditions.checkArgument(value > 0, property + " must be positive");
    return value;
  }

  private FluoConfiguration setPort(String property, int value) {
    Preconditions.checkArgument(value >= 1 && value <= 65535, property
        + " must be valid port (1-65535)");
    setProperty(property, value);
    return this;
  }

  private int getPort(String property, int defaultValue) {
    int value = getInt(property, defaultValue);
    Preconditions.checkArgument(value >= 1 && value <= 65535, property
        + " must be valid port (1-65535)");
    return value;
  }

  private FluoConfiguration setNonEmptyString(String property, String value) {
    Preconditions.checkNotNull(value, property + " cannot be null");
    Preconditions.checkArgument(!value.isEmpty(), property + " cannot be empty");
    setProperty(property, value);
    return this;
  }

  private String getNonEmptyString(String property, String defaultValue) {
    String value = getString(property, defaultValue);
    Preconditions.checkNotNull(value, property + " cannot be null");
    Preconditions.checkArgument(!value.isEmpty(), property + " cannot be empty");
    return value;
  }

  private String getNonEmptyString(String property) {
    String value = getString(property);
    Preconditions.checkNotNull(value, property + " cannot be null");
    Preconditions.checkArgument(!value.isEmpty(), property + " cannot be empty");
    return value;
  }

  private static String verifyNotNull(String property, String value) {
    Preconditions.checkNotNull(value, property + " cannot be null");
    return value;
  }
}
