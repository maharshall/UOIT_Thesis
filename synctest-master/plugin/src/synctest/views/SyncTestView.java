package synctest.views;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.*;

import synctest.testing.SyncTestRunnable;
import synctest.testing.SyncTestRunner;
import synctest.util.ExecutionResult;
import synctest.util.Result;

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

public class SyncTestView extends ViewPart {

	public static final String ID = "synctest.views.SyncTestView";

	private synctest.testing.SyncTestRunner runner;

	private double pass = 0, fail = 0, error = 0, dead = 0, total = 0;

	private Vector<Result> results = new Vector<Result>();
	private Vector<ExecutionResult> executionResults = new Vector<ExecutionResult>();
	private Result selection;
	private String currentTest;

	IWorkbench 		workbench 	= PlatformUI.getWorkbench();
	ISharedImages 	images 		= workbench.getSharedImages();


	//CONFIG TAB
	CTabItem 		config;
	CTabItem		testing;
    Composite 		composite;
    GridData 		gridData;
    GridLayout 		gridLayout;

    Text 			baseDir, sourceDir, testDir;

    Text 			sleepAmnt, testCountAmnt;
    Scale 			threshold;

    Button 			passBox, failBox, deadBox;

    //TESTING TAB
	ProgressBar 	progress, execProgress;
    Label			running, execRunning, testLabel, execLabel;
    Text 			passAmnt, failAmnt, errAmnt, deadAmnt;
    
    boolean			passAmntIsFraction = false, failAmntIsFraction = false,
    				errAmntIsFraction = false,  deadAmntIsFraction = false;

    Canvas 			canvas;
    Canvas 			detailCanvas;

    Combo 			combo;
    Tree 			resultTree;
    Text 			testDetails;

    Button 			run;

	public SyncTestView() {}

	public void createPartControl(Composite parent) {
	    //Create tabs for configuration and testing
		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		createConfigTab(tabs);
		createTestingTab(tabs);
	}

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

		Button findDirs = new Button(folders, SWT.CHECK);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.horizontalSpan = 2;
		findDirs.setLayoutData(gridData);
		findDirs.setText("Automatically find souce and test directories");

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

//		Label thresholdLbl = new Label(settings, SWT.NULL);
//		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
//		thresholdLbl.setLayoutData(gridData);
//		thresholdLbl.setText("Pass Threshold: ");
//
//		threshold = new Scale(settings, SWT.HORIZONTAL);
//		gridData = new GridData(GridData.FILL_HORIZONTAL);
//		gridData.horizontalSpan = 4;
//		threshold.setLayoutData(gridData);
//		threshold.setSelection(70);
//
//		Label scaleLbl = new Label(settings, SWT.NULL);
//		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//		scaleLbl.setLayoutData(gridData);
//		scaleLbl.setText(threshold.getSelection() + "%");
//
//		threshold.addListener(SWT.Selection, new Listener() {
//		      public void handleEvent(Event event) {
//		    	  scaleLbl.setText(threshold.getSelection() + "%");
//		      }
//		    });


		/**************************************************************************************************/

//		Group parser = new Group(composite, SWT.NULL);
//		parser.setText("Parser Settings");
//		gridData = new GridData(GridData.FILL_HORIZONTAL);
//		parser.setLayoutData(gridData);
//		parser.setLayout(new GridLayout(1, false));
//
//		passBox = new Button(parser, SWT.CHECK);
//		passBox.setText("Count Test Passes");
//		passBox.setSelection(true);
//
//		failBox = new Button(parser, SWT.CHECK);
//		failBox.setText("Count Test Failures");
//		failBox.setSelection(true);
//
//		deadBox = new Button(parser, SWT.CHECK);
//		deadBox.setText("Count Test Deadlocks");
//		deadBox.setSelection(true);

		config.setControl(composite);
	}

	private void createTestingTab(CTabFolder tabFolder) {
		testing = new CTabItem(tabFolder, SWT.NONE);
	    testing.setText("Testing");

	    composite = new Composite(tabFolder, SWT.NULL);
		gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);
		gridLayout = new GridLayout(2, false);
		composite.setLayout(gridLayout);
		
		/**************************************************************************************************/

		Group tests = new Group(composite, SWT.NULL);
		tests.setText("Testing Overview");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		tests.setLayoutData(gridData);
		tests.setLayout(new GridLayout(4, false));

		// A toolbar containing some useful buttons
		ToolBar toolbar = new ToolBar(tests, SWT.NULL);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 4;
		toolbar.setLayoutData(gridData);

		// Button to clear all widgets
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

		// Label to show currently running test
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
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
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
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
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
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
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
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
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
		
		canvas.addPaintListener(new PaintListener() {
			// Automatically update canvas as the numbers come in
			public void paintControl(PaintEvent e) {
				canvas.setToolTipText("Pass: "+(int)pass+", Fail: "+(int)fail+", Error: "+(int)error+", Deadlock: "+(int)dead);
				Rectangle clientArea = canvas.getClientArea();
				
				//pass
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
				e.gc.fillRectangle(clientArea.x, clientArea.y, (int)(clientArea.width*(pass/total)), clientArea.height);
				
				//fail
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
				e.gc.fillRectangle((int)(clientArea.width*(pass/total)), 
						clientArea.y, (int)(clientArea.width*(fail/total)), clientArea.height);
				
				//error
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
				e.gc.fillRectangle((int)(clientArea.width*(pass/total))+(int)(clientArea.width*(fail/total)), 
					clientArea.y,(int)(clientArea.width*(error/total)), clientArea.height);
				
				//deadlock
				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
				e.gc.fillRectangle((int)(clientArea.width*(pass/total))+(int)(clientArea.width*(fail/total))+
						(int)(clientArea.width*(error/total)),  clientArea.y, 
						(int)(clientArea.width*(dead/total)), clientArea.height);
			}
		});
		
		/**************************************************************************************************/
		
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
		
		// A drop-down menu to select an individual test and see its results
		combo = new Combo(testResults, SWT.DROP_DOWN | SWT.READ_ONLY);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 3;
		combo.setLayoutData(gridData);
		
		// A second canvas to visualize the executions of a selected test
		detailCanvas = new Canvas(testResults, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 8;
		gridData.heightHint = 20;
		detailCanvas.setLayoutData(gridData);
		
		/**************************************************************************************************/
		
		Group executions = new Group(testResults, SWT.NULL);
		executions.setText("Select an execution to view raw output:");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 8;
		gridData.heightHint = 200;
		executions.setLayoutData(gridData);
		executions.setLayout(new GridLayout(1, false));

		// A window containing individual executions which can be selected to see raw output
		resultTree = new Tree(executions, SWT.V_SCROLL | SWT.H_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		resultTree.setLayoutData(gridData);

		/**************************************************************************************************/

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

		combo.addListener(SWT.Selection, new Listener() {
			// Get the selected test, update the canvas and tree
			public void handleEvent(Event event) {
	    		for(int i = 0; i < results.size(); i++) {
	    			if(results.get(i).getName().equals(combo.getText())) {
	    				selection = results.get(i);
	    				resultTree.removeAll();
	    				break;
	    			}
	    		}
		    		
	    		for(int i = 0; i < selection.getExecutionResults().size(); i++) {
	    			TreeItem treeItem = new TreeItem(resultTree, SWT.NONE);
	    			treeItem.setText("Execution "+selection.getExecutionResults().get(i).getExecutionNumber()+": "+
	    						selection.getExecutionResults().get(i).getTestStatus());
	    		}
	    			
	    		detailCanvas.redraw();
	    		detailCanvas.addPaintListener(new PaintListener() {
	    			public void paintControl(PaintEvent e) {
	    				detailCanvas.setToolTipText("Pass: "+(int)selection.getPass()+", Fail: "+(int)selection.getFail()+
	    								", Error: "+(int)selection.getError()+", Deadlock: "+(int)selection.getDeadlock());
		    		            
	    				Rectangle clientArea = detailCanvas.getClientArea();
		    		            
	    		        //pass
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
						e.gc.fillRectangle(clientArea.x, clientArea.y, 
								(int)(clientArea.width*(selection.getPass()/selection.getTotal())), clientArea.height);
								
						//fail
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
						e.gc.fillRectangle((int)(clientArea.width*(selection.getPass()/selection.getTotal())), 
								clientArea.y, (int)(clientArea.width*(selection.getFail()/selection.getTotal())), 
								clientArea.height);
								
						//error
						e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
						e.gc.fillRectangle((int)(clientArea.width*(selection.getPass()/selection.getTotal())) + 
								(int)(clientArea.width*(selection.getFail()/selection.getTotal())), 
								clientArea.y,(int)(clientArea.width*(selection.getError()/selection.getTotal())), 
								clientArea.height);
								
						//deadlock
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
		
		resultTree.addListener(SWT.Selection, new Listener() {
			// Ensure the right test is selected, find the execution and set the raw output
			public void handleEvent(Event event) {
				for(int i = 0; i < results.size(); i++) {
	    			if(results.get(i).getName().equals(combo.getText())) {
	    				selection = results.get(i);
	    				break;
	    			}
	    		}
				
		    	for(int i = 0; i < selection.getExecutionResults().size(); i++) {
					if(Integer.toString(selection.getExecutionResults().get(i).getExecutionNumber()).charAt(0) == ((TreeItem)event.item).getText().charAt(10)) {
						testDetails.setText(selection.getExecutionResults().get(i).getOutputFile());
						break;
					}
				}
		      } 
		 });

		/**************************************************************************************************/

    	run = new Button(composite, SWT.PUSH);
		run.setText("Run Tests");
		run.addListener(SWT.Selection, new Listener() {
			// Clear widgets and file, begin running tests
			public void handleEvent(Event event) {
				switch (event.type) {
		        case SWT.Selection:
		        	clearResultWidgets();
		        	clearOutputFile();
		        	runTests();
				}
			}
		});

		Button cancel = new Button(composite, SWT.PUSH);
		cancel.setText("Cancel");

		testing.setControl(composite);
	}
	
	public void runTests() {
		File file = new File(testDir.getText());
    	
		progress.setMaximum(file.listFiles(new FilenameFilter() {
			public boolean accept(File f, String s) {
				if(s.contains("java")) return true;
				return false;
			}
    	}).length); //lengthy way to get number of tests in directory
    	
    	execProgress.setMaximum(Integer.valueOf(testCountAmnt.getText()));

    	runner = new SyncTestRunner(baseDir.getText(), sourceDir.getText(),
    			testDir.getText(), sleepAmnt.getText(), testCountAmnt.getText());

    	// Start the thread for running tests
    	(new Thread(new SyncTestRunnable(runner))).start();
    	
    	// Start the thread for getting test results
    	(new Thread() {
    		public void run() {
    			try {
    				URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
    				BufferedReader in = new BufferedReader(new FileReader(location.getFile()+"src/synctest/testing/syncTestOutput.txt"));
    				String line;
    				while(true) {
    					if((line = in.readLine()) != null) {
    						// grab the output line from the other thread and pass it on to update UI
    						if(line.contains("finished")) {
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
    	}).start();
	}

	
	public void importDirs(String path) {
		// Try to find the source and test directories in the base directory
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
					// Create new result instance and add it to the combo
					progress.setSelection(progress.getSelection()+1);
					execRunning.setText("Execution 0/"+testCountAmnt.getText());
					
					results.add(new Result(currentTest, (Vector<ExecutionResult>) executionResults.clone()));
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
	
	public void clearResultWidgets() {
		// Clear all widgets that display results
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

	
	public void clearOutputFile() {
		//Clear the output file so the results unaffected
		try {
			URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
			PrintWriter pw = new PrintWriter(location.getFile()+"src/synctest/testing/syncTestOutput.txt");
			pw.write(""); pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	
	public void exportResults(String path) {
		// Print the result vector to a text file
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
