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
package io.appform.ranger.core.finder;

import io.appform.ranger.core.finder.shardselector.ListShardSelector;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.ServiceNodeSelector;
import io.appform.ranger.core.units.TestNodeData;
import io.appform.ranger.core.utils.RangerTestUtils;
import io.appform.ranger.core.utils.RegistryTestUtils;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class UnshardedClusterFinderTest {

    static class TestUnshardedNodeSelector implements ServiceNodeSelector<TestNodeData> {

        @Override
        public ServiceNode<TestNodeData> select(List<ServiceNode<TestNodeData>> serviceNodes) {
            return serviceNodes.isEmpty() ? null : serviceNodes.get(0);
        }
    }

    @Test
    void unshardedClusterFinder() {
        val unshardedRegistry = RegistryTestUtils.getUnshardedRegistry();
        val shardSelector = new ListShardSelector<TestNodeData>();
        val simpleUnshardedServiceFinder = new SimpleUnshardedServiceFinder<>(
                unshardedRegistry,
                shardSelector,
                new TestUnshardedNodeSelector()
        );
        val serviceNode = simpleUnshardedServiceFinder.get(RangerTestUtils.getCriteria(1));
        Assertions.assertTrue(serviceNode.isPresent());
        Assertions.assertEquals("localhost-1", serviceNode.get().getHost());
    }
}
