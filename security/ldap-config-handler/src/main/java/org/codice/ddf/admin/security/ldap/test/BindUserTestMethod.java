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
 */

package org.codice.ddf.admin.security.ldap.test;

import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_USER;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateBindRealm;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.toDescriptionMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;

public class BindUserTestMethod extends TestMethod<LdapConfiguration> {
    public static final String DESCRIPTION =
            "Attempts to bind the specified user to specified ldap connection.";

    private static final List<String> REQUIRED_FIELDS = ImmutableList.of(HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER,
            BIND_USER_PASSWORD,
            BIND_METHOD);

    private static final List<String> OPTIONAL_FIELDS = ImmutableList.of(BIND_REALM);

    private static final Map<String, String> SUCCESS_TYPES =
            toDescriptionMap(Collections.singletonList(SUCCESSFUL_BIND));

    private static final Map<String, String> FAILURE_TYPES = toDescriptionMap(Arrays.asList(
            CANNOT_CONFIGURE,
            CANNOT_CONNECT,
            CANNOT_BIND));

    private static final String LDAP_BIND_TEST_ID = "bind";

    private final LdapTestingCommons ldapTestingCommons;

    public BindUserTestMethod(LdapTestingCommons ldapTestingCommons) {
        super(LDAP_BIND_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);

        this.ldapTestingCommons = ldapTestingCommons;
    }

    @Override
    public Report test(LdapConfiguration configuration) {
        LdapTestingCommons.LdapConnectionAttempt bindConnectionAttempt =
                ldapTestingCommons.bindUserToLdapConnection(configuration);

        if (bindConnectionAttempt.result() == SUCCESSFUL_BIND) {
            bindConnectionAttempt.connection()
                    .close();
        }

        return Report.createReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                Collections.singletonList(bindConnectionAttempt.result()
                        .name()));
    }

    @Override
    public List<ConfigurationMessage> validateOptionalFields(LdapConfiguration configuration) {
        return validateBindRealm(configuration);
    }
}
