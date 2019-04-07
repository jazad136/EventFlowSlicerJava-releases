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
package edu.umd.cs.guitar.model.data;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ParameterListType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ParameterListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Parameter" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParameterListType", propOrder = {
    "parameter"
})
public class ParameterListType {

    @XmlElement(name = "Parameter")
    protected List<String> parameter;

    /**
     * Gets the value of the parameter property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameter property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameter().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<String>();
        }
        return this.parameter;
    }

    /**
     * Sets the value of the parameter property.
     *
     * @param parameter
     *     allowed object is
     *     {@link String }
     *
     */
    public void setParameter(List<String> parameter) {
        this.parameter = parameter;
    }

    /**
     * Returns true if this list type has at least one
     * non-empty, non-null parameter in its parameter list.
     * @return
     */
    public boolean hasOneParameter()
    {
    	boolean hasOne = parameter != null && !parameter.isEmpty();
    	if(!hasOne)
    		return false;
    	for(String s : parameter) {
    		hasOne = s != null && !s.isEmpty();
    		if(hasOne)
    			return true;
    	}
    	return false;
    }
    public String toString()
    {
    	return parameter == null ? "" : parameter.toString();
    }
}