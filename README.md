# SCIM2 Server

A complete SCIM 2.0 server implementation using Spring Boot and UnboundID SCIM2 SDK with JSON file backend.

## Features

- **Complete SCIM 2.0 Implementation**: Supports Users, Groups, and Enterprise User extensions
- **REST API Operations**: Create, Read, Update, Delete, Search, and Bulk operations
- **JSON File Backend**: Persistent storage using JSON files
- **Authentication**: Bearer token authentication (hardcoded for demo purposes)
- **Discovery Endpoints**: ServiceProviderConfig, ResourceTypes, and Schemas
- **Swagger Documentation**: Interactive API documentation
- **Comprehensive Testing**: Unit and integration tests
- **Error Handling**: Proper SCIM 2.0 error responses

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Server

1. Clone the repository
2. Navigate to the project directory
3. Run the server:

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

### API Documentation

Access the Swagger UI at: `http://localhost:8080/swagger-ui.html`

## Authentication

All SCIM endpoints require Bearer token authentication. Use the following token:

```
Authorization: Bearer scim-token-123
```

## API Endpoints

### Users

- `GET /scim/v2/Users` - List all users
- `GET /scim/v2/Users/{id}` - Get user by ID
- `POST /scim/v2/Users` - Create user
- `PUT /scim/v2/Users/{id}` - Update user
- `DELETE /scim/v2/Users/{id}` - Delete user

### Groups

- `GET /scim/v2/Groups` - List all groups
- `GET /scim/v2/Groups/{id}` - Get group by ID
- `POST /scim/v2/Groups` - Create group
- `PUT /scim/v2/Groups/{id}` - Update group
- `DELETE /scim/v2/Groups/{id}` - Delete group

### Bulk Operations

- `POST /scim/v2/Bulk` - Perform bulk operations

### Discovery

- `GET /scim/v2/ServiceProviderConfig` - Service provider configuration
- `GET /scim/v2/ResourceTypes` - Supported resource types
- `GET /scim/v2/Schemas` - Supported schemas

## Examples

### Create a User

```bash
curl -X POST "http://localhost:8080/scim/v2/Users" \
  -H "Authorization: Bearer scim-token-123" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "john.doe",
    "name": {
      "givenName": "John",
      "familyName": "Doe",
      "formatted": "John Doe"
    },
    "emails": [{
      "value": "john.doe@example.com",
      "type": "work",
      "primary": true
    }],
    "active": true
  }'
```

### Create a Group

```bash
curl -X POST "http://localhost:8080/scim/v2/Groups" \
  -H "Authorization: Bearer scim-token-123" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
    "displayName": "Developers",
    "members": []
  }'
```

### Search Users

```bash
curl "http://localhost:8080/scim/v2/Users?filter=userName eq \"john.doe\"" \
  -H "Authorization: Bearer scim-token-123" \
  -H "Content-Type: application/scim+json"
```

### Bulk Operations

```bash
curl -X POST "http://localhost:8080/scim/v2/Bulk" \
  -H "Authorization: Bearer scim-token-123" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkRequest"],
    "Operations": [{
      "method": "POST",
      "path": "/Users",
      "bulkId": "user1",
      "data": {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": "bulk.user",
        "name": {
          "givenName": "Bulk",
          "familyName": "User"
        },
        "active": true
      }
    }]
  }'
```

## Data Storage

The server uses JSON files for data persistence:

- `data/users.json` - User data
- `data/groups.json` - Group data

These files are created automatically in the project directory when the server starts.

## Enterprise User Extensions

The server supports the Enterprise User extension with the following attributes:

- `employeeNumber`
- `costCenter`
- `organization`
- `division`
- `department`
- `manager`

Example with Enterprise User extension:

```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
  ],
  "userName": "enterprise.user",
  "name": {
    "givenName": "Enterprise",
    "familyName": "User"
  },
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
    "employeeNumber": "12345",
    "department": "Engineering",
    "organization": "ACME Corp"
  }
}
```

## Testing

Run the tests:

```bash
mvn test
```

The test suite includes:
- Unit tests for controllers
- Integration tests for full API workflows
- Authentication tests
- Error handling tests

## Configuration

The server can be configured through `application.properties`:

- `server.port` - Server port (default: 8080)
- `logging.level.com.scim2.server` - Logging level for SCIM server
- `springdoc.swagger-ui.enabled` - Enable/disable Swagger UI

## SCIM 2.0 Compliance

This implementation follows the SCIM 2.0 specification (RFC 7644) and includes:

- Proper HTTP status codes
- SCIM-compliant error responses
- Resource versioning with ETags
- Pagination support
- Filtering support (basic implementation)
- Standard SCIM schemas

## Security Notes

**This is a demo implementation with hardcoded authentication. For production use:**

1. Implement proper OAuth 2.0 or similar authentication
2. Use a proper database instead of JSON files
3. Add input validation and sanitization
4. Implement rate limiting
5. Add audit logging
6. Use HTTPS in production

## License

MIT License - see LICENSE file for details.# scim2-server
