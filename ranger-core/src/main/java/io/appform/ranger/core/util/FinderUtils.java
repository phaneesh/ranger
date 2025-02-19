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
package io.appform.ranger.core.util;

import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.model.Service;
import io.appform.ranger.core.model.ServiceNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
@Slf4j
@UtilityClass
public class FinderUtils {

    public static Set<Service> getEligibleServices(final Collection<Service> services,
                                                   final Set<String> excludedServices) {
        return services.stream()
                .filter(service -> !excludedServices.contains(service.getServiceName()))
                .collect(Collectors.toSet());
    }


    public static<T> List<ServiceNode<T>> filterValidNodes(
            final Service service,
            final Collection<ServiceNode<T>> serviceNodes,
            long healthcheckZombieCheckThresholdTime) {
        return serviceNodes.stream()
                .filter(serviceNode -> isValidNode(service, healthcheckZombieCheckThresholdTime, serviceNode))
                .toList();
    }

    public static <T> boolean isValidNode(
            Service service,
            long healthcheckZombieCheckThresholdTime,
            ServiceNode<T> serviceNode) {
        if(null == serviceNode) return false;

        if(HealthcheckStatus.healthy != serviceNode.getHealthcheckStatus()) {
            log.debug("Unhealthy node [{}:{}] found for [{}]",
                      serviceNode.getHost(), serviceNode.getPort(), service.getServiceName());
            return false;
        }
        if(serviceNode.getLastUpdatedTimeStamp() < healthcheckZombieCheckThresholdTime) {
            log.warn("Zombie node [{}:{}] found for [{}]",
                      serviceNode.getHost(), serviceNode.getPort(), service.getServiceName());
            return false;
        }
        return true;
    }
}
