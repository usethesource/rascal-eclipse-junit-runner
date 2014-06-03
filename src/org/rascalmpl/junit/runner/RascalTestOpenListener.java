/*******************************************************************************
 * Copyright (c) 2009-2012 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Davy Landman - CWI
 *   
*******************************************************************************/
package org.rascalmpl.junit.runner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.TypedListener;

	@SuppressWarnings("restriction")
class RascalTestOpenListener extends SelectionAdapter {

	private static final Pattern splitName = Pattern.compile("^([^:]*):([0-9]*)\\(.*");
	private final SelectionProviderMediator provider;

	private final TestRunnerViewPart part;

	private final SelectionListener oldListener;

	public RascalTestOpenListener(TestRunnerViewPart part, SelectionProviderMediator provider, TypedListener oldListener) {
		super();
		this.part = part;
		this.provider = provider;
		this.oldListener =(SelectionListener)oldListener.getEventListener();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		IStructuredSelection selection = (IStructuredSelection)provider.getSelection();
		if (selection.size() != 1)
			return;

		TestElement testElement = (TestElement) selection.getFirstElement();
		if (testElement.getParent() != null) {
			String parent = testElement.getParent().getTestName();
			if (parent.contains("::")) {
				
				Matcher name = splitName.matcher(testElement.getTestName());
				
				if (name.matches()) {
					String testName = name.group(1);
					int lineNumber = Integer.parseInt(name.group(2));
					System.out.println("We have a rascal thing: " + parent +"::"+testName + "(" + lineNumber + ")");
					return;					
				}
			}
		}
		
		oldListener.widgetDefaultSelected(e);
	}
}