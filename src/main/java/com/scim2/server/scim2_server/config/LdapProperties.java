package com.scim2.server.scim2_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for LDAP backend connection and settings.
 * Maps to ldap.* properties in application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {
    
    private boolean enabled = false;
    private String url = "ldap://localhost:1389";
    private String bindDn = "cn=Directory Manager";
    private String bindPassword = "password";
    private String baseDn = "dc=example,dc=com";
    private String userBaseDn = "ou=users,dc=example,dc=com";
    private String groupBaseDn = "ou=groups,dc=example,dc=com";
    private boolean useEntryUuidDn = true;
    
    private PoolSettings pool = new PoolSettings();
    private SearchSettings search = new SearchSettings();
    
    // Getters and setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getBindDn() {
        return bindDn;
    }
    
    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }
    
    public String getBindPassword() {
        return bindPassword;
    }
    
    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }
    
    public String getBaseDn() {
        return baseDn;
    }
    
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }
    
    public String getUserBaseDn() {
        return userBaseDn;
    }
    
    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }
    
    public String getGroupBaseDn() {
        return groupBaseDn;
    }
    
    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }
    
    public boolean isUseEntryUuidDn() {
        return useEntryUuidDn;
    }
    
    public void setUseEntryUuidDn(boolean useEntryUuidDn) {
        this.useEntryUuidDn = useEntryUuidDn;
    }
    
    public PoolSettings getPool() {
        return pool;
    }
    
    public void setPool(PoolSettings pool) {
        this.pool = pool;
    }
    
    public SearchSettings getSearch() {
        return search;
    }
    
    public void setSearch(SearchSettings search) {
        this.search = search;
    }
    
    /**
     * Connection pool settings
     */
    public static class PoolSettings {
        private int initialConnections = 2;
        private int maxConnections = 10;
        private long maxConnectionAgeMillis = 3600000L; // 1 hour
        private long healthCheckIntervalMillis = 60000L; // 1 minute
        
        public int getInitialConnections() {
            return initialConnections;
        }
        
        public void setInitialConnections(int initialConnections) {
            this.initialConnections = initialConnections;
        }
        
        public int getMaxConnections() {
            return maxConnections;
        }
        
        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
        
        public long getMaxConnectionAgeMillis() {
            return maxConnectionAgeMillis;
        }
        
        public void setMaxConnectionAgeMillis(long maxConnectionAgeMillis) {
            this.maxConnectionAgeMillis = maxConnectionAgeMillis;
        }
        
        public long getHealthCheckIntervalMillis() {
            return healthCheckIntervalMillis;
        }
        
        public void setHealthCheckIntervalMillis(long healthCheckIntervalMillis) {
            this.healthCheckIntervalMillis = healthCheckIntervalMillis;
        }
    }
    
    /**
     * LDAP search settings
     */
    public static class SearchSettings {
        private int sizeLimit = 1000;
        private int timeLimitSeconds = 30;
        
        public int getSizeLimit() {
            return sizeLimit;
        }
        
        public void setSizeLimit(int sizeLimit) {
            this.sizeLimit = sizeLimit;
        }
        
        public int getTimeLimitSeconds() {
            return timeLimitSeconds;
        }
        
        public void setTimeLimitSeconds(int timeLimitSeconds) {
            this.timeLimitSeconds = timeLimitSeconds;
        }
    }
}
