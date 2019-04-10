/*
 *  Copyright (c) 2009-@year@. The  GUITAR group  at the University of
 *  Maryland. Names of owners of this group may be obtained by sending
 *  an e-mail to atif@cs.umd.edu
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files
 *  (the "Software"), to deal in the Software without restriction,
 *  including without limitation  the rights to use, copy, modify, merge,
 *  publish,  distribute, sublicense, and/or sell copies of the Software,
 *  and to  permit persons  to whom  the Software  is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO  EVENT SHALL THE  AUTHORS OR COPYRIGHT  HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR  OTHER LIABILITY,  WHETHER IN AN  ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.umd.cs.guitar.model.wrapper;

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.exception.PropertyInconsistentException;
import edu.umd.cs.guitar.util.GUITARLog;


/**
 * @author Bao Nguyen </a>
 */
public class PropertyTypeWrapper {
    PropertyType property;

    /**
     * @param property
     */
    public PropertyTypeWrapper(PropertyType property) {
        super();
        this.property = property;
    }

 	@Override
	public boolean
	equals(Object obj)
	{
		if (!(obj instanceof PropertyTypeWrapper)) {
			return false;
		}
		PropertyTypeWrapper oPropertyWrapper = (PropertyTypeWrapper) obj;
		PropertyType oProperty = oPropertyWrapper.property;

		String sName = property.getName();
		String oName = oProperty.getName();

		if (sName == null || oName == null) {
			return false;
		}

		if (!sName.equals(oName)) {
			return false;
		}

		List<String> lValue = property.getValue();
		List<String> loValue = oProperty.getValue();

		if (lValue == null && loValue == null) {
			return true;
		} else if (lValue == null || loValue == null) {
			return false;
		} else {
			int iLength = lValue.size();
			int ioLength = loValue.size();

			if (iLength != ioLength) {
				return false;
			}
			for (int i = 0; i < iLength; i++) {
				String sValue = lValue.get(i);
				String soValue = loValue.get(i);
				if (!sValue.equals(soValue)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Compere two AttributesType objects, the current object and a specified
	 * object, and return a list of properties which do not match.
	 *
	 * For example:
	 * Note: Properties being compared MUST have the same name. This implicitly
	 *       implies that the order of properties must be the same.
	 *       If a pair of property name does not match, a
	 *       PropertyInconsistentException is thrown. This implies that two
	 *       non-comparable GUI components are being compared. This exception
	 *       is the basis for determining the presence of non-comparable
	 *       GUI components (potentially new or deleted component).
	 *
    * @param    Other PropertyType to compare with
	 * @return   Returns a List of two Objects.
	 *				    The first Object is a boolean: false=match, true=mismatch
	 *              The second Object is a PropertyType with match/mismatch
	 */
	public List<Object> compare(Object obj) throws PropertyInconsistentException
	{
		boolean retStatus = false;

		// For storing the "diff"
		List<String> retValue = new ArrayList<String>();
		PropertyType retProperty = new PropertyType();
		List<Object> retList = new ArrayList<Object>();

		assert(property != null);
		assert(obj instanceof PropertyType || obj == null);

		PropertyType oProperty = (PropertyType)obj;

		// If one if null
		if (oProperty == null) {
			retStatus = true;

			// Prepare return PropertyType
			retProperty.setName(property.getName());
			retProperty.setValue(property.getValue());

			// Create return List
			retList.add(retStatus);
			retList.add(retProperty);
			return retList;
		}

		String sName = property.getName();
		String oName = oProperty.getName();

		List<String> lValue = property.getValue();
		List<String> loValue = oProperty.getValue();

		// Name must be valid and of the same name
		assert(lValue != null && loValue != null);
		assert(sName != null && oName != null);

		/*
		 * If there is a mismatch in property name, it means different
		 * different components are being compared. Bail out with an
		 * exception.
		 */
		if (!sName.equals(oName)) {
			GUITARLog.log.debug("[" + sName + "," + oName + "] **");
			throw new PropertyInconsistentException();
		}

		int iLength = lValue.size();
		int ioLength = loValue.size();

		for (int i = 0; i < Math.max(iLength, ioLength); i++) {
			boolean add = false;
			String sValue = (i < iLength) ? lValue.get(i) : "";
			String soValue = (i < ioLength) ? loValue.get(i) : "";

			// Compare value.
			if (!sValue.equals(soValue)) {
				add = true;
				// Do not consider ID when matching properties
				if (!sName.equals("ID")) {
					// true => mismatch
					retStatus = true;
				}
			}
			// Unconditionally add ID for reference
			if (sName.equals("ID")) {
				add = true;
			}

			if (add) {
				GUITARLog.log.debug("[" + sName + "," + oName + "] " +
			   						  "[" + sValue + "," + soValue + "]");
				if (sValue.equals("") && !soValue.equals("")) {
					retValue.add(soValue);
				}
				if (!sValue.equals("") && soValue.equals("")) {
					retValue.add(sValue);
				}
				if (!sValue.equals("") && !soValue.equals("")) {
					retValue.add(sValue + ":" + soValue);
				}
			}
		}

		// Create return PropertyType
		retProperty.setName(sName);
		retProperty.setValue(retValue);

		// Create return List
		retList.add(retStatus);
		retList.add(retProperty);

		return retList;
	}

	public String toString()
	{
		if(property != null)
			return "ptw:\n" + property.toString();
		else
			return "ptw: no properties";
	}
} // End of class
