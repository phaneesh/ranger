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
package io.appform.ranger.core.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HubConstants {
    public static final int SERVICE_REFRESH_TIMEOUT_MS = 10_000;
    public static final int HUB_START_TIMEOUT_MS = 30_000;
    public static final long REFRESH_FREQUENCY_MS = 10_000;
    public static final int CONNECTION_RETRY_TIME_MS = 5_000;
    public static final int MINIMUM_REFRESH_TIME_MS = 5_000;
    public static final int MINIMUM_SERVICE_REFRESH_TIMEOUT_MS = 1_000;
    public static final int MINIMUM_HUB_START_TIMEOUT_MS = 5_000;
}
