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

Read the [thesis](./additional_materials/Thesis.pdf) to understand how the database is used in this project and which modifications were made to it.
*******
**🗒️ Project**

This software is the result of my master's [thesis](./additional_materials/Thesis.pdf) under supervision of Prof. Daniel Huson. It evolved out of the course 'Advanced Java for Bioinformatics (2026)', also by Prof. Daniel Huson at the University of Tübingen.
Read the thesis to understand how the project developed. Check out the [GitHub](https://github.com/husonlab) page of Prof. Huson's lab for lots of useful and exciting bioinformatics software! 🧪

*******
**⚖️ License information**

Anatomy Explorer Copyright © 2026 Luis Reimer

This project is licensed under [GPLv3](./LICENSE).

Third-party components:

- JavaFX
  Copyright © Oracle and contributors
  Licensed under [GPLv2 with the Classpath Exception](./licenses/JavaFX-GPL-2.0-CE.txt).

- PDFViewFX (see the [repository](https://github.com/dlsc-software-consulting-gmbh/PDFViewFX)) Licensed under [Apache License 2.0](./licenses/Apache-2.0.txt).

- BodyParts3D
  © The Database Center for Life Science
  Licensed under [CC BY 4.0](./licenses/CC-BY-4.0.txt).
  https://creativecommons.org/licenses/by/4.0/
