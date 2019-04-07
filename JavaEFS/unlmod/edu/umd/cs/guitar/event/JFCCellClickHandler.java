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
package edu.umd.cs.guitar.event;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JTree;
import javax.swing.text.Position;
import javax.swing.tree.TreePath;

import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.util.GUITARLog;

public class JFCCellClickHandler {

	public class Coordinate {

	}
	/**
	 * Checks and returns true if this handler can enact events on this object. 
	 * @param gComponent
	 * @return
	 */
	public boolean isSupportedBy(GObject gComponent) {

		if (!(gComponent instanceof JFCXComponent))
			return false;
		JFCXComponent jComponent = (JFCXComponent) gComponent;
		Component component = jComponent.getComponent();
		AccessibleContext aContext = component.getAccessibleContext();
		if (aContext == null)
			return false;

		Object event;

		// Text
		event = aContext.getAccessibleTable();
		return event != null;
	}
	
	protected void performImpl(GObject gComponent, Object parameters, Hashtable<String, List<String>> optionalData) 
	{
		if (gComponent == null) {
			System.err.println("JFCEditableTextHandler: PerformImpl was passed a null component.");
			return;
		}
		
		if (parameters instanceof List) {
			 // set the text to be edited.
			List<String> lParameter = (List<String>) parameters;
			String sInputText;
			final String[][] nParam = new String[lParameter.size()][];
			if(!lParameter.isEmpty())
				for(int i = 0; i < lParameter.size(); i++) {
//					String param = lParameter.get(i);
//					String[] pComponents = param.split(GUITARConstants.NAME_SEPARATOR);
//					if(pComponents.length < 2) {
//						System.err.println("Text Step not supported: Invalid Parameter Set. Continuing.");
//						return;
//					}
//					nParam[i] = pComponents;
					
					//TODO: PARSE THE PARAMETERS SOMEHOW.
				}
		}
	}
	protected void performImpl(GObject gComponent,
			Hashtable<String, List<String>> optionalData) {
		
//		if (!(gComponent instanceof JFCXComponent)){
//		
//			GUITARLog.log.debug("JFCXComponent! ");
//			return;
//		}
//
//		JFCXComponent jComponent = (JFCXComponent) gComponent;
//		Component component = jComponent.getComponent();
//
//		if (!(component instanceof JTree)){
//			GUITARLog.log.debug("NOT JTree! ");
//			return;
//		}
//
//		JTree tree = (JTree) component;
//
//		List<String> nodes = optionalData.get(JFCConstants.TITLE_TAG);
//
//		if (nodes == null){
//			GUITARLog.log.debug("No option! ");
//			return;
//		}
//		
//		if (nodes.size() < 1){
//			GUITARLog.log.debug("Selecting....");
//			return;	
//		}
//		
//		String node = nodes.get(0);
//		expandAll(tree, true);
////		new EventTool().waitNoEvent(1000);
//
//		TreePath path = tree.getNextMatch(node, 0, Position.Bias.Forward);
//		
//		
//		tree.setSelectionPath(path);

	}
}
