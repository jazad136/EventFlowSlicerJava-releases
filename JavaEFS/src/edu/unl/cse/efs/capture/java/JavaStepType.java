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
package edu.unl.cse.efs.capture.java;

import java.util.ArrayList;

import javax.accessibility.AccessibleRole;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.unl.cse.efs.java.StepTypeHandler;

/**
 * Source for the JavaStepType class. 
 * The JavaStepType is a simple extension of StepTypeHandler that accomodates
 * the fact that java does not use ints for roleNames.
 * A string has been set aside for this purpose, so that
 * this value may be stored and retrieved. 
 * 
 * @author Jonathan Saddler
 */
public class JavaStepType extends StepTypeHandler 
{
	protected String componentRoleName;
	protected String parentRoleName;
	protected String componentID;
	
	public JavaStepType()
	{
		super();
		componentRoleName = "";
		parentRoleName = "";
		componentID = "";
	}
	
	public String getComponentID()
	{
		return componentID;
	}
	public void setComponentID(String newComponentID)
	{
		componentID = newComponentID;
	}
	
	public String getRoleName() {
		return componentRoleName;
	}
	public void setRoleName(String crn) {
		componentRoleName = crn;
	}
	public String getParentRoleName() {
		return parentRoleName;
	}
	
	public void setParentRoleName(String prn) {
		parentRoleName = prn;
	}
	
	private String listParameters()
	{
		String toReturn = "";
		
		for(int i = 0; i < parameters.size(); i++) {
			String fullParam = parameters.get(i);
			if(fullParam.contains(GUITARConstants.NAME_SEPARATOR)) {
				int sep = fullParam.indexOf(GUITARConstants.NAME_SEPARATOR);
				String command = fullParam.substring(0, sep);
				String end = fullParam.substring(sep + 1);
				if(command.equals("TextInsert")) fullParam = "inserted: " + end;
				else if(command.equals("Command")) {
					String commandStrings = end.replace(":", ", ");
					fullParam = "cmd: " + commandStrings;	
				}
				else if(command.equals("Click")) fullParam = "click: (x_y) = (" + end + ")"; 
			}
			toReturn += fullParam;
			if(i != parameters.size()-1)
				toReturn += "\n  ";
		}
		return toReturn;
	}
	
	
	public String toString()
	{
		String toReturn = "";
		
		// name and role of component. 
		toReturn += "\"" + this.componentID + "\" " + this.componentRoleName;
		
		// add role name and position
		if(!(componentRoleName.equals(AccessibleRole.MENU_ITEM.toDisplayString()) 
			|| componentRoleName.equals(AccessibleRole.MENU.toDisplayString()))) {
			toReturn += " at (" + this.getX() + "," + this.getY() + ")";
		}
		
		if(!parameters.isEmpty()) {
			if(componentRoleName.equals(AccessibleRole.TEXT.toDisplayString())
			|| componentRoleName.equals(AccessibleRole.PANEL.toDisplayString())) {
				toReturn += " " + listParameters();			
			}
		}
		
		if(componentRoleName.equals(AccessibleRole.COMBO_BOX.toDisplayString())) 
			if(action.equals(ActionClass.PARSELECT.actionName)) 
				toReturn += ", sel idx: " + listParameters();
		
		if(componentRoleName.equals(AccessibleRole.PAGE_TAB_LIST.toDisplayString()))
			toReturn += ", sel idx: " + listParameters();
		
		if(componentRoleName.equals(AccessibleRole.TABLE.toDisplayString())) 
			toReturn += ", sel itms: " + listParameters();
		
		return toReturn;
	}
	
	public void addParameter(String newParam)
	{
		if(parameters == null)
			parameters = new ArrayList<String>();
		parameters.add(newParam);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((componentID == null) ? 0 : componentID.hashCode());
		result = prime * result + ((componentRoleName == null) ? 0 : componentRoleName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaStepType other = (JavaStepType) obj;
		if (componentID == null) {
			if (other.componentID != null)
				return false;
		} else if (!componentID.equals(other.componentID))
			return false;
		if (componentRoleName == null) {
			if (other.componentRoleName != null)
				return false;
		} else if (!componentRoleName.equals(other.componentRoleName))
			return false;
		if(parameters == null) {
			if (other.parameters != null)
				return false;
		}
		else if(!parameters.equals(other.parameters))
			return false;
		return true;
	}
	
}

