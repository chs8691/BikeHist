@echo off
set fileName=ic_launcher.png
set svgName=launcherV3.svg 
set sizeM=48
set sizeH=72
set sizeXH=96
set sizeXXH=144
set sizeXXXH=192
set sizeStore=512
set dirM=drawable-mdpi
set dirH=drawable-hdpi
set dirXH=drawable-xhdpi
set dirXXH=drawable-xxhdpi
set dirXXXH=drawable-xxxhdpi
set dirStore=store

REM Delete all existing files
del %dirM% /S /Q
del %dirH% /S /Q
del %dirXH% /S /Q
del %dirXXH% /S /Q
del %dirXXXH% /S /Q
del %dirStore% /S /Q

REM Delete directories
rmdir %dirM%
rmdir %dirH%
rmdir %dirXH%
rmdir %dirXXH%
rmdir %dirXXXH%
rmdir %dirStore%

REM Create export directories
mkdir %dirM%
mkdir %dirH%
mkdir %dirXH%
mkdir %dirXXH%
mkdir %dirXXXH%
mkdir %dirStore%

REM Create new images
start d:\Progs\Inkscape\inkscape.exe --export-png=%dirM%\%fileName% --export-width=%sizeM% --export-height=%sizeM% --file=%svgName%
start d:\Progs\Inkscape\inkscape.exe --export-png=%dirStore%\%fileName% --export-width=%sizeStore% --export-height=%sizeStore% --file=%svgName%
start d:\Progs\Inkscape\inkscape.exe --export-png=%dirH%\%fileName% --export-width=%sizeH% --export-height=%sizeH% --file=%svgName%
start d:\Progs\Inkscape\inkscape.exe --export-png=%dirXH%\%fileName% --export-width=%sizeXH% --export-height=%sizeXH% --file=%svgName%
start d:\Progs\Inkscape\inkscape.exe --export-png=%dirXXXH%\%fileName% --export-width=%sizeXXXH% --export-height=%sizeXXXH% --file=%svgName%
start d:\Progs\Inkscape\inkscape.exe --export-png=%dirXXH%\%fileName% --export-width=%sizeXXH% --export-height=%sizeXXH% --file=%svgName%
