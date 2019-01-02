# EventFlowSlicer for Java.

Maintainer email: saddler@huskers.unl.edu

Release repository for EventFlowSlicer tool, created for the Java Swing GUI Architecture.
The EventFlowSlicer applied technique helps developers test C# and Java GUI's with confidence, using scientific advances in state of the art model-and-constraint-your-task-based, GUI-RE generation - the term "RE" denoting the art of "reverse-engineering" a running GUI app on your desktop to extract precisely the controls that are available to the user plus user-accessible actions, for the purpose of test case generation.

This is the repository for the Java EventFlowSlicer application, dependent only on the GUITAR framework from University of Maryland, (https://sourceforge.net/projects/guitar/), Args4J http://args4j.kohsuke.org/, and Apache Log4J (https://logging.apache.org/log4j/1.2/license.html) for use in testing Java Swing desktop applications. 

### EventFlowSlicer
In Jonathan Saddler's 2016 Thesis, _EventFlowSlicer: A Goal-based Test Case Generation Strategy for Graphical User Interfaces_, a thorough treatment of RE-based GUI testing is covered in detail. There are a number of "constraints" defined in this thesis that when applied give the tester power to express boundaries on the scope of test cases desired. Underlying all this is an algorithm that involves depth first traversal to discover test cases in a CSP-like manner. The plan is at this time, that this software will support all constraints defined in this thesis at its release time on GitHub and forward.
 
If you are interested in the technique and a walkthrough of this tools use and its larger impact in the community, see below: 

## Published Papers (for walkthroughs and background)
We have published for the community two copies of this paper in top-tier Software Engineering conferences. 

The technique of EventFlowSlicer: [link to easily-accessible, downloadable version page -- click the item beside "Full Text"](https://dl.acm.org/citation.cfm?doid=2994291.2994293)
by Jonathan A. Saddler and Myra B. Cohen. 

and finally release of the tool itself: 

[link to conference website page](https://ieeexplore.ieee.org/document/8115711)

[link to downloadable version](https://cse.unl.edu/~jsaddle/paper_images/preprint.pdf)
by Jonathan A. Saddler and Myra B. Cohen.

## Artifacts

You may also be interested in artifacts for use with testing EventFlowSlicer. EventFlowSlicer has 5 steps in which at least 2 make use of human-readable XML files called EFG files and GUI files and Rules files. You can learn about how these files are created from the published papers. For a limited time, the artifacts themselves for various kinds of GUI tasks you can execute yourself will be posted to the following website: [cse.unl.edu/~myra/artifacts/EventFlowSlicer](cse.unl.edu/~myra/artifacts/EventFlowSlicer)

#### UPDATE: You may also be interested in downloading this tool as a dependency!

[Jan. 2019] Today we're announcing that we will soon host the ability to check out this project via Gradle and Maven. 

This will make it simple for you to run scripts and automate setup of the tool and its dependencies in a few keystrokes and small downloads. 

Check back soon for more details. 

## Appeal and Audience

EventFlowSlicer is an application built to help and support the GUI testing and Human Computer Interaction research communities. Often times there are few alternative methods available allowing researchers in this field a clean-cut compromise between creating GUI tests for study, and specifying the ones that truthfully mimic real human users.
#### EventFlowSlicer is a tool that trades of directly specifying every executable scenario via point-and-click...
#### with specifying just the clickable widgets via point-and-click, and leaving the rest to a search algorithm.

 EFS trades off the ability to directly click to capture for an *algorithm*-based perspective that relies on *search.* Using EFS, real human GUI scenarios are "found" for a specific application the researcher chooses, amongst all available human-accessible scenarios amongst the widgets the researcher specifies. As proof of workmanship, these *scenarios* or **test cases** are executable on the GUI itself. You can use **constraints** to bound the search algorithm to look for certain widget patterns and output those test cases matching the criteria. 

Search is done using some of the constraints defined contained in [Swearngin's 2013 Conference Paper (affiliation: University of Nebraska, Lincoln)](http://digitalcommons.unl.edu/cseconfwork/260/)[1]. More constraints were defined as more uses became pertinent.

email the maintainer Jonathan Saddler @ saddler@huskers.unl.edu

*[1] Swearngin, A, Cohen, Myra B., John, B. E., and Bellamy, R. K. E. "Human Performance Regression Testing." Software Engineering (ICSE), 2013 35th International Conference on Year: 2013 Pages: 152 - 161, DOI: 10.1109/ICSE.2013.6606561*
