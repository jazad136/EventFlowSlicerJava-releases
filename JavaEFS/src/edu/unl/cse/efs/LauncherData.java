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
package edu.unl.cse.efs;

/**
 * Source for the LauncherData class. The launcher data class simply holds information
 * extracted from arguments passed to applications relevant to application launchers.
 * Data stored here can be parsed by objects responsible for using the information. 
 * No validation occurs here.
 * @author jsaddle
 *
 */
public class LauncherData {
	public String launchSelectionArguments;
	private String sendtoRMIPort;
	private String sendbackRMIPort;
	public static enum GenType {WORSEC, RSECWO, ECRSWO, NOREDS, NOCHOICE};
	private GenType generatorRunningType;
	
	public LauncherData(String defaultToRMIPort)
	{
		launchSelectionArguments = "";
		sendtoRMIPort = defaultToRMIPort;
		sendbackRMIPort = "";
		generatorRunningType = GenType.NOCHOICE;
	}
	
	public LauncherData(LauncherData old)
	{
		this.launchSelectionArguments = old.launchSelectionArguments;
		this.sendtoRMIPort = old.sendtoRMIPort;
	}
	
	/** 
	 * Returns true if this launcher data specifies to send data to an RMI port. 
	 * @return
	 */
	public boolean sendsToRMI()
	{
		return !sendtoRMIPort.isEmpty();
	}
	/**
	 * Returns the RMI port this object specifies
	 * @return
	 */
	public String getSendtoRMIPort()
	{
		return sendtoRMIPort;
	}
	
	public boolean hasLaunchSelectionArguments()
	{
		return !launchSelectionArguments.isEmpty();
	}
	/**
	 * Set the RMI port in this object
	 * @param portString
	 */
	public void setSendtoRMIPort(String portString)
	{
		sendtoRMIPort = portString;
		sendbackRMIPort = "";
	}
	
	public boolean sendsBackRMI()
	{
		return !sendbackRMIPort.isEmpty();
	}
	public String getSendbackRMIPort()
	{
		return sendbackRMIPort;
	}
	public void setSendbackRMIPort(String portString)
	{
		sendbackRMIPort = portString;
		sendtoRMIPort = "";
	}
	
	public void setGeneratorRunningType(GenType runningType)
	{
		this.generatorRunningType = runningType;
	}
	
	public GenType getGeneratorRunningType()
	{
		return generatorRunningType;
	}
	public void setLaunchSelectionArguments(String newSelection)
	{
		if(newSelection == null)
			newSelection = "";
		launchSelectionArguments = newSelection;
	}
	
	
	public String toString()
	{
		String launchArgs = hasLaunchSelectionArguments() ? 
				"select and launch " + launchSelectionArguments : "select and launch all test cases";
		String sendingBack = sendsBackRMI() ? "\n  and send back to port " + sendbackRMIPort : "";
		String sendingDown = sendsToRMI() ? "\n  and send down to port " + sendtoRMIPort : "";
		return launchArgs + sendingBack + sendingDown;
		
		
	}
}
