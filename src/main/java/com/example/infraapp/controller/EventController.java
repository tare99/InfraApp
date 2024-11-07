package com.example.infraapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

  @GetMapping("/events/{eventId}")
  public Event getEvent(@PathVariable String eventId) {
    return new Event(eventId, "Barcelona vs Real Madrid");
  }

  public record Event(String id, String name) {}
}
