package edu.unl.cse.efs.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeFormats {
	public static final String MILITARY_TIME_FORMAT = "MMM-dd-yyyy-kkmm";
	
	/**
	 * Taken from CogTool-Helper.
	 * @return
	 */
	public static String nowMilitary() 
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(MILITARY_TIME_FORMAT);
		return sdf.format(cal.getTime());
	}
	public static String ymdhsTimeStamp() {
        DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }
}
