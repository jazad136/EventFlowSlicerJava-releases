package edu.unl.cse.guitarext;

/**
 * Source for the HeadTable class. The head table is an object that stores a JavaTestInteractions instance
 * that is always available from any class within the project. 
 * @author jsaddle
 *
 */
public class HeadTable {
	public static java.util.List<JavaTestInteractions> allInteractions;
	
	public static JavaTestInteractions getInteractionsForWindowName(String name) 
	{
		for(JavaTestInteractions jti : allInteractions) 
			for(java.awt.Window w : jti.getWindowsScanned()) {
				String windowName = w.getAccessibleContext().getAccessibleName();
//				if(windowName != null && windowName.equals(name)) 
				if(windowName != null && JavaTestInteractions.windowTitlesAreSame(windowName, name))
					return jti;
			}
		return new JavaTestInteractions();
	}
}
