/*******************************************************************************
 *    Copyright (c) 2018 Jonathan A. Saddler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Contributors:
 *     Jonathan A. Saddler - initial API and implementation
 *******************************************************************************/
package edu.unl.cse.jontools.string;

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
