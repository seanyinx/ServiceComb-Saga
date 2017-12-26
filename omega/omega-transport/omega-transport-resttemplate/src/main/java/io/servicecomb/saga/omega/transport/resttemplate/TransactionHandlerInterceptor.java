/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package io.servicecomb.saga.omega.transport.resttemplate;

import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.GLOBAL_TX_ID_KEY;
import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.LOCAL_TX_ID_KEY;

import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.servicecomb.saga.omega.context.OmegaContext;

public class TransactionHandlerInterceptor implements HandlerInterceptor {

  private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final OmegaContext omegaContext;

  public TransactionHandlerInterceptor(OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String globalTxId = request.getHeader(GLOBAL_TX_ID_KEY);
    if (globalTxId == null) {
      LOG.info("no such header: {}", GLOBAL_TX_ID_KEY);
    } else {
      omegaContext.setGlobalTxId(globalTxId);
    }
    String parentTxId = request.getHeader(LOCAL_TX_ID_KEY);
    if (parentTxId == null) {
      LOG.info("no such header: {}", LOCAL_TX_ID_KEY);
    } else {
      omegaContext.setParentTxId(parentTxId);
    }
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o,
      ModelAndView modelAndView) {
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o,
      Exception e) {
  }
}
