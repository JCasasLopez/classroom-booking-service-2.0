package dev.jcasaslopez.booking.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserContext {
	
private static final Logger logger = LoggerFactory.getLogger(UserContext.class);
    
    private static final ThreadLocal<UserIdentity> userHolder = new ThreadLocal<>();

    private record UserIdentity(String email, Integer idUser) {}

    private UserContext() {

    }

    public static void setContext(String email, Integer idUser) {
        if (email == null || idUser == null) {
            throw new IllegalArgumentException("Neither email nor idUser can be null in UserContext");
        }
        logger.debug("Setting UserContext for thread {}: email={}, idUser={}", 
                Thread.currentThread().getName(), email, idUser);
        userHolder.set(new UserIdentity(email, idUser));
    }

    public static String getEmail() {
        UserIdentity identity = userHolder.get();
        if (identity == null) {
            throw new IllegalStateException("No user context present in ThreadLocal for thread: " + Thread.currentThread().getName());
        }
        return identity.email();
    }
    
    public static Integer getIdUser() {
        UserIdentity identity = userHolder.get();
        if (identity == null) {
            throw new IllegalStateException("No user context present in ThreadLocal for thread: " + Thread.currentThread().getName());
        }
        return identity.idUser();
    }

    public static void clear() {
        if (logger.isDebugEnabled() && userHolder.get() != null) {
            logger.debug("Clearing UserContext for thread {}: email={}", 
                    Thread.currentThread().getName(), userHolder.get().email());
        }
        userHolder.remove();
    }

}
