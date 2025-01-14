package com.example.flow.controller;

import com.example.flow.dto.AllowUserResponse;
import com.example.flow.dto.AllowedUserResponse;
import com.example.flow.dto.RankNumberResponse;
import com.example.flow.dto.RegisterUserResponse;
import com.example.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {

    private final UserQueueService userQueueService;

    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(
            @RequestParam("user_id") Long userId,
            @RequestParam(value = "queue", defaultValue = "default") String queue
    ) {
        return userQueueService.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }

    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(
            @RequestParam(value = "queue", defaultValue = "default") String queue,
            @RequestParam("count") Long count
    ) {
        return userQueueService.allowUser(queue, count)
                .map(allowed -> new AllowUserResponse(count, allowed));
    }

    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(
            @RequestParam(value = "queue", defaultValue = "default") String queue,
            @RequestParam("user_id") Long userId,
            @RequestParam("token") String token
    ) {
        return userQueueService.isAllowedByToken(queue, userId, token)
                .map(AllowedUserResponse::new);
    }

    @GetMapping("/rank")
    public Mono<RankNumberResponse> getUserRank(
            @RequestParam(value = "queue", defaultValue = "default") String queue,
            @RequestParam("user_id") Long userId
    ) {
        return userQueueService.getRank(queue, userId)
                .map(RankNumberResponse::new);
    }

    @GetMapping("/touch")
    Mono<String> touch(
            @RequestParam(value = "queue", defaultValue = "default") String queue,
            @RequestParam("user_id") Long userId,
            ServerWebExchange exchange
    ) {
        return Mono.defer(() -> userQueueService.generateToken(queue, userId))
                .map(token -> {
                    exchange.getResponse().addCookie(
                            ResponseCookie
                                    .from("user-queue-%s-token".formatted(queue), token)
                                    .maxAge(Duration.ofSeconds(300))
                                    .path("/")
                                    .build()
                    );

                    return token;
                });
    }
}
