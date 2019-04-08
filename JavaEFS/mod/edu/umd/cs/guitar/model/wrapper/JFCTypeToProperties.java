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
package edu.umd.cs.guitar.model.wrapper;

import javax.accessibility.*;
import javax.swing.text.AttributeSet;


import static javax.accessibility.AccessibleState.*;
import static javax.accessibility.AccessibleRole.*;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.*;

import edu.umd.cs.guitar.model.data.*;

/**
 * This class is responsible for ripping relevant properties from Java Foundation Class
 * components. 
 * 
 * @author jsaddle
 *
 */
public class JFCTypeToProperties {
	
	private static ArrayList<AccessibleState> importantJavaStates = new ArrayList<>(Arrays.asList(
				CHECKED, 
				ENABLED,  
				EXPANDED,
				PRESSED,
//				SELECTED,
				SHOWING));
	
	private static ArrayList<AccessibleRole> importantTextComponents = new ArrayList<>(Arrays.asList(
			//DOCUMENT, not a java component, uno only
			PARAGRAPH,
			TEXT, 
			//END_NOTE, not a java component, uno only
			EDITBAR, // java only
			FOOTER,
			HEADER,
			//FOOTNOTE, not a java component, uno only
			PASSWORD_TEXT,
			LABEL));
			
	/**
	 * Constructor does nothing.
	 */
	public JFCTypeToProperties()
	{	
	}
	
	/**
	 * Get the selection state of this component. The selection state defines
	 * the selection made within a component if and only if one can be made and one has been made.
	 * 
	 * Returns empty selection information for menu bars, even if a selection can be made. 
	 * @return
	 */
	public static List<PropertyType> getSelectionState(Component component)
	{
		
		List<PropertyType> toReturn = new ArrayList<PropertyType>();
		LinkedList<Integer> selection;
		boolean hasNoSelectable = 
				component == null || 
				component.getAccessibleContext() == null ||
				component.getAccessibleContext().getAccessibleSelection() == null;
		if(hasNoSelectable) 
			return toReturn;
		
		AccessibleContext selectableContext = component.getAccessibleContext();
		AccessibleSelection mySelection = selectableContext.getAccessibleSelection();
		AccessibleRole role = selectableContext.getAccessibleRole();
		selection = new LinkedList<Integer>();
		if(role.equals(AccessibleRole.MENU_BAR))
			return toReturn; // don't collect selection information for menu bars. 
		
		if(role.equals(AccessibleRole.COMBO_BOX)) {
			for(int i = 0; i < mySelection.getAccessibleSelectionCount(); i++) {
				Accessible a = mySelection.getAccessibleSelection(i);
				if(a != null) {
					int index = a.getAccessibleContext().getAccessibleIndexInParent();
					selection.add(index);
				}
			}
		}
		
		else { 
			for(int i = 0; i < selectableContext.getAccessibleChildrenCount(); i++)
				if(mySelection.isAccessibleChildSelected(i))
					selection.add(i);
		}
		String selValue;
		if(selection.isEmpty()) 
			return toReturn;
		
		selValue = "" + selection.get(0);
		for(int i = 1; i < selection.size(); i++)
			selValue += ", " + selection.get(i);
		
		PropertyType selState = new PropertyType();
		selState.setName("Selection");
		selState.setValue(Arrays.asList(new String[]{selValue}));
		toReturn.add(selState);
		return toReturn;
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
	 * Get the text state of this component. The text state is one of the more
	 * complex states of any JFC Component object.
	 * Preconditions: 	(none)
	 * Postconditions: 	if component != null, the state of the component is returned
	 * 					as a list of propertyType objects that together describe
	 * 					the component's "text state".
	 * 					otherwise, an empty list of propertyType objects is returned. 
	 * @return
	 */
	public static List<PropertyType> getTextState(Component component)
	{
		List<PropertyType> toReturn = new ArrayList<PropertyType>();
		
		boolean dontParse = component == null 
				|| component.getAccessibleContext() == null 
				|| component.getAccessibleContext().getAccessibleRole() == null
				|| component.getAccessibleContext().getAccessibleText() == null;
		
		if(dontParse)
			return toReturn;
		
		AccessibleContext aContext = component.getAccessibleContext();
		AccessibleRole componentRole = aContext.getAccessibleRole();
		
		if(!importantTextComponents.contains(componentRole))
			return toReturn;
		
		AccessibleText componentText = aContext.getAccessibleText();
		
		AccessibleEditableText editText = aContext.getAccessibleEditableText();
		if(editText != null) {
			
			// 1. parse the caret
			
			AccessibleStateSet componentState = aContext.getAccessibleStateSet();
			
			// under what conditions should we parse caret position?
			boolean parseCaret = componentState != null // we have discernable states
					&& componentState.contains(FOCUSED) // the component is focused
					&& editText.getSelectedText() != null 
					&& editText.getSelectedText().isEmpty(); // the selection is empty
			if(parseCaret) {
				PropertyType caretProperty = new PropertyType();
				caretProperty.setName("Caret Position");
				caretProperty.getValue().add("" + componentText.getCaretPosition());
				toReturn.add(caretProperty);
			}
			
			int charCount = componentText.getCharCount();
			for(int i = 0; i < charCount; i++) {
				AttributeSet myAttributes = componentText.getCharacterAttribute(i);
				// character bounds
				Rectangle bounds = componentText.getCharacterBounds(i);
				if(bounds == null)
					continue;
				PropertyType characterBoundsProperty = new PropertyType();
				characterBoundsProperty.setName("Character Bounds");
				String boundsString = bounds.x + " " + bounds.y + " " + bounds.width + " " + bounds.height;
				characterBoundsProperty.getValue().add(boundsString);
				toReturn.add(characterBoundsProperty);
				
				// the character
				PropertyType characterProperty = new PropertyType();
				characterProperty.setName("Character");
				characterProperty.getValue().add(componentText.getAtIndex(AccessibleText.CHARACTER, i));
				toReturn.add(characterProperty);
				
				toReturn.addAll(JFCTextState.fontFamilyState(myAttributes));
				
				toReturn.addAll(JFCTextState.fontSizeState(myAttributes));
				
				toReturn.addAll(JFCTextState.italicUnderlineBoldState(myAttributes));
				
				toReturn.addAll(JFCTextState.alignmentState(myAttributes));
				
				toReturn.addAll(JFCTextState.foregroundColorState(myAttributes));
				
				toReturn.addAll(JFCTextState.backgroundColorState(myAttributes));
//				ffProperty.setName("Character State");
//				for(Enumeration<?> names = myAttributes.getAttributeNames(); names.hasMoreElements();) {
//					StyleConstants nextAttributesList = (StyleConstants)names.nextElement();
//					
//				}
					
					
					// in UNO:
//					String name = names.nextElement().toString();
//					if(importantCharacterProperties.contains(propertyName)) {
//						PropertyType characterStateProperty = new PropertyType();
//						characterStateProperty.setName("Character State");
//						characterStateProperty.getValue().add(myAttributes.get)
//					}
//					result.add(createSingleProperty("Character", Integer.toString(xAccessibleText.getCharacter(z))));
//					
//					for(int q=0; q<props.length; q++){
//						String propertyName = props[q].Name; 
//						if(allowedCharacterProperties.contains(propertyName)){
						//do not record any of the complex properties 
						//or Asian properties
//							result.add(createSingleProperty("Character State ", Integer.toString(props[q].State.getValue())));
//							result.add(createSingleProperty("Character Handle ", Integer.toString(props[q].Handle)));	
//						}
//					}
//				} catch (IndexOutOfBoundsException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//            }
//            //Extract the currently selected Text
//            String sel = xAccessibleText.getSelectedText(); 
//            
					
		}}
		return toReturn;
	}
	
	/**
	 * Get the value of this component and put it in a propertyType object
	 * Return a list containing that propertyType. 
	 * @return
	 */
	public static List<PropertyType> getValueState(Component component)
	{
		List<PropertyType> toReturn = new LinkedList<PropertyType>();
		if(component == null)
			return toReturn;
		else if(component.getAccessibleContext() == null)
			return toReturn;
		
		AccessibleContext aContext = component.getAccessibleContext();
		if(aContext.getAccessibleValue() == null)
			return toReturn;
		
		PropertyType valueProperty = new PropertyType();
		valueProperty.setName("Value");

		AccessibleValue componentValue = aContext.getAccessibleValue();
		if(componentValue.getCurrentAccessibleValue() != null) 
			valueProperty.getValue().add(componentValue.getCurrentAccessibleValue().toString());
		else // value of this "value" property of this component has not been set yet.
			valueProperty.getValue().add("null"); 
		toReturn.add(valueProperty);
		
		return toReturn;
	}
	
	
	public static List<PropertyType> getComponentStateSet(Component component)
	{
		List<PropertyType> toReturn = new LinkedList<PropertyType>();
		boolean dontParse = 
				component == null
				|| component.getAccessibleContext() == null
				|| component.getAccessibleContext().getAccessibleStateSet() == null;
		
		if(dontParse)
			return toReturn;
				
		PropertyType stateProperty = new PropertyType();
		
		AccessibleStateSet compStateSet = component.getAccessibleContext().getAccessibleStateSet();
		LinkedList<String> savedStates = new LinkedList<String>();
		for(AccessibleState compState : compStateSet.toArray()) 
			if(importantJavaStates.contains(compState)) {
				if(compState.equals(CHECKED)) {
					int x = 1;
				}
				// we have a match
				savedStates.add(compState.toDisplayString());
			}
		
		stateProperty.setName("State");
		stateProperty.setValue(savedStates);
		toReturn.add(stateProperty);
		return toReturn;
	}
}
