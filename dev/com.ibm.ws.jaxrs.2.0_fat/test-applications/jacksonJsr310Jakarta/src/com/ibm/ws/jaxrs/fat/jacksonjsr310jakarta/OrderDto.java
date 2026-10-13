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

import java.time.LocalDate;

/**
 * Simple DTO with a {@code java.time.LocalDate} property used to verify that an
 * application-packaged Jackson jakarta.ws.rs provider handles date types
 * correctly and takes precedence over the container JSON-B provider under
 * restfulWS-3.1.
 */
public class OrderDto {

    private String id;
    private LocalDate orderDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }
}
