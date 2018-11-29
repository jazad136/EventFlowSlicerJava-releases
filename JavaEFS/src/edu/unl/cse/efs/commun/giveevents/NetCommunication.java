package edu.unl.cse.efs.commun.giveevents;

import java.awt.AWTEvent;
import java.util.List;

public interface NetCommunication extends java.rmi.Remote {
	public void gotWindowCloseEvent(String componentID, String windowName) throws java.rmi.RemoteException;
	public void gotHoverEvent(String componentID, String componentRoleName, String windowName) throws java.rmi.RemoteException;
	public void gotKeyEvent(String[] keyData, char keyChar, String eventID, String windowName, String componentRoleName) throws java.rmi.RemoteException;
	public void gotEvent(AWTEvent nextEvent, String eventID, String windowName, String componentRoleName) throws java.rmi.RemoteException;
	public void gotListEvent(String eventID, String windowName) throws java.rmi.RemoteException;
	public void gotMenuItemEvent(String[][] components, String windowName) throws java.rmi.RemoteException;
	public void gotComboSelectEvent(String eventID, String windowName, List<Integer> selection) throws java.rmi.RemoteException;
	public void gotPageTabEvent(String eventID, String windowName, String tabData) throws java.rmi.RemoteException;
	public void flushTextItems() throws java.rmi.RemoteException;
	public void flushListItems(List<Integer> itemNumbers) throws java.rmi.RemoteException;
	public void shutMeDown() throws java.rmi.RemoteException;
}
