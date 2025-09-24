package com.scim2.server.scim2_server.model;

import com.unboundid.scim2.common.BaseScimResource;
import com.unboundid.scim2.common.annotations.Attribute;
import com.unboundid.scim2.common.annotations.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Enterprise User Extension", 
        id = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
        name = "EnterpriseUser")
public class EnterpriseUser extends BaseScimResource {
    
    @Attribute(description = "Identifies the name of a cost center.")
    @JsonProperty("costCenter")
    private String costCenter;
    
    @Attribute(description = "Identifies the name of an organization.")
    @JsonProperty("organization")
    private String organization;
    
    @Attribute(description = "Identifies the name of a division.")
    @JsonProperty("division")
    private String division;
    
    @Attribute(description = "Identifies the name of a department.")
    @JsonProperty("department")
    private String department;
    
    @Attribute(description = "The user's manager.")
    @JsonProperty("manager")
    private Manager manager;
    
    @Attribute(description = "A numeric or alphanumeric identifier assigned to a person.")
    @JsonProperty("employeeNumber")
    private String employeeNumber;
    
    public String getCostCenter() {
        return costCenter;
    }
    
    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }
    
    public String getOrganization() {
        return organization;
    }
    
    public void setOrganization(String organization) {
        this.organization = organization;
    }
    
    public String getDivision() {
        return division;
    }
    
    public void setDivision(String division) {
        this.division = division;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public Manager getManager() {
        return manager;
    }
    
    public void setManager(Manager manager) {
        this.manager = manager;
    }
    
    public String getEmployeeNumber() {
        return employeeNumber;
    }
    
    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }
    
    public static class Manager {
        @JsonProperty("value")
        private String value;
        
        @JsonProperty("$ref")
        private String ref;
        
        @JsonProperty("displayName")
        private String displayName;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getRef() {
            return ref;
        }
        
        public void setRef(String ref) {
            this.ref = ref;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}