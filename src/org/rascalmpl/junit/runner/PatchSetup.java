/*******************************************************************************
 * Copyright (c) 2012-2013 itemis AG
 * Copyright (c) 2014-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Moritz Eysholdt - itemis AG - Original version
 *   * Davy Landman - CWI
 *   
 *   The original version (https://github.com/meysholdt/eclipse_jdt_junit_runners/blob/master/org.eclipse.jdt.junit.runners/src/org/eclipse/jdt/junit/runners/PatchSetup.java)
 *   was unlicensed although other files in the repository were licensed as EPL. 
 *   
 *   It has been heavily modified, except for the reflection hacking to get to the right
 *   parts of the Eclipse UI.
*******************************************************************************/

package org.rascalmpl.junit.runner;


import java.lang.reflect.Field;

import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.junit.ui.TestViewer;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class PatchSetup implements IStartup {

	private class WorkbenchListener implements IWindowListener, IPageListener, IPartListener {

		public void pageActivated(IWorkbenchPage page) {
		}

		public void pageClosed(IWorkbenchPage page) {
			page.removePartListener(listener);
		}

		public void pageOpened(IWorkbenchPage page) {
			page.addPartListener(listener);
		}

		public void partActivated(IWorkbenchPart part) {
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {

		}

		public void partOpened(IWorkbenchPart part) {
			checkPart(part);
		}

		public void windowActivated(IWorkbenchWindow window) {
		}

		public void windowClosed(IWorkbenchWindow window) {
			window.removePageListener(listener);
		}

		public void windowDeactivated(IWorkbenchWindow window) {
		}

		public void windowOpened(IWorkbenchWindow window) {
			window.addPageListener(listener);
		}

	}

	private WorkbenchListener listener = new WorkbenchListener();

	private void checkPart(IWorkbenchPart part) {
		if (part instanceof TestRunnerViewPart) {
			TestRunnerViewPart testpart = (TestRunnerViewPart) part;
			TestViewer viewer = readField(part, "fTestViewer", TestViewer.class);
			TreeViewer treeViewer = readField(viewer, "fTreeViewer", TreeViewer.class);
			TableViewer tableViewer = readField(viewer, "fTableViewer", TableViewer.class);

			TypedListener treeListener = findSelectionListener(treeViewer.getTree(), TestViewer.class);
			TypedListener tableListener = findSelectionListener(tableViewer.getTable(), TestViewer.class);
			SelectionProviderMediator selectionProvider = readField(viewer, "fSelectionProvider", SelectionProviderMediator.class);
			if (treeListener != null && tableListener != null) {
				replaceListener(treeViewer.getTree(), treeListener, new RascalTestOpenListener(testpart, selectionProvider,treeListener));
				replaceListener(tableViewer.getTable(), tableListener, new RascalTestOpenListener(testpart, selectionProvider,tableListener));
			}

			TypedListener treeMenuListener = findMenuListener(treeViewer.getTree().getMenu(), MenuManager.class);
			TypedListener tableMenuListener = findMenuListener(tableViewer.getTable().getMenu(), MenuManager.class);
			if (treeMenuListener != null && tableMenuListener != null) {
				replaceMenuListener(treeViewer.getTree().getMenu(), treeMenuListener, new RascalMenuListener(testpart, selectionProvider, treeMenuListener));
				replaceMenuListener(tableViewer.getTable().getMenu(), tableMenuListener, new RascalMenuListener(testpart, selectionProvider, tableMenuListener));
			}
		}
	}

	@Override
	public void earlyStartup() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				workbench.addWindowListener(listener);
				for (IWorkbenchWindow window : workbench.getWorkbenchWindows())
					for (IWorkbenchPage page : window.getPages()) {
						page.addPartListener(listener);
						for (IViewReference part : page.getViewReferences()) {
							checkPart(part.getView(false));
						}
					}
			}
		});
	}

	private TypedListener findSelectionListener(Widget provider, Class<?> declaringClass) {
		for (Listener o : provider.getListeners(SWT.Selection))
			if (o instanceof TypedListener && ((TypedListener) o).getEventListener().getClass().getDeclaringClass() == declaringClass)
				return (TypedListener) o;
		return null;
	}
	private TypedListener findMenuListener(Widget provider, Class<?> declaringClass) {
		for (Listener o : provider.getListeners(SWT.Show))
			try {
				if (o instanceof TypedListener && ((TypedListener) o).getEventListener().getClass().getDeclaredField("this$0").getType() == declaringClass)
					return (TypedListener) o;
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
	}

	private void replaceListener(Widget provider, TypedListener oldListener, SelectionListener newListener) {
		provider.removeListener(SWT.Selection, oldListener);
		provider.removeListener(SWT.DefaultSelection, oldListener);
		provider.addListener(SWT.Selection, new TypedListener(newListener));
		provider.addListener(SWT.DefaultSelection, new TypedListener(newListener));
	}
	private void replaceMenuListener(Widget provider, TypedListener oldListener, MenuListener newListener) {
		provider.removeListener(SWT.Show, oldListener);
		provider.addListener(SWT.Show, new TypedListener(newListener));
	}

	@SuppressWarnings("unchecked")
	private static <T> T readField(Class<?> declaredIn, Object owner, String fieldName, Class<T> type) {
		try {
			Field field = declaredIn.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(owner);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T readField(Object owner, String fieldName, Class<T> type) {
		return readField(owner.getClass(), owner, fieldName, type);
	}
}
