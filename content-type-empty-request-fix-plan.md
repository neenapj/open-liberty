# Plan: Fix `set.content.type.for.empty.request` ignored when set via `Invocation.Builder.property()`

## Overview

`InvocationBuilderImpl.property()` stores properties in a nested map under the key `jaxrs.filter.properties`
inside the WebClient request context. `Headers.setProtocolHeadersInConnection()` reads `set.content.type.for.empty.request`
via `message.getContextualProperty()`, which only does a shallow merge and never unpacks the nested map.
As a result, the property is invisible to the transport layer and the Content-Type suppression never happens.

The fix (aligned with Apache CXF upstream reviewer Freeman's guidance) is to make `InvocationBuilderImpl.property()`
also write the property directly to the flat `contextProps` map — matching how `Client.property()` /
`WebTarget.property()` already behave. This change must be applied to both the EE7 (CXF 3.1.18) and EE8 (CXF 3.5.5)
Liberty-vendored copies of `InvocationBuilderImpl`.

---

## Sub-Task 1 — Fix `InvocationBuilderImpl.property()` in `com.ibm.ws.jaxrs.2.0.common` (EE7 / CXF 3.1.18)

**Intent:** Apply the dual-write fix to the EE7 bundle so that any property set via `Invocation.Builder.property()`
is stored both in the nested `filterProps` map (for JAX-RS filters) and at the top level of `contextProps`
(for the CXF transport layer).

**Expected Outcomes:**
- `builder.property("set.content.type.for.empty.request", "FALSE")` is now visible to `message.getContextualProperty()`
- `builder.property(name, null)` removes the property from both maps
- Existing filter-based consumers of `jaxrs.filter.properties` are unaffected (the nested map still exists)

**Todo List:**
1. In [`InvocationBuilderImpl.java`](dev/com.ibm.ws.jaxrs.2.0.common/src/org/apache/cxf/jaxrs/client/spec/InvocationBuilderImpl.java:277), in the `property()` method:
   - Add `contextProps.remove(name)` alongside `filterProps.remove(name)` in the `value == null` branch
   - Add `contextProps.put(name, value)` alongside `filterProps.put(name, value)` in the `else` branch
   - Mark the additions with `// Liberty change start` / `// Liberty change end` comments

**Relevant Context:**
- File: `dev/com.ibm.ws.jaxrs.2.0.common/src/org/apache/cxf/jaxrs/client/spec/InvocationBuilderImpl.java`
- Method: `property()` at line 270
- `PROPERTY_KEY = "jaxrs.filter.properties"` (line 53)
- The Liberty-specific proxy password `ProtectedString` conversion (lines 280–284) must be preserved unchanged
- The converted `value` (after ProtectedString wrapping) must be the one written to both maps

**Status:** [ ] pending — ready to implement in Agent mode

---

## Sub-Task 2 — Fix `InvocationBuilderImpl.property()` in `com.ibm.ws.org.apache.cxf.cxf.rt.rs.client.3.2` (EE8 / CXF 3.5.5)

**Intent:** Apply the identical dual-write fix to the EE8 bundle.

**Expected Outcomes:** Same as Sub-Task 1, for JAX-RS 2.1 / EE8 clients.

**Todo List:**
1. In [`InvocationBuilderImpl.java`](dev/com.ibm.ws.org.apache.cxf.cxf.rt.rs.client.3.2/src/org/apache/cxf/jaxrs/client/spec/InvocationBuilderImpl.java:298), in the `property()` method:
   - Add `contextProps.remove(name)` alongside `filterProps.remove(name)` in the `value == null` branch
   - Add `contextProps.put(name, value)` alongside `filterProps.put(name, value)` in the `else` branch
   - Mark the additions with `// Liberty change start` / `// Liberty change end` comments

**Relevant Context:**
- File: `dev/com.ibm.ws.org.apache.cxf.cxf.rt.rs.client.3.2/src/org/apache/cxf/jaxrs/client/spec/InvocationBuilderImpl.java`
- Method: `property()` at line 291
- Structure is identical to EE7 file; same Liberty proxy password change at lines 301–305

**Status:** [ ] pending — ready to implement in Agent mode

---

## Sub-Task 3 — Regression tests in `com.ibm.ws.jaxrs.2.x_fat_clientProps`

**Intent:** Verify regression test coverage for the fix.

**Expected Outcomes:** Tests confirm `set.content.type.for.empty.request` works when set via `Invocation.Builder.property()`.

**Status:** [x] done — already present on this branch:
- `Resource.java`: new `DELETE /contentTypeCheck` endpoint (returns `"none"` or Content-Type value)
- `CxfClientPropsTestServlet.java`: 7 new tests (lines 538–674) including exact z/OS Connect reproducer
  (`testSetContentTypeForEmptyRequest_DELETE_propertyFalse_onInvocationBuilder`) and `builder.method("DELETE", String.class)` pattern

**Note:** The transport-layer fallback fix (`getSetEmptyRequestCtProperty()`) is also already applied to both
`Headers.java` files on this branch. Sub-Tasks 1 and 2 add Freeman's upstream-recommended `InvocationBuilderImpl`
dual-write on top of that existing fix.
