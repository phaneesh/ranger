/*
 * Copyright 2024 Authors, Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.ranger.hub.server.bundle.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.util.List;

@Slf4j
public class RangerHealthCheck extends HealthCheck {

  private final List<CuratorFramework> curatorFrameworks;

  public RangerHealthCheck(final List<CuratorFramework> curatorFrameworks){
    this.curatorFrameworks = curatorFrameworks;
  }

  @Override
  protected Result check() {
    return curatorFrameworks.stream()
            .allMatch(curatorFramework -> curatorFramework.getZookeeperClient().isConnected())
            ? Result.healthy("Service is healthy")
            : Result.unhealthy("Can't connect to zookeeper");
  }
}
