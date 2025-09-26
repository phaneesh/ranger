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

package io.appform.ranger.common.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;


class ShardInfoTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String getResource(String path) {
        val data = ShardInfoTest.class.getClassLoader().getResourceAsStream(path);
        if(null == data) return null;
        return new BufferedReader(
                new InputStreamReader(data))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    @SneakyThrows
    @SuppressWarnings("SameParameterValue")
    private  <T> T getResource(String path, Class<T> klass) {
        val data = getResource(path);
        if(null == data) return null;
        return mapper.readValue(data, klass);
    }

    @Test
    void testShardInfo(){
        val shardInfo1 = getResource("fixtures/env1.json", ShardInfo.class);
        val shardInfo2 = getResource("fixtures/env2.json", ShardInfo.class);
        Assertions.assertNotNull(shardInfo1);
        Assertions.assertNotNull(shardInfo2);
        Assertions.assertNotEquals(shardInfo1, shardInfo2);
        Arrays.asList(shardInfo1, shardInfo2).forEach(shardInfo -> Assertions.assertEquals("e", shardInfo.getEnvironment()));
        Assertions.assertEquals("r", shardInfo1.getRegion());
        Assertions.assertNull(shardInfo2.getRegion());
        Assertions.assertNotNull(shardInfo1.getTags());
        Assertions.assertNotNull(shardInfo2.getTags());
        Assertions.assertTrue(shardInfo2.getTags().contains("tag1"));
    }
}
