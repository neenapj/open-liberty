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
package com.ibm.ws.jaxrs.fat.jacksonjsr310;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Correctly-configured provider: JavaTimeModule is registered on the
 * ObjectMapper at static initialisation time, before any thread can invoke
 * readFrom() or writeTo(). This is the fix pattern.
 *
 * JavaTimeModule is loaded reflectively to avoid a compile-time dependency on
 * jackson-datatype-jsr310 in the FAT project bnd buildpath. The jar is present
 * in WEB-INF/lib at runtime (added by the FAT setUp via addAsLibraries), which
 * is exactly the customer's deployment model.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FixedJacksonProvider implements javax.ws.rs.ext.MessageBodyReader<Object>,
                                             javax.ws.rs.ext.MessageBodyWriter<Object> {

    /**
     * Shared ObjectMapper with JavaTimeModule registered at construction time.
     * No race condition is possible because module registration completes
     * during class loading, before any request thread reaches readFrom().
     */
    static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Load JavaTimeModule reflectively so that jackson-datatype-jsr310 only
        // needs to be on the runtime classpath (WEB-INF/lib), not the compile path.
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

    private final JacksonJaxbJsonProvider delegate;

    public FixedJacksonProvider() {
        delegate = new JacksonJaxbJsonProvider();
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
