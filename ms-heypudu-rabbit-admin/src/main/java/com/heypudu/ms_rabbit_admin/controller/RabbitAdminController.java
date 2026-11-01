package com.heypudu.ms_rabbit_admin.controller;

import com.heypudu.ms_rabbit_admin.dto.BindingRequest;
import com.heypudu.ms_rabbit_admin.dto.ExchangeRequest;
import com.heypudu.ms_rabbit_admin.dto.QueueRequest;
import com.heypudu.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rabbit")
@PreAuthorize("hasRole('ADMIN')")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/exchanges/{name}")
    public ResponseEntity<Void> createExchange(
            @PathVariable String name,
            @Valid @RequestBody ExchangeRequest request) {
        rabbitAdminService.createExchange(name, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/queues/{name}")
    public ResponseEntity<Void> createQueue(
            @PathVariable String name,
            @Valid @RequestBody QueueRequest request) {
        rabbitAdminService.createQueue(name, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> createBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.createBinding(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.deleteBinding(request);
        return ResponseEntity.noContent().build();
    }
}
