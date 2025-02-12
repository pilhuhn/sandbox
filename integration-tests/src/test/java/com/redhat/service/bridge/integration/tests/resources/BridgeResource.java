package com.redhat.service.bridge.integration.tests.resources;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;

import io.restassured.response.Response;

public class BridgeResource {

    public static BridgeResponse addBridge(String token, String bridgeName) {
        return ResourceUtils.jsonRequest(token)
                .body(new BridgeRequest(bridgeName))
                .post(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .as(BridgeResponse.class);
    }

    public static Response getBridgeDetailsResponse(String token, String bridgeId) {
        return ResourceUtils.jsonRequest(token)
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId);
    }

    public static BridgeResponse getBridgeDetails(String token, String bridgeId) {
        return ResourceUtils.jsonRequest(token)
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId)
                .then()
                .statusCode(200)
                .extract()
                .as(BridgeResponse.class);
    }

    public static BridgeListResponse getBridgeList(String token) {
        return getBridgeListResponse(token)
                .then()
                .statusCode(200)
                .extract()
                .as(BridgeListResponse.class);
    }

    public static Response getBridgeListResponse(String token) {
        return ResourceUtils.jsonRequest(token)
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH);
    }

    public static void deleteBridge(String token, String bridgeId) {
        ResourceUtils.jsonRequest(token)
                .delete(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId)
                .then()
                .statusCode(202);
    }
}
