package com.redhat.service.bridge.infra;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.BridgeError;
import com.redhat.service.bridge.infra.exceptions.BridgeErrorService;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.test.exceptions.ExceptionHelper;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BridgeErrorServiceTest {

    @Inject
    BridgeErrorService service;

    private static Collection<Class<?>> userExceptionClasses;
    private static Collection<Class<?>> platformExceptionClasses;

    @BeforeAll
    private static void init() {
        userExceptionClasses = ExceptionHelper.getUserExceptions();
        platformExceptionClasses = ExceptionHelper.getPlatformExceptions();
    }

    @Test
    void testUserErrorList() {
        final int pageSize = 2;
        Collection<BridgeError> bridgeErrors = new ArrayList<>();
        ListResult<BridgeError> result;
        int page = 0;
        do {
            result = service.getUserErrors(new QueryInfo(page++, pageSize));
            bridgeErrors.addAll(result.getItems());
        } while (result.getSize() == pageSize);
        assertThat(userExceptionClasses).hasSize(bridgeErrors.size()).withFailMessage(String.format("Exception classes: %s Errors: %s", userExceptionClasses, bridgeErrors));
        bridgeErrors.forEach(this::checkId);
    }

    @Test
    void testUserErrorException() {
        userExceptionClasses.forEach(this::checkExceptionIsInCatalog);
    }

    @Test
    void testPlatformErrorException() {
        platformExceptionClasses.forEach(this::checkExceptionIsNotInCatalog);
    }

    private void checkId(BridgeError bridgeError) {
        assertThat(service.getUserError(bridgeError.getId()).isPresent()).isTrue();
    }

    private void checkExceptionIsInCatalog(Class<?> clazz) {
        BridgeError bridgeError = service.getError(clazz).get();
        assertThat(bridgeError).isNotNull().withFailMessage(String.format("exception %s not found in the errors", clazz));
        assertThat(service.getUserError(bridgeError.getId()).isPresent()).isTrue().withFailMessage(String.format("exception %s not found in the user errors", clazz));
    }

    private void checkExceptionIsNotInCatalog(Class<?> clazz) {
        BridgeError bridgeError = service.getError(clazz).get();
        assertThat(bridgeError).isNotNull().withFailMessage(String.format("exception %s not found in the errors", clazz));
        assertThat(service.getUserError(bridgeError.getId()).isPresent()).isFalse().withFailMessage(String.format("exception %s should not be in the user errors", clazz));
    }

}
