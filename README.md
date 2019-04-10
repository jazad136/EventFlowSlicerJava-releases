# EventFlowSlicer for Java.

Maintainer email: saddler@huskers.unl.edu

EventFlowSlicer, created for Java Swing. EventFlowSlicer helps developers test GUIs by building a graph containing all possible interactions that can be extracted or “scraped” from a running GUI, and using that graph to create replay-able test scenarios. Built in are state-of-the-art techniques that enable EFS to "see and replay interactions" available on a GUI with the help of assisted capture. Using algorithms devised as part of the 2016 thesis, it will link these actions together in ways that resemble workflows a real user would perform.

The tool itself is capable of test suite customization, test suite generation, and test suite replay -- its method of specifying test cases through "assisted capture", however, is a unique feature that sets EFS apart from many other testing tools. There are a number of "constraints" and "rules" defined and that are customizable that give the tester power to express boundaries on the scope of test cases desired. One key to this algorithm simply invokes a form of depth first traversal to discover test cases, while at each traversal/step, applying constraints and refuting paths in a CSP-like manner. 

The plan is at this time, that this software will support all constraints defined in this thesis at its release time on GitHub and forward.

This is the repository for the Java EventFlowSlicer application, dependent only on the GUITAR framework from University of Maryland, (https://sourceforge.net/projects/guitar/), Args4J http://args4j.kohsuke.org/, and Apache Log4J (https://logging.apache.org/log4j/1.2/license.html) for use in testing Java Swing desktop applications. You can get the complete package, EFS with all its dependencies by following the steps under "Installation" below. 

## Installation

[April 2019] You can download the artifacts as a standalone complete software package at this location on Maven Central: https://repo1.maven.org/maven2/edu/unl/cse/efs/eventflowslicerjava-releases/0.0.2/

The steps for installation are simple:

**(Prerequisites: A Java 8 or later installation.)** 
1. Download an eventflowslicer-zip-with-dependencies file after following the link above, which will contain the product. 
2. Unzip the (zip-with-dependencies) zip file to a location on your file system, preferably an empty directory specifically designated for EFS to run. 
3. (After Java 8 or above is properly installed) Double-click the eventflowslicer(…).jar file in the folder of the extracted zip file to run. 

    You are running EFS. 

### Usage: 
EventFlowSlicer is a GUI program, with many command line arguments available. For ease of use, double clicking the .jar file will display a GUI tool. To use it, you can follow along in this 5-min. video we submitted to and utilized at one of our previous conferences. 

[https://youtu.be/hw7WYz8WYVU](https://youtu.be/hw7WYz8WYVU)

EventFlowSlicer has five concretely-defined steps, in which at least two (2) make use of three (3) human-readable XML files called the "EFG" the "GUI" and the "Rules TaskList". EFS will automatically create these as you work, but you can learn about how these files are created and their use from the published papers. For a limited time, the artifacts themselves for various kinds of GUI tasks you can execute yourself will be posted to the following website: [http://cse.unl.edu/~myra/artifacts/EventFlowSlicer](http://cse.unl.edu/~myra/artifacts/EventFlowSlicer)

## Appeal and Audience

EventFlowSlicer is an application built to help and support the GUI testing and Human Computer Interaction research communities. Often times there are few alternative methods available allowing researchers in our field a clean-cut compromise between creating GUI tests for study, and specifying the ones that truthfully mimic real human users.
#### EventFlowSlicer is a tool that trades directly specifying all executable GUI scenarios via point-and-click...
#### with specifying just the important widgets via point-and-click, and leaving the rest to a search algorithm.

Search is done using some of the constraints defined contained in [Swearngin's 2013 Conference Paper (affiliation: University of Nebraska, Lincoln)](http://digitalcommons.unl.edu/cseconfwork/260/)[1]. More constraints were defined as more test suite use cases became pertinent in our follow up studies.

## More on the History of EventFlowSlicer / Published Papers
In Jonathan Saddler's 2016 Thesis, _EventrFlowSlicer: A Goal-based Test Case Generation Strategy for Graphical User Interfaces_, a thorough treatment of RE-based GUI testing is covered in detail. There are a number of "constraints" defined in this thesis that when applied give the tester power to express boundaries on the scope of test cases desired. Underlying all this is an algorithm that involves depth first traversal to discover test cases in a CSP-like manner. 

EFS trades off the ability to directly click to capture for an *algorithm*-based perspective that relies on *search.* Using EFS, real human GUI scenarios are "found" for a specific application the researcher chooses, amongst all available human-accessible scenarios amongst the widgets the researcher specifies. As proof of workmanship, these *scenarios* or **test cases** are executable on the GUI itself. You can use **constraints** to bound the search algorithm to look for certain widget patterns and output those test cases matching the criteria. 

The plan is at this time, that this software will support all constraints defined in this thesis at its release time on GitHub and forward.
 
If you are interested in the technique and a walkthrough of this tools use and its larger impact in the community, see below: 

**Published Papers (for walkthroughs and background)**
We have published for the community two copies of this paper in top-tier Software Engineering conferences. 

The technique of EventFlowSlicer: [link to easily-accessible, downloadable version page](https://dl.acm.org/citation.cfm?doid=2994291.2994293)
by Jonathan A. Saddler and Myra B. Cohen. 

and finally release of the tool itself: 

[link to conference website page](https://ieeexplore.ieee.org/document/8115711)

[link to downloadable version](https://cse.unl.edu/~jsaddle/paper_images/preprint.pdf)
by Jonathan A. Saddler and Myra B. Cohen.

## For more information

email the maintainer Jonathan A. Saddler @ saddler@huskers.unl.edu

*[1] Swearngin, A, Cohen, Myra B., John, B. E., and Bellamy, R. K. E. "Human Performance Regression Testing." Software Engineering (ICSE), 2013 35th International Conference on Year: 2013 Pages: 152 - 161, DOI: 10.1109/ICSE.2013.6606561*
