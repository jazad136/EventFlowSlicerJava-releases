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

import static java.awt.event.KeyEvent.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.umd.cs.guitar.model.wrapper.JFCTTPGuitarState;
import edu.unl.cse.efs.CaptureTestCase.CommandKey;
import edu.unl.cse.efs.capture.java.JavaStepType;

public class JavaCaptureUtils {
	
	public static TaskList getTranslatedTaskList(Collection<JavaStepType> testSteps)
	{
		ObjectFactory factory = new ObjectFactory();
		TaskList taskList = factory.createTaskList();
		for(JavaStepType oldType : testSteps)
			taskList.getWidget().add(widgetFromStepType(oldType));
		return taskList;
	}
	
	public static Widget widgetFromStepType(JavaStepType oldStepType)
	{
		ObjectFactory factory = new ObjectFactory();
		Widget newWidget = factory.createWidget();
		
		// need to set 
		// EventID
		newWidget.setEventID(oldStepType.getComponentID());
		// name
		String fName = oldStepType.getComponent();
		newWidget.setName(fName);
		// type 
		String fType = oldStepType.getRoleName();
		fType = fType.toUpperCase();
		newWidget.setType(oldStepType.getRoleName());
		// window 
		String fWindow = oldStepType.getWindow();
		newWidget.setWindow(fWindow);
		// action. 
		String fAction = oldStepType.getAction();
		fAction = JavaActionTypeProvider.getTypeFromActionHandler(fAction);
		newWidget.setAction(fAction);
		// parameter
		List<String> params = oldStepType.getParameters();
		String parameter;
		if(!params.isEmpty()) {
			parameter = params.get(0);
			for(int p = 1; p < params.size(); p++)
				parameter += GUITARConstants.NAME_SEPARATOR + params.get(p);
			newWidget.setParameter(parameter);
		}
		
		return newWidget;
	}
	
	/**
	 * Return a key reflecting the type of command that was sent by keyEvent
	 * Uses CommandKeys enumeration
	 * @param keyEvent
	 * @param windowName
	 * @return
	 */
	public static String getCommandKey(KeyEvent keyEvent, String windowName)
	{
		boolean keyFound = false;
		String commandName = "";
		CommandKey[] activeKeys = CommandKey.values();
		for(CommandKey ck : activeKeys) 
			if(ck.replicationVK == keyEvent.getKeyCode()) {
				commandName = ck.keyText;
				keyFound = true;
				break;
			}
		
		if(!keyFound) return "";
		if(keyEvent.isConsumed()) return "";
		return commandName;
	}
	
	public static boolean isUnrecognizedActionKey(int keyCode)
	{
		switch(keyCode) {
        case VK_KP_LEFT:
        case VK_KP_UP:
        case VK_KP_RIGHT:
        case VK_KP_DOWN:
		case VK_PRINTSCREEN:
        case VK_SCROLL_LOCK:
        case VK_CAPS_LOCK:
        case VK_NUM_LOCK:
        case VK_PAUSE:
        case VK_INSERT:

        case VK_FINAL:
        case VK_CONVERT:
        case VK_NONCONVERT:
        case VK_ACCEPT:
        case VK_MODECHANGE:
        case VK_KANA:
        case VK_KANJI:
        case VK_ALPHANUMERIC:
        case VK_KATAKANA:
        case VK_HIRAGANA:
        case VK_FULL_WIDTH:
        case VK_HALF_WIDTH:
        case VK_ROMAN_CHARACTERS:
        case VK_ALL_CANDIDATES:
        case VK_PREVIOUS_CANDIDATE:
        case VK_CODE_INPUT:
        case VK_JAPANESE_KATAKANA:
        case VK_JAPANESE_HIRAGANA:
        case VK_JAPANESE_ROMAN:
        case VK_KANA_LOCK:
        case VK_INPUT_METHOD_ON_OFF:

        case VK_AGAIN:
        case VK_UNDO:
        case VK_COPY:
        case VK_PASTE:
        case VK_CUT:
        case VK_FIND:
        case VK_PROPS:
        case VK_STOP:

        case VK_HELP:
        case VK_WINDOWS:
        case VK_CONTEXT_MENU:
        	return true;
		}
		return false;
	}
	public static String getCommandKey(int keyCode, boolean consumed, String windowName)
	{
		if(consumed)
			return "";
		CommandKey[] activeKeys = CommandKey.values();
		for(CommandKey ck : activeKeys) 
			if(ck.replicationVK == keyCode) 
				return ck.keyText;
		return "";
	}

	/**
	 * Detect if the keyEvent represents the use of a modifier key
	 */
	public static boolean isModifierOnly(KeyEvent ke)
	{
		switch(ke.getKeyCode()) {
		case VK_SHIFT 		:
		case VK_CONTROL 	:
		case VK_META		:
		case VK_ALT			: 
		case VK_UNDEFINED	: return true;
		}
		return false;
	}
	public static boolean isModifierOnly(int keyCode)
	{
		switch(keyCode) {
		case VK_SHIFT 		:
		case VK_CONTROL 	:
		case VK_META		:
		case VK_ALT			: 
		case VK_UNDEFINED	: return true;
		}
		return false;
	}
	/**
	 * Detects whether the given component is an accessible text editor component
	 */
	public static boolean isAccessibleTextEditor(Component component)
	{
		if(component == null)
			return false;
		
		if(component.getAccessibleContext() == null) 
			return false; 
		
		AccessibleRole role = component.getAccessibleContext().getAccessibleRole();
		return isAccessibleTextEditorRole(role.toDisplayString());
	}
	/**
	 * Does this component allow for the user to edit text? 
	 * @param component
	 * @return
	 */
	public static boolean isAccessibleTextEditorRole(String role)
	{
		return role.equals(AccessibleRole.TEXT.toDisplayString()) 
		|| role.equals(AccessibleRole.HTML_CONTAINER.toDisplayString()) 
		|| role.equals(AccessibleRole.PASSWORD_TEXT.toDisplayString())
		|| role.equals(AccessibleRole.EDITBAR.toDisplayString());
	}
	/**
	 * Method useful for getting the selected indices from the selection-supported component specified in the parameter
	 * Returns an empty list if the component currently has no active selection (including components that don't support selection). 
	 * 
	 * Preconditions: 	none 	
	 * Postconditions: 	If the component has a selection, and it is accessible, the indices of all child items	
	 * 					selected within component are returned in a list. 
	 * 					If the component doesn't support selection, or has no items selected,
	 * 					an empty list is returned. 
	 */
	public static List<Integer> getAccessibleSelectionFrom(Component component)
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

	/**
	 * Return a non-null name that will designate to this component a base
	 * name to use for capture. This method is useful for constructing event ID's and for getting a proper title
	 * for a component.
	 */
	public static String getCaptureComponentName(Component component)
	{
		String toReturn = null;
		toReturn = JFCTTPGuitarState.getGUITARTitleValueOf(component);
		if(toReturn == null || toReturn.isEmpty())
			toReturn = component.getAccessibleContext().getAccessibleName();
		if(toReturn == null || toReturn.isEmpty())
			toReturn = "";
		return toReturn;
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
	
	/**
	 * Finds the list object that is used by this combo box. 
	 * (The JList actually displays the items after the button is clicked.)
	 * 
	 * Preconditions: 	comboBox must be a combo box.
	 * Postconditions: 	The list contained within comboBox is returned. 
	 */
	public static AccessibleContext findComboList(Accessible comboBox)
	{
		if(comboBox == null)
			throw new IllegalArgumentException("Could not find combo list");
		
		for(int i = 0; i < comboBox.getAccessibleContext().getAccessibleChildrenCount(); i++) {
			Accessible c = comboBox.getAccessibleContext().getAccessibleChild(i);	
			// find the popup menu
			AccessibleRole role = c.getAccessibleContext().getAccessibleRole();
			if(role.equals(AccessibleRole.POPUP_MENU)) {
				AccessibleContext tContext;
				ArrayDeque<AccessibleContext> discovered = new ArrayDeque<AccessibleContext>();
				// find the accessibleSelection: do a BFS
				for(tContext = c.getAccessibleContext(); tContext != null; tContext = discovered.poll()) {
					if(tContext.getAccessibleSelection() != null)
						break;
					for(int j = 0; j < tContext.getAccessibleChildrenCount(); j++)
						discovered.offer(tContext.getAccessibleChild(i).getAccessibleContext());
				}
				if(tContext != null) 
					return tContext;
				ArrayDeque<Component> discovered2 = new ArrayDeque<Component>();
				Component next = null;
				if(c instanceof Container) {
					for(next = (Container)c; next != null; next = discovered2.poll()) {
						if(next.getAccessibleContext() != null 
						&& next.getAccessibleContext().getAccessibleSelection() != null) 
							break;
						
						if(next instanceof Container) 
							for(Component inC : ((Container)next).getComponents())  
								discovered2.offer(inC);
					}
				}
				if(next != null)
					return next.getAccessibleContext();
			}
		}
		throw new IllegalArgumentException("Could not find combo list");
	}
}
