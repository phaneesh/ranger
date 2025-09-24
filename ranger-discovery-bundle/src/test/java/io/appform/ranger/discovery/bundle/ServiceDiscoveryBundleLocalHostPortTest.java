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

package io.appform.ranger.discovery.bundle;

import com.alibaba.dcm.DnsCacheManipulator;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.ranger.discovery.bundle.util.ConfigurationUtils;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.AdminEnvironment;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import static io.appform.ranger.discovery.bundle.Constants.LOCAL_ADDRESSES;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Slf4j
class ServiceDiscoveryBundleLocalHostPortTest {

    private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final LifecycleEnvironment lifecycleEnvironment = new LifecycleEnvironment(metricRegistry);
    private final Environment environment = mock(Environment.class);
    private final Bootstrap<?> bootstrap = mock(Bootstrap.class);
    private final Configuration configuration = mock(Configuration.class);

    private final ServiceDiscoveryBundle<Configuration> bundle = new ServiceDiscoveryBundle<Configuration>() {
        @Override
        protected ServiceDiscoveryConfiguration getRangerConfiguration(Configuration configuration) {
            return serviceDiscoveryConfiguration;
        }

        @Override
        protected String getServiceName(Configuration configuration) {
            return "TestService";
        }


    };

    @AfterEach
    void afterMethod() {
        DnsCacheManipulator.clearDnsCache();
    }

    private ServiceDiscoveryConfiguration serviceDiscoveryConfiguration;


    @Test
    void shouldFailLocalhostPublish() {
        DnsCacheManipulator.setDnsCache("myzookeeper", "19.10.1.1");
        DnsCacheManipulator.setDnsCache("myfavzookeeper", "127.0.0.1");
        DnsCacheManipulator.setDnsCache("custom-host", "127.0.0.1");

        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper("myzookeeper:2181,myfavzookeeper:2181")
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedHost("custom-host")
                .publishedPort(8021)
                .initialRotationStatus(true)
                .build();
        bundle.initialize(bootstrap);
        assertLocalHostNotAllowed();

    }

    @Test
    void shouldThrowExceptionForInvalidZkHost() {
        DnsCacheManipulator.setDnsCache("custom-host", "127.0.0.1");

        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper(String.format("%s:2181", UUID.randomUUID()))
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedHost("custom-host")
                .publishedPort(8021)
                .initialRotationStatus(true)
                .build();
        bundle.initialize(bootstrap);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            bundle.run(configuration, environment);

        });

        assertTrue(thrown.getMessage()
                           .contains("Couldn't resolve host address for zkHost"));

    }

    @Test
    void testPublishWithEmptyZkHost() throws UnknownHostException {
        DnsCacheManipulator.setDnsCache("myzookeeper", "19.10.1.1");
        DnsCacheManipulator.setDnsCache("myfavzookeeper", "127.0.0.1");
        DnsCacheManipulator.setDnsCache("custom-host", "127.0.0.1");
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper("myzookeeper:2181,myfavzookeeper:2181")
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedHost("")
                .publishedPort(8021)
                .initialRotationStatus(true)
                .build();
        bundle.initialize(bootstrap);

        val publishedHost = ConfigurationUtils.resolveNonEmptyPublishedHost(
                serviceDiscoveryConfiguration.getPublishedHost());
        val publishedHostAddress = InetAddress.getByName(publishedHost).getHostAddress();
        if (LOCAL_ADDRESSES.contains(publishedHostAddress) || LOCAL_ADDRESSES.contains(publishedHost)) {
            assertLocalHostNotAllowed();
        } else {
            assertDoesNotThrow();
        }
    }

    @Test
    void testPublishWithNullZkHost() throws UnknownHostException {
        DnsCacheManipulator.setDnsCache("myzookeeper", "19.10.1.1");
        DnsCacheManipulator.setDnsCache("myfavzookeeper", "127.0.0.1");
        DnsCacheManipulator.setDnsCache("custom-host", "127.0.0.1");
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper("myzookeeper:2181,myfavzookeeper:2181")
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedPort(8021)
                .initialRotationStatus(true)
                .build();
        bundle.initialize(bootstrap);

        val publishedHost = ConfigurationUtils.resolveNonEmptyPublishedHost(
                serviceDiscoveryConfiguration.getPublishedHost());
        val publishedHostAddress = InetAddress.getByName(publishedHost).getHostAddress();
        if (LOCAL_ADDRESSES.contains(publishedHostAddress) || LOCAL_ADDRESSES.contains(publishedHost)) {
            assertLocalHostNotAllowed();
        } else {
            assertDoesNotThrow();
        }
    }

    @Test
    void shouldPublishingToLocalZk() {
        DnsCacheManipulator.setDnsCache("myfavzookeeper", "127.0.0.1");
        DnsCacheManipulator.setDnsCache("custom-host", "127.0.0.1");

        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper("localhost:2181,myfavzookeeper:2181")
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedHost("localhost")
                .publishedPort(8021)
                .initialRotationStatus(true)
                .build();
        bundle.initialize(bootstrap);

        assertDoesNotThrow();

    }

    @Test
    void shouldPublishToRemoteZk() {
        DnsCacheManipulator.setDnsCache("myfavzookeeper", "17.4.0.1");
        DnsCacheManipulator.setDnsCache("custom-host", "17.1.2.1");

        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper("myfavzookeeper:2181")
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedHost("custom-host")
                .publishedPort(8021)
                .initialRotationStatus(true)
                .build();
        bundle.initialize(bootstrap);

        assertDoesNotThrow();
    }

    private void assertLocalHostNotAllowed() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            bundle.run(configuration, environment);

        });
        assertTrue(thrown.getMessage()
                           .contains("Not allowed to publish localhost address to remote zookeeper"));
    }


    private void assertDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> {
            bundle.run(configuration, environment);
        });
    }

}