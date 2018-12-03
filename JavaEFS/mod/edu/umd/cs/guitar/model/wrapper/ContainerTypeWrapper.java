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
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.ContentsType;
import edu.umd.cs.guitar.model.data.ComponentType;

import edu.umd.cs.guitar.exception.PropertyInconsistentException;
import edu.umd.cs.guitar.util.GUITARLog;


/**
 * Wrapper for class ContainerType.
 */
public class ContainerTypeWrapper
{
	ContainerType dContainerType;

	public ContainerType
	getContainerType() {
		return dContainerType;
	}

	public
	ContainerTypeWrapper(ContainerType dContainerType)
	{
		this.dContainerType = dContainerType;
	}


	/**
	 * Compares and returns the difference between this object and the
	 * specified ContainerType 'obj'.
	 *
	 * First compares the base class ComponentType then compares this
	 * class ContainerType. The results are combined into a ContainerType
	 * amd returned.
	 *
    * @return   Returns a List of two Objects.
    *             The first Object is a boolean: false=match, true=mismatch
    *             The second Object is a ContainerType with match/mismatch
	 */
	public List<Object> compare(Object obj) throws PropertyInconsistentException
	{
		int i;

		// This
		assert(dContainerType != null);
		ContentsType contents = dContainerType.getContents();
		List<ComponentType> componentList = contents.getWidgetOrContainer();
	
		// Other
		ContainerType oContainer = (ContainerType)obj;	
		ContentsType oContents
			= (oContainer == null) ? null : oContainer.getContents();
		List<ComponentType> oComponentList
			= (oContents == null) ? null : oContents.getWidgetOrContainer();

		GUITARLog.log.debug("BEGIN Container");

		// First compare the parent class ComponentType
		List<Object> diffListBase;
		try {
			diffListBase
				= (new ComponentTypeWrapper((ComponentType)dContainerType)).
					compare((ComponentType)oContainer);
		} catch (PropertyInconsistentException e) {
			/*
			 * Mismatch in property name, caused when comparing
			 * components which likely cannot be compared.
			 */
			GUITARLog.log.debug("DONE Container - base inconsistent property");
			throw e;
		}

		// Second compare ContainerType specific members
		ContentsType diffContents = new ContentsType();
		List<ComponentType> diffComponentList = new ArrayList<ComponentType>();

		// If "other" is null
		boolean retStatus = false;
		if (oComponentList == null) {
			diffContents.setWidgetOrContainer(componentList);
			retStatus = true;
		} else {
			ComponentType diffComp = null;
			int jCur = 0;
			int numFound = 0;
			for (i = 0; i < componentList.size(); i++) {

				int j;
				boolean found = false;
				List<Object> diffList = null;
				for (j = jCur; j < oComponentList.size(); ) {
					try {
						diffList = diffComponents(componentList, i,
											oComponentList, j);
						found = true;
						numFound++;
						j++;
						break;
					} catch (PropertyInconsistentException e) {
						j++;
						continue;
					}
				} // for j

				if (found) {
					int k;
					/*
					 * All elements [jCur, j-1) from "other" could not be matched.
                * Hence they must be new elements in "other".
					 */
					for (k = jCur; k < j - 1; k++) {
						diffComponentList.add(oComponentList.get(k));
					}
					// Now update 'jCur' to the next element
					jCur = j;

					// After adding unmatched items from "other", add matched item
					retStatus = retStatus || (Boolean)diffList.get(0);
					diffComponentList.add((ComponentType)diffList.get(1));

				} else if (!found) {
					/*
					 * If "this" could not be matched with any other component
					 * from "other", then "this" must be an unmatched component.
					 * Hence, add "this" component into "diff" list.
					 */
					retStatus = true;
					diffComponentList.add(componentList.get(i));
				}

			} // for i

			/*
			 * All elements [jCur, j-1) from "other" could not be matched.
			 * Hence they must be new elements in "other".
			 */
			int k;
			for (k = jCur; k < oComponentList.size(); k++) {
				diffComponentList.add(oComponentList.get(k));
			}

			diffContents.setWidgetOrContainer(diffComponentList);

			/*
			 * If many components could not be matched, then
 			 * "this" might be comparing with a new "other". Throw an exception
			 * informing the caller that there were too many excpetions.
			 */
			if (numFound == 0) {
				GUITARLog.log.debug("Unable to match component. numFound = "
											+ numFound);
				GUITARLog.log.debug("DONE Container - inconsistent property");
				throw new PropertyInconsistentException();
			}
		} // if "oComponentList == null"

		GUITARLog.log.debug("DONE Container");

		// Construct and return (boolean, ContainerType)
		ContainerType retContainer = new ContainerType();
		// Consolidate retStatus
		retStatus = retStatus || (Boolean)diffListBase.get(0);
		// Add base component diff
		if (((ComponentType)diffListBase.get(1)).getAttributes() != null) {
			retContainer.setAttributes(
				((ComponentType)diffListBase.get(1)).getAttributes());
		}
		// Add this ContainerType diff
		retContainer.setContents(diffContents);
		// Construct return object
		List<Object> retList = new ArrayList<Object>();
		retList.add(retStatus);
		retList.add(retContainer);

		return retList;
	}

	private List<Object> diffComponents(List<ComponentType> componentList, 
			int i, List<ComponentType> oComponentList, int j) throws PropertyInconsistentException 
	{
		if (!componentList.get(i).getClass().
			  equals(oComponentList.get(j).getClass())) {
			throw new PropertyInconsistentException();
		}

		List<Object> retList = null;
		ComponentType comp = componentList.get(i);

		assert(i < componentList.size());
		assert(j < oComponentList.size());

		try {
			// For containers
			if (comp instanceof ContainerType) {
				retList
				= (new ContainerTypeWrapper((ContainerType)comp)).
					compare(oComponentList.get(j));
			} else if (comp instanceof ComponentType) {
				retList
				= (new ComponentTypeWrapper(comp)).
					compare(oComponentList.get(j));
			}
		} catch (PropertyInconsistentException e) {
			throw e;
		}
		return retList;
	}

} // End of class
