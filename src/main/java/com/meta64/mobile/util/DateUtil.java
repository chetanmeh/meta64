package com.meta64.mobile.util;

import java.util.HashMap;
import java.util.Locale;

public class DateUtil {
	/** Used to format date values */
	// public static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";
	public static final String DATE_FORMAT_WITH_TIMEZONE = "yyyy/MM/dd hh:mm:ss a z";
	public static final String DATE_FORMAT_NO_TIMEZONE = "yyyy/MM/dd hh:mm:ss a";

	/** Used to format date values */
	public static final Locale DATE_FORMAT_LOCALE = Locale.US;

	private static final HashMap<String, String> zoneMap = new HashMap<String, String>();

	/*
	 * Date APIs have deprecated the use of short abbreviations for timezones, but we preferr to
	 * still have them shown for at least United States timezones, so we have to implement our own
	 * way of detecting the proper one and also consider proper Daylight Savings Time value, which
	 * is of course coming from the browser.
	 */
	static {
		zoneMap.put("-4S", "AST"); // ATLANTIC STANDARD TIME UTC - 4
		zoneMap.put("-5S", "EST"); // EASTERN STANDARD TIME UTC - 5
		zoneMap.put("-4D", "EDT"); // EASTERN DAYLIGHT TIME UTC - 4
		zoneMap.put("-6S", "CST"); // CENTRAL STANDARD TIME UTC - 6
		zoneMap.put("-5D", "CDT"); // CENTRAL DAYLIGHT TIME UTC - 5
		zoneMap.put("-7S", "MST"); // MOUNTAIN STANDARD TIME UTC - 7
		zoneMap.put("-6D", "MDT"); // MOUNTAIN DAYLIGHT TIME UTC - 6
		zoneMap.put("-8S", "PST"); // PACIFIC STANDARD TIME UTC - 8
		zoneMap.put("-7D", "PDT"); // PACIFIC DAYLIGHT TIME UTC - 7
	}

	/* I'm too lazy right not to code these. Will properly display as GMT-NNN */
	// AKST ALASKA TIME UTC - 9
	// AKDT ALASKA DAYLIGHT TIME UTC - 8
	// HST HAWAII STANDARD TIME UTC - 10
	// HAST HAWAII-ALEUTIAN STANDARD TIME UTC - 10
	// HADT HAWAII-ALEUTIAN DAYLIGHT TIME UTC - 9
	// SST SAMOA STANDARD TIME UTC - 11
	// SDT SAMOA DAYLIGHT TIME UTC - 10
	// CHST CHAMORRO STANDARD TIME UTC +10

	public static String getUSTimezone(int hours, boolean dst) {
		return zoneMap.get(String.valueOf(hours) + (dst ? "D" : "S"));
	}

	/* This is not the most elegant solution but appears to work */
	public static String getTimezoneFromOffset(int offsetMinutes) {

		int hours = offsetMinutes / 60;
		int mins = offsetMinutes - hours * 60;
		String minStr = String.valueOf(mins);
		if (minStr.length() < 2) {
			minStr = "0" + minStr;
		}

		// //////////////////
		// String[] timeZones = TimeZone.getAvailableIDs();
		// for (String timeZone : timeZones) {
		// TimeZone tz = TimeZone.getTimeZone(timeZone);
		//
		// // long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
		// // long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset())
		// // - TimeUnit.HOURS.toMinutes(hours);
		// //
		// // String timeZoneString = String.format("( GMT %d:%02d ) %s(%s)", hours,
		// // minutes, tz.getDisplayName(), timeZone);
		// // tzList.add(timeZoneString);
		// int h = tz.getRawOffset() / (1000 * 60 * 60);
		// int m = (tz.getRawOffset() - h * 1000 * 60 * 60)/(1000*60);
		// System.out.println("tz: " + h + ":" + m + " name: " + tz.getDisplayName());
		// }
		// // //////////////////
		//
		String gmtZoneStr = "GMT-" + hours + ":" + minStr;
		//
		// TimeZone z = TimeZone.getTimeZone(gmtZoneStr);
		// String zoneString = z.getDisplayName();
		// log.debug("ZoneString: " + zoneString);

		return gmtZoneStr;
	}
}
