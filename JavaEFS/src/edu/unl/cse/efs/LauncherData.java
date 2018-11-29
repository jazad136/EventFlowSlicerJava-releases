package edu.unl.cse.efs;

import java.util.HashMap;
import java.util.Map;

import edu.unl.cse.efs.replay.JFCReplayerConfigurationEFS;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;

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
	private String defaultRMIPort;
	private String sendtoRMIPort;
	private String sendbackRMIPort;
	public static enum GenType {WORSEC, RSECWO, ECRSWO, NOREDS, NOCHOICE};
	public static enum PersonaType{TYPE1, TYPE2, TYPE1_2};
	public static enum PersonaFacet{IP, RA};
	private Map<PersonaFacet, PersonaType> providedPersonas;
	private GenType generatorRunningType;
	private JFCRipperConfigurationEFS ripc;
	private JFCReplayerConfigurationEFS repc;

	public LauncherData(String defaultRMIPort)
	{
		launchSelectionArguments = "";
		sendtoRMIPort = this.defaultRMIPort = defaultRMIPort;
		sendbackRMIPort = "";
		generatorRunningType = GenType.NOCHOICE;
		providedPersonas = new HashMap<PersonaFacet, PersonaType>();
	}

	public LauncherData(LauncherData old)
	{
		this.launchSelectionArguments = old.launchSelectionArguments;
		this.sendtoRMIPort = old.sendtoRMIPort;
		this.sendbackRMIPort = old.sendbackRMIPort;
		this.generatorRunningType = old.generatorRunningType;
		this.providedPersonas = new HashMap<>(old.providedPersonas);
	}

	/**
	 * Resets variables that are not transient (kept around) between separate runs of EventFlowSlicer.
	 */
	public void resetNonTransientData()
	{
		launchSelectionArguments = "";
		sendtoRMIPort = defaultRMIPort;
		sendbackRMIPort = "";
		generatorRunningType = GenType.NOCHOICE;
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

	public void unsetRMI()
	{
		sendbackRMIPort = "";
		sendtoRMIPort = "";
	}
	public void setGeneratorRunningType(String runningType)
	{


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

	public void setRipperConfiguration(JFCRipperConfigurationEFS config) { this.ripc = config; }
	public void setReplayerConfiguration(JFCReplayerConfigurationEFS config) {this.repc = config; }
	public JFCRipperConfigurationEFS getRipperConfiguration() { return ripc; }
	public JFCReplayerConfigurationEFS getReplayerConfiguration() { return repc;}

	public Map<PersonaFacet, PersonaType> getChosenPersonaArguments()
	{
		return providedPersonas;
	}
	public void setPersonaFacetType(PersonaFacet facet, PersonaType type)
	{
		providedPersonas.put(facet, type);
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
