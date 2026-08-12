@echo off
set JAVA_HOME=G:\C\OHCP\.local\jdk-17\jdk-17.0.19+10
cd /d G:\C\OHCP
call mvnw.cmd clean compile
pause
