package edu.cit.arnejo.dormshare.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller that handles mobile-initiated OAuth2 flows.
 *
 * The mobile app opens a Chrome Custom Tab to /auth/google-mobile,
 * which sets a session flag and then redirects to the standard
 * Spring Security OAuth2 authorization endpoint. When the flow
 * completes, OAuth2LoginSuccessHandler checks this flag and
 * redirects to dormshare://oauth2/callback instead of the web frontend.
 */
@RestController
@RequestMapping("/auth")
public class MobileOAuth2Controller {

    @GetMapping("/google-mobile")
    public void initiateGoogleLoginForMobile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Set session attribute so OAuth2LoginSuccessHandler knows to redirect to mobile
        request.getSession().setAttribute("oauth2_source", "mobile");

        // Redirect to Spring Security's built-in Google OAuth2 authorization endpoint
        response.sendRedirect("/oauth2/authorization/google");
    }
}
