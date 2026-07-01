@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://  wrapper
@REM Maven Wrapper script for Windows

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"
set MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"

@REM Determine the Java command to use
if NOT "%JAVA_HOME%"=="" (
    set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_EXE="java"
)

@REM Check if wrapper jar exists, if not download it
if not exist %MAVEN_WRAPPER_JAR% (
    echo Downloading Maven Wrapper...

    @REM Try to find the wrapper URL from properties
    for /f "usebackq tokens=1,2 delims==" %%a in (%MAVEN_WRAPPER_PROPERTIES%) do (
        if "%%a"=="wrapperUrl" set WRAPPER_URL=%%b
    )

    if not defined WRAPPER_URL (
        set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
    )

    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile %MAVEN_WRAPPER_JAR%"
    if ERRORLEVEL 1 (
        echo Failed to download Maven Wrapper jar.
        echo Falling back to using Maven directly...
        goto useMaven
    )
)

@REM Run using the wrapper jar
%JAVA_EXE% ^
  -Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR% ^
  -jar %MAVEN_WRAPPER_JAR% %*
goto end

:useMaven
@REM Fallback: try to use mvn directly
where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
    mvn %*
) else (
    echo.
    echo ERROR: Maven is not installed and the wrapper jar could not be downloaded.
    echo Please install Maven: https://maven.apache.org/download.cgi
    echo Or run: choco install maven
    exit /b 1
)

:end
endlocal
