/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

public class RequiredParameterNotSetException extends MuleRuntimeException {

  private final ParameterModel parameterModel;

  public RequiredParameterNotSetException(ParameterModel parameterModel) {
    super(createStaticMessage(format("Parameter '%s' is required but was not found", hyphenize(parameterModel.getName()))));
    this.parameterModel = parameterModel;
  }

  public ParameterModel getParameterModel() {
    return parameterModel;
  }
}
