/*
 *  Copyright (c) 2009-@year@. The  GUITAR group  at the University of
 *  Maryland. Names of owners of this group may be obtained by sending
 *  an e-mail to atif@cs.umd.edu
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files
 *  (the "Software"), to deal in the Software without restriction,
 *  including without limitation  the rights to use, copy, modify, merge,
 *  publish,  distribute, sublicense, and/or sell copies of the Software,
 *  and to  permit persons  to whom  the Software  is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO  EVENT SHALL THE  AUTHORS OR COPYRIGHT  HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR  OTHER LIABILITY,  WHETHER IN AN  ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.umd.cs.guitar.model.wrapper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import edu.umd.cs.guitar.model.GHashcodeGenerator;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.ContentsType;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.exception.PropertyInconsistentException;

/**
 * Adapter class to process ComponentType.
 * 
 * jsaddler: This class is useful for defining components that we can't see 
 * on the interface, but which we still wish to associate with component properties.
 * (such as keyboard shortcuts) And generally useful for helping to rip information
 * from properties within attributes quickly
 * 
 * jsaddler: Note that this class doesn't necessarily need to utilize data from interface
 * components that are actually available on the screen. After analysis, I found
 * that this class and all its data can be constructed without ever ripping any 
 * interface, but instead through the aid of using a GUI xml file. 
 * 
 * <p>
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 * 
 */
public class ComponentTypeWrapper {

	ComponentType dComponentType;

	// -------------------------------
	// Parsed structured data
	// -------------------------------

	/**
	 * Container window
	 */
	GUITypeWrapper window;

	/**
	 * List of window invoked by the component
	 */
	List<GUITypeWrapper> invokedWindows;

	/**
	 * @return the invokedWindows
	 */
	public List<GUITypeWrapper> getInvokedWindows() {
		return invokedWindows;
	}

	/**
	 * Parent node
	 */
	ComponentTypeWrapper parent;

	/**
	 * Children nodes
	 */
	List<ComponentTypeWrapper> children;

	/**
	 * 
	 * A lazy method to parse data
	 * 
	 * <p>
	 * 
	 * @param dGUIStructure
	 * @param wGUIStructure
	 */
	public void parseData(GUIStructure dGUIStructure,
			GUIStructureWrapper wGUIStructure) {

		List<String> sInvokeList = getValueListByName(GUITARConstants.INVOKELIST_TAG_NAME);

		// Parse invoked windows
		if (sInvokeList != null) {

			this.invokedWindows = new ArrayList<GUITypeWrapper>();

			for (String invokedWinTitle : sInvokeList) {
				GUITypeWrapper invokedWin = wGUIStructure
						.getGUIByTitle(invokedWinTitle);
				if (invokedWin == null)
					continue;

				if (invokedWin.equals(this.getWindow().getTitle()))
					continue;

				if (invokedWin != null) {
					invokedWindows.add(invokedWin);
					invokedWin.setInvoker(this);

					// Use an invoker list instead of a single invoker
					invokedWin.getInvokerList().add(this);

					List<GUITypeWrapper> lGUIs = wGUIStructure.getGUIs();
					if (!lGUIs.contains(invokedWin)) {
						wGUIStructure.getGUIs().add(invokedWin);
						invokedWin.parseData(dGUIStructure, wGUIStructure);
					}
				}
			}
		}

		// Create a children list
		if (dComponentType instanceof ContainerType) {

			List<ComponentType> dChildrenList = ((ContainerType) dComponentType)
					.getContents().getWidgetOrContainer();
			if (dChildrenList != null) {
				this.children = new ArrayList<ComponentTypeWrapper>();

				for (ComponentType dChild : dChildrenList) {
					ComponentTypeWrapper wChild = new ComponentTypeWrapper(
							dChild);
					wChild.setParent(this);
					wChild.setWindow(this.window);
					wChild.parseData(dGUIStructure, wGUIStructure);
					children.add(wChild);
				}
			}
		}
	}

	/**
	 * Parse Data without invoke list
	 */
	public void parseData() {
		// Create a children list
		if (dComponentType instanceof ContainerType) {

			List<ComponentType> dChildrenList = ((ContainerType) dComponentType)
					.getContents().getWidgetOrContainer();
			if (dChildrenList != null) {
				this.children = new ArrayList<ComponentTypeWrapper>();

				for (ComponentType dChild : dChildrenList) {
					ComponentTypeWrapper wChild = new ComponentTypeWrapper(
							dChild);
					wChild.setParent(this);
					wChild.setWindow(this.window);
					wChild.parseData();
					children.add(wChild);
				}
			}
		}
	}

	/**
	 * @return the parent
	 */
	public ComponentTypeWrapper getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(ComponentTypeWrapper parent) {
		this.parent = parent;
	}

	/**
	 * @return the invokedWindowList
	 */
	public List<GUITypeWrapper> getInvokedWindowList() {
		return invokedWindows;
	}

	/**
	 * @return the children
	 */
	public List<ComponentTypeWrapper> getChildren() {
		return children;
	}

	/**
	 * @param window
	 *            the window to set
	 */
	public void setWindow(GUITypeWrapper window) {
		this.window = window;
	}

	/**
	 * @return the window
	 */
	public GUITypeWrapper getWindow() {
		return window;
	}

	static ObjectFactory factory = new ObjectFactory();

	/**
	 * @return the dComponentType
	 */
	public ComponentType getDComponentType() {
		return dComponentType;
	}

//	public ComponentTypeWrapper(List<AttributesType> attributes)
//	{
//		ComponentType type = new ComponentType();
//	}
	/**
	 * @param componentType
	 */
	public ComponentTypeWrapper(ComponentType componentType) {
		super();
		dComponentType = componentType;
	}

	public PropertyType getFirstPropertyByName(String sName) {
		if (dComponentType == null) {
			return null;
		}

		AttributesType attributes = this.dComponentType.getAttributes();

		if (attributes == null) {
			return null;
		}

		List<PropertyType> lProperty = attributes.getProperty();

		for (PropertyType p : lProperty) {
			if (p.getName().equals(sName)) {
				return p;
			}
		}

		return null;
	}

	/**
	 * Obtain the first value of a property.
	 * 
	 * Returns the first value of a specified property.
	 * 
	 * @param sName
	 *            String name of the property to look for.
	 * @return The first attribute value only
	 */
	public String getFirstValueByName(String sName) {
		PropertyType property = getFirstPropertyByName(sName);

		if (property != null && property.getValue().size() > 0) {
			return property.getValue().get(0);
		}

		return null;
	}

	/**
	 * Get a list of property by name
	 * 
	 * @param sName
	 * @return list of property values
	 */
	public List<String> getValueListByName(String sName) {
		AttributesType attributes = this.dComponentType.getAttributes();
		if (attributes == null) {
			return null;
		}
		List<PropertyType> lProperty = attributes.getProperty();

		for (PropertyType p : lProperty) {
			if (p.getName().equals(sName)) {
				return p.getValue();
			}
		}

		return new ArrayList<String>();
	}

	public void setID(String ID) {
		AttributesType attributes = this.dComponentType.getAttributes();
		if (attributes == null) {
			return;
		}

		List<PropertyType> lProperty = attributes.getProperty();
		if (lProperty == null)
			lProperty = new ArrayList<PropertyType>();

		for (PropertyType p : lProperty) {
			if (GUITARConstants.ID_TAG_NAME.equals(p.getName())) {
				lProperty.remove(p);
				break;
			}
		}

		PropertyType pID = factory.createPropertyType();
		pID.setName(GUITARConstants.ID_TAG_NAME);
		pID.getValue().add(ID);
		lProperty.add(0, pID);
	}

	/**
	 * Set a property of child object
	 * 
	 * @param sTitle
	 * @param sName
	 * @param sValue
	 */
	public void setValueByName(String sTitle, String sName, String sValue) {
		String sMyTitle = getFirstValueByName(GUITARConstants.ID_TAG_NAME);

		if (sTitle.equals(sMyTitle)) {
			AttributesType attributes = this.dComponentType.getAttributes();

			List<PropertyType> lProperty = attributes.getProperty();

			for (PropertyType p : lProperty) {

				if (p.getName().equals(sName)) {
					lProperty.remove(p);
					List<String> lValue = new ArrayList<String>();
					lValue.add(sValue);
					p.setValue(lValue);
					lProperty.add(p);
				}
			}
			attributes.setProperty(lProperty);
			dComponentType.setAttributes(attributes);
		} else if (dComponentType instanceof ContainerType) {

			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			List<ComponentType> lChildren = container.getContents()
					.getWidgetOrContainer();
			List<ComponentType> lNewChildren = new ArrayList<ComponentType>();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				childA.setValueByName(sTitle, sName, sValue);
				lNewChildren.add(childA.getDComponentType());
			}

			contents.setWidgetOrContainer(lNewChildren);
			((ContainerType) dComponentType).setContents(contents);
		}
	}

	/**
	 * 
	 * Add an attribute value to the current ComponentType
	 * jsaddler: This method is used to initialize properties. Via
	 * this method, whenever values are added to component type "by name"
	 * they are created and immediately stored in this component type object.
	 * If the previously set values do not contain the value needing to be set,
	 * it is added to the property that will be returned. 
	 * If the requested property was already present, its value is removed from
	 * the attributes, set with new values as stated above, then re-added to the this
	 * componentTypeWrapper's attributes
	 * <p>
	 * 
	 * @param sName
	 * @param sValue
	 */
	public void addValueByName(String sName, String sValue) {
		AttributesType attributes = dComponentType.getAttributes();

		if (attributes == null)
			attributes = factory.createAttributesType();

		List<PropertyType> lProperty = attributes.getProperty();

		// if the value was already present...
		PropertyType property = null;
		for (PropertyType aProperty : lProperty) {
			if (sName.equals(aProperty.getName())) {
				lProperty.remove(aProperty);
				property = aProperty;
				break;
			}
		}
		// if the value was not already present...
		if (property == null) {
			property = new PropertyType();
			property.setName(sName);
		}

		// if the value wasn't already in this property, it is added
		// to its list of values
		List<String> lValue = property.getValue();

		if (!lValue.contains(sValue)) {
			lValue.add(sValue);
		}

		// if(lProperty==null){
		// attributes.setProperty(new ArrayList<PropertyType>());
		// }
		//
		// attributes.getProperty().add(property);
		// set the property gotten or created with the new value. 
		property.setValue(lValue);
		lProperty.add(property);
		attributes.setProperty(lProperty);
		dComponentType.setAttributes(attributes);
	}

	/**
	 * 
	 * Add an attribute value to the child ComponentType having name Title
	 * 
	 * @param sTitle
	 * @param sName
	 * @param sValue
	 */
	public void addValueByName(String sTitle, String sName, String sValue) {
		String sMyTitle = getFirstValueByName(GUITARConstants.ID_TAG_NAME);

		if (sTitle.equals(sMyTitle)) {

			AttributesType attributes = this.dComponentType.getAttributes();

			List<PropertyType> lProperty = attributes.getProperty();
			PropertyType p = new PropertyType();
			p.setName(sName);
			List<String> lValue = new ArrayList<String>();
			lValue.add(sValue);
			p.setValue(lValue);
			lProperty.add(p);

			attributes.setProperty(lProperty);
			dComponentType.setAttributes(attributes);
		} else

		if (dComponentType instanceof ContainerType) {

			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			List<ComponentType> lChildren = container.getContents()
					.getWidgetOrContainer();
			List<ComponentType> lNewChildren = new ArrayList<ComponentType>();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				childA.addValueByName(sTitle, sName, sValue);
				lNewChildren.add(childA.getDComponentType());
			}

			contents.setWidgetOrContainer(lNewChildren);
			((ContainerType) dComponentType).setContents(contents);
		}
	}

	/**
	 * Remove a property by name
	 * 
	 * @param sName
	 */
	public void removeProperty(String sName) {
		AttributesType attributes = this.dComponentType.getAttributes();
		if (attributes == null)
			return;

		List<PropertyType> lProperties = attributes.getProperty();

		if (lProperties == null)
			return;

		for (PropertyType property : lProperties) {
			if (sName.equals(property.getName())) {
				lProperties.remove(property);
				break;
			}
		}
	}

	/**
	 * 
	 * Search component by id
	 * 
	 * <p>
	 * 
	 * @param ID
	 * @return the component found
	 */
	public ComponentTypeWrapper getChildByID(String ID) {
		String sID = getFirstValueByName(GUITARConstants.ID_TAG_NAME);

		if (ID.equals(sID)) {
			return this;
		} else if (dComponentType instanceof ContainerType) {

			List<ComponentTypeWrapper> lChildrenWrapper = children;
			if (lChildrenWrapper == null) {
				lChildrenWrapper = new ArrayList<ComponentTypeWrapper>();

				ContainerType container = (ContainerType) dComponentType;
				List<ComponentType> lChildren = container.getContents()
						.getWidgetOrContainer();

				for (ComponentType child : lChildren) {
					ComponentTypeWrapper childAdapter = new ComponentTypeWrapper(
							child);
					lChildrenWrapper.add(childAdapter);

				}

			}
			for (ComponentTypeWrapper childAdapter : lChildrenWrapper) {
				ComponentTypeWrapper retComp = childAdapter.getChildByID(ID);
				if (retComp != null)
					return retComp;
			}
		}
		return null;
	}

	/**
	 * Print the information of the component, used for debugging
	 */
	public void printInfo() {
		List<PropertyType> properties = this.getDComponentType()
				.getAttributes().getProperty();
		for (PropertyType p : properties) {
			String out;
			out = p.getName();
			for (String value : p.getValue())
				out += "\t" + value;
			System.out.println(out);
		}

	}

	/**
	 * Check if the component has a child
	 * 
	 * @return <code>true</code> if it has a child
	 */
	public boolean hasChild() {
		if (children == null)
			return false;

		if (children.size() == 0)
			return false;
		return true;
	}

	/**
	 * @param signature
	 * @param name
	 * @param values
	 */
	public void addValueBySignature(AttributesType signature, String name,
			Set<String> values) {

		if (isMatchSignature(signature)) {
			// System.out.println("SIGNATURE FOUND!!!!");

			for (String value : values)
				this.addValueByName(name, value);

		} else

		if (dComponentType instanceof ContainerType) {
			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return;
			List<ComponentType> lChildren = contents.getWidgetOrContainer();
			List<ComponentType> lNewChildren = new ArrayList<ComponentType>();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				childA.addValueBySignature(signature, name, values);
				lNewChildren.add(childA.getDComponentType());
			}
		}

	}

	/**
	 * Check if a component matches with a signature
	 * <p>
	 * 
	 * @param signature
	 * @return <code>true </code> if the component match the signature
	 */
	public boolean isMatchSignature(AttributesType signature) {
		if (signature == null)
			return false;

		AttributesType attributes = this.dComponentType.getAttributes();
		if (attributes == null)
			return false;

		AttributesTypeWrapper wSignature = new AttributesTypeWrapper(signature);
		AttributesTypeWrapper wAttributes = new AttributesTypeWrapper(
				attributes);

		return (wAttributes.containsAll(wSignature));

	}

	/**
	 * jsaddle: Complement method for the getComponentBySignature method. This method rather than creating
	 * a new ComponentTypeWrapper each time we want to search a componentTypeWrapper child of this component
	 * type, uses the children of this component type previously set by parseData to search
	 * the GUI tree for the ComponentTypeWrapper in question. 
	 * 
	 * The componentTypeWrapper returned by this method should contain all the data that a call to parseData put
	 * into it previously, the parent and the child data alongside data about the widget's XML properties
	 * (unlike the getComponentBySignature method, whose return value contains no parent or child data)
	 * @return
	 */
	public ComponentTypeWrapper getComponentBySignaturePreserveTree(AttributesType signature)
	{
		if (isMatchSignature(signature)) 
			return this;
		
		else if (dComponentType instanceof ContainerType) {
			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return null;

			// search depth first for the child in question. 
			ComponentTypeWrapper result = null;
			if(children != null)
				for (ComponentTypeWrapper child : children) {
					result = child.getComponentBySignaturePreserveTree(signature);
					if(result != null)
						return result;
				}
		}

		return null;
	}
	/**
	 * @param signature
	 * @return the component found
	 */
	public ComponentTypeWrapper getComponentBySignature(AttributesType signature) {

		if (isMatchSignature(signature)) {
			return this;
		} else if (dComponentType instanceof ContainerType) {
			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return null;

			
			List<ComponentType> lChildren = contents.getWidgetOrContainer();
			ComponentTypeWrapper result = null;

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				
				result = childA.getComponentBySignature(signature);
				if (result != null)
					return result;
			}
		}

		return null;
	}

	/**
	 * Generate a signature of component
	 * <p>
	 * 
	 * @param lPropertyName
	 *            List of input property names
	 * @return the component signature
	 */
	public AttributesType getSubAttributes(List<String> lPropertyName) {

		AttributesType signature = factory.createAttributesType();
		AttributesType attributes = dComponentType.getAttributes();
		if (attributes == null)
			return signature;

		for (PropertyType property : attributes.getProperty())
			if (lPropertyName.contains(property.getName())) {
				signature.getProperty().add(property);
			}

		return signature;
	}

	/**
	 * @param signature
	 * @param name
	 * @param values
	 */
	public void updateValueBySignature(AttributesType signature, String name,
			Set<String> values) {

		if (isMatchSignature(signature)) {
			for (String value : values)
				this.updateValueByName(name, value);
		} else

		if (dComponentType instanceof ContainerType) {
			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return;
			List<ComponentType> lChildren = contents.getWidgetOrContainer();
			List<ComponentType> lNewChildren = new ArrayList<ComponentType>();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				childA.updateValueBySignature(signature, name, values);
				lNewChildren.add(childA.getDComponentType());
			}
		}
	}

	/**
	 * @param name
	 * @param value
	 */
	private void updateValueByName(String name, String value) {

		AttributesType attributes = dComponentType.getAttributes();

		if (attributes == null)
			attributes = factory.createAttributesType();

		List<PropertyType> lProperty = attributes.getProperty();

		if (lProperty == null)
			lProperty = new ArrayList<PropertyType>();

		int index = 0;

		for (PropertyType aProperty : lProperty) {
			if (name.equals(aProperty.getName())) {
				index = lProperty.indexOf(aProperty);
				lProperty.remove(index);

				break;
			}
		}

		PropertyType property = null;
		property = new PropertyType();
		property.setName(name);

		List<String> lValue = new ArrayList<String>();
		lValue.add(value);

		property.setValue(lValue);

		attributes.getProperty().add(index, property);
		dComponentType.setAttributes(attributes);

	}

	/**
	 * Get max ID in the tree rooted from this node
	 * 
	 * @return max id
	 */
	public int getMaxID() {

		String ID = getFirstValueByName(GUITARConstants.ID_TAG_NAME);

		int nMaxID = 0;

		if (ID == null)
			nMaxID = 0;

		else if (ID.length() < 1)
			nMaxID = 0;
		else {
			ID = ID.substring(1);

			try {
				nMaxID = Integer.parseInt(ID);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (dComponentType instanceof ContainerType) {

			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return 0;

			List<ComponentType> lChildren = contents.getWidgetOrContainer();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				int localMax = childA.getMaxID();
				if (nMaxID < localMax)
					nMaxID = localMax;
			}
		}

		return nMaxID;
	}

	/**
	 * This static variable is used to generate widget ID
	 */
	static int ID_COUNTER = 0;

	/**
	 * Update ID according to the new counter
	 */
	public void updateID() {

		updateValueByName(
				GUITARConstants.ID_TAG_NAME,
				GUITARConstants.COMPONENT_ID_PREFIX
						+ Integer.toString(ID_COUNTER++));

		if (dComponentType instanceof ContainerType) {
			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return;
			List<ComponentType> lChildren = contents.getWidgetOrContainer();
			List<ComponentType> lNewChildren = new ArrayList<ComponentType>();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				childA.updateID();
				lNewChildren.add(childA.getDComponentType());
			}
		}

	}

	/**
	 * @param nID_COUNTER
	 *            the iD_COUNTER to set
	 */
	public static void setID_COUNTER(int nID_COUNTER) {
		ID_COUNTER = nID_COUNTER;
	}

	/**
	 * @param hashcodeGenerator
	 * @param guiTypeWrapper
	 */
	public void generateID(GHashcodeGenerator hashcodeGenerator,
			GUITypeWrapper guiTypeWrapper) {

		long hashcode = hashcodeGenerator.getHashcodeFromData(dComponentType,
				guiTypeWrapper.getData());

		setID(GUITARConstants.COMPONENT_ID_PREFIX + hashcode);

		if (dComponentType instanceof ContainerType) {
			ContainerType container = (ContainerType) dComponentType;
			ContentsType contents = container.getContents();
			if (contents == null)
				return;
			List<ComponentType> lChildren = contents.getWidgetOrContainer();
			List<ComponentType> lNewChildren = new ArrayList<ComponentType>();

			for (ComponentType child : lChildren) {
				ComponentTypeWrapper childA = new ComponentTypeWrapper(child);
				childA.generateID(hashcodeGenerator, guiTypeWrapper);
				lNewChildren.add(childA.getDComponentType());
			}
		}

	}

	/**
	 * Get the list of event ID from widget
	 * 
	 * @return the list of event supported by the component
	 */
	public List<EventType> getEventList() {
		List<String> sActionList = this
				.getValueListByName(GUITARConstants.EVENT_TAG_NAME);

		List<EventType> eventList = null;

		if (sActionList != null)
			eventList = new ArrayList<EventType>();
		for (String action : sActionList) {
			EventType event = new EventType();

			String wigetID = getFirstValueByName(GUITARConstants.ID_TAG_NAME);
			String eventID = wigetID.replace(
					GUITARConstants.COMPONENT_ID_PREFIX,
					GUITARConstants.EVENT_ID_PREFIX);
			String type = getFirstValueByName(GUITARConstants.TYPE_TAG_NAME);

			String posFix = (sActionList.size() <= 1) ? "" : EVENT_ID_SPLITTER
					+ Integer.toString(sActionList.indexOf(action));
			eventID = eventID + posFix;
			event.setEventId(eventID);
			event.setWidgetId(wigetID);
			event.setAction(action);
			event.setType(type);
			eventList.add(event);
		}

		return eventList;
	}

	/**
	 * Add a property with one value to the component.
	 * 
	 * @param strName
	 *            Name of the property.
	 * @param strValue
	 *            Value of the property
	 */
	public void addProperty(String strName, String strValue) {
		AttributesType attributes = dComponentType.getAttributes();
		if (attributes == null) {
			attributes = factory.createAttributesType();
			dComponentType.setAttributes(attributes);
		}

		PropertyType propertyType = new PropertyType();
		propertyType.setName(strName);
		List<String> value = new ArrayList<String>();
		value.add(strValue);
		propertyType.setValue(value);
		attributes.getProperty().add(propertyType);
	}

	private static final String EVENT_ID_SPLITTER = "_";

	/**
	 * Compare two ComponentType and return a ComponentType containing the
	 * differences between the two. The objects being compared may be a derived
	 * class of ComponentType. That is acceptable. The diff is done only for the
	 * ComponentType members.
	 * 
	 * Currently this does not compare the 'optional' AttributesType.
	 * 
	 * @return Returns a List of two Objects. The first Object is a boolean:
	 *         false=match, true=mismatch The second Object is a ComponentType
	 *         with match/mismatch
	 */
	public List<Object> compare(Object obj)
			throws PropertyInconsistentException {
		assert (dComponentType != null);

		// This
		AttributesType thisAttr = (dComponentType == null) ? null
				: dComponentType.getAttributes();

		// Other
		ComponentType otherComponent = (ComponentType) obj;
		AttributesType otherAttr = (otherComponent == null) ? null
				: otherComponent.getAttributes();

		// For receiving from AttributesType
		List<Object> diffList = null;
//		GUITARLog.log.debug("BEGIN Component");
		try {
			if (thisAttr != null) {
				AttributesTypeWrapper attrWrap = new AttributesTypeWrapper(
						thisAttr);
				diffList = attrWrap.compare(otherAttr);
			} else if (otherAttr != null) {
				// This is null, Other is not null
				AttributesTypeWrapper attrWrap = new AttributesTypeWrapper(
						otherAttr);
				diffList = attrWrap.compare(null);
			} else {
				// Both are null. Fake a match when both are null
				diffList = new ArrayList<Object>();
				diffList.add(false);
				diffList.add((AttributesType) null);
			}
		} catch (PropertyInconsistentException e) {
//			GUITARLog.log.debug("DONE Component - inconsistent propoerty");
			throw e;
		}

//		GUITARLog.log.debug("DONE Component");
		// For returning
		List<Object> retList = new ArrayList<Object>();
		// Extract and return retStatus from diffList[0]
		retList.add((Boolean) diffList.get(0));
		// Return AttributesType from diffList[1]
		ComponentType retComponent = new ComponentType();
		retComponent.setAttributes((AttributesType) diffList.get(1));
		retList.add(retComponent);

		return retList;
	}

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		String packageName = ComponentType.class.getPackage().getName();
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(packageName);
			JAXBIntrospector introspector = jc.createJAXBIntrospector();
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			if (null == introspector.getElementName(dComponentType)) {
				JAXBElement<ComponentType> jaxbElement = new JAXBElement<ComponentType>(
						new QName("Widget"), ComponentType.class,
						dComponentType);
				marshaller.marshal(jaxbElement, writer);
			} else {
				marshaller.marshal(dComponentType, writer);
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}

	/**
	 * Get all sub-components of the object
	 * 
	 * @return
	 */
	public List<ComponentType> getAllComponents() {
		List<ComponentType> allComponents = new ArrayList<ComponentType>();
		allComponents.add(dComponentType);
		if (dComponentType instanceof ContainerType) {
			ContentsType contents = ((ContainerType) dComponentType)
					.getContents();
			if (contents != null) {
				List<ComponentType> subComponentList = contents.getWidgetOrContainer();
				for(ComponentType subComponent: subComponentList){
					ComponentTypeWrapper wSubComponent = new ComponentTypeWrapper(subComponent);
					List<ComponentType> allSubComponents = wSubComponent.getAllComponents();
					allComponents.addAll(allSubComponents);
				}
			}
		}
		return allComponents;
	}
	
	public static AttributesType trimToSignatureQuality(AttributesType origOptional)
	{
		ArrayList<PropertyType> newProps = new ArrayList<PropertyType>();
		for(PropertyType oldProp : origOptional.getProperty()) {
			boolean containsSomething = true;
			if(!oldProp.getValue().isEmpty()) { 
				for(String value : oldProp.getValue()) 
					if(value != null && !value.isEmpty()) {
						containsSomething = false;
						break;
					}
			}
			if(!containsSomething)
				newProps.add(oldProp);
		}
		AttributesType attType = factory.createAttributesType();
		attType.setProperty(newProps);
		return attType;
	}
}
