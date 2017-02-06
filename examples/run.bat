@echo off
:start
java -jar studentTester.jar -testroot %cd%\testRoot\ -contentroot %cd%\contentRoot\ -temproot %cd%\target
pause
goto start