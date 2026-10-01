package dev.jcasaslopez.booking.util;

public class BookingEndpoints {
	
	public static final String BOOKINGS = "/bookings";
	public static final String CANCEL = "/bookings/{idBooking}";
	
	public static final String CLASSROOM_AVAILABILITY = "/classrooms/{idClassroom}/availability";
	public static final String CLASSROOMS_AVAILABLE = "/classrooms/available";
	
	public static final String WATCH_ALERTS = "/watch-alerts";        
    public static final String TARGET_BOOKING = "/watch-alerts/target-booking";
	
	public static final String GENERATE_TOKEN = "/generate-token";
}
