package com.squareman.profile.service;

import java.util.Optional;
import java.util.Set;

import com.squareman.profile.entity.PasswordResetToken;
import com.squareman.profile.entity.User;
import com.squareman.profile.entity.UserRole;

public interface UserService {

	PasswordResetToken getPasswordResetToken(final String token);

	void createPasswordResetTokenForUser(final User user, final String token);

	User findByUsername(String username);

	User findByEmail(String email);

	Optional<User> findById(Long id);

	User createUser(User user, Set<UserRole> userRoles) throws Exception;

	User save(User user);
}
