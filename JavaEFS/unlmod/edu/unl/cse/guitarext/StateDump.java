package edu.unl.cse.guitarext;

import java.util.*;
import java.io.*;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;


/**
 * This class was made to dump the properties of attributes types provided to an xml file.
 * 
 * @author jsaddle
 */
public class StateDump extends File {
	public static enum PropertyWrite {WIDGET};
	private List<PropertyType> propertiesCollection;
	private List<AttributesType> attributesCollection;
	private ObjectFactory newXMLObjects;
	private PropertyWrite propertyWriteFormat;
	
	/**
	 * Major shortcuts have been taken here to make the process 
	 * of creating a state dump easy and painless. 
	 * 
	 * 	Preconditions: 	guitarProperties is not null and contains the properties we wish to write.
	 * 	Postconditions: A dump file of all the property types defined is written to an xml file with
	 * 					the filename filename.
	 * 
	 * @param guitarProperties
	 * @param filename
	 * @throws IOException
	 */
	@SafeVarargs
	public StateDump(String filename, List<PropertyType>... guitarProperties)
	{
		super(filename);
		propertiesCollection = new LinkedList<PropertyType>();
		for(List<PropertyType> p : guitarProperties)	
			propertiesCollection.addAll(p);
		attributesCollection = new LinkedList<AttributesType>();
		newXMLObjects = new ObjectFactory();
	}
	
	public void setPropertyWriteFormat(PropertyWrite theFormat)
	{
		this.propertyWriteFormat = theFormat;
	}
	
	public PropertyWrite getPropertyWriteFormat()
	{
		return propertyWriteFormat;
	}
	
	/**
	 * Add extra properties to those 
	 * @param extras
	 */
	public void addExtraProperties(List<PropertyType> extras)
	{
		propertiesCollection.addAll(extras);
	}
	
	public void addExtraAttributes(AttributesType extraAtt)
	{
		attributesCollection.add(extraAtt);
	}
	public void writeContentToFile() throws IOException
	{
		boolean trouble;
		delete(); // delete the old file if one exists.
		trouble = !createNewFile(); // create a new one. 
		if(trouble)
			throw new IOException("StateDump: Cannot create new GUI State file.");
		
		// try to write all properties to the file output stream. 
		try (FileOutputStream fos = new FileOutputStream(this, true)) 
		{
			XMLHandler handler = new XMLHandler();
			
			// attributes first, then properties.
			for(AttributesType a : attributesCollection) 
				handler.writeObjToFileNoClose(a, fos);
			for(PropertyType p : propertiesCollection)
				handler.writeObjToFileNoClose(p, fos);
		} 
	}
	
	/*
	 * new method
	 * 
	 * AttributesType modAttType = new AttributesType();
	 * List<PropertyType> newProperties;
	 *
	 */
	
	/**
	 * Write all ID Properties to a file. 
	 * @throws IOException
	 */
	public void writeDefaultIDPropertiesToFile() throws IOException
	{
		boolean trouble;
		delete(); // delete the old file if one exists.
		trouble = !createNewFile(); // create a new one. 
		if(trouble)
			throw new IOException("StateDump: Cannot create new GUI state file.");
		
		// try to write all properties to the file output stream. 
		try (FileOutputStream fos = new FileOutputStream(this, true)) 
		{
			XMLHandler handler = new XMLHandler();
			// attributes first, then properties. 
			for(AttributesType a : attributesCollection) {
				List<PropertyType> properties = a.getProperty();
				for(PropertyType p : properties) 
					if(p.getName().equals(GUITARConstants.ID_TAG_NAME)) 
						handler.writeObjToFileNoClose(p, fos);										
			}
			for(PropertyType p : propertiesCollection)
				if(p.getName().equals(GUITARConstants.ID_TAG_NAME)) 
					handler.writeObjToFileNoClose(p, fos);					
		} 
		
	}
	
	public void writeWidgetPropertiesToFile() throws IOException
	{
		boolean trouble;
		delete(); // delete the old file if one exists.
		trouble = !createNewFile(); // create a new one. 
		if(trouble)
			throw new IOException("StateDump: Cannot create new GUI state file.");
		
		// try to write all properties to the file output stream. 
		try (FileOutputStream fos = new FileOutputStream(this, true)) 
		{
			XMLHandler handler = new XMLHandler();
			// attributes first, then properties.
			AttributesType customAtt;
			
			List<PropertyType> filterList;
			List<PropertyType> currentProperties;
			for(AttributesType a : attributesCollection) {
				if(a != null) {
					customAtt = newXMLObjects.createAttributesType();
					filterList = new LinkedList<PropertyType>();
					currentProperties = a.getProperty();
					boolean gotId, gotCls, gotInW, gotTit;
					gotId = gotCls = gotInW = gotTit = false;
					for(PropertyType p : currentProperties) 
						if(p.getName().equals(GUITARConstants.ID_TAG_NAME) && !gotId) { 
							filterList.add(p);	
							gotId = true;
						}
						else if(p.getName().equals(GUITARConstants.CLASS_TAG_NAME) && !gotCls) { 
							filterList.add(p);
							gotCls = true;
						}
						else if(p.getName().equals(GUITARConstants.INWINDOW_TAG_NAME) && !gotInW) {
							filterList.add(p);
							gotInW = true;
						}
						else if(p.getName().equals(GUITARConstants.TITLE_TAG_NAME) && !gotTit) {
							filterList.add(p);
							gotTit = true;
						}
					customAtt.setProperty(filterList);
					handler.writeObjToFileNoClose(customAtt, fos);
				}
			}
			for(PropertyType p : propertiesCollection)
				if(p.getName().equals(GUITARConstants.ID_TAG_NAME)) 
					handler.writeObjToFileNoClose(p, fos);		
				else if(p.getName().equals(GUITARConstants.CTH_EVENT_ID_NAME))
					handler.writeObjToFileNoClose(p, fos);
				else if(p.getName().equals(GUITARConstants.EVENT_TAG_NAME)) {
					PropertyType newP = newXMLObjects.createPropertyType();
					newP.setName("Action");			
					handler.writeObjToFileNoClose(newP, fos);
				}
		} 	
	}
	public void writeTitlePropertiesToFile() throws IOException
	{
		boolean trouble;
		delete(); // delete the old file if one exists.
		trouble = !createNewFile(); // create a new one. 
		if(trouble)
			throw new IOException("StateDump: Cannot create new GUI state file.");
		
		// try to write all properties to the file output stream. 
		try (FileOutputStream fos = new FileOutputStream(this, true)) 
		{
			XMLHandler handler = new XMLHandler();
			// attributes first, then properties.
			for(AttributesType a : attributesCollection) {
				List<PropertyType> properties = a.getProperty();
				for(PropertyType p : properties) 
					if(p.getName().equals(GUITARConstants.TITLE_TAG_NAME)) 
						handler.writeObjToFileNoClose(p, fos);										
			}
			for(PropertyType p : propertiesCollection)
				if(p.getName().equals(GUITARConstants.TITLE_TAG_NAME)) 
					handler.writeObjToFileNoClose(p, fos);					
		} 
	}
	
}
