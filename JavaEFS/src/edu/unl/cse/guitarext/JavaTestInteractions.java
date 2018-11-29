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
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCXWindow;
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
		windowDuplications,			// instances of duplications of windows.
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
		namelessWindows,
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
	 * Every menu uses this pipe separator, '|', to separate parts of its name description.
	 */
	public static final char menu_separator = '|';
	/**
	 * Every name uses this underscore character, '_', to separate the name type from the name description.
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
		namelessWindows = namelessPushButtons = namelessTextFields = namelessRadios =
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
		windowDuplications = new HashMap<>();
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
	 * It is a basic assumption that the best key that can be used to look up any interaction
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
			assignMenuNames(window, W_HOVER); // DO NOT LEAVE THIS OUT!
			assignNameByRole(window, W_HOVER);
			for(Component child : window.getComponents())
				addInteractionsFrom(child, W_HOVER);
		}
	}

	private void assignMenuNames(final Window nextComponent, int hoverCode)
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
			addInteractionsFromMenuElement(e, hoverCode);
	}

	private void addInteractionsFromMenuElement(final MenuElement nextElement, int hoverCode)
	{
		Component nextComponent = (Component)nextElement;
		if(nextComponent instanceof Accessible) {
			AccessibleRole componentRole = ((Accessible) nextComponent).getAccessibleContext().getAccessibleRole();
				// for menus (don't attach a listener)
				if(componentRole.equals(AccessibleRole.MENU))
					saveNewMenuEventIDs(nextComponent, hoverCode);
				// for menu items
				else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
					saveNewMenuItemEventIDs(nextComponent, hoverCode);
				}
				// for radio buttons
				else if(componentRole.equals(AccessibleRole.RADIO_BUTTON))
					saveNewMenuItemEventIDs(nextComponent, hoverCode);
				// for check boxes
				else if(componentRole.equals(AccessibleRole.CHECK_BOX))
					saveNewMenuItemEventIDs(nextComponent, hoverCode);
		}
		for(MenuElement nextChild : nextElement.getSubElements())
			addInteractionsFromMenuElement(nextChild, hoverCode);
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
	public String[] saveNewTextEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();

		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Text Field " + (++namelessTextFields) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.TEXT.toDisplayString() + "_" + componentName)) {
			if(!textDuplications.containsKey(componentName)) {
				textDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = textDuplications.get(componentName) + 1;
				textDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}

		// name is not empty and unique
		else
			stub = componentName;
		eventID =  AccessibleRole.TEXT.toDisplayString() + "_" + stub;
		// add this event to possible test interactions.
		String actionType = ActionClass.TEXT.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		ReplayInteraction textEntry = new ReplayInteraction(eventID, actionType, windowName, offX, offY);
		return getEventIDArray(AccessibleRole.TEXT, hActionType, hoverCode, stub, textEntry);
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
		if(componentName.isEmpty())
			stub = "[Nameless Toggle Button " + (++namelessToggleButtons) + "]";
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + componentName)) {
			if(!toggleButtonDuplications.containsKey(componentName)) {
				toggleButtonDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = toggleButtonDuplications.get(componentName) + 1;
				toggleButtonDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;

		eventID = AccessibleRole.TOGGLE_BUTTON.toDisplayString() + "_" + stub;

		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		ArrayList<String> eids = new ArrayList<String>();

		switch(hoverCode) {
			case W_HOVER: // do a hover and action
				testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, offX, offY));
				eids.add(eventID);
			case HOVER_ONLY: // fall through and do just hover
			{
				String hId = hoverEventID(stub, AccessibleRole.TOGGLE_BUTTON.toDisplayString());
				testActions.put(hId, new ReplayInteraction(hId, hActionType, windowName, offX, offY));
				eids.add(hId);
			}
	break;	case NO_HOVER: // don't do a hover. just do an action.
				testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, offX, offY));
				eids.add(eventID);
		}

		return eids.toArray(new String[0]);
	}



	/**
	 * Save a new window event ID to the ReplayableActions map.
	 * @return
	 */
	public String saveNewWindowEventID(Component component)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");
		String eventID;
		Window jWindow = (Window)component;
		String componentName = JFCXWindow.getGUITARTitle(jWindow);
		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Window " + (++namelessWindows) + "]";
		else if(testActions.containsKey(componentName)) {
			if(!windowDuplications.containsKey(componentName)) {
				windowDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else  {
				int duplicateNumber = windowDuplications.get(componentName) + 1;
				windowDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = AccessibleRole.WINDOW.toDisplayString() + "_" + stub;
		String actionType = ActionClass.WINDOW.actionName;
		String windowName = componentName;
		ReplayInteraction windowClose = new ReplayInteraction(eventID, actionType, windowName,
				0, 0);
		testActions.put(eventID, windowClose);
		return eventID;
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
	public String[] saveNewPushButtonEventIDs(Component component, int hoverCode)
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
		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Push Button " + (++namelessPushButtons) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" + componentName)) {
			if(!pushButtonDuplications.containsKey(componentName)) {
				pushButtonDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = pushButtonDuplications.get(componentName) + 1;
				pushButtonDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = AccessibleRole.PUSH_BUTTON.toDisplayString() + "_" + stub;

		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		ReplayInteraction buttonClick = new ReplayInteraction(eventID, actionType, windowName,
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component));

		return getEventIDArray(AccessibleRole.PUSH_BUTTON, hActionType, hoverCode, stub, buttonClick);
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
	public String[] saveNewMenuItemEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = climbMenuTreeForName(component);

		// name string does not exist.
		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);

		String stub;
		// name string is empty.
		if(componentName.isEmpty())
			stub = "[Nameless Menu Item " + (++namelessMenuItems) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.MENU_ITEM.toDisplayString() + "_" + componentName)) {
			if(!menuItemDuplications.containsKey(componentName)) {
				menuItemDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = menuItemDuplications.get(componentName) + 1;
				menuItemDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = AccessibleRole.MENU_ITEM.toDisplayString() + "_" + stub;
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();

		// add this event id to possible test interactions
		ReplayInteraction menuItemClick = new ReplayInteraction(eventID, actionType, windowName, 0, 0);
		return getEventIDArray(AccessibleRole.MENU_ITEM, hActionType, hoverCode, stub, menuItemClick);
	}

	/**
	 * Saves the name of a new menu event ID, and returns the string identifying the event
	 * that would trigger this menu.
	 * @param component
	 * @return
	 */
	public String[] saveNewMenuEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = climbMenuTreeForName(component);

		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);

		// name string does not exist.
		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Menu " + (++namelessMenus) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.MENU.toDisplayString() + "_" + componentName)) {
			if(!menuDuplications.containsKey(componentName)) {
				menuDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = menuDuplications.get(componentName) + 1;
				menuDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = AccessibleRole.MENU.toDisplayString() + "_" + stub;
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		// add this event to possible testInteractions
		ReplayInteraction menuClick = new ReplayInteraction(eventID, actionType, windowName, 0, 0);
		return getEventIDArray(AccessibleRole.MENU, hActionType, hoverCode, stub, menuClick);
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
	public String[] saveNewRadioButtonEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();

		// name string does not exist.
		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		String stub;
		// name string is empty.
		if(componentName.isEmpty())
			stub = "[Nameless Radio Button " + (++namelessRadios) + "]";
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + componentName)) {
			if(!radioButtonDuplications.containsKey(componentName)) {
				radioButtonDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = radioButtonDuplications.get(componentName) + 1;
				radioButtonDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = AccessibleRole.RADIO_BUTTON.toDisplayString() + "_" + stub;
		// add this event to possible test interactions

		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		ReplayInteraction radioButtonClick = new ReplayInteraction(eventID, actionType, windowName,
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component));

		return getEventIDArray(AccessibleRole.RADIO_BUTTON, hActionType, hoverCode, stub, radioButtonClick);
	}

	/**
	 * Saves the name of a new list event ID, and returns the name uniquely identifying the event
	 * that would trigger this list.
	 * @param component
	 * @return
	 */
	public String[] saveNewListEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();

		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		String stub;
		// name string does not exist.
		if(componentName.isEmpty())
			stub = "[Nameless List " + (++namelessLists) + "]";
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.LIST.toDisplayString() + "_" + componentName)) {
			if(!listDuplications.containsKey(componentName)) {
				listDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = listDuplications.get(componentName) + 1;
				listDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = AccessibleRole.LIST.toDisplayString() + "_" + stub;
		// add this event to possible test interactions
		String actionType = ActionClass.SELECTION.actionName;
		String hActionType = ActionClass.SELECTIVE_HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
//
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

		// add this event to possible test interactions

		ReplayInteraction listSelect = new ReplayInteraction(eventID, actionType, windowName,
				getGUITAROffsetXInWindow(parentPane), getGUITAROffsetYInWindow(parentPane));

		return getEventIDArray(AccessibleRole.LIST, hActionType, hoverCode, stub, listSelect);
	}

	public String[] saveNewComboBoxEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID1, eventID2;
		String componentName = component.getAccessibleContext().getAccessibleName();

		// add this event to possible test interactions
		if(componentName == null || componentName.isEmpty())
			componentName = getSimpleName(component);

		// component has no canonical name or has a simple class name name value of "null" (rare)
		if(componentName.isEmpty() || componentName.equals((String)null))
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);

		String stub;
		if(componentName.isEmpty()) {
			stub = "[Nameless Combo Box " + (++namelessCombos) + "]";
//			eventID1 = AccessibleRole.COMBO_BOX.toDisplayString() + " " + clickSurname + "_" +
//					"[Nameless Combo Box " + (++namelessCombos) + "]";
//			eventID2 = AccessibleRole.COMBO_BOX.toDisplayString() + " " + selectSurname + "_" +
//					"[Nameless Combo Box " + (++namelessCombos) + " ]";
		}
		// name is a duplicate of another name of the same type
		// combo box (type) _ (name)
		else {
			String componentNameBase = AccessibleRole.COMBO_BOX.toDisplayString() + " " + clickSurname + "_" + componentName;
			if(testActions.containsKey(componentNameBase)) {
				if(!comboDuplications.containsKey(componentName)) {
					comboDuplications.put(componentName, 1);
					stub = componentName + " dup#" + "1";
				}
				else {
					int duplicateNumber = comboDuplications.get(componentName) + 1;
					comboDuplications.put(componentName, duplicateNumber);
					stub = componentName + " dup#" + duplicateNumber;
				}
			}
			// no duplicates, first encounter with this name.
			else {
				stub = componentName;
			}
		}
		eventID1 = AccessibleRole.COMBO_BOX.toDisplayString() + " " + clickSurname + "_" + stub;
		eventID2 = AccessibleRole.COMBO_BOX.toDisplayString() + " " + selectSurname + "_" + stub;

		String actionType1 = ActionClass.ACTION.actionName;
		String hActionType1 = ActionClass.HOVER.actionName;
		String actionType2 = ActionClass.PARSELECT.actionName;
		String hActionType2 = ActionClass.SELECTIVE_HOVER.actionName;

		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		// add the action (base the x and y position off the scroll pane.
		ReplayInteraction comboClick = new ReplayInteraction(eventID1, actionType1, windowName,
				offX, offY);
		ReplayInteraction comboSelect = new ReplayInteraction(eventID2, actionType2, windowName,
				offX, offY);
		String[] ids1 = getEventIDArray(AccessibleRole.COMBO_BOX,
				hActionType1, hoverCode, stub, comboClick);
		String[] ids2 = getEventIDArray(AccessibleRole.COMBO_BOX,
				hActionType2, hoverCode, stub, comboSelect);
		return new String[]{ids1[0], ids1[1], ids2[0], ids2[1]};
	}

	public String[] saveNewCheckboxEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		// name string does not exist.
		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Checkbox " + (++namelessCheckboxes) + "]";
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.CHECK_BOX.toDisplayString() + "_" + componentName)) {
			if(!checkboxDuplications.containsKey(componentName)) {
				checkboxDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = checkboxDuplications.get(componentName) + 1;
				checkboxDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;

		eventID = AccessibleRole.CHECK_BOX.toDisplayString() + "_" + stub;
		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		ReplayInteraction interaction = new ReplayInteraction(eventID, actionType, windowName, offX, offY);

		return getEventIDArray(AccessibleRole.CHECK_BOX, hActionType, hoverCode, stub, interaction);
	}

	public String[] getEventIDArray(AccessibleRole role, String hActionType, int hoverCode, String eventIdStub, ReplayInteraction... mainInters)
	{
		ArrayList<String> eids = new ArrayList<String>();

		switch(hoverCode) {
		case W_HOVER: // do a hover and action
			for(ReplayInteraction inter : mainInters) {
				testActions.put(inter.getEventID(), inter);
				eids.add(inter.getEventID());
			}
		case HOVER_ONLY: // fall through and do just hover
		{
			String hId = hoverEventID(eventIdStub, role.toDisplayString());
			testActions.put(hId, new ReplayInteraction(hId, hActionType, mainInters[0].window, mainInters[0].componentX, mainInters[0].componentY));
			eids.add(hId);
		}
break;	case NO_HOVER: // don't do a hover. just do an action.
		{
			for(ReplayInteraction inter : mainInters) {
				testActions.put(inter.getEventID(), inter);
				eids.add(inter.getEventID());
			}
		}
		}
		return eids.toArray(new String[0]);
	}
	/**
	 * Saves a new ID for the tab list accessible in the UI represented by component.
	 * @param component
	 * @return
	 */
	public String[] saveNewTabListEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = climbTabListForName(component);
		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Pane Tab " + (++namelessPaneTabs) + "]";
		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(AccessibleRole.PAGE_TAB_LIST.toDisplayString() + "_" + componentName)) {
			if(!paneTabDuplications.containsKey(componentName)) {
				paneTabDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = paneTabDuplications.get(componentName) + 1;
				paneTabDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;

		eventID = AccessibleRole.PAGE_TAB_LIST.toDisplayString() + " " + selectSurname + "_" + stub;

		// add this event to possible test interactions.
		String actionType;

		actionType = ActionClass.PARSELECT.actionName; // we can select children form this tab
		String hActionType = ActionClass.SELECTIVE_HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();

		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		ArrayList<String> eids = new ArrayList<String>();
		switch(hoverCode) {
		case W_HOVER: // do a hover and action
			testActions.put(eventID, new ReplayInteraction(eventID, actionType, windowName, offX, offY));
			eids.add(eventID);
		case HOVER_ONLY: // fall through and do just hover
		{
			String hId = hoverEventID(stub, AccessibleRole.PAGE_TAB_LIST.toDisplayString());
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
	 * Constructs a new eventID for the clickable panel represented by component.
	 *
	 * Preconditions: 	component is not null
	 * Postconditions: 	A new componentID is returned that uniquely corresponds to component
	 * 					and the action that can be carried out on it.
	 */
	public String[] saveNewMouseInputPanelEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);

		String stub;
		if(componentName.isEmpty())
			stub = "[Nameless Mouse Input Panel " + (++namelessMousePanels) + "]";
		// name string is empty

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + componentName)) {
			if(!mousePanelDuplications.containsKey(componentName)) {
				mousePanelDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = mousePanelDuplications.get(componentName) + 1;
				mousePanelDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = mouseSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + stub;

		// add this event to possible test interactions
		String actionType = ActionClass.ACTION.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		int offX = getGUITAROffsetXInWindow(component);
		int offY = getGUITAROffsetYInWindow(component);
		ReplayInteraction panelClick = new ReplayInteraction(eventID, actionType, windowName, offX, offY);
		return getEventIDArray(AccessibleRole.PANEL, hActionType, hoverCode, stub, panelClick);
	}

	/**
	 * Typing panels register events when keystrokes are registered while the panel itself is in focus.
	 * @param component
	 * @return
	 */
	public String[] saveNewTypingPanelEventIDs(Component component, int hoverCode)
	{
		if(component == null)
			throw new NullPointerException("Null component was passed to JavaTestInteractions module");

		String eventID;
		String componentName = component.getAccessibleContext().getAccessibleName();
		if(componentName == null || componentName.isEmpty())
			componentName = JFCTTPGuitarState.getGUITARTitleValueOf(component);

		String stub;
		// name string is empty
		if(componentName.isEmpty())
			stub = "[Nameless Typing Panel " + (++namelessTypingPanels) + "]";

		// name is a duplicate of another name of the same type
		else if(testActions.containsKey(typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + componentName)) {
			if(!typingPanelDuplications.containsKey(componentName)) {
				typingPanelDuplications.put(componentName, 1);
				stub = componentName + " dup#" + "1";
			}
			else {
				int duplicateNumber = typingPanelDuplications.get(componentName) + 1;
				typingPanelDuplications.put(componentName, duplicateNumber);
				stub = componentName + " dup#" + duplicateNumber;
			}
		}
		// name is not empty and unique
		else
			stub = componentName;
		eventID = typingSurname + " " + AccessibleRole.PANEL.toDisplayString() + "_" + stub;
		// add this event to possible test interactions
		String actionType = ActionClass.TEXT.actionName;
		String hActionType = ActionClass.HOVER.actionName;
		String windowName = currentWindow.getAccessibleContext().getAccessibleName();
		ReplayInteraction textEntry = new ReplayInteraction(eventID, actionType, windowName,
				getGUITAROffsetXInWindow(component), getGUITAROffsetYInWindow(component));
		return getEventIDArray(AccessibleRole.TEXT, hActionType, hoverCode, stub, textEntry);

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
			addInteractionsFromMenuElement((MenuElement)nextComponent, hoverCode);
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
		if(componentRole.equals(AccessibleRole.TEXT)) {
			String[] eids = saveNewTextEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for button
		else if(componentRole.equals(AccessibleRole.PUSH_BUTTON)) {
			String[] eids = saveNewPushButtonEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for toggle buttons
		else if(componentRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
			String[] eids = saveNewToggleButtonEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for menus
		else if(componentRole.equals(AccessibleRole.MENU)) {
			String[] eids = saveNewMenuEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for menu items
		else if(componentRole.equals(AccessibleRole.MENU_ITEM)) {
			String[] eids = saveNewMenuItemEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for comboBoxes
		else if(componentRole.equals(AccessibleRole.COMBO_BOX)) {
			String[] eids = saveNewComboBoxEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				toReturn += eventId_separator + eids[i];
		}
		// for radio buttons
		else if(componentRole.equals(AccessibleRole.RADIO_BUTTON)) {
			String[] eids;
			if(component instanceof MenuElement) // in case of RadioButtonMenuItems
				eids = saveNewMenuItemEventIDs(component, hoverCode);
			else
				eids = saveNewRadioButtonEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for checkboxes
		else if(componentRole.equals(AccessibleRole.CHECK_BOX)) {
			String[] eids;
			if(component instanceof MenuElement) // in case of CheckboxMenuItems
				eids = saveNewMenuItemEventIDs(component, hoverCode);
			else
				eids = saveNewCheckboxEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}
		// for flat lists
		else if(componentRole.equals(AccessibleRole.LIST)) {
			String[] eids = saveNewListEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				toReturn += eventId_separator + eids[i];
		}
		// for page tab lists
		else if(componentRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
			String[] eids = saveNewTabListEventIDs(component, hoverCode);
			toReturn = eids[0];
			for(int i = 1; i < eids.length; i++)
				 toReturn += eventId_separator + eids[i];
		}

		// for panels for which clicking or typing generates some response.
		else if(componentRole.equals(AccessibleRole.PANEL)) {
			boolean buttonListeners = hasListeners(component, "button");
			boolean textListeners = hasListeners(component, "textbox");
			ArrayList<String> eidsA = new ArrayList<String>();
			if(buttonListeners) {
				for(String s : saveNewMouseInputPanelEventIDs(component, hoverCode))
					eidsA.add(s);
			}
			if(textListeners)
				for(String s : saveNewTypingPanelEventIDs(component, hoverCode))
					eidsA.add(s);

			if(!eidsA.isEmpty()) {
				String[] eids = eidsA.toArray(new String[0]);
				toReturn = eids[0];
				for(int i = 1; i < eids.length; i++)
					 toReturn += eventId_separator + eids[i];
			}
		}
		// for tables
		else if(componentRole.equals(AccessibleRole.TABLE))
			toReturn = saveNewTableEventID(component);
		else if(
			componentRole.equals(AccessibleRole.WINDOW)
		||	componentRole.equals(AccessibleRole.DIALOG)
		|| 	componentRole.equals(AccessibleRole.FRAME))
			toReturn = saveNewWindowEventID(component);
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


	public static boolean windowTitlesAreSame(Window w1, Window w2)
	{
		return windowTitlesAreSame(JFCXWindow.getGUITARTitle(w1), JFCXWindow.getGUITARTitle(w2));
	}
	public static boolean windowTitlesAreSame(GWindow w1, GWindow w2)
	{
		return windowTitlesAreSame(w1.getTitle(), w2.getTitle());
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

	/*
	 * This method can only be called when an event ID for a component has been saved.
	 */
	private String hoverEventID(String componentStub, String roleString)
	{
		return roleString + " " + hoverSurname + "_" + componentStub;
	}
	/**
	 * Returns the proper hover capture constant appropriate for the booleans
	 * that depict whether the normal function capture is on and the
	 * hoverCapture function for that function is turned on.
	 * <br><br>
	 * This method should not be used if both booleans are false (ergo, neither the normal
	 * or hover capture of the function of focus is turned on.
	 * <br><br>
	 * If both booleans are false, this method returns -1 to indicate an invalid configuration.
	 * @param normalFunctionCaptureOn
	 * @param hoverFunctionOn
	 * @return
	 */
	public static int hoverCaptureCode(boolean normalFunctionCaptureOn, boolean hoverFunctionOn)
	{
		if(normalFunctionCaptureOn && hoverFunctionOn)
			return W_HOVER;
		else if(normalFunctionCaptureOn)
			return NO_HOVER;
		else if(hoverFunctionOn)
			return HOVER_ONLY;

		return -1; // neither function is being captured. This is an error state. Don't capture anything
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
