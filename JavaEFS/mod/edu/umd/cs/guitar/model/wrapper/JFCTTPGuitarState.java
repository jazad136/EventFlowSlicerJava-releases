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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.Accessible;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.data.PropertyType;

/**
 * Source for the JFCTTPGuitarState. This class contains methods that make
 * reading guitar-related variables from a simple AWT component possible.
 * Methods from this class allow the caller to retrieve the class, type, and title
 * of an AWT component, through the use of the public method provided to retrieve
 * all three states at once. 
 * 
 * Methods of this class make frequent use of procedures carried out in 
 * GUITAR's JFCXComponent class.
 * @author Jonathan Saddler (jsaddle) 
 *
 */

public class JFCTTPGuitarState {

	public JFCTTPGuitarState()
	{
		
	}
	
	/**
	 * Return the full guitar jfcxComponent state of this component.
	 * @param component
	 * @return
	 */
	public static List<PropertyType> getGUITARComponentState(Component component)
	{
		List<PropertyType> toReturn = null;
		boolean dontParse = 
				component == null ||
				component.getAccessibleContext() == null;
		if(dontParse)
			return toReturn;
		
		toReturn = new ArrayList<PropertyType>();
		// guitar ID value
		String componentTitle = getGUITARTitleValueOf(component);
		String componentClassRole = getGUITARClassValueOf(component);
		String componentType = getGUITARTypeValueOf(component, componentTitle);
		
		PropertyType titleProperty, classProperty, typeProperty;
		titleProperty = new PropertyType();
		titleProperty.setName(GUITARConstants.TITLE_TAG_NAME);
		titleProperty.getValue().add(componentTitle);
		classProperty = new PropertyType();
		classProperty.setName(GUITARConstants.CLASS_TAG_NAME);
		classProperty.getValue().add(componentClassRole);
		typeProperty = new PropertyType();
		typeProperty.setName(GUITARConstants.TYPE_TAG_NAME);
		typeProperty.getValue().add(componentType);
		
		toReturn.add(titleProperty);
		toReturn.add(classProperty);
		toReturn.add(typeProperty);
		return toReturn;
	}
	
	/**
	 * Return the "GUITAR ClassVal" value of this component. 
	 */
	private static String getGUITARClassValueOf(Component component)
	{
		return component.getAccessibleContext().
				getAccessibleRole().toDisplayString();
	}
	/**
	 * Return the "GUITAR TypeVal" value of this component.
	 */
	public static String getGUITARTypeValueOf(Component component, String titleValue)
	{
		// jsaddle: check the static list of terminals for valid terminals 
		// (this is how JFCXComponents does it. Who am I to mess with it.)
		List<AttributesTypeWrapper> termSig = JFCConstants.sTerminalWidgetSignature;
		for (AttributesTypeWrapper sign : termSig) {
			String terminalTitle = sign.getFirstValByName(JFCConstants.TITLE_TAG);

			if (terminalTitle == null)
				continue;

			if (terminalTitle.equalsIgnoreCase(titleValue))
				return GUITARConstants.TERMINAL;
		}
		// if we didn't that this item was a terminal value
		// it is a system interaction value.
		return GUITARConstants.SYSTEM_INTERACTION;
	}
	
	/**
	 * Calculate and return the GUITAR Title value of the list item provided
	 */
	public static String getListItemGUITARTitle(Accessible accessibleListItem)
	{	
		return accessibleListItem.getAccessibleContext().getAccessibleName();
	}
	
	/**
	 * Calculate and return the GUITAR Title value of component
	 */
	public static String getGUITARTitleValueOf(Component component)
	{
		return JFCXComponent.getTitle(component);
	}
}
