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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

	@SuppressWarnings("restriction")
class RascalTestOpenListener extends SelectionAdapter {

	private static final Pattern splitName = Pattern.compile("^([^:]*): <([0-9]*),([0-9]*)>\\(.*");
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
		if (selection.size() != 1) {
			return;
		}

		TestElement testElement = (TestElement) selection.getFirstElement();
		if (testElement.getParent() != null) {
			String parent = testElement.getParent().getTestName();
			if (parent.contains("::")) {
				
				Matcher name = splitName.matcher(testElement.getTestName());
				
				if (name.matches()) {
					String testName = name.group(1);
					int offset = Integer.parseInt(name.group(2));
					int length = Integer.parseInt(name.group(3));
					openRascalTest(parent, testName, offset, length);
					//System.out.println("We have a rascal thing: " + parent +"::"+testName + "(" + lineNumber + ")");
					return;					
				}
			}
		}
		if (testElement.getTestName().contains("::")) {
			// click on a test module
			if (testElement.getParent() == null || testElement.getParent() instanceof TestRoot || !testElement.getParent().getTestName().contains("::")) {
				// this is root level contain which tends to be a folder
				return ;
			}
			openRascalTest(testElement.getTestName(), "", 0, 0);
			return;
		}
		
		oldListener.widgetDefaultSelected(e);
	}

	private IWorkbenchWindow getWorkbenchWindow() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();

		if (win == null && wb.getWorkbenchWindowCount() != 0) {
			win = wb.getWorkbenchWindows()[0];
		}
		return win;
	}
	
	
	private void openRascalTest(String parent, String testName, int offset, int length) {
		String file = parent.replace("::", "/").replace("\\","");
		openFile("rascal", "/src/org/rascalmpl/library/" + file + ".rsc", offset, length);
	}
	
	private void openFile(final String projectName, String path, final int offset,final int length) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project == null) {
			return;
		}
		final FileEditorInput target = new FileEditorInput(project.getFile(path));
		
		final IWorkbenchWindow win = getWorkbenchWindow();
		if (win != null) {
			final IWorkbenchPage page = win.getActivePage();
			if (page != null) {
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						try {
							IEditorPart part = IDE.openEditor(page, target.getFile());
							if (part != null && part instanceof ITextEditor ) {
								((ITextEditor)part).selectAndReveal(offset, length);
							}
						} catch (PartInitException e) {
						}
					}
				});
			}
		}
	}
}