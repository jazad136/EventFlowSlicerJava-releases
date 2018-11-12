package edu.unl.cse.guitarext;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public interface CaptureHandler {
	public void saveButtonClick(AWTEvent event, String componentID, String windowName, String componentRoleName);
	public void saveKeyEntry(KeyEvent keyEvent, String componentID, String windowName, String componentRoleName);
	public void saveMenuItemSelection(String[][] componentNamesAndRoles, String windowName);
	public void saveComboSelect(String componentID, String windowName, List<Integer> selection);
	public void saveListItemSelection(String componentID, String windowName);
	public void flushTextEntryEvent();
	public void flushListItemSelectionEvent();
}
