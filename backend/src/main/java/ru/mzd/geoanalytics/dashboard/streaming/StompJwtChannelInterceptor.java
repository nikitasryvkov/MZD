package ru.mzd.geoanalytics.dashboard.streaming;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import ru.mzd.geoanalytics.dashboard.common.config.ApplicationProperties;
import ru.mzd.geoanalytics.dashboard.security.JwtAuthorityExtractor;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Set<String> SUBSCRIBABLE_DESTINATIONS = Set.of(StreamingTopics.TRAINS, StreamingTopics.EVENTS);

    private final ApplicationProperties applicationProperties;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final JwtAuthorityExtractor jwtAuthorityExtractor;

    public StompJwtChannelInterceptor(
        ApplicationProperties applicationProperties,
        ObjectProvider<JwtDecoder> jwtDecoderProvider,
        JwtAuthorityExtractor jwtAuthorityExtractor
    ) {
        this.applicationProperties = applicationProperties;
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.jwtAuthorityExtractor = jwtAuthorityExtractor;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(resolveAuthentication(accessor));
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            requireAuthentication(accessor);
            throw new AccessDeniedException("Клиентские операции STOMP SEND не поддерживаются.");
        }

        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Authentication authentication = requireAuthentication(accessor);
        String destination = accessor.getDestination();

        if (destination == null || !SUBSCRIBABLE_DESTINATIONS.contains(destination)) {
            throw new AccessDeniedException("Подписка на запрошенный STOMP-адрес назначения запрещена.");
        }

        boolean hasRequiredAuthority = authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(applicationProperties.getSecurity().getDashboardAuthorities()::contains);

        if (!hasRequiredAuthority) {
            throw new AccessDeniedException("Подписка на стриминговые топики дашборда запрещена.");
        }
    }

    private Authentication requireAuthentication(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Требуется аутентифицированная STOMP-сессия.");
        }
        return authentication;
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (!applicationProperties.getSecurity().isEnabled()) {
            return new UsernamePasswordAuthenticationToken(
                applicationProperties.getSecurity().getLocalDevUser().getPrincipalId(),
                "N/A",
                applicationProperties.getSecurity().getLocalDevUser().getRoles()
                    .stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList()
            );
        }

        String authorizationHeader = Optional.ofNullable(accessor.getNativeHeader("Authorization"))
            .stream()
            .flatMap(List::stream)
            .filter(headerValue -> headerValue != null && !headerValue.isBlank())
            .findFirst()
            .orElseThrow(() -> new AccessDeniedException("В кадре STOMP CONNECT отсутствует заголовок Authorization."));

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Схема STOMP-аутентификации не поддерживается.");
        }

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder == null) {
            throw new AccessDeniedException("JWT-декодер недоступен.");
        }

        return jwtAuthorityExtractor.convert(jwtDecoder.decode(authorizationHeader.substring(7)));
    }
}
