package com.nexora.platform.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexora.platform.identity.IdentityPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("database")
@RequestMapping(path = "/api/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    ProfileService.UserProfile get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(IdentityPrincipal.from(jwt).subjectId());
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ProfileService.UserProfile update(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.update(IdentityPrincipal.from(jwt).subjectId(), request.toUpdate());
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$") String locale,
            @NotNull Boolean reducedMotion,
            @NotNull Boolean highContrast,
            @Min(0) long expectedVersion) {
        ProfileService.ProfileUpdate toUpdate() {
            return new ProfileService.ProfileUpdate(
                    displayName, locale, reducedMotion, highContrast, expectedVersion);
        }
    }
}
