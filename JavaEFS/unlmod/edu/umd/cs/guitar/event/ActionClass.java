package edu.umd.cs.guitar.event;

/**
 * Source for the enum ActionClass. For the purpose of simplifying the translation of events to the name of their 
 *  respective event handler classes and the path to the spot in the package structure
 *  where the action classes are found. Reflection is so common in guitar, this means of providing these 
 *  mappings from action to classpath for the many classes that use them is necessary!
 * @author Jonathan Saddler
 *
 */
public enum ActionClass {
	
	SELECTION("edu.umd.cs.guitar.event.JFCSelectionHandler"),
	TEXT("edu.umd.cs.guitar.event.JFCEditableTextHandler"),
	ACTION("edu.umd.cs.guitar.event.JFCActionHandler"),
	PARSELECT("edu.umd.cs.guitar.event.JFCSelectFromParent"),
	HOVER("edu.umd.cs.guitar.event.JFCBasicHoverHandler"),
	SELECTIVE_HOVER("edu.umd.cs.guitar.event.JFCSelectiveHoverHandler"),
	WINDOW("edu.umd.cs.guitar.event.JFCWindowHandler");
	
	public final String actionName;
	public final String simpleName;
	private ActionClass(String pathToReflectedAction) 
	{
		this.actionName = pathToReflectedAction;
		this.simpleName = pathToReflectedAction.substring(
				pathToReflectedAction.lastIndexOf('.') + 1);
	}
}
