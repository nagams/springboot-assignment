package com.demo.solventumdemo.web.controller;

import com.demo.solventumdemo.service.RedissonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Semaphore;

/**
 * This controller makes use of distributed Semaphore available with Redis via Redisson client implementation
 * This will ensure that only set number of requests will succeed when it receives the requests concurrently.
 *
 * @author Nagaraja Settra
 */
@RestController
@RequestMapping("/api/v2")
public class DistributedDemoController {

    private final RedissonService redissonService;

    public DistributedDemoController(RedissonService redissonService) {
        this.redissonService = redissonService;
    }

    @GetMapping("/one")
    public ResponseEntity<String> getOne() {
        return processRequest("this is one");
    }

    @GetMapping("/two")
    public ResponseEntity<String> getTwo() {
        return processRequest("this is two");
    }

    @GetMapping("/three")
    public ResponseEntity<String> getThree() {
        return processRequest("this is three");
    }

    private ResponseEntity<String> processRequest(String message) {
        if (redissonService.getRedissonSemaphore().tryAcquire()) {
            try {
                return ResponseEntity.ok(message);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error occurred while processing");
            } finally {
                redissonService.getRedissonSemaphore().release();
            }
        } else {
            // concurrent requests
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests. Please try later.");
        }
    }

    @GetMapping("/four")
    public String getFour() {
        return "this is Four!";
    }
}
