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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Reproduces the customer bug: the ObjectMapper is created WITHOUT registering
 * JavaTimeModule. When the first deserialization of a bean with a LocalDate
 * property occurs, BeanDeserializerFactory._findUnsupportedTypeDeserializer()
 * installs an UnsupportedTypeDeserializer in the DeserializerCache. All
 * subsequent requests fail with InvalidDefinitionException even though the
 * jackson-datatype-jsr310 jar is on the classpath.
 *
 * This class deliberately mirrors the customer pattern: the ObjectMapper is
 * a static field (simulating a provider singleton) and JavaTimeModule is
 * never registered on it.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BrokenJacksonProvider implements javax.ws.rs.ext.MessageBodyReader<Object>,
                                              javax.ws.rs.ext.MessageBodyWriter<Object> {

    /**
     * Shared ObjectMapper with NO JavaTimeModule registered.
     * jackson-datatype-jsr310 is on the classpath but ignored — exactly
     * like the customer's misconfigured application.
     */
    static final ObjectMapper MAPPER = new ObjectMapper();
    // NOTE: new ObjectMapper() does NOT auto-register modules; JavaTimeModule
    // must be added explicitly. The jsr310 jar being present is not enough.

    private final JacksonJaxbJsonProvider delegate;

    public BrokenJacksonProvider() {
        delegate = new JacksonJaxbJsonProvider();
        delegate.setMapper(MAPPER);   // inject the module-less mapper
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
