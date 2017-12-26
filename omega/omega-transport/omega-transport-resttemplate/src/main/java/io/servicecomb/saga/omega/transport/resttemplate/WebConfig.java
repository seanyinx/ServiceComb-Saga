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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.MessageSerializer;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

  private MessageSender sender;

  private MessageSerializer serializer;

  @Autowired
  public WebConfig(MessageSender sender, MessageSerializer serializer) {
    this.sender = sender;
    this.serializer = serializer;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new TransactionHandlerInterceptor(sender, serializer));
  }
}
