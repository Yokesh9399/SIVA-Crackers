package com.sivacrackers.user;

import com.sivacrackers.security.AuthCookies;
import com.sivacrackers.security.AuthUser;
import com.sivacrackers.security.JwtService;
import com.sivacrackers.user.AuthDtos.ChangePasswordRequest;
import com.sivacrackers.user.AuthDtos.UpdateProfileRequest;
import com.sivacrackers.user.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's own account (any role). */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accounts;
    private final JwtService jwt;
    private final AuthCookies cookies;

    public AccountController(AccountService accounts, JwtService jwt, AuthCookies cookies) {
        this.accounts = accounts;
        this.jwt = jwt;
        this.cookies = cookies;
    }

    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal AuthUser user) {
        return UserResponse.of(accounts.get(user.id()));
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest body) {
        return UserResponse.of(accounts.updateProfile(user.id(), body));
    }

    /** Ends all other sessions of this user and gives this browser a fresh session cookie. */
    @PutMapping("/password")
    public UserResponse changePassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest body,
                                       HttpServletResponse response) {
        User updated = accounts.changePassword(user.id(), body);
        cookies.write(response, jwt.issue(updated), jwt.lifetime());
        return UserResponse.of(updated);
    }
}
