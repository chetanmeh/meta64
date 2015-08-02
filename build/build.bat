call setenv.bat

java -jar google-compiler.jar --js_output_file="..\src\main\resources\public\js\meta64.min.js" "..\src\main\resources\public\js\meta64\**.js"
pause

cd ..

call mvn dependency:sources
call mvn dependency:resolve -Dclassifier=javadoc

call mvn clean package -DskipTests=true

copy .\target\com.meta64.mobile-0.0.1-SNAPSHOT.jar .\run-root\*

echo "ALL done."
pause


