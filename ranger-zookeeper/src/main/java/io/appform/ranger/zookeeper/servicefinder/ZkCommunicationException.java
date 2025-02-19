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

package io.appform.ranger.zookeeper.servicefinder;

import io.appform.ranger.core.exceptions.CommunicationException;

/**
 * Thrown in case there is an issue communicating with the Zookeeper upstream.

 */
public class ZkCommunicationException extends CommunicationException {
    public ZkCommunicationException(final String message) {
        super(message);
    }
}
