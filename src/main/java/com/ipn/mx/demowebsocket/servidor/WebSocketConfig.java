package com.ipn.mx.demowebsocket.servidor;

import com.ipn.mx.demowebsocket.basedatos.service.ScoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ScoreService scoreService;
    private final PendingConnectionRegistry registry;
    private final TableroHandler tableroHandler;

    public WebSocketConfig(ScoreService scoreService, PendingConnectionRegistry registry, TableroHandler tableroHandler) {
        this.scoreService = scoreService;
        this.registry = registry;
        this.tableroHandler = tableroHandler;
    }

    @Bean
    public RojoHandler rojoHandler() {
        return new RojoHandler(scoreService, registry);
    }

    @Bean
    public AzulHandler azulHandler() {
        return new AzulHandler(scoreService, registry);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(1800000L); // 5 minutos
        container.setAsyncSendTimeout(5000L); // 5 segundos
        return container;
    }

    @Bean
    public ThreadPoolTaskExecutor webSocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ws-handler-");
        executor.initialize();
        return executor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry r) {
        r.addHandler(rojoHandler(), "/ws/peto/rojo")
                .setAllowedOrigins("*");
        r.addHandler(azulHandler(), "/ws/peto/azul")
                .setAllowedOrigins("*");
        r.addHandler(tableroHandler, "/ws/tablero/{combateId}")
                .setAllowedOrigins("*");
        //r.addHandler(celularHandler, "/ws/celular/{celularnum}")
         //       .setAllowedOrigins("*");
    }
}