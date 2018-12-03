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
package edu.unl.cse.jontools.widget;

import java.util.LinkedList;

import edu.umd.cs.guitar.model.data.Repeat;
import edu.umd.cs.guitar.model.data.Widget;

public class RepeatList extends HyperList<Widget>{
	LinkedList<String> minSettings;
	LinkedList<String> maxSettings;
	public RepeatList()
	{
		minSettings = new LinkedList<>();
		maxSettings = new LinkedList<>();
	}
	
	public boolean add(Widget element)
	{
		minSettings.add(Repeat.UNBOUNDED_SETTING);
		maxSettings.add(Repeat.UNBOUNDED_SETTING);
		return super.add(element);
	}
}
