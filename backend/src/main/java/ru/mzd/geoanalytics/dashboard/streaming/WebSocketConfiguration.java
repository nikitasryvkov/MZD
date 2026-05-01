package ru.mzd.geoanalytics.dashboard.streaming;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final ApplicationProperties applicationProperties;
    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    public WebSocketConfiguration(
        ApplicationProperties applicationProperties,
        StompJwtChannelInterceptor stompJwtChannelInterceptor
    ) {
        this.applicationProperties = applicationProperties;
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(applicationProperties.getCors().getAllowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }
}
