package com.redhat.service.bridge.manager.api.internal;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.ShardService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ShardBridgesSyncSegmentationAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    RhoasService rhoasServiceMock;

    @InjectMock
    ShardService shardService;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.SUBJECT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);

        // Authorize all
        when(shardService.isAuthorizedShard(any(String.class))).thenReturn(true);

        // Always assign to the default shard id
        when(shardService.getAssignedShardId(any(String.class))).thenReturn(TestConstants.SHARD_ID);
    }

    /**
     * This test needs to be in a separated class since for the current implementation the ShardService fetches
     * the authorized shards at startup. We inject a mock for this scenario.
     */
    @Test
    @TestSecurity(user = "knative")
    public void testShardSegmentation() {
        // the bridge gets assigned to the default shard
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        // The default shard retrieves the bridge to deploy
        List<BridgeDTO> bridgesToDeployForDefaultShard = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        assertThat(bridgesToDeployForDefaultShard.size()).isEqualTo(1);

        reset(jwt);
        when(jwt.getClaim(APIConstants.SUBJECT_ATTRIBUTE_CLAIM)).thenReturn("knative");

        // No bridges are assigned to the 'knative' shard
        List<BridgeDTO> bridgesToDeployForOtherShard = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        assertThat(bridgesToDeployForOtherShard.size()).isEqualTo(0);
    }
}
