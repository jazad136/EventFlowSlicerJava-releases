package edu.umd.cs.guitar.model.wrapper;

import java.awt.Component;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;


/**
 * Extract properties of a Java Component
 * @author Jonathan Saddler
 *
 */
public class JavaExtractFilteredGUIProperties {
	
	static ObjectFactory schemaFactory = new ObjectFactory();
	
	public static ComponentType extractComponentType(Component component)
	{
		boolean dontParse = component == null 
				|| component.getAccessibleContext() == null;
		if(dontParse)
			return null;
		
		
		if(component.getAccessibleContext().getAccessibleChildrenCount() == 0
				&& !(component instanceof java.awt.Container)) {
			ComponentType toReturn;
			toReturn = schemaFactory.createComponentType();
			toReturn.setAttributes(extractAttributes(component));
			return toReturn;
		}
		else {
			ContainerType toReturn;
			toReturn = schemaFactory.createContainerType();
			toReturn.setContents(schemaFactory.createContentsType());
			toReturn.setAttributes(extractAttributes(component));
			return toReturn;
		}
	}
	
	public static AttributesType extractAttributes(Component component)
	{
		AttributesType toReturn = schemaFactory.createAttributesType();
		toReturn.getProperty().addAll(extractGUITARProperties(component));
		toReturn.getProperty().addAll(extractGUIProperties(component));
		return toReturn;
	}

	private static List<PropertyType> extractGUIProperties(Component component) 
	{
		List<PropertyType> toReturn = new LinkedList<PropertyType>();
		toReturn.addAll(JFCTypeToProperties.getValueState(component));
		toReturn.addAll(JFCTypeToProperties.getComponentStateSet(component));
		toReturn.addAll(JFCTypeToProperties.getTextState(component));
		toReturn.addAll(JFCTypeToProperties.getSelectionState(component));
		return toReturn;
	}

	private static List<PropertyType> extractGUITARProperties(Component component) 
	{
		List<PropertyType> toReturn = new LinkedList<PropertyType>();
		toReturn.addAll(JFCTTPGuitarState.getGUITARComponentState(component));
		return toReturn;
	}
}
