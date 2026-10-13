/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jacksonjsr310jakarta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * Application-packaged Jakarta REST Jackson provider for restfulWS-3.1.
 *
 * <p>This class is the Jakarta-namespace equivalent of the
 * {@code javax.ws.rs}-based {@code FixedJacksonProvider} used by the
 * {@code jaxrs-2.1} test. It uses:
 * <ul>
 *   <li>{@code com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider} —
 *       the Jakarta EE 9+ provider from
 *       {@code com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider}</li>
 *   <li>{@code JavaTimeModule} registered via reflection so that
 *       {@code jackson-datatype-jsr310} only needs to be on the runtime classpath
 *       (in {@code WEB-INF/lib}), not the compile-time buildpath of the FAT project.</li>
 * </ul>
 *
 * <p>Key assertion: because this provider is application-registered and the
 * container JSON-B provider ({@code io.openliberty.restfulWS30.jsonb20provider.JsonBProvider})
 * is a built-in, RESTEasy's provider-factory sorts this provider ahead of the
 * container provider for {@code application/json}. The container JSON-B provider
 * therefore never fires for JSON content while this provider is active.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JakartaJacksonProvider implements MessageBodyReader<Object>,
                                               MessageBodyWriter<Object> {

    static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Load JavaTimeModule reflectively: only needs to be present at runtime
        // in WEB-INF/lib (added by the FAT setUp via addAsLibraries).
        try {
            Class<?> moduleClass = Class.forName(
                    "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            Module javaTimeModule = (Module) moduleClass.getDeclaredConstructor().newInstance();
            mapper.registerModule(javaTimeModule);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load JavaTimeModule — ensure jackson-datatype-jsr310 "
                    + "is in WEB-INF/lib", e);
        }
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private final JacksonJsonProvider delegate;

    public JakartaJacksonProvider() {
        delegate = new JacksonJsonProvider();
        delegate.setMapper(MAPPER);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        return delegate.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType,
                           Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream)
            throws IOException, WebApplicationException {
        return delegate.readFrom(type, genericType, annotations, mediaType,
                                 httpHeaders, entityStream);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return delegate.isWriteable(type, genericType, annotations, mediaType);
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType,
                        Annotation[] annotations, MediaType mediaType) {
        return delegate.getSize(t, type, genericType, annotations, mediaType);
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType,
                        Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream)
            throws IOException, WebApplicationException {
        delegate.writeTo(t, type, genericType, annotations, mediaType,
                         httpHeaders, entityStream);
    }
}
