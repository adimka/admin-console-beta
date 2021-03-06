/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.admin.api.configurator;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.codice.ddf.admin.api.configurator.operations.AdminOperation;
import org.codice.ddf.admin.api.configurator.operations.BundleOperation;
import org.codice.ddf.admin.api.configurator.operations.FeatureOperation;
import org.codice.ddf.admin.api.configurator.operations.ManagedServiceOperation;
import org.codice.ddf.admin.api.configurator.operations.PropertyOperation;
import org.codice.ddf.ui.admin.api.ConfigurationAdmin;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.audit.SecurityLogger;

/**
 * Transactional orchestrator for persisting configuration changes.
 * <p>
 * Sequentially processes {@link Operation}s, committing their changes. If a failure occurs
 * during the processing, a rollback is attempted of those handlers that had already been committed.
 * When the {@link #commit()} operation completes - either successfully, with a successful rollback,
 * or with a failure to rollback - it returns a {@link OperationReport} of the outcome. In the case of
 * rollback failures, callers of this class should inform users of those failures so they may manually
 * intercede.
 * <p>
 * This class does not guarantee that it can reliably rollback changes in the case of failure. It
 * makes a best-effort to revert changes and reports the outcome.
 * <p>
 * To use this class, first instantiate then invoke the various methods for feature, bundle, config,
 * etc. updates in the order they should be applied. When all have been completed, call the
 * {@link #commit()} method to write the changes to the system. The resulting {@link OperationReport}
 * will have the outcome.
 */
public class Configurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configurator.class);

    private final Map<String, Operation> configHandlers = new LinkedHashMap<>();

    /**
     * Sequentially invokes all the {@link Operation}s, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     * <p>
     * In the case of a successful commit, the {@link SecurityLogger} will be invoked to log changes.
     *
     * @param auditMessage In the case of a successful commit, the message to pass to the
     *                     {@link SecurityLogger}
     * @param auditParams  In the case of a successful commit, the optional parameters to pass to the
     *                     {@link SecurityLogger} to be interpolated into the message
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    public OperationReport commit(String auditMessage, String... auditParams) {
        OperationReport report = commit();
        if (report.txactSucceeded()) {
            SecurityLogger.audit(auditMessage, (Object[]) auditParams);
        }

        return report;
    }

    /**
     * Sequentially invokes all the {@link Operation}s, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     *
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    public OperationReport commit() {
        OperationReport configReport = new OperationReport();
        for (Map.Entry<String, Operation> row : configHandlers.entrySet()) {
            try {
                Object commitResult = row.getValue()
                        .commit();
                if (commitResult instanceof String) {
                    configReport.putResult(row.getKey(),
                            OperationReport.Result.passManagedService((String) commitResult));
                } else {
                    configReport.putResult(row.getKey(), OperationReport.Result.pass());
                }
            } catch (ConfiguratorException e) {
                LOGGER.debug("Error committing configuration change", e);

                // On failure, attempt to rollback any config changes that have already been made
                // and then break out of loop processing, only reporting the remaining as skipped
                rollback(row.getKey(), configReport, e);
                break;
            }
        }

        return configReport;
    }

    /**
     * Starts the bundle with the given name.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String startBundle(String bundleSymName) {
        return registerHandler(BundleOperation.forStart(bundleSymName, getBundleContext()));
    }

    /**
     * Stops the bundle with the given name.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String stopBundle(String bundleSymName) {
        return registerHandler(BundleOperation.forStop(bundleSymName, getBundleContext()));
    }

    /**
     * Determines if the bundle with the given name is started.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return true if started; else, false
     */
    public boolean isBundleStarted(String bundleSymName) {
        try {
            return BundleOperation.forStart(bundleSymName, getBundleContext())
                    .readState();
        } catch (ConfiguratorException e) {
            return false;
        }
    }

    /**
     * Installs and starts the feature with the given name.
     *
     * @param featureName the name of the feature
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String startFeature(String featureName) {
        return registerHandler(FeatureOperation.forStart(featureName, getBundleContext()));
    }

    /**
     * Stops the feature with the given name.
     *
     * @param featureName the name of the feature
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String stopFeature(String featureName) {
        return registerHandler(FeatureOperation.forStop(featureName, getBundleContext()));
    }

    /**
     * Determines if the feature with the given name is started.
     *
     * @param featureName the name of the feature
     * @return true if started; else, false
     */
    public boolean isFeatureStarted(String featureName) {
        return FeatureOperation.forStart(featureName, getBundleContext())
                .readState();
    }

    /**
     * Creates a property file in the system with the given set of new key:value pairs.
     *
     * @param propFile   the property file to create
     * @param properties the set of key:value pairs to save to the property file
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String createPropertyFile(Path propFile, Map<String, String> properties) {
        return registerHandler(PropertyOperation.forCreate(propFile, properties));
    }

    /**
     * Deletes a property file in the system.
     *
     * @param propFile the property file to delete
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String deletePropertyFile(Path propFile) {
        return registerHandler(PropertyOperation.forDelete(propFile));
    }

    /**
     * Updates a property file in the system with the given set of new key:value pairs.
     *
     * @param propFile    the property file to update
     * @param properties  the set of key:value pairs to save to the property file
     * @param keepIgnored if true, then any keys already in the property file will retain their
     *                    initial values if they are excluded from the {@code properties} param; if
     *                    false, then the only properties that will be in the updated file are those
     *                    provided by the {@code properties} param
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String updatePropertyFile(Path propFile, Map<String, String> properties,
            boolean keepIgnored) {
        return registerHandler(PropertyOperation.forUpdate(propFile, properties, keepIgnored));
    }

    /**
     * Gets the current key:value pairs set in the given property file.
     *
     * @param propFile the property file to query
     * @return the current set of key:value pairs
     */
    public Map<String, String> getProperties(Path propFile) {
        return PropertyOperation.forUpdate(propFile, Collections.emptyMap(), true)
                .readState();
    }

    /**
     * Updates a bundle configuration file in the system with the given set of new key:value pairs.
     *
     * @param configPid   the configId of the bundle configuration file to update
     * @param configs     the set of key:value pairs to save in the configuration
     * @param keepIgnored if true, then any keys already in the config file will retain their
     *                    initial values if they are excluded from the {@code properties} param; if
     *                    false, then the only config entires that will be in the updated file are those
     *                    provided by the {@code properties} param
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String updateConfigFile(String configPid, Map<String, Object> configs,
            boolean keepIgnored) {
        return registerHandler(AdminOperation.instance(configPid,
                configs,
                keepIgnored,
                getConfigAdminMBean()));
    }

    /**
     * Gets the current key:value pairs set in the given configuration file.
     *
     * @param configPid the configId of the bundle configuration file to query
     * @return the current set of key:value pairs
     */
    public Map<String, Object> getConfig(String configPid) {
        return AdminOperation.instance(configPid,
                Collections.emptyMap(),
                true,
                getConfigAdminMBean())
                .readState();
    }

    /**
     * Creates a new managed service for the given factory.
     *
     * @param factoryPid the factoryPid of the service to create
     * @param configs    the set of key:value pairs to save in the new managed service's configuration
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String createManagedService(String factoryPid, Map<String, Object> configs) {
        return registerHandler(ManagedServiceOperation.forCreate(factoryPid,
                configs,
                getConfigAdmin(),
                getConfigAdminMBean()));
    }

    /**
     * Deletes a managed service.
     *
     * @param configPid the configPid of the instance of the service to delete
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    public String deleteManagedService(String configPid) {
        return registerHandler(ManagedServiceOperation.forDelete(configPid,
                getConfigAdmin(),
                getConfigAdminMBean()));
    }

    public Map<String, Map<String, Object>> getManagedServiceConfigs(String factoryPid) {
        return ManagedServiceOperation.forCreate(factoryPid,
                Collections.emptyMap(),
                getConfigAdmin(),
                getConfigAdminMBean())
                .readState();
    }

    /**
     * Retrieves the service reference. The reference should only be used for reading purposes,
     * any changes should be done through a commit
     *
     * @param serviceClass - Class of service to retrieve
     * @return first found service reference of serviceClass
     * @throws ConfiguratorException if any errors occur
     */
    public <S> S getServiceReference(Class<S> serviceClass) throws ConfiguratorException {
        BundleContext context = getBundleContext();
        ServiceReference<S> ref = context.getServiceReference(serviceClass);
        if (ref == null) {
            return null;
        }

        return context.getService(ref);
    }

    private String registerHandler(Operation handler) {
        String key = UUID.randomUUID()
                .toString();
        configHandlers.put(key, handler);
        return key;
    }

    private void rollback(String failedStep, OperationReport configReport,
            ConfiguratorException exception) {
        configReport.putResult(failedStep, OperationReport.Result.fail(exception));

        Deque<Map.Entry<String, Operation>> undoStack = new ArrayDeque<>();
        boolean skipRest = false;

        for (Map.Entry<String, Operation> row : configHandlers.entrySet()) {
            if (failedStep.equals(row.getKey())) {
                skipRest = true;
            }

            if (!skipRest) {
                undoStack.push(row);
            } else if (!failedStep.equals(row.getKey())) {
                configReport.putResult(row.getKey(), OperationReport.Result.skip());
            }
        }

        for (Map.Entry<String, Operation> row : undoStack) {
            try {
                row.getValue()
                        .rollback();

                configReport.putResult(row.getKey(), OperationReport.Result.rollback());
            } catch (ConfiguratorException e) {
                String configId = configReport.getResult(row.getKey())
                        .getConfigId();
                if (configId == null) {
                    configReport.putResult(row.getKey(), OperationReport.Result.rollbackFail(e));
                } else {
                    configReport.putResult(row.getKey(),
                            OperationReport.Result.rollbackFailManagedService(e, configId));
                }
            }
        }
    }

    /**
     * Gets the OSGi bundle context.
     *
     * @return the bundle context
     * @throws ConfiguratorException if this bundle cannot be found
     */
    private BundleContext getBundleContext() throws ConfiguratorException {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle == null) {
            LOGGER.info("Unable to access bundle context");
            throw new ConfiguratorException("Internal error");
        }

        return bundle.getBundleContext();
    }

    /**
     * Gets the config admin for working with OSGi features and bundles.
     *
     * @return the service wrapper to use for working with features and bundles
     * @throws ConfiguratorException if there is an error accessing the config admin
     */
    private ConfigurationAdmin getConfigAdmin() throws ConfiguratorException {
        BundleContext context = getBundleContext();
        ServiceReference<org.osgi.service.cm.ConfigurationAdmin> serviceReference =
                context.getServiceReference(org.osgi.service.cm.ConfigurationAdmin.class);
        return new ConfigurationAdmin(context.getService(serviceReference));
    }

    /**
     * Gets the config admin mbean for working with OSGi bundle configurations.
     *
     * @return the mbean to use for updating bundle configurations
     * @throws ConfiguratorException if there is an error accessing the mbean
     */
    private ConfigurationAdminMBean getConfigAdminMBean() throws ConfiguratorException {
        try {
            ObjectName objectName = new ObjectName(ConfigurationAdminMBean.OBJECTNAME);
            return MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(),
                    objectName,
                    ConfigurationAdminMBean.class,
                    false);
        } catch (MalformedObjectNameException e) {
            LOGGER.debug("Unexpected error finding ConfigurationAdminMBean", e);
            throw new ConfiguratorException("Internal error");
        }
    }

}
