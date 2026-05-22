**🫀 What is this?**

This program allows you to explore human anatomy in 3D:

- Navigate anatomy parts via two tree hierarchies
- Select what you want to see and hit ``Draw in 3D``
- Use coloring and animations for better understanding

For more details, also see [Doc.html](Project/doc/guide/HTML/main.html), which you can also find
in the app via ``About > Guide``, or watch the short [video](src/main/resources/Project/video.mp4).


**▶️ How to run project using Maven**

Navigate to ``AnatomyViewer3d/`` and run ``mvn clean javafx:run@anatomyExplorer``

Requires Java 21 or later.

******
**💻 How to build distributable image for Windows**

Navigate to `AnatomyViewer3d\` and run ``mvn clean javafx:jlink@anatomyExplorer``

The image contains all necessary dependencies, resources and a JRE to run the program. 

Run via ``\target\image\bin\anatomyExplorer.bat``

******
**🗒️Credits:**

3D anatomy data by: BodyParts3D, © The Database Center for Life Science licensed under CC Attribution 4.0 International.

The basic functionality of tree view and 3D view is adapted from previous work done in cooperation with Niklas Gerbes.

******

Author: Luis Reimer
