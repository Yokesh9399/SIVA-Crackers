package com.sivacrackers.user;

import com.sivacrackers.security.AuthCookies;
import com.sivacrackers.security.AuthUser;
import com.sivacrackers.security.JwtService;
import com.sivacrackers.user.AuthDtos.CsrfResponse;
import com.sivacrackers.user.AuthDtos.LoginRequest;
import com.sivacrackers.user.AuthDtos.RegisterRequest;
import com.sivacrackers.user.AuthDtos.SessionResponse;
import com.sivacrackers.user.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login, registration, logout and "who am I".
 * The session token is only ever placed in the HttpOnly cookie – never in a response body.
 * All POSTs require the CSRF token (header X-XSRF-TOKEN, obtained via GET /api/auth/csrf).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AccountService accounts;
    private final JwtService jwt;
    private final AuthCookies cookies;
    private final CsrfTokenRepository csrfTokens;

    public AuthController(AuthService auth, AccountService accounts, JwtService jwt, AuthCookies cookies,
                          CsrfTokenRepository csrfTokens) {
        this.auth = auth;
        this.accounts = accounts;
        this.jwt = jwt;
        this.cookies = cookies;
        this.csrfTokens = csrfTokens;
    }

    /** Issues the XSRF-TOKEN cookie. The frontend calls this once before its first POST. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName());
    }

    /** Public sign-up: always a CUSTOMER account; signs the new customer in. */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest body,
                                                 HttpServletRequest request, HttpServletResponse response) {
        User user = auth.register(body, request.getRemoteAddr());
        startSession(user, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(user));
    }

    /** Customer login (CUSTOMER accounts only). */
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        User user = auth.login(body, false, request.getRemoteAddr());
        startSession(user, request, response);
        return UserResponse.of(user);
    }

    /** Staff login (OWNER, MANAGER, CASHIER only). */
    @PostMapping("/staff/login")
    public UserResponse staffLogin(@Valid @RequestBody LoginRequest body,
                                   HttpServletRequest request, HttpServletResponse response) {
        User user = auth.login(body, true, request.getRemoteAddr());
        startSession(user, request, response);
        return UserResponse.of(user);
    }

    /** Ends the session in this browser (deletes the cookie). Safe to call when not logged in. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        cookies.clear(response);
        rotateCsrfToken(request, response);
        return ResponseEntity.noContent().build();
    }

    /**
     * The current session for the website: the user, or {@code {"user": null}} for visitors.
     * Always 200, so a normal visit does not produce an error response.
     */
    @GetMapping("/session")
    public SessionResponse session(@AuthenticationPrincipal AuthUser user) {
        return new SessionResponse(user == null ? null : UserResponse.of(accounts.get(user.id())));
    }

    /** The logged-in user (401 when not logged in). */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return UserResponse.of(accounts.get(user.id()));
    }

    private void startSession(User user, HttpServletRequest request, HttpServletResponse response) {
        cookies.write(response, jwt.issue(user), jwt.lifetime());
        rotateCsrfToken(request, response);
    }

    /** A new CSRF token after login/logout, so a token seen before login cannot be reused. */
    private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokens.saveToken(csrfTokens.generateToken(request), request, response);
    }
}
