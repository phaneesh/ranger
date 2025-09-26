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
package io.appform.ranger.zookeeper.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.appform.ranger.core.finder.serviceregistry.MapBasedServiceRegistry;
import io.appform.ranger.core.healthcheck.Healthchecks;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.ShardSelector;
import io.appform.ranger.core.serviceprovider.ServiceProvider;
import io.appform.ranger.zookeeper.ServiceFinderBuilders;
import io.appform.ranger.zookeeper.ServiceProviderBuilders;
import io.appform.ranger.zookeeper.serde.ZkNodeDataSerializer;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.curator.test.TestingCluster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


@Slf4j
class CustomShardSelectorTest {
    private TestingCluster testingCluster;
    private ObjectMapper objectMapper;
    private final List<ServiceProvider<TestShardInfo, ZkNodeDataSerializer<TestShardInfo>>> serviceProviders = Lists.newArrayList();

    @BeforeEach
    public void startTestCluster() throws Exception {
        objectMapper = new ObjectMapper();
        testingCluster = new TestingCluster(3);
        testingCluster.start();
        registerService("localhost-1", 9000, 1, 2);
        registerService("localhost-2", 9001, 1, 3);
        registerService("localhost-3", 9002, 2, 3);
    }

    @AfterEach
    public void stopTestCluster() throws Exception {
        serviceProviders.forEach(ServiceProvider::stop);
        if (null != testingCluster) {
            testingCluster.close();
        }
    }

    @Value
    @Builder
    @Jacksonized
    private static class TestShardInfo {
        int a;
        int b;

        private static Predicate<TestShardInfo> getCriteria(int a, int b) {
            return nodeData -> nodeData.getA() == a && nodeData.getB() == b;
        }
    }

    private static final class TestShardSelector implements ShardSelector<TestShardInfo, MapBasedServiceRegistry<TestShardInfo>> {

        @Override
        public List<ServiceNode<TestShardInfo>> nodes(Predicate<TestShardInfo> criteria, MapBasedServiceRegistry<TestShardInfo> serviceRegistry) {
            val nodes = new ArrayList<ServiceNode<TestShardInfo>>();
            serviceRegistry.nodes().entries().forEach(entry -> {
                val shardInfo = entry.getKey();
                if (criteria.test(shardInfo)) {
                    nodes.add(entry.getValue());
                }
            });
            return nodes;
        }
    }

    @Test
    void testBasicDiscovery() {
        val serviceFinder = ServiceFinderBuilders.<TestShardInfo>shardedFinderBuilder()
                .withConnectionString(testingCluster.getConnectString())
                .withNamespace("test")
                .withServiceName("test-service")
                .withShardSelector(new TestShardSelector())
                .withDeserializer(data -> {
                    try {
                        return objectMapper.readValue(data,
                                new TypeReference<ServiceNode<TestShardInfo>>() {});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .build();
        serviceFinder.start();
        {
            val node = serviceFinder.get(TestShardInfo.getCriteria(1, 10)).orElse(null);
            Assertions.assertNull(node);
        }
        {
            val node = serviceFinder.get(TestShardInfo.getCriteria(1, 2)).orElse(null);
            Assertions.assertNotNull(node);
            Assertions.assertEquals(new TestShardInfo(1, 2), node.getNodeData());
        }
        {
            val node = serviceFinder.get(TestShardInfo.getCriteria(2, 3)).orElse(null);
            Assertions.assertNotNull(node);
        }
        serviceFinder.stop();
    }

    private void registerService(String host, int port, int a, int b) {
        val serviceProvider = ServiceProviderBuilders.<TestShardInfo>shardedServiceProviderBuilder()
                .withConnectionString(testingCluster.getConnectString())
                .withNamespace("test")
                .withServiceName("test-service")
                .withSerializer(data -> {
                    try {
                        return objectMapper.writeValueAsBytes(data);
                    }
                    catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .withHostname(host)
                .withPort(port)
                .withNodeData(new TestShardInfo(a, b))
                .withHealthcheck(Healthchecks.defaultHealthyCheck())
                .withHealthUpdateIntervalMs(1000)
                .build();
        serviceProvider.start();
        serviceProviders.add(serviceProvider);
    }
}