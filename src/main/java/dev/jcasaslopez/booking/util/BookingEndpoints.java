package dev.jcasaslopez.booking.util;

public class BookingEndpoints {
	
	public static final String BOOK = "/bookings";
	public static final String BOOKINGS_BY_USER = "/bookings";
	public static final String CANCEL = "/bookings/{idBooking}/cancel";
	
	public static final String CLASSROOM_AVAILABILITY = "/classrooms/{idClassroom}/availability";
	public static final String CLASSROOMS_AVAILABLE = "/classrooms";
	
	public static final String ADD_WATCH_ALERT = "/watch-alerts/{idBooking}";        
	public static final String WATCH_ALERTS = "/watch-alerts";        
    public static final String TARGET_BOOKING = "/watch-alerts/{idClassroom}/target-booking";
	
	public static final String GENERATE_TOKEN = "/generate-token";
}
