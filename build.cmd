@echo off

setlocal
call gis_setenv.cmd

cd /d "%MEDIA%\gis"
IF ERRORLEVEL 1 GOTO error
mkdir apache-tomcat-%TOMCAT_VERSION%-windows-x86
IF ERRORLEVEL 1 GOTO error
"%SEVENZIP%" x -oapache-tomcat-%TOMCAT_VERSION%-windows-x86 apache-tomcat-%TOMCAT_VERSION%-windows-x86.zip
IF ERRORLEVEL 1 GOTO error

echo gs-main
cd /d %GEOSERVER_SRC%\src\main
IF ERRORLEVEL 1 GOTO error
call mvn %MVN_FLAGS% package >NUL
IF ERRORLEVEL 1 GOTO error
copy /y %GEOSERVER_SRC%\src\main\target\gs-main-%GEOSERVER_VERSION%.jar %MEDIA%\gis\apache-tomcat-%TOMCAT_VERSION%-windows-x86\webapps\geoserver\WEB-INF\lib
IF ERRORLEVEL 1 GOTO error

echo gs-sec-ldap
cd /d %GEOSERVER_SRC%\src\security\ldap
IF ERRORLEVEL 1 GOTO error
call mvn %MVN_FLAGS% package >NUL
IF ERRORLEVEL 1 GOTO error
copy /y %GEOSERVER_SRC%\src\security\ldap\target\gs-sec-ldap-%GEOSERVER_VERSION%.jar %MEDIA%\gis\apache-tomcat-%TOMCAT_VERSION%-windows-x86\webapps\geoserver\WEB-INF\lib
IF ERRORLEVEL 1 GOTO error

echo gs-restconfig
cd /d %GEOSERVER_SRC%\src\restconfig
IF ERRORLEVEL 1 GOTO error
call mvn %MVN_FLAGS% package >NUL
IF ERRORLEVEL 1 GOTO error
copy /y %GEOSERVER_SRC%\src\restconfig\target\gs-restconfig-%GEOSERVER_VERSION%.jar %MEDIA%\gis\apache-tomcat-%TOMCAT_VERSION%-windows-x86\webapps\geoserver\WEB-INF\lib
IF ERRORLEVEL 1 GOTO error

echo gs-wms
cd /d %GEOSERVER_SRC%\src\wms
IF ERRORLEVEL 1 GOTO error
call mvn %MVN_FLAGS% package >NUL
IF ERRORLEVEL 1 GOTO error
copy /y %GEOSERVER_SRC%\src\wms\target\gs-wms-%GEOSERVER_VERSION%.jar %MEDIA%\gis\apache-tomcat-%TOMCAT_VERSION%-windows-x86\webapps\geoserver\WEB-INF\lib
IF ERRORLEVEL 1 GOTO error

echo gs-fka-search
cd /d %GEOSERVER_SRC%\src\community\fka-search
IF ERRORLEVEL 1 GOTO error
call mvn %MVN_FLAGS% package >NUL
IF ERRORLEVEL 1 GOTO error
copy /y %GEOSERVER_SRC%\src\community\fka-search\target\gs-fka-search-%GEOSERVER_VERSION%.jar %MEDIA%\gis\apache-tomcat-%TOMCAT_VERSION%-windows-x86\webapps\geoserver\WEB-INF\lib
IF ERRORLEVEL 1 GOTO error

cd /d "%MEDIA%\gis\apache-tomcat-%TOMCAT_VERSION%-windows-x86"
IF ERRORLEVEL 1 GOTO error
"%SEVENZIP%" u ..\apache-tomcat-%TOMCAT_VERSION%-windows-x86.zip webapps\geoserver\WEB-INF\lib\*.jar >NUL
IF ERRORLEVEL 1 GOTO error
cd /d "%MEDIA%\gis"
IF ERRORLEVEL 1 GOTO error
rd /s /q apache-tomcat-%TOMCAT_VERSION%-windows-x86
IF ERRORLEVEL 1 GOTO error

GOTO ok

:error
echo ERROR: failed
goto end

:ok
echo OK: success

:end
endlocal
