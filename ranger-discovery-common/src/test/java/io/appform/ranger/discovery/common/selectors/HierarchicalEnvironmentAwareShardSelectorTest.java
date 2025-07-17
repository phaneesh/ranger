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

package io.appform.ranger.discovery.common.selectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import io.appform.ranger.common.server.ShardInfo;
import io.appform.ranger.core.finder.serviceregistry.MapBasedServiceRegistry;
import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.model.Service;
import io.appform.ranger.core.model.ServiceNode;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

/**
 *
 */
class HierarchicalEnvironmentAwareShardSelectorTest {

    @Mock
    private MapBasedServiceRegistry<ShardInfo> serviceRegistry;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNoNodeAvailableForTheEnvironment() {
        val serviceName = UUID.randomUUID().toString();
        val service = Mockito.mock(Service.class);
        doReturn(serviceName).when(service).getServiceName();
        doReturn(service).when(serviceRegistry).getService();

        val serviceNodes = ArrayListMultimap.create();
        serviceNodes.put(
                ShardInfo.builder().environment("x.y").build(),
                new ServiceNode<>("host1",
                                  8888,
                                  ShardInfo.builder().environment("x.y").build(),
                                  HealthcheckStatus.healthy,
                                  System.currentTimeMillis(),
                                 "http"));

        serviceNodes.put(
                ShardInfo.builder().environment("x").build(),
                new ServiceNode<>("host2",
                                  8888,
                                  ShardInfo.builder().environment("x.y").build(),
                                  HealthcheckStatus.healthy,
                                  System.currentTimeMillis(),
                                 "http"));

        doReturn(serviceNodes).when(serviceRegistry).nodes();

        val nodes = selector("z").nodes(null, serviceRegistry);
        assertEquals(0, nodes.size());
    }

    @Test
    void testNodeAvailableForChildEnv() {
        val serviceName = UUID.randomUUID().toString();
        val service = Mockito.mock(Service.class);
        doReturn(serviceName).when(service).getServiceName();
        doReturn(service).when(serviceRegistry).getService();

        val serviceNodes = ArrayListMultimap.create();
        serviceNodes.put(
                ShardInfo.builder().environment("x.y").build(),
                new ServiceNode<>("host1",
                                  8888,
                                  ShardInfo.builder().environment("x.y").build(),
                                  HealthcheckStatus.healthy,
                                  System.currentTimeMillis(),
                                 "http"));
        serviceNodes.put(
                ShardInfo.builder().environment("x").build(),
                new ServiceNode<>("host2",
                                  8888,
                                  ShardInfo.builder().environment("x").build(),
                                  HealthcheckStatus.healthy,
                                  System.currentTimeMillis(),
                                 "http"));
        doReturn(serviceNodes).when(serviceRegistry).nodes();

        val nodes = selector("x.y")
                .nodes(null, serviceRegistry);
        assertEquals(1, nodes.size());
        assertEquals("host1", nodes.get(0).getHost());
        assertEquals(8888, nodes.get(0).getPort());
    }

    private HierarchicalEnvironmentAwareShardSelector selector(String environment) {
        return new HierarchicalEnvironmentAwareShardSelector(environment);
    }

    @Test
    void testNoNodeAvailableForChildEnvButAvailableForParentEnv() {
        val serviceName = UUID.randomUUID().toString();
        val service = Mockito.mock(Service.class);
        doReturn(serviceName).when(service).getServiceName();
        doReturn(service).when(serviceRegistry).getService();

        val serviceNodes = ArrayListMultimap.create();
        serviceNodes.put(
                ShardInfo.builder().environment("x.y.z").build(),
                new ServiceNode<>("host1",
                                  8888,
                                  ShardInfo.builder().environment("x.y").build(),
                                  HealthcheckStatus.healthy,
                                  System.currentTimeMillis(),
                                 "http"));
        serviceNodes.put(
                ShardInfo.builder().environment("x").build(),
                new ServiceNode<>("host2",
                                  9999,
                                  ShardInfo.builder().environment("x").build(),
                                  HealthcheckStatus.healthy,
                                  System.currentTimeMillis(),
                                 "http"));
        doReturn(serviceNodes).when(serviceRegistry).nodes();

        val nodes = selector("x.y").nodes(null, serviceRegistry);
        assertEquals(1, nodes.size());
        assertEquals("host2", nodes.get(0).getHost());
        assertEquals(9999, nodes.get(0).getPort());
    }

    @Test
    void testChildNodesAvailableForParentEnv() {
        val serviceName = UUID.randomUUID().toString();
        val service = Mockito.mock(Service.class);
        doReturn(serviceName).when(service).getServiceName();
        doReturn(service).when(serviceRegistry).getService();

        // service in env: x.y.z should be able to discover service in env: x
        ListMultimap<ShardInfo, ServiceNode<ShardInfo>> serviceNodes = ArrayListMultimap.create();
        serviceNodes.put(
                ShardInfo.builder().environment("x").build(),
                new ServiceNode<>("host1",
                        8888,
                        ShardInfo.builder().environment("x").build(),
                        HealthcheckStatus.healthy,
                        System.currentTimeMillis(),
                        "http"));
        doReturn(serviceNodes).when(serviceRegistry).nodes();

        List<ServiceNode<ShardInfo>> nodes = selector("x.y.z").nodes(null, serviceRegistry);
        assertEquals(1, nodes.size());
        assertEquals("host1", nodes.get(0).getHost());
        assertEquals(8888, nodes.get(0).getPort());

        // service in env: x.y.z should be able to discover service in env: x.y
        serviceNodes = ArrayListMultimap.create();
        serviceNodes.put(
                ShardInfo.builder().environment("x.y").build(),
                new ServiceNode<>("host2",
                        9999,
                        ShardInfo.builder().environment("x.y").build(),
                        HealthcheckStatus.healthy,
                        System.currentTimeMillis(),
                        "http"));
        doReturn(serviceNodes).when(serviceRegistry).nodes();

        nodes = selector("x.y.z").nodes(null, serviceRegistry);
        assertEquals(1, nodes.size());
        assertEquals("host2", nodes.get(0).getHost());
        assertEquals(9999, nodes.get(0).getPort());

        // service in env: x.y.z should be able to discover service in env: x
        serviceNodes = ArrayListMultimap.create();
        serviceNodes.put(
                ShardInfo.builder().environment("x").build(),
                new ServiceNode<>("host3",
                        9999,
                        ShardInfo.builder().environment("x").build(),
                        HealthcheckStatus.healthy,
                        System.currentTimeMillis(),
                        "http"));
        doReturn(serviceNodes).when(serviceRegistry).nodes();

        nodes = selector("x.y.z").nodes(null, serviceRegistry);
        assertEquals(1, nodes.size());
        assertEquals("host3", nodes.get(0).getHost());
        assertEquals(9999, nodes.get(0).getPort());

    }
}