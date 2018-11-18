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

package edu.umd.cs.guitar.event;

/**
 * Source for the enum ActionClass. For the purpose of simplifying the translation of events to the name of their 
 *  respective event handler classes and the path to the spot in the package structure
 *  where the action classes are found. Reflection is so common in guitar, this means of providing these 
 *  mappings from action to classpath for the many classes that use them is necessary!
 * @author Jonathan A. Saddler
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
