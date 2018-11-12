/*	
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *	the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *	conditions:
 * 
 *	The above copyright notice and this permission notice shall be included in all copies or substantial 
 *	portions of the Software.
 *
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *	LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *	EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *	THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.umd.cs.guitar.event;

import java.awt.Component;

import javax.accessibility.AccessibleRole;

import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.JFCXComponent;

/**
 * 
 * Abstract class for all JFC events in GUITAR.
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public abstract class JFCEventHandler extends GThreadEvent {
	/**
	 * A helper method to get the real JFC Accessible object from a
	 * <code> GXComponent </code>
	 * 
	 * @param gComponent
	 * @return Accessible
	 */
	// protected Accessible getAccessible(GComponent gComponent) {
	// JFCXComponent jxComponent = (JFCXComponent) gComponent;
	// return jxComponent.getAComponent();
	// }

	protected Component getComponent(GObject gComponent) {
		JFCXComponent jxComponent = (JFCXComponent) gComponent;
		return jxComponent.getComponent();
	}
	
	/**
	 * Returns a string identifying the type of hover that JFCEvent handlers will 
	 * be able to actuate on the javaComponent specified.
	 * There are two levels of valid hover support that can be returned from this method.
	 * : "basic" and "parental". <br>
	 * <br>
	 * If JFCEventHandlers do not support hover on the component specified, the empty string is returned.
	 * @return
	 */
	public static String hoverTypeAvailable(Component javaComponent)
	{
		AccessibleRole myRole;
		if(javaComponent == null || javaComponent.getAccessibleContext() == null)
			return "";
		myRole = javaComponent.getAccessibleContext().getAccessibleRole();
		if(myRole == null)
			return "";
		String hoverType = "";
		if(myRole.equals(AccessibleRole.TEXT))
			hoverType = "parental";
		else if(myRole.equals(AccessibleRole.PUSH_BUTTON) ||
				myRole.equals(AccessibleRole.RADIO_BUTTON) ||
				myRole.equals(AccessibleRole.MENU) ||
				myRole.equals(AccessibleRole.MENU_ITEM) ||
				myRole.equals(AccessibleRole.CHECK_BOX) ||
				myRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
			hoverType = "basic";
		}
		else if(myRole.equals(AccessibleRole.COMBO_BOX)) {
			if(JFCXComponent.hasChildren(javaComponent))
				hoverType = "basic"; // this must be top level.
			else
				hoverType = "parental"; // this must be an element in the list
		}
		
		else if(myRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
			if(JFCXComponent.hasChildren(javaComponent))
				hoverType = "basic"; // this must be the top level.
			// otherwise, no support
		}
		
		else if(myRole.equals(AccessibleRole.PANEL)) {
			if(JFCXComponent.hasListeners(javaComponent, "button"))
				hoverType = "basic";
		}
		
		else if(myRole.equals(AccessibleRole.LIST))
			hoverType = "parental";
		
		return hoverType;
	}

}
