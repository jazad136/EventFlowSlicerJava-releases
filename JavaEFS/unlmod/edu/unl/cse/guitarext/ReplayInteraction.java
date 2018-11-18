/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.unl.cse.guitarext;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;

/**
 * Source for the ReplayInteraction class. The ReplayInteraction is used to convey to the Replayer extra 
 * information relevant to the files read in useful modifying the AUT's Graphical User View in accordance with actions
 * that "interact" with it. Information listed below can be ascertained from AUT components. 
 * @author Jonathan A. Saddler
 *
 */
public class ReplayInteraction {
	
	public final String action;
	public final String window;
	public final int componentX;
	public final int componentY;
	private String eventID;
	private String realEventID;
	
	/**
	 * Constructor for the Replay
	 * @param eventID
	 * @param action
	 * @param window
	 * @param componentX
	 * @param componentY
	 */
	public ReplayInteraction(String eventID, String action, String window, int componentX, int componentY)
	{
		
		
		this.eventID = eventID;
		String shortAction = "";
		if(action.contains(".") && !eventID.isEmpty()) {
			shortAction = JavaActionTypeProvider.getTypeFromActionHandler(action.substring(action.lastIndexOf('.')+1));
			this.realEventID = eventID + shortAction;
		}
		else 
			this.realEventID = "";
		this.action = action;
		this.window = window;
		this.componentX = componentX;
		this.componentY = componentY;
	}
	
	
	/**
	 * Return the eventID this action was registered with.
	 * @return
	 */
	public String getEventID()
	{
		return eventID;
	}
	
	public String getRealEventID()
	{
		return realEventID;
	}
	/**
	 * Return the string representing this interaction, the eventID.
	 */
	public String toString()
	{
		return "[" + eventID + "] in window [" + window + "] with action [" + action + "]\n";
	}
}
