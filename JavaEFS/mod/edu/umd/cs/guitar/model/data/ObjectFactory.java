/*	
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *	the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *	conditions:
 * 
 *	The above copyright notice and this permission notice shall be included in all copies or substantial 
 *	portions of the Software.
 *
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *	LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *	EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *	THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.umd.cs.guitar.model.data;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the edu.umd.cs.guitar.model.data package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: edu.umd.cs.guitar.model.data
     *
     */
    public ObjectFactory() {
    }

    public PartialConfiguration createPartialConfiguration()
    {
    	PartialConfiguration object = new PartialConfiguration();
    	object.initializeDefaults();
    	return object;
    }
    /**
     * Create an instance of {@link EventGraphType }
     *
     */
    public EventGraphType createEventGraphType() {
        return new EventGraphType();
    }


    /**
     * Create an instance of {@link ParameterListType }
     *
     */
    public ParameterListType createParameterListType() {
        return new ParameterListType();
    }


    /**
     * Create an instance of {@link GUIType }
     *
     */
    public GUIType createGUIType() {
        return new GUIType();
    }

    /**
     * Create an instance of {@link ContainerType }
     *
     */
    public ContainerType createContainerType() {
        return new ContainerType();
    }

    /**
     * Create an instance of {@link ComponentType }
     *
     */
    public ComponentType createComponentType() {
        return new ComponentType();
    }

    /**
     * Create an instance of {@link EventsType }
     *
     */
    public EventsType createEventsType() {
        return new EventsType();
    }

    /**
     * Create an instance of {@link TestCase }
     *
     */
    public TestCase createTestCase() {
        return new TestCase();
    }

    /**
     * Create an instance of {@link PropertyType }
     *
     */
    public PropertyType createPropertyType() {
        return new PropertyType();
    }


    /**
     * Create an instance of {@link EFG }
     *
     */
    public EFG createEFG() {
        return new EFG();
    }

    /**
     * Create an instance of {@link AttributesType }
     *
     */
    public AttributesType createAttributesType() {
        return new AttributesType();
    }


    /**
     * Create an instance of {@link FullComponentType }
     *
     */
    public FullComponentType createFullComponentType() {
        return new FullComponentType();
    }


    public ConfigurationReplay createConfigurationReplay()
    {
    	return new ConfigurationReplay();
    }
    /**
     * Create an instance of {@link ComponentListType }
     *
     */
    public ComponentListType createComponentListType() {
        return new ComponentListType();
    }

    /**
     * Create an instance of {@link StepType }
     *
     */
    public StepType createStepType() {
        return new StepType();
    }

    /**
     * Create an instance of {@link Configuration }
     *
     */
    public Configuration createConfiguration() {
        return new Configuration();
    }

    /**
     * Create an instance of {@link EventType }
     *
     */
    public EventType createEventType() {
        return new EventType();
    }


    /**
     * Create an instance of {@link GUIStructure }
     *
     */
    public GUIStructure createGUIStructure() {
        return new GUIStructure();
    }

    /**
     * Create an instance of {@link ContentsType }
     *
     */
    public ContentsType createContentsType() {
        return new ContentsType();
    }

    /**
     * Create an instance of {@link RowType }
     *
     */
    public RowType createRowType() {
        return new RowType();
    }
   

    /**
     * Create an instance of {@link Task }
     *
     */
    public Task createTask() {
        return new Task();
    }

    /**
     * Create an instance of {@link Step }
     *
     */
    public Step createStep() {
        return new Step();
    }
    /**
     * Create an instance of {@link TaskList }
     *
     */
    public TaskList createTaskList() {
        return new TaskList();
    }

    /**
     * Create an instance of {@link Widget }
     *
     */
    public Widget createWidget() {
        return new Widget();
    }


    /**
     * Create an instance of {@link UNO }
     *
     */
    public UNO createUNO() {
        return new UNO();
    }

    /**
     * Create an instance of {@link Preferences }
     *
     */
    public Preferences createPreferences() {
        return new Preferences();
    }

    /**
     * Create an instance of {@link Atomic }
     *
     */
    public Atomic createAtomic() {
        return new Atomic();
    }

    /**
     * Create an instance of {@link AtomicGroup }
     *
     */
    public AtomicGroup createAtomicGroup()
    {
        return new AtomicGroup();
    }

    /**
     * Create an instance of {@link Exclusion }
     *
     */
    public Exclusion createExclusion() {
        return new Exclusion();
    }

    /**
     * Create an instance of {@link Linux }
     *
     */
    public Linux createLinux()
    {
        return new Linux();
    }

    /**
     * Create an instance of {@link Order }
     *
     */
    public Order createOrder()
    {
        return new Order();
    }

    /**
     * Create an instance of {@link OrderGroup }
     *
     */
    public OrderGroup createOrderGroup()
    {
        return new OrderGroup();
    }

    /**
     * Create an instance of {@link Repeat }
     *
     */
    public Repeat createRepeat()
    {
    	return new Repeat();
    }

    /**
     * Create an instance of {@link Stop }
     */
    public Stop createStop()
    {
    	return new Stop();
    }
    /**
     * Create an instance of {@link Required }
     *
     */
    public Required createRequired() {
        return new Required();
    }


    /**
     * Create an instance of {@link LogWidget }
     * 
     */
    public LogWidget createLogWidget() {
        return new LogWidget();
    }

}
