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
 *     Jonathan A. Saddler - modifications
 *******************************************************************************/

package edu.unl.cse.efs.guitarplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.guitar.model.GIDGenerator;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.GUIType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUITypeWrapper;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.umd.cs.guitar.util.AppUtil;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.guitarext.StateDump;

/**
 * Default ID generator for JFC application
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao N. Nguyen </a>
 * 
 */
public class JFCIDGeneratorEFS implements GIDGenerator
{
   static JFCIDGeneratorEFS instance = null;
   static ObjectFactory factory = new ObjectFactory();
   static final int prime = 31;
   public static StateDump outputNamesFile;
   public final boolean writeIds; 
   
   public static JFCIDGeneratorEFS getInstance()
   {
      if (instance == null) 
         instance = new JFCIDGeneratorEFS();

      return instance;
   }

   private JFCIDGeneratorEFS() 
   {
	   if(JFCRipperConfigurationEFS.NAMES_FILE == null)
		   writeIds = false;
	   else {
		   outputNamesFile = new StateDump(JFCRipperConfigurationEFS.NAMES_FILE);
		   writeIds = true;
	   }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * edu.umd.cs.guitar.model.GIDGenerator#genID(edu.umd.cs.guitar.model.data
    * .GUIStructure)
    */
   @Override
   public void generateID(GUIStructure gs) {
      for (GUIType gui : gs.getGUI()) {
         generateGUIID(gui);
      }
   }

   /**
    * @param gui
    */
   private void generateGUIID(GUIType gui)
   {
      ContainerType container = gui.getContainer();

      if (container == null) {
         return;
      }
      
      for(PropertyType p : gui.getWindow().getAttributes().getProperty()) {
    	  
      }
      long windowHashCode = getWindowHashCode(gui);

      List<ComponentType> subComponentList = container.getContents()
            .getWidgetOrContainer();

      if (subComponentList == null) {
         return;
      }

      for (ComponentType subComponent : subComponentList) {
         // Generate first container hash code specially to ignore window
         // susceptible to title change

         AttributesType attributes = subComponent.getAttributes();
         long hashcode = windowHashCode;
         String gotTitle = "";
         if (attributes != null) {
            String sID = GUITARConstants.COMPONENT_ID_PREFIX + hashcode;

            List<PropertyType> lProperty = new ArrayList<PropertyType>();
            
            for (PropertyType p : attributes.getProperty()) {
               if (!GUITARConstants.ID_TAG_NAME.equals(p.getName())) {
                  lProperty.add(p);
               }
               if (GUITARConstants.TITLE_TAG_NAME.equals(p.getName())) {
                  GUITARLog.log.debug("generateGUIID: " + 
                                      "[" + sID + "] " + p.getValue() +
                                      "windowHashCode " + windowHashCode);
                  gotTitle = p.getValue().get(0);
               }
            }

            PropertyType property = factory.createPropertyType();
            property.setName(GUITARConstants.ID_TAG_NAME);
            property.getValue().add(sID);
            lProperty.add(0, property);
            attributes.getProperty().clear();
            attributes.getProperty().addAll(lProperty);
            
            if(writeIds) 
            	outputNamesFile.addExtraAttributes(attributes);
         }

         if (subComponent instanceof ContainerType) {
            ContainerType subContainer = (ContainerType) subComponent;
            for (ComponentType component : subContainer.getContents()
                  .getWidgetOrContainer()) {
               generateComponentID(component, hashcode, gotTitle);
            }
         }
      }
   }

   /**
    * Compute hash code for given GUIType.
    *
    * A hashcode is computer for the given GUIType. First an attempt
    * is made to match the title of the input 'gui' into a pre-defined
    * set of regex. If a match is found, then the regex is used for
    * generating the hashcode.
    *
    * @param  gui      GUIType shows hashcode is to be generated
    * @return          Hashcode for the input GUIType
    */
   private long
   getWindowHashCode(GUIType gui)
   {
      GUITypeWrapper wGUI = new GUITypeWrapper(gui);
      String title = wGUI.getTitle();
      AppUtil appUtil = new AppUtil();

      System.out.println("SSS " + title);
      String fuzzyTitle = appUtil.findRegexForString(title);

      long hashcode = fuzzyTitle.hashCode();

      hashcode = (hashcode * 2) & 0xffffffffL;

      return hashcode;
   }


   private void generateComponentID(ComponentType component,
                       long windoHashCode, String windowName)
   {
      AttributesType attributes = component.getAttributes();

      long hashcode = 1;

      if (attributes != null) {

         long localHashCode = getLocalHashcode(component);
         hashcode = windoHashCode * prime + localHashCode;
         hashcode = (hashcode * 2) & 0xffffffffL;

         String sID = GUITARConstants.COMPONENT_ID_PREFIX + hashcode;

         List<PropertyType> lProperty = new ArrayList<PropertyType>();

         for (PropertyType p : attributes.getProperty()) {
            if (!GUITARConstants.ID_TAG_NAME.equals(p.getName())) {
               lProperty.add(p);
            }
            if (GUITARConstants.TITLE_TAG_NAME.equals(p.getName())) {
               GUITARLog.log.debug("generateComponentID " +
                                   "[" + sID + "] " + p.getValue() +
                                   "windoHashCode " + windoHashCode +
                                   "localHashCode " + localHashCode);
            }
         }
         if(!windowName.isEmpty()) {
	         PropertyType windowProperty = factory.createPropertyType();
	         windowProperty.setName(GUITARConstants.INWINDOW_TAG_NAME);
	         windowProperty.getValue().add(windowName);
	         lProperty.add(windowProperty);
         }
         
         PropertyType property = factory.createPropertyType();
         
         property.setName(GUITARConstants.ID_TAG_NAME);
         property.getValue().add(sID);
         lProperty.add(0, property);
         attributes.getProperty().clear();
         attributes.getProperty().addAll(lProperty);
         if(writeIds) 
         	outputNamesFile.addExtraAttributes(attributes);
      } else {
         hashcode = windoHashCode;
      }

      if (component instanceof ContainerType) {
         ContainerType container = (ContainerType) component;
         List<ComponentType> children = container.getContents()
               .getWidgetOrContainer();

         boolean isAddIndex;

         for (ComponentType child : children) {

            // Debug
            ComponentTypeWrapper wChild = new ComponentTypeWrapper(child);

            String sClass = wChild
                  .getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);

            Integer index = children.indexOf(child);
            long propagatedHashCode = prime * windoHashCode
                    + index.hashCode();

            generateComponentID(child, propagatedHashCode, windowName);
         }
      }
   }

   /**
    * @param component
    * @return
    */
   private boolean hasUniqueChildren(ComponentType component)
   {
      if (!(component instanceof ContainerType)) {
         return true;
      }

      List<Long> examinedHashCode = new ArrayList<Long>();

      ContainerType container = (ContainerType) component;
      for (ComponentType child : container.getContents()
            .getWidgetOrContainer()) {
         long hashcode = getLocalHashcode(child);
         if (examinedHashCode.contains(hashcode)) {
            return false;
         } else {
            examinedHashCode.add(hashcode);
         }
      }

      return true;
   }

   static List<String> ID_PROPERTIES = new ArrayList<String>(
         JFCConstants.ID_PROPERTIES);

   /**
    * Those classes are invisible widgets but cause false-positive when
    * calculating ID
    */
   static List<String> IGNORED_CLASSES = Arrays.asList("javax.swing.JPanel",
         "javax.swing.JTabbedPane", "javax.swing.JScrollPane",
         "javax.swing.JSplitPane", "javax.swing.Box",
         "javax.swing.JViewport", "javax.swing.JScrollBar",
         "javax.swing.JLayeredPane",
         "javax.swing.JList$AccessibleJList$AccessibleJListChild",
         "javax.swing.JList$AccessibleJList", "javax.swing.JList",
         "javax.swing.JScrollPane$ScrollBar",
         "javax.swing.plaf.metal.MetalScrollButton");

   /**
    * Those classes are invisible widgets but cause false-positive when
    * calculating ID
    */
   static List<String> IS_ADD_INDEX_CLASSES = Arrays
         .asList("javax.swing.JTabbedPane$Page");

   /**
    * @param component
    * @return
    */
   private long getLocalHashcode(ComponentType component)
   {
      final int prime = 31;

      long hashcode = 1;

      AttributesType attributes = component.getAttributes();
      if (attributes == null) {
         return hashcode;
      }

      // Specially handle titles
      AttributesTypeWrapper wAttribute =
         new AttributesTypeWrapper(attributes);
      String sClass = wAttribute
            .getFirstValByName(GUITARConstants.CLASS_TAG_NAME);

      if (IGNORED_CLASSES.contains(sClass)) {
         hashcode = (prime * hashcode + (sClass == null ? 0 : (sClass
               .hashCode())));
         return hashcode;
      }

      // Normal cases
      // Using ID_Properties for hash code

      List<PropertyType> lProperty = attributes.getProperty();

      if (lProperty == null) {
         return hashcode;
      }

      for (PropertyType property : lProperty) {

         String name = property.getName();
         if (ID_PROPERTIES.contains(name)) {

            hashcode = (prime * hashcode + (name == null ? 0 : name
                  .hashCode()));

            List<String> valueList = property.getValue();
            if (valueList != null) {
               for (String value : valueList) {
                  hashcode = (prime * hashcode + (value == null ? 0
                        : (value.hashCode())));

               }
            }
         }
      }

      hashcode = (hashcode * 2) & 0xffffffffL;

      return hashcode;

   }
} // End of class
