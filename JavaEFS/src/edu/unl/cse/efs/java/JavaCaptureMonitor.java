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
package edu.unl.cse.efs.java;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.unl.cse.efs.commun.JavaListener;
import edu.unl.cse.guitarext.JavaTestInteractions;


public class JavaCaptureMonitor implements WindowListener{
	protected JavaCaptureTaskList taskList;
	protected final ArrayList<JavaTestInteractions> interactionsByWindow;
	protected final ArrayList<GWindow> appRelatedWindows;
	protected final ArrayList<Window> modals;
	protected final ArrayList<JavaListener> javaListeners;
	protected final Toolkit toolkit;
	protected final JavaSpecialEventQueue specialQueue;
	protected ArrayList<Window> windowsInCapture;
	protected boolean textCapture, buttonCapture, menuCapture, radioCapture,
		listCapture, comboCapture, checkboxCapture, 
		contextboxCapture, tabchangeCapture, tableCapture;
	protected boolean hTextCapture, hButtonCapture, hMenuCapture, hRadioCapture,
		hListCapture, hComboCapture, hCheckboxCapture,
		hContextboxCapture, hTabchangeCapture, hTableCapture;

	protected boolean windowEventPrint;

	/**
	 * Constructor for JavaCaptureMonitor. Finally, initialize the list
	 * of java listeners that will be used to capture these windows. 
	 * @param windowsToCapture
	 */
	public JavaCaptureMonitor(Collection<GWindow> windowsToCapture, JavaCaptureTaskList testCase)
	{
		this(windowsToCapture);
		this.taskList = testCase;
	}

	/**
	 * Constructor for the JavaCaptureMonitor. 
	 * Set up the containers for windows and interaction records. 
	 * Set the features that capture certain interactables to all be on by default. 
	 */
	public JavaCaptureMonitor(Collection<GWindow> windowsToCapture)
	{
		toolkit = Toolkit.getDefaultToolkit();
		specialQueue = new JavaSpecialEventQueue();
		modals = new ArrayList<Window>();
		javaListeners = new ArrayList<JavaListener>();
		interactionsByWindow = new ArrayList<JavaTestInteractions>();
		appRelatedWindows = new ArrayList<GWindow>(windowsToCapture);
		windowsInCapture = new ArrayList<Window>();
		// set feature states. By default, all capture features are turned on 
		// for all windows in capture.
		textCapture = buttonCapture = menuCapture = radioCapture = listCapture = 
				comboCapture = checkboxCapture = 
				contextboxCapture = tabchangeCapture = tableCapture = true;
		hTextCapture = hButtonCapture = hMenuCapture = hRadioCapture = 
				hListCapture = hComboCapture = hCheckboxCapture = 
				hContextboxCapture = hTabchangeCapture = hTableCapture = true;
		windowEventPrint = false; 
	}
	/**
	 * Returns the JavaTestInteractions that correspond to captureWindow. If no interactions
	 * were ever registered on capture window or captureWindow is not currently in the capture,
	 * an empty JavaTestInteractions is returned, implying that there are no capture interactions currently
	 * registered for the window provided.
	 * 
	 * Preconditions: 	none
	 * Postconditions:	the testInteractions that correspond to the provided window
	 * 					are returned if and only if captureWindow is currently being captured. 
	 * 					Otherwise, an empty JavaTestInteractions is returned. 
	 */
	public JavaTestInteractions interactionsForWindow(String windowName)
	{
		int windowIndex = lookupWindowIndex(windowName);
		if(windowIndex == -1)
			return new JavaTestInteractions();
		else
			return interactionsByWindow.get(windowIndex);
	}
	
	/**
	 * Initiates the capture of windows that were provided to this JavaCaptureMonitor through the constructor.
	 * 
	 * Preconditions: 	javaApplication is currently open.
	 * Postconditions: 	Capture has been initiated to all windows provided to JavaCaptureMonitor that could be captured.
	 * 					If no window provided could be captured, an error message is printed to console.					
	 */
	public boolean captureWindowsProvided()
	{
		for(int i = 0; i < appRelatedWindows.size(); i++) {
			JFCXWindow nextWindow;
			nextWindow = (JFCXWindow)appRelatedWindows.get(i);
			// if capture is allowed, add a window listener to this
			// window, and attach startCapture on its interactables. 
			if(captureIsAllowedOn(nextWindow.getWindow())) 
				startCaptureOn(nextWindow.getWindow());
		}

		if(windowsInCapture.size() == 0) {
			debugStatusMessage(DebugMessage.CAPTURE_CANCELED);
			return false;
		}
		else {
			System.out.println(windowsInCapture.size() + " windows in initial list.");
			JavaListener.startCapturing();
			receiveSpecialEvents();
			System.out.println("Ready to capture.");
			return true;
		}
	}

	
	public void receiveSpecialEvents()
	{
		toolkit.getSystemEventQueue().push(specialQueue);
		specialQueue.setKeyboardManager();
	}
	
	/**
	 * Start capturing interaction events from openedWindow. Events
	 * are sent by the appropriate Java AWTEvent listeners to the JavaCaptureTaskList object in this 
	 * CaptureMonitor. 
	 * 
	 * Preconditions: openedWindow should be a valid window. 
	 * Postconditions: 	OpenedWindow is in the windowsInCapture list
	 * 					A java listener is registered for openedWindow
	 * 					A testInteractions module is registered for openedWindow
	 * 					buttons and interactibles of openedWindow are picked up by java listener and recorded to 
	 * 					  testInteractions module
	 * 					OpenedWindow's window interactions are registered with the windowListener methods of this class.
	 * 					Events enacted by the user on openedWindow are sent to testCase for post-processing.
	 * 					A close event enacted on openedWindow will cause openedWindow to be removed from capture.
	 */
	protected void startCaptureOn(Window openedWindow)
	{
		if(lookupWindowIndex(openedWindow) == -1) {
			String windowNameID = openedWindow.getAccessibleContext().getAccessibleName();
			windowsInCapture.add(openedWindow);
			javaListeners.add(new JavaListener(windowNameID, taskList));
			interactionsByWindow.add(new JavaTestInteractions());
			
			// ensure that we're not getting interrupted while...
			int newWindowIndex = javaListeners.size()-1;
			Object awtTreeLock = openedWindow.getTreeLock();
			synchronized(awtTreeLock) {
				// traversing the tree for items.
				interactionsByWindow.get(newWindowIndex).setCurrentWindow(openedWindow);
				traverseComponentGraph(openedWindow, true, newWindowIndex);
			}
			openedWindow.addWindowListener(this);
		}
		// otherwise, simply register listeners for openedWindow.
		else {
			// ensure that we're not getting interrupted
			Object awtTreeLock = openedWindow.getTreeLock();
			synchronized(awtTreeLock) {
				// traverse the tree for items
				traverseComponentGraph(openedWindow, true, lookupWindowIndex(openedWindow));
			}
			openedWindow.addWindowListener(this);
		}
	}
	
	/**
	 * Conduct the proper measures to re-configure this window for capturing interaction events
	 * after it has already been set up for capture one time. 
	 * Note that this method only replaces interactions from a window that has already entered
	 * capture.
	 * 
	 * Preconditions: openedWindow is not null
	 * 					if this window never entered capture before, an error message is printed, and 
	 * 						the method returns. 
	 * 					Else: a new testInteractions module is registered for openedWindow
	 * 					buttons and interactibles of openedWindow are picked up by java listener and recorded to 
	 * 					  testInteractions module
	 * 					A close event enacted on openedWindow will cause openedWindow to be removed from capture.
	 */
	protected boolean reRip(Window openedWindow)
	{
		int currentWindowIndex = lookupWindowIndex(openedWindow); 
		if(currentWindowIndex == -1)
			return false;
		
		interactionsByWindow.set(currentWindowIndex, new JavaTestInteractions());
		
		// ensure that we can't be interrupted by user input by using a tree lock
		Object awtTreeLock = openedWindow.getTreeLock();
		synchronized(awtTreeLock) {
			// traversing the tree for items.
			interactionsByWindow.get(currentWindowIndex).setCurrentWindow(openedWindow);
			traverseComponentGraph(openedWindow, false, currentWindowIndex);
			traverseComponentGraph(openedWindow, true, currentWindowIndex);
		}
		return true;
	}
	
	/**
	 *  Stop capturing events from toUnCapture. Events from toUncapture are no longer sent to 
	 *  this captureMonitor's testCase object.
	 * 
	 *  Preconditions: 	toUnmonitor is not null. 
	 *  Postconditions: toUnCapture is not in the windowsInCapture list
	 * 					A java listener is not registered for toUnCapture
	 * 					A testInteractions module is not registered for toUnCapture
	 * 					buttons and interactibles of openedWindow are no longer picked up by a java listener and are no longer
	 * 						recorded to a testInteractions module
	 * 					toUnCapture's window-related interactions are not registered with the windowListener methods
	 * 					    of this class.
	 * 					Events enacted by the user on openedWindow are not sent to testCase for post-processing.
	 * @param toUnmonitor
	 */
	protected void endCaptureOn(Window toUnCapture)
	{	
		// do everything from start except backwards and in the negative
		
		int windowIndex = lookupWindowIndex(toUnCapture);
		
		if(windowIndex != -1) { // only take action if toUnCapture is actually in the capture.
			// remove the window interaction listener
			toUnCapture.removeWindowListener(this);
			
			// remove listeners attached to buttons
			Object awtTreeLock = toUnCapture.getTreeLock();
			synchronized(awtTreeLock) {
				interactionsByWindow.get(windowIndex).setCurrentWindow(toUnCapture);
				
				traverseComponentGraph(toUnCapture, false, windowIndex);
			}
			// deregister listeners associated with this window.
			javaListeners.remove(windowIndex);	
			
			// remove recorded JTI because it's no longer needed after capture is over.
			interactionsByWindow.remove(windowIndex);
			
			// remove the window from capture list
			windowsInCapture.remove(windowIndex);
		}
	}	
	/** 
	 * 
	 * Stops the capture process of any window currently in capture. 
	 * 
	 * Preconditions: none
	 * Postconditions: This JavaCaptureMonitor is no longer capturing elements of windows previously involved in capture.
	 */
	public void stopCapture()
	{	
		for(Window w : Window.getWindows())
			endCaptureOn(w);
		JavaListener.stopCapturing();
		specialQueue.stopReceiving();
		
		System.out.println(DebugMessage.CAPTURE_STOPPED);
	}
	
	
	/**
	 * Returns the integer ID matching the window in capture corresponding to
	 * the windowNameID (accessible name) represented in targetWindowName.
	 * If target is being captured by this JavaCaptureMonitor, this ID can be used
	 * to perform further lookups in the listener list and the interactions list.
	 * Returns -1 if no window bearing targetWindowName is being captured.
	 * 
	 * Preconditions: 	none
	 * Postconditions:	if a window bearing targetWindowName is being captured, the ID of target is returned. 
	 */
	private int lookupWindowIndex(String targetWindowName) 
	{
		for(int i = 0; i < windowsInCapture.size(); i++) {
			Window w = windowsInCapture.get(i);
			AccessibleContext ac = w.getAccessibleContext();
			if(ac != null && ac.getAccessibleName() != null) {
				String windowName = ac.getAccessibleName();
				if(JavaTestInteractions.windowTitlesAreSame(windowName, targetWindowName)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Returns the integer ID matching the window in capture corresponding to target
	 * if target is being captured by this JavaCaptureMonitor, that can be used to perform
	 * lookups in the listener list and the interactions list.
	 * Returns -1 if target is not being captured.
	 * The ID can be used to index the map of captured windows.
	 * 
	 * Preconditions: 	none
	 * Postconditions:	if target is being captured, the ID of target is returned. 
	 */
	private int lookupWindowIndex(Window target)
	{
		AccessibleContext targetAC = target.getAccessibleContext();
		if(targetAC == null)
			return -1;
		
		for(int i = 0; i < windowsInCapture.size(); i++) {
			Window w = windowsInCapture.get(i);
			AccessibleContext ac = w.getAccessibleContext();
			if(ac != null && ac.getAccessibleName() != null)
				if(ac.getAccessibleName().equals(targetAC.getAccessibleName()))
					return i;
		}
		return -1;
	}
	
	
	/**
	 * Return false if toTest is null, is a tool tip or a menu, is nameless, or has a name matching "CogTool Helper". 
	 * Returns true otherwise.
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	true is returned if capture is allowed. False is returned otherwise. 
	 */
	private boolean captureIsAllowedOn(Window toTest)
	{
		if(toTest == null) 
			return false;
		
		String windowName = toTest.getAccessibleContext().getAccessibleName();
		
		if(windowName == null)
			return false;
		if(windowName.equals("CogTool Helper"))
			return false;
		
		AccessibleRole windowRole = toTest.getAccessibleContext().getAccessibleRole();
		if( windowRole.equals(AccessibleRole.TOOL_TIP) ||
			windowRole.equals(AccessibleRole.MENU)) {
				return false;
		}
		
		// all tests passed
		return true;
	}
	
	public void traverseMenuBarGraph(final Window nextComponent, boolean forRegistration, int listenerIndex)
	{
		ArrayList<MenuElement> subElements = new ArrayList<MenuElement>();
		
		if(nextComponent instanceof JDialog) {
			JDialog w = (JDialog) nextComponent;
			JMenuBar treeRoot = w.getJMenuBar();
			if(treeRoot == null)
				return;
			for(MenuElement me : treeRoot.getSubElements()) 
				subElements.add(me);
		}
		else if(nextComponent instanceof JFrame) {
			JFrame w = (JFrame) nextComponent;
			JMenuBar treeRoot = w.getJMenuBar();
			if(treeRoot == null)
				return;
			for(MenuElement me : treeRoot.getSubElements()) 
				subElements.add(me);
		}
		for(MenuElement e : subElements) 
			traverseMenuElementGraph(e, forRegistration, listenerIndex);
	}
	public void traverseMenuElementGraph(final MenuElement nextElement, boolean forRegistration, int listenerIndex)
	{
		Component nextComponent = (Component)nextElement;
		
		@SuppressWarnings("unused")
		String eventId = "";
		if(nextComponent instanceof Accessible) {
			AccessibleRole componentRole = ((Accessible) nextComponent).getAccessibleContext().getAccessibleRole();
			if(forRegistration) {
				
				// for menus (don't attach a listener) 
				if(componentRole.equals(AccessibleRole.MENU)) {
					eventId = interactionsByWindow.get(listenerIndex).saveNewMenuEventID(nextComponent);
				}
				// for menu items
				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
					javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
					eventId = interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
				}
				// for radio buttons
				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) {
					javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
					eventId = interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
				}// for check boxes
				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) {
					
					javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
					eventId = interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
				}
			}
			else {
				if(componentRole.equals(AccessibleRole.MENU_ITEM))
					javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) 
					javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) 
					javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
			}
		}
		for(MenuElement nextChild : nextElement.getSubElements()) 
			traverseMenuElementGraph(nextChild, forRegistration, listenerIndex);
	}
	
	
	/**
	 * This method selects children of the interact-able 
	 * referenced by root, and registers them properly 
	 * with the java listener if it can be registered. Following
	 * a call to this method, all actions taken upon root, 
	 * and children of root that involve the following options 
	 * are recorded: 
	 * text editing, button pressing, selection among radio button or checkbox choices, selection among page tabs,
	 * selection among menu items, selection among flat lists, selections within table cells. 
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	Based on which capture features are turned on immediately preceding a call to this method
	 * 					1. the component root and all its children now send 
	 * 				   	  events to the JavaListener in javaListeners specified 
	 * 				   	  by listenerIndex
	 * 					2. the component root and all its children are recorded 
	 * 					  in the local JavaTestInteractions object registered at index "listenerIndex" for this capture 
	 * 					  if forRegistration is true.
	 */
	protected void traverseComponentGraph(final Component nextComponent, boolean forRegistration, int listenerIndex) 
	{
		if(nextComponent == null)
			return;
		
		if(nextComponent instanceof Window) {
			if(menuCapture) 
				traverseMenuBarGraph((Window)nextComponent, forRegistration, listenerIndex);
		}
		else if( nextComponent instanceof Accessible && 
				 nextComponent.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.MENU_BAR))
					return; // do not parse menu bars in this method.
		AccessibleRole componentRole = null;
		if(nextComponent instanceof Accessible) {
			componentRole = nextComponent.getAccessibleContext().getAccessibleRole();
			if(forRegistration)  {
				// for panels that you can click or type into
				if(componentRole.equals(AccessibleRole.PANEL)) {
					if(buttonCapture && JFCXComponent.hasListeners(nextComponent, "button")) {
						javaListeners.get(listenerIndex).registerDirectClickComponent(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewMouseInputPanelEventID(nextComponent);
					}
					if(textCapture && JFCXComponent.hasListeners(nextComponent, "textbox")) {
						javaListeners.get(listenerIndex).registerText(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewTypingPanelEventID(nextComponent);
					}
				}
				// for text elements
				else if(componentRole.equals(AccessibleRole.TEXT)) {
					if(textCapture) {
						boolean comboText = findRoleAbove(AccessibleRole.COMBO_BOX, nextComponent, 2);
						if(comboCapture && comboText) 
							javaListeners.get(listenerIndex).registerComboBoxText(nextComponent);
						
						else if(!comboText) {
							javaListeners.get(listenerIndex).registerText(nextComponent);	
							interactionsByWindow.get(listenerIndex).saveNewTextEventID(nextComponent);
						}
					}
				}
				
				// for button elements 
				else if(componentRole.equals(AccessibleRole.PUSH_BUTTON)) {
					boolean comboButton = false;
					comboButton = findRoleAbove(AccessibleRole.COMBO_BOX, nextComponent, 2);
					if(comboCapture && comboButton) 
						javaListeners.get(listenerIndex).registerComboBoxButton(nextComponent);	
					
					else if(buttonCapture && !comboButton) {
						javaListeners.get(listenerIndex).registerButton(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewPushButtonEventID(nextComponent);
					}
				}
				
				// for page tabs
				else if(componentRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
					if(tabchangeCapture) {
						javaListeners.get(listenerIndex).registerTabList(nextComponent);
						if(nextComponent instanceof JTabbedPane) 
							((JTabbedPane)nextComponent).addChangeListener(new ReRipAction(SwingUtilities.getWindowAncestor(nextComponent)));
						interactionsByWindow.get(listenerIndex).saveNewTabListEventID(nextComponent);
					}
				}
		
				// for menu items
				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
					if(menuCapture) {
						javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
					}
				}
				
				// for menus (don't attach a listener) 
				else if(componentRole.equals(AccessibleRole.MENU)) {
					if(menuCapture) 
						interactionsByWindow.get(listenerIndex).saveNewMenuEventID(nextComponent);
				}
				
				// for radio buttons
				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) {
					if(nextComponent instanceof MenuElement) {
						if(menuCapture) {
							javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
							interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
						}
					}
					else if(radioCapture) {
						javaListeners.get(listenerIndex).registerButton(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewRadioButtonEventID(nextComponent);
					}
				}
				
				// for flat lists
				else if(componentRole.equals(AccessibleRole.LIST)) {
					javaListeners.get(listenerIndex).registerListSelector(nextComponent);
					interactionsByWindow.get(listenerIndex).saveNewListEventID(nextComponent);
				}
				// for toggle buttons
				else if(componentRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
					if(buttonCapture || hButtonCapture) {
						javaListeners.get(listenerIndex).registerToggleButton(nextComponent);
						int hoverCode;
						if(buttonCapture && hButtonCapture)
							hoverCode = JavaTestInteractions.W_HOVER; // typical
						else if(buttonCapture)
							hoverCode = JavaTestInteractions.NO_HOVER; // typical
						else
							hoverCode = JavaTestInteractions.HOVER_ONLY; // rare
						interactionsByWindow.get(listenerIndex).saveNewToggleButtonEventIDs(nextComponent, hoverCode);
					}		
				}
				// for combo boxes
				else if(componentRole.equals(AccessibleRole.COMBO_BOX)) {
					if(comboCapture) {
						javaListeners.get(listenerIndex).registerComboBoxComponent(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewComboBoxEventIDs(nextComponent);
					}
				}
				// for check boxes
				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) {
	
					if(nextComponent instanceof MenuElement) {
						if(menuCapture) {
							javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
							interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
						}
					}
					else 
						if(checkboxCapture) {	
						javaListeners.get(listenerIndex).registerButton(nextComponent);
						interactionsByWindow.get(listenerIndex).saveNewCheckboxEventID(nextComponent);
					}
				}
				
				else if(componentRole.equals(AccessibleRole.TABLE)) {
					javaListeners.get(listenerIndex).registerTable(nextComponent);
					interactionsByWindow.get(listenerIndex).saveNewTableEventID(nextComponent);
				}
				
				else if(componentRole.equals(AccessibleRole.POPUP_MENU)) { 
					// assumes we know where in the hierarchy the window will be. 
					if(contextboxCapture) {
						// none of these tests are good at discovering the special role of a popup menu
						// except for perhaps menuAbove
						boolean barAbove = findRoleAbove(AccessibleRole.MENU_BAR, nextComponent, 1);
						boolean menuAbove = findRoleAbove(AccessibleRole.MENU, nextComponent, 1);
						
						if(!(menuAbove || barAbove)) {
							// we have to capture the clicking of an item within a context box.
							javaListeners.get(listenerIndex).registerContextboxPotential(nextComponent);
							interactionsByWindow.get(listenerIndex).saveNewContextboxPotentialEventID(nextComponent);
						}
					}
				}
			}
			else {
				if(componentRole.equals(AccessibleRole.PANEL)) {
					if(buttonCapture)
						javaListeners.get(listenerIndex).unRegisterDirectClickComponent(nextComponent);
					if(textCapture)
						javaListeners.get(listenerIndex).unRegisterText(nextComponent);
				}
				
				else if(componentRole.equals(AccessibleRole.TEXT)) {
					if(textCapture)
						javaListeners.get(listenerIndex).unRegisterText(nextComponent);
					if(comboCapture) 
						javaListeners.get(listenerIndex).unregisterComboBoxText(nextComponent);
				}
				else if(componentRole.equals(AccessibleRole.PUSH_BUTTON)) {
					boolean comboButton = false;
					comboButton = findRoleAbove(AccessibleRole.COMBO_BOX, nextComponent, 2);
					if(comboCapture && comboButton) 
						javaListeners.get(listenerIndex).unRegisterComboBoxButton(nextComponent);	
					else if(buttonCapture && !comboButton) 
						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
				}
				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
					if(menuCapture)
						javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
				}
				
				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) {
					if(nextComponent instanceof MenuElement) {
						if(menuCapture) 
							javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
					}
					else if(radioCapture) 
						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
				}
				else if(componentRole.equals(AccessibleRole.LIST)) {
					if(listCapture) 
						javaListeners.get(listenerIndex).unRegisterListSelector(nextComponent);
				}
				else if(componentRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
					if(buttonCapture) 
						javaListeners.get(listenerIndex).unRegisterToggleButton(nextComponent);
				}
				else if(componentRole.equals(AccessibleRole.COMBO_BOX)) {
					if(comboCapture)
						javaListeners.get(listenerIndex).unRegisterComboBoxComponent(nextComponent);
				}
				else if(componentRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
					if(tabchangeCapture) {
						javaListeners.get(listenerIndex).unRegisterTabList(nextComponent);
						if(nextComponent instanceof JTabbedPane) {
							for(ChangeListener cl : (((JTabbedPane)nextComponent).getListeners(ChangeListener.class)))
								if(cl instanceof ReRipAction)
									 ((JTabbedPane)nextComponent).removeChangeListener(cl);
						}
					}
				}
				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) {
					if(nextComponent instanceof MenuElement) {
						if(menuCapture) 
							javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
					}
					else if(checkboxCapture) 
						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
				}
				
				else if(componentRole.equals(AccessibleRole.TABLE)) {
					if(tableCapture)
						javaListeners.get(listenerIndex).unRegisterTable(nextComponent);
				}
			}
		}		
		
		if(nextComponent instanceof Accessible && componentRole.equals(AccessibleRole.COMBO_BOX)) 
			for(Component nextChild: ((Container)nextComponent).getComponents()) 
				traverseComponentGraph(nextChild, forRegistration, listenerIndex);
		else if(nextComponent instanceof Accessible && nextComponent instanceof MenuElement) 
			traverseMenuElementGraph((MenuElement)nextComponent, forRegistration, listenerIndex);
		
//		else if(nextComponent instanceof Accessible && componentRole.equals(AccessibleRole.POPUP_MENU)) {
//			traversePopupMenuGraph(nextComponent, forRegistration, listenerIndex);
//		}
		
		// now process this root's children if root is a container, but not a menu element. 
		else if(nextComponent instanceof Container) {
			Component[] childComponents = ((Container)nextComponent).getComponents();
			for(Component nextChild : childComponents) 
				traverseComponentGraph(nextChild, forRegistration, listenerIndex);
			if(nextComponent instanceof JComponent) {
				JPopupMenu compPopup = ((JComponent)nextComponent).getComponentPopupMenu();
				if(compPopup != null) 
					traverseComponentGraph(compPopup, forRegistration, listenerIndex);
			}
		}
	}
	
	
	/**
	 * Flip the switch for all hover related capture to be either on or off.<br>
	 * 
	 * This method must be called before attachAndStartListeners 
	 * is called, or this method will have no effect until the
	 * attachAndStartListeners is called a second time.  
	 */
	public void switchHoverCapture(boolean on)
	{
		hTextCapture = hButtonCapture = hMenuCapture = hRadioCapture = 
				hListCapture = hComboCapture = hCheckboxCapture = 
				hContextboxCapture = hTabchangeCapture = hTableCapture = on;
	}
	
	/** 
	 * Turns off capture of certain interactables within the java application.<br>
	 * This method must be called before attachAndStartListeners 
	 * is called, or this method will have no effect until the
	 * attachAndStartListeners is called a second time.  
	 * 
	 * Preconditions: 	each string in captureTypes represents a valid widget string - 
	 * 					"text", "button", "menu", "radio", "list", "combo", "checkbox", 
	 * 					  "contextbox", "tabchange", "table", or their plural variants. 
	 * 					If an element of captureTypes is a valid capture type specified above, that capture 
	 * 					  every element of that capture type in the UI will be listened to by at least one java 
	 * 					  listener when traverse tree is called on a window in capture following a call to this method. 
	 * Postconditions: 	Capture for each of the widgets specified in capture types is turned on. 
	 * 					If an element of captureTypes is a valid capture type specified above, that capture 
	 * 					  every element of that capture type in the UI will make at least one java 
	 * 					  listener listen to its fired events when traverse tree is called on a window in capture 
	 * 					   following a call to this method.
	 */
	public void turnOnCaptureOf(String... captureTypes)
	{
		for(String s : captureTypes)
			switch(s.toLowerCase()) {
				case "text"				: textCapture = true; break;
				case "texthover"		: hTextCapture = false; break;
				case "button" 			:
				case "buttons" 			: buttonCapture = true; break;
				case "buttonhover"		: 
				case "buttonshover"		: hButtonCapture = true; break;
				case "menu"				: 
				case "menus"			: menuCapture = true; break;
				case "menuhover"		: 
				case "menushover"		: hMenuCapture = true; break;
				case "radio"			: 
				case "radios" 			: radioCapture = true; break;
				case "radiohover"		: 
				case "radioshover"		: hRadioCapture = true; break;
				case "list"				: 
				case "lists"			: listCapture = true; break;
				case "listhover"		: 
				case "listshover"		: hListCapture = true; break;
				case "combo" 			: 
				case "combos" 			: comboCapture = true; break;
				case "comobhover"		: 
				case "comboshover"		: hComboCapture = true; break;
				case "checkbox"			: 
				case "checkboxes" 		: checkboxCapture = true; break;
				case "checkboxhover"	: 
				case "checkboxeshover" 	: hCheckboxCapture = true; break;
				case "contextbox" 		: 
				case "contextboxes"		: contextboxCapture = true; break;
				case "contextboxhover"	:
				case "contextboxeshover": hContextboxCapture = true; break;
				case "tabchange"		: 
				case "tabchanges"		: tabchangeCapture = true; break;
				case "tabchangehover"	: 
				case "tabchangeshover"	: hTabchangeCapture = true; break;
				case "table"			: 
				case "tables"			: tableCapture = true; break;
				case "tablehover"		: 
				case "tableshover"		: hTableCapture = true; break;
//				case "all"		: textCapture = buttonCapture = menuCapture = listCapture = comboCapture = 
// 								  checkboxCapture = contextboxCapture = tabChangeCapture = tableCapture = true; break;
				// little need for this option
			}
	}
	
	/**
	 * Turn on capture of certain interactables within the java application.
	 * This method must be called before attachAndStartListeners 
	 * is called, or this method will have no effect until the 
	 * attachAndStartListeners is called a second time. 
	 *  
	 * Preconditions: 	each string in captureTypes represents a valid widget string - 
	 * 					"text", "button", "menu", "radio", "list", "combo", "checkbox", or their plural variants. 
	 * Postconditions: 	Capture for each of the widgets specified in capture types is turned off. 
	 * 					If an element of captureTypes is a valid capture type specified above, that capture 
	 * 					  every element of that capture type in the UI will make at least one java 
	 * 					  listener listen to its fired events when traverse tree is called on a window 
	 * 					  in capture following a call to this method.
	 */
	public void turnOffCaptureOf(String... captureTypes)
	{
		for(String s : captureTypes) 
			switch(s.toLowerCase()) {
				case "text"				: textCapture = false; break;
				case "texthover"		: hTextCapture = false; break;
				case "button"			:
				case "buttons" 			: buttonCapture = false; break;
				case "buttonhover"		: 
				case "buttonshover"		: hButtonCapture = false; break;
				case "menu"				:
				case "menus"			: menuCapture = false; break;
				case "menuhover"		: 
				case "menushover"		: hMenuCapture = false; break;
				case "radio" 			: 
				case "radios" 			: radioCapture = false; break;
				case "radiohover"		: 
				case "radioshover"		: hRadioCapture = false; break;
				case "list"				: 
				case "lists"			: listCapture = false; break;
				case "listhover"		: 
				case "listshover"		: hListCapture = false; break;
				case "combo"			:
				case "combos"			: comboCapture = false; break;
				case "comobhover"		: 
				case "comboshover"		: hComboCapture = false; break;
				case "checkbox"			: 
				case "checkboxes"		: checkboxCapture = false; break;
				case "checkboxhover"	: 
				case "checkboxeshover" 	: hCheckboxCapture = false; break;
				case "contextbox"		: 
				case "contextboxes"		: contextboxCapture = false; break;
				case "contextboxhover"	:
				case "contextboxeshover": hContextboxCapture = false; break;
				case "tabchange" 		: 
				case "tabchanges"		: tabchangeCapture = false; break;
				case "tabchangehover"	: 
				case "tabchangeshover"	: hTabchangeCapture = false; break;
				case "table"			:
				case "tables"			: tableCapture = false; break;
				case "tablehover"		: 
				case "tableshover"		: hTableCapture = false; break;
//				case "all" 				: textCapture = buttonCapture = menuCapture = radioCapture = listCapture = comboCapture = true;
			}
	}
	
	/**
	 *  If capturing has been started, WindowEvents sent to windows in the capture 
	 *  are printed to console if printOn is true. If printOn is false, WindowEvents from this
	 *  JavaCaptureMonitor are never printed to the console.
	 * 
	 *  Preconditions: 	Test case has already been initialized 
	 *  Postconditions:	WindowEvents on windows in capture will be printed when 
	 * 					they are triggered. 
	 */
	public void setWindowEventPrint(boolean printOn)
	{
		windowEventPrint = printOn;
	}
	
	
	private static void debugStatusMessage(DebugMessage messageType)
	{
		System.out.println(messageType);
	}

	private static boolean isModalWindow(Window w) 
	{
		if(w.getAccessibleContext() != null)
			if(w.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.MODAL))
				return true;
		return false;
	}
	
	/**
	 * If capture is allowed on the window opposite from the one being activated, and the 
	 * window is not already being captured, start capturing that window opposite. 
	 * 
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowDeactivated(WindowEvent e)
	{
		if(windowEventPrint) System.out.println(e);

		// if a switch is to be done, do things, otherwise, do nothing 
		Window discoveredWindow = e.getOppositeWindow();
		
		if(captureIsAllowedOn(discoveredWindow) && lookupWindowIndex(discoveredWindow) == -1) {
		 	debugStatusMessage(DebugMessage.WAIT);
		 	startCaptureOn(discoveredWindow);
		 	debugStatusMessage(DebugMessage.CAPTURE_UPDATED);
		 	if(isModalWindow(discoveredWindow))
				modals.add(discoveredWindow);
		}
	}
	
	/**
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowClosed(WindowEvent e) { 
		if(windowEventPrint) 
			System.out.println(e); 
		 
		if(modals.contains(e.getWindow()))
			modals.remove(e.getWindow());
		
		endCaptureOn(e.getWindow());
	}	
	/**
	 * If capture is allowed on this new window. End capture on whatever window was switched out of
	 * to get here. Note that in Swing, window deactivated is always called before window activated. 
	 * 
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowActivated(WindowEvent e) 
	{ 
		if(windowEventPrint) System.out.println(e); 
	}
	
	
	/**
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowClosing(WindowEvent e) 
	{ 
		if(windowEventPrint) 
			System.out.println(e); 
		 
		if(modals.contains(e.getWindow()))
			modals.remove(e.getWindow());
		
		endCaptureOn(e.getWindow());
		if(windowsInCapture.isEmpty()) {
			DebugMessage.CAPTURE_CLOSED_ALL_WINDOWS.print();
			taskList.interrupt();
		}
	}
	
	/**
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowOpened(WindowEvent e) { if(windowEventPrint) System.out.println(e); }
	

	/**
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowDeiconified(WindowEvent e) { if(windowEventPrint) System.out.println(e); }
	
	/**
	 * Print the caught event if windowEventPrint is on
	 */
	public void windowIconified(WindowEvent e) { if(windowEventPrint) System.out.println(e); }
	
	/** 
	 * Class designed for printing debugging messages related to capture. 
	 */
	public enum DebugMessage {
		
		WAIT("JavaCaptureMonitor: Setting up capture, please wait."),
		CAPTURE_UPDATED("JavaCaptureMonitor: Ready to capture actions on opened window."),
		CAPTURE_STOPPED("JavaCaptureMonitor: Capture session is finished."),
		CAPTURE_CANCELED("JavaCaptureMonitor: No application windows were provided that could be captured. Capture was canceled."),
		CAPTURE_CLOSED_ALL_WINDOWS("JavaCaptureMonitor: No more windows are available to capture. Stopping capture"),
		CAPTURE_NEW_WINDOW("JavaCaptureMonitor: New Window Found."),
		CAPTURE_WINDOW_AGAIN("JavaCaptureMonitor: Rescanning Window Elements");
		private String messageString;
		
		private DebugMessage(String messageString)
		{
			this.messageString = messageString;
		}
		
		public String toString()
		{
			return messageString;
		}
		
		public void print(String... parameters)
		{
			switch(this) {
				default:	System.out.println(messageString); 
			}
		}
	}
	
	/**
	 * Searches for the role specified if it can be found within n ancestors in component's parent hierarchy 
	 * (where n = stepsToLook).  Return false if the role specified doesn't exist up to and including n steps up from components
	 * position in the component hierarchy. Else, return true.
	 */
	private boolean findRoleAbove(AccessibleRole targetRole, Component component, int stepsToLook) 
	{
		Accessible bParent;
		bParent = getProperParent((Accessible)component);
		int pCount = 0;
		while(bParent != null && pCount < stepsToLook) {
			if(bParent.getAccessibleContext().getAccessibleRole().equals(targetRole))
				break;
			pCount++;
			if(bParent.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.POPUP_MENU)) {
				Component menuParent = ((JPopupMenu)bParent).getInvoker();
				if(menuParent.getAccessibleContext() == null)
					return false;
				bParent = menuParent.getAccessibleContext().getAccessibleParent();
			}
			else
				bParent = bParent.getAccessibleContext().getAccessibleParent();
		}
		
		if(bParent != null && bParent.getAccessibleContext().getAccessibleRole().equals(targetRole))
			return true;
		return false;
	}
	
	/**
	 * Return the accessible parent of this accessible object. If the object is a windowless popupMenu, its parent
	 * is returned as the invoker of the popup menu. 
	 */
	private Accessible getProperParent(Accessible c) {
		if(c.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.POPUP_MENU)) {
			Component invoker = ((JPopupMenu)c).getInvoker();
			if(invoker == null || invoker.getAccessibleContext() == null)
				return null;
			return (Accessible)invoker;
		}
		else
			return c.getAccessibleContext().getAccessibleParent();
	}
	
	/**
	 * Process the reception of a hover event via this Capture Monitor
	 * @param w
	 * @param c
	 * @param mouseXPosition
	 * @param mouseYPosition
	 */
	private void processHoverEvent(Window w, Component c, int mouseXPosition, int mouseYPosition)
	{
		
		int i = lookupWindowIndex(w);
		if(i == -1)
			return;
		if(doCaptureHover(c)) {
			javaListeners.get(i).getMouseHoverListener().captureHover(c, mouseXPosition, mouseYPosition);
		}
	}
	
	private boolean doCaptureHover(Component c)
	{
		if(c == null)
			return false;
		AccessibleContext aContext = c.getAccessibleContext();
		if(aContext == null)
			return false;
		AccessibleRole myRole = c.getAccessibleContext().getAccessibleRole();
		
		if(myRole.equals(AccessibleRole.TEXT)) {
			if(!hTextCapture)
				return false;
		}
		else if(myRole.equals(AccessibleRole.PUSH_BUTTON) ||
				myRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
			if(!hButtonCapture)
				return false;
			else
				return true; // this is temporary, we're just testing buttons now!
		}
		return false;
//		else if()
//		myRole.equals(AccessibleRole.RADIO_BUTTON) ||
//		myRole.equals(AccessibleRole.MENU) ||
//		myRole.equals(AccessibleRole.MENU_ITEM) ||
//		myRole.equals(AccessibleRole.CHECK_BOX) ||
//		
//		else if(myRole.equals(AccessibleRole.COMBO_BOX)) {
//			if(JFCXComponent.hasChildren(c))
//				hoverType = "basic"; // this must be top level.
//			else
//				hoverType = "parental"; // this must be an element in the list
//		}
//		
//		else if(myRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
//			if(JFCXComponent.hasChildren(c))
//				hoverType = "basic"; // this must be the top level. 
//			else
//				hoverType = "parental"; // this must be an element in the list. 
//		}
//		
//		else if(myRole.equals(AccessibleRole.PANEL)) 
//			hoverType = "basic";
//		
//		else if(myRole.equals(AccessibleRole.LIST))
//			hoverType = "parental";
	}
	private class JavaSpecialEventQueue extends EventQueue implements FocusListener, KeyEventDispatcher
	{
		boolean popupRaised;
		KeyboardFocusManager currentManager;
		boolean receivingKeys;
		public JavaSpecialEventQueue()
		{
			receivingKeys = false;
		}
		
		public void setKeyboardManager()
		{
			currentManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			currentManager.addKeyEventDispatcher(this);
			receivingKeys = true;
		}
		
		public void unsetKeyboardManager()
		{
			currentManager.removeKeyEventDispatcher(this);
		}
		
		/**
		 * Stop processing events through this event queue. 
		 * Return true if this event queue was currently dispatching events and has now stopped.
		 * Return false if nothing happened, and the stack was empty prior to the call.
		 * 
		 *  Preconditions: none
		 *  Postconditions: this event queue is no longer dispatching events. 
		 */
		public boolean stopReceiving()
		{
			try {
				pop();
				if(receivingKeys)
					unsetKeyboardManager();
			} catch(EmptyStackException e) {
				return false;
			}
			return true;
		}
		
		public boolean goodCE(AWTEvent event)
		{
			if(!(event instanceof ComponentEvent))
				return false;
			
			boolean useful = !( 
//				(event instanceof PeerEvent)
				(event instanceof PaintEvent)
				|| (event instanceof MouseEvent) 
				|| (event instanceof KeyEvent) 
				|| (event instanceof FocusEvent) 
				|| (event instanceof WindowEvent) 
				);
			if(useful)
				return true;
			return false;
		}
		
		private boolean usedPopup(AWTEvent event)
		{
			Object source = event.getSource();
			if(source instanceof Window) {
				Window w = (Window)source;
				if(w.getType() == Window.Type.POPUP) 
					return true;
			}
			return false;
		}
		
		@Override
		protected void dispatchEvent(AWTEvent awte)
		{
			
			if(goodCE(awte)) {
				int awtID = awte.getID();
				if(usedPopup(awte) 
				&& !popupRaised 
				&& awtID != ComponentEvent.COMPONENT_HIDDEN) {
					popupRaised = true;
					if(awtID == ComponentEvent.COMPONENT_SHOWN) {
						Window popup = getWindowFromComponent(awte.getSource());
						if(captureIsAllowedOn(popup)) 
							startCaptureOn(popup);
					}
				}
				else if(awte instanceof PaintEvent) {
					if(popupRaised)   
						popupRaised = false;
					else {
						Window w = getWindowFromComponent(awte.getSource());
						// if window is in capture and is not valid yet, rerip it. 
						boolean inCapture = windowsInCapture.contains(w);
						if(inCapture && !w.isValid()) 
							reRip(w);
						// if window is not currently in capture, and is valid, don't rerip it.
						else if(!inCapture && w.isValid() && captureIsAllowedOn(w)) {
							DebugMessage.CAPTURE_NEW_WINDOW.print();
							startCaptureOn(w);
							DebugMessage.CAPTURE_UPDATED.print();
						}
						
					}
				}
			}
			
			super.dispatchEvent(awte);
		}
		
		
		public Window getWindowFromComponent(Object compObject)
		{
			Component comp = (Component)compObject;
			if(comp instanceof Window) 
				return (Window)comp;
			else
				return SwingUtilities.getWindowAncestor(comp);
		}

		@Override
		public void focusGained(FocusEvent e) 
		{	
//			System.out.println("JavaCaptureMonitor: Reripping window due to changed elements, please wait.");
//			reRip(w);
//			w.removeFocusListener(this);
//			System.out.println("JavaCaptureMonitor: Ready to capture window. Please begin performing steps.");
		}
		
		public void focusLost(FocusEvent fe) {/*Nothing done on focus lost*/}
		
		@Override
		public boolean dispatchKeyEvent(KeyEvent e) 
		{
			Window w = currentManager.getActiveWindow();
			if(e.getID() == KeyEvent.KEY_PRESSED) { // if we pressed ctrl_shift_R
				int code = e.getKeyCode();
				int mask = e.getModifiersEx();
				int captureMask = KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK;
				if((mask & captureMask) == captureMask)  {
					if(code == KeyEvent.VK_R) {
						reRip(w);
						DebugMessage.CAPTURE_WINDOW_AGAIN.print();
						return true;
					}
					else if(code == KeyEvent.VK_H) {
						// thanks goes out to 
						// http://stackoverflow.com/questions/2733896/identifying-swing-component-at-a-particular-screen-coordinate-and-manually-dis 
						
						Point mouse = MouseInfo.getPointerInfo().getLocation();
						Point origMouse = (Point)mouse.clone();
						mouse.x -= w.getLocationOnScreen().x;
						mouse.y -= w.getLocationOnScreen().y;
						
						Component underneath = SwingUtilities.getDeepestComponentAt(w, mouse.x, mouse.y);
						Point underPoint = underneath.getLocationOnScreen();
						int mouseHovX = origMouse.x - underPoint.x;
						int mouseHovY = origMouse.y - underPoint.y;
						processHoverEvent(w, underneath, mouseHovX, mouseHovY);
					}
				}
			}
			
			return false;
		};
	}
	private class ReRipAction implements ChangeListener
	{
		private final Window reRipWindow;
		public ReRipAction(Window window)
		{
			reRipWindow = window;
		}
		public void stateChanged(ChangeEvent ae) 
		{
			reRip(reRipWindow);
		}
	}
}
