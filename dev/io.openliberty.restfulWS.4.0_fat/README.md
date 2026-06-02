# Jakarta REST 4.0 Examples FAT Test

This FAT (Feature Acceptance Test) project tests Jakarta REST 4.0 features with working code examples.

## Test Coverage

### 1. Basic REST Resource (ProductResource)
- **Fixed Issues from Original Code:**
  - Added no-arg constructor to Product class for JSON deserialization
  - Used thread-safe `CopyOnWriteArrayList` instead of regular ArrayList
  - Fixed PUT method to preserve ID from path parameter
  
- **Tests:**
  - CRUD operations (Create, Read, Update, Delete)
  - JSON serialization/deserialization
  - HTTP status codes (200, 201, 204, 404)

### 2. getMatchedResourceTemplate (UserResource) - NEW in REST 4.0
- **Feature:** `UriInfo.getMatchedResourceTemplate()`
- **Tests:**
  - Retrieves the URI template pattern with path parameters
  - Validates template contains `{userId}` and `{orderId}` placeholders
  - Useful for logging, metrics, and routing decisions

### 3. JSON Merge Patch (CustomerResource) - NEW in REST 4.0
- **Feature:** RFC 7396 JSON Merge Patch support
- **Tests:**
  - Partial updates using `application/merge-patch+json` content type
  - Validates only specified fields are updated
  - Tests error handling for invalid patches

### 4. Multipart Form Data
- **Status:** NOT INCLUDED
- **Reason:** Original example used Jersey-specific APIs incompatible with Open Liberty's RESTEasy implementation
- **Note:** Would require RESTEasy multipart provider for proper implementation

## Project Structure

```
io.openliberty.restfulWS.4.0.examples_fat/
├── bnd.bnd                                    # Build configuration
├── fat/src/                                   # FAT test classes
│   └── io/openliberty/restfulWS40/examples/fat/
│       ├── FATSuite.java                      # Test suite
│       └── Rest40ExamplesTest.java            # Main test class
├── test-applications/rest40examples/src/      # Application code
│   └── io/openliberty/restfulWS40/examples/fat/rest40examples/
│       ├── Customer.java                      # Model class
│       ├── CustomerResource.java              # JSON Merge Patch example
│       ├── Order.java                         # Model class
│       ├── Product.java                       # Model class
│       ├── ProductResource.java               # Basic CRUD example
│       ├── Rest40ExamplesApplication.java     # JAX-RS Application
│       ├── Rest40ExamplesTestServlet.java     # Test servlet
│       └── UserResource.java                  # getMatchedResourceTemplate example
└── publish/servers/                           # Server configuration
    └── io.openliberty.restfulWS.4.0.examples.fat/
        └── server.xml                         # Liberty server config
```

## Running the Tests

```bash
# From the Open Liberty dev directory
./gradlew :io.openliberty.restfulWS.4.0.examples_fat:buildandrun
```

## Features Tested

- `restfulWS-4.0` - Jakarta REST 4.0
- `jsonb-3.0` - JSON Binding 3.0
- `jsonp-2.1` - JSON Processing 2.1
- `servlet-6.1` - Jakarta Servlet 6.1

## Code Quality Improvements

The examples in this test fix several issues from the original code:

1. **Thread Safety:** Used concurrent collections for shared state
2. **Proper Constructors:** Added no-arg constructors for JSON frameworks
3. **ID Preservation:** Fixed PUT method to maintain resource IDs
4. **Error Handling:** Proper 404 responses for missing resources
5. **Resource Cleanup:** Added helper methods for test cleanup
