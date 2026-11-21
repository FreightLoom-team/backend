package com.truckingsystem.service;

import com.truckingsystem.common.EmailAlreadyRegisteredException;
import com.truckingsystem.common.ExternalIdentityProviderException;
import com.truckingsystem.common.InvalidSignUpRequestException;
import com.truckingsystem.pojo.SignUpRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class SupabaseAuthClient {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final Pattern VALID_EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;

    public SupabaseAuthClient(
            @Value("${supabase.url}") String baseUrl,
            @Value("${supabase.anon-key}") String apiKey) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
    }

    public SupabaseUser signUp(SignUpRequest request) {
        JsonNode response;
        String signupEmail = effectiveSignupEmail(request.email());
        try {
            response = restClient.post()
                    .uri(baseUrl + "/auth/v1/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("apikey", apiKey)
                    .body(Map.of(
                            "email", signupEmail,
                            "password", request.password(),
                            "data", Map.of(
                                    "first_name", request.firstName().trim(),
                                    "last_name", request.lastName().trim())))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            String providerMessage = extractProviderMessage(exception.getResponseBodyAsString());
            if (exception.getStatusCode().value() == 409
                    || providerMessage.toLowerCase().contains("already registered")) {
                throw new EmailAlreadyRegisteredException();
            }
            if (exception.getStatusCode().is4xxClientError()
                    && exception.getStatusCode().value() != 401
                    && exception.getStatusCode().value() != 403) {
                throw new InvalidSignUpRequestException(providerMessage);
            }
            throw new ExternalIdentityProviderException(
                    "Supabase Auth rejected signup (HTTP " + exception.getStatusCode().value()
                            + "): " + providerMessage,
                    exception);
        } catch (RuntimeException exception) {
            if (exception instanceof EmailAlreadyRegisteredException
                    || exception instanceof ExternalIdentityProviderException) {
                throw exception;
            }
            throw new ExternalIdentityProviderException("Could not reach Supabase Auth", exception);
        }

        JsonNode user = response == null ? null : response.hasNonNull("user") ? response.get("user") : response;
        if (user == null || !user.hasNonNull("id") || !user.hasNonNull("email")) {
            throw new ExternalIdentityProviderException("Supabase Auth returned an incomplete signup response");
        }
        JsonNode identities = user.get("identities");
        if (identities != null && identities.isArray() && identities.isEmpty()) {
            throw new EmailAlreadyRegisteredException();
        }

        UUID userId;
        try {
            userId = UUID.fromString(user.get("id").asText());
        } catch (IllegalArgumentException exception) {
            throw new ExternalIdentityProviderException("Supabase Auth returned an invalid user ID", exception);
        }

        String accessToken = response.hasNonNull("access_token")
                ? response.get("access_token").asText()
                : null;
        return new SupabaseUser(userId.toString(), user.get("email").asText(), accessToken);
    }

    private String effectiveSignupEmail(String requestedEmail) {
        if (requestedEmail != null) {
            String trimmed = requestedEmail.trim();
            if (VALID_EMAIL.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return "signup-" + UUID.randomUUID() + "@freightloom.invalid";
    }

    private String extractProviderMessage(String responseBody) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode body = JSON_MAPPER.readTree(responseBody);
                for (String field : new String[]{"msg", "message", "error_description", "error"}) {
                    JsonNode message = body.get(field);
                    if (message != null && message.isTextual() && !message.asText().isBlank()) {
                        return safeMessage(message.asText());
                    }
                }
            } catch (JacksonException ignored) {
                return safeMessage(responseBody);
            }
        }
        return "Supabase did not provide an error description";
    }

    private String safeMessage(String message) {
        String sanitized = message.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return sanitized.substring(0, Math.min(sanitized.length(), 300));
    }

    public record SupabaseUser(String subject, String email, String accessToken) {
    }
}
