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

import javax.accessibility.AccessibleRole;

import org.netbeans.jemmy.drivers.OrderedListDriver;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.data.PropertyType;

import static javax.accessibility.AccessibleRole.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
/**
 * This model provides information for how EventID's in a GUI file shall be structured when
 * they are built.
 * @author jsaddle
 *
 */
public class GUIEventModel {

	public static final String noAction = "hasNoActionID";
	public static final String noText = "hasNoTextID";
	public static final String noSelect = "hasNoSelectID";
	public static final String noSelHover = "hasNoSelectHoverID";
	public static final String noParSelect = "hasNoParSelectID";
	public static final String noHover = "hasNoHoverID";
	public static final String noWindow = "hasNoWindowID";
	public static final String hasNoID = "noID";

	private static final HashMap<AccessibleRole, Integer> roleIdx =
		new HashMap<AccessibleRole, Integer>() {{
			put(PUSH_BUTTON, 0);
			put(TOGGLE_BUTTON,1);
			put(MENU,2);
			put(MENU_ITEM,3);
			put(TEXT,4);
			put(COMBO_BOX,5);
			put(LIST,6);
			put(PAGE_TAB_LIST,7);
			put(CHECK_BOX,8);
			put(RADIO_BUTTON,9);
			put(PANEL,10);
			put(WINDOW,11);
			put(DIALOG,12);
			put(FRAME,13);
		}};
	public static final HashMap<String, Integer> roleIdxS =
		new HashMap<String, Integer>() {{
			put(PUSH_BUTTON.toDisplayString(), 0);
			put(TOGGLE_BUTTON.toDisplayString(), 1);
			put(MENU.toDisplayString(), 2);
			put(MENU_ITEM.toDisplayString(), 3);
			put(TEXT.toDisplayString(), 4);
			put(COMBO_BOX.toDisplayString(), 5);
			put(LIST.toDisplayString(), 6);
			put(PAGE_TAB_LIST.toDisplayString(), 7);
			put(CHECK_BOX.toDisplayString(), 8);
			put(RADIO_BUTTON.toDisplayString(), 9);
			put(PANEL.toDisplayString(), 10);
			put(WINDOW.toDisplayString(), 11);
			put(DIALOG.toDisplayString(), 12);
			put(FRAME.toDisplayString(), 13);
	}};
	private static final ActionClass[] modelAction = new ActionClass[]{
		ActionClass.ACTION,
		ActionClass.HOVER,
		ActionClass.TEXT,
		ActionClass.SELECTION,
		ActionClass.PARSELECT,
		ActionClass.SELECTIVE_HOVER,
		ActionClass.WINDOW
	};
	/**
	 * Defines which actions from roleIdx are supported by each role.
	 * Roles are rows, actions are columns
	 */
	private static final int[][] model = new int[][]
	{
			// act	hov	txt	sel	psl	slh win
			{	0, 	1, 	-1, -1, -1, -1, -1},	//PB
			{	0,	1,	-1,	-1,	-1,	-1, -1}, 	//TB
			{	0,	1,	-1,	-1,	-1,	-1, -1},	//ME
			{	0,	1,	-1,	-1,	-1,	-1, -1},	//MI
			{	-1,	1,	0,	-1,	-1,	-1, -1},	//TF
			{	0,	1,	4,	-1,	 2,	3,  -1},	//CB
			{	-1,	-1,	-1,	0,	-1,	1,  -1},	//FL
			{	-1,	-1,	-1,	-1,	 0, 1,  -1},	//TA
			{	0, 	1, 	-1, -1, -1, -1, -1},	//CH
			{	0, 	1, 	-1, -1, -1, -1, -1},	//RB
			{	0,	1,	2,	-1,	-1,	-1, -1},	//PA
			{  -1, -1,  -1,	-1,	-1,	-1, 0 },	//WI
			{  -1, -1,  -1,	-1,	-1,	-1, 0 },	//DI
			{  -1, -1,  -1,	-1,	-1,	-1, 0 },	//FM
	};
	public static final ActionClass[][] modelActions = new ActionClass[][]{
		supportedActionsFor(PUSH_BUTTON),
		supportedActionsFor(TOGGLE_BUTTON),
		supportedActionsFor(MENU),
		supportedActionsFor(MENU_ITEM),
		supportedActionsFor(TEXT),
		supportedActionsFor(COMBO_BOX),
		supportedActionsFor(LIST),
		supportedActionsFor(PAGE_TAB_LIST),
		supportedActionsFor(CHECK_BOX),
		supportedActionsFor(RADIO_BUTTON),
		supportedActionsFor(PANEL),
		supportedActionsFor(WINDOW),
		supportedActionsFor(DIALOG),
		supportedActionsFor(FRAME)
	};

	/**
	 * Queries the table containing the actions that are supported by this application
	 * for the role specified, arranged in the order they shall appear in GUI files,
	 * Returns an array of action classes that are supported for the specified role.
	 * @param role
	 * @return
	 */
	public static ActionClass[] getSupportedActionsFor(AccessibleRole role)
	{
		if(codeFor(role) == -1)
			return new ActionClass[0];
		return modelActions[codeFor(role)];
	}
	public static ActionClass[] getSupportedActionsFor(String weakRole)
	{
		if(codeFor(weakRole) == -1)
			return new ActionClass[0];
		return modelActions[codeFor(weakRole)];
	}
	/*
	 * Used to create a table of supported actions, arranged in the order they shall appear
	 * in written GUI files,
	 */
	private static ActionClass[] supportedActionsFor(AccessibleRole role)
	{
		int modelIdx = codeFor(role);
		if(modelIdx == -1)
			return new ActionClass[0];

		TreeMap<Integer, ActionClass> returnMap = new TreeMap<>();
		// return map will help keep the actions in the proper order.
		// as elements are added to the return map, they are sorted
		// using the integers given in the model.
		// only the actions that belong in the model are returned
		for(int i = 0; i < model[modelIdx].length; i++)
			if(model[modelIdx][i] != -1)
				returnMap.put(model[modelIdx][i], modelAction[i]);

		return returnMap.values().toArray(new ActionClass[0]);
	}
	private static int codeFor(AccessibleRole role)
	{
		Integer toReturn = roleIdx.get(role);
		if(toReturn == null)
			return -1;
		return toReturn;
	}

	public static int codeFor(String roleString)
	{
		Integer toReturn = roleIdxS.get(roleString);
		if(toReturn == null)
			return -1;
		return toReturn;
	}


	private static int roleIndexOf(ActionClass a, ActionClass[] supportedActions)
	{
		for(int i = 0; i < supportedActions.length; i++)
			if(supportedActions[i] == a)
				return i;
		return -1;
	}
	/**
	 * Return the number of eventID's that can be assigned to the role specified.
	 * @param role
	 * @return
	 */
	public static int eventIDTotal(AccessibleRole role)
	{
		int modelIdx = codeFor(role);
		if(modelIdx == -1)
			return 0;
		int counter = 0;
		for(int i = 0; i < model[modelIdx].length; i++)
			if(model[modelIdx][i] != -1)
				counter++;
		return counter;
	}

	public static String[] defaultIDs(ActionClass[] supportedActions)
	{
		String[] defaults = new String[supportedActions.length];
		for(int i = 0; i < defaults.length; i++) {
			switch(supportedActions[i]) {
			case ACTION 		 : defaults[i] = noAction;
	break;	case TEXT 			 : defaults[i] = noText;
	break;	case SELECTION 		 : defaults[i] = noSelect;
	break;	case SELECTIVE_HOVER : defaults[i] = noSelHover;
	break; 	case PARSELECT 		 : defaults[i] = noParSelect;
	break;	case HOVER			 : defaults[i] = noHover;
	break;	case WINDOW			 : defaults[i] = noWindow;
	break;	default				 : defaults[i] = hasNoID; // something went wrong.
			}
		}
		return defaults;
	}
	public static String[] allDefaultIds()
	{
		return defaultIDs(ActionClass.values());
	}

	/**
	 * This method returns eventIds in a string array pertaining to the role specified in forRole,
	 * arranged in the order in which they must appear when written to a GUI file created
	 * after a rip operation. The event id string provided is parsed for one or more eventIDs,
	 * each separated in the string by the delimiter specified, and their contents
	 * are used to determine which spot within the array they should fill.
	 * Events that appear in this string that are required to appear in a GUI file are then
	 * returned in their proper order.
	 */
	public static String[] getOrderedIds(AccessibleRole forRole, String eventIDString, String delimiter)
	{
		if(forRole.equals(TOGGLE_BUTTON))
			return orderedNormalButtonIDs(forRole, eventIDString, delimiter);
		else if(forRole.equals(CHECK_BOX))
			return orderedNormalButtonIDs(forRole, eventIDString, delimiter);
		else if(forRole.equals(RADIO_BUTTON))
			return orderedNormalButtonIDs(forRole, eventIDString, delimiter);
		else if(forRole.equals(MENU))
			return orderedNormalButtonIDs(forRole, eventIDString, delimiter);
		else if(forRole.equals(MENU_ITEM))
			return orderedNormalButtonIDs(forRole, eventIDString, delimiter);
		else if(forRole.equals(PUSH_BUTTON))
			return orderedNormalButtonIDs(forRole, eventIDString, delimiter);
		else if(forRole.equals(PANEL))
			return orderedPanelIDs(eventIDString, delimiter);
		else if(forRole.equals(PAGE_TAB_LIST))
			return orderedPageTabIDs(eventIDString, delimiter);
		else if(forRole.equals(TEXT))
			return orderedTextIDs(eventIDString, delimiter);
		else if(forRole.equals(LIST)) {
			return orderedFlatListIDs(eventIDString, delimiter);
		}
		else if(
			forRole.equals(WINDOW)
		|| 	forRole.equals(FRAME)
		|| 	forRole.equals(DIALOG))
			return windowID(eventIDString);
		return new String[0];
	}


	public static String[] getOrderedIds(String weakRole, String eventIDString, String delimiter)
	{
		if(
		   weakRole.equals(TOGGLE_BUTTON.toDisplayString())
		|| weakRole.equals(CHECK_BOX.toDisplayString())
		|| weakRole.equals(RADIO_BUTTON.toDisplayString())
		|| weakRole.equals(MENU.toDisplayString())
		|| weakRole.equals(MENU_ITEM.toDisplayString())
		|| weakRole.equals(PUSH_BUTTON.toDisplayString()))
			return orderedNormalButtonIDs(weakRole, eventIDString, delimiter);
		else if(weakRole.equals(PANEL.toDisplayString()))
			return orderedPanelIDs(eventIDString, delimiter);
		else if(weakRole.equals(PAGE_TAB_LIST.toDisplayString()))
			return orderedPageTabIDs(eventIDString, delimiter);
		else if(weakRole.equals(TEXT.toDisplayString()))
			return orderedTextIDs(eventIDString, delimiter);
		else if(weakRole.equals(LIST.toDisplayString())) {
			return orderedFlatListIDs(eventIDString, delimiter);
		}
		else if(
			weakRole.equals(WINDOW)
		|| 	weakRole.equals(FRAME)
		|| 	weakRole.equals(DIALOG))
			return windowID(eventIDString);

		return new String[0];
	}
	public static String[] windowID(String eventIDString)
	{
		return new String[]{eventIDString};
	}
	/**
	 * Return an event ID array containing the event ID strings of eventIdString, separated into cells using
	 * the delimiter provided, ordered by how this GUIEventModel class specifies the proper ordering of
	 * the strings in the list. This method works specifically on button ID's, and has methods
	 * designed to extract information about the event ID related to that effort
	 */
	private static String[] orderedNormalButtonIDs(String weakButtonRole, String eventIdString, String delimiter)
	{
		ActionClass[] suppActions = getSupportedActionsFor(weakButtonRole);
		String[] toReturn = defaultIDs(suppActions);
		StringTokenizer st = new StringTokenizer(eventIdString, delimiter);
		while(st.hasMoreTokens()) {
			String nextName = st.nextToken();
			if(!nextName.equals(hasNoID)) {
				String beforeSep = nextName.substring(0, nextName.indexOf(JavaTestInteractions.name_part_separator));
				if(beforeSep.contains(JavaTestInteractions.hoverSurname)) // this is the hover event.
					toReturn[roleIndexOf(ActionClass.HOVER, suppActions)] = nextName;
				else // normal action.
					toReturn[roleIndexOf(ActionClass.ACTION, suppActions)] = nextName;
			}
		}
		return toReturn;
	}

	private static String[] orderedNormalButtonIDs(AccessibleRole buttonRole, String eventIdString, String delimiter)
	{
		ActionClass[] suppActions = getSupportedActionsFor(buttonRole);
		String[] toReturn = defaultIDs(suppActions);
		StringTokenizer st = new StringTokenizer(eventIdString, delimiter);
		while(st.hasMoreTokens()) {
			String nextName = st.nextToken();
			if(!nextName.equals(hasNoID)) {
				String beforeSep = nextName.substring(0, nextName.indexOf(JavaTestInteractions.name_part_separator));
				if(beforeSep.contains(JavaTestInteractions.hoverSurname)) // this is the hover event.
					toReturn[roleIndexOf(ActionClass.HOVER, suppActions)] = nextName;
				else // normal action.
					toReturn[roleIndexOf(ActionClass.ACTION, suppActions)] = nextName;
			}
		}
		return toReturn;
	}

	/**
	 * This method returns panel eventIDs parsed from eventIDString in the order they
	 * should appear when written to a GUI file created after a rip operation.
	 */
	private static String[] orderedPanelIDs(String eventIdString, String delimiter)
	{
		ActionClass[] suppActions = getSupportedActionsFor(PANEL);
		String[] toReturn = defaultIDs(suppActions);
		StringTokenizer st = new StringTokenizer(eventIdString, delimiter);
		while(st.hasMoreTokens()) {
			String nextName = st.nextToken();
			if(!nextName.equals(hasNoID)) {
				String beforeSep = nextName.substring(0, nextName.indexOf(JavaTestInteractions.name_part_separator));
				if(beforeSep.contains(JavaTestInteractions.mouseSurname))
					toReturn[roleIndexOf(ActionClass.ACTION, suppActions)] = nextName;
				else if(beforeSep.contains(JavaTestInteractions.hoverSurname))
					toReturn[roleIndexOf(ActionClass.HOVER, suppActions)] = nextName;
				else if(beforeSep.contains(JavaTestInteractions.typingSurname))
					toReturn[roleIndexOf(ActionClass.TEXT, suppActions)] = nextName;

			}
		}
		return toReturn;
	}

	private static String[] orderedPageTabIDs(String eventIdString, String delimiter)
	{
		ActionClass[] suppActions = getSupportedActionsFor(PAGE_TAB_LIST);
		String[] toReturn = defaultIDs(suppActions);
		StringTokenizer st = new StringTokenizer(eventIdString, delimiter);
		while(st.hasMoreTokens()) {
			String nextName = st.nextToken();
			if(!nextName.equals(hasNoID)) {
				String beforeSep = nextName.substring(0, nextName.indexOf(JavaTestInteractions.name_part_separator));
				if(beforeSep.contains(JavaTestInteractions.hoverSurname)) // this is the hover event.// normal action.
					// this is the hover event.
					toReturn[roleIndexOf(ActionClass.SELECTIVE_HOVER, suppActions)] = nextName;
				else
					// this is the selection event.
					toReturn[roleIndexOf(ActionClass.PARSELECT, suppActions)] = nextName;
			}
		}
		return toReturn;
	}
	private static String[] orderedTextIDs(String eventIDString, String delimiter)
	{
		ActionClass[] suppActions = getSupportedActionsFor(TEXT);
		String[] toReturn = defaultIDs(suppActions);
		StringTokenizer st = new StringTokenizer(eventIDString, delimiter);
		while(st.hasMoreTokens()) {
			String nextName = st.nextToken();
			if(!nextName.equals(hasNoID)) {
				String beforeSep = nextName.substring(0, nextName.indexOf(JavaTestInteractions.name_part_separator));
				if(beforeSep.contains(JavaTestInteractions.hoverSurname))
					toReturn[roleIndexOf(ActionClass.HOVER, suppActions)] = nextName;
				else
					toReturn[roleIndexOf(ActionClass.TEXT, suppActions)] = nextName;
			}
		}
		return toReturn;
	}

	private static String[] orderedFlatListIDs(String eventIDString, String delimiter)
	{
		ActionClass[] suppActions = getSupportedActionsFor(LIST);
		String[] toReturn = defaultIDs(suppActions);
		StringTokenizer st = new StringTokenizer(eventIDString, delimiter);
		while(st.hasMoreTokens()) {
			String nextName = st.nextToken();
			if(!nextName.equals(hasNoID)) {
				String beforeSep = nextName.substring(0, nextName.indexOf(JavaTestInteractions.name_part_separator));
				if(beforeSep.contains(JavaTestInteractions.hoverSurname))
					toReturn[roleIndexOf(ActionClass.SELECTIVE_HOVER, suppActions)] = nextName;
				else
					toReturn[roleIndexOf(ActionClass.SELECTION, suppActions)] = nextName;
			}
		}
		return toReturn;
	}
	/**
	 * Two property types have congruent ValueLists if they
	 * both have nonempty values lists, and if for all values in their lists that don't
	 * match any of the default ID's, if when the lists are compared as sets,
	 * one contains at least one element of the other
	 */
	public static boolean checkCongruentEventIDValueLists(PropertyType pType1, PropertyType pType2)
	{
		HashSet<String> p1Id, p2Id;
		if(pType1.getValue().isEmpty() || pType2.getValue().isEmpty())
			return false;
		List<String> filterValues = Arrays.asList(allDefaultIds());
		List<String> values1 = new ArrayList<String>(pType1.getValue());
		List<String> values2 = new ArrayList<String>(pType2.getValue());
		values1.removeAll(filterValues);
		values2.removeAll(filterValues);
		p1Id = new HashSet<String>(values1);
		p2Id = new HashSet<String>(values2);

		for(String p2 : p2Id)
			if(p1Id.contains(p2))
				return true;

//		if(p1Id.equals(p2Id))
//			return true;
		return false;
	}

//	public static boolean componentActionMatch(JFCXComponent component, ActionClass action)
//	{
//		String classVal = component.getClassVal();
//		ActionClass[] supported = getSupportedActionsFor(classVal);
//		for(ActionClass ac : supported)
//			if(action == ac)
//				return true;
//		return false;
//	}
}
