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

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import javax.swing.SwingUtilities;

import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCXComponent;

/**
 * 
 * @author Jonathan A. Saddler
 *
 */
public class JFCSelectiveHoverHandler extends JFCEventHandler {

	public static final int miniEventWaitTime = 1000;
	public static final int defaultWaitSeconds = 3;

	public static final String Direct_Click_Command = "Click", 
		Wait_Command = "Wait", 
		Hover_Command = "Hover", 
		Text_Insert_Command= "Text_Insert";
	
	public JFCSelectiveHoverHandler()
	{
		
	}
	
	@Override
	protected void performImpl(GObject gComponent, Hashtable<String, List<String>> optionalData) {
		if(gComponent == null)
			return;
		Component component = getComponent(gComponent);
		Rectangle compR = component.getBounds();
		int offX = compR.width / 2;
		int offY = compR.height / 2;
		String ns = GUITARConstants.NAME_SEPARATOR;
		performImpl(gComponent, Arrays.asList(new String[]{
				Hover_Command + ns + offX + ns + offY, 
				Wait_Command + ns + defaultWaitSeconds}), null);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void performImpl(GObject gComponent, Object parameters, Hashtable<String, List<String>> optionalData) {
		if(gComponent == null)
			return;
		Component component = getComponent(gComponent);
		// parse all the parameters into a 2D array, each row is a command. 
		if (parameters instanceof List) {
			 // set the text to be edited.
			List<String> lParameter = (List<String>) parameters;
			final String[][] nParam = new String[lParameter.size()][];
			if(!lParameter.isEmpty()) {
				for(int i = 0; i < lParameter.size(); i++) {
					String param = lParameter.get(i);
					String[] pComponents = param.split(GUITARConstants.NAME_SEPARATOR);
					if(pComponents.length < 2) {
						System.err.println("Hover Step not supported: Invalid Parameter Set. Continuing.");
						return;
					}
					nParam[i] = pComponents;
				}
			}
			for(String[] set : nParam) {
				final String command = set[0];
				Rectangle compR = component.getBounds();
				int offX = compR.width / 2;
				int offY = compR.height / 2;
				
				if(command.equals(Hover_Command)) {
					try {
						if(set.length >= 3) {
							String offXString = set[1];
							String offYString = set[2];
							offX = Integer.parseInt(offXString);
							offY = Integer.parseInt(offYString);
						}
					
						// move the mouse.
						Point compL = component.getLocationOnScreen();
						
						final int hovX = compL.x + offX;
						final int hovY = compL.y + offY;
						if(hovX > compL.x + compR.width
						|| hovY > compL.y + compR.height) {
							System.err.println("Hover action may place mouse outside bounds of component.");
						}
						SwingUtilities.invokeLater(new Runnable(){public void run(){
							try {
								Robot robot = new Robot();
								robot.mouseMove(hovX, hovY);
							}
							catch(AWTException e) {
								System.err.println("Error: Hover action could not be performed: Java Robot could not be instantiated.");
							}
						}});
					} 
					catch(IllegalArgumentException e) {
						System.err.println("Error: Step passed to hover perform method is invalid.");
					}	
				}
				else if(command.equals(Wait_Command)) {
					int waitDuration = defaultWaitSeconds;
					
					if(set.length >= 2) {
						String waitString = set[1];
						waitDuration = Integer.parseInt(waitString);
					}
					try {
						Thread.sleep(waitDuration);	
					} catch(InterruptedException e) {
						System.err.println("Hover command was interrupted during execution.");
					}
				}
			}
		}
		
		

	}
	@Override
	public boolean isSupportedBy(GObject gComponent) 
	{
		// TODO Auto-generated method stub
		if (!(gComponent instanceof JFCXComponent))
			return false;
		JFCXComponent jComponent = (JFCXComponent) gComponent;
		Component component = jComponent.getComponent();
		String hoverSupport = hoverTypeAvailable(component);
		return hoverSupport.equals("parental");
	}

}
