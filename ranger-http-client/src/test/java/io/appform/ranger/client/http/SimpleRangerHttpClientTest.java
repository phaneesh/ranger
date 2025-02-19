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
package io.appform.ranger.client.http;

import io.appform.ranger.core.units.TestNodeData;
import io.appform.ranger.core.utils.RangerTestUtils;
import io.appform.ranger.http.utils.RangerHttpUtils;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleRangerHttpClientTest extends BaseRangerHttpClientTest{

    @Test
    void testSimpleHttpRangerClient(){
        val httpClientConfig = getHttpClientConfig();
        val client = SimpleRangerHttpClient.<TestNodeData>builder()
                .clientConfig(httpClientConfig)
                .mapper(getObjectMapper())
                .httpClient(RangerHttpUtils.httpClient(httpClientConfig, getObjectMapper()))
                .deserializer(this::read)
                .namespace("test-n")
                .serviceName("test-s")
                .nodeRefreshIntervalMs(1000)
                .build();
        client.start();
        RangerTestUtils.sleepUntilFinderStarts(client.getServiceFinder());
        Assertions.assertNotNull(client.getNode().orElse(null));
        Assertions.assertFalse(client.getAllNodes().isEmpty());
        Assertions.assertNotNull(client.getNode(nodeData -> nodeData.getShardId() == 1).orElse(null));
        Assertions.assertFalse(client.getAllNodes(nodeData -> nodeData.getShardId() == 1).isEmpty());
        client.stop();
    }
}
