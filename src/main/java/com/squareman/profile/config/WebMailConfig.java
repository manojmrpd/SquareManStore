package com.squareman.profile.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import com.squareman.profile.entity.User;

@Component
public class WebMailConfig {

	@Autowired
	private Environment env;

	public SimpleMailMessage constructResetTokenEmailTemplate(HttpServletRequest request, String token, User user) {
		String contextPath = "http://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath();
		String url = contextPath + "/newUser?token=" + token;
		String messageBody = "Hi "+user.getFirstName()+ " \n \n"
				+ "Thanks for Registering with Square Man Store!\n \n"
				+ "Please click on below url to verify your email address and create a new Password. \n"
				+ ""+url+"\n\n"
				+ "Your Current password is: \n"+user.getPassword()+"\n\n"
				+  "Best Regards, \n"
				+ "Square Man Store, India";
		SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(user.getEmail());
		email.setSubject("Square Man Store India - Create New Password");
		email.setText(messageBody);
		email.setFrom(env.getProperty("support.email"));
		return email;

	}
}
