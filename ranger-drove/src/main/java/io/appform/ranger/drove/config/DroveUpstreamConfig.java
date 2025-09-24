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
package io.appform.ranger.drove.config;

import io.dropwizard.util.Duration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 *
 */
@Value
@Builder
@Jacksonized
public class DroveUpstreamConfig {
    public static final String DEFAULT_DISCOVERY_TAG_NAME = "ranger.name";
    public static final String DEFAULT_ENVIRONMENT_TAG_NAME = "ranger.environment";
    public static final String DEFAULT_REGION_TAG_NAME = "ranger.region";
    public static final String DEFAULT_SKIP_TAG_NAME = "ranger.hidden";
    public static final Duration DEFAULT_CHECK_INTERVAL = Duration.seconds(10);
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.seconds(5);
    public static final Duration DEFAULT_OPERATION_TIMEOUT = Duration.seconds(5);
    public static final Duration DEFAULT_EVENT_POLLING_INTERVAL = Duration.seconds(5);

    @Valid
    @NotEmpty
    List<String> endpoints;

    Duration checkInterval;

    Duration connectionTimeout;
    Duration operationTimeout;

    Duration eventPollingInterval;

    boolean insecure;

    String authHeader;
    String username;
    String password;

    String discoveryTagName;
    String environmentTagName;
    String regionTagName;
    String skipTagName;

    String defaultEnvironment;
    String defaultRegion;

    boolean skipCaching;
}
