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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.guitar.model.GHashcodeGenerator;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.GUIType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.exception.PropertyInconsistentException;
import edu.umd.cs.guitar.util.GUITARLog;

/**
 * Adapter class to process GUIType.
 * 
 * <p>
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 * 
 * jsaddler: This class holds real references to ripped interfaces, but fills something 
 * called a ComponentTypeWrapper with all information necessary to build an information
 * bearing structure called a wrapper tree, containing all relevant information about the interface.
 * To build this tree, we need a GUIStructure object, which we can obtain from either a rip
 * or from a persisted GUI xml file.  
 */
public class GUITypeWrapper {
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GUITypeWrapper [ID=" + ID + "]";
	}

	GUIType dGUIType;
	String ID;

	// -------------------------------
	// Parsed structured data
	// -------------------------------

	/**
	 * Root container of the window
	 */
	ComponentTypeWrapper container;

	/**
     * 
     */
	ComponentTypeWrapper invoker;

	List<ComponentTypeWrapper> lInvokers;

	/**
	 * @return the lInvokers
	 */
	public List<ComponentTypeWrapper> getInvokerList() {
		if (lInvokers == null) {
			lInvokers = new ArrayList<ComponentTypeWrapper>();
		}
		return this.lInvokers;
	}

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

//		GUITARLog.log.debug("Parsing window: " + this.getTitle());

		this.container = new ComponentTypeWrapper(this.dGUIType.getContainer());
		this.container.setWindow(this);
		this.container.parseData(dGUIStructure, wGUIStructure);

	}

	/**
	 * @return the invoker
	 */
	public ComponentTypeWrapper getInvoker() {
		return invoker;
	}

	/**
	 * @param invoker
	 *            the invoker to set
	 */
	public void setInvoker(ComponentTypeWrapper invoker) {
		this.invoker = invoker;
	}

	/**
	 * @return the container
	 */
	public ComponentTypeWrapper getContainer() {
		return this.container;
	}

	/**
	 * @param data
	 */
	public GUITypeWrapper(GUIType data) {
		super();
		this.dGUIType = data;
		this.ID = getTitle();
	}

	/**
	 * @return the data
	 */
	public GUIType getData() {
		return dGUIType;
	}

	public ComponentTypeWrapper getChildByID(String ID) {

		ComponentTypeWrapper subContainer = container;

		if (subContainer == null)
			subContainer = new ComponentTypeWrapper(dGUIType.getContainer());

		return subContainer.getChildByID(ID);
	}

	public List<ComponentType> getAllComponents() {
		ComponentTypeWrapper subContainer = container;

		if (subContainer == null)
			subContainer = new ComponentTypeWrapper(dGUIType.getContainer());

		return subContainer.getAllComponents();
	}

	public List<String> getAllComponentID() {
		List<String> allComponentID = new ArrayList<String>();
		List<ComponentType> allComponents = getAllComponents();
		for (ComponentType component : allComponents) {
			ComponentTypeWrapper wComponent = new ComponentTypeWrapper(
					component);
			String id = wComponent
					.getFirstValueByName(GUITARConstants.ID_TAG_NAME);
			if (id != null)
				allComponentID.add(id);
		}

		return allComponentID;
	}

	public void setWindowValueByName(String sName, String sValue) {
		ComponentType window = dGUIType.getWindow();
		ComponentTypeWrapper windowA = new ComponentTypeWrapper(window);
		List<String> propertyValue = windowA.getValueListByName(sName);
		propertyValue.clear();
		propertyValue.add(sValue);
	}

	public void setValueByName(String sTitle, String sName, String sValue) {
		ComponentType window = dGUIType.getWindow();
		ComponentTypeWrapper windowA = new ComponentTypeWrapper(window);
		windowA.setValueByName(sTitle, sName, sValue);

		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		containerA.setValueByName(sTitle, sName, sValue);
	}

	public void addValueByName(String sTitle, String sName, String sValue) {
		ComponentType window = dGUIType.getWindow();
		ComponentTypeWrapper windowA = new ComponentTypeWrapper(window);
		windowA.addValueByName(sTitle, sName, sValue);

		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		containerA.addValueByName(sTitle, sName, sValue);
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof GUITypeWrapper))
			return false;

		GUITypeWrapper other = (GUITypeWrapper) obj;

		String sMyTitle = getTitle();
		String sOtherTitle = other.getTitle();

		return sMyTitle.equals(sOtherTitle);
	}

	/**
	 * Compare and compute the difference between this GUIType object and the
	 * specified 'obj'.
	 * 
	 * @return Returns a List of two Objects. The first Object is a boolean:
	 *         false=match, true=mismatch The second Object is a GUIType with
	 *         match/mismatch
	 */
	public List<Object> compare(Object obj) {
		List<Object> retList = new ArrayList<Object>();
		boolean retStatus = false;

		assert (obj instanceof GUIType || obj == null);

		GUIType oguiType = (GUIType) obj;
		GUIType diffGUIType = new GUIType();

		// Both should not be null
		assert (dGUIType != null);
		assert (!(dGUIType == null && oguiType == null));

//		GUITARLog.log.debug("BEGIN - GUIType");
		if (oguiType == null) {
//			GUITARLog.log.debug("DONE - GUIType");

			diffGUIType.setWindow(dGUIType.getWindow());
			diffGUIType.setContainer(dGUIType.getContainer());
			retList.add(true);
			retList.add(diffGUIType);
			return retList;
		}

		// Other
		ComponentType oWindow = oguiType.getWindow();
		ContainerType oContainer = oguiType.getContainer();

		// This object
		ComponentType thisWindow = dGUIType.getWindow();
//		GUITARLog.log.info((new ComponentTypeWrapper(thisWindow))
//				.getFirstValueByName(GUITARConstants.TITLE_TAG_NAME));
		ContainerType thisContainer = dGUIType.getContainer();

		ComponentType diffWindow = null;
		if (thisWindow != null) {
			List<Object> diffList;
			diffList = (new ComponentTypeWrapper(thisWindow)).compare(oWindow);
			retStatus = retStatus || (Boolean) diffList.get(0);
			diffWindow = (ComponentType) diffList.get(1);
		}

		ContainerType diffContainer = null;
		try {
			if (thisContainer != null) {
				List<Object> diffList;
				diffList = (new ContainerTypeWrapper(thisContainer))
						.compare(oContainer);
				retStatus = retStatus || (Boolean) diffList.get(0);
				diffContainer = (ContainerType) diffList.get(1);
			}
		} catch (PropertyInconsistentException e) {
//			GUITARLog.log.debug("DONE - GUIType - inconsistent property");
			throw e;
		}

//		GUITARLog.log.debug("DONE - GUIType");

		// Construct and return diff
		diffGUIType.setWindow(diffWindow);
		diffGUIType.setContainer(diffContainer);

		retList.add(retStatus);
		retList.add(diffGUIType);

		return retList;
	}

	/**
	 * Get title of the GUIType window
	 * 
	 * @return String containing the GUIType (window) title
	 */
	public String getTitle() {
		ComponentType window = dGUIType.getWindow();
		ComponentTypeWrapper winAdapter = new ComponentTypeWrapper(window);
		String sGUITitle = winAdapter
				.getFirstValueByName(GUITARConstants.TITLE_TAG_NAME);

		return sGUITitle;
	}

	/**
	 * Set title of the GUIType window
	 * 
	 * @return None
	 */
	public void setTitle(String sTitle) {
		ObjectFactory factory = new ObjectFactory();

		ComponentType window = dGUIType.getWindow();
		AttributesType attributes = window.getAttributes();

		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(GUITARConstants.TITLE_TAG_NAME);

		List<String> value = new ArrayList<String>();
		value.add(sTitle);

		newProperty.setValue(value);

		List<PropertyType> lProperty = attributes.getProperty();

		for (int i = 0; i < lProperty.size(); i++) {
			PropertyType p = lProperty.get(i);

			if (p.getName().equals(GUITARConstants.TITLE_TAG_NAME)) {
				lProperty.add(i, newProperty);
				lProperty.remove(p);
			}
		}
	}

	public boolean isRoot() {
		ComponentType window = dGUIType.getWindow();
		ComponentTypeWrapper windowA = new ComponentTypeWrapper(window);
		String isRoot = windowA
				.getFirstValueByName(GUITARConstants.ROOTWINDOW_TAG_NAME);
		return (isRoot.equalsIgnoreCase("true"));

	}

	/**
	 * Check if the window is modal
	 * 
	 * <p>
	 * 
	 * @return
	 */
	public boolean isModal() {
		ComponentType window = dGUIType.getWindow();
		ComponentTypeWrapper windowA = new ComponentTypeWrapper(window);
		String isRoot = windowA
				.getFirstValueByName(GUITARConstants.MODAL_TAG_NAME);
		return (isRoot.equalsIgnoreCase("true"));

	}

	/**
	 * Get a list of windows must be available when this window is opened
	 * 
	 * <p>
	 * 
	 * @return
	 */
	public Set<GUITypeWrapper> getAvailableWindowList() {
		Set<GUITypeWrapper> retWins = new HashSet<GUITypeWrapper>();

		GUITypeWrapper availWindow = this;
		ComponentTypeWrapper invoker;

		while (!availWindow.isModal()) {
			retWins.add(availWindow);
			invoker = availWindow.invoker;
			if (invoker == null)
				break;
			availWindow = invoker.getWindow();
		}

		retWins.add(availWindow);
		return retWins;

	}

	/**
	 * Get a list of windows must be available when this window is opened
	 * 
	 * <p>
	 * 
	 * @return
	 */
	public Set<GUITypeWrapper> getAvailableWindowListNew() {
		Set<GUITypeWrapper> retWins = getAvailableWindowHelper(this);

		return retWins;
	}

	// Set<GUITypeWrapper> allAvailWindows=new HashSet<GUITypeWrapper>();
	private Set<GUITypeWrapper> getAvailableWindowHelper(GUITypeWrapper window) {
		Set<GUITypeWrapper> allAvailWindows = new HashSet<GUITypeWrapper>();
		if (window.isModal()) {
			allAvailWindows.add(window);
		} else {

			for (ComponentTypeWrapper invoker : window.getInvokerList()) {
				GUITypeWrapper parentWindow = invoker.getWindow();
				Set<GUITypeWrapper> allParentAvailWindows = getAvailableWindowHelper(parentWindow);
				allAvailWindows.addAll(allParentAvailWindows);
			}

			// ComponentTypeWrapper invoker = window.getInvoker();
			//
			// if (invoker != null) {
			// Set<GUITypeWrapper> allParentAvailWindows;
			// GUITypeWrapper parentWindow = invoker.getWindow();
			// allParentAvailWindows = getAvailableWindowHelper(parentWindow);
			// allAvailWindows.addAll(allParentAvailWindows);
			// }
			allAvailWindows.add(window);
		}

		return allAvailWindows;

	}

	/**
	 * @param signature
	 * @param name
	 * @param values
	 */
	public void addValueBySignature(AttributesType signature, String name,
			Set<String> values) {
		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		containerA.addValueBySignature(signature, name, values);

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
	 */
	public ComponentTypeWrapper getComponentBySignaturePreserveTree(AttributesType signature)
	{
		if(container != null)
			return container.getComponentBySignaturePreserveTree(signature);
		return null;
	}
	
	
	/**
	 * @param signature
	 * @return
	 */
	public ComponentTypeWrapper getComponentBySignature(AttributesType signature) {
		ComponentTypeWrapper container = new ComponentTypeWrapper(
				dGUIType.getContainer());
		return container.getComponentBySignature(signature);
	}

	/**
	 * @param signature
	 * @param name
	 * @param values
	 */
	public void updateValueBySignature(AttributesType signature, String name,
			Set<String> values) {
		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		containerA.updateValueBySignature(signature, name, values);

	}

	/**
	 * @return
	 */
	public int getMaxID() {
		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		return containerA.getMaxID();
	}

	/**
	 * 
	 */
	public void updateID() {
		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		containerA.updateID();
	}

	/**
	 * Generate ID for widgets based on a hashcode generator
	 * 
	 * @param hashcodeGenerator
	 */
	public void generateID(GHashcodeGenerator hashcodeGenerator) {
		ComponentType container = dGUIType.getContainer();
		ComponentTypeWrapper containerA = new ComponentTypeWrapper(container);
		containerA.generateID(hashcodeGenerator, this);
	}

	/**
	 * Add a property with one value to the "window" of the GUIType.
	 * 
	 * @param strName
	 *            Name of the property.
	 * @param strValue
	 *            Value of the property
	 */
	public void addWindowProperty(String strName, String strValue) {
		ComponentType window = dGUIType.getWindow();
		AttributesType attributes = window.getAttributes();

		PropertyType propertyType = new PropertyType();
		propertyType.setName(strName);
		List<String> value = new ArrayList<String>();
		value.add(strValue);
		propertyType.setValue(value);
		attributes.getProperty().add(propertyType);
	}

	/**
	 * Remove properties with the given name strName from the "window" of the
	 * GUIType.
	 * 
	 * @param strName
	 *            Name of the property.
	 */
	public void deleteWindowProperty(String strName) {
		ComponentType window = dGUIType.getWindow();
		AttributesType attributes = window.getAttributes();

		List<PropertyType> lProperty = attributes.getProperty();

		for (int i = 0; i < lProperty.size(); i++) {
			PropertyType p = lProperty.get(i);

			if (p.getName().equals(strName)) {
				lProperty.remove(p);
			}
		}
	}

	/**
	 * Get properties with the given name strName from the "window" of the
	 * GUIType.
	 * 
	 * @param strName
	 *            Name of the property.
	 * @returns Returns properties for strName on success, null on failure
	 */
	public PropertyType getWindowProperty(String strName) {
		ComponentType window = dGUIType.getWindow();
		AttributesType attributes = window.getAttributes();

		List<PropertyType> lProperty = attributes.getProperty();

		for (int i = 0; i < lProperty.size(); i++) {
			PropertyType p = lProperty.get(i);

			if (p.getName().equals(strName)) {
				return p;
			}
		}

		return null;
	}

} // End of class
