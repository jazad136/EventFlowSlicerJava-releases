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
package edu.unl.cse.efs.commun.giveevents;

import java.awt.AWTEvent;
import java.rmi.Remote;
import java.util.List;

public interface NetCommunication extends Remote{
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
