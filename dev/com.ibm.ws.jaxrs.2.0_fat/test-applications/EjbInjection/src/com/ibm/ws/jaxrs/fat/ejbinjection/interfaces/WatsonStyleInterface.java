/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection.interfaces;

/**
 * @Local interface that declares only the parameterised method.
 * The no-arg getInfo() is intentionally absent — mirroring WatsonV01ApiRsBeanInterface
 * which only declared inviaMessaggioChatbot(MessageInputDTO) and not getInfo().
 */
public interface WatsonStyleInterface {

    public String writeMessage(String input);

}
