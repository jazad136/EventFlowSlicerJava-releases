//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2011.09.30 at 01:14:03 PM CDT
//


package edu.umd.cs.guitar.model.data;

import java.util.Arrays;

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
     * Create an instance of {@link WidgetMapElementType }
     *
     */
    public WidgetMapElementType createWidgetMapElementType() {
        return new WidgetMapElementType();
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
     * Create an instance of {@link PathType }
     *
     */
    public PathType createPathType() {
        return new PathType();
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
     * Create an instance of {@link WidgetMapType }
     *
     */
    public WidgetMapType createWidgetMapType() {
        return new WidgetMapType();
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
