package edu.cit.arnejo.dormshare.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        logger.info("OAuth2 authentication successful, processing handler");
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            logger.info("Got OAuth2User: {}", oAuth2User.getName());

            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String givenName = oAuth2User.getAttribute("given_name");
            String familyName = oAuth2User.getAttribute("family_name");
            logger.info("Email: {}, Name: {}, GivenName: {}, FamilyName: {}", email, name, givenName, familyName);

            final String userFirstName = (givenName != null) ? givenName : (name != null ? name : "User");
            final String userLastName = (familyName != null) ? familyName : "";

            // Find or create user
            logger.info("Looking up user by email: {}", email);
            UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
                logger.info("User not found, creating new user");
                UserEntity newUser = new UserEntity();
                newUser.setEmail(email);
                newUser.setFirstName(userFirstName);
                newUser.setLastName(userLastName);
                newUser.setPasswordHash("OAUTH2_USER");
                newUser.setRole("USER");
                return userRepository.save(newUser);
            });
            logger.info("User loaded/created: {}", user.getId());

            // Generate JWT token
            logger.info("Generating JWT token for user: {}", user.getId());
            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            logger.info("Token generated successfully");

            // Redirect to frontend with token (URL-encoded to handle special characters)
            String redirectUrl = frontendUrl + "/oauth2/callback?"
                    + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&userId=" + URLEncoder.encode(user.getId().toString(), StandardCharsets.UTF_8)
                    + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                    + "&firstName=" + URLEncoder.encode(user.getFirstName(), StandardCharsets.UTF_8)
                    + "&lastName=" + URLEncoder.encode(user.getLastName(), StandardCharsets.UTF_8)
                    + "&role=" + URLEncoder.encode(user.getRole(), StandardCharsets.UTF_8);
            logger.info("Redirecting to: {}", redirectUrl);

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            logger.info("Redirect sent successfully");
        } catch (Exception e) {
            logger.error("Error in OAuth2 success handler", e);
            throw new IOException(e);
        }
    }
}
