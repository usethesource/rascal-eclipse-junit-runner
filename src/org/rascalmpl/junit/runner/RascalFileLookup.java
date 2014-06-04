package org.rascalmpl.junit.runner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.junit.model.ITestRunSession;
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
					openRascalTest(test.getParentContainer().getTestRunSession(), parent, testName, offset, length);
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
			openRascalTest(test.getParentContainer().getTestRunSession(), test.getTestName(), "", 0, 0);
			return true;
		}
		return false;
	}
	private static void openRascalTest(ITestRunSession session, String parent, String name, int offset, int length) {
		String file = parent.replace("::", "/").replace("\\","") + ".rsc";
		openFile(session.getLaunchedProject().getProject(), file, offset, length);
	}

	private static IWorkbenchWindow getWorkbenchWindow() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();

		if (win == null && wb.getWorkbenchWindowCount() != 0) {
			win = wb.getWorkbenchWindows()[0];
		}
		return win;
	}
	
	private static void openFile(final IProject project, String path, final int offset,final int length) {
		if (project == null) {
			return;
		}
		List<String> roots = tryFindRascalLibraryPaths(project);
		if (roots.isEmpty()) {
			return;
		}
		IFile fullFileName = null;
		for (String r : roots) {
			fullFileName = project.getFile(r + "/" + path);
			if (fullFileName.exists()) {
				break;
			}
		}

		final FileEditorInput target = new FileEditorInput(fullFileName);
		
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
	
	private static List<String> tryFindRascalLibraryPaths(IProject project) {
		try {
			IFile rascalMF = project.getFile("/META-INF/RASCAL.MF");
			if (rascalMF.exists()) {
				return getAttributeList(rascalMF.getContents(), "Source", "/src/org/rascalmpl/library/");
			}
		} catch (CoreException e) {
		}
		List<String> result = new ArrayList<>();
		result.add("/src/org/rascalmpl/library/");
		return result;
	}
	
	private static String[] trim(String[] elems) {
		for (int i = 0; i < elems.length; i++) {
			elems[i] = elems[i].trim();
		}
		return elems;
	}

	private static List<String> getAttributeList(InputStream mf, String label,
			String def) {
		if (mf != null) {
			try {
				Manifest manifest = new Manifest(mf);
				String source = manifest.getMainAttributes().getValue(label);

				if (source != null) {
					return Arrays.<String> asList(trim(source.split(",")));
				}
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					mf.close();
				} catch (IOException e) {
					// too bad
				}
			}
		}

		return Arrays.<String> asList(new String[] { def });
	}
}
