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
import java.util.Hashtable;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.SwingUtilities;

import edu.umd.cs.guitar.exception.EventPerformException;
import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.JFCXComponent;


/**
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen
 * @author Jonathan Saddler</a>
 */
public class JFCSelectionHandler extends JFCEventHandler {

	Integer selectedIndex;

	/**
     *
     */
	public JFCSelectionHandler()
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.umd.cs.guitar.event.JEventHandler#actionPerformImp(edu.umd.cs.guitar
	 * .model.GXComponent)
	 */
	@Override
	protected void performImpl(GObject component,Hashtable<String, List<String>> optionalData)
	{
		// do nothing. This action needs a parameters object.
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.umd.cs.guitar.event.JFCEventHandler#actionPerformImp(edu.umd.cs.guitar
	 * .model.GXComponent, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void performImpl(GObject gComponent, Object parameters,Hashtable<String, List<String>> optionalData)
	{

		if (gComponent == null)
			return;

		if (parameters instanceof List<?>) {
			List<String> lParameter;
			try {
				lParameter = (List<String>) parameters;
			}
			catch(ClassCastException e) {
				lParameter = null;
			}

			if (lParameter == null)
				selectedIndex = 0;
			else {
				String sIndex;
				if(!lParameter.isEmpty())
					sIndex = lParameter.get(0);
				else
					sIndex = "0";

				try {
					selectedIndex = Integer.parseInt(sIndex);
				}
				catch (Exception e)
				{
					selectedIndex = 0;
				}
			}
		}

		final Component component = getComponent(gComponent);

		AccessibleContext lContext = component.getAccessibleContext();
		if(lContext == null)
			return;
		final AccessibleSelection aSelection = lContext.getAccessibleSelection();
		if (aSelection == null)
			return;

		try {
			// the accessible way
			SwingUtilities.invokeAndWait(new Runnable() {public void run() {
				aSelection.clearAccessibleSelection();
				aSelection.addAccessibleSelection(selectedIndex);
			}});
		}
		catch(InterruptedException e) {
			throw new EventPerformException();
		}
		catch (SecurityException e) {
			System.err.println("Could not complete selection on component due to SecurityException.");
			throw new EventPerformException();
		}
		catch(Exception e) {
			System.err.println("Could not complete selection on component.\n"
					+ "Cause: " + e.getCause() + "\n"
					+ "Message: " + e.getMessage());
			throw new EventPerformException();
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.guitar.event.GEvent#isSupportedBy(edu.umd.cs.guitar.model.GComponent)
	 */
	@Override
	public boolean isSupportedBy(GObject gComponent)
	{
		if (!(gComponent instanceof JFCXComponent))
			return false;
		GEvent gFilterEvent;

		JFCXComponent jComponent = (JFCXComponent) gComponent;
		Component component = jComponent.getComponent();
		AccessibleContext aContext = component.getAccessibleContext();
		if (aContext==null)
			return false;

		// added the lines below for pagetablists and combo boxes
		gFilterEvent = new JFCSelectFromParent();
		if(gFilterEvent.isSupportedBy(gComponent))
			return false;

		Object event = aContext.getAccessibleSelection();
		if (event == null)
			return false;

		if(aContext.getAccessibleRole().equals(AccessibleRole.MENU_BAR))
			return false; // jsaddle: we just don't support menubars as selection events.
		return true;
	}
}
