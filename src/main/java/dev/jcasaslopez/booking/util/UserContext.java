package dev.jcasaslopez.booking.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserContext {
	
	private static final Logger logger = LoggerFactory.getLogger(UserContext.class);
	private static final ThreadLocal<String> emailHolder = new ThreadLocal<>();

	public static void setEmail(String email) {
		logger.debug("Setting email in UserContext for thread {}: {}", Thread.currentThread().getName(), email);
        emailHolder.set(email);
    }

    public static String getEmail() {
        String email = emailHolder.get();
        if (email == null) {
        	 throw new IllegalStateException("No email present in UserContext for thread: " + Thread.currentThread().getName());        }
        return email;
    }

    public static void clear() {
    	logger.debug("Clearing UserContext for thread {}: {}", Thread.currentThread().getName(), emailHolder.get());
        emailHolder.remove();
    }

}
