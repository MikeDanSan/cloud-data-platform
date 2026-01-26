package com.michael.backendservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "Health API", description = "Endpoints for health checks")
public class PingController {
    @Operation(
    summary = "Ping endpoint", 
    description = "Simple endpoint to check if the service is running."
    )
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
