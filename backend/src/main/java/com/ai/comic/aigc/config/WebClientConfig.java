package com.ai.comic.aigc.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * WebClient 配置（用于调用 Agnes AI）。
 * <p>
 * 配置连接与读写超时。
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient agnesWebClient(AgnesProperties agnesProperties) {
        int timeoutSec = agnesProperties.getTimeoutSec();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSec * 1000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSec, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSec, TimeUnit.SECONDS)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
