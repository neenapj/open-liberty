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
package com.ibm.ws.diagnostics.summary.internal;

import java.util.Map;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ResourceInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;

/**
 * Callback that logs FFDC for exceptions caught by the JAX-RS DefaultExceptionMapper.
 * This ensures that exceptions in REST endpoints are captured by the diagnostic summary feature.
 */
@Component(
    service = { DefaultExceptionMapperCallback.class },
    configurationPolicy = ConfigurationPolicy.IGNORE,
    property = { "service.vendor=IBM" }
)
public class DiagnosticSummaryExceptionCallback implements DefaultExceptionMapperCallback {

    private static final Logger logger = Logger.getLogger(DiagnosticSummaryExceptionCallback.class.getName());

    @Override
    public Map<String, Object> onDefaultMappedException(Throwable throwable, int statusCode, ResourceInfo resourceInfo) {
        try {
            // Log FFDC for the exception to trigger diagnostic summary processing
            String sourceClass = resourceInfo != null && resourceInfo.getResourceClass() != null
                ? resourceInfo.getResourceClass().getName()
                : "UnknownResource";

            String sourceMethod = resourceInfo != null && resourceInfo.getResourceMethod() != null
                ? resourceInfo.getResourceMethod().getName()
                : "unknownMethod";

            logger.info("=== DiagnosticSummaryExceptionCallback triggered ===");
            logger.info("  Exception: " + throwable.getClass().getName());
            logger.info("  Message: " + throwable.getMessage());
            logger.info("  Status Code: " + statusCode);
            logger.info("  Resource: " + sourceClass + "." + sourceMethod);
            logger.info("  Logging FFDC...");

            // Log FFDC - this will trigger our IncidentForwarder
            FFDCFilter.processException(
                throwable,
                sourceClass,
                sourceMethod,
                new Object[] { statusCode, resourceInfo }
            );

            logger.info("  FFDC logged successfully");

        } catch (Exception e) {
            logger.severe("Error in DiagnosticSummaryExceptionCallback: " + e.getMessage());
            e.printStackTrace();
        }

        // Return null - we don't need to add any headers
        return null;
    }
}

// Made with Bob
