package org.rascalmpl.junit.runner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.swt.widgets.Display;
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
public class RascalFileLookup {

	private static final Pattern splitName = Pattern.compile("^([^:]*): <([0-9]*),([0-9]*)>\\(.*");
	
	public static boolean isRascalTestElement(TestElement test) {
		if (test.getParent() != null) {
			String parent = test.getParent().getTestName();
			if (parent.contains("::")) {
				if ( splitName.matcher(test.getTestName()).matches()) {
					return true;
				}
			}
		}
		if (test.getTestName().contains("::")) {
			if (test.getParent() == null || test.getParent() instanceof TestRoot || !test.getParent().getTestName().contains("::")) {
				// this is root level contain which tends to be a folder
				return false;
			}
			return true;
		}
		return false;
	}

	public static boolean tryOpenRascalTest(TestElement test) {
		if (test.getParent() != null) {
			String parent = test.getParent().getTestName();
			if (parent.contains("::")) {
				
				Matcher name = splitName.matcher(test.getTestName());
				
				if (name.matches()) {
					String testName = name.group(1);
					int offset = Integer.parseInt(name.group(2));
					int length = Integer.parseInt(name.group(3));
					openRascalTest(parent, testName, offset, length);
					//System.out.println("We have a rascal thing: " + parent +"::"+testName + "(" + lineNumber + ")");
					return true;					
				}
			}
		}
		if (test.getTestName().contains("::")) {
			// click on a test module
			if (test.getParent() == null || test.getParent() instanceof TestRoot || !test.getParent().getTestName().contains("::")) {
				// this is root level contain which tends to be a folder
				return false;
			}
			openRascalTest(test.getTestName(), "", 0, 0);
			return true;
		}
		return false;
	}
	private static void openRascalTest(String parent, String name, int offset, int length) {
		String file = parent.replace("::", "/").replace("\\","");
		openFile("rascal", "/src/org/rascalmpl/library/" + file + ".rsc", offset, length);
	}

	private static IWorkbenchWindow getWorkbenchWindow() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();

		if (win == null && wb.getWorkbenchWindowCount() != 0) {
			win = wb.getWorkbenchWindows()[0];
		}
		return win;
	}
	
	private static void openFile(final String projectName, String path, final int offset,final int length) {
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
