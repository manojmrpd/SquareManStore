package com.squareman.profile.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.squareman.profile.config.WebMailConfig;
import com.squareman.profile.config.WebSecurityUtility;
import com.squareman.profile.constants.LocaleConstants;
import com.squareman.profile.entity.PasswordResetToken;
import com.squareman.profile.entity.Role;
import com.squareman.profile.entity.User;
import com.squareman.profile.entity.UserRole;
import com.squareman.profile.service.UserSecurityService;
import com.squareman.profile.service.UserService;

@Controller
public class ProfileController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);
	private static final String HOME_PAGE = "index";
	private static final String LOGIN_PAGE = "login";
	private static final String MYPROFILE_PAGE = "myProfile";
	private static final String STORE_LOCATOR = "storeLocator";

	@Autowired
	UserService userService;

	@Autowired
	UserSecurityService userSecurityService;

	@Autowired
	private WebMailConfig webMailConfig;

	@Autowired
	private JavaMailSender mailSender;

	@RequestMapping("/")
	public String homePage() {
		LOGGER.debug("Loading the Home Page of Manstore Application.");
		return HOME_PAGE;
	}

	@RequestMapping("/login")
	public String login(Model model) {
		model.addAttribute("classActiveLogin", true);
		return LOGIN_PAGE;
	}

	@RequestMapping(value = "/newUser", method = RequestMethod.POST)
	public String newUserPost(HttpServletRequest request, @ModelAttribute("email") String userEmail,
			@ModelAttribute("username") String username, @ModelAttribute("firstName") String firstName,
			@ModelAttribute("lastName") String lastName, Model model) throws Exception {

		LOGGER.debug("Entering newUserPost() method in AccountController class");

		// STEP-1 : Add user attributes to model
		if (username != null && !username.isEmpty() && userEmail != null && !userEmail.isEmpty()) {
			addUserAttributesToModel(firstName, lastName, username, userEmail, model);
		}

		// STEP-2 : Check if user exists in database
		if (validateIfUserExists(username, userEmail, model)) {
			return LOGIN_PAGE;
		}

		// STEP-3: If user does not exists in database, save the new user details in
		// database.
		User user = saveUserDetails(userEmail, username, firstName, lastName);

		// STEP-4: Send Email Notification to user email address
		sendEmailNotification(request, model, user);
		model.addAttribute("emailSent", "true");

		return LOGIN_PAGE;
	}

	@RequestMapping("/newUser")
	public String newUser(Locale locale, @RequestParam("token") String token, Model model) {
		PasswordResetToken passToken = userService.getPasswordResetToken(token);

		if (passToken == null) {
			String message = "Invalid Token.";
			model.addAttribute("message", message);
			return "redirect:/badRequest";
		}
		User user = passToken.getUser();
		String username = user.getUsername();
		UserDetails userDetails = userSecurityService.loadUserByUsername(username);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);

		model.addAttribute("user", user);

		model.addAttribute("classActiveEdit", true);

		return MYPROFILE_PAGE;
	}

	@RequestMapping("/myProfile")
	public String myProfile(Model model, Principal principal) {

		LOGGER.debug("Entering myProfile() method in AccountController class");
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		List<String> stateList = LocaleConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("classActiveEdit", true);
		return MYPROFILE_PAGE;
	}

	@RequestMapping(value = "/updateUserInfo", method = RequestMethod.POST)
	public String updateUserInfo(@ModelAttribute("user") User user, @ModelAttribute("newPassword") String newPassword,
			Model model) throws Exception {
		Optional<User> currentUserOps = userService.findById(user.getId());
		User currentUser = currentUserOps.get();

		if (currentUser == null) {
			throw new Exception("User not found");
		}

		/* check username already exists */
		/* check username already exists */
		if (userService.findByUsername(user.getUsername()) != null) {
			if (userService.findByUsername(user.getUsername()).getId() != currentUser.getId()) {
				model.addAttribute("usernameExists", true);
				return MYPROFILE_PAGE;
			}
		}

//		update password
		if (newPassword != null && !newPassword.isEmpty() && !newPassword.equals("")) {
			BCryptPasswordEncoder passwordEncoder = WebSecurityUtility.passwordEncoder();
			String dbPassword = currentUser.getPassword();
			// if(passwordEncoder.matches(user.getPassword(), dbPassword)){
			if (dbPassword != null && user.getPassword() != null && dbPassword.equals(user.getPassword())) {
				currentUser.setPassword(passwordEncoder.encode(newPassword));
			} else {
				model.addAttribute("incorrectPassword", true);

				return MYPROFILE_PAGE;
			}
		}

		currentUser.setFirstName(user.getFirstName());
		currentUser.setLastName(user.getLastName());
		currentUser.setUsername(user.getUsername());
		currentUser.setEmail(user.getEmail());

		userService.save(currentUser);

		model.addAttribute("updateSuccess", true);
		model.addAttribute("user", currentUser);
		model.addAttribute("classActiveEdit", true);

		UserDetails userDetails = userSecurityService.loadUserByUsername(currentUser.getUsername());

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);

		return MYPROFILE_PAGE;
	}

	@RequestMapping("/storeLocator")
	public String hours() {
		return STORE_LOCATOR;
	}

	@RequestMapping("/faq")
	public String faq() {
		return "faq";
	}

	private User saveUserDetails(String userEmail, String username, String firstName, String lastName)
			throws Exception {

		LOGGER.debug("Entering saveUserDetails() method in AccountController class");
		User user = new User();
		user.setUsername(username);
		user.setEmail(userEmail);
		user.setFirstName(firstName);
		user.setLastName(lastName);

		// generate password using hashing algorithm
		String password = WebSecurityUtility.randomPassword();
		String encryptedPassword = WebSecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);

		Role role = new Role();
		role.setRoleId(1);
		role.setName("MANSTONE_USER");
		Set<UserRole> userRoles = new HashSet<>();
		userRoles.add(new UserRole(user, role));

		userService.createUser(user, userRoles);
		return user;
	}

	private void addUserAttributesToModel(String firstName, String lastName, String username, String userEmail,
			Model model) {
		LOGGER.debug("Entering addUserAttributes() method in AccountController class");

		model.addAttribute("firstName", firstName);
		model.addAttribute("lastName", lastName);
		model.addAttribute("username", username);
		model.addAttribute("email", userEmail);
		model.addAttribute("classActiveNewAccount", true);

	}

	private void sendEmailNotification(HttpServletRequest request, Model model, User user) throws MailException {
		String token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, token);
		SimpleMailMessage email = webMailConfig.constructResetTokenEmailTemplate(request, token, user);
		mailSender.send(email);
	}

	private boolean validateIfUserExists(String username, String userEmail, Model model) {
		LOGGER.debug("Entering validateIfUserExists() method in AccountController class");
		if (userService.findByUsername(username) != null) {
			model.addAttribute("usernameExists", true);
			return true;
		}

		/*
		 * if (userService.findByEmail(userEmail) != null) {
		 * model.addAttribute("emailExists", true); return true; }
		 */
		return false;

	}

}
