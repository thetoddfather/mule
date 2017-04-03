/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import java.io.InputStream;

import javax.wsdl.WSDLException;

/**
 * A strategy to retrieve the wsdl from the url defined
 */
public interface WsdlRetrieverStrategy 
{
	InputStream retrieveWsdlResource(String url) throws WSDLException;	
}
