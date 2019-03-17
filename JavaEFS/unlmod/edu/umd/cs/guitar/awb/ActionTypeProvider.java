package edu.umd.cs.guitar.awb;
import java.util.HashMap;


/** Provide a map between GUITAR action handlers and cogtool action types
*/

// Modified by Amanda Swearngin
//class NameProvider

public class ActionTypeProvider
{
    public static String getActionType (String actionHandler)
    {
        return maActionHandlerMap.get (actionHandler);
    }
 
    public static String getClickType (String actionHandler)
    {
        return maClickTypeMap.get (actionHandler);
    }

    public static String getKeyboardType (String actionHandler)
    {
        return maKeyboardTypeMap.get (actionHandler);
    }
    
	public static String getActionHandler(String action) 
	{
		return maGetActionHandlerMap.get (action);
	}
	
	public static String getTypeFromActionHandler(String action) 
	{
		return maGetTypeForActionHandlerMap.get (action);
	}


    private static HashMap<String, String> maActionHandlerMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maClickTypeMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maKeyboardTypeMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maGetActionHandlerMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maGetTypeForActionHandlerMap = new HashMap<String, String>(); 

    static {
    	
    	/*Action handler mappings*/
    	/*Keyboard*/
        maActionHandlerMap.put ("OOEditableTextSelectionHandler", "keyboardAction");
        maActionHandlerMap.put ("OOEditableTextHandler", "keyboardAction");
        maActionHandlerMap.put ("OOKeyboardHandler", "keyboardAction");
        maActionHandlerMap.put ("OOKeyboardShortcutHandler", "keyboardAction");
        maActionHandlerMap.put ("OOValueHandler", "keyboardAction");
        /*Mouse*/
        maActionHandlerMap.put ("OOActionHandlerMouseControl", "mouseAction");
        maActionHandlerMap.put ("OOActionHandler", "mouseAction");
        maActionHandlerMap.put ("OOSelectFromParentHandler", "mouseAction");
        maActionHandlerMap.put ("OOSelectionHandler", "mouseAction");
        
        
        /*Click type mappings (single, double, triple, downUp, etc.) , for now, we only support "downUp"*/
        maClickTypeMap.put ("OOActionHandlerMouseControl", "downUp");
        maClickTypeMap.put ("OOActionHandler", "downUp");
        maClickTypeMap.put ("OOSelectFromParentHandler", "downUp");
        maClickTypeMap.put ("OOSelectionHandler", "downUp");
        
        /*keyboard type mappings*/
        maKeyboardTypeMap.put ("OOEditableTextSelectionHandler", "text"); //changing the text of the object
        maKeyboardTypeMap.put ("OOEditableTextHandler", "text");
        maKeyboardTypeMap.put ("OOValueHandler", "value");
        maKeyboardTypeMap.put ("OOKeyboardShortcutHandler", "shortcut"); //using a keyboard shortcut with modifiers
        maKeyboardTypeMap.put ("OOKeyboardHandler", "keyboard"); //using a keyboard shortcut without modifiers

        /*Action handler mappings*/ 
        maGetActionHandlerMap.put("Click", "edu.umd.cs.guitar.event.OOActionHandler"); 
        maGetActionHandlerMap.put("Type", "edu.umd.cs.guitar.event.OOEditableTextSelectionHandler");
        maGetActionHandlerMap.put("Set Value", "edu.umd.cs.guitar.event.OOValueHandler"); 
        maGetActionHandlerMap.put("Select", "edu.umd.cs.guitar.event.OOSelectionHandler"); 
        maGetActionHandlerMap.put("Select From Parent", "edu.umd.cs.guitar.event.OOSelectFromParentHandler"); 

        maGetActionHandlerMap.put("Keyboard Shortcut", "edu.umd.cs.guitar.event.OOKeyboardShortcutHandler");
        maGetActionHandlerMap.put("Keyboard Access", "edu.umd.cs.guitar.event.OOKeyboardHandler");
        
        /*Action handler to type mappings*/ 
        maGetTypeForActionHandlerMap.put("OOActionHandler", "Click"); 
        maGetTypeForActionHandlerMap.put("OOEditableTextSelectionHandler", "Type");
        maGetTypeForActionHandlerMap.put("OOValueHandler", "Set Value"); 
        maGetTypeForActionHandlerMap.put("OOSelectionHandler", "Select"); 
        maGetTypeForActionHandlerMap.put("OOSelectFromParentHandler", "Select From Parent"); 

        maGetTypeForActionHandlerMap.put("OOKeyboardShortcutHandler", "Keyboard Shortcut");
        maGetTypeForActionHandlerMap.put("OOKeyboardHandler","Keyboard Access");

    }



}
