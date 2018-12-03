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
package edu.unl.cse.guitarext;

import java.util.*;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.io.Serializable;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRole;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.wrapper.JFCTTPGuitarState;
import static edu.umd.cs.guitar.model.JFCXComponent.*;

/**
 * Source for the Java Test Interactions class. An instance of this class holds information about
 * interactions that may successfully be carried out on java AUT's. Typically, as in the application
 * CogToolHelper (Capture), one JavaTestInteractions instance is instantiated for every window available, 
 * although actions from multiple windows are allowed to be grouped together into one instance. It also provides
 * special methods to dispense unique ID's for components that need to be associated
 * with the same AUT, keep track of the names dispensed, and ensure uniqueness among newly dispensed names.
 * 
 * It is not assumed that each java component for one AUT has one unique name associated with it via its
 * in-memory "label" or "text" or "getAccessibleName" property. When two duplicate 'labels' are found in two widgets 
 * of the same type by an instance of this class, support has been added to this class to handle duplicate-name'd 
 * interactables within a single AUT. Even with these duplicates in key, the getNew(Component)EventID() methods will attempt to assign 
 * unique names to each component passed to some saveFoo() method. The object will keep correct records
 * provided that each component passed to a single saveFoo() method is unique.
 * 
 * @author Jonathan Saddler
 */
public class JavaTestInteractions implements Serializable, Iterable<ReplayInteraction>{
	
	
	private HashMap<String, ReplayInteraction> testActions;
	private LinkedList<Window> windows;
	private Window currentWindow;
	public static final String hasNoID = "noID";
	public static final String typingSurname = "typing";
	public static final String mouseSurname = "mouse";
	public static final String clickSurname = "click";
	public static final String selectSurname = "select";
	public static final String hoverSurname = "hover";
	
	public static final int NO_HOVER = 0;
	public static final int W_HOVER = 1;
	public static final int HOVER_ONLY = 2;
	
	private HashMap<String, Integer> 
		textDuplications, 			// instances of duplications of text fields. 
		radioButtonDuplications, 	// instances of duplications of radio buttons.
		pushButtonDuplications, 	// instances of duplications of push buttons.
		menuItemDuplications, 		// instances of duplications of menu items.
		menuDuplications, 			// instances of duplications of menus.
		listDuplications, 			// instances of duplications of list boxes.
		toggleButtonDuplications, 	// instances of duplications. 
		comboDuplications, 			// instances of duplications of combo boxes.
		checkboxDuplications, 		// instances of duplications of checkboxes.
		paneTabDuplications,		// instances of duplications of paneTabs.
		mousePanelDuplications,		// instances of duplications of mousePanels
		typingPanelDuplications, 
		tableDuplications;	// instances of duplications of typePanels
		
	private int 
		namelessTextFields, 
		namelessRadios, 
		namelessPushButtons, 
		namelessMenuItems, /* this should be impossible to increment and remain at 0.*/
		namelessMenus, /*this should be impossible to increment and remain at 0.*/
		namelessLists, 
		namelessToggleButtons,
		namelessCombos, 
		namelessCheckboxes,
		namelessPaneTabs, 
		namelessMousePanels,
		namelessTypingPanels,
		namelessTables;
	
	/**
	 * Every menu uses this separator to separate parts of its name description.
	 */
	public static final char menu_separator = '|';
	/**
	 * Every name uses this character to separate the name type from the name description.
	 */
	public static final char name_part_separator = '_';
	
	/**
	 * whenever we want to separate two parts of an event ID string, we use this special event_id separator "percent slash" 
	 */
	public static final String eventId_separator = "%\\";
	
	/**
	 * Whenever we want to separate two names given in the same string, we use this special part separator
	 */
	public static final String name_version_separator = ";";
	
	/**
	 * Constructor for the JavaTestInteractions class. The updating of all
	 * duplication structures must be done asynchronously. If there are duplicates between replay
	 * interactions, they are suppressed and not accounted for. 
	 * @param possibleInteractions
	 */
	public JavaTestInteractions(List<ReplayInteraction> possibleInteractions)
	{
		testActions = new HashMap<String, ReplayInteraction>();
		
		windows = new LinkedList<Window>();
		namelessPushButtons = namelessTextFields = namelessRadios = 
				namelessMenuItems = namelessMenus = namelessLists = 
				namelessToggleButtons = namelessCombos = namelessCheckboxes = 
				namelessMousePanels = namelessTypingPanels = namelessTables = 0;
		textDuplications = new HashMap<>();
		pushButtonDuplications = new HashMap<>();
		toggleButtonDuplications = new HashMap<>();
		menuItemDuplications = new HashMap<>();
		menuDuplications = new HashMap<>();
		radioButtonDuplications = new HashMap<>();
		listDuplications = new HashMap<>();
		comboDuplications = new HashMap<>();
		checkboxDuplications = new HashMap<>();
		paneTabDuplications = new HashMap<>();
		mousePanelDuplications = new HashMap<>();
		typingPanelDuplications = new HashMap<>();
		tableDuplications = new HashMap<>();
		for(ReplayInteraction interaction: possibleInteractions) 
			testActions.put(interaction.getEventID(), interaction);
	}
	
	/**
	 * Constructor for the JavaTestInteractions class that instantiates this class with
	 * an empty list of replayInteractions. 
	 */
	public JavaTestInteractions() 
	{
		this(new LinkedList<ReplayInteraction>());
	}

	/**
	 * This function looks up the ID of a list by looking for its scroll pane element. 
	 * 
	 * Once we find the scroll pane of the list component, we check its attributes against
	 * the stored interactions to see if we have a match 
	 * 
	 * For now, it is assumed that the best key that can be used to look up any list
	 * is via {{scrollpane coordinate}, {list action}, {list window}}.
	 * 
	 * Preconditions: 	component is a list element contained in a scroll pane. 
	 * Postconditions: 	If there is an available listID for component, it is returned. 
	 * 					Else, the string "noId" is returned
	 */
	public String lookupLargeObjectID(Component component, String windowName, String actionName)
	{
		Component parent = component.getParent();
		AccessibleContext parentContext = parent.getAccessibleContext();
		
		while(parentContext != null && !parentContext.getAccessibleRole().equals(AccessibleRole.SCROLL_PANE)) {
			parent = parent.getParent();
			parentContext = parent.getAccessibleContext();
		}
		
		int componentX = getGUITAROffsetXInWindow(parent);
		int componentY = getGUITAROffsetYInWindow(parent);
		
		for(ReplayInteraction interaction : testActions.values()) 
			if(windowName.equals(interaction.window) 
			&& componentX == interaction.componentX 
			&& componentY == interaction.componentY 
			&& actionName.equals(interaction.action)) {
				return interaction.getEventID();
			}
		
		return hasNoID;
	}
	
	
	/**
	 * Lookup an eventID previously saved to this JavaTestInteractions object by 
	 * component coordinates of the component specified, name of window, and action string.
	 * 
	 * It is assumed that the best key that can be used to look up any interaction
	 * is via {{component coordinate}, {component action}, {component window}}. I.e. 
	 * interactables are made unique by their coordinate position, window, and correlated
	 * clicking or typing action.
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	If a component was found matching the requirements specified 
 	 * 					that was previously saved to this JavaTestInteractions instance
 	 * 					its eventID is returned from this method. 
 	 * 					Else the string "noID" is returned from this method.
	 */
	public String lookupID(Component component, String windowName, String actionName)
	{	
		AccessibleContext context = component.getAccessibleContext();
		if(context == null)
			return hasNoID; // we don't store items that don't implement java accessibility 
		
		AccessibleRole compRole = context.getAccessibleRole();
		
		// JMenus
		// JMenuItems
		// JRadioButtonMenuItems go here
		if(component instanceof MenuElement) {
			String menuIDToMatch;
			if(compRole.equals(AccessibleRole.MENU_ITEM) || compRole.equals(AccessibleRole.RADIO_BUTTON) ||
			   compRole.equals(AccessibleRole.CHECK_BOX)) 
				menuIDToMatch = AccessibleRole.MENU_ITEM.toDisplayString() + "_" + climbMenuTreeForName(component);
			else
				menuIDToMatch = AccessibleRole.MENU.toDisplayString() + "_" + climbMenuTreeForName(component);
			
			for(ReplayInteraction interaction : testActions.values()) 
				if(windowName.equals(interaction.window) 
				&& actionName.equals(interaction.action)
				&& menuIDToMatch.equals(interaction.getEventID())) {
					return interaction.getEventID();
				}
			return hasNoID;
		}
		
		// all other components go here 
		int componentX = getGUITAROffsetXInWindow(component);
		int componentY = getGUITAROffsetYInWindow(component);
		
		// match window, x, y, and action.
	
		for(ReplayInteraction interaction : testActions.values()) 
			if(windowName.equals(interaction.window) 
			&& componentX == interaction.componentX 
			&& componentY == interaction.componentY 
			&& actionName.equals(interaction.action)) {
				return interaction.getEventID();
			}
		return hasNoID;
	}
	
	/**
	 * A special method to take a preexistent eventID, and return the name of the eventID
	 * that corresponds to the action name specified stored within this JTI instance.
	 * This method can be used if we don't know all specific names for a component's action, but we do know
	 * at least one of those specific names.
	 */
	public String matchActionWithID(String eventID, String actionName)
	{
		ReplayInteraction myInteraction = testActions.get(eventID);
		if(myInteraction == null) 
			return hasNoID; // this JTI has no record of this eventID
		else if(myInteraction.action.equals(actionName))
			return eventID; // this JTI has a record matching the specified actionName with the specified ID
		else 
			for(ReplayInteraction other : testActions.values()) 
				if(myInteraction.window.equals(other.window)
				&& myInteraction.componentX == other.componentX
				&& myInteraction.componentY == other.componentY)
					if(actionName.equals(other.action))
						return other.getEventID(); // this JTI has a record matching the specified actionName with another ID.
		
		return hasNoID;
	}
	
	/**
	 * Return a list of all windows scanned through the use of the scanWindowForInteractions method.
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	The list of windows scanned through the use of the scanWindowForInteractions method
	 * 					is returned. 
	 * @return
	 */
	public List<Window> getWindowsScanned()
	{
		return windows;
	}
	
	/**
	 * Set the current window that new eventID's will be added to when they are retrieved
	 * by methods of this class. 
	 * 
	 * Preconditions: 	window is not null
	 * Postconditions: 	new components to be added to this testInteractions module will be added while 
	 * 					referring to the window provided in window.
	 * @param window
	 */
	public void setCurrentWindow(Window window)
	{
		if(window == null)
			throw new NullPointerException("Null Window was passed to JavaTestInteractions module.");
		
		if(!windows.contains(window))
			windows.add(window);
		currentWindow = window;
	}
	/**
	 * Searches through this window for interactions inherently performable on
	 * components within the window, including those that can be performed
	 * on its children. This method is normally called when we just want to get a snapshot of
	 * interactions within the window, and not when we want to also capture them. 
	 * 
	 * Preconditions: 	window is not null
	 * Postconditions: 	The interactions performable on nextComponent are stored in testActions. 
	 * 					Any nameless components found during the process of finding interactions
	 * 					are aptly named, and the counters of this class are incremented
	 * 					to reflect how many nameless components were found. 
	 */
	public void scanWindowForInteractions(Window window) 
	{
		if(window == null) 
			throw new NullPointerException("Null Window was passed to JavaTestInteractions module.");
		
		Object awtTreeLock = window.getTreeLock();
		synchronized(awtTreeLock) {
			setCurrentWindow(window);
			assignMenuNames(window); // DO NOT LEAVE THIS OUT!
			for(Component child : window.getComponents())
				addInteractionsFrom(child, W_HOVER);
		}
	}
	
	private void assignMenuNames(final Window nextComponent)
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
			addInteractionsFromMenuElement(e);
	}
	
	private void addInteractionsFromMenuElement(final MenuElement nextElement)
	{
		Component nextComponent = (Component)nextElement;
		if(nextComponent instanceof Accessible) {
			AccessibleRole componentRole = ((Accessible) nextComponent).getAccessibleContext().getAccessibleRole();
				// for menus (don't attach a listener) 
				if(componentRole.equals(AccessibleRole.MENU)) 
					saveNewMenuEventID(nextComponent);
				// for menu items
				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) 
					saveNewMenuItemEventID(nextComponent);
				// for radio buttons
				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) 
					saveNewMenuItemEventID(nextComponent);
				// for check boxes
				else if(componentRole.equals(AccessibleRole.CHECK_BOX)) 
					saveNewMenuItemEventID(nextComponent);			
		}
		for(MenuElement nextChild : nextElement.getSubElements()) 
			addInteractionsFromMenuElement(nextChild);
	}
	/**
	 * Remove one element from testActions by eventID if it exists in the map, and returns true if 
	 * it was successfully removed. Returns false if no action was removed. 
	 * Preconditions: 	eventID is not null
	 * Postconditions:	testAction mapped to eventID was removed from testActions.
	 * 
	 */
	public boolean removeOldTestAction(String eventID)
	{
		if(testActions.containsKey(eventID)) {
			testActions.remove(eventID);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes any test action pertaining to the window specified from testActions. 
	 * 
	 * Preconditions: window is not null
	 * @param window
	 * @return
	 */
	public boolean deleteOldActionsOfWindow(Window window)
	{
		if(window == null)
			throw new NullPointerException("Null Window was passed to JavaTestInteractions module.");
		
		ListIterator<Window> windIt = windows.listIterator(); 
		
		boolean targetFound = false;
		// find the window in the list of windows captured. 
		String windowName;
		while(windIt.hasNext()) {
			windowName = windIt.next().getAccessibleContext().getAccessibleName();
			if(windowName.equals(window.getAccessibleContext().getAccessibleName())) {
				targetFound = true;
				break;
			}	
		}
		if(!targetFound)
			return false;
		
		windIt.remove();
		return true;
		
	}
	
	/**
	 * Returns the name of a new text event id based on the name information that can
	 * be gathered from the component specified by component. 
	 * 
	 * Postconditions: 	component is not null
	 * 					An appropriate name for component is returned. 
	 * 					If component is a duplicate of another text field with the same name
	 * 					already recorded in testActions, the count of text field duplicates 
	 * 					is incremented by one. 
	 * 					The new eventID for the text field is returned from the function
	 */
	public String saveNewTextEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		String eventID;  
		String componentName = component.getAccessibleContext().getAccessibleName();
		if(componentName == null || componentName.isEmpty())
			eventID =  AccessibleRole.TEXT.toDisplayString() + "_" + 
					"[Nameless Text Field " + (++namelessTextFields) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.TEXT.toDisplayString() + "_" + componentName)) {
			if(!textDuplications.containsKey(componentName)) {
				textDuplications.put(componentName, 1);
				eventID = AccessibleRole.TEXT.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = textDuplications.get(componentName) + 1;
				textDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.TEXT.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		
		// name is not empty and unique
		else 
			eventID = AccessibleRole.TEXT.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions. 
		String actionType = ActionClass.TEXT.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		testActions.put(eventID, new ReplayInteraction(eventID, 
				actionType, windowName, offX, offY));
		 
		// return the created eventID
		return eventID;	
	}
	
	/**
	 * Save a new ToggleButton EventID
	 * @param component
	 * @param hoverCode
	 * @return
	 */
	public String[] saveNewToggleButtonEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		if(componentName == null || componentName.isEmpty()) 
			componentName = stripButtonIconName(component);
		if(componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		String stub;
		// double check. 
		if(componentName.isEmpty()) {
			stub = "[Nameless Toggle Button " + (++namelessToggleButtons) + "]";
			eventID = AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + stub;
//			eventID = AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + 
//					"[Nameless Toggle Button " + (++namelessToggleButtons) + "]";
		}
		
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + componentName)) {
			if(!toggleButtonDuplications.containsKey(componentName)) {
				toggleButtonDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
				eventID = AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + stub;
			}
			else {
				int duplicateNumber = toggleButtonDuplications.get(componentName) + 1;
				toggleButtonDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
				eventID = AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + stub;
			}
		}
		// name is not empty and unique
		else {
			stub = componentName;
			eventID = AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + stub;
		}
		
		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		ArrayList<String> eids = new ArrayList<String>();
//		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
//				offX, offY));
		
		switch(hoverCode) {
			case W_HOVER: // do a hover and action
				testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, offX, offY));
				eids.add(eventID);
			case HOVER_ONLY: // fall through and do just hover 
			{
				String hId = hoverEventID(stub, AccessibleRole.TOGGLE_BUTTON.toDisplayString()); 
				testActions.put(hId, new ReplayInteraction(hId, hActionType, windowName, offX, offY));
				eids.add(hId);
			} break; 
			case NO_HOVER: // don't do a hover. just do an action.
				testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, offX, offY));
				eids.add(eventID);
		}
		
		return eids.toArray(new String[0]);
	}
	
	/**
	 * This method can only be called when an event ID for a component has been saved. 
	 * @param componentStub
	 * @param roleString
	 * @param componentX
	 * @param componentY
	 * @return
	 */
	private String hoverEventID(String componentStub, String roleString)
	{
		return roleString + " " + hoverSurname + "_" + componentStub;
	}
	
	/**
	 * Returns the name of a new push button event id based on the name information that can
	 * be gathered from the component specified by component. 
	 * 
	 * Postconditions: 	component is not null
	 * 					An appropriate name for component is returned. 
	 * 					If component is a duplicate of another push button with the same name
	 * 					already recorded in testActions, the count of push button duplicates 
	 * 					is incremented by one. 
	 */
	public String saveNewPushButtonEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		
		// name string does not exist. 
		if(componentName == null || componentName.isEmpty()) 
			componentName = stripButtonIconName(component);	
		
		// no name string and no icon name.
		if(componentName.isEmpty()) 
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		
		if(componentName.isEmpty()) {
			eventID = AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" + 
					"[Nameless Push Button " + (++namelessPushButtons) + "]";
		}
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" + componentName)) {
			if(!pushButtonDuplications.containsKey(componentName)) {
				pushButtonDuplications.put(componentName, 1);
				eventID = AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = pushButtonDuplications.get(componentName) + 1;
				pushButtonDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component)));
		
		return eventID;
	}
	
	
	/**
	 * Returns the name of a new menu item event id based on the name information that can
	 * be gathered from the component specified by component. 
	 * 
	 * Postconditions: 	component is not null.
	 * 					An appropriate name for component is returned. 
	 * 					If component is a duplicate of another menuItem with the same name
	 * 					already recorded in testActions, the count of menu item duplicates 
	 * 					is incremented by one. 
	 */
	public String saveNewMenuItemEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = climbMenuTreeForName(component);
		
		// name string does not exist. 
		if(componentName == null)
			eventID = AccessibleRole.MENU_ITEM.toDisplayString() + "_" + 
					"[Nameless Menu Item " + (++namelessMenuItems) + "]";
		
		// name string is empty.
		else if(componentName.isEmpty())
			eventID =  AccessibleRole.MENU_ITEM.toDisplayString() + "_" + 
					"[Nameless Menu Item " + (++namelessMenuItems) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.MENU_ITEM.toDisplayString() + "_" + componentName)) {
			if(!menuItemDuplications.containsKey(componentName)) {
				menuItemDuplications.put(componentName, 1);
				eventID = AccessibleRole.MENU_ITEM.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = menuItemDuplications.get(componentName) + 1;
				menuItemDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.MENU_ITEM.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.MENU_ITEM.toDisplayString() + "_" +
					componentName;
		
		String actionType = ActionClass.ACTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		
		// add this event id to possible test interactions
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 0, 0));
		return eventID;
	}
	
	/**
	 * Saves the name of a new menu event ID, and returns the string identifying the event
	 * that would trigger this menu. 
	 * @param component
	 * @return
	 */
	public String saveNewMenuEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = climbMenuTreeForName(component);
		
		// name string does not exist. 
		if(componentName == null)
			eventID = AccessibleRole.MENU.toDisplayString() + "_" + 
					"[Nameless Menu " + (++namelessMenus) + "]";
		
		// name string is empty.
		else if(componentName.isEmpty())
			eventID =  AccessibleRole.MENU.toDisplayString() + "_" + 
					"[Nameless Menu " + (++namelessMenus) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.MENU.toDisplayString() + "_" + componentName)) {
			if(!menuDuplications.containsKey(componentName)) {
				
				menuDuplications.put(componentName, 1);
				eventID = AccessibleRole.MENU.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = menuDuplications.get(componentName) + 1;
				menuDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.MENU.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.MENU.toDisplayString() + "_" +
					componentName;
		
		String actionType = ActionClass.ACTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		
		// add this event to possible testInteractions
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 0, 0));
		return eventID;
	}
	/**
	 * Returns the name of a new radio button event id based on the name information that can
	 * be gathered from the component specified by component. 
	 * 
	 * Postconditions: 	component is not null.
	 * 					An appropriate name for component is returned. 
	 * 					If component is a duplicate of another radio button with the same name
	 * 					already recorded in testActions, the count of radio button duplicates 
	 * 					is incremented by one. 
	 */
	public String saveNewRadioButtonEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		// name string does not exist. 
		if(componentName == null)
			eventID = AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + 
					"[Nameless Radio Button " + (++namelessRadios) + "]";
		
		// name string is empty.
		else if(componentName.isEmpty())
			eventID =  AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + 
					"[Nameless Radio Button " + (++namelessRadios) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + componentName)) {
			if(!radioButtonDuplications.containsKey(componentName)) {
				radioButtonDuplications.put(componentName, 1);
				eventID = AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = radioButtonDuplications.get(componentName) + 1;
				radioButtonDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component)));
		
		return eventID;
	}
	
	/**
	 * Saves the name of a new list event ID, and returns the name uniquely identifying the event
	 * that would trigger this list. 
	 * @param component
	 * @return
	 */
	public String saveNewListEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		// name string does not exist. 
		if(componentName == null)
			eventID = AccessibleRole.LIST.toDisplayString() + "_" + 
					"[Nameless List " + (++namelessLists) + "]";
		
		// name string is empty.
		else if(componentName.isEmpty())
			eventID =  AccessibleRole.LIST.toDisplayString() + "_" + 
					"[Nameless List " + (++namelessLists) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.LIST.toDisplayString() + "_" + componentName)) {
			if(!listDuplications.containsKey(componentName)) {
				listDuplications.put(componentName, 1);
				eventID = AccessibleRole.LIST.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = listDuplications.get(componentName) + 1;
				listDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.LIST.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.LIST_ITEM.toDisplayString() + "_" + componentName;
		
		// add this event to possible test interactions
		String actionType = ActionClass.SELECTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		
		// search for the scroll pane that contains this list. 
		Component parentPane;
		AccessibleContext parentContext;
		
		parentPane = component.getParent();
		parentContext = parentPane.getAccessibleContext();
		while(parentContext != null && !parentContext.getAccessibleRole().equals(AccessibleRole.SCROLL_PANE)) {
			parentPane = parentPane.getParent();
			if(parentPane == null) break;
			parentContext = parentPane.getAccessibleContext();
		}
		
		// add the action (base the x and y position off the scroll pane. 
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(parentPane), getGUITAROffsetYInWindow(parentPane)));
		return eventID;
	}

	public String[] saveNewComboBoxEventIDs(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID1, eventID2;
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		// add this event to possible test interactions
		if(componentName == null || componentName.isEmpty())  
			componentName = getSimpleName(component); 
		
		// component has no canonical name or has a simple class name name value of "null" (rare)
		if(componentName.isEmpty() || componentName.equals((String)null)) { 
			eventID1 = AccessibleRole.COMBO_BOX.toDisplayString() + " " + clickSurname + "_" + 
					"[Nameless Combo Box " + (++namelessCombos) + "]";
			eventID2 = AccessibleRole.COMBO_BOX.toDisplayString() + " " + selectSurname + "_" + 
					"[Nameless Combo Box " + (++namelessCombos) + " ]";
		}
		// name is a duplicate of another name of the same type
		// combo box (type) _ (name)
		else {
			String componentNameBase = AccessibleRole.COMBO_BOX.toDisplayString() + " click_" + componentName;
			if(testActions.containsKey(componentNameBase)) {
				if(!comboDuplications.containsKey(componentName)) {
					comboDuplications.put(componentName, 1);
					eventID1 = AccessibleRole.COMBO_BOX.toDisplayString() + 
							" click_" + componentName + " dup#" + 
							"1";
					eventID2 = AccessibleRole.COMBO_BOX.toDisplayString() + 
							" select_" + componentName + " dup#" + 
							"1";
				}
				else {
					int duplicateNumber = comboDuplications.get(componentName) + 1;
					comboDuplications.put(componentName, duplicateNumber);
					eventID1 = AccessibleRole.COMBO_BOX.toDisplayString() + 
							" click_" + componentName + " dup#" + 
							duplicateNumber;
					eventID2 = AccessibleRole.COMBO_BOX.toDisplayString() + 
							" select_" + componentName + " dup#" + 
							duplicateNumber;
				}
			}
			// no duplicates, first encounter with this name. 
			else {
				eventID1 = AccessibleRole.COMBO_BOX.toDisplayString() + " click_" + componentName;
				eventID2 = AccessibleRole.COMBO_BOX.toDisplayString() + " select_" + componentName;
			}
		}
	
		String actionType1 = ActionClass.ACTION.actionName;
		String actionType2 = ActionClass.PARSELECT.actionName;
		
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int theX = getGUITAROffsetXInWindow(component);
		int theY = getGUITAROffsetYInWindow(component);
		// add the action (base the x and y position off the scroll pane. 
		testActions.put(eventID1, new ReplayInteraction(eventID1, actionType1, windowName, 
				theX, theY));	
		testActions.put(eventID2, new ReplayInteraction(eventID2, actionType2, windowName,
				theX, theY));
		
		edu.umd.cs.guitar.model.data.AttributesType at = new edu.umd.cs.guitar.model.data.AttributesType();
		at.getProperty().addAll(JFCTTPGuitarState.getGUITARComponentState(component));
		
		return new String[]{eventID1, eventID2};
	}
	
	public String saveNewCheckboxEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		// name string does not exist. 
		if(componentName == null)
			eventID = AccessibleRole.CHECK_BOX.toDisplayString() + "_" + 
					"[Nameless Checkbox " + (++namelessCheckboxes) + "]";
		
		// name string is empty.
		else if(componentName.isEmpty())
			eventID =  AccessibleRole.CHECK_BOX.toDisplayString() + "_" + 
					"[Nameless Checkbox " + (++namelessCheckboxes) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.CHECK_BOX.toDisplayString() + "_" + componentName)) {
			if(!checkboxDuplications.containsKey(componentName)) {
				checkboxDuplications.put(componentName, 1);
				eventID = AccessibleRole.CHECK_BOX.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = checkboxDuplications.get(componentName) + 1;
				checkboxDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.CHECK_BOX.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.CHECK_BOX.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component)));
		
		return eventID;
	}
	
	
	/**
	 * Saves a new ID for the tab list accessible in the UI represented by component. 
	 * @param component
	 * @return
	 */
	public String saveNewTabListEventID(Component component)
	{
		if(component == null) 
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = climbTabListForName(component);
		
		if(componentName.isEmpty())
			eventID =  AccessibleRole.PAGE_TAB_LIST.toDisplayString() + "_" + 
					"[Nameless Pane Tab " + (++namelessPaneTabs) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.PAGE_TAB_LIST.toDisplayString() + "_" + componentName)) {
			if(!paneTabDuplications.containsKey(componentName)) {
				paneTabDuplications.put(componentName, 1);
				eventID = AccessibleRole.PAGE_TAB_LIST.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = paneTabDuplications.get(componentName) + 1;
				paneTabDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.PAGE_TAB_LIST.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.PAGE_TAB_LIST.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions.
		String actionType;
		
		if(JFCXComponent.hasChildren(component)) 
			actionType = ActionClass.PARSELECT.actionName; // we can select children form this tab
		else 
			actionType = ActionClass.ACTION.actionName; // this tab can only be clicked.
			
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int newX = getGUITAROffsetXInWindow(component);
		int newY = getGUITAROffsetYInWindow(component);
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				newX, newY));
		return eventID;
	}
	
	/**
	 * Constructs a new eventID for the clickable panel represented by component. 
	 * 
	 * Preconditions: 	component is not null
	 * Postconditions: 	A new componentID is returned that uniquely corresponds to component
	 * 					and the action that can be carried out on it.
	 */
	public String saveNewMouseInputPanelEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		if(componentName == null)
			eventID = mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
					"[Nameless Mouse Input Panel " + (++namelessMousePanels) + "]";
		// name string is empty
		else if(componentName.isEmpty())
			eventID =  mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
					"[Nameless Mouse Input Panel " + (++namelessMousePanels) + "]";
		
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + componentName)) {
			if(!mousePanelDuplications.containsKey(componentName)) {
				mousePanelDuplications.put(componentName, 1);
				eventID = mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = mousePanelDuplications.get(componentName) + 1;
				mousePanelDuplications.put(componentName, duplicateNumber);
				eventID = mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component)));
		
		return eventID;
	}
	
	/**
	 * Typing panels register events when keystrokes are registered while the panel itself is in focus. 
	 * @param component
	 * @return
	 */
	public String saveNewTypingPanelEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		
		if(componentName == null)
			eventID = typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
					"[Nameless Typing Panel " + (++namelessTypingPanels) + "]";
		// name string is empty
		else if(componentName.isEmpty())
			eventID = typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
					"[Nameless Typing Panel " + (++namelessTypingPanels) + "]";
		
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + componentName)) {
			if(!typingPanelDuplications.containsKey(componentName)) {
				typingPanelDuplications.put(componentName, 1);
				eventID = typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = typingPanelDuplications.get(componentName) + 1;
				typingPanelDuplications.put(componentName, duplicateNumber);
				eventID = typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" +
					componentName;
		
		// add this event to possible test interactions
		String actionType = ActionClass.TEXT.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component)));
		
		return eventID;
	}
	
	
	/**
	 * Saves a unique ID for some table in the current UI represented by component. 
	 * Tables themselves are uniquely named, but each cell within a table is a parameter indexing the table by column and row. 
	 * @param component
	 */
	public String saveNewTableEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		// add this event to possible test interactions
		if(componentName == null || componentName.isEmpty()) 
			componentName = getSimpleName(component);
		
		// component has no canonical name or has a simple class name name value of "null" (rare)
		if(componentName.isEmpty() || componentName.equals((String)null)) 
			eventID =  AccessibleRole.TABLE.toDisplayString() + "_" + 
					"[Nameless Table " + (++namelessTables) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.TABLE.toDisplayString() + "_" + componentName)) {
			if(!tableDuplications.containsKey(componentName)) {
				tableDuplications.put(componentName, 1);
				eventID = AccessibleRole.TABLE.toDisplayString() + "_" + 
						componentName + " dup#" + 
						"1";
			}
			else {
				int duplicateNumber = tableDuplications.get(componentName) + 1;
				tableDuplications.put(componentName, duplicateNumber);
				eventID = AccessibleRole.TABLE.toDisplayString() + "_" + 
						componentName + " dup#" + 
						duplicateNumber;
			}
		}
		// name is not empty and unique
		else 
			eventID = AccessibleRole.TABLE.toDisplayString() + "_" +
					componentName;
		
		Component parentPane;
		AccessibleContext parentContext;
		
		parentPane = component.getParent();
		parentContext = parentPane.getAccessibleContext();
		while(parentContext != null && !parentContext.getAccessibleRole().equals(AccessibleRole.SCROLL_PANE)) {
			parentPane = parentPane.getParent();
			if(parentPane == null) break;
			parentContext = parentPane.getAccessibleContext();
		}
		
		// types of actions that can be performed on this component (table).
		String actionType = ActionClass.PARSELECT.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
				
		// add this event to possible test interactions
		testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, 
				getGUITAROffsetXInWindow(parentPane), getGUITAROffsetYInWindow(parentPane)));
		
		return eventID;
	}
	
	/**
	 * A context box potential is a click event that causes a context box to show. 
	 * The potential can happen anywhere on the GUI, but most importantly is the fact
	 * @param component
	 * @return
	 */
	public String saveNewContextboxPotentialEventID(Component component)
	{
		return "";
	}
	
	
//	/**
//	 * A context box key potential is a keystroke event that causes a context box to show.
//	 * @param component
//	 * @return
//	 */
//	public String saveNewContextboxKeyPotentialEventID(Component component)
//	{
//		
//	}
	/**
	 * Searches this component and its children for interactions inherently performable on the 
	 * parameter specified, including those that can be performed on it or its 
	 * children, and stores these interactions in testActions. This is a recursive method. 
	 * Only components that implement the Accessible interface are tested for interactions. 
	 * Preconditions: 	currentWindow was set by a call to the setCurrentWindow() method of this object
	 * 					previous to calling this method. 
	 * Postconditions: 	The interactions performable on nextComponent are stored in testActions. 
	 * 					Any nameless components found during the process of finding interactions
	 * 					are aptly named, and the counters of this class are incremented
	 * 					to reflect how many nameless components were found.
	 */
	private void addInteractionsFrom(Component nextComponent, int hoverCode)
	{
		if(nextComponent instanceof Accessible && 
			 nextComponent.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.MENU_BAR))
				return; // do not parse menu bars in this method.
		
		assignNameByRole(nextComponent, hoverCode);
		 
		// NEXT STEP: process this menu's sub children if it's a menu element.
		if(nextComponent instanceof Accessible && nextComponent instanceof MenuElement) 
			addInteractionsFromMenuElement((MenuElement)nextComponent);
		// or if it's a combo box. 
		else if((nextComponent instanceof Accessible) && 
				nextComponent.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.COMBO_BOX)) 
			for(Component nextChild: ((Container)nextComponent).getComponents()) 
				addInteractionsFrom(nextChild, hoverCode);
		// otherwise process this root's children if root is just a container. 
		else if(nextComponent instanceof Container)
			for(Component nextChild : ((Container)nextComponent).getComponents()) 
				addInteractionsFrom(nextChild, hoverCode);
			
	}

	/**
	 * Calculate the unique name of the pageTabList by getting the accessible
	 * name of the first tab in the list.
	 * 
	 * Preconditions: 	component is a page tab list.
	 * 					tabList has children.
	 * Postconditions:	the name of this page tab list is returned.
	 * 					  the name returned is equal to either of two entities: 
	 * 					  a) the non-empty name of the first tab child of this tab list if one exists.
	 * 					  b) the empty string if the name of that child is null or empty, or if component has no
	 * 					  tab children.
	 * @param component
	 * @return
	 */
	private static String climbTabListForName(Component component)
	{
		AccessibleContext pContext = component.getAccessibleContext();
		
		if(pContext.getAccessibleChildrenCount() > 0) { 
			String toReturn = pContext.getAccessibleChild(0).getAccessibleContext().getAccessibleName();
			if(toReturn != null) {
				toReturn = toReturn.replace("_", "");
				return toReturn;
			}
		}
		return "";
	}
	
	/**
	 * Calculate the unique name of the menu element represented by component.
	 * Appends the name of this component to a string list containing all
	 * the parents leading to the menu at the top of this components menu tree. 
	 * @param component
	 * @return
	 */
	private static String climbMenuTreeForName(Component component)
	{
		String toReturn = component.getAccessibleContext().getAccessibleName();
		Component nextRoot = component.getParent();
		AccessibleRole nextRole;
		
		while(nextRoot!=null) {
			nextRole = nextRoot.getAccessibleContext().getAccessibleRole();
			
			// check the type of this component
			if(nextRoot instanceof JPopupMenu)	
				nextRoot = ((JPopupMenu)nextRoot).getInvoker(); // skip JPopupMenu container elements.
			
			else if(nextRole.equals(AccessibleRole.MENU_ITEM) 
					|| nextRole.equals(AccessibleRole.MENU)) {
				// Append the parent name to the back of the current name
				toReturn = nextRoot.getAccessibleContext().getAccessibleName() + menu_separator + toReturn;
				// update the root
				nextRoot = nextRoot.getParent();
			}
			else
				break; // if we find any menuElement that isn't a menu item or menu.
		}
		return toReturn;
	}

	private static String stripButtonIconName(Component toggleButton)
	{
		// get icon description
		String toReturn = "";
		if(toggleButton != null) {
			AccessibleIcon[] tIcons = toggleButton.getAccessibleContext().getAccessibleIcon();
			if(tIcons != null && tIcons.length > 0) 
				toReturn = tIcons[0].getAccessibleIconDescription();
		}
		
		if(toReturn == null || toReturn.isEmpty()) 
			return "";
		
		// remove extra information
		String[] nameSections = toReturn.split("\\/");
		toReturn = nameSections[nameSections.length-1];
		for(int dotPos = toReturn.lastIndexOf('.'); dotPos != -1; dotPos = toReturn.lastIndexOf('.')) 
			toReturn = toReturn.substring(0, dotPos);
		// make the name compatible
		toReturn = toReturn.replace(GUITARConstants.NAME_SEPARATOR, " ");
		
		// return "img " + name.
		return "img " + toReturn;
	}
	
	public boolean isEmpty()
	{
		return testActions.isEmpty();
	}

	/**
	 * Filter component into a category to help decide the kind of name should be used to identify it, 
	 * then calculate the name and return it if a name should be assigned to it. 
	 */
	public String assignNameByRole(Component component, int hoverCode)
	{
		if(component == null) return hasNoID;
		if(!(component instanceof Accessible)) return hasNoID;
		
		String toReturn = "";
		AccessibleRole componentRole = component.getAccessibleContext().getAccessibleRole();
		// for text fields
		if(componentRole.equals(AccessibleRole.TEXT)) 
			toReturn = saveNewTextEventID(component);
		// for button  
		else if(componentRole.equals(AccessibleRole.PUSH_BUTTON)) 
			toReturn = saveNewPushButtonEventID(component);
		// for toggle buttons
		else if(componentRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
			String[] toggleIDs = saveNewToggleButtonEventIDs(component, hoverCode);
			toReturn = toggleIDs[0] + eventId_separator + toggleIDs[1];
		}
		// for menus
		else if(componentRole.equals(AccessibleRole.MENU))
			toReturn = saveNewMenuEventID(component);
		
		// for menu items
		else if(componentRole.equals(AccessibleRole.MENU_ITEM)) 
			toReturn = saveNewMenuItemEventID(component);
		// for comboBoxes
		else if(componentRole.equals(AccessibleRole.COMBO_BOX)) {
			String[] comboBoxIds = saveNewComboBoxEventIDs(component);
			toReturn = comboBoxIds[0] + eventId_separator + comboBoxIds[1];
		}
		// for radio buttons
		else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) 
			if(component instanceof MenuElement) // in case of RadioButtonMenuItems
				toReturn = saveNewMenuItemEventID(component);
			else 
				toReturn = saveNewRadioButtonEventID(component);

		// for checkboxes
		else if(componentRole.equals(AccessibleRole.CHECK_BOX)) 
			if(component instanceof MenuElement) // in case of CheckboxMenuItems
				toReturn = saveNewMenuItemEventID(component);
			else
				toReturn = saveNewCheckboxEventID(component);
		
		// for flat lists
		else if(componentRole.equals(AccessibleRole.LIST)) 
			toReturn = saveNewListEventID(component); 		
		// for page tab lists
		else if(componentRole.equals(AccessibleRole.PAGE_TAB_LIST))
			toReturn = saveNewTabListEventID(component);
		// for panels for which clicking or typing generates some response. 
		else if(componentRole.equals(AccessibleRole.PANEL)) {
			boolean buttonListeners = hasListeners(component, "button"); 
			boolean textListeners = hasListeners(component, "textbox");
			if(buttonListeners && textListeners) 
				toReturn = saveNewMouseInputPanelEventID(component) + eventId_separator + saveNewTypingPanelEventID(component);
			else if(buttonListeners) 
				toReturn = saveNewMouseInputPanelEventID(component);
			else if(textListeners) 
				toReturn = saveNewTypingPanelEventID(component);
		}
		// for tables
		else if(componentRole.equals(AccessibleRole.TABLE)) 
			toReturn = saveNewTableEventID(component);
		
		if(!toReturn.isEmpty())
			return toReturn;
		else
			return hasNoID;
	}
	
	public boolean hasChildren(Component component) 
	{
		AccessibleContext xContext = component.getAccessibleContext();

		if (xContext == null)
			return false;

		// TODO: Check this
		int nChildren = xContext.getAccessibleChildrenCount();

		if (nChildren > 0)
			return true;

		if (component instanceof Container) {
			Container container = (Container) component;
			if (container.getComponentCount() > 0)
				return true;
		}
		return false;
	}
	@Override
	public Iterator<ReplayInteraction> iterator() {
		return testActions.values().iterator();
	}
	
	
	
	public static boolean windowTitlesAreSame(String origTitle, String otherTitle)
	{		
		boolean substrings, equals;
		boolean containsMod1, containsMod2;
		
		substrings = origTitle.contains(otherTitle) || otherTitle.contains(origTitle);
		equals = origTitle.equals(otherTitle);
		// return true  
		// if the titles are equal.
		// if the titles are substrings and one says modified and the other doesn't
		containsMod1 = (origTitle.contains("*") || origTitle.contains("modified"));
		containsMod2 = (otherTitle.contains("*") || otherTitle.contains("modified"));
		if(equals || (substrings && (containsMod1 ^ containsMod2))) {
			origTitle = origTitle.replace("*", "");
			origTitle = origTitle.replace("(modified)", "");
			origTitle = origTitle.replace("modified", ""); // remove window title annotations
			origTitle = origTitle.trim();
			otherTitle = otherTitle.replace("*", "");
			otherTitle = otherTitle.replace("(modified)", "");
			otherTitle = otherTitle.replace("modified", "");
			otherTitle = otherTitle.trim();
			if(origTitle.equals(otherTitle))
				return true;
		}

		
		return false;
	}
	
	
	public HashMap<String, List<ReplayInteraction>> binSortTestActions(String criterion)
	{
		
		HashMap<String, List<ReplayInteraction>> toReturn = new HashMap<String, List<ReplayInteraction>>();
		if(criterion.equals("action")) {
			for(ReplayInteraction ri : testActions.values()) {
				if(!toReturn.containsKey(ri.action)) {
					LinkedList<ReplayInteraction> newList = new LinkedList<ReplayInteraction>();
					newList.add(ri);
					toReturn.put(ri.action, newList);
				}
				else
					toReturn.get(ri.action).add(ri);
			}
		}
		else {
			for(ReplayInteraction ri : testActions.values()) {
				int firstUnder = ri.getEventID().indexOf('_');
				String type = ri.getEventID().substring(0, firstUnder);
				if(!toReturn.containsKey(type)) {
					LinkedList<ReplayInteraction> newList = new LinkedList<ReplayInteraction>();
					newList.add(ri);
					toReturn.put(type, newList);
				}
				else
					toReturn.get(type).add(ri);
			}
		}
		return toReturn;
	}
	public String toString()
	{
		String toReturn;
		toReturn = "Interactions Collected\n";
		HashMap<String, List<ReplayInteraction>> getSet = binSortTestActions("action");
		for(String s : getSet.keySet()) {
			List<ReplayInteraction> nextList = getSet.get(s);
			toReturn += s.toUpperCase() + "\n\n";
			for(ReplayInteraction ri : nextList) 
				toReturn += ri;
		}
		return toReturn;
	}
//	private static class Old
//	{
//		/**
//		 * Return a proper name for the window title provided
//		 * @param arg
//		 * @return
//		 */
//		public static String properWindowTitle(String title)
//		{
//			if(title == null)
//				return "";
//			String toReturn = title;
//			toReturn = toReturn.trim();
//			int lastChar = toReturn.length()-1;
//			
//			if(toReturn.charAt(lastChar) == '*') {
//				toReturn = toReturn.substring(0, lastChar);
//				toReturn = toReturn.trim();
//			}
//			return toReturn;
//		}
//	}
}
