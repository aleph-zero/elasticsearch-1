package org.apache.mesos.elasticsearch.systemtest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.jayway.awaitility.Awaitility;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.state.Framework;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * TODO (jhftrifork): This is a useless JavaDoc comment
 */
public class FrameworkRoleSystemTest {
    protected static final int NODE_COUNT = 3;

    protected static final MesosClusterConfig CONFIG = MesosClusterConfig.builder()
            .numberOfSlaves(NODE_COUNT)
            .privateRegistryPort(15000) // Currently you have to choose an available port by yourself
            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
            .build();

    public static final Logger LOGGER = Logger.getLogger(FrameworkRoleSystemTest.class);

    @Rule
    public final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @Before
    public void before() throws Exception {
        CLUSTER.injectImage("mesos/elasticsearch-executor");
    }

    @After
    public void after() {
        CLUSTER.stop();
    }

    @Test
    public void miniMesosReportsTheRequestedFrameworkRole() throws UnirestException, JsonParseException, JsonMappingException {
        final String ARBITRARY_ROLE_STRING = "*";

        LOGGER.info("Starting Elasticsearch scheduler");
        ElasticsearchSchedulerContainer scheduler = new ElasticsearchSchedulerContainer(
                CONFIG.dockerClient,
                CLUSTER.getMesosContainer().getIpAddress(),
                ARBITRARY_ROLE_STRING
        );
        CLUSTER.addAndStartContainer(scheduler);
        LOGGER.info("Started Elasticsearch scheduler on " + scheduler.getIpAddress() + ":31100");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> CLUSTER.getStateInfo().getFramework("elasticsearch") != null);
        Assert.assertEquals(ARBITRARY_ROLE_STRING, CLUSTER.getStateInfo().getFramework("elasticsearch").getRole());
        scheduler.remove();
    }
}