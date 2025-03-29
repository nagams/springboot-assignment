package com.demo.solventumdemo.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.locks.ReentrantLock;

/**
 *  With my initial quick read of the problem statement, I thought what was requested was to make api /one and api /two
 *  to be always successful and only restrict the api /three from concurrent calls and wrote this controller
 *  using ReentrantLock. However, I later realized that, when all endpoints are called concurrently to respond successfully with only
 *  two of the three and return the error with the remaining one.
 *
 *  Since, I had already written this, I kept it here. But the requested implementation is in
 *  OnemoreDemoController
 *
 * @author Nagaraja Settra
 */
@RestController
public class DemoController {

    private final ReentrantLock lock = new ReentrantLock();

    @GetMapping("/one")
    public String getOne() {
        return "this is one";
    }

    @GetMapping("/two")
    public String getTwo() {
        return "this is two";
    }

    @GetMapping("/three")
    public ResponseEntity<String> getThree() {
        if (lock.tryLock()) {
            try {
                return ResponseEntity.ok("this is three");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error occurred while processing");
            } finally {
                lock.unlock();
            }
        } else {
            // concurrent requests
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests. Please try later.");
        }
    }
}
