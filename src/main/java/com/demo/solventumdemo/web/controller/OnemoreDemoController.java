package com.demo.solventumdemo.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Semaphore;

/**
 * This is the correct controller for the assignment.
 * Controller makes use of Semaphore to manage the number of permits available to access endpoint available in this controller.
 * Currently, it is set to 2 so that at any given point at max only two endpoints would respond and the other would return error
 * response.
 *
 * @author Nagaraja Settra
 */
@RestController
@RequestMapping("/api")
public class OnemoreDemoController {
    private static final int MAX_AVAILABLE = 2;

    private final Semaphore availableEndPoints = new Semaphore(MAX_AVAILABLE);

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
        if (availableEndPoints.tryAcquire()) {
            try {
                return ResponseEntity.ok(message);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error occurred while processing");
            } finally {
                availableEndPoints.release();
            }
        } else {
            // concurrent requests
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests. Please try later.");
        }
    }

}
