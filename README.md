**🫀 What is this?**

This program allows you to study human anatomy in 3D:

- Navigate anatomy parts via a tree hierarchy
- Select what you want to see and hit ``Draw in 3D``
- Create cross-sections
- Take quizzes

Additional features:

- load any custom tree hierarchy
- edit tree hierarchies
- visualize OBJ files

For more details, also see the guide [[PDF]](Project/guide.pdf), which you can also find
in the app via ``Help > Guide``.


**▶️ How to run project using Maven**

In the root directory, run ``mvn clean javafx:run@anatomyExplorer -Pwindows/mac/linux``

******
**💻 How to build installer for your operating system**

In the root directory, run ``mvn clean package -Pwindows/linux/mac``.

You will find the installer in ``target/dist/``.

******
**🗒️Credits:**

3D anatomy data by: BodyParts3D, © The Database Center for Life Science licensed under CC Attribution 4.0 International.

******

Author: Luis Reimer
