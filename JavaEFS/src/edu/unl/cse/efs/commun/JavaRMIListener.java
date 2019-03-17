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
package edu.unl.cse.efs.commun;

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;
import edu.unl.cse.efs.java.JavaCaptureUtils;
import edu.unl.cse.efs.tools.ErrorTraceConformance;
import edu.unl.cse.efs.commun.JavaListener;
import edu.unl.cse.guitarext.JavaTestInteractions;

/**
 * A compilation of all the necessary calls that need to be implemented in order to support 
 * the handling of Java Events that are triggered by Accessible objects. 
 * 
 * The JavaListener does not choose which events need to be handled, given an accessible object. 
 * 
 * JavaListener DOES NOT SUPPORT WindowListener interfaces. That is the job of some other class. 
 * 
 * The JavaListener has to report back to a CaptureTestCase object when it has finished capturing.
 * 
 * One java listener is assigned to one java window. 
 * 
 * This JavaListener may throw RemoteExceptions
 */
public class JavaRMIListener extends JavaListener 
{	
	private JavaTestInteractions windowInteractions;
	private final JavaButtonListener jb, jcb, jba;
	private final JavaTableCellListener jta;
	private final JavaToggleButtonListener jtb;
	private final JavaTextListener jt;
	private final JavaMenuItemListener jm;
	private final JavaListSelectionListener jl;
	private final JavaTabChangeListener jts;
	private final String windowName;
	private Component workingList;
	NetCommunication saver;
	
	// this state allows capture to being performed if true
	// call startCapturing() to make this variable true, stopCapturing() to reset it to false.  
	
	
	public JavaRMIListener(String windowName, NetCommunication networkStub, JavaTestInteractions windowInteractions)
	{
		super(windowName);
		jb = new JavaButtonListener(false, false);
		jcb = new JavaButtonListener(true, false);
		jba = new JavaButtonListener(false, true);
		jt = new JavaTextListener(false);
		jta = new JavaTableCellListener();
		jm = new JavaMenuItemListener();
		jl = new JavaListSelectionListener();
		jtb = new JavaToggleButtonListener();
		jts = new JavaTabChangeListener();
		this.windowName = windowName;
		this.windowInteractions = windowInteractions;
		this.saver = networkStub;
	}
	
	/**
	 * Turns on event capturing.  Events captured by
	 * this listener will be processed into steps and printed
	 * to console if printing is turned on. 
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	events captured will be converted to steps.
	 * 					events captured will be printed to console
	 * 					if event printing mode is turned on.
	 */
	public static void startCapturing()
	{
		captureOn = true;
	}
	
	/**
	 * Reverses the effect of startCapturing(). Events captured 
	 * by this listener will not be processed into steps or printed
	 * to the console. 
	 * 
	 * Preconditions:	none
	 * Postconditions: 	events captured will not be converted to steps.
	 * 					events captured will not be printed to console.
	 */
	public static void stopCapturing()
	{
		captureOn = false;
	}
	
	/**
	 * Register a button with this 
	 */
	public void registerTabList(Component touchable)
	{
		touchable.addMouseListener(jts);
		AccessibleContext tContext = touchable.getAccessibleContext();
		for(int i = 0; i < tContext.getAccessibleChildrenCount(); i++) {
			AccessibleContext nextPage = tContext.getAccessibleChild(i).getAccessibleContext();
			nextPage.addPropertyChangeListener(jts);
		}
	}
	
	public void unRegisterTabList(Component touchable)
	{
		touchable.removeMouseListener(jts);
		AccessibleContext tContext = touchable.getAccessibleContext();
		for(int i = 0; i < tContext.getAccessibleChildrenCount(); i++) {
			AccessibleContext nextPage = tContext.getAccessibleChild(i).getAccessibleContext();
			nextPage.removePropertyChangeListener(jts);
		}
	}
	
	
	// add listeners for
	// push button
	/**
	 * Register a button with this java listener. 
	 * Events from touchable will be reported to JavaButtonListener's methods.
	 * @param touchable
	 */
	public void registerButton(Component touchable)
	{
		touchable.addMouseListener(jb);
	}
	
	/**
	 * Unregister a button element from this javaListener. 
	 * Events from touchable will not be reported to JavaButtonListener's methods.
	 * @param touchable
	 */
	public void unRegisterButton(Component touchable)
	{
		touchable.removeMouseListener(jb);
	}
	// toggle button
	/**
	 * Register a toggle button element with this java listener. 
	 * Events from touchable will be reported to JavaToggleButtonListener's methods.
	 * @param touchable
	 */
	public void registerToggleButton(Component touchable)
	{
		touchable.addMouseListener(jtb);
	}
	
	/**
	 * Unregister a toggle button element from this javaListener. 
	 * Events from touchable will not be reported to JavaToggleButtonListener's methods.
	 * @param touchable
	 */
	public void unRegisterToggleButton(Component touchable)
	{
		touchable.removeMouseListener(jtb);
	}
	// text element
	/**
	 * Unregister a text element from this javaListener. Events from touchable, if performed, 
	 * will not be reported to this javaListener's JavaTextListener's methods.
	 * @param touchable
	 */
	public void registerText(Component typeable)
	{
		typeable.addKeyListener(jt);
		typeable.addFocusListener(jt);
	}
	
	
	/**
	 * Unregister's touchable from this java listener's text field compatible listeners. 
	 * Events from touchable will not be reported to this javaListener's JavaTextListener methods. 
	 * @param touchable
	 */
	public void unRegisterText(Component typeable)
	{
		typeable.removeKeyListener(jt);
		typeable.removeFocusListener(jt);
	}
	
	// menu item
	/**
	 * Registers touchable using this javaListener's menu item compatible listeners. 
	 * @param touchable
	 */
	public void registerMenuItem(Component touchable)
	{
//		touchable.addMouseListener(jm);
		if(touchable instanceof AbstractButton)
			((AbstractButton)touchable).addActionListener(jm);
		else
			touchable.addMouseListener(jm);
	}
	
	/**
	 * Unregisters touchable from this javaListener's menu item compatible listeners.
	 * @param touchable
	 */
	public void unRegisterMenuItem(Component touchable)
	{
		if(touchable instanceof AbstractButton)
			((AbstractButton)touchable).removeMouseListener(jm);
		else
			touchable.removeMouseListener(jm);
	}
	// lists
	/**
	 * Registers touchable using this javaListener's list selector compatible listeners. 
	 * 
	 * Preconditions: 	touchable must be a component with a list role.
	 * 					touchable must have a scroll pane parent. 
	 * @param touchable
	 */
	public void registerListSelector(Component touchable)
	{
		touchable.addMouseListener(jl);
	}
	
	/**
	 * Unregisters touchable from this javaListener's list selector compatible listeners. 
	 * @param touchable
	 */
	public void unRegisterListSelector(Component touchable)
	{
		touchable.removeMouseListener(jl);
	}
	
	// combo boxes
	/**
	 * Registers touchable with this javaListener's combo box listeners. 
	 * @param touchable
	 */
	public void registerComboBoxComponent(Component touchable)
	{
		touchable.addMouseListener(jcb);
	}
	/**
	 * Registers typeable with this javaListener's listeners that handle text boxes within combo boxes. 
	 * NOTE THAT THIS ADDS A MOUSE LISTENER AND NOT A KEY LISTENER. 
	 * @param touchable
	 */
	public void registerComboBoxText(Component typeable)
	{
		typeable.addMouseListener(jcb);
	}
	
	/**
	 * Registers touchable with this javaListener's listeners that handle push buttons within combo boxes.
	 * @param touchable
	 */
	public void registerComboBoxButton(Component touchable)
	{
		touchable.addMouseListener(jcb);
	}
	
	/**
	 * Unregisters touchable from this javaListener's listeners that handle actions on the combo box component. 
	 * @param touchable
	 */
	public void unRegisterComboBoxComponent(Component touchable)
	{
		touchable.removeMouseListener(jcb);
	}
	
	/**
	 * Unregisters typeable from this javaListener's combo box text component listeners. 
	 * @param typable
	 */
	public void unregisterComboBoxText(Component typeable)
	{
		typeable.removeMouseListener(jcb);
	}
	
	/**
	 * Unregisters touchable from this javaListener's listeners that handle actions on the push button within the combo box.  
	 * @param touchable
	 */
	public void unRegisterComboBoxButton(Component touchable)
	{
		touchable.removeMouseListener(jcb);
	}
	
	public void registerDirectClickComponent(Component touchable)
	{
		touchable.addMouseListener(jba);
	}
	
	public void unRegisterDirectClickComponent(Component touchable)
	{
		touchable.removeMouseListener(jba);
	}
	
	/**
	 * Responds to the action of clicking a table. 
	 */
	private class JavaTableCellListener implements MouseListener, PropertyChangeListener
	{
		public void mouseEntered(MouseEvent me) {}
		public void mouseExited(MouseEvent me) {}
		public void mouseClicked(MouseEvent me) 
		{
			if(captureOn) {
				try{
					saver.flushTextItems();
					saver.flushListItems(listSelection());
					Component mouseComponent = (Component)me.getSource();
					String eventID = windowInteractions.lookupLargeObjectID(mouseComponent, windowName, ActionClass.PARSELECT.actionName);
					String componentName = JavaCaptureUtils.getCaptureComponentName(mouseComponent);
					
					saver.gotEvent(me, eventID + JavaTestInteractions.name_version_separator + componentName, windowName, AccessibleRole.TOGGLE_BUTTON.toDisplayString());
				} 
				catch(RemoteException e) {
					System.err.println("JavaRMIListener: MousePressed failed to send the event.");
				}
//				System.out.println(me);
				testCase.saveTableCellClick(me, windowName);
			}
			
		}
		public void mousePressed(MouseEvent me) {}
		public void mouseReleased(MouseEvent me) {}
		public void propertyChange(PropertyChangeEvent e) 
		{
			System.out.println(e);
		}
	}
	
	private class JavaMenuItemListener implements MouseListener, ActionListener
	{
		public void mouseEntered(MouseEvent me){/*do nothing when mouse entered*/}
		public void mouseExited(MouseEvent me) {/* do nothing when mouse exited*/}
		public void mouseClicked(MouseEvent me){/* do nothing when mouse clicked*/}	
		public void mouseReleased(MouseEvent me){/* do nothing when mouse released*/}
		public void mousePressed(MouseEvent me){
			if(captureOn) {
				try {
					saver.flushTextItems();
					saver.flushListItems(listSelection());
					Component menuItem = (Component)me.getSource();
					saver.gotMenuItemEvent(menuTreeComponents(menuItem), windowName);
				} catch(RemoteException e) {
					System.err.println("JavaRMIListener: MousePressed failed to send the event.");
				}
			}
		}
		
		public String[][] menuTreeComponents(Component menuStepRoot)
		{
			LinkedList<String> names = new LinkedList<>();
			LinkedList<String> roles = new LinkedList<>();
			AccessibleRole parentRole;
			while(menuStepRoot!=null) {
				parentRole = menuStepRoot.getAccessibleContext().getAccessibleRole();
				if(menuStepRoot instanceof JPopupMenu)	
					menuStepRoot = ((JPopupMenu)menuStepRoot).getInvoker(); // skip JPopupMenu container elements.
				else if(parentRole.equals(AccessibleRole.MENU) 
						|| parentRole.equals(AccessibleRole.MENU_ITEM)) {					
					String menuComponentID = windowInteractions.lookupID(menuStepRoot, windowName, ActionClass.ACTION.actionName); 
					String menuComponentName = JavaCaptureUtils.getCaptureComponentName(menuStepRoot); 		
					names.push(menuComponentID + JavaTestInteractions.name_version_separator + menuComponentName);
					roles.push(parentRole.toDisplayString());
					menuStepRoot = menuStepRoot.getParent();							
				}
				else
					break;
			}
			Iterator<String> nIt = names.iterator();
			Iterator<String> rIt = roles.iterator();
			String[][] namesAndRoles = new String[names.size()][2];
			int nextIndex = 0;
			while(nIt.hasNext()) {
				namesAndRoles[nextIndex][0] = nIt.next();
				namesAndRoles[nextIndex][1] = rIt.next();
				nextIndex++;
			}
			return namesAndRoles;
		}
		@Override
		public void actionPerformed(ActionEvent ae) 
		{
			try {
				saver.flushTextItems();
				saver.flushListItems(listSelection());
				Component menuItem = (Component)ae.getSource();
				saver.gotMenuItemEvent(menuTreeComponents(menuItem), windowName);
			} 
			catch(RemoteException e) {
				System.err.println("JavaRMIListener: ActionPerformed failed to send the event.");
			}
		}
	}

	/**
	 * Responds to the action of clicking a normal button
	 * @author jsaddle
	 */
	private class JavaButtonListener implements MouseListener
	{
		private final boolean combo;
		private final boolean background;
		public JavaButtonListener(boolean combo, boolean background)
		{
			this.combo = combo;
			this.background = background;
		}
		
		public void mouseEntered(MouseEvent me){/*do nothing when mouse entered*/}
		public void mouseReleased(MouseEvent me){/*do nothing when mouse released*/}
		public void mouseExited(MouseEvent me) {/* do nothing when mouse exited*/}
		public void mouseClicked(MouseEvent me){/* do nothing when mouse clicked all the way*/}
		public void mousePressed(MouseEvent me)
		{
			if(captureOn) {
				if(combo) {
					JComboBox<?> myCombo = findCombo((Component)me.getSource());
					try {
						saver.flushTextItems();
						saver.flushListItems(listSelection());
					} catch(RemoteException e) {
						System.err.println("JavaRMIListener: Combo MousePressed failed to send the event");
					}
					myCombo.addActionListener(new ComboListener());
				}
				else {
					Component mouseComponent = (Component)me.getSource();
					try {
						saver.flushTextItems();
						saver.flushListItems(listSelection());
						String eventID = windowInteractions.lookupID(mouseComponent, windowName, ActionClass.ACTION.actionName);
						String componentName = JavaCaptureUtils.getCaptureComponentName(mouseComponent);
						String eidString = eventID + JavaTestInteractions.name_version_separator + componentName;
						AccessibleRole role = mouseComponent.getAccessibleContext().getAccessibleRole();
						String roleString;
						if(background) 
							roleString = role.toDisplayString() + 
									GUITARConstants.NAME_SEPARATOR + me.getX() + 
									GUITARConstants.NAME_SEPARATOR + me.getY();
						else 
							roleString = role.toDisplayString();
						saver.gotEvent(me, eidString, windowName, roleString);
					} catch(RemoteException e) {
						System.err.println("JavaRMIListener: Button MousePressed failed to send the event");
					}
				}
			}
		}
		
		/**
		 * Attempts to locate the combo box in the parent hierarchy of this widget. 
		 */
		private JComboBox<?> findCombo(Component first)
		{
			Component target;
			for(target = first; target != null; target = target.getParent()) {
				AccessibleRole tRole = target.getAccessibleContext().getAccessibleRole();
				if(tRole != null && tRole.equals(AccessibleRole.COMBO_BOX)) 
					return (JComboBox<?>)target;
			}
			throw new IllegalArgumentException("Could not traverse hierarchy of combo box element");
		}
	}

	private class ComboListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent ae) {
			try {
				// send two events at the same time.
				saver.flushTextItems();
				saver.flushListItems(listSelection());
				Component actComponent = (Component)ae.getSource();
				String eventID1 = windowInteractions.lookupID(actComponent, windowName, ActionClass.ACTION.actionName);
				String eventID2 = windowInteractions.lookupID(actComponent, windowName, ActionClass.PARSELECT.actionName);
				String componentName = JavaCaptureUtils.getCaptureComponentName(actComponent);
				
				saver.gotEvent(ae, eventID1 + JavaTestInteractions.name_version_separator + componentName, windowName, AccessibleRole.COMBO_BOX.toDisplayString());
				saver.gotComboSelectEvent(eventID2 + JavaTestInteractions.name_version_separator + componentName, windowName, getAccessibleSelectionFrom(actComponent));
			} 
			catch(RemoteException e) {
				System.err.println("JavaRMIListener: ActionPerformed failed to send the event.");
			}
			((JComboBox<?>)ae.getSource()).removeActionListener(this);
			
		}
	}
	/**
	 * Responds to the action of clicking a toggle button
	 */
	private class JavaToggleButtonListener implements MouseListener
	{
		public void mouseEntered(MouseEvent me){/*do nothing when mouse entered*/}
		public void mouseReleased(MouseEvent me){
			if(captureOn) {
				try {
					saver.flushTextItems();
					saver.flushListItems(listSelection());
					Component mouseComponent = (Component)me.getSource();
					String eventID = windowInteractions.lookupID(mouseComponent, windowName, ActionClass.ACTION.actionName);
					String componentName = JavaCaptureUtils.getCaptureComponentName(mouseComponent);
					
					saver.gotEvent(me, eventID + JavaTestInteractions.name_version_separator + componentName, windowName, AccessibleRole.TOGGLE_BUTTON.toDisplayString());
				} 
				catch(RemoteException e) {
					System.err.println("JavaRMIListener: MousePressed failed to send the event.");
				}
			}
		}
		public void mouseExited(MouseEvent me) {/* do nothing when mouse exited*/}
		public void mouseClicked(MouseEvent me){/* do nothing when mouse clicked all the way*/}
		public void mousePressed(MouseEvent me){/*do nothing when mouse pressed*/}
	}
	
	/**
	 * Responds to the action of typing a key
	 * @author jsaddle
	 *
	 */
	private class JavaTextListener implements KeyListener, FocusListener
	{
		public JavaTextListener(boolean combo)
		{
		}
		public void keyTyped(KeyEvent ke){/*do nothing when key typed}*/}
		public void keyReleased(KeyEvent ke){/* do nothing when key released*/}
		/** 
		 * If capture is on, will construct a step for a text event and send the step to the 
		 * test case. 
		 */
		public void keyPressed(KeyEvent ke)
		{
			if(captureOn) {
				try {
					saver.flushListItems(listSelection());
					Component keyComponent = (Component)ke.getSource();
					String eventID = windowInteractions.lookupID(keyComponent, windowName, ActionClass.TEXT.actionName);
					String componentName = JavaCaptureUtils.getCaptureComponentName(keyComponent);
					String role = keyComponent.getAccessibleContext().getAccessibleRole().toDisplayString();					
					saver.gotKeyEvent(new String[]{""+ke.getKeyCode(), ""+ke.isConsumed(), KeyEvent.getKeyText(ke.getKeyCode())}, 
							ke.getKeyChar(), 
							eventID + JavaTestInteractions.name_version_separator + componentName, 
							windowName, 
							role);
				}
				catch(RemoteException e) {
					System.err.println("JavaRMIListener: KeyPressed() failed to send the event.");
					System.out.println("Error at Line: " + e.getStackTrace()[0].getLineNumber());
					System.err.println("Exception. " + e.getClass().getCanonicalName() + "\n" +
							"Cause: " + e.getCause().getClass().getCanonicalName());
					System.err.println(ErrorTraceConformance.someOfStackTrace(e, 40));
					System.err.println("Suppressed:");
					if(e.getSuppressed() != null) 
						System.err.println(e);
				}
			}
		}
		@Override
		public void focusGained(FocusEvent e) {}
		@Override
		public void focusLost(FocusEvent fe) {
			if(captureOn) {
				try {
					saver.flushTextItems();
				} catch(RemoteException e) {
					System.err.println("JavaRMIListener: focusLost() failed to send the event.");
					System.err.println("Exception. " + e.getClass().getCanonicalName() + "\n" +
							"Cause: " + e.getCause().getClass().getCanonicalName());
					System.err.println(ErrorTraceConformance.someOfStackTrace(e));
					System.err.println("Suppressed:");
					if(e.getSuppressed() != null) 
						System.err.println(e);
				}
			}
			
		}
	}
	/**
	 * Responds to the action of selecting an item from a list. 
	 * @author jsaddle
	 *
	 */
	private class JavaListSelectionListener implements MouseListener
	{
		public void mouseClicked(MouseEvent me) {}
		public void mousePressed(MouseEvent me) {
			if(captureOn) {
				try {
					saver.flushTextItems();
					Component mouseComponent = (Component)me.getSource();
					// pointer comparison. Is the list we're looking at the same as the one we had before?
					if(workingList != null && mouseComponent != workingList) 
						saver.flushListItems(listSelection());
					
					String eventID = windowInteractions.lookupLargeObjectID(mouseComponent, windowName, ActionClass.SELECTION.actionName);
					String componentName = JavaCaptureUtils.getCaptureComponentName(mouseComponent);
					saver.gotListEvent(eventID + JavaTestInteractions.name_version_separator + componentName, windowName);
					workingList = mouseComponent;
				} 
				catch(RemoteException e) {
					System.err.println("JavaRMIListener: MousePressed failed to send the event.");
				}
			}
		}
		public void mouseReleased(MouseEvent me) {}
		public void mouseEntered(MouseEvent me) {}
		public void mouseExited(MouseEvent me) {}
	}
	
	
	private class JavaTabChangeListener implements MouseListener, PropertyChangeListener
	{
		boolean changedIndex;
		boolean clickedMouse;
		boolean inside;
		
		MouseEvent specialMouseEvent;
		PropertyChangeEvent specialChangeEvent;

		public JavaTabChangeListener()
		{
			changedIndex = clickedMouse = inside = false;
		}
		
		/**
		 * The job of this method is to ensure that the outcome of the operation
		 * of clicking a savedTab is saved to the testCase via the networkStub. To do this
		 * we must check for both a mouseClick. We are unsure of the order of
		 * how propertyChangeEvents and mouseEvents will arrive, so if both
		 * arrive, we attempt to save the step.
		 * 
		 * Every other time, starting on the second time, this method is entered, all the states are reset to false. 
		 */
		public void validateSteps()
		{
			if(inside && clickedMouse && changedIndex) {
				Accessible ces = (Accessible)specialChangeEvent.getSource();
				Accessible mes = (Accessible)specialMouseEvent.getSource();
				if(!ces.getAccessibleContext().getAccessibleParent().equals(mes)) 
					return;	
				
				try {
					saver.flushTextItems();
					saver.flushListItems(listSelection());
					Component mouseComponent = (Component)specialMouseEvent.getSource();
					String eventID = windowInteractions.lookupID(mouseComponent, windowName, ActionClass.PARSELECT.actionName);
					String componentName = JavaCaptureUtils.getCaptureComponentName(mouseComponent);
					List<Integer> selection = getAccessibleSelectionFrom(mouseComponent);
					String stringSelection = "" + selection.get(0);
					saver.gotPageTabEvent(eventID + JavaTestInteractions.name_version_separator + componentName, windowName, stringSelection);
				} 
				catch(RemoteException e) {
					System.err.println("JavaRMIListener: MousePressed failed to send the event.");
				}
			}
			if(inside) {
				specialMouseEvent = null;
				specialChangeEvent = null;
				clickedMouse = changedIndex = false; // reset the states
			}
		}
 
		public void mousePressed(MouseEvent me) 
		{
			if(captureOn) {
				clickedMouse = true;
				specialMouseEvent = me;
				validateSteps();
				inside = !inside;
			}
		}
		public void propertyChange(PropertyChangeEvent pce) {
			if(captureOn) 
				if(pce.getNewValue() != null && pce.getNewValue().equals(AccessibleState.SELECTED)) {
					changedIndex = true;
					specialChangeEvent = pce;
					validateSteps();
					inside = !inside;
				}
		}
	
		public void resetStates()
		{
			clickedMouse = changedIndex = inside = false;
			specialMouseEvent = null;
			specialChangeEvent = null;
		}
		
		public void mouseReleased(MouseEvent me) 
		{
			if(captureOn) 
				resetStates();
		}

		public void mouseExited(MouseEvent me) 
		{
			if(captureOn)
				resetStates();
		}
		public void mouseClicked(MouseEvent me) {/*Do nothing on mouse clicked*/}
		public void mouseEntered(MouseEvent me){/*Do nothing on mouse entered*/}
		
	}
	
	private List<Integer> listSelection()
	{	
		if(workingList == null)
			return new LinkedList<Integer>();
		else
			return getAccessibleSelectionFrom(workingList);
	}
	
	private List<Integer> getAccessibleSelectionFrom(Component component)
	{
		LinkedList<Integer> toReturn;
		boolean hasNoSelectable = 
				component == null || 
				component.getAccessibleContext() == null ||
				component.getAccessibleContext().getAccessibleSelection() == null;
		if(hasNoSelectable)
			return new LinkedList<Integer>();
		
		
		AccessibleContext selectableContext = component.getAccessibleContext();
		AccessibleSelection mySelection = selectableContext.getAccessibleSelection();
		AccessibleRole role = selectableContext.getAccessibleRole();
		toReturn = new LinkedList<Integer>();
		if(role.equals(AccessibleRole.COMBO_BOX)) {
			List<Accessible> theSelected = new ArrayList<Accessible>();
			for(int i = 0; i < mySelection.getAccessibleSelectionCount(); i++)
				theSelected.add(mySelection.getAccessibleSelection(i));
			
			for(Accessible a : theSelected) {
				int index = a.getAccessibleContext().getAccessibleIndexInParent();
				toReturn.add(index);
			}
		}
		
		else 
			for(int i = 0; i < selectableContext.getAccessibleChildrenCount(); i++)
				if(mySelection.isAccessibleChildSelected(i))
					toReturn.add(i);
			
		return toReturn;
	}
}
