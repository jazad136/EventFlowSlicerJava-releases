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
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.accessibility.*;

import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCXComponent;


/**
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 * modified by Jonathan Saddler
 * 
 *	
 */
public class JFCActionHandler extends JFCEventHandler {
	
	public static final int miniEventWaitTime = 1000;
	
	public JFCActionHandler() 
	{
	}

	@Override
	public void performImpl(GObject gComponent, Hashtable<String, List<String>> optionalData) 
	{
		performImpl(gComponent, new ArrayList<String>(), optionalData);
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.event.JFCEventHandler#actionPerformImp(edu.umd.cs.guitar
	 * .model.GXComponent, java.lang.Object)
	 */
	/**
	 * Takes an extra parameter object, but ignores the parameters.
	 */
	@Override
	protected void performImpl(GObject gComponent, Object parameters, Hashtable<String, List<String>> optionalData) 
	{
		if (gComponent == null) 
			return;
		
		Component component = getComponent(gComponent);

		final AccessibleContext componentContext = component.getAccessibleContext();
		if (componentContext == null)
			return;
		
		final AccessibleAction componentActions = componentContext.getAccessibleAction();
		if (componentActions == null) {
			int offsetX = -1, offsetY = -1;
			boolean paramsPresent = parameters != null && parameters instanceof List;
			boolean paramsValid = false;
			String offXString = "", offYString = "";
			if(paramsPresent) {
				List<?> params = (List<?>)parameters;
				if(!params.isEmpty() && !(params.get(0) == null) && !(params.get(0).toString().isEmpty())) {
					StringTokenizer paramString = new StringTokenizer(params.remove(0).toString(), GUITARConstants.NAME_SEPARATOR);
					try {
						String command = paramString.nextToken();
						if(command.equals("Click")) {
							offXString = paramString.nextToken();
							offYString = paramString.nextToken();
							Integer.parseInt(offXString);
							Integer.parseInt(offYString);
							paramsValid = true;
						}
					} catch(NoSuchElementException | NumberFormatException e) {
						paramsValid = false;
					}
				}
			}
			if(paramsValid) {
				offsetX = Integer.parseInt(offXString);
				offsetY = Integer.parseInt(offYString);
			}
			else {
				offsetX = 0;
				offsetY = 0;
			}
//			int x = gComponent.getX() + offsetX;
//			int y = gComponent.getY() + offsetY;
			Component source = component;
			int x = offsetX;
			int y = offsetY;
			int pid = MouseEvent.MOUSE_PRESSED;
			int rid = MouseEvent.MOUSE_RELEASED;
			int cid = MouseEvent.MOUSE_CLICKED;
			
			long when = System.currentTimeMillis();
			int modifiers = 0;
			int clickCount = 1;
			boolean popupTrigger = false;
			final MouseEvent pe = new MouseEvent(source, pid, when, modifiers, x, y, clickCount, popupTrigger);
			final MouseEvent re = new MouseEvent(source, rid, when, modifiers, x, y, clickCount, popupTrigger);
			final MouseEvent ce = new MouseEvent(source, cid, when, modifiers, x, y, clickCount, popupTrigger);
			
			javax.swing.SwingUtilities.invokeLater(new Runnable() {public void run() {
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(pe);
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(re);
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ce);
			}});
		}
		else 
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() 
				{
					componentActions.doAccessibleAction(0);
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
		
		// if it's a text event and it's not a panel.
		if (gFilterEvent.isSupportedBy(gComponent) && !aContext.getAccessibleRole().equals(AccessibleRole.PANEL))
			return false; // only panels dual actions, they can only be supported by ETH.
		
		Object event;
		// if it has an accessible action. 
		event = aContext.getAccessibleAction();
		if (event != null)
			return true;
		// if it has a button related listener attached to it, and it's a panel. 
		else if(JFCXComponent.hasListeners(component, "button") && aContext.getAccessibleRole().equals(AccessibleRole.PANEL))
			return true; // you have to be a panel to have this kind of mouse listener event.
		return false;
	}
}
