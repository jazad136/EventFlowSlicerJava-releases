/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.unl.cse.guitarext;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Interface for handling capture
 * @author Jonathan A. Saddler
 *
 */
public interface CaptureHandler {
	public void saveButtonClick(AWTEvent event, String componentID, String windowName, String componentRoleName);
	public void saveKeyEntry(KeyEvent keyEvent, String componentID, String windowName, String componentRoleName);
	public void saveMenuItemSelection(String[][] componentNamesAndRoles, String windowName);
	public void saveComboSelect(String componentID, String windowName, List<Integer> selection);
	public void saveListItemSelection(String componentID, String windowName);
	public void flushTextEntryEvent();
	public void flushListItemSelectionEvent();
}
