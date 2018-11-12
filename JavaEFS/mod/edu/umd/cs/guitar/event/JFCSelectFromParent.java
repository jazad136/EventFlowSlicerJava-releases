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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import edu.umd.cs.guitar.exception.EventPerformException;
import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.util.GUITARLog;

/**
 * Select a sub-item by calling a selection function form its parent
 * jsaddle: Items in which we might have to dig down deep below the component in question to find
 * the selected item we need in order to select it properly. 
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public class JFCSelectFromParent extends JFCEventHandler {

	/**
     * Constructor for JFCSelectFromParent. Does nothing. 
     */
	public JFCSelectFromParent() 
	{
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// * edu.umd.cs.guitar.event.ThreadEventHandler#actionPerformImp(edu.umd.cs
	// * .guitar.model.GXComponent)
	// */
	// @Override
	// protected void performImpl(GComponent gComponent) {
	// Accessible aChild = ((JFCXComponent) gComponent).getAComponent();
	// Component cChild = (Component) aChild;
	//
	// // Find the closet parent which is support selection
	// Accessible aParent = getSelectableParent(aChild);
	//
	// if (aParent != null) {
	// Method selectionMethod;
	//
	// try {
	// selectionMethod = aParent.getClass().getMethod(
	// "setSelectedComponent", Component.class);
	// selectionMethod.invoke(aParent, cChild);
	// } catch (SecurityException e) {
	// GUITARLog.log.error(e);
	// } catch (NoSuchMethodException e) {
	// GUITARLog.log.error(e);
	// } catch (IllegalArgumentException e) {
	// GUITARLog.log.error(e);
	// } catch (IllegalAccessException e) {
	// GUITARLog.log.error(e);
	// } catch (InvocationTargetException e) {
	// GUITARLog.log.error(e);
	// }
	// }
	//
	// }

	/**
	 * Reverse the process by attempting to find a parent of gComponent, so that a
	 * special command can be invoked on the parent to select the child. 
	 */
	@Override
	protected void performImpl(GObject gComponent,Hashtable<String, List<String>> optionalData) {
		
		Component cChild = ((JFCXComponent) gComponent).getComponent();

		// Find the closet parent which is support selection
		Component aParent = getSelectableParent(cChild);

		if (aParent != null) {

			Method selectionMethod;

			try {
				selectionMethod = aParent.getClass().getMethod(
						"setSelectedComponent", Component.class);
				selectionMethod.invoke(aParent, cChild);
			} catch (SecurityException e) {
				GUITARLog.log.error(e);
			} catch (NoSuchMethodException e) {
				GUITARLog.log.error(e);
			} catch (IllegalArgumentException e) {
				GUITARLog.log.error(e);
			} catch (IllegalAccessException e) {
				GUITARLog.log.error(e);
			} catch (InvocationTargetException e) {
				GUITARLog.log.error(e);
			}
		}

	}

	/**
	 * Selects the child of parent gComponent specified in the parameters object. 
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void performImpl(GObject gComponent, Object parameters,Hashtable<String, List<String>> optionalData) {

		int iParam;

		List<String> lParameter = null;
		try {
			lParameter = (List<String>)parameters;
			if (lParameter == null || lParameter.isEmpty()) 
				iParam = 0; 
			else 
				iParam = Integer.parseInt(lParameter.remove(0));
			
		} catch (NumberFormatException e) {
			iParam = 0;
		}

		final int index = iParam;
	
		Component parent = getComponent(gComponent);
		if(parent == null)
			return;
		final JFCXComponent finalGComponent = (JFCXComponent)gComponent;
		
		try {
			if(parent.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.PAGE_TAB_LIST)) {
				// try to select the index specified
				SwingUtilities.invokeAndWait(new Runnable(){public void run() {
					AccessibleContext theTabSet = finalGComponent.getAccessibleContext();
					theTabSet.getAccessibleSelection().addAccessibleSelection(index);
				}});
			}
			else if(parent.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.COMBO_BOX)) {
				final JComboBox<?> compCombo = (JComboBox<?>)parent;
				AccessibleSelection aSelection = findComboList(finalGComponent).getAccessibleSelection();
				aSelection.addAccessibleSelection(index);
				final Accessible newSelection = aSelection.getAccessibleSelection(0);
				SwingUtilities.invokeAndWait(new Runnable(){public void run() {
					compCombo.scrollRectToVisible(JFCXComponent.accessibleBounds(newSelection));
					String selectedItemName = newSelection.getAccessibleContext().getAccessibleName();
					compCombo.getModel().setSelectedItem(selectedItemName);
				}});
			}
		} 
		catch (SecurityException se) {
			System.err.println("Could not select requested component from parent due to SecurityException.");
			throw new EventPerformException();
		}
		catch(Exception e ) {
			System.err.println("Could not select requested component from parent.\n"
					+ "Cause: " + e.getCause() + "\n"
					+ "Message: " + e.getLocalizedMessage());
			throw new EventPerformException();
		}
		
	}

	/**
	 * 
	 * A helper method to find the closest ancestor having setSelectedComponent
	 * method Presumably this method will select the current element.
	 * 
	 * <p>
	 * 
	 * @param aComponent
	 * @return Accessible
	 */
	// private Accessible getSelectableParent(Accessible aComponent) {
	private Component getSelectableParent(Component component) {
		// if (aComponent == null)
		if (component == null)
			return null;

		Component parent = component.getParent();
		Method[] methods = parent .getClass().getMethods();
		for (Method m : methods) {
			if ("setSelectedComponent".equals(m.getName()))
				return parent;
		}

		return getSelectableParent(parent);
		
//		
//		AccessibleContext aContext = component.getAccessibleContext();
//
//		if (aContext == null)
//			return null;
//		
//		
//		
//
//		Accessible aParent = aContext.getAccessibleParent();
//		if (aParent == null)
//			return null;
//
//		if (!(aParent instanceof Component))
//			return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.guitar.event.GEvent#isSupportedBy(edu.umd.cs.guitar.model.GComponent)
	 */
	@Override
	public boolean isSupportedBy(GObject gComponent) 
	{
		// return false
		if (!(gComponent instanceof JFCXComponent))
			return false;
		
		JFCXComponent jComponent = (JFCXComponent) gComponent;
		Component component = jComponent.getComponent();
		AccessibleContext aContext = component.getAccessibleContext();
		if (aContext==null)
			return false;
		
		Object event = aContext.getAccessibleSelection();
		if (event == null)
			return false;
		
		AccessibleRole role = aContext.getAccessibleRole();
		if(!role.equals(AccessibleRole.PAGE_TAB_LIST) && !role.equals(AccessibleRole.COMBO_BOX)) 
			return false; // this class only supports selectable pagetablists and comboboxes
		
		return true;
	}
	
	/**
	 * Returns a list of all the JFCAccessibleChildren belonging to currentWidget.
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	a list of all JFCAccessibleChildren belonging to currentWidget is returned.
	 */
	public static List<Accessible> collectJFCAccessibleChildren(JFCXComponent currentWidget)
	{
		List<Accessible> toReturn = new LinkedList<Accessible>();
		
		if(currentWidget == null || currentWidget.getAccessibleContext() == null)
			return toReturn;
			
		AccessibleContext aContext = currentWidget.getAccessibleContext();
		for(int i = 0; i < aContext.getAccessibleChildrenCount(); i++) 
			toReturn.add(aContext.getAccessibleChild(i));
		
		return toReturn;
	}
	public static AccessibleContext findComboList(GComponent comboBox)
	{
		if(comboBox == null)
			throw new IllegalArgumentException("Could not find combo list");
		
		for(Accessible c : collectJFCAccessibleChildren((JFCXComponent)comboBox)) {		
			AccessibleRole role = c.getAccessibleContext().getAccessibleRole();
			// find the popup menu
			
			if(role.equals(AccessibleRole.POPUP_MENU)) {
				
				AccessibleContext tContext;
				ArrayDeque<AccessibleContext> discovered = new ArrayDeque<AccessibleContext>();
				// find the accessibleSelection: do a BFS
				for(tContext = c.getAccessibleContext(); tContext != null; tContext = discovered.poll()) {
					if(tContext.getAccessibleSelection() != null)
						break;
					for(int i = 0; i < tContext.getAccessibleChildrenCount(); i++)
						discovered.offer(tContext.getAccessibleChild(i).getAccessibleContext());
				}
				
				if(tContext == null)
					throw new IllegalArgumentException("Could not find combo list");
				return tContext;
			}
		}
		throw new IllegalArgumentException("Could not find combo list");
	}
}
