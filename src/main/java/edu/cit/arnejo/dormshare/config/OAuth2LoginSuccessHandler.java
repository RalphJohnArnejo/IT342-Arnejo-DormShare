package edu.cit.arnejo.dormshare.config;

import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");

        final String userFirstName = (givenName != null) ? givenName : (name != null ? name : "User");
        final String userLastName = (familyName != null) ? familyName : "";

        // Find or create user
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setFirstName(userFirstName);
            newUser.setLastName(userLastName);
            newUser.setPasswordHash("OAUTH2_USER");
            newUser.setRole("USER");
            return userRepository.save(newUser);
        });

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        // Redirect to frontend with token
        String redirectUrl = "http://localhost:5173/oauth2/callback?token=" + token
                + "&userId=" + user.getId()
                + "&email=" + user.getEmail()
                + "&firstName=" + user.getFirstName()
                + "&lastName=" + user.getLastName()
                + "&role=" + user.getRole();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
