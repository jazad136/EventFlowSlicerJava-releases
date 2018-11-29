package edu.unl.cse.efs.view.ft;

import edu.umd.cs.guitar.model.data.Widget;

/**
 * This display icon class correctly parses the information passed to the constructor into
 * parts that can be used for display.
 * @author jsaddle
 *
 */
public class DisplayIcon implements Comparable<DisplayIcon>
	{
		String toDisplay;
		String compareString;
		String firstString;
		String secondString;
		String thirdString;
		boolean primed;

		public DisplayIcon()
		{
			toDisplay = "";
			compareString = "";
			secondString = "";
			thirdString = "";
			primed = false;
		}
		public DisplayIcon(Widget w)
		{
			toDisplay = w.toString();
			String paramString;
			if(w.getParameter() != null && !w.getParameter().isEmpty()) {
				paramString = w.getParameter();
			}
			else
				paramString = "";
			compareString = w.getName() + w.getEventID() + w.getWindow() + paramString;
			firstString = w.getName();
			secondString = w.getWindow();
			thirdString = w.getEventID();
			primed = false;
		}

		public DisplayIcon(String toDisplay)
		{
			this.toDisplay = toDisplay;

			compareString = toDisplay;
			firstString = "";
			secondString = "";
			thirdString = compareString;
			primed = false;
		}
		public DisplayIcon(String toDisplay, int number)
		{
			this.toDisplay = toDisplay + " " + number;
			firstString = "";
			secondString = "";
			String pad = "";
			if(number < 10) pad += "0";
			if(number < 100) pad += "0";
			compareString = toDisplay + " " + pad + number;
			thirdString = compareString;
			primed = false;
		}
		public boolean setPrimed()
		{
			boolean primeBefore = primed;
			primed = true;
			return primeBefore;
		}
		/**
		 * Return true if and only if the display icon that would be created from
		 * w matches this icon semantically.
		 */
		public boolean matchesIconOf(Widget w){ return this.equals(new DisplayIcon(w)); }



		public static String getPrimedString(String firstString, String secondString, String thirdString)
		{
			// shorten the window string.
			String sStr = secondString.length() > 8 ? secondString.substring(0,8) + "..." : secondString + "...";
			// use all three parts in this new string.
			return firstString + " [" + sStr + "] (" + thirdString + ")";
		}

		public static String getWindowedIconString(Widget w)
		{
			return getPrimedString(w.getName(), w.getWindow(), w.getEventID());
		}

		/**
		 * Return a string version
		 */
		public String toString()
		{
			if(primed)
				return getPrimedString(firstString, secondString, thirdString);
			return toDisplay;
		}

		public int compareTo(DisplayIcon other)
		{
			return compareString.compareTo(other.compareString);
		}

		/**
		 * primeString is a factor in this hashcode method.
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((compareString == null) ? 0 : compareString.hashCode());
			return result;
		}

		/**
		 * primeString is a factor in this equals method
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof DisplayIcon))
				return false;
			DisplayIcon other = (DisplayIcon) obj;
			if (compareString == null) {
				if (other.compareString != null)
					return false;
			} else if (!compareString.equals(other.compareString)) {
				return false;
			}
			return true;
		}
	}