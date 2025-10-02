package com.scim2.server.scim2_server.service;

import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.GroupResource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Service for handling SCIM sorting (sortBy and sortOrder parameters)
 */
@Service
public class ScimSortingService {
    
    /**
     * Sort a list of users
     * @param users List of users to sort
     * @param sortBy Attribute name to sort by
     * @param sortOrder "ascending" or "descending"
     * @return Sorted list of users
     */
    public List<UserResource> sortUsers(List<UserResource> users, String sortBy, String sortOrder) {
        if (users == null || users.isEmpty() || sortBy == null || sortBy.trim().isEmpty()) {
            return users;
        }
        
        boolean ascending = !"descending".equalsIgnoreCase(sortOrder);
        Comparator<UserResource> comparator = createUserComparator(sortBy.trim().toLowerCase());
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return users.stream().sorted(comparator).toList();
    }
    
    /**
     * Sort a list of groups
     * @param groups List of groups to sort
     * @param sortBy Attribute name to sort by
     * @param sortOrder "ascending" or "descending"
     * @return Sorted list of groups
     */
    public List<GroupResource> sortGroups(List<GroupResource> groups, String sortBy, String sortOrder) {
        if (groups == null || groups.isEmpty() || sortBy == null || sortBy.trim().isEmpty()) {
            return groups;
        }
        
        boolean ascending = !"descending".equalsIgnoreCase(sortOrder);
        Comparator<GroupResource> comparator = createGroupComparator(sortBy.trim().toLowerCase());
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return groups.stream().sorted(comparator).toList();
    }
    
    private Comparator<UserResource> createUserComparator(String sortBy) {
        return switch (sortBy) {
            case "id" -> Comparator.comparing(UserResource::getId, nullSafeStringComparator());
            case "username" -> Comparator.comparing(UserResource::getUserName, nullSafeStringComparator());
            case "displayname" -> Comparator.comparing(UserResource::getDisplayName, nullSafeStringComparator());
            case "name.familyname" -> Comparator.comparing(
                user -> user.getName() != null ? user.getName().getFamilyName() : null,
                nullSafeStringComparator()
            );
            case "name.givenname" -> Comparator.comparing(
                user -> user.getName() != null ? user.getName().getGivenName() : null,
                nullSafeStringComparator()
            );
            case "title" -> Comparator.comparing(UserResource::getTitle, nullSafeStringComparator());
            case "usertype" -> Comparator.comparing(UserResource::getUserType, nullSafeStringComparator());
            case "active" -> Comparator.comparing(UserResource::getActive, nullSafeBooleanComparator());
            case "meta.created" -> Comparator.comparing(
                user -> user.getMeta() != null && user.getMeta().getCreated() != null ?
                    user.getMeta().getCreated().toInstant() : null,
                nullSafeInstantComparator()
            );
            case "meta.lastmodified" -> Comparator.comparing(
                user -> user.getMeta() != null && user.getMeta().getLastModified() != null ?
                    user.getMeta().getLastModified().toInstant() : null,
                nullSafeInstantComparator()
            );
            default -> Comparator.comparing(UserResource::getId, nullSafeStringComparator()); // Default to ID
        };
    }
    
    private Comparator<GroupResource> createGroupComparator(String sortBy) {
        return switch (sortBy) {
            case "id" -> Comparator.comparing(GroupResource::getId, nullSafeStringComparator());
            case "displayname" -> Comparator.comparing(GroupResource::getDisplayName, nullSafeStringComparator());
            case "meta.created" -> Comparator.comparing(
                group -> group.getMeta() != null && group.getMeta().getCreated() != null ?
                    group.getMeta().getCreated().toInstant() : null,
                nullSafeInstantComparator()
            );
            case "meta.lastmodified" -> Comparator.comparing(
                group -> group.getMeta() != null && group.getMeta().getLastModified() != null ?
                    group.getMeta().getLastModified().toInstant() : null,
                nullSafeInstantComparator()
            );
            default -> Comparator.comparing(GroupResource::getId, nullSafeStringComparator()); // Default to ID
        };
    }
    
    private Comparator<String> nullSafeStringComparator() {
        return Comparator.nullsLast(String::compareToIgnoreCase);
    }
    
    private Comparator<Boolean> nullSafeBooleanComparator() {
        return Comparator.nullsLast(Boolean::compareTo);
    }
    
    private Comparator<Instant> nullSafeInstantComparator() {
        return Comparator.nullsLast(Instant::compareTo);
    }
}