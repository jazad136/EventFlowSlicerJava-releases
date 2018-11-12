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
package edu.umd.cs.guitar.ripper.adapter;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.data.ComponentListType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.FullComponentType;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;

/**
 * 
 * Ignore component while ripping by a subset of its attributes
 * 
 * <p>
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 * 
 */
public class IgnoreSignExpandFilter extends GRipperAdapter {

	List<FullComponentType> lIgnoreFullComponent;

	// ComponentListType sIgnoreWidgetSignList;

	/**
	 * Ignore a certain widget
	 * 
	 * @param lIgnoredFullComponent
	 *            list of widgets to ignore
	 */
	public IgnoreSignExpandFilter(List<FullComponentType> lIgnoredFullComponent) {
		super();
		this.lIgnoreFullComponent = lIgnoredFullComponent;
	}

	/**
	 * 
	 */
	public IgnoreSignExpandFilter() {
		this(new ArrayList<FullComponentType>());
	}

	/**
	 * @return the lIgnoreFullComponent
	 */
	public List<FullComponentType> getlIgnoreFullComponent() {
		return lIgnoreFullComponent;
	}

	/**
	 * @param lIgnoreFullComponent
	 *            the lIgnoreFullComponent to set
	 */
	public void setlIgnoreFullComponent(
			List<FullComponentType> lIgnoreFullComponent) {
		this.lIgnoreFullComponent = lIgnoreFullComponent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.ripper.ComponentFilter#isProcess(edu.umd.cs.guitar.
	 * model.GXComponent)
	 */
	@Override
	public boolean isProcess(GComponent gComponent, GWindow gWindow) {

		ComponentType dComponent = gComponent.extractProperties();
		ComponentType dWindow = gWindow.extractWindow().getWindow();

		AttributesTypeWrapper compAttributesWrapper = new AttributesTypeWrapper(
				dComponent.getAttributes());
		AttributesTypeWrapper winAttributesAdapter = new AttributesTypeWrapper(
				dWindow.getAttributes());

		ComponentType signComp;
		ComponentType signWin;

		for (FullComponentType sign : lIgnoreFullComponent) {
			signComp = sign.getComponent();
			signWin = sign.getWindow();

			AttributesTypeWrapper dCompSignAttributes = new AttributesTypeWrapper(
					signComp.getAttributes());

			if (signWin != null) {

				AttributesTypeWrapper signWinAttributes = new AttributesTypeWrapper(
						signWin.getAttributes());

				if (!winAttributesAdapter.containsAll(signWinAttributes)) {
					continue; // wrong window.
				}

			}
			// passed window check. 
			if (compAttributesWrapper.containsAll(dCompSignAttributes))
				return true; // matched component
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.ripper.ComponentMonitor#ripComponent(edu.umd.cs.guitar
	 * .model.GXComponent)
	 */
	@Override
	public ComponentType ripComponent(GComponent component, GWindow window) {
		return component.extractProperties();
	}
}
