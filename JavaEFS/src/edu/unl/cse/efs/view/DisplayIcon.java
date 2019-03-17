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
package edu.unl.cse.efs.view;

import edu.umd.cs.guitar.model.data.Widget;

public class DisplayIcon implements Comparable<DisplayIcon> 
	{
		String toDisplay;
		String primeString;
		String secondString;
		
		public DisplayIcon()
		{
			toDisplay = "";
			primeString = "";
			secondString = "";
		}
		public DisplayIcon(Widget w)
		{
			toDisplay = w.toString();
			primeString = w.getName() + w.getEventID() + w.getWindow();
			secondString = w.getName();
		}
		
		public DisplayIcon(String toDisplay)
		{
			this.toDisplay = toDisplay;
			primeString = toDisplay;
			secondString = "";
		}
		public DisplayIcon(String toDisplay, int number)
		{
			this.toDisplay = toDisplay + " " + number;
			secondString = "";
			String pad = "";
			if(number < 10) pad += "0";
			if(number < 100) pad += "0";
			primeString = toDisplay + " " + pad + number;
		}
		/**
		 * Return true if and only if the display icon that would be created from 
		 * w matches this icon semantically.
		 */
		public boolean matchesIconOf(Widget w){ return this.equals(new DisplayIcon(w)); }
		
		/**
		 * Return a string version
		 */
		public String toString()
		{
			return toDisplay;
		}
		
		public int compareTo(DisplayIcon other)
		{
			return primeString.compareTo(other.primeString);
		}
		
		/**
		 * primeString is a factor in this hashcode method.
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((primeString == null) ? 0 : primeString.hashCode());
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
			if (primeString == null) {
				if (other.primeString != null)
					return false;
			} else if (!primeString.equals(other.primeString)) {
				return false;
			}
			return true;
		}		
	}