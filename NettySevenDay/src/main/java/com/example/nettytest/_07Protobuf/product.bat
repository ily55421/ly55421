@echo off
for %%i in (proto/*.proto) do(
    protoc ./proto/%%i --java_out = ./java
    echo exchange %%i To java file successFully!
)
pause