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

For more details see the guide [[PDF]](./src/main/resources/Project/guide.pdf), which you can also find
in the app via ``Help > Guide``.


**▶️ How to run project using Maven**

In the root directory, run ``mvn clean javafx:run@anatomyExplorer -Pwindows/mac/linux``

Requires Maven, Java 24 or later.

******
**💻 How to build installer for your operating system using Maven**

You can find installers for Windows, Linux and Mac in the release. You can also build installers yourself like this:

In the root directory, run ``mvn clean package -Pwindows/linux/mac``.

You will find the installer in ``target/dist/``.

Note that building installers may require operating system specific tools, like Wix on Windows or dpkg-deb on Linux to be installed on your machine.

******
**💾 Data**

3D anatomy data by: BodyParts3D, © The Database Center for Life Science licensed under CC Attribution 4.0 International.

- Webpage: https://dbarchive.biosciencedbc.jp/en/bodyparts3d/desc.html
- Data: https://dbarchive.biosciencedbc.jp/data/bodyparts3d/

*******
**🗒️ Project**

This software is the result of my master's thesis under supervision of Prof. Daniel Huson. It evolved out of the course 'Advanced Java for Bioinformatics (2026)', also by Prof. Daniel Huson at the University of Tübingen. Check out his lab's [GitHub](https://github.com/husonlab) for lots of useful and exciting bioinformatics software! 🧪

******

Author: Luis Reimer
