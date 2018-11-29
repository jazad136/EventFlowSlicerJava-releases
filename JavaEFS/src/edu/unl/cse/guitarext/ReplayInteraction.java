package edu.unl.cse.guitarext;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;

/**
 * Source for the ReplayInteraction class. The ReplayInteraction is used to convey to the Replayer extra 
 * information relevant to the files read in useful modifying the AUT's Graphical User View in accordance with actions
 * that "interact" with it. Information listed below can be ascertained from AUT components. 
 * @author Jonathan Saddler, jsaddler
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
