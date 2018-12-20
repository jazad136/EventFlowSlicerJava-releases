# EventFlowSlicer for Java.
email: saddler@huskers.unl.edu
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

email the maintainer Jonathan Saddler @ saddler@huskers.unl.edu
