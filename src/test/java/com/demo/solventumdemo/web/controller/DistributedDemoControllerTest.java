package com.demo.solventumdemo.web.controller;

import com.demo.solventumdemo.config.DemoConfigProperties;
import com.demo.solventumdemo.config.SecurityConfig;
import com.demo.solventumdemo.service.RedissonService;
import com.demo.solventumdemo.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest({DistributedDemoController.class, AuthController.class})
@Import({SecurityConfig.class, TokenService.class, RedissonService.class})
class DistributedDemoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void whenNotAuthenticated_then401() throws Exception {
        this.mockMvc.perform(get("/api/v2/four"))
                .andExpect(status().isUnauthorized());
    }

    String token;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/token")
                        .with(httpBasic("demo", "password")))
                .andExpect(status().isOk())
                .andReturn();

        token = result.getResponse().getContentAsString();

    }

    @Test
    void whenAuthenticated_getFour() throws Exception {

        mockMvc.perform(get("/api/v2/four")
                        .header("Authorization", "Bearer " + token))
                .andExpectAll(
                        status().isOk(),
                        content().string("this is Four!")
                );
    }

    /**
     * This test method when run, it hits set number of requests concurrently to the distributed demo controller.
     * Since all of them are invoked concurrently with the limit set at two on the semaphore only two will succeed.
     *
     * @throws InterruptedException
     */
    @Test
    void withThreeConcurrentRequests_oneShouldFail() throws InterruptedException {
        int threadCount = 5;
        ExecutorService exService = Executors.newFixedThreadPool(threadCount);

        String[] apis = {"/api/v2/one", "/api/v2/two", "/api/v2/three"};
        List<Callable<MvcResult>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Callable<MvcResult> task = () -> {
                int randomApiIndex = ThreadLocalRandom.current().nextInt(apis.length);
                return mockMvc.perform(get(apis[randomApiIndex])
                                .header("Authorization", "Bearer " + token))
                        .andReturn();
            };
            tasks.add(task);
        }
        List<Future<MvcResult>> futureList = exService.invokeAll(tasks);

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

}