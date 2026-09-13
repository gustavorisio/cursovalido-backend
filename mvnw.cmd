@echo off
setlocal

set "MAVEN_PARENT=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16"
for /d %%D in ("%MAVEN_PARENT%\*") do (
    if exist "%%~fD\bin\mvn.cmd" (
        call "%%~fD\bin\mvn.cmd" %*
        exit /b %ERRORLEVEL%
    )
)

echo Maven Wrapper distribution was not found. 1>&2
echo Run this command again with internet access to download Maven, or install Maven globally. 1>&2
exit /b 1