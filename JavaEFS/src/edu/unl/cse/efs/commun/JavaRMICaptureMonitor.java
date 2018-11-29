package edu.unl.cse.efs.commun;

import java.awt.Window;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.Collection;
import javax.accessibility.*;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;
import edu.unl.cse.efs.java.JavaCaptureMonitor;
import edu.unl.cse.efs.java.JavaCaptureMonitor.DebugMessage;
import edu.unl.cse.guitarext.JavaTestInteractions;

/**
 * Source for the JavaCaptureMonitor class. This class instantiates
 * classes that enable a test case to receive events when interactibles on
 * the provided windows are interacted with. If these windows open a new window
 * the interactables present in that window will also send events to the test case.
 * @author Jonathan Saddler
 */
public class JavaRMICaptureMonitor extends JavaCaptureMonitor implements WindowListener {

	private boolean windowEventPrint;
	private NetCommunication saver;

	/**
	 * Constructor for JavaCaptureMonitor. Collects the windows to capture,
	 * and stores them in AppRelatedWindows. Finally, initialize the list
	 * of java listeners that will be used to capture these windows.
	 * @param windowsToCapture
	 */
	public JavaRMICaptureMonitor(Collection<GWindow> windowsToCapture, NetCommunication networkStub)
	{
		super(windowsToCapture);
		saver = networkStub;
	}



	/**
	 * Start capturing interaction events from openedWindow. Events
	 * are sent by the appropriate Java AWTEvent listeners to the JavaCaptureTestCase object in this
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
			JavaTestInteractions openedWindowInteractions = new JavaTestInteractions();

			javaListeners.add(new JavaRMIListener(windowNameID, saver, openedWindowInteractions));
			interactionsByWindow.add(openedWindowInteractions);

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
	 *
	 * Stops the capture process of any window currently in capture.
	 *
	 * Preconditions: none
	 * Postconditions: This JavaCaptureMonitor is no longer capturing elements of windows previously involved in capture.
	 */
	public void stopCapture()
	{
		while(!windowsInCapture.isEmpty())
			endCaptureOn(windowsInCapture.get(windowsInCapture.size()-1));

		JavaRMIListener.stopCapturing();
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
	@SuppressWarnings("unused")
	private int lookupWindowIndex(String targetWindowName)
	{
		for(int i = 0; i < windowsInCapture.size(); i++) {
			Window w = windowsInCapture.get(i);
			AccessibleContext ac = w.getAccessibleContext();
			if(ac != null && ac.getAccessibleName() != null)
				if(ac.getAccessibleName().equals(targetWindowName))
					return i;
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

//

	/**
	 * Turns on capture of certain interactables within the java application.
	 * This method must be called before attachAndStartListeners
	 * is called, or this method will have no effect until the
	 * attachAndStartListeners is called a second time.
	 *
	 * Preconditions: 	each string in captureTypes represents a valid widget string -
	 * 					"text", "button", "menu", "radio", "list", "combo", "checkbox",
	 * 					  "contextbox", "tabchange", or their plural variants.
	 * Postconditions: 	Capture for each of the widgets specified in capture types is turned on.
	 */
	public void turnOnCaptureOf(String... captureTypes)
	{
		for(String s : captureTypes)
			switch(s.toLowerCase()) {
				case "text"			: textCapture = true; break;
				case "button" 		:
				case "buttons" 		: buttonCapture = true; break;
				case "menu"			:
				case "menus"		: menuCapture = true; break;
				case "radio"		:
				case "radios" 		: radioCapture = true; break;
				case "list"			:
				case "lists"		: listCapture = true; break;
				case "combo" 		:
				case "combos" 		: comboCapture = true; break;
				case "checkbox"		:
				case "checkboxes" 	: checkboxCapture = true; break;
//				case "contextbox" 	:
//				case "contextboxes"	: contextboxCapture = true; break;
				case "tabchange"	:
				case "tabchanges"	: tabchangeCapture = true; break;
//				case "all"		: textCapture = buttonCapture = menuCapture = listCapture = comboCapture = true; break;
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
	 */
	public void turnOffCaptureOf(String... captureTypes)
	{
		for(String s : captureTypes)
			switch(s.toLowerCase()) {
				case "text"			: textCapture = false; break;
				case "button"		:
				case "buttons" 		: buttonCapture = false; break;
				case "menu"			:
				case "menus"		: menuCapture = false; break;
				case "radio" 		:
				case "radios" 		: radioCapture = false; break;
				case "list"			:
				case "lists"		: listCapture = false; break;
				case "combo"		:
				case "combos"		: comboCapture = false; break;
				case "checkbox"		:
				case "checkboxes"	: checkboxCapture = false; break;
//				case "contextbox"	:
//				case "contextboxes"	: contextboxCapture = false; break;
				case "tabchange" 	:
				case "tabchanges"	: tabchangeCapture = false; break;
//				case "all" 			: textCapture = buttonCapture = menuCapture = radioCapture = listCapture = comboCapture = true;
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

		if( captureIsAllowedOn(discoveredWindow) && lookupWindowIndex(discoveredWindow) == -1) {
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
		if(windowsInCapture.isEmpty()) {
			try {
				saver.shutMeDown();
			} catch(RemoteException re) {
				System.err.println("JavaCaptureMonitor: Capture ran out of windows, but monitor failed to shut down the app.");
			}
		}
	}

	public void windowClosing(WindowEvent we)
	{
		if(windowEventPrint)
			System.out.println(we);
		Window w = we.getWindow();

		int idx = lookupWindowIndex(w);
		if(idx != -1) {
			try {
				JavaRMIListener jl = (JavaRMIListener)javaListeners.get(idx);
				String windowName = jl.windowName;
				saver.flushTextItems();
				saver.flushListItems(jl.listSelection());
				String windowId = AccessibleRole.WINDOW.toDisplayString() +
						JavaTestInteractions.name_part_separator + windowName;
				saver.gotWindowCloseEvent(windowId, windowName);
				if(modals.contains(we.getWindow()))
					modals.remove(we.getWindow());
			}
			catch(RemoteException e) {
				System.err.println("EventFlowSlicer Capt Monitor: Window closing failed to send the event.");
			}
			endCaptureOn(w);
		}
		if(windowsInCapture.isEmpty()) {
			DebugMessage.CAPTURE_CLOSED_ALL_WINDOWS.print();
			taskList.interrupt();
		}
	}





	public class OldAlgoritihms
	{
		/**
//		 * This method selects children of the interact-able
//		 * referenced by root, and registers them properly
//		 * with the java listener if it can be registered. Following
//		 * a call to this method, all actions taken upon root,
//		 * and children of root that involve the following options
//		 * are recorded:
//		 * text editing, or button pressing.
//		 *
//		 * Preconditions: 	none
//		 * Postconditions: 	Based on which capture features are turned on immediately preceding a call to this method
//		 * 					1. the component root and all its children now send
//		 * 				   	  events to the JavaListener in javaListeners specified
//		 * 				   	  by listenerIndex
//		 * 					2. the component root and all its children are recorded
//		 * 					  in the local JavaTestInteractions object registered at index "listenerIndex" for this capture
//		 * 					  if forRegistration is true.
//		 */
//		private void traverseTreeNew(Component nextComponent, boolean forRegistration, int listenerIndex)
//		{
//			if(nextComponent == null || !(nextComponent instanceof Accessible))
//				return;
//
//			AccessibleRole componentRole = nextComponent.getAccessibleContext().getAccessibleRole();
//
//			if(forRegistration)  {
//				// for text elements
//				if(componentRole.equals(AccessibleRole.TEXT)) {
//					if(textCapture) {
//						boolean comboText = findRoleAbove(AccessibleRole.COMBO_BOX, nextComponent, 2);
//						if(comboCapture && comboText)
//							javaListeners.get(listenerIndex).registerComboBoxText(nextComponent);
//
//						else if(textCapture && !comboText) {
//							javaListeners.get(listenerIndex).registerText(nextComponent);
//							interactionsByWindow.get(listenerIndex).saveNewTextEventID(nextComponent);
//						}
//					}
//				}
//
//				// for button elements
//				else if(componentRole.equals(AccessibleRole.PUSH_BUTTON)) {
//					boolean comboButton = false;
//					comboButton = findRoleAbove(AccessibleRole.COMBO_BOX, nextComponent, 2);
//					if(comboCapture && comboButton)
//						javaListeners.get(listenerIndex).registerComboBoxButton(nextComponent);
//
//					else if(buttonCapture && !comboButton) {
//						javaListeners.get(listenerIndex).registerButton(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewPushButtonEventID(nextComponent);
//					}
//				}
//
//				// for page tabs
//				else if(componentRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
//					if(tabchangeCapture) {
//						javaListeners.get(listenerIndex).registerTabList(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewTabListEventID(nextComponent);
//					}
//				}
	//
//				// for menu items
//				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
//					if(menuCapture) {
//						javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
//					}
//				}
//				// for menus (don't attach a listener)
//				else if(componentRole.equals(AccessibleRole.MENU)) {
//					if(menuCapture)
//						interactionsByWindow.get(listenerIndex).saveNewMenuEventID(nextComponent);
//				}
//				// for radio buttons
//				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) {
//					if(nextComponent instanceof MenuElement) {
//						if(menuCapture) {
//							javaListeners.get(listenerIndex).registerMenuItem(nextComponent);
//							interactionsByWindow.get(listenerIndex).saveNewMenuItemEventID(nextComponent);
//						}
//					}
//					else if(radioCapture) {
//						javaListeners.get(listenerIndex).registerButton(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewRadioButtonEventID(nextComponent);
//					}
//				}
//				// for flat lists
//				else if(componentRole.equals(AccessibleRole.LIST)) {
//					javaListeners.get(listenerIndex).registerListSelector(nextComponent);
//					interactionsByWindow.get(listenerIndex).saveNewListEventID(nextComponent);
//				}
//				// for toggle buttons
//				else if(componentRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
//					if(buttonCapture) {
//						javaListeners.get(listenerIndex).registerToggleButton(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewToggleButtonEventID(nextComponent);
//					}
//				}
//				// for combo boxes
//				else if(componentRole.equals(AccessibleRole.COMBO_BOX)) {
//					if(comboCapture) {
//						javaListeners.get(listenerIndex).registerComboBoxComponent(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewComboBoxEventIDs(nextComponent);
//					}
//				}
//				// for check boxes
//				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) {
	//
//					if(nextComponent instanceof MenuElement) {
//						if(menuCapture) {
//							javaListeners.get(listenerIndex).registerButton(nextComponent);
//							interactionsByWindow.get(listenerIndex).saveNewCheckboxEventID(nextComponent);
//						}
//					}
//					else if(checkboxCapture) {
//						javaListeners.get(listenerIndex).registerButton(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewCheckboxEventID(nextComponent);
//					}
//				}
//				// for panels that you can click or type into
//				else if(componentRole.equals(AccessibleRole.PANEL)) {
//					if(buttonCapture && JFCXComponent.hasListeners(nextComponent, "button")) {
//						javaListeners.get(listenerIndex).registerButton(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewMouseInputPanelEventID(nextComponent);
//					}
//					if(textCapture && JFCXComponent.hasListeners(nextComponent, "textbox")) {
//						javaListeners.get(listenerIndex).registerText(nextComponent);
//						interactionsByWindow.get(listenerIndex).saveNewTypingPanelEventID(nextComponent);
//					}
//				}
//
////				else if(componentRole.equals(AccessibleRole.POPUP_MENU)) {
////					boolean menuAbove = findRoleAbove(AccessibleRole.MENU, nextComponent, 1);
////					// assumes we know where in the hierarchy the window will be.
////					// just use SwingUtilities.getWindowAbove, or detect if we're under a menu bar.
//////					boolean windowAbove = findRoleAbove(AccessibleRole.WINDOW, nextComponent, 3);
////					boolean barAbove = findRoleAbove(AccessibleRole.MENU_BAR, nextComponent);
////					if(!menuAbove && !barAbove)
////						if(contextboxCapture) {
////							int x = 1;
////						}
////				}
//			}
//			else {
//				if(componentRole.equals(AccessibleRole.TEXT)) {
//					if(textCapture)
//						javaListeners.get(listenerIndex).unRegisterText(nextComponent);
//					if(comboCapture)
//						javaListeners.get(listenerIndex).unregisterComboBoxText(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.PUSH_BUTTON)) {
//					if(buttonCapture)
//						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
//					if(comboCapture)
//						javaListeners.get(listenerIndex).unRegisterComboBoxButton(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
//					if(menuCapture)
//						javaListeners.get(listenerIndex).unRegisterMenuItem(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) {
//					if(radioCapture)
//						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.LIST)) {
//					if(listCapture)
//						javaListeners.get(listenerIndex).unRegisterListSelector(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
//					if(buttonCapture)
//						javaListeners.get(listenerIndex).unRegisterToggleButton(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.COMBO_BOX)) {
//					if(comboCapture)
//						javaListeners.get(listenerIndex).unRegisterComboBoxComponent(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
//					if(tabchangeCapture)
//						javaListeners.get(listenerIndex).unRegisterTabList(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) {
//					if(checkboxCapture)
//						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
//				}
//				else if(componentRole.equals(AccessibleRole.PANEL)) {
//					if(buttonCapture)
//						javaListeners.get(listenerIndex).unRegisterButton(nextComponent);
//				}
//			}
//			// if it's a menu element.
//			if(nextComponent instanceof MenuElement) {
//				for(MenuElement nextChild : ((MenuElement)nextComponent).getSubElements())
//					if(nextChild instanceof Component)
//						traverseTree((Component)nextChild, forRegistration, listenerIndex);
//			}
//
//			else if(componentRole.equals(AccessibleRole.COMBO_BOX))
//				for(Component nextChild: ((Container)nextComponent).getComponents())
//					traverseTree(nextChild, forRegistration, listenerIndex);
	//
//			// now process this root's children if root is a container, but not a menu element.
//			else if(nextComponent instanceof Container)
//				for(Component nextChild : ((Container)nextComponent).getComponents())
//					traverseTree(nextChild, forRegistration, listenerIndex);
//		}
	}
}
