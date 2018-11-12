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

package edu.umd.cs.guitar.model;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.accessibility.*;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
//import javax.swing.SwingUtilities;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.event.EventManager;
import edu.umd.cs.guitar.event.GEvent;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.ContentsType;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.PropertyTypeWrapper;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.guitarext.GUIEventModel;
import edu.unl.cse.guitarext.HeadTable;
import edu.unl.cse.guitarext.JavaTestInteractions;
/**
 * Implementation for {@link GComponent} for Java Swing
 */
public class JFCXComponent extends GComponent {

	final Component component;
	public static boolean nowRipping = false;

	private String storedCTHEventId;
	private List<String> storedEventIDs;
	/**
	 * @param component
	 */
	public JFCXComponent(Component component, GWindow window) {

		super(window);
		this.component = component;
		storedCTHEventId = "";
		storedEventIDs = new ArrayList<String>();
	}

	public Window getWindow()
	{
		JFCXWindow myWindow = (JFCXWindow)window;
		return myWindow.getWindow();
	}

	public AccessibleContext getAccessibleContext()
	{
		if(component == null)
			return null;

		return component.getAccessibleContext();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GComponent#getGUIProperties()
	 */
	@Override
	public List<PropertyType> getGUIProperties() {
		List<PropertyType> retList = new ArrayList<PropertyType>();
		// Other properties

		PropertyType p;
		List<String> lPropertyValue;
		String sValue;

		// Title
		sValue = null;
		sValue = getTitle();
		if (sValue != null) {
			p = factory.createPropertyType();
			p.setName(JFCConstants.TITLE_TAG);
			lPropertyValue = new ArrayList<String>();
			lPropertyValue.add(sValue);
			p.getValue().addAll(lPropertyValue);
			retList.add(p);
		}

		// Icon
		sValue = null;
		sValue = getIconName();
		if (sValue != null) {
			p = factory.createPropertyType();
			p.setName(JFCConstants.ICON_TAG);
			lPropertyValue = new ArrayList<String>();
			lPropertyValue.add(sValue);
			p.getValue().addAll(lPropertyValue);
			retList.add(p);
		}

		// Index in parent
		if (isSelectedByParent()) {
			sValue = null;
			sValue = getIndexInParent().toString();

			p = factory.createPropertyType();
			p.setName(JFCConstants.INDEX_TAG);
			lPropertyValue = new ArrayList<String>();
			lPropertyValue.add(sValue);
			p.getValue().clear();
			p.getValue().addAll(lPropertyValue);
			retList.add(p);

		}

		// get ActionListeners
		List<String> actionListener = getActionListenerClasses();
		if (actionListener != null) {
			p = factory.createPropertyType();
			p.setName("ActionListeners");
			p.getValue().clear();
			p.getValue().addAll(actionListener);
			retList.add(p);
		}

		// Get bean properties
		List<PropertyType> lBeanProperties = getGUIBeanProperties();
		retList.addAll(lBeanProperties);

		// Get Screenshot
		return retList;

	}

	/**
	 * Get component index in its parent
	 *
	 * @return
	 */
	private Integer getIndexInParent() {

		// AccessibleContext aContext = aComponent.getAccessibleContext();
		AccessibleContext aContext = component.getAccessibleContext();
		if (aContext != null) {
			return aContext.getAccessibleIndexInParent();
		}

		return 0;
	}

	/**
	 * Check if the component is activated by an action in parent
	 *
	 * @return
	 */
	private boolean isSelectedByParent() {
		// if (aComponent instanceof Component) {
		Container parent = ((Component) this.component).getParent();

		if (parent == null)
			return false;

		if (parent instanceof JTabbedPane)
			return true;
		// }
		return false;
	}

	/**
	 * Get all bean properties of the component
	 *
	 * @return
	 */
	private List<PropertyType> getGUIBeanProperties() {
		List<PropertyType> retList = new ArrayList<PropertyType>();
		Method[] methods = component.getClass().getMethods();
		PropertyType p;
		List<String> lPropertyValue;

		for (Method m : methods) {
			if (m.getParameterTypes().length > 0) {
				continue;
			}
			String sMethodName = m.getName();
			String sPropertyName = sMethodName;

			if (sPropertyName.startsWith("get")) {
				sPropertyName = sPropertyName.substring(3);
			} else if (sPropertyName.startsWith("is")) {
				sPropertyName = sPropertyName.substring(2);
			} else
				continue;

			// make sure property is in lower case
			sPropertyName = sPropertyName.toLowerCase();

			if (JFCConstants.GUI_PROPERTIES_LIST.contains(sPropertyName)) {

				Object value;
				try {
					// value = m.invoke(aComponent, new Object[0]);
					value = m.invoke(component, new Object[0]);
					if (value != null) {
						p = factory.createPropertyType();
						lPropertyValue = new ArrayList<String>();
						lPropertyValue.add(value.toString());
						p.setName(sPropertyName);
						p.getValue().clear();
						p.getValue().addAll(lPropertyValue);
						retList.add(p);
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
		}
		return retList;
	}

	/**
	 * Return the children of this JFCXComponent. Note that only
	 * components that are containers can contain children.
	 * Searches through two sources where children can be found:
	 * the components returned from container.getComponent(), and if this
	 * fails, from the the Components returned from
	 * container.getAccessibleContext().getAccessibleChild().
	 * Each component found is converted to a GComponent, and returned
	 * along with its siblings in the returned list.
	 *
	 * Preconditions: 	none
	 * Postconditions: 	if component has children, they are returned as
	 * 					newly instantiated GComponents. Else, an empty list
	 * 					is returned.
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GXComponent#getChildren()
	 */
	@Override
	public List<GComponent> getChildren() {
		List<GComponent> retList = new ArrayList<GComponent>();

		if (component instanceof Container) {

			Container container = (Container) component;

			try {
				int nChildren = 0;

				nChildren = container.getComponentCount();

				if (nChildren > 0) {
					for (int i = 0; i < nChildren; i++) {
						Component cChild = container.getComponent(i);
						GComponent gChild = new JFCXComponent(cChild, window);
						retList.add(gChild);
					}
				} else {
					AccessibleContext aContext = container.getAccessibleContext();
					if (aContext == null)
						return retList;
					nChildren = aContext.getAccessibleChildrenCount();
					for (int i = 0; i < nChildren; i++) {
						Accessible aChild = aContext.getAccessibleChild(i);
						if (aChild instanceof Component) {
							Component cChild = (Component) aChild;
							GComponent gChild = new JFCXComponent(cChild,
									window);
							retList.add(gChild);
						}
					}

				}

			} catch (Exception e) {
				System.err.println("Error: getChildren");
				System.err.println(e);
			}
		}

		return retList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GXComponent#getParent()
	 */
	@Override
	public GComponent getParent() {
		Component parent = this.component.getParent();

		return new JFCXComponent(parent, window);
	}

	/**
	 * jsaddler: Checks to see if this component has children, and returns
	 * true if it does.
	 *
	 * Preconditions: 	component is not null.
	 * Postconditions:	True is returned if this component has children.
	 * 					False is returned otherwise.
	 */
	@Override
	public boolean hasChildren()
	{
		AccessibleContext xContext = component.getAccessibleContext();

		if (xContext != null) {
			int nChildren = xContext.getAccessibleChildrenCount();
			if (nChildren > 0)
				return true;
		}

		if (component instanceof Container) {
			Container container = (Container) component;
			if (container.getComponentCount() > 0)
				return true;
		}

		return false;
	}

	public static boolean hasChildren(Component theComponent)
	{
		AccessibleContext xContext = theComponent.getAccessibleContext();

		if (xContext == null)
			return false;

		// TODO: Check this
		int nChildren = xContext.getAccessibleChildrenCount();

		if (nChildren > 0)
			return true;

		if (theComponent instanceof Container) {
			Container container = (Container) theComponent;

			if (container.getComponentCount() > 0)
				return true;
		}

		return false;
	}

	/**
	 * @return the component
	 */
	public Component getComponent() {
		return component;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;

		int result = 1;
		List<PropertyType> lProperties = getGUIProperties();

		for (PropertyType property : lProperties) {
			if (JFCConstants.ID_PROPERTIES.contains(property.getName())) {
				String name = property.getName();

				result = prime * result + (name == null ? 0 : name.hashCode());

				List<String> valueList = property.getValue();

				if (valueList != null)
					for (String value : valueList) {
						result = prime * result
								+ (value == null ? 0 : value.hashCode());

					}
			}
		}

		// Class
		result = prime * result + GUITARConstants.CLASS_TAG_NAME.hashCode();
		result = prime * result + getClassVal().hashCode();

		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JFCXComponent other = (JFCXComponent) obj;
		if (component == null) {
			if (other.component != null)
				return false;
		} else if (!component.equals(other.component))
			return false;
		return true;
	}

	public static String getSimpleName(Component component)
	{
		String toReturn = "";
		if(component != null) {
			toReturn = component.getClass().getSimpleName();
			if(toReturn.isEmpty()) {
				toReturn = component.getClass().getName();
				int dotIndex = toReturn.lastIndexOf('.');
				if(dotIndex != -1)
					toReturn = toReturn.substring(dotIndex+1);
				toReturn = toReturn.replace("_", "");
			}
		}
		return toReturn;
	}

	public boolean isTabPageList()
	{
		if (component == null || component.getAccessibleContext() == null)
			return false;
		AccessibleContext aContext = component.getAccessibleContext();
		if(aContext.getAccessibleRole().equals(AccessibleRole.PAGE_TAB_LIST))
			return true;
		return false;
	}
	public static boolean isTabPageList(Component component)
	{
		if (component == null || component.getAccessibleContext() == null)
			return false;
		AccessibleContext aContext = component.getAccessibleContext();
		if(aContext.getAccessibleRole().equals(AccessibleRole.PAGE_TAB_LIST))
			return true;
		return false;
	}
	public static String getDirectParentTitle(Component cWithParent)
	{
		Container component = cWithParent.getParent();
		if (component == null || component.getAccessibleContext() == null)
			return ""; // give up finding a title. This component has no title.
		AccessibleContext aContext = component.getAccessibleContext();
		// keep trying until we find the right name.

		// accessible name?
		String sName;
		if(isTabPageList(component)) {

			int childCount = aContext.getAccessibleChildrenCount();

			if (childCount > 0) {
				String firstCName = aContext.getAccessibleChild(0).getAccessibleContext().getAccessibleName();
				return firstCName;
			}
			else
				return aContext.getAccessibleName();
		}

		sName = aContext.getAccessibleName();
		if (sName != null && !sName.isEmpty())
			return sName;

		// icon name?
		sName = JFCXComponent.parseIconName(component);
		if (sName != null && !sName.isEmpty())
			return sName;

		if(component.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.COMBO_BOX))
			return getSimpleName(component);

		// name to the right of component?
		sName = component.getName();
		// Use the left sibling title instead
		Container parent = component.getParent();
		if (parent != null) {
//					int index = getIndex(parent, component);
			int index = parseIndex(parent, component);
			if (index > 0) {
				Component rightComponent = parent.getComponent(index - 1);
				if(rightComponent.getAccessibleContext() != null)
					sName = getTitle(rightComponent);
			}
		}
		// the parent doesn't have a name or a left sibling with a name, return empty string.
		return "";
	}
	/**
	 * Public statically accessible method to return the name of the java component
	 * passed in.
	 * @return
	 */
	public static String getTitle(Component component)
	{
		if (component == null || component.getAccessibleContext() == null)
			return ""; // give up finding a title. This component has no title.

		AccessibleContext aContext = component.getAccessibleContext();
		// keep trying until we find the right name.

		// accessible name?
		String sName;
		if(isTabPageList(component)) {

			int childCount = aContext.getAccessibleChildrenCount();

			if (childCount > 0) {
				String firstCName = aContext.getAccessibleChild(0).getAccessibleContext().getAccessibleName();
				return firstCName;
			}
			else
				return aContext.getAccessibleName();
		}

		sName = aContext.getAccessibleName();
		if (sName != null && !sName.isEmpty())
			return sName;

		// icon name?
		sName = JFCXComponent.parseIconName(component);
		if (sName != null && !sName.isEmpty())
			return sName;

		if(component.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.COMBO_BOX))
			return getSimpleName(component);

		// name to the right of component?
		sName = component.getName();
		// Use the left sibling title instead
		Container parent = component.getParent();
		if (parent != null) {
//			int index = getIndex(parent, component);
			int index = parseIndex(parent, component);
			if (index > 0) {
				Component rightComponent = parent.getComponent(index - 1);
				if(rightComponent.getAccessibleContext() != null)
					sName = getTitle(rightComponent);
			}
			// jsaddle: if the component doesn't have a left sibling, then inherit its direct parent's title
			if(sName == null || sName.isEmpty())
				sName = getDirectParentTitle(component);
		}

		// In the worst case we must use the screen position
		if (sName == null || sName.isEmpty()) {
			sName = "Pos(" +
					JFCXComponent.getGUITAROffsetXInWindow(component) + "," +
					JFCXComponent.getGUITAROffsetYInWindow(component) + ")";
		}
		return sName;
	}

	public String getTooltipText()
	{
		if(component == null || component.getAccessibleContext() == null)
			return "";
		if(!(component instanceof JComponent))
			return "";
		JComponent jC = (JComponent)component;
		String s = jC.getToolTipText();
		if(s == null)
			return "";
		return s;
	}
	/**
	 * Returns the name of this JFCXComponent.
	 */
	@Override
	public String getTitle()
	{
		if (component == null || component.getAccessibleContext() == null)
			return ""; // give up finding a title. This component has no title.

		AccessibleContext aContext = component.getAccessibleContext();
		// keep trying until we find the right name.

		// accessible name?
		String sName;
		if(isTabPageList()) {
			int childCount = aContext.getAccessibleChildrenCount();

			if (childCount > 0) {
				String firstCName = aContext.getAccessibleChild(0).getAccessibleContext().getAccessibleName();
				return firstCName;
			}
			else
				return aContext.getAccessibleName();
		}


		sName = aContext.getAccessibleName();
		if (sName != null && !sName.isEmpty())
			return sName;

		// icon name?
		sName = getIconName();
		if (sName != null && !sName.isEmpty())
			return sName;

		if(isCombo())
			return getSimpleName(component);

		// name to the right of or above component?
		sName = component.getName();
		// Use the left sibling title instead
		Container parent = component.getParent();
		if (parent != null) {
			int index = getIndex(parent, component);
			if (index > 0) {
				Component rightComponent = parent.getComponent(index - 1);
				JFCXComponent jfcRightComponent = new JFCXComponent(
						rightComponent, window);
				sName = jfcRightComponent.getTitle();
				// jsaddle: if the component doesn't have a left sibling, then inherit its direct parent's title
				if(sName == null || sName.isEmpty())
					sName = getDirectParentTitle(component);
			}
		}

		// In the worst case we must use the screen position
		if (sName == null || sName.isEmpty()) {
			sName = "Pos(" +
					JFCXComponent.getGUITAROffsetXInWindow(component) + "," +
					JFCXComponent.getGUITAROffsetYInWindow(component) + ")";
		}

		return sName;
	}

	/**
	 * Parse the index of this component within the parent container's children.
	 *
	 * Preconditions:	parent and javaComponent are not null
	 * Postconditions:	If javaComponent is a child of parent, return the index of the child
	 * 					Otherwise, return -1.
	 */
	public static int parseIndex(Container parent, Component javaComponent)
	{
		Component[] children = parent.getComponents();
		if (children == null)
			return -1;

		for (int index = 0; index < children.length; index++) {
			if (javaComponent.equals(children[index]))
				return index;
		}
		return -1;
	}

	private static int getIndex(Container parent, Component component) {
		Component[] children = parent.getComponents();
		if (children == null)
			return -1;

		for (int index = 0; index < children.length; index++) {
			if (component.equals(children[index]))
				return index;
		}
		return -1;
	}

	/**
	 * Parse the icon name of a javaComponent from the resource's absolute path.
	 *
	 * Preconditions: 	javaComponent is not null
	 * 					There are no java runtime permission issues that restrict
	 * 					us from picking up resource information about images rendered for display within components
	 * Postconditions: 	the simple filename (no path) of the image displayed in this component's icon is returned.
	 */
	public static String parseIconName(Component javaComponent)
	{
		String retIcon = null;
//		return javaComponent.getAccessibleContext().getAccessibleIcon()[0].getAccessibleIconDescription();
		try {
			Class<?> partypes[] = new Class[0];
			Method m = javaComponent.getClass().getMethod("getIcon", partypes);

			String sIconPath = null;
			// if (m != null) {
			// Object obj = (m.invoke(aComponent, new Object[0]));
			// if (obj != null)
			// sIconPath = obj.toString();
			// }

			if (m != null) {
				Object obj = (m.invoke(javaComponent, new Object[0]));

				if (obj != null) {
					sIconPath = obj.toString();
				}
			}

			if (sIconPath == null || sIconPath.contains("@"))
				return null;

			String[] sIconElements = sIconPath.split(File.separator);
			retIcon = sIconElements[sIconElements.length - 1];
			// remove periods from the name.
			for(int dotPos = retIcon.lastIndexOf('.'); dotPos != -1; dotPos = retIcon.lastIndexOf('.'))
				retIcon = retIcon.substring(0, dotPos);


		} catch (SecurityException e) {
			// e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			// e.printStackTrace();
			return null;
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			// e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			// e.printStackTrace();
			return null;
		}
		return retIcon;
	}

	public static String getGuitarIconName(Component c, GWindow w)
	{
		JFCXComponent myComp = new JFCXComponent(c, w);
		return myComp.getIconName();
	}

	/**
	 * Parse the icon name of a widget from the resource's absolute path.
	 *
	 * <p>
	 *
	 * @param component
	 * @return
	 */
	private String getIconName() {
		String retIcon = null;
		try {
			Class<?> partypes[] = new Class[0];
			Method m = component.getClass().getMethod("getIcon", partypes);

			String sIconPath = null;

			if (m != null) {
				Object obj = (m.invoke(component, new Object[0]));

				if (obj != null) {
					sIconPath = obj.toString();
				}
			}

			if (sIconPath == null || sIconPath.contains("@"))
				return null;

			String[] sIconElements = sIconPath.split(File.separator);
			retIcon = sIconElements[sIconElements.length - 1];
			for(int dotPos = retIcon.lastIndexOf('.'); dotPos != -1; dotPos = retIcon.lastIndexOf('.'))
				retIcon = retIcon.substring(0, dotPos);


		} catch (SecurityException e) {
			// e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			// e.printStackTrace();
			return null;
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			// e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			// e.printStackTrace();
			return null;
		}
		return retIcon;
	}

	private List<String> getActionListenerClasses() {
		List<String> ret = null;
		Class<?> c = component.getClass();
		try {
			Method m = c.getMethod("getActionListeners");
			if (m.getReturnType() == ActionListener[].class) {
				ActionListener[] listeners = (ActionListener[]) m
						.invoke(component);
				if (listeners != null && listeners.length > 0) {
					// it is quite usual that only one listener is registered
					if (listeners.length == 1) {
						ret = new ArrayList<String>();
						ret.add(listeners[0].getClass().getName());
					} else {
						// to avoid duplicates a HashSet is used
						HashSet<String> tmp = new HashSet<String>();
						for (ActionListener al : listeners) {
							tmp.add(al.getClass().getName());
						}
						ret = new ArrayList<String>(tmp);
					}
				}
			}
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return ret;
	}

	// TODO: Move this variable to an external configuration file
	// Keeping it here because we don't want to interfere the current GUITAR
	// structure
	private static List<String> IGNORED_CLASS_EVENTS = Arrays.asList(
			"com.jgoodies.looks.plastic.PlasticArrowButton",
			"com.jgoodies.looks.plastic.PlasticComboBoxButton",
			"com.jgoodies.looks.plastic.PlasticSpinnerUI$SpinnerArrowButton",
			"javax.swing.plaf.synth.SynthScrollBarUI$1",
			"javax.swing.plaf.synth.SynthScrollBarUI$2",
			"javax.swing.plaf.synth.SynthArrowButton",
			"javax.swing.plaf.basic.BasicComboPopup$1",
			"javax.swing.JScrollPane$ScrollBar",
			"javax.swing.plaf.metal.MetalScrollButton",
			"javax.swing.plaf.metal.MetalComboBoxButton",
			"sun.awt.X11.XFileDialogPeer$2",
			"javax.swing.JScrollPane$ScrollBar", "sun.swing.FilePane$1",
			"sun.swing.FilePane$2", "sun.swing.FilePane$3",
			"sun.swing.FilePane$4", "sun.swing.FilePane$5"

	);

	public GComponent getFirstChild(List<PropertyTypeWrapper> lIDProperties) {

		List<PropertyType> lProperties;
		//ComponentType comp = extractIDProperties();
		//List<PropertyType> lProperties = comp.getAttributes().getProperty();

		lProperties = getIDProperties();
		List<PropertyTypeWrapper> normalPropertyTypeAdapters = new ArrayList<PropertyTypeWrapper>();

		//System.out.println("----");
		List<PropertyTypeWrapper> eventIDAdapters = new ArrayList<PropertyTypeWrapper>();
		for (PropertyTypeWrapper p : lIDProperties){
			if(p.getProperty().getName().equals(GUITARConstants.CTH_EVENT_ID_NAME))
				eventIDAdapters.add(p);
			else
				normalPropertyTypeAdapters.add(p);
		}

		// run a special check if there are any eventIDAdapters found, and run the check only by the first one.

		if(!eventIDAdapters.isEmpty()) {
			boolean idMatch = false;
			for (PropertyType p : lProperties) {
				if(p != null && p.getName() != null && p.getName().equals(GUITARConstants.CTH_EVENT_ID_NAME)) {
					idMatch = GUIEventModel.checkCongruentEventIDValueLists(eventIDAdapters.get(0).getProperty(), p);
					break;
				}
			}
			if(idMatch)
				return this;
		}
		else {
			if (normalPropertyTypeAdapters.containsAll(lIDProperties))
				return this;
		}

		List<GComponent> gChildren = getChildren();
		GComponent result = null;

		for (GComponent gChild : gChildren) {
			result = gChild.getFirstChild(lIDProperties);
			if (result != null)
				return result;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GXComponent#getEventList()
	 */
	@Override
	public List<GEvent> getEventList() {

		List<GEvent> retEvents = new ArrayList<GEvent>();
		String sClass = this.getClassVal();

		if (IGNORED_CLASS_EVENTS.contains(sClass))
			return retEvents;

		EventManager em = EventManager.getInstance();

		for (GEvent gEvent : em.getEvents()) {
			if (gEvent.isSupportedBy(this))
				retEvents.add(gEvent);
		}

		return retEvents;
	}

	/**
	 * jsaddler:
	 *
	 * Return the class value of this component, represented by
	 * its accessible role.
	 *
	 * This class was modified so that it would only depend on the
	 * Java Accessibility framework, rather than reflection, to get the
	 * class name. OOXComponent does something exactly similar to what has been done here.
	 * Instead of returning the name of the class, we return the name of the role.
	 *
	 * Preconditions: 	none
	 * Postconditions: 	returns null if component has not been initialized, or has no accessible context.
	 */
	@Override
	public String getClassVal() {
		if(component == null)
			return null;
		if(component.getAccessibleContext() == null)
			return null;
		// jsaddle: modified to use Java Accessibility framework.
		return component.getAccessibleContext().getAccessibleRole().toDisplayString();
		//return component.getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GXComponent#getTypeVal()
	 */
	@Override
	public String getTypeVal() {

		String retProperty;

		if (isTerminal())
			retProperty = GUITARConstants.TERMINAL;
		else
			retProperty = GUITARConstants.SYSTEM_INTERACTION;
		return retProperty;
	}

	/**
	 * Check if this component is a terminal widget
	 *
	 * <p>
	 *
	 * @return
	 */
	@Override
	public boolean isTerminal() {

		AccessibleContext aContext = component.getAccessibleContext();

		if (aContext == null)
			return false;

		AccessibleAction aAction = aContext.getAccessibleAction();

		if (aAction == null)
			return false;

		String sName = getTitle();

		List<AttributesTypeWrapper> termSig = JFCConstants.sTerminalWidgetSignature;
		for (AttributesTypeWrapper sign : termSig) {
			String titleVals = sign.getFirstValByName(JFCConstants.TITLE_TAG);

			if (titleVals == null)
				continue;

			if (titleVals.equalsIgnoreCase(sName))
				return true;

		}

		return false;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GComponent#isEnable()
	 */
	@Override
	public boolean isEnable() {
		try {
			Class<?>[] types = new Class[] {};
			Method method = component.getClass().getMethod("isEnabled", types);
			Object result = method.invoke(component, new Object[0]);

			if (result instanceof Boolean)
				return (Boolean) result;
			else
				return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check if the component is activated by its parent. For example, a tab
	 * panel is enable by a select item call from its parent JTabPanel
	 *
	 * <p>
	 *
	 * @return
	 */
	public boolean isActivatedByParent() {

		if (component instanceof Component) {
			Container parent = ((Component) component).getParent();
			if (parent instanceof JTabbedPane) {
				return true;
			}
		}
		return false;
	}



	/**
	 * Hierarchically search "this" component for matching widget. The
	 * search-item is provided as an image via a file name.
	 *
	 * @param sFilePath
	 *            Search item's file path.
	 */
	public GObject searchComponentByImage(String sFilePath)
			throws IOException {
		BufferedImage searchImage = null;

		try {
			searchImage = ImageIO.read(new File(sFilePath));
		} catch (IOException e) {
			// Image is expected
			GUITARLog.log.info("Could not read " + sFilePath);
			throw e;
		}

		return (GObject) searchComponentByImageInt(searchImage);
	}

	private JFCXComponent searchComponentByImageInt(BufferedImage searchImage) {
//		JFCXWindow jfcxWindow = (JFCXWindow) this.window;

		BufferedImage thisImage = null;
		if (!this.getComponent().isShowing()) {
			return null;
		}

		Dimension size = this.getComponent().getSize();
		thisImage = new BufferedImage(size.width, size.height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = thisImage.createGraphics();
		this.getComponent().paint(g2);
		g2.dispose();

		//boolean b = SikuliAPI.compare(thisImage, searchImage, 0.01);
		//if (b == true) {
		//	return this;
		//}

		List<GComponent> gChildren = getChildren();
		JFCXComponent result = null;

		for (GObject gChild : gChildren) {
			result = ((JFCXComponent) gChild)
					.searchComponentByImageInt(searchImage);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	public ComponentType extractProperties()
	{
		return extractCTHProperties();
	}

	public ComponentType extractCTHProperties()
	{
		ComponentType retComp;

		if (!hasChildren())
			retComp = factory.createComponentType();
		else {
			retComp = factory.createContainerType();
			ContentsType contents = factory.createContentsType();
			((ContainerType) retComp).setContents(contents);
		}

		ComponentTypeWrapper retCompAdapter = new ComponentTypeWrapper(retComp);

		// Title
		String sTitle = getTitle();
		retCompAdapter.addValueByName("Title", sTitle);

		String sTooltip = getTooltipText();
		if(!sTooltip.isEmpty())
			retCompAdapter.addValueByName(GUITARConstants.TOOLTIPTEXT_TAG_NAME, sTooltip);
		// Class
		String sClass = getClassVal();
		retCompAdapter.addValueByName(GUITARConstants.CLASS_TAG_NAME, sClass);

		// Type
		String sType = getTypeVal();
		retCompAdapter.addValueByName(GUITARConstants.TYPE_TAG_NAME, sType);
		// x
		int x = getX();
		retCompAdapter.addValueByName(GUITARConstants.X_TAG_NAME, Integer.toString(x));
		// y
		int y = getY();
		retCompAdapter.addValueByName(GUITARConstants.Y_TAG_NAME, Integer.toString(y));

		if(sClass != null) {
			List<String> theEids = getMultiEventID();
			for(String id : theEids)
				retCompAdapter.addValueByName(GUITARConstants.CTH_EVENT_ID_NAME, id);
		}
		else
			retCompAdapter.addValueByName(GUITARConstants.CTH_EVENT_ID_NAME, JavaTestInteractions.hasNoID);

		// Events
		List<GEvent> lEvents = getEventList();
		for (GEvent event : lEvents)
			retCompAdapter.addValueByName(GUITARConstants.EVENT_TAG_NAME, event
					.getClass().getName());

		// Add other GUI Properties
		retComp = retCompAdapter.getDComponentType();

		AttributesType attributes = retComp.getAttributes();
		List<PropertyType> lProperties = attributes.getProperty();
		List<PropertyType> lGUIProperties = getGUIProperties();

		// Update list
		if (lGUIProperties != null) {
			gpLoop:
			for(PropertyType gp : lGUIProperties) {
				// if a named property doesn't already belong here,
				// add it to the new list of properties.
				Iterator<PropertyType> lIt = new ArrayList<PropertyType>(lProperties).iterator();
				while(lIt.hasNext())
					if(lIt.next().getName().equals(gp.getName()))
						continue gpLoop;
				lProperties.add(gp);
			}

		}
		/*
		 * if (lGUIProperties != null) {
			lProperties.addAll(lGUIProperties);
		}
		 */
		// return all attributes in a ComponentType object.
		attributes.setProperty(lProperties);

		retComp.setAttributes(attributes);

		return retComp;
	}

	@Override
	public List<PropertyType> getIDProperties() {
		ComponentType component = this.extractProperties();
		if (component == null) {
			return new ArrayList<PropertyType>();
		}

		List<PropertyType> retIDProperties = new ArrayList<PropertyType>();

		AttributesType attributes = component.getAttributes();
		List<PropertyType> lProperties = attributes.getProperty();

		for (PropertyType p : lProperties) {
			if (JFCConstants.ID_PROPERTIES.contains(p.getName()))
				retIDProperties.add(p);
		}
		return retIDProperties;
	}

	/**
	 * Returns the CogToolHelper event IDs related to this JFCXComponent if it can support multiple id's.
	 * This method can only handle roles that support more than one event ID.
	 * Relies on HeadTable.allInteractions to work properly.
	 *
	 * Preconditions: 	HeadTable.allInteractions object has been instantiated
	 * 					component != null
	 * Postconditions:	If component is a multi-type component capable of being represented as up to n types,
	 * 					  0 - n strings are returned in a list corresponding to the names assigned to this component in the order
	 * 					  "clickableId" then "typableId OR cselectableId".
	 * 					One or both strings will be equivalent to JavaTestInteractions.hasNoId
	 * 					  if the component has no clickable and/or typeable/selectable eventId or is not a multi-type component.
	 */
	public List<String> getMultiEventID()
	{
		if(!storedEventIDs.isEmpty())
			return storedEventIDs;

		AccessibleRole myRole = component.getAccessibleContext().getAccessibleRole();
		JavaTestInteractions myJTI = null;
		boolean jtiFound = false;
		for(JavaTestInteractions jti : HeadTable.allInteractions) {
			for(Window w : jti.getWindowsScanned())
				if(JavaTestInteractions.windowTitlesAreSame(window.getTitle(), JFCXWindow.getGUITARTitle(w))) {
					myJTI = jti;
					jtiFound = true;
				}
		}
		if(!jtiFound) {
			myJTI = new JavaTestInteractions();
			myJTI.setCurrentWindow(((JFCXWindow)window).getWindow());
			HeadTable.allInteractions.add(myJTI);
		}
		// can replace by method findJTI() and the code above to create JTI.
		if(nowRipping) {
			// since we're ripping, let's just get the names we need right now
			String newIds = myJTI.assignNameByRole(component, JavaTestInteractions.W_HOVER);
			String[] ordered = GUIEventModel.getOrderedIds(myRole, newIds, JavaTestInteractions.eventId_separator);
			storedEventIDs.addAll(Arrays.asList(ordered));
			return storedEventIDs;
		}
		else {
			// since we're not ripping, we have to look up the id amongst the events
			String newIds = "";
			String nextAct;
			for(ActionClass actRole : GUIEventModel.getSupportedActionsFor(myRole)) {
				// lookup the id
				if(myRole.equals(AccessibleRole.LIST))
					nextAct = myJTI.lookupLargeObjectID(component, window.getTitle(), actRole.actionName);
				else
					nextAct = myJTI.lookupID(component, window.getTitle(), actRole.actionName);
				newIds += newIds.isEmpty() ? nextAct : JavaTestInteractions.eventId_separator + nextAct;
			}
			String[] ordered = GUIEventModel.getOrderedIds(myRole, newIds, JavaTestInteractions.eventId_separator);
			storedEventIDs.addAll(Arrays.asList(ordered));
			return storedEventIDs;
		}
	}

	/**
	 * Returns the CogToolHelper event ID related to this JFCXComponent. Relies on HeadTable.allInteractions
	 * to work properly.
	 *
	 * Preconditions: 	HeadTable.allInteractions object has been instantiated
	 * 					component != null
	 * Postconditions:	CogToolHelper EventID is returned from this function as a string.
	 */
	public String getCTHEventID()
	{
		if(!storedCTHEventId.isEmpty())
			return storedCTHEventId;
		if(nowRipping) {
			boolean jtiFound = false;
			// select window
			JavaTestInteractions myJTI = null;
			for(JavaTestInteractions jti : HeadTable.allInteractions)
				for(Window w : jti.getWindowsScanned()) {
					GWindow gWin = new JFCXWindow(w);
					if(window.getTitle().equals(gWin.getTitle())) {
						myJTI = jti;
						jtiFound = true;
					}
				}
			if(!jtiFound) {
				myJTI = new JavaTestInteractions();
				myJTI.setCurrentWindow(((JFCXWindow)window).getWindow());
				HeadTable.allInteractions.add(myJTI);
			}
			storedCTHEventId = myJTI.assignNameByRole(component, JavaTestInteractions.NO_HOVER);
			return storedCTHEventId;
//			return myJTI.assignNameByRole(component);
		}
		else {
			// obtain the action.
			String roleAsAction;
			if(!(component instanceof Accessible))
				return JavaTestInteractions.hasNoID;

			AccessibleRole myRole = component.getAccessibleContext().getAccessibleRole();
			if(myRole.equals(AccessibleRole.TEXT))
				roleAsAction = ActionClass.TEXT.actionName;
			else if(
					myRole.equals(AccessibleRole.PUSH_BUTTON) ||
					myRole.equals(AccessibleRole.RADIO_BUTTON) ||
					myRole.equals(AccessibleRole.MENU) ||
					myRole.equals(AccessibleRole.MENU_ITEM) ||
					myRole.equals(AccessibleRole.CHECK_BOX) ||
					myRole.equals(AccessibleRole.TOGGLE_BUTTON)) {
				roleAsAction = ActionClass.ACTION.actionName;
			}
			else if(myRole.equals(AccessibleRole.COMBO_BOX))
				roleAsAction = ActionClass.PARSELECT.actionName;

			else if(myRole.equals(AccessibleRole.PAGE_TAB_LIST)) {
				if(hasChildren())
					roleAsAction = ActionClass.PARSELECT.actionName; // selectable
				else
					roleAsAction = ActionClass.ACTION.actionName; // clickable
			}

			else if(myRole.equals(AccessibleRole.PANEL))
				roleAsAction = ActionClass.ACTION.actionName;

			else if(myRole.equals(AccessibleRole.LIST))
				roleAsAction = ActionClass.SELECTION.actionName;
			else
				roleAsAction = "no assigned role";

			String CTHEventID;
			JavaTestInteractions myJTI = null;

			boolean jtiFound = false;
			// select window
			if(HeadTable.allInteractions == null)
				HeadTable.allInteractions = new ArrayList<JavaTestInteractions>();
			for(JavaTestInteractions jti : HeadTable.allInteractions)
				for(Window w : jti.getWindowsScanned()) {
					GWindow gWin = new JFCXWindow(w);
					if(window.getTitle().equals(gWin.getTitle())) {
						myJTI = jti;
						jtiFound = true;
					}
				}

			boolean idIsNonexistent = roleAsAction.equals("no assigned role") || !jtiFound;

			if(idIsNonexistent)
				return JavaTestInteractions.hasNoID;

			 // select correct id lookup method
			if(myRole.equals(AccessibleRole.LIST))
				CTHEventID = myJTI.lookupLargeObjectID(component, window.getTitle(), roleAsAction);
			else
				CTHEventID = myJTI.lookupID(component, window.getTitle(), roleAsAction);
			storedCTHEventId = CTHEventID;
			return CTHEventID;
		}
	}

	/**
	 * Determines whether the component has listeners attached to it that are typically
	 * used in detecting input for the role specified by treatAs. For instance, if
	 * treatAs.equals("button"), then the component is checked for mouseListeners.
	 * Returns false if the component does not contain listeners that correspond to this role,
	 * and otherwise return true.
	 *
	 * Preconditions: 	component is not null
	 * 					treatAs matches the strings "button" and "text".
	 * Postconditions: 	If the component has listeners that likely correspond to the role specified by treatAs
	 * 					true is returned. If component has no listeners that correspond to this role, return false.
	 * @return
	 */
	public static boolean hasListeners(Component component, String treatAs)
	{
		if(treatAs.toLowerCase().equals("button")) {
			MouseListener[] mouseListeners = component.getMouseListeners();
			if(mouseListeners.length == 0)
				return false;
			return true;
		}
		else if(treatAs.toLowerCase().equals("textbox")) {
			KeyListener[] keyListeners = component.getKeyListeners();
			if(keyListeners.length == 0)
				return false;
			return true;
		}
		else
			throw new RuntimeException("JavaCaptureMonitor: hasListeners method was passed an invalid listenersUse parameter: " + treatAs);
	}

	//
	// IDENTITY METHODS
	//
	/**
	 * Returns true if this component is a combo box.
	 * Preconditions: 	(none)
	 * Postconditions: 	true is returned if component is a combo box. False is returned otherwise.
	 */
	/* (non-Javadoc)
	 * @see edu.umd.cs.guitar.model.GComponent#isCombo()
	 */
	@Override
	public boolean isCombo()
	{
		String classVal = getClassVal();
		if(classVal == null)
			return false;

		if(classVal.equals(AccessibleRole.COMBO_BOX.toDisplayString()))
			return true;
		return false;
	}

	/**
	 * jsaddler: Returns true if this component is a menu.
	 *
	 *  Preconditions: (none)
	 *  Postconditions: true is returned if component is a menu. False is returned otherwise.
	 */
	@Override
	public boolean isMenu()
	{
		String classVal = getClassVal();
		if(classVal == null)
			return false;
		if(classVal.equals(AccessibleRole.MENU.toDisplayString()))
			return true;
		return false;
	}


	/**
	 * jsaddler: Returns true if this component is a list.
	 *
	 * Preconditions: (none)
	 * Postconditions: True is returned if component is a list. False is returned otherwise.
	 */
	@Override
	public boolean isList()
	{
		// check role
		String classVal = getClassVal();
		if(classVal.equals(AccessibleRole.LIST.toDisplayString()))
			return true;
		return false;
	}

	/**
	 * jsaddler: Returns true if this component is a separator.
	 *
	 * Preconditions: (none)
	 * Postconditions: True is returned if component is a separator. False is returned otherwise.
	 */
	@Override
	public boolean isSeparator()
	{
		// check role
		String classVal = getClassVal();
		if(classVal.equals(AccessibleRole.SEPARATOR.toDisplayString()))
			return true;
		return false;
	}

	//
	// STATE METHODS
	//
	@Override
	/**
	 * jsaddler:
	 * This method returns true if component is currently "visible".
	 *
	 * Preconditions: 	(none)
	 * Postconditions: 	The visible state of the component is checked. If it is visible,
	 * 					true is returned. False otherwise.
	 */
	public boolean isVisible()
	{
		// check for null information
		if(component == null)
			return false;
		if(component.getAccessibleContext() == null)
			return false;
		AccessibleStateSet componentStates =
				component.getAccessibleContext().getAccessibleStateSet();

		// check for visibility
		if(componentStates.contains(AccessibleState.VISIBLE))
			return true;
		return false;
	}

	public static boolean isVisible(Accessible javaComponent)
	{
		if(javaComponent != null) {
			AccessibleStateSet componentStates = javaComponent.getAccessibleContext().getAccessibleStateSet();
			if(componentStates.contains(AccessibleState.VISIBLE))
				return true;
		}
		return false;
	}

	/**
	 * jsaddler:
	 * This method returns true if the component is a check box or toggle button.
	 * False otherwise.
	 *
	 * Preconditions: 	(none)
	 * Postconditions: 	If component is a check box or toggle button, this method returns true.
	 * 					False is returned otherwise.
	 */
	@Override
	public boolean isToggleable() {
		if(component == null)
			return false;
		if(component.getAccessibleContext() ==  null)
			return false;
		AccessibleRole componentRole = component.getAccessibleContext().getAccessibleRole();
		if( componentRole.equals(AccessibleRole.TOGGLE_BUTTON)
		|| 	componentRole.equals(AccessibleRole.CHECK_BOX)
		||  componentRole.equals(AccessibleRole.PAGE_TAB))
			return true;
		return false;
	}

	public static boolean isToggleable(Accessible component) {
		if(component == null)
			return false;

		if(component.getAccessibleContext() == null)
			return false;

		AccessibleRole componentRole = component.getAccessibleContext().getAccessibleRole();
		if( componentRole.equals(AccessibleRole.TOGGLE_BUTTON)
		||	componentRole.equals(AccessibleRole.CHECK_BOX)
		||	componentRole.equals(AccessibleRole.PAGE_TAB)) {
			return true;
		}
		return false;
	}
	/**
	 * jsaddler:
	 * Returns true if this object is currently checked. False otherwise.
	 *
	 * Preconditions: (none)
	 * Postconditions: if object's current state can be described as "checked", true is returned.
	 * 				   false otherwise.
	 */
	@Override
	public boolean isChecked()
	{
		// check for null information.
		if(component == null)
			return false;
		if(component.getAccessibleContext() == null)
			return false;

		// if this object is currently "checked", return true.
		AccessibleStateSet componentStates =
				component.getAccessibleContext().getAccessibleStateSet();
		if(componentStates.contains(AccessibleState.CHECKED))
			return true;
		return false;
	}


	public boolean isSelected()
	{
		// check for null information.
		if(component == null)
			return false;
		if(component.getAccessibleContext() == null)
			return false;

		// if this object is currently "checked", return true.
		AccessibleStateSet componentStates =
				component.getAccessibleContext().getAccessibleStateSet();
		if(componentStates.contains(AccessibleState.SELECTED))
			return true;

		return false;
	}

	/**
	 * jsaddler: Returns the name of the child item of component that has been selected.
	 * Preconditions: 	(none)
	 * Postconditions: 	if component or its accessible context is null, returns empty string.
	 * 					otherwise, if the component has at least one selected component,
	 * 					the first selected component found in order among component's accessible
	 * 					children is returned.
	 */
	@Override
	public String getSelectedItem()
	{
		// check for nullity
		if(component == null)
			return "";
		AccessibleContext aContext = component.getAccessibleContext();
		if(aContext == null)
			return "";

		if(isCombo()) {
			AccessibleSelection theSelection = aContext.getAccessibleSelection();
			Accessible child = theSelection.getAccessibleSelection(0);
			if(child != null)
				return child.getAccessibleContext().getAccessibleName();
			else
				return "";
		}
		// get the accessible name of the selected child.

		int childCount = aContext.getAccessibleChildrenCount();
		for (int i = 0; i < childCount; i++) {
			try {
				Accessible child = aContext.getAccessibleChild(i);
				AccessibleStateSet childState = child.getAccessibleContext().getAccessibleStateSet();
				if(childState.contains(AccessibleState.SELECTED))
					return child.getAccessibleContext().getAccessibleName();
			} catch(IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}

		return "";
	}

	/**
	 * Returns the x-component of component's location
	 * on the screen.
	 *
	 * Preconditions: 	(none)
	 * Postconditions: 	If component is null, 0 is returned.
	 * 					Otherwise, the x component of the location of this
	 * 					component, relative to its parent, is returned.
	 */
	@Override
	public int getXRelative()
	{
		if(component==null)
			return 0;
		return component.getX();
	}


	/**
	 * Returns the y-component of component's location on the screen.
	 *
	 * Preconditions: 	(none)
	 * Postconditions:	If component is null, 0 is returned.
	 * 					Otherwise, the y component of the location of this
	 * 					component, relative to its parent, is returned.
	 */
	@Override
	public int getYRelative()
	{
		if(component==null)
			return 0;
		return component.getY();
	}

	@Override
	/**
	 * jsaddler:
	 * Returns the width of this component.
	 *
	 * Preconditions: 	(none)
	 * Postconditions: 	Width of this component is returned.
	 * 					0 returned if component is null.
	 */
	public int getWidth()
	{
		if(component == null)
			return 0;
		return component.getWidth();
	}


	@Override
	/**
	 * jsaddler:
	 * Returns the height of this component.
	 *
	 * Preconditions: 	(none)
	 * Postconditions:  Height of this component is returned.
	 * 					0 is returned if component is null.
	 */
	public int getHeight()
	{
		if(component == null)
			return 0;
		return component.getHeight();
	}

	@Override
	public int getX() {
		Component pointer = component;

		if (pointer == null || pointer instanceof Window)
			return 0;

		int x = 0;

		while (!(pointer instanceof Window)) {
			x += pointer.getX();
			pointer = pointer.getParent();
			if (pointer == null)
				break;
		}

		// account for being in a dialog box.
		if(pointer != null)
			if(pointer instanceof Dialog) {
				try {
					x += pointer.getLocationOnScreen().x;
				}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
					+ "Component might not be showing on the screen.", e);
				}
			}
			// account for border insets
			else {
				Window w = ((Window)pointer);
				Insets insets = w.getInsets();
				x += insets.left;



				try{x += pointer.getLocationOnScreen().x;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
					+ "Component might not be showing on the screen.", e);
				}

				Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(w.getGraphicsConfiguration());
				x -= screenInsets.left;
			}


		return x;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.model.GComponent#getY()
	 */
	@Override
	public int getY()
	{
		Component pointer = component;

		if (pointer == null || pointer instanceof Window)
			return 0;

		int y = 0;

		while (!(pointer instanceof Window)) {
			y += pointer.getY();
			pointer = pointer.getParent();
			if (pointer == null)
				break;
		}

		// account for being in a dialog box
		if(pointer != null)
			if(pointer instanceof Dialog) {
				try {y += pointer.getLocationOnScreen().y;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
						+ "Component might not be showing on the screen.", e);
				}
			}
			// account for border insets
			else {
				Window w = (Window)pointer;
				Insets insets = w.getInsets();
				y += insets.top;



				try {y += pointer.getLocationOnScreen().y;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
					+ "Component might not be showing on the screen.", e);
				}

				Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(w.getGraphicsConfiguration());
				y -= screenInsets.top;
			}

		return y;
	}

	public static int getGUITAROffsetXInWindow(Component javaComponent)
	{
		Component pointer = javaComponent;

		if(pointer == null || pointer instanceof Window)
			return 0;

		int x = 0;

		while(!(pointer instanceof Window)) {
			x += pointer.getX();
			pointer = pointer.getParent();
			if (pointer == null)
				break;
		}
		if(pointer != null && !(pointer instanceof Dialog)){
			Window w = ((Window)pointer);
			Insets insets = w.getInsets();
			x += insets.left;
		}
		return x;
	}

	public static int getGUITAROffsetYInWindow(Component javaComponent)
	{
		Component pointer = javaComponent;
		if (pointer == null || pointer instanceof Window)
			return 0;

		int y = 0;

		while (!(pointer instanceof Window)) {
			y += pointer.getY();
			pointer = pointer.getParent();
			if (pointer == null)
				break;
		}

		if(pointer != null && !(pointer instanceof Dialog)) {
			Window w = (Window)pointer;
			Insets insets = w.getInsets();
			y += insets.top;
		}
		return y;
	}
	/**
	 * Returns the offset x value of this component: the 'X' value of this component + relative position
	 * of the component within all its parental containers.
	 *
	 * Preconditions: 	(none)
	 * Postconditions:	The offset x value of javaComponent is returned.
	 */
	public static int getGUITAROffsetX(Component javaComponent)
	{
		Component pointer = javaComponent;

		if (pointer == null || pointer instanceof Window)
			return 0;

		int x = 0;

		while (!(pointer instanceof Window)) {
			x += pointer.getX();
			pointer = pointer.getParent();
			if (pointer == null)
				break;
		}


		// account for being in a dialog box
		if(pointer != null)
			if(pointer instanceof Dialog) {
				try {x += pointer.getLocationOnScreen().x;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
							+ "Component might not be showing on the screen.");
				}
			}
			// account for border insets
			else {
				Window w = ((Window)pointer);
				Insets insets = w.getInsets();
				x += insets.left;

				try{x += pointer.getLocationOnScreen().x;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
					+ "Component might not be showing on the screen.", e);
				}

				Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(w.getGraphicsConfiguration());
				x -= screenInsets.left;
			}
		return x;
	}

	/**
	 * Returns the offset y value of this component: the 'Y' value of this component + relative position
	 * of the component within all its parental containers.
	 *
	 * Preconditions: 	(none)
	 * Postconditions:	The offset y value of javaComponent is returned.
	 */
	public static int getGUITAROffsetY(Component javaComponent)
	{
		Component pointer = javaComponent;

		if (pointer == null || pointer instanceof Window)
			return 0;

		int y = 0;

		while (!(pointer instanceof Window)) {
			y += pointer.getY();
			pointer = pointer.getParent();
			if (pointer == null)
				break;
		}

		// account for being in a dialog box
		if(pointer != null)
			if(pointer instanceof Dialog) {
				try {y += pointer.getLocationOnScreen().y;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
					+ "Component might not be showing on the screen.", e);
				}
			}
			// account for border insets
			else {
				Window w = (Window)pointer;
				Insets insets = w.getInsets();
				y += insets.top;



				try{y += pointer.getLocationOnScreen().y;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
					+ "Component might not be showing on the screen.", e);
				}

				Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(w.getGraphicsConfiguration());
				y -= screenInsets.top;
			}
		return y;
	}


	/**
	 * Returns the offset y value of this accessible using parent accessibleComponents:
	 * the 'Y' value of this component + relative position
	 * of the component within all its parental containers.
	 */
	public static int getGUITAROffsetX(Accessible javaComponent)
	{
		Accessible pointer = javaComponent;

		if(pointer == null || pointer instanceof Window)
			return 0;

		int x = 0;

		while(!(pointer instanceof Window)) {
			x += pointer.getAccessibleContext().getAccessibleComponent().getBounds().x;
			pointer = pointer.getAccessibleContext().getAccessibleParent();
			if(pointer == null)
				break;
		}

		// account for being in a dialog box
		if(pointer != null)
			if(pointer instanceof Dialog) {
				try {x += pointer.getAccessibleContext().getAccessibleComponent().getLocationOnScreen().x;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
							+ "Component might not be showing on the screen.");
				}
			}
			// account for border insets
			else {
				Window w = ((Window)pointer);
				Insets insets = w.getInsets();
				x += insets.left;

				try {x += pointer.getAccessibleContext().getAccessibleComponent().getLocationOnScreen().x;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
							+ "Component might not be showing on the screen.");
				}

				Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(w.getGraphicsConfiguration());
				x -= screenInsets.left;
			}
		return x;
	}

	/**
	 * Returns the offset y value of this accessible using parent accessibleComponents:
	 * the 'Y' value of this component + relative position
	 * of the component within all its parental containers. Accounts for components
	 * shown in dialog boxes, and also frames which may have insets
	 *
	 * Preconditions: none
	 * Postconditions:
	 */
	public static int getGUITAROffsetY(Accessible javaComponent)
	{
		Accessible pointer = javaComponent;

		if(pointer == null || pointer instanceof Window)
			return 0;

		int y = 0;

		while(!(pointer instanceof Window)) {
			y += pointer.getAccessibleContext().getAccessibleComponent().getBounds().y;
			pointer = pointer.getAccessibleContext().getAccessibleParent();
			if(pointer == null)
				break;
		}

		// account for being in a dialog box.
		if(pointer != null)
			if(pointer instanceof Dialog) {

				try {y += pointer.getAccessibleContext().getAccessibleComponent().getLocationOnScreen().y;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
						+ "Component might not be showing on the screen.");
				}
			}
			// account for border insets
			else {
				Window w = (Window)pointer;
				Insets insets = w.getInsets();
				y += insets.top;

				Insets screenInsets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(w.getGraphicsConfiguration());
				y -= screenInsets.top;
				try {y += pointer.getAccessibleContext().getAccessibleComponent().getLocationOnScreen().y;}
				catch(IllegalComponentStateException e) {
					throw new RuntimeException("Could not retrieve location of component.\n"
							+ "Component might not be showing on the screen.");
				}
			}
		return y;
	}
	/**
	 * Returns the rectangle that bounds the component represented by the accessible provided.
	 */
	public static Rectangle accessibleBounds(Accessible javaAcc)
	{
		AccessibleComponent jComp = javaAcc.getAccessibleContext().getAccessibleComponent();
		return jComp.getBounds();
	}

	/**
	 * Attempts to locate the combo box in the parent hierarchy of this widget.
	 */
	public static boolean isInCombo(Accessible first)
	{
		AccessibleRole firstRole = first.getAccessibleContext().getAccessibleRole();
		if(firstRole.equals(AccessibleRole.COMBO_BOX))
			return false; // combo boxes cannot be in combo boxes.
		if(firstRole.equals(AccessibleRole.WINDOW))
			return false; // windows cannot be in combo boxes.

		Accessible upstream = first.getAccessibleContext().getAccessibleParent();
		while(upstream != null) {
			if(upstream.getAccessibleContext().getAccessibleRole().equals(AccessibleRole.COMBO_BOX))
				return true;
			upstream = upstream.getAccessibleContext().getAccessibleParent();
		}
		return false;
	}
}
