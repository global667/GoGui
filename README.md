# GoGui
A graphical user interface for the asian board game of Go. Follow the instructions below to build on your computer.
 
## On macOS
### Presetting
First make sure you have installed:
* Java: https://jdk.java.net/
* Ant: https://ant.apache.org/bindownload.cgi

* IntelliJ: https://www.jetbrains.com/de-de/idea/download/#section=mac

For the documentation, you need also:
* docbook xsl: https://github.com/docbook/xslt10-stylesheets

The developing is tested with Netbeans or IntelliJ and Java 8/11 on a macOS Catalina system.

### Build
Clone the GoGui package from this site and uncompress it. Then type: 

    cd /path/of/the/goguipackage
    ant
    java -jar ./lib/gogui.jar
Or:

    cd /path/of/the/goguipackage
    ant gogui.app
    open ./build/gogui.app

On the console.

On IntelliJ or Netbeans there is the option to import a project from GitHub. Do this with the address:

    https://github.com/global667/GoGui.git 
    
That's it.
If you got problems, send a mail or put a ticket on [issues](https://github.com/global667/GoGui/issues). This project is licensed under the terms of the gpl-3.0 license.
