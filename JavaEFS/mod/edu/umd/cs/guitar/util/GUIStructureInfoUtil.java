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
package edu.umd.cs.guitar.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.ContainerType;
import edu.umd.cs.guitar.model.data.ContentsType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.GUIType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUITypeWrapper;

import edu.umd.cs.guitar.util.GUITARLog;

/**
 * This class implements a functionality for logging all
 * window names of a GUI structure.
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao N. Nguyen </a>
 * 
 */
public class GUIStructureInfoUtil
{
   public static List<String> LOG_PROPERTIES =
      Arrays.asList("Title", "ID");

   public GUIStructureInfoUtil() {
   }

   /*
    * (non-Javadoc)
    */
   public void
   generate(GUIStructure guistructure,
            boolean leaf)
   {
      for (GUIType gui : guistructure.getGUI()) {
         generateGUI(gui, leaf);
      }
   }

   /**
    * @param gui
    */
   private void
   generateGUI(GUIType gui,
               boolean leaf)
   {
      ContainerType container = gui.getContainer();

      if (container == null) {
         return;
      }

      List<ComponentType> subComponentList = container.getContents()
            .getWidgetOrContainer();

      if (subComponentList == null) {
         return;
      }

      for (ComponentType subComponent : subComponentList) {
         AttributesType attributes = subComponent.getAttributes();
         if (attributes != null) {
            String str = "";

            for (PropertyType p : attributes.getProperty()) {
               if (LOG_PROPERTIES.contains(p.getName())) {
                  str += p.getValue() + " ";
               }
            }

            if (!str.equals("")) {
              GUITARLog.log.info(str);
            }
         }

         if (subComponent instanceof ContainerType) {
            ContainerType subContainer = (ContainerType) subComponent;
            for (ComponentType component : subContainer.getContents()
                  .getWidgetOrContainer()) {
               generateComponentID(component, 1, leaf);
            }
         }
      }
   }

   private void
   generateComponentID(ComponentType component,
                       int level,
                       boolean leaf)
   {
      AttributesType attributes = component.getAttributes();
      String space="";

      for (int i = 0 ; i < level ; i++) {
         space = space + "   ";
      }

      // Write the titles of containers only
      if (attributes != null &&
          ((component instanceof ContainerType) || leaf)) {
         String str = space;

         for (PropertyType p : attributes.getProperty()) {
            if (LOG_PROPERTIES.contains(p.getName())) {
               str += p.getValue() + " ";
            }
         }

         if (!str.equals("")) {
           GUITARLog.log.info(str);
         }
      }

      if (component instanceof ContainerType) {
         ContainerType container = (ContainerType) component;
         List<ComponentType> children = container.getContents()
               .getWidgetOrContainer();

         boolean isAddIndex;

         for (ComponentType child : children) {
            generateComponentID(child, level + 1, leaf);
         }
      }
   }
} // End of class
