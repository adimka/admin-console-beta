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

package org.codice.ddf.admin.api.config.ldap;

import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateBindUserMethod;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateDn;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateEncryptionMethod;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateLdapQuery;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateLdapType;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.validateLdapUseCase;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validateHostName;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validateMappingNoWhiteSpace;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validatePort;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validateString;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validateStringNoWhiteSpace;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.validation.ValidationUtils;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public class LdapConfiguration extends Configuration {

    public static final String CONFIGURATION_TYPE = "ldap";

    public static final String HOST_NAME = "hostName";

    public static final String PORT = "port";

    public static final String ENCRYPTION_METHOD = "encryptionMethod";

    public static final String BIND_USER = "bindUser";

    public static final String BIND_USER_PASSWORD = "bindUserPassword";

    public static final String BIND_METHOD = "bindMethod";

    //    public static final String BIND_KDC = "kdcAddress";
    public static final String BIND_REALM = "bindRealm";

    public static final String USER_NAME_ATTRIBUTE = "userNameAttribute";

    public static final String BASE_GROUP_DN = "baseGroupDn";

    public static final String BASE_USER_DN = "baseUserDn";

    public static final String QUERY = "query";

    public static final String QUERY_BASE = "queryBase";

    public static final String LDAP_TYPE = "ldapType";

    public static final String LDAP_USE_CASE = "ldapUseCase";

    public static final String GROUP_OBJECT_CLASS = "groupObjectClass";

    public static final String GROUP_ATTRIBUTE_HOLDING_MEMBER = "groupAttributeHoldingMember";

    public static final String MEMBER_ATTRIBUTE_REFERENCED_IN_GROUP =
            "memberAttributeReferencedInGroup";

    public static final String ATTRIBUTE_MAPPINGS = "attributeMappings";

    public static final String ATTRIBUTE_MAPPING_PATH = "attributeMappingsPath";

    private String hostName;

    private int port;

    private String encryptionMethod;

    private String bindUserMethod;

    private String bindUser;

    private String bindUserPassword;

    //    private String bindKdcAddress;
    private String bindRealm;

    private String userNameAttribute;

    private String baseGroupDn;

    private String baseUserDn;

    private String query;

    private String queryBase;

    private String ldapType;

    private String ldapUseCase;

    private String groupObjectClass;

    private String groupAttributeHoldingMember;

    private String memberAttributeReferencedInGroup;

    public Map<String, String> attributeMappings;

    private String attributeMappingsPath;

    private static final Map<String, Function<LdapConfiguration, List<ConfigurationMessage>>>
            FIELD_TO_VALIDATION_FUNC =
            new ImmutableMap.Builder<String, Function<LdapConfiguration, List<ConfigurationMessage>>>().put(
                    SERVICE_PID,
                    config -> validateString(config.servicePid(), SERVICE_PID))
                    .put(FACTORY_PID, config -> validateString(config.factoryPid(), FACTORY_PID))
                    .put(HOST_NAME, config -> validateHostName(config.hostName(), HOST_NAME))
                    .put(PORT, config -> validatePort(config.port(), PORT))
                    .put(ENCRYPTION_METHOD,
                            config -> validateEncryptionMethod(config.encryptionMethod(),
                                    ENCRYPTION_METHOD))
                    .put(BIND_USER, config -> validateString(config.bindUser(), BIND_USER))
                    .put(BIND_USER_PASSWORD,
                            config -> validateString(config.bindUserPassword(), BIND_USER_PASSWORD))
                    .put(BIND_METHOD,
                            config -> validateBindUserMethod(config.bindUserMethod(), BIND_METHOD))
                    // .put(BIND_KDC, config -> validateBindKdcAddress(config.bindKdcAddress(), BIND_KDC))
                    .put(BIND_REALM,
                            config -> validateStringNoWhiteSpace(config.bindRealm(), BIND_REALM))
                    .put(USER_NAME_ATTRIBUTE,
                            config -> validateStringNoWhiteSpace(config.userNameAttribute(),
                                    USER_NAME_ATTRIBUTE))
                    .put(BASE_GROUP_DN, config -> validateDn(config.baseGroupDn(), BASE_GROUP_DN))
                    .put(BASE_USER_DN, config -> validateDn(config.baseUserDn(), BASE_USER_DN))
                    .put(QUERY, config -> validateLdapQuery(config.query(), QUERY))
                    .put(QUERY_BASE, config -> validateDn(config.queryBase(), QUERY_BASE))
                    .put(LDAP_TYPE, config -> validateLdapType(config.ldapType(), LDAP_TYPE))
                    .put(LDAP_USE_CASE,
                            config -> validateLdapUseCase(config.ldapUseCase(), LDAP_USE_CASE))
                    .put(GROUP_OBJECT_CLASS,
                            config -> validateStringNoWhiteSpace(config.groupObjectClass(),
                                    GROUP_OBJECT_CLASS))
                    .put(GROUP_ATTRIBUTE_HOLDING_MEMBER,
                            config -> validateStringNoWhiteSpace(config.groupAttributeHoldingMember(),
                                    GROUP_ATTRIBUTE_HOLDING_MEMBER))
                    .put(MEMBER_ATTRIBUTE_REFERENCED_IN_GROUP,
                            config -> validateStringNoWhiteSpace(config.memberAttributeReferencedInGroup(),
                                    MEMBER_ATTRIBUTE_REFERENCED_IN_GROUP))
                    .put(ATTRIBUTE_MAPPINGS,
                            config -> validateMappingNoWhiteSpace(config.attributeMappings(),
                                    ATTRIBUTE_MAPPINGS))
                    .put(ATTRIBUTE_MAPPING_PATH,
                            config -> validateString(config.attributeMappingsPath(),
                                    ATTRIBUTE_MAPPING_PATH))
                    .build();

    public List<ConfigurationMessage> validate(List<String> fields) {
        return ValidationUtils.validate(fields, this, FIELD_TO_VALIDATION_FUNC);
    }

    //Getters
    public String hostName() {
        return hostName;
    }

    public int port() {
        return port;
    }

    public String encryptionMethod() {
        return encryptionMethod;
    }

    public String bindUser() {
        return bindUser;
    }

    public String bindUserPassword() {
        return bindUserPassword;
    }

    public String bindUserMethod() {
        return bindUserMethod;
    }

    //    public String bindKdcAddress() {
    //        return bindKdcAddress;
    //    }
    public String bindRealm() {
        return bindRealm;
    }

    public String userNameAttribute() {
        return userNameAttribute;
    }

    public String baseUserDn() {
        return baseUserDn;
    }

    public String baseGroupDn() {
        return baseGroupDn;
    }

    public String query() {
        return query;
    }

    public String queryBase() {
        return queryBase;
    }

    public String ldapType() {
        return ldapType;
    }

    public String ldapUseCase() {
        return ldapUseCase;
    }

    public String groupObjectClass() {
        return groupObjectClass;
    }

    public String groupAttributeHoldingMember() {
        return groupAttributeHoldingMember;
    }

    public String memberAttributeReferencedInGroup() {
        return memberAttributeReferencedInGroup;
    }

    public Map<String, String> attributeMappings() {
        return attributeMappings;
    }

    public String attributeMappingsPath() {
        return attributeMappingsPath;
    }

    //Setters
    public LdapConfiguration hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public LdapConfiguration port(int port) {
        this.port = port;
        return this;
    }

    public LdapConfiguration encryptionMethod(String encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
        return this;
    }

    public LdapConfiguration bindUser(String bindUser) {
        this.bindUser = bindUser;
        return this;
    }

    public LdapConfiguration bindUserPassword(String bindUserPassword) {
        this.bindUserPassword = bindUserPassword;
        return this;
    }

    public LdapConfiguration bindUserMethod(String bindUserMethod) {
        this.bindUserMethod = bindUserMethod;
        return this;
    }

    //    public LdapConfiguration bindKdcAddress(String bindKdcAddress) {
    //        this.bindKdcAddress = bindKdcAddress;
    //        return this;
    //    }
    public LdapConfiguration bindRealm(String bindRealm) {
        this.bindRealm = bindRealm;
        return this;
    }

    public LdapConfiguration userNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
        return this;
    }

    public LdapConfiguration baseGroupDn(String baseGroupDn) {
        this.baseGroupDn = baseGroupDn;
        return this;
    }

    public LdapConfiguration baseUserDn(String baseUserDn) {
        this.baseUserDn = baseUserDn;
        return this;
    }

    public LdapConfiguration query(String query) {
        this.query = query;
        return this;
    }

    public LdapConfiguration queryBase(String queryBase) {
        this.queryBase = queryBase;
        return this;
    }

    public LdapConfiguration ldapType(String ldapType) {
        this.ldapType = ldapType;
        return this;
    }

    public LdapConfiguration ldapUseCase(String ldapUseCase) {
        this.ldapUseCase = ldapUseCase;
        return this;
    }

    public LdapConfiguration groupObjectClass(String groupObjectClass) {
        this.groupObjectClass = groupObjectClass;
        return this;
    }

    public LdapConfiguration groupAttributeHoldingMember(String groupAttributeHoldingMember) {
        this.groupAttributeHoldingMember = groupAttributeHoldingMember;
        return this;
    }

    public LdapConfiguration memberAttributeReferencedInGroup(
            String memberAttributeReferencedInGroup) {
        this.memberAttributeReferencedInGroup = memberAttributeReferencedInGroup;
        return this;
    }

    public LdapConfiguration attributeMappings(Map<String, String> attributeMapping) {
        this.attributeMappings = attributeMapping;
        return this;
    }

    public LdapConfiguration attributeMappingsPath(String attributeMappingsPath) {
        this.attributeMappingsPath = attributeMappingsPath;
        return this;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, LdapConfiguration.class);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostName", hostName)
                .add("port", port)
                .add("encryptionMethod", encryptionMethod)
                .add("bindUserMethod", bindUserMethod)
                .add("bindUser", bindUser)
                .add("bindUserPassword", "********")
                .add("bindRealm", bindRealm)
                .add("userNameAttribute", userNameAttribute)
                .add("baseGroupDn", baseGroupDn)
                .add("baseUserDn", baseUserDn)
                .add("ldapType", ldapType)
                .add("ldapUseCase", ldapUseCase)
                .add("groupObjectClass", groupObjectClass)
                .add("groupAttributeHoldingMember", groupAttributeHoldingMember)
                .add("memberAttributeReferencedInGroup", memberAttributeReferencedInGroup)
                .add("attributeMappings", attributeMappings)
                .add("attributeMappingsPath", attributeMappingsPath)
                .toString();
    }
}
