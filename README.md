# GoGui
A graphical user interface for the asien boardgame of Go. Follow the instructions below to build on your computer.
 
## On macOS
### Presettings
First make sure you have installed:
* Java 6: https://support.apple.com/kb/DL1572
* Ant 1.9: https://ant.apache.org/bindownload.cgi

For documentations you need also:
* docbook xsl: https://sourceforge.net/projects/docbook/files/#files

The developing works with Netbeans 7.4 and Java 6 on a macOS system.

### Build
Clone the GoGui package from this site and uncompress it. Then type 

    cd /path/of/the/goguipackage
    ant
    java -jar ./lib/gogui.jar
or

    cd /path/of/the/goguipackage
    ant gogui.app
    open ./build/gogui.app

on the console.

That's it.
If you got problems, send a mail. This project is licensed under the terms of the gpl-3.0 license.
