package com.ttclub.backend.security;

import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.RoleRepository;
import com.ttclub.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository   users;
    private final RoleRepository   roles;
    private final PasswordEncoder  encoder;

    public CustomOidcUserService(UserRepository  users,
                                 RoleRepository  roles,
                                 PasswordEncoder encoder) {
        this.users   = users;
        this.roles   = roles;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest req) {
        OidcUser google = super.loadUser(req);

        String email = google.getEmail() == null ? "" : google.getEmail().trim().toLowerCase(Locale.ROOT);

        users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFirstName(google.getGivenName());
            u.setLastName(google.getFamilyName());

            Role client = roles.findByName(RoleName.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("CLIENT role missing"));
            u.setRole(client);

            u.setProvider(AuthProvider.GOOGLE);

            String dummy = UUID.randomUUID().toString();
            u.setPasswordHash(encoder.encode(dummy));

            return users.save(u);
        });

        return google;
    }
}
