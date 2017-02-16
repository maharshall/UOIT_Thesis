package synctest.views;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.*;

import synctest.testing.SyncTestRunnable;
import synctest.testing.SyncTestRunner;
import synctest.util.ExecutionResult;
import synctest.util.TestResult;

import org.eclipse.ui.ISharedImages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Vector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;

/**
 * This class creates and updates the UI for the plugin.
 * Values for running tests are taken from the widgets in 
 * the testing tab and are passed to SyncTestRunner.
 * The UI is updated by updateResultWidgets() which receives
 * input from runTests(), which reads test results fron the 
 * syncTestOutput.txt file.
 * 
 * @author Alexander Marshall
 * */
public class SyncTestView extends ViewPart {

	public static final String ID = "synctest.views.SyncTestView";

	// An instance of SyncTestRunner to send input values to SyncTestRunnable
	private synctest.testing.SyncTestRunner runner;

	// The variables for storing test results
	private double pass = 0, fail = 0, error = 0, dead = 0, total = 0;

	private Vector<TestResult> results = new Vector<TestResult>();
	private Vector<ExecutionResult> executionResults = new Vector<ExecutionResult>();
	// The currently selected test in the Combo widget
	private TestResult selection;
	private String currentTest;

	IWorkbench 		workbench 	= PlatformUI.getWorkbench();
	ISharedImages 	images 		= workbench.getSharedImages();

	Thread			syncTestRunnable, getTestResults;

	//CONFIG TAB WIDGETS
	CTabItem 		config;
	CTabItem		testing;
    Composite 		composite;
    GridData 		gridData;
    GridLayout 		gridLayout;
    Text 			baseDir, sourceDir, testDir;
    Text 			sleepAmnt, testCountAmnt;
    Button			doInstrumentation;
    Label			instrumentationLabel;

    //TESTING TAB WIDETS
	ProgressBar 	progress, execProgress;
    Label			running, execRunning, testLabel, execLabel;
    Text 			passAmnt, failAmnt, errAmnt, deadAmnt;
    boolean			passAmntIsFraction = false, failAmntIsFraction = false;
    boolean			errAmntIsFraction = false,  deadAmntIsFraction = false;
    Canvas 			canvas, detailCanvas;
    Combo 			combo;
    Tree 			resultTree;
    Text 			testDetails;
    Button 			run, cancel;

	public SyncTestView() {}

	/**
	 * Initializes the two tabs of the plugin
	 * */
	public void createPartControl(Composite parent) {
	    //Create tabs for configuration and testing
		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		createConfigTab(tabs);
		createTestingTab(tabs);
	}

	/**
	 * Creates the config tab of the plugin
	 * */
	private void createConfigTab(CTabFolder tabFolder) {
	    config = new CTabItem(tabFolder, SWT.NONE);
	    config.setText("Settings");
	    tabFolder.setSelection(config);

	    composite = new Composite(tabFolder, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);
		gridLayout = new GridLayout();
		composite.setLayout(gridLayout);
		
		/**************************************************************************************************/

		// A group for the directory options
		Group folders = new Group(composite, SWT.NULL);
		folders.setText("Directory Settings");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		folders.setLayoutData(gridData);
		folders.setLayout(new GridLayout(2, false));

		// Text entry for project base directory
		baseDir = new Text(folders, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		baseDir.setLayoutData(gridData);
		baseDir.setMessage("Select the project base directory");

		// Button to launch file browser
		Button setBaseDir = new Button(folders, SWT.PUSH);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		setBaseDir.setLayoutData(gridData);
		setBaseDir.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("/icons/fldr_obj.gif")));

		// Checkbox for automatically finding test/source directories
		Button findDirs = new Button(folders, SWT.CHECK);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.horizontalSpan = 2;
		findDirs.setLayoutData(gridData);
		findDirs.setText("Automatically find souce and test directories");

		// spawn a window to select a directory
		setBaseDir.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		        switch (e.type) {
		        case SWT.Selection:
		            DirectoryDialog dialog = new DirectoryDialog(
		            		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
		            dialog.setMessage("Select the base directory for your project");

		            if(System.getProperty("os.name").equals("Linux")) {
		            	dialog.setFilterPath("/home");
		            }

		            String path = dialog.open();
		            baseDir.setText(path);
		            if(findDirs.getSelection() == true) importDirs(path);
		          break;
		        }
		      }
		    });

		Label seperator = new Label(folders, SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		seperator.setLayoutData(gridData);

		// Text entry for project source code
		sourceDir = new Text(folders, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		sourceDir.setLayoutData(gridData);
		sourceDir.setMessage("Select the directory containing source code");

		// Button to launch file browser
		Button setSourceDir = new Button(folders, SWT.PUSH);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		setSourceDir.setLayoutData(gridData);
		setSourceDir.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("/icons/fldr_obj.gif")));

		// spawn a window to select a directory
		setSourceDir.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		        switch (e.type) {
		        case SWT.Selection:
		            DirectoryDialog dialog = new DirectoryDialog(
		            		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
		            dialog.setMessage("Select the directory containing project source files");

		            if(System.getProperty("os.name").equals("Linux")) {
		            	dialog.setFilterPath("/home");
		            }
		            sourceDir.setText(dialog.open());
		          break;
		        }
		      }
		    });

		// Text entry for project test directory
	    testDir = new Text(folders, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		testDir.setLayoutData(gridData);
		testDir.setMessage("Select the directory containing junit tests");

		// Button to launch file browser
		Button setTestDir = new Button(folders, SWT.PUSH);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		setTestDir.setLayoutData(gridData);
		setTestDir.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("/icons/fldr_obj.gif")));

		// spawn a window to select a directory
		setTestDir.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		        switch (e.type) {
		        case SWT.Selection:
		            DirectoryDialog dialog = new DirectoryDialog(
		            		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
		            dialog.setMessage("Select the directory containing your test files");

		            if(System.getProperty("os.name").equals("Linux")) {
		            	dialog.setFilterPath("/home");
		            }
		            sourceDir.setText(dialog.open());
		          break;
		        }
		      }
		    });

	    /**************************************************************************************************/

	    // Group for setting test values such as sleep amount and num of repetitions
	    Group settings = new Group(composite, SWT.NULL);
		settings.setText("Test Settings");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		settings.setLayoutData(gridData);
		settings.setLayout(new GridLayout(6, false));

		Label checkLbl = new Label( settings, SWT.NULL );
		gridData = new GridData( GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER );
		gridData.horizontalSpan = 2;
		checkLbl.setLayoutData( gridData );
		checkLbl.setText("Check for deadlocks every ");

		// The amount of seconds to sleep between deadlock checks
		sleepAmnt = new Text(settings, SWT.NULL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		sleepAmnt.setLayoutData(gridData);
		sleepAmnt.setText("0.6");

		Label seconds = new Label(settings, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		seconds.setLayoutData(gridData);
		seconds.setText(" seconds");

		Label testCountLbl = new Label( settings, SWT.NULL );
		gridData = new GridData( GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER );
		testCountLbl.setLayoutData( gridData );
		testCountLbl.setText("Run each test ");

		// The amount of times to run each test
		testCountAmnt = new Text(settings, SWT.NULL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		testCountAmnt.setLayoutData(gridData);
		testCountAmnt.setText("100");

		Label times = new Label(settings, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		times.setLayoutData(gridData);
		times.setText(" time(s)");
		
		// A checkbox for instrumenting code
		doInstrumentation = new Button(settings, SWT.CHECK);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_CENTER);
		doInstrumentation.setLayoutData(gridData);
		doInstrumentation.setSelection(true);
		
		instrumentationLabel = new Label(settings, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 5;
		instrumentationLabel.setLayoutData(gridData);
		instrumentationLabel.setText("Instrument source code before running test. (unimplemented)");

		config.setControl(composite);
	}

	/**
	 * Creates the testing tab of the plugin
	 * */
	private void createTestingTab(CTabFolder tabFolder) {
		testing = new CTabItem(tabFolder, SWT.NONE);
	    testing.setText("Testing");

	    composite = new Composite(tabFolder, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);
		gridLayout = new GridLayout(2, false);
		composite.setLayout(gridLayout);
		
		/**************************************************************************************************/

		// A group for overall testing related things
		Group tests = new Group(composite, SWT.NULL);
		tests.setText("Testing Overview");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		tests.setLayoutData(gridData);
		tests.setLayout(new GridLayout(4, false));

		// A toolbar containing some (possibly) useful buttons
		ToolBar toolbar = new ToolBar(tests, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 4;
		toolbar.setLayoutData(gridData);

		// Button to clear all widgets (will also reset all values to zero!)
		ToolItem clear = new ToolItem(toolbar, SWT.PUSH);
		clear.setToolTipText("clear all fields");
		clear.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("/icons/clear_co.gif")));
		clear.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				clearResultWidgets();
			}
		});
		
		// Button to print results to a text file
		ToolItem export = new ToolItem(toolbar, SWT.PUSH);
		export.setToolTipText("export results to text file");
		export.setImage(new Image(Display.getDefault(), getClass().getResourceAsStream("/icons/insert_template.gif")));
		export.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
				dialog.setText("Choose the directory to export results to");

	            if(System.getProperty("os.name").equals("Linux")) {
	            	dialog.setFilterPath("/home");
	            }

	            String path = dialog.open();
	            exportResults(path);
			}
		});

		/* Label to show currently running test
		 *
		 * The label only fills as much space as it's initialized to
		 * so I just filled it with a bunch of tabs
		 * */
		running = new Label(tests, SWT.LEFT);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 4;
		running.setLayoutData(gridData);
		running.setText("\t\t\t\t\t\t\t\t\t\t");

		// Progress bar for test progress
		progress = new ProgressBar(tests, SWT.NULL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		progress.setMinimum(0);
		progress.setSelection(0);
		progress.setLayoutData(gridData);

		// Label for execution progress
		execRunning = new Label(tests, SWT.LEFT);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 4;
		execRunning.setLayoutData(gridData);
		execRunning.setText("\t\t\t\t\t\t\t\t\t\t");

		// Progress bar for execution progress
		execProgress = new ProgressBar(tests, SWT.NULL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		progress.setMinimum(0);
		execProgress.setLayoutData(gridData);
		execProgress.setMinimum(0);
		execProgress.setSelection(0);

		Label passed = new Label(tests, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		passed.setLayoutData(gridData);
		passed.setText("Passes:");

		// Box showing the amount of tests that have passed
		// Can be a fraction or percentile
		passAmnt = new Text(tests, SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		passAmnt.setLayoutData(gridData);
		passAmnt.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		passAmnt.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		passAmnt.setText("0.00%");

		// This listener allows the toggle between fraction and percentile
		passAmnt.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				if(passAmntIsFraction) {
					passAmnt.setText(String.format("%.2f", pass/total*100)+"%");
					passAmntIsFraction = false;
				} else {
					passAmnt.setText((int)pass+"/"+(int)total);
					passAmntIsFraction = true;
				}
			}

		});

		Label failed = new Label(tests, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		failed.setLayoutData(gridData);
		failed.setText("Failures:");


		// Box showing the amount of tests that have failed
		// Can be a fraction or percentile
		failAmnt = new Text(tests, SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		failAmnt.setLayoutData(gridData);
		failAmnt.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
		failAmnt.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		failAmnt.setText("0.00%");

		// This listener allows the toggle between fraction and percentile
		failAmnt.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				if(failAmntIsFraction) {
					failAmnt.setText(String.format("%.2f", fail/total*100)+"%");
					failAmntIsFraction = false;
				} else {
					failAmnt.setText((int)fail+"/"+(int)total);
					failAmntIsFraction = true;
				}
			}

		});

		Label errorLbl = new Label(tests, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		errorLbl.setLayoutData(gridData);
		errorLbl.setText("Errors:");


		// Box showing the amount of tests that have resulted in an error
		// Can be a fraction or percentile
		errAmnt = new Text(tests, SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		errAmnt.setLayoutData(gridData);
		errAmnt.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
		errAmnt.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		errAmnt.setText("0.00%");

		// This listener allows the toggle between fraction and percentile
		errAmnt.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				if(errAmntIsFraction) {
					errAmnt.setText(String.format("%.2f", error/total*100)+"%");
					errAmntIsFraction = false;
				} else {
					errAmnt.setText((int)error+"/"+(int)total);
					errAmntIsFraction = true;
				}
			}

		});

		Label deadlocked = new Label(tests, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		deadlocked.setLayoutData(gridData);
		deadlocked.setText("Deadlocks:");

		// Box showing the amount of tests that have deadlocked
		// Can be a fraction or percentile
		deadAmnt = new Text(tests, SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		deadAmnt.setLayoutData(gridData);
		deadAmnt.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		deadAmnt.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		deadAmnt.setText("0.00%");

		// This listener allows the toggle between fraction and percentile
		deadAmnt.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				if(deadAmntIsFraction) {
					deadAmnt.setText(String.format("%.2f", dead/total*100)+"%");
					deadAmntIsFraction = false;
				} else {
					deadAmnt.setText((int)dead+"/"+(int)total);
					deadAmntIsFraction = true;
				}
			}

		});

		// A canvas to visualize the overall test results
		canvas = new Canvas(tests, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 4;
		gridData.heightHint = 20;
		canvas.setLayoutData(gridData);
		
		// This is updated automatically based on the pass/fail/error/deadlock values
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				canvas.setToolTipText("Pass: "+(int)pass+", Fail: "+(int)fail+", Error: "+(int)error+", Deadlock: "+(int)dead);
				Rectangle clientArea = canvas.getClientArea();
				
				// pass
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
				e.gc.fillRectangle(clientArea.x, clientArea.y, (int)(clientArea.width*(pass/total)), clientArea.height);
				
				// fail
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
				e.gc.fillRectangle((int)(clientArea.width*(pass/total)), 
						clientArea.y, (int)(clientArea.width*(fail/total)), clientArea.height);
				
				// error
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
				e.gc.fillRectangle((int)(clientArea.width*(pass/total))+(int)(clientArea.width*(fail/total)), 
					clientArea.y,(int)(clientArea.width*(error/total)), clientArea.height);
				
				// deadlock
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
				e.gc.fillRectangle((int)(clientArea.width*(pass/total))+(int)(clientArea.width*(fail/total))+
						(int)(clientArea.width*(error/total)),  clientArea.y, 
						(int)(clientArea.width*(dead/total)), clientArea.height);
			}
		});
		
		/**************************************************************************************************/
		
		// A group containing results for a selected test class
		Group testResults = new Group(composite, SWT.NULL);
		testResults.setText("Testing Detail");
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		testResults.setLayoutData(gridData);
		testResults.setLayout(new GridLayout(8, false));

		Label selectTest = new Label(testResults, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 5;
		selectTest.setLayoutData(gridData);
		selectTest.setText("Select a test to view detailed results:");
		
		// A drop-down menu to select an individual test class and see its results
		combo = new Combo(testResults, SWT.DROP_DOWN | SWT.READ_ONLY);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 3;
		combo.setLayoutData(gridData);
		
		// A second canvas to visualize the executions of a selected test
		// This one IS NOT updated automatically. It has a listener further down
		detailCanvas = new Canvas(testResults, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 8;
		gridData.heightHint = 20;
		detailCanvas.setLayoutData(gridData);
		
		/**************************************************************************************************/
		
		// A group containing only a tree of execution results
		Group executions = new Group(testResults, SWT.NULL);
		executions.setText("Select an execution to view raw output:");
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 8;
		executions.setLayoutData(gridData);
		executions.setLayout(new GridLayout(1, false));

		// A tree containing individual test executions which can be selected to see raw output
		resultTree = new Tree(executions, SWT.V_SCROLL | SWT.H_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		resultTree.setLayoutData(gridData);

		/**************************************************************************************************/

		// A group containing only a multi-line text for showing raw output files
		Group details = new Group(testResults, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 8;
		details.setLayoutData(gridData);
		details.setLayout(new GridLayout(1, false));
		details.setText("Raw Output");

		// A multi-line text to contain raw execution output
		testDetails = new Text(details, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		gridData.horizontalSpan = 8;
		gridData = new GridData(GridData.FILL_BOTH);
		testDetails.setLayoutData(gridData);

		// This listener fills the resultTree with the execution results
		combo.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
	    		// Find the selected test
				for(int i = 0; i < results.size(); i++) {
	    			if(results.get(i).getName().equals(combo.getText())) {
	    				selection = results.get(i);
	    				resultTree.removeAll();
	    				break;
	    			}
	    		}
		    		
	    		// Fill the tree with test executions
				for(int i = 0; i < selection.getExecutionResults().size(); i++) {
	    			TreeItem treeItem = new TreeItem(resultTree, SWT.NONE);
	    			treeItem.setText("Execution "+selection.getExecutionResults().get(i).getExecutionNumber()+": "+
	    						selection.getExecutionResults().get(i).getTestStatus());
	    		}
	    			
	    		// Fill the detail canvas based on selected test class
				detailCanvas.redraw();
	    		detailCanvas.addPaintListener(new PaintListener() {
	    			public void paintControl(PaintEvent e) {
	    				detailCanvas.setToolTipText("Pass: "+(int)selection.getPass()+", Fail: "+(int)selection.getFail()+
	    								", Error: "+(int)selection.getError()+", Deadlock: "+(int)selection.getDeadlock());
		    		            
	    				Rectangle clientArea = detailCanvas.getClientArea();
		    		            
	    		        // pass
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
						e.gc.fillRectangle(clientArea.x, clientArea.y, 
								(int)(clientArea.width*(selection.getPass()/selection.getTotal())), clientArea.height);
								
						// fail
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
						e.gc.fillRectangle((int)(clientArea.width*(selection.getPass()/selection.getTotal())), 
								clientArea.y, (int)(clientArea.width*(selection.getFail()/selection.getTotal())), 
								clientArea.height);
								
						// error
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
						e.gc.fillRectangle((int)(clientArea.width*(selection.getPass()/selection.getTotal())) + 
								(int)(clientArea.width*(selection.getFail()/selection.getTotal())), 
								clientArea.y,(int)(clientArea.width*(selection.getError()/selection.getTotal())), 
								clientArea.height);
								
						// deadlock
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
						e.gc.fillRectangle((int)(clientArea.width*(selection.getPass()/selection.getTotal())) + 
								(int)(clientArea.width*(selection.getFail()/selection.getTotal())) + 
								(int)(clientArea.width*(selection.getError()/selection.getTotal())), 
								clientArea.y, (int)(clientArea.width*(selection.getDeadlock()/selection.getTotal())), 
								clientArea.height);
	    			}
	    	    });
	      	} 
	    });

		// Listener for the resultTree. Fills the multi-line text with raw output
		resultTree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// Make sure the right test is selected
				for(int i = 0; i < results.size(); i++) {
	    			if(results.get(i).getName().equals(combo.getText())) {
	    				selection = results.get(i);
	    				break;
	    			}
	    		}
				
		    	// Find the execution and fill the text with the output file
				for(int i = 0; i < selection.getExecutionResults().size(); i++) {
					if(Integer.toString(selection.getExecutionResults().get(i).getExecutionNumber()).charAt(0) == ((TreeItem)event.item).getText().charAt(10)) {
						testDetails.setText(selection.getExecutionResults().get(i).getOutputFile());
						break;
					}
				}
		      } 
		 });

		/**************************************************************************************************/

    	// Push this button to begin running tests
		// Clears widgets and file, begins running tests
		run = new Button(composite, SWT.PUSH);
		run.setText("Run Tests");
		run.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
		        case SWT.Selection:
		        	clearResultWidgets();
		        	clearOutputFile();
		        	runTests();
				}
			}
		});

		// Stop everything (not recommended)
		cancel = new Button(composite, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
		        case SWT.Selection:
		        	syncTestRunnable.interrupt();
		        	getTestResults.interrupt();
				}
			}
		});

		testing.setControl(composite);
	}
	
	/**
	 * A placeholder method for instrumenting testing project source code,
	 *  presumably with the SyncDebugger TXL scripts. To be implemented by 
	 *  a future thesis student
	 * */
	public void instrumentSourceCode() {
		// unimplemented
	}
	
	/**
	 * This method begins running test and updates the UI with test results.
	 * */
	public void runTests() {
		if(doInstrumentation.getSelection()) {
			instrumentSourceCode();
		}
		
		File file = new File(testDir.getText());
    	
		// Get the number of .java files in the test directory and 
		// set the maximum value for the first progress bar
		progress.setMaximum(file.listFiles(new FilenameFilter() {
			public boolean accept(File f, String s) {
				if(s.contains("java")) return true;
				return false;
			}
    	}).length);
    	
    	// Set the maximum value for the second progress bar
		execProgress.setMaximum(Integer.valueOf(testCountAmnt.getText()));

    	// Initialize the instance of SyncTestRunner to pass the config values
		// to the test running thread
		runner = new SyncTestRunner(baseDir.getText(), sourceDir.getText(),
    			testDir.getText(), sleepAmnt.getText(), testCountAmnt.getText());

    	// Start the thread for running tests
    	syncTestRunnable = new Thread(new Thread(new SyncTestRunnable(runner)));
    	syncTestRunnable.start();
    	
    	// The thread for getting test results
    	getTestResults = new Thread() {
    		public void run() {
    			try {
    				URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
    				// Open the output file for reading
    				BufferedReader in = new BufferedReader(new FileReader(location.getFile()+"src/synctest/testing/syncTestOutput.txt"));
    				String line;
    				while(true) {
    					if(Thread.currentThread().isInterrupted()) break;
    					if((line = in.readLine()) != null) {
    						// grab the output line from the other thread and pass it on to updateResultWidgets
    						if(line.contains("finished")) {
    							// All tests finished
    							in.close();
    							updateResultWidgets(line);
    							break;
    						}
    						
    						updateResultWidgets(line);
    					} else {
    						// poll the file every n milliseconds
    						Thread.sleep(500);
    					}
    				}
    			} catch(Exception e) {
    				e.printStackTrace();
    			}
    		}
    	};
    	// Start the thread
    	getTestResults.start();
	}

	/**
	 * A method to attempt automatically finding the directories
	 * containing source code and test files in the base directory
	 * 
	 * @param path	The path to the testing project base directory
	 * */
	public void importDirs(String path) {
		File file = new File(path);
		String[] names = file.list();

		for(String name : names) {
			if(new File(path + "/" + name).isDirectory()) {
				if(name.contains("source") || name.contains("src")) {
					sourceDir.setText(path + "/" + name);
				} else if(name.contains("test") || name.contains("tst")) {
					testDir.setText(path + "/" + name);
				}
			}
		}
	}
	
	/**
	 * A method to parse test results and update the UI
	 * 
	 * @param line	The most recent line from the output file
	 * */
	public void updateResultWidgets(String line) {
		Display.getDefault().asyncExec(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				
				if(line.contains("Running")) {
					// A new set of tests is running, reset executions progress
					running.setText(line);
					execProgress.setSelection(0);
					currentTest = line.split(" ")[1];
					
				} else if(line.contains("Completed")) {
					// A set of tests has finished, update first progress bar/label
					// Create new result instance and add it to the drop-down widget
					progress.setSelection(progress.getSelection()+1);
					execRunning.setText("Execution 0/"+testCountAmnt.getText());
					
					results.add(new TestResult(currentTest, (Vector<ExecutionResult>) executionResults.clone()));
					executionResults.clear();
					
					combo.add(results.get(results.size()-1).getName());
					
				} else if(line.contains("Execution")) {
					// An execution has finished, update execution progress bar/label
					// Create a new execution result instance
					execProgress.setSelection(execProgress.getSelection()+1);
					String str[] = line.split(" ");
					execRunning.setText(str[0]+" "+str[1]);
					
					int number = Integer.parseInt(str[1].split("/")[0]);
					File output = new File(baseDir.getText()+"/out/"+currentTest+"-"+number+".txt");
					
					if(line.contains("Passed")) {
						pass += 1;total += 1;
						executionResults.add(new ExecutionResult(number, "pass", output));
					} else if(line.contains("Failed")) {
						fail +=1; total +=1;
						executionResults.add(new ExecutionResult(number, "fail", output));
					} else if(line.contains("Deadlocked")) {
						dead += 1; total +=1;
						executionResults.add(new ExecutionResult(number, "deadlock", output));
					} else if(line.contains("Error")) {
						error += 1; total +=1;
						executionResults.add(new ExecutionResult(number, "error", output));
					}
					
				} else if(line.contains("finished")) {
					// All tests/executions finished
					running.setText("All Tests Completed");
					execRunning.setText("All Executions Completed");
				}

				//update result text boxes
				if(passAmntIsFraction) {
					passAmnt.setText((int)pass+"/"+(int)total);
				} else {
					passAmnt.setText(String.format("%.2f", pass/total*100)+"%");
				}

				if(failAmntIsFraction) {
					failAmnt.setText((int)fail+"/"+(int)total);
				} else {
					failAmnt.setText(String.format("%.2f", fail/total*100)+"%");
				}

				if(errAmntIsFraction) {
					errAmnt.setText((int)error+"/"+(int)total);
				} else {
					errAmnt.setText(String.format("%.2f", error/total*100)+"%");
				}

				if(deadAmntIsFraction) {
					deadAmnt.setText((int)dead+"/"+(int)total);
				} else {
					deadAmnt.setText(String.format("%.2f", dead/total*100)+"%");
				}
			  }
		});


	}
	
	/**
	 * A method to clear the result widgets. This also resets all variables
	 * to zero/null so be careful!
	 * */
	public void clearResultWidgets() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				pass = 0; fail = 0; error = 0; dead = 0; total = 0;
				
				if(results != null) {
					results.clear();
				}
				
				if(executionResults != null) {
					executionResults.clear();
				}
				
				progress.setSelection(0);
				execProgress.setSelection(0);
				
				resultTree.deselectAll();
				
				selection = null;
				currentTest = null;
				
				canvas.redraw();
				detailCanvas.redraw();
				
				combo.removeAll();
				resultTree.removeAll();
				testDetails.setText("");

				if(passAmntIsFraction) {
					passAmnt.setText("000/000");
				} else {
					passAmnt.setText("0.00%");
				}

				if(failAmntIsFraction) {
					failAmnt.setText("000/000");
				} else {
					failAmnt.setText("0.00%");
				}

				if(errAmntIsFraction) {
					errAmnt.setText("000/000");
				} else {
					errAmnt.setText("0.00%");
				}

				if(deadAmntIsFraction) {
					deadAmnt.setText("000/000");
				} else {
					deadAmnt.setText("0.00%");
				}
			}
		});
	}

	/**
	 * A method to clear the output file of previous test results
	 * */
	public void clearOutputFile() {
		try {
			URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
			PrintWriter pw = new PrintWriter(location.getFile()+"src/synctest/testing/syncTestOutput.txt");
			pw.write(""); pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A method to print the result vector to a text file
	 * 
	 * @param path	The path to the desired output file
	 * */
	public void exportResults(String path) {
		String allResults = "";
		for(int i = 0; i < results.size(); i++) {
			allResults += results.get(i).toString();
		}
		
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			writer.println(allResults);
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setFocus() {}
}
