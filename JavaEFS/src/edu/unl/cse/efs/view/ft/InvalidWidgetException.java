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
package edu.unl.cse.efs.view.ft;

public class InvalidWidgetException extends Exception 
{
	public enum Attribute{
		EVENT_ID("event ID"), NAME("name"), 
		TYPE("type"), WINDOW("window"), ACTION("action");
		
		public final String nameString;
		Attribute(String nameString)
		{
			this.nameString = nameString;
		}
	}
	public final Attribute cause;
	public InvalidWidgetException(Attribute att)
	{
		this.cause = att;
	}
}