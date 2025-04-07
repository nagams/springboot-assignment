package com.demo.solventumdemo.web.controller;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@WebMvcTest(OnemoreDemoController.class)
@AutoConfigureMockMvc(addFilters = false)
class OnemoreDemoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void getOne() throws Exception {
        mockMvc.perform(get("/api/one"))
                .andExpectAll(
                        status().isOk(),
                        content().string("this is one")
                );
    }

    @Test
    void getTwo() throws Exception {
        mockMvc.perform(get("/api/two"))
                .andExpectAll(
                        status().isOk(),
                        content().string("this is two")
                );
    }

    @Test
    void getThree_test() throws Exception {
        mockMvc.perform(get("/api/three"))
                .andExpectAll(
                        status().isOk(),
                        content().string("this is three")
                );
    }

    /**
     * Lot going on here to achieve concurrent invocation of controller end points. With very limited number of
     * requests such as 3 matching the number of endpoints in the controller, it is difficult achieve concurrent invocations.
     * So for this reason I am sending 500 requests randomly picking one of the APIs to trigger a case where there concurrent hit of
     * 3 calls so that one can error out with 429.
     *
     * I have made use 3 CountDownLatch object to maximize the probability of hitting concurrency.
     *
     * This method tests that requests will result in OK status and some with TOO_MANY_REQUESTS status.
     * @throws Exception
     */
    @Test
    void someRequestsShouldFail_forConcurrentReqs() throws Exception {
        // Making use of CountDownLatches and callable tasks to simulate
        // concurrent invocation of endpoints in the controller
        int threadCount = 500;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        ExecutorService exService = Executors.newFixedThreadPool(threadCount);

        String[] apis = {"/api/one", "/api/two", "/api/three"};
        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Callable<MvcResult> worker = () -> {
                try {
                    readyLatch.countDown();
                    // wait for the start latch is counted down
                    startLatch.await();
                    int randomApiIndex = ThreadLocalRandom.current().nextInt(apis.length);
                    return mockMvc.perform(get(apis[randomApiIndex])).andReturn();
                } finally {
                    finishLatch.countDown();
                }

            };
            tasks.add(worker);

        }

        List<Future<MvcResult>> futureList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futureList.add(exService.submit(tasks.get(i)));
        }

        readyLatch.await();
        //Once threads are created, start them concurrently
        startLatch.countDown();

        // Now wait for them to finish
        finishLatch.await();
        exService.shutdown();

        List<Integer> statusList = futureList.stream()
                .map(f -> {
                    try {
                        return f.get().getResponse().getStatus();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        assertThat(statusList).contains(HttpStatus.OK.value());
        assertThat(statusList).contains(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    /**
     * This method tests that all calls to actuator will succeed when hit concurrently with the hits to
     * test controller end points.
     *
     * @throws Exception
     */
    @Test
    void actuatorEndpoints_alwaysSucceed() throws Exception {
        // Making use of CountDownLatches and callable tasks to simulate
        // concurrent invocation of endpoints in the controller
        int threadCount = 500;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        ExecutorService exService = Executors.newFixedThreadPool(threadCount);

        String[] apis = {"/api/one", "/api/two", "/api/three", "/actuator/info"};
        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Callable<MvcResult> worker = () -> {
                try {
                    readyLatch.countDown();
                    // wait for the start latch is counted down
                    startLatch.await();
                    int randomApiIndex = ThreadLocalRandom.current().nextInt(apis.length);
                    return mockMvc.perform(get(apis[randomApiIndex])).andReturn();
                } finally {
                    finishLatch.countDown();
                }

            };
            tasks.add(worker);

        }

        List<Future<MvcResult>> futureList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futureList.add(exService.submit(tasks.get(i)));
        }

        readyLatch.await();
        //Once threads are created, start them concurrently
        startLatch.countDown();

        // Now wait for them to finish
        finishLatch.await();
        exService.shutdown();

        List<ApiResp> apiRespList = futureList.stream()
                .map(f -> {
                    try {
                        return new ApiResp(f.get().getRequest().getPathInfo(), f.get().getResponse().getStatus());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

/*
        assertThat(apiRespList)
                .extracting(ApiResp::status)
                .contains(HttpStatus.TOO_MANY_REQUESTS.value());
*/

        assertThat(apiRespList)
                .filteredOn(apiResp -> apiResp.name().equals("/actuator/info"))
                .extracting(ApiResp::status)
                .allMatch(s -> s == HttpStatus.OK.value());
    }
}

record ApiResp(String name, int status){}
