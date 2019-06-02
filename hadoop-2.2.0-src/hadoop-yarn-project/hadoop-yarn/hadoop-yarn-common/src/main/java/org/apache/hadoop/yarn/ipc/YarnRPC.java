/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.yarn.ipc;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.security.token.SecretManager;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;

/**
 * Abstraction to get the RPC implementation for Yarn.
 *
 * YARNA对外提供的类，基于这个类，用户可以构建一个基于Hadoop RPC且采用Protocol Buffers序列化框架的通信协议
 * 1. 这是一个抽象类，实际实现由参数 yarn.ipc.rpc.class指定，默认为：org.apache.hadoop.yarn.ipc.HadoopYarnProtoRPC
 * 2. HadoopYarnProtoRPC通过RPC工厂生成器RpcFactoryProvider分别生成客户端工厂、服务端工厂
 *
 */
@InterfaceAudience.LimitedPrivate({ "MapReduce", "YARN" })
public abstract class YarnRPC {
  private static final Log LOG = LogFactory.getLog(YarnRPC.class);
  
  public abstract Object getProxy(Class protocol, InetSocketAddress addr,
      Configuration conf);

  public abstract void stopProxy(Object proxy, Configuration conf);

  public abstract Server getServer(Class protocol, Object instance,
      InetSocketAddress addr, Configuration conf,
      SecretManager<? extends TokenIdentifier> secretManager,
      int numHandlers, String portRangeConfig);

  public Server getServer(Class protocol, Object instance,
      InetSocketAddress addr, Configuration conf,
      SecretManager<? extends TokenIdentifier> secretManager,
      int numHandlers) {
    return getServer(protocol, instance, addr, conf, secretManager, numHandlers,
        null);
  }
  
  public static YarnRPC create(Configuration conf) {
    LOG.debug("Creating YarnRPC for " + 
        conf.get(YarnConfiguration.IPC_RPC_IMPL));
    String clazzName = conf.get(YarnConfiguration.IPC_RPC_IMPL);
    if (clazzName == null) {
      clazzName = YarnConfiguration.DEFAULT_IPC_RPC_IMPL;
    }
    try {
      return (YarnRPC) Class.forName(clazzName).newInstance();
    } catch (Exception e) {
      throw new YarnRuntimeException(e);
    }
  }

}
