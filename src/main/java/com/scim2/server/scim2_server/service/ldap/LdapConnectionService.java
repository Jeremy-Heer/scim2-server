package com.scim2.server.scim2_server.service.ldap;

import com.scim2.server.scim2_server.config.LdapProperties;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;

/**
 * Service for managing LDAP connections using UnboundID LDAP SDK.
 * Provides connection pooling, health checks, and UnboundID-specific controls.
 * 
 * Features:
 * - Connection pooling with health checks
 * - Support for Name with entryUUID control (OID 1.3.6.1.4.1.30221.2.5.44)
 * - Automatic reconnection
 * - SSL/TLS support
 */
@Service
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
public class LdapConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(LdapConnectionService.class);
    
    /**
     * UnboundID Name with entryUUID Request Control OID.
     * When used in ADD operations, the server generates the DN using the entryUUID.
     * Format: entryUUID=<uuid>,<parent-dn>
     */
    public static final String NAME_WITH_ENTRY_UUID_OID = "1.3.6.1.4.1.30221.2.5.44";
    
    private final LdapProperties ldapProperties;
    private LDAPConnectionPool connectionPool;
    
    public LdapConnectionService(LdapProperties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }
    
    @PostConstruct
    public void init() throws LDAPException, java.security.GeneralSecurityException {
        logger.info("Initializing LDAP connection pool...");
        logger.info("LDAP URL: {}", ldapProperties.getUrl());
        logger.info("LDAP Bind DN: {}", ldapProperties.getBindDn());
        logger.info("LDAP Base DN: {}", ldapProperties.getBaseDn());
        logger.info("LDAP User Base DN: {}", ldapProperties.getUserBaseDn());
        logger.info("LDAP Group Base DN: {}", ldapProperties.getGroupBaseDn());
        logger.info("Use entryUUID DN: {}", ldapProperties.isUseEntryUuidDn());
        
        // Parse LDAP URL
        LDAPURL ldapUrl = new LDAPURL(ldapProperties.getUrl());
        String host = ldapUrl.getHost();
        int port = ldapUrl.getPort();
        boolean useSSL = ldapUrl.getScheme().equalsIgnoreCase("ldaps");
        
        LDAPConnection connection;
        
        if (useSSL) {
            // Create SSL socket factory (for development, trust all certificates)
            // TODO: In production, use proper certificate validation
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
            
            connection = new LDAPConnection(socketFactory, host, port, 
                                           ldapProperties.getBindDn(), 
                                           ldapProperties.getBindPassword());
        } else {
            connection = new LDAPConnection(host, port, 
                                           ldapProperties.getBindDn(), 
                                           ldapProperties.getBindPassword());
        }
        
        logger.info("LDAP connection established successfully");
        
        // Create connection pool
        LDAPConnectionPoolHealthCheck healthCheck = new GetEntryLDAPConnectionPoolHealthCheck(
            "cn=config", 
            ldapProperties.getPool().getHealthCheckIntervalMillis(),
            false, // invokeOnCreate
            false, // invokeAfterAuthentication
            false, // invokeOnCheckout
            true,  // invokeOnRelease
            true,  // invokeForBackgroundChecks
            true   // invokeOnException
        );
        
        connectionPool = new LDAPConnectionPool(
            connection,
            ldapProperties.getPool().getInitialConnections(),
            ldapProperties.getPool().getMaxConnections()
        );
        connectionPool.setHealthCheck(healthCheck);
        
        // Set connection pool options
        connectionPool.setMaxConnectionAgeMillis(ldapProperties.getPool().getMaxConnectionAgeMillis());
        connectionPool.setCreateIfNecessary(true);
        connectionPool.setRetryFailedOperationsDueToInvalidConnections(true);
        
        logger.info("LDAP connection pool initialized with {} initial connections, max {}",
                   ldapProperties.getPool().getInitialConnections(),
                   ldapProperties.getPool().getMaxConnections());
    }
    
    @PreDestroy
    public void destroy() {
        if (connectionPool != null) {
            logger.info("Closing LDAP connection pool...");
            connectionPool.close();
            logger.info("LDAP connection pool closed");
        }
    }
    
    /**
     * Get a connection from the pool.
     * Connection is automatically returned to pool when closed.
     * 
     * @return LDAPConnection from pool
     * @throws LDAPException if connection cannot be obtained
     */
    public LDAPConnection getConnection() throws LDAPException {
        if (connectionPool == null) {
            throw new LDAPException(ResultCode.CONNECT_ERROR, "Connection pool not initialized");
        }
        return connectionPool.getConnection();
    }
    
    /**
     * Execute a search operation using a connection from the pool.
     * 
     * @param searchRequest SearchRequest to execute
     * @return SearchResult with matching entries
     * @throws LDAPException if search fails
     */
    public SearchResult search(SearchRequest searchRequest) throws LDAPException {
        try (LDAPConnection connection = getConnection()) {
            return connection.search(searchRequest);
        }
    }
    
    /**
     * Execute an add operation using a connection from the pool.
     * 
     * @param addRequest AddRequest to execute
     * @return LDAPResult with operation result
     * @throws LDAPException if add fails
     */
    public LDAPResult add(AddRequest addRequest) throws LDAPException {
        try (LDAPConnection connection = getConnection()) {
            return connection.add(addRequest);
        }
    }
    
    /**
     * Execute a modify operation using a connection from the pool.
     * 
     * @param modifyRequest ModifyRequest to execute
     * @return LDAPResult with operation result
     * @throws LDAPException if modify fails
     */
    public LDAPResult modify(ModifyRequest modifyRequest) throws LDAPException {
        try (LDAPConnection connection = getConnection()) {
            return connection.modify(modifyRequest);
        }
    }
    
    /**
     * Execute a delete operation using a connection from the pool.
     * 
     * @param deleteRequest DeleteRequest to execute
     * @return LDAPResult with operation result
     * @throws LDAPException if delete fails
     */
    public LDAPResult delete(DeleteRequest deleteRequest) throws LDAPException {
        try (LDAPConnection connection = getConnection()) {
            return connection.delete(deleteRequest);
        }
    }
    
    /**
     * Create a Name with entryUUID control for ADD operations.
     * This control tells PingDirectory to use the server-generated entryUUID
     * as the RDN component of the DN.
     * 
     * Usage:
     * AddRequest addRequest = new AddRequest(dn, attributes);
     * addRequest.addControl(createNameWithEntryUuidControl());
     * 
     * Result DN format: entryUUID=<generated-uuid>,ou=users,dc=example,dc=com
     * 
     * @return Control for Name with entryUUID
     */
    public Control createNameWithEntryUuidControl() {
        return new Control(NAME_WITH_ENTRY_UUID_OID, true);
    }
    
    /**
     * Search for an entry by entryUUID attribute.
     * This is used to resolve SCIM IDs (which are UUIDs) to LDAP DNs.
     * 
     * @param uuid UUID value to search for
     * @param baseDn Base DN to search under
     * @return SearchResultEntry if found, null otherwise
     * @throws LDAPException if search fails
     */
    public SearchResultEntry findEntryByUuid(String uuid, String baseDn) throws LDAPException {
        Filter filter = Filter.createEqualityFilter("entryUUID", uuid);
        
        SearchRequest searchRequest = new SearchRequest(
            baseDn,
            SearchScope.SUB,
            filter,
            "*", "+" // Request all user and operational attributes
        );
        
        searchRequest.setSizeLimit(1);
        
        SearchResult result = search(searchRequest);
        
        if (result.getEntryCount() > 0) {
            return result.getSearchEntries().get(0);
        }
        
        return null;
    }
    
    /**
     * Extract the UUID from a DN that uses entryUUID naming.
     * 
     * Example DN: entryUUID=4058d13c-6a5c-4c27-aa71-9bd20048ddc5,ou=users,dc=example,dc=com
     * Returns: 4058d13c-6a5c-4c27-aa71-9bd20048ddc5
     * 
     * @param dn DN string
     * @return UUID value or null if DN doesn't use entryUUID naming
     */
    public String extractUuidFromDn(String dn) {
        if (dn == null || !dn.toLowerCase().startsWith("entryuuid=")) {
            return null;
        }
        
        int commaIndex = dn.indexOf(',');
        if (commaIndex > 0) {
            return dn.substring(10, commaIndex); // "entryUUID=".length() == 10
        }
        
        return null;
    }
    
    /**
     * Build a user DN from a username (uid).
     * If useEntryUuidDn is false, uses uid-based DN.
     * If useEntryUuidDn is true, DN will be set after ADD operation with Name-with-entryUUID control.
     * 
     * @param uid Username value
     * @return DN string
     */
    public String buildUserDn(String uid) {
        if (ldapProperties.isUseEntryUuidDn()) {
            // Placeholder DN for ADD - will be replaced by server with entryUUID-based DN
            return "uid=" + escapeRdnValue(uid) + "," + ldapProperties.getUserBaseDn();
        } else {
            return "uid=" + escapeRdnValue(uid) + "," + ldapProperties.getUserBaseDn();
        }
    }
    
    /**
     * Build a group DN from a group name (cn).
     * Similar to buildUserDn for groups.
     * 
     * @param cn Group common name
     * @return DN string
     */
    public String buildGroupDn(String cn) {
        if (ldapProperties.isUseEntryUuidDn()) {
            // Placeholder DN for ADD - will be replaced by server with entryUUID-based DN
            return "cn=" + escapeRdnValue(cn) + "," + ldapProperties.getGroupBaseDn();
        } else {
            return "cn=" + escapeRdnValue(cn) + "," + ldapProperties.getGroupBaseDn();
        }
    }
    
    /**
     * Build DN from entryUUID for member attribute operations.
     * When entryUUID-based DNs are enabled, this constructs the proper DN format.
     * 
     * @param uuid The entryUUID value (SCIM ID)
     * @param baseDn The base DN (user or group base DN)
     * @return DN string in entryUUID format
     */
    public String buildDnFromUuid(String uuid, String baseDn) {
        if (ldapProperties.isUseEntryUuidDn()) {
            return "entryUUID=" + uuid + "," + baseDn;
        } else {
            // Fallback: search for the entry to get its DN
            try {
                SearchResultEntry entry = findEntryByUuid(uuid, baseDn);
                return entry != null ? entry.getDN() : null;
            } catch (LDAPException e) {
                logger.error("Failed to find entry by UUID: {}", uuid, e);
                return null;
            }
        }
    }

    /**
     * Escape special characters in RDN values.
     * 
     * @param value Raw RDN value
     * @return Escaped value
     */
    private String escapeRdnValue(String value) {
        if (value == null) {
            return "";
        }
        
        // UnboundID SDK handles escaping in RDN and DN classes
        // For simple cases, just return as-is
        return value.replace(",", "\\,")
                    .replace("=", "\\=")
                    .replace("+", "\\+")
                    .replace("<", "\\<")
                    .replace(">", "\\>")
                    .replace("#", "\\#")
                    .replace(";", "\\;")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }
    
    /**
     * Get LDAP connection pool statistics for monitoring.
     * 
     * @return String with connection pool statistics
     */
    public String getConnectionPoolStats() {
        if (connectionPool == null) {
            return "Connection pool not initialized";
        }
        
        LDAPConnectionPoolStatistics stats = connectionPool.getConnectionPoolStatistics();
        return String.format(
            "LDAP Pool Stats: Available=%d, Max=%d, " +
            "Successful Checkouts=%d, Failed Checkouts=%d, " +
            "Connections Closed Defunct=%d, Connections Closed Expired=%d",
            connectionPool.getCurrentAvailableConnections(),
            connectionPool.getMaximumAvailableConnections(),
            stats.getNumSuccessfulCheckouts(),
            stats.getNumFailedCheckouts(),
            stats.getNumConnectionsClosedDefunct(),
            stats.getNumConnectionsClosedExpired()
        );
    }
    
    public LdapProperties getLdapProperties() {
        return ldapProperties;
    }
}
