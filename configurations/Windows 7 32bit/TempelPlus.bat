@echo off

cd %~dp0
SET JAVA="C:\java\jdk1.7.0_13\bin\java"

echo Starting TempelPlus from Windows Batch
echo Using Java: %JAVA%

SET firstNineParameters= %1 %2 %3 %4 %5 %6 %7 %8 %9

REM Shift 10 times
FOR /L %%A IN (1,1,10) DO SHIFT

SET lastNineParameters=%0 %1 %2 %3 %4 %5 %6 %7 %8 %9
SET parameters= %firstNineParameters% %lastNineParameters%

%JAVA% -Dfile.encoding=UTF-8 -Xmx1024m -jar Run.jar %parameters%