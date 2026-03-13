package com.maher.booking_system.controller;

import com.maher.booking_system.model.NotificationLog;
import com.maher.booking_system.service.NotificationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationLogService notificationLogService;

    public NotificationController(NotificationLogService notificationLogService) {
        this.notificationLogService = notificationLogService;
    }

    @GetMapping("/admin/logs")
    public List<NotificationLog> getLogs() {
        return notificationLogService.getAllLogs();
    }

    @PostMapping("/admin/run-return-reminders")
    public Map<String, Integer> runReturnReminders() {
        return Map.of("sent", notificationLogService.sendReturnReminders());
    }
}
