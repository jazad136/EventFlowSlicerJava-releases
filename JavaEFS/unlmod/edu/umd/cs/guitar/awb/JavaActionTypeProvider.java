package edu.umd.cs.guitar.awb;

import java.util.HashMap;
import edu.umd.cs.guitar.event.ActionClass;


/** Provide a map between GUITAR action handlers and cogtool action types
*/

// Modified by Amanda Swearngin
//class NameProvider

public class JavaActionTypeProvider extends ActionTypeProvider
{
    public static String getActionType (String actionHandler) { return maActionHandlerMap.get (actionHandler);    }
 
    public static String getClickType (String actionHandler) { return maClickTypeMap.get (actionHandler); }

    public static String getKeyboardType (String actionHandler) { return maKeyboardTypeMap.get (actionHandler);}
    
	public static String getActionHandler(String action) { return maGetActionHandlerMap.get (action); }
	
	public static String getTypeFromActionHandler(String action) { return maGetTypeForActionHandlerMap.get (action); }

    private static HashMap<String, String> maActionHandlerMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maClickTypeMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maKeyboardTypeMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maGetActionHandlerMap = new HashMap<String, String>(); 
    private static HashMap<String, String> maGetTypeForActionHandlerMap = new HashMap<String, String>(); 
    
    
    static {
    	
    	/*Action handler mappings*/
    	/*Keyboard*/
        maActionHandlerMap.put ("JFCEditableTextHandler", 	"keyboardAction");
        maActionHandlerMap.put ("JFCEditableTextHandler", 	"keyboardAction");
        maActionHandlerMap.put ("JFCValueHandler", 			"keyboardAction");
        /*Mouse*/
        /*Java Mouse*/
        maActionHandlerMap.put("JFCActionHandler", 			"mouseAction");
        maActionHandlerMap.put("JFCActionHandler", 			"mouseAction");
        maActionHandlerMap.put("JFCSelectFromParent", 		"mouseAction");
        maActionHandlerMap.put("JFCSelectionHandler", 		"mouseAction");
        maActionHandlerMap.put("JFCBasicHoverHandler", 		"mouseAction");
        
        /*Click type mappings (single, double, triple, downUp, etc.) , for now, we only support "downUp"*/
        maClickTypeMap.put ("JFCActionHandler", 			"downUp");
        maClickTypeMap.put ("JFCSelectFromParent", 			"downUp");
        maClickTypeMap.put ("JFCSelectionHandler", 			"downUp");
        maClickTypeMap.put("JFCBasicHoverHandler",			"hover");
        
        /*keyboard type mappings*/
        maKeyboardTypeMap.put ("JFCEditableTextHandler", 	"text");
        maKeyboardTypeMap.put ("JFCValueHandler", 			"value");
        // no keyboard shortcut mapping
        // no keystroke press mapping
        // no text selection mapping
        
        
        /*Action handler mappings*/ 
        maGetActionHandlerMap.put("Click", 				ActionClass.ACTION.actionName); 
        maGetActionHandlerMap.put("Type", 				ActionClass.TEXT.actionName);
        maGetActionHandlerMap.put("Set Value", 			"edu.umd.cs.guitar.event.JFCValueHandler"); 
        maGetActionHandlerMap.put("Select", 			ActionClass.SELECTION.actionName);
        maGetActionHandlerMap.put("Select From Parent", ActionClass.PARSELECT.actionName);
        maGetActionHandlerMap.put("Hover", 				ActionClass.HOVER.actionName);
        maGetActionHandlerMap.put("Selective Hover", 	ActionClass.SELECTIVE_HOVER.actionName);
        maGetActionHandlerMap.put("Window", 			ActionClass.WINDOW.actionName);
        
        /*Action handler to type mappings*/ 
        maGetTypeForActionHandlerMap.put("JFCActionHandler", 					"Click"); 
        maGetTypeForActionHandlerMap.put(ActionClass.ACTION.actionName, 		"Click");
        maGetTypeForActionHandlerMap.put("JFCEditableTextHandler", 				"Type");
        maGetTypeForActionHandlerMap.put(ActionClass.TEXT.actionName, 			"Type");
        maGetTypeForActionHandlerMap.put("JFCValueHandler", 					"Set Value");
        maGetTypeForActionHandlerMap.put("JFCSelectionHandler", 				"Select"); 
        maGetTypeForActionHandlerMap.put(ActionClass.SELECTION.actionName, 		"Select");
        maGetTypeForActionHandlerMap.put("JFCSelectFromParent", 				"Select From Parent");
        maGetTypeForActionHandlerMap.put(ActionClass.PARSELECT.actionName, 		"Select From Parent");
        maGetTypeForActionHandlerMap.put("JFCBasicHoverHandler", 				"Hover");
        maGetTypeForActionHandlerMap.put(ActionClass.HOVER.actionName,          "Hover");
        maGetTypeForActionHandlerMap.put("JFCSelectiveHoverHandler",			"Selective Hover");
        maGetTypeForActionHandlerMap.put(ActionClass.SELECTIVE_HOVER.actionName,"Selective Hover");
        maGetTypeForActionHandlerMap.put("JFCWindowHandler", 					"Window Action");
        maGetTypeForActionHandlerMap.put(ActionClass.WINDOW.actionName, 		"Window Action");
    }
}
