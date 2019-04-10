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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;

import edu.umd.cs.guitar.model.JFCXComponent;
import edu.unl.cse.efs.java.JavaCaptureTaskList;

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
 */
public class JavaListener {
	
	protected JavaCaptureTaskList testCase;
	private final JavaButtonListener jb, jcb;
//	private final JavaBackgroundClickListener jba;
	private final JavaToggleButtonListener jtb;
	private final JavaTextListener jt;
	private final JavaMenuItemListener jm;
	private final JavaListSelectionListener jl;
	private final JavaTabChangeListener jts;
	private final JavaTableCellListener jta;
	private final JavaMouseHoverListener jmh;
	private final String windowName;
	
	// this state allows capture to being performed if true
	// call startCapturing() to make this variable true, stopCapturing() to reset it to false.  
	protected static boolean captureOn = false;
	
	/**
	 * Initialize JavaListener with a windowName and an attached test case.
	 */
	public JavaListener(String windowName, JavaCaptureTaskList testCase)
	{
		this(windowName);
		this.testCase = testCase;
	}
	
	/**
	 * Initialize a JavaListener with a windowName, but with no attached test case. 
	 */
	protected JavaListener(String windowName)
	{
		this.windowName = windowName;
		jb = new JavaButtonListener(false, false);
		jcb = new JavaButtonListener(true, false);
		jt = new JavaTextListener(false);
		jm = new JavaMenuItemListener();
		jl = new JavaListSelectionListener();
		jtb = new JavaToggleButtonListener();
		jts = new JavaTabChangeListener();
		jta = new JavaTableCellListener();
		jmh = new JavaMouseHoverListener();
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
	 * Register a tab list from this java listener
	 * Events from this touchable will be reported to JavaTabChangeListener methods
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
	
	/**
	 * Unregister a tab list from this java listener
	 * Events from this touchable will not be reported to JavaTabChangeListener methods
	 */
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
	 * 
	 */
	public void registerButton(Component touchable)
	{
		touchable.addMouseListener(jb);
	}
	
	/**
	 * Unregister a button element from this javaListener. 
	 * Events from touchable will not be reported to JavaButtonListener's methods.
	 * 
	 */
	public void unRegisterButton(Component touchable)
	{
		touchable.removeMouseListener(jb);
	}
	// toggle button
	/**
	 * Register a toggle button element with this java listener. 
	 * Events from touchable will be reported to JavaToggleButtonListener's methods.
	 * 
	 */
	public void registerToggleButton(Component touchable)
	{
		touchable.addMouseListener(jtb);
	}
	
	/**
	 * Unregister a toggle button element from this javaListener. 
	 * Events from touchable will not be reported to JavaToggleButtonListener's methods.
	 * 
	 */
	public void unRegisterToggleButton(Component touchable)
	{
		touchable.removeMouseListener(jtb);
	}
	// text element
	/**
	 * Unregister a text element from this javaListener. Events from touchable, if performed, 
	 * will not be reported to this javaListener's JavaTextListener's methods.
	 * 
	 */
	public void registerText(Component typeable)
	{
		typeable.addKeyListener(jt);
		typeable.addFocusListener(jt);
	}
	
	
	/**
	 * Unregister's touchable from this java listener's text field compatible listeners. 
	 * Events from touchable will not be reported to this javaListener's JavaTextListener methods. 
	 * 
	 */
	public void unRegisterText(Component typeable)
	{
		typeable.removeKeyListener(jt);
		typeable.removeFocusListener(jt);
	}
	
	public void registerOldMenuItem(MenuComponent touchable)
	{
		MenuItem x = (MenuItem)touchable;
		x.addActionListener(jm);
	}
	
	// menu item
	/**
	 * Registers touchable using this javaListener's menu item compatible listeners. 
	 * 
	 */
	public void registerMenuItem(Component touchable)
	{
		if(touchable instanceof AbstractButton)
			((AbstractButton)touchable).addActionListener(jm);
		else
			touchable.addMouseListener(jm);
	}
	
	/**
	 * Unregisters touchable from this javaListener's menu item compatible listeners.
	 * 
	 */
	public void unRegisterMenuItem(Component touchable)
	{
		if(touchable instanceof AbstractButton)
			((AbstractButton)touchable).removeActionListener(jm);
		else
			touchable.removeMouseListener(jm);
	}
	// lists
	/**
	 * Registers touchable using this javaListener's list selector compatible listeners. 
	 * 
	 * Preconditions: 	touchable must be a component with a list role.
	 * 					touchable must have a scroll pane parent. 
	 * 
	 */
	public void registerListSelector(Component touchable)
	{
		touchable.addMouseListener(jl);
	}
	
	/**
	 * Unregisters touchable from this javaListener's list selector compatible listeners. 
	 * 
	 */
	public void unRegisterListSelector(Component touchable)
	{
		touchable.removeMouseListener(jl);
	}
	
	// combo boxes
	/**
	 * Registers touchable with this javaListener's combo box listeners. 
	 * 
	 */
	public void registerComboBoxComponent(Component touchable)
	{
		touchable.addMouseListener(jcb);
	}
	/**
	 * Registers typeable with this javaListener's listeners that handle text boxes within combo boxes. 
	 * NOTE THAT THIS ADDS A MOUSE LISTENER AND NOT A KEY LISTENER. 
	 * 
	 */
	public void registerComboBoxText(Component typeable)
	{
		typeable.addMouseListener(jcb);
	}
	/**
	 * Registers touchable with this javaListener's listeners that handle push buttons within combo boxes.
	 * 
	 */
	public void registerComboBoxButton(Component touchable)
	{
		touchable.addMouseListener(jcb);
	}
	
	/**
	 * Unregisters touchable from this javaListener's listeners that handle actions on the combo box component. 
	 * 
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
	 * 
	 */
	public void unRegisterComboBoxButton(Component touchable)
	{
		touchable.removeMouseListener(jcb);
	}
	
	public void registerDirectClickComponent(Component touchable)
	{
		touchable.addMouseListener(jb);
	}
	
	public void unRegisterDirectClickComponent(Component touchable)
	{
		touchable.removeMouseListener(jb);
	}
	
	public void registerContextboxPotential(Component fromClick)
	{
		
	}
	
	public void registerTable(Component touchable)
	{
		for(MouseListener ml : touchable.getMouseListeners()) 
			if(ml instanceof JavaTableCellListener)
				return;
		touchable.addMouseListener(jta);
	}
	
	public void unregisterTable(Component touchable)
	{
		touchable.removeMouseListener(jta);
	}
	public JavaMouseHoverListener getMouseHoverListener()
	{
		return jmh;
	}
	@SuppressWarnings("unused")
	public class JavaTableHeaderListener implements PropertyChangeListener
	{
		private final int row;
		private final int col;
		
		public JavaTableHeaderListener(int row, int col)
		{
			this.row = row;
			this.col = col;
		}
		
		public void propertyChange(PropertyChangeEvent pce)
		{
			if(!pce.getPropertyName().equals(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY)) {
				System.out.println(pce);
//				System.out.println("Clicked header " + row + ", " + col);
			}
		}
	}
//	
//	public class JavaTableCellListener implements PropertyChangeListener, FocusListener
//	{
//		private final int row;
//		private final int col;
//		public JavaTableCellListener()
//		{
//			row = -1;
//			col = -1;
//		}
//		public JavaTableCellListener(int row, int col) 
//		{
//			this.row = row;
//			this.col = col;
//		}
//		
//		public void propertyChange(PropertyChangeEvent pce) 
//		{
//			if(!pce.getPropertyName().equals(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY))
//				System.out.println("You clicked row " + row + " column " + col);
//		}
//		
//		public void focusGained(FocusEvent e) 
//		{
//			System.out.println("You clicked row " + row + " column " + col);
//		}
//		
//		public void focusLost(FocusEvent e) 
//		{
//			System.out.println("You clicked row " + row + " column " + col);
//		}
//		
//	}
	public void unRegisterTable(Component touchable)
	{
		touchable.removeMouseListener(jts);
		AccessibleContext tContext = touchable.getAccessibleContext();
		for(int i = 0; i < tContext.getAccessibleChildrenCount(); i++) {
			AccessibleContext nextPage = tContext.getAccessibleChild(i).getAccessibleContext();
			nextPage.removePropertyChangeListener(jts);
		}
	}
	
	

	
	/**
	 * Responds to the action of clicking a menu item.
	 * @author jsaddle
	 *
	 */
	private class JavaMenuItemListener implements MouseListener, ActionListener
	{
		
		public void mouseEntered(MouseEvent me){/*do nothing when mouse entered*/}
		public void mouseExited(MouseEvent me) {/* do nothing when mouse exited*/}
		public void mouseClicked(MouseEvent me){/* do nothing when mouse clicked*/}	
		public void mouseReleased(MouseEvent me){/* do nothing when mouse released*/}
		public void mousePressed(MouseEvent me){
			if(captureOn) {
				testCase.flushStoredKeystrokes();
				testCase.flushListItemSelectionToSavedSteps();
				testCase.saveMenuItemSelection(me, windowName);	
			}
		}
		@Override
		public void actionPerformed(ActionEvent ae) 
		{
			if(captureOn) {
				testCase.flushStoredKeystrokes();
				testCase.flushListItemSelectionToSavedSteps();
				testCase.saveMenuItemSelection(ae, windowName);	
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
					testCase.saveComboClick(me, myCombo, windowName);
					myCombo.addActionListener(new ComboListener());
				}
				else {
					testCase.flushStoredKeystrokes();
					testCase.flushListItemSelectionToSavedSteps();
					if(background) 
						testCase.saveBackgroundClickEvent(me, windowName);
					else
						testCase.saveButtonClick(me, windowName);
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
		public void actionPerformed(ActionEvent e) {
			testCase.flushStoredKeystrokes();
			testCase.flushListItemSelectionToSavedSteps();
			testCase.saveComboSelect(e, windowName);
			((JComboBox<?>)e.getSource()).removeActionListener(this);
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
				testCase.flushStoredKeystrokes();
				testCase.flushListItemSelectionToSavedSteps();
				testCase.saveToggleButtonClick(me, windowName);
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
//		private final boolean combo;
		public JavaTextListener(boolean combo)
		{
//			this.combo = combo;
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
				testCase.flushListItemSelectionToSavedSteps();
				testCase.saveKeyEntry(ke, windowName);
			}
		}
		@Override
		public void focusGained(FocusEvent e) {}
		
		@Override
		public void focusLost(FocusEvent e) {
			testCase.flushStoredKeystrokes();
		}
		
		
	}
	/**
	 * Responds to the action of selecting an item from a list. 
	 * @author jsaddle
	 *
	 */
	private class JavaListSelectionListener implements MouseListener
	{
		public void mouseClicked(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			if(captureOn) {
				testCase.flushStoredKeystrokes();
				testCase.saveListItemSelection(e, windowName);
			}
		}
		public void mouseReleased(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
	}
	
	/**
	 * Responds to the action of clicking a table. 
	 * @author jsaddle
	 *
	 */
	private class JavaTableCellListener implements MouseListener, PropertyChangeListener
	{
		public void mouseEntered(MouseEvent me) {}
		public void mouseExited(MouseEvent me) {}
		public void mouseClicked(MouseEvent me) 
		{
			System.out.println(me);
			testCase.saveTableCellClick(me, windowName);
			
		}
		public void mousePressed(MouseEvent me) {}
		public void mouseReleased(MouseEvent me) {}
		public void propertyChange(PropertyChangeEvent e) 
		{
			System.out.println(e);
		}
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
		 * of clicking a savedTab is saved to the testCase. To do this
		 * we must check for both a mouseClick. We are unsure of the order of
		 * how propertyChangeEvents and mouseEvents will arrive, so if both
		 * arrive, we attempt to save the step.
		 * 
		 * The second time this method is entered, all the states are reset to false. 
		 */
		public void validateSteps()
		{
			if(inside && clickedMouse && changedIndex) {
				Accessible ces = (Accessible)specialChangeEvent.getSource();
				Accessible mes = (Accessible)specialMouseEvent.getSource();
				if(!ces.getAccessibleContext().getAccessibleParent().equals(mes)) 
					return;
				
				testCase.flushStoredKeystrokes();
				testCase.flushListItemSelectionToSavedSteps();
				testCase.saveTabSelection(specialMouseEvent, specialChangeEvent, windowName);
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
	
	/**
	 * Save a proper event depending on the component that was hovered over.  
	 * @author jsaddle
	 *
	 */
	public class JavaMouseHoverListener
	{
		/**
		 * Captures a hover event over a specific component.
		 * This method determines how to save the event based on the state of the machine, 
		 * and the role of the component being passed in. If the role is parental, we need
		 * to save the hover in a special way. If not parental, we need to save the hover
		 * by ensuring that the component passed in is treated as the sole step of the hover, 
		 * so we use a method designed for that. This method makes the decision of which hover method to call. 
		 */
		public void captureHover(Component component, int mouseXPosition, int mouseYPosition)
		{
			if(captureOn) {
				testCase.flushStoredKeystrokes();
				testCase.flushListItemSelectionToSavedSteps();
				String hoverType = "";
				AccessibleRole myRole = component.getAccessibleContext().getAccessibleRole();
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
					if(JFCXComponent.hasChildren(component))
						hoverType = "basic"; // this must be top level.
					else
						hoverType = "parental"; // this must be an element in the list
				}
				
				else if(myRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
					if(!JFCXComponent.hasChildren(component))
						hoverType = "basic"; // this must be the top level. 
					else
						hoverType = "parental"; // this must be an element in the list. 
				}
				
				else if(myRole.equals(AccessibleRole.PANEL)) 
					hoverType = "basic";
				
				else if(myRole.equals(AccessibleRole.LIST))
					hoverType = "parental";
				
	//			testCase.saveSelectionHoverEvent(name, role, windowName);
				if(hoverType.equals("basic"))
					testCase.saveHoverEvent(windowName, component, mouseXPosition, mouseYPosition);
				else if(hoverType.equals("parental"))
					testCase.saveSelectionOrientedHover(windowName, component, mouseXPosition, mouseYPosition);
			}
		}
	}
}
