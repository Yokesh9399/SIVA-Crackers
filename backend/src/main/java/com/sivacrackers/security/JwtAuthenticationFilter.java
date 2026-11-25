package com.sivacrackers.security;

import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the session cookie on every request and, if the token is valid, signs the user in for
 * that request only (stateless).
 *
 * <p>The user is re-loaded from the database each time, so deactivating an account, changing its
 * role or changing its password (token version) takes effect immediately. Any problem simply
 * leaves the request anonymous – protected endpoints then answer 401. An invalid cookie is cleared.
 *
 * <p>Created in SecurityConfig (not a @Component) so it runs only inside the security chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthCookies cookies;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, AuthCookies cookies, UserRepository users) {
        this.jwtService = jwtService;
        this.cookies = cookies;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = cookies.read(request);
        if (token != null && !token.isEmpty()) {
            Optional<User> user = jwtService.verify(token)
                    .flatMap(claims -> users.findById(claims.userId())
                            .filter(User::isActive)
                            .filter(u -> u.getTokenVersion() == claims.version())
                            .filter(u -> u.getRole() == claims.role()));
            if (user.isPresent()) {
                User u = user.get();
                AuthUser principal = new AuthUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole());
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                cookies.clear(response); // expired, tampered, revoked or for a deactivated account
            }
        }
        chain.doFilter(request, response);
    }
}
