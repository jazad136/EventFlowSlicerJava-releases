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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.accessibility.*;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.JFCXComponent;


/**
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 * modified by Jonathan Saddler
 * 
 *	
 */
public class JFCMenuReopenHandler extends JFCEventHandler {
	
	public static final int miniEventWaitTime = 1000;
	
	public JFCMenuReopenHandler() 
	{ 
		// Constructor does nothing. 
	}

	@Override
	public void performImpl(GObject gComponent, Hashtable<String, List<String>> optionalData) 
	{
		// delegate to parameter argument based implementation
		performImpl(gComponent, new MenuElement[0], optionalData);
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.event.JFCEventHandler#actionPerformImp(edu.umd.cs.guitar
	 * .model.GXComponent, java.lang.Object)
	 */
	/**
	 * Takes a set of parameters, but ignores the GObject object.
	 */
	@Override
	protected void performImpl(GObject gComponent, Object parameters, Hashtable<String, List<String>> optionalData) 
	{	
		// gauntlet stage 1: if parameters is not a menu element
		if(!(parameters instanceof MenuElement[]))
			return;
		
		final MenuElement[] path = (MenuElement[]) parameters;
		if(path.length == 0)
			return;
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {public void run() {
			MenuSelectionManager.defaultManager().setSelectedPath(path);
		}});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.event.GEvent#isSupportedBy(edu.umd.cs.guitar.model.
	 * GComponent)
	 */
	@Override
	public boolean isSupportedBy(GObject gComponent) {

		if (!(gComponent instanceof JFCXComponent))
			return false;
		JFCXComponent jComponent = (JFCXComponent) gComponent;
		Component component = jComponent.getComponent();
		AccessibleContext aContext = component.getAccessibleContext();
		if (aContext == null)
			return false;
		
		GEvent gFilterEvent;
		gFilterEvent= new JFCEditableTextHandler();
		
		if (gFilterEvent.isSupportedBy(gComponent) && !aContext.getAccessibleRole().equals(AccessibleRole.PANEL))
			return false; // only panels dual actions, they can only be supported by ETH.
		
		Object event;
		// Action
		event = aContext.getAccessibleAction();
		if (event != null)
			return true;
		else if(JFCXComponent.hasListeners(component, "button") && aContext.getAccessibleRole().equals(AccessibleRole.PANEL))
			return true; // you have to be a panel to have this kind of mouse listener event.
		return false;
	}
}
