/*******************************************************************************
 * Copyright (c) 2014-2014 CWI
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

import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TypedListener;

@SuppressWarnings("restriction")
public class RascalMenuListener extends MenuAdapter {

	private final MenuAdapter oldListener;
	private final SelectionProviderMediator selectionProvider;


	public RascalMenuListener(TestRunnerViewPart testpart,
			SelectionProviderMediator selectionProvider, TypedListener oldListener) {
		this.oldListener = (MenuAdapter)oldListener.getEventListener();
		this.selectionProvider = selectionProvider;
	}


	@Override
	public void menuShown(MenuEvent e) {
		oldListener.menuShown(e);

		// now lets see if we have a rascal menu
		IStructuredSelection selection= (IStructuredSelection) selectionProvider.getSelection();
		if (selection.size() != 1) {
			return;
		}

		final TestElement testElement = (TestElement) selection.getFirstElement();
		if (TestNameTranslator.isRascalTestElement(testElement)) {
			MenuItem gotoFile = ((Menu)e.getSource()).getItem(0);
			Listener oldListener = gotoFile.getListeners(SWT.Selection)[0];
			gotoFile.removeListener(SWT.Selection, oldListener);
			gotoFile.addListener(SWT.Selection, new TypedListener( new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					TestNameTranslator.tryOpenRascalTest(testElement);
				}
			}));
		}
		else if (TestNameTranslator.isRascalSuiteElement(testElement)) {
			MenuItem gotoFile = ((Menu)e.getSource()).getItem(0);
			gotoFile.setEnabled(false);
		}
	}
	@Override
	public void menuHidden(MenuEvent e) {
		oldListener.menuHidden(e);
	}
}
