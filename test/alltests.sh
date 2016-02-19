BUILDDIR=../build
test -d ${BUILDDIR} && rm -rf ${BUILDDIR}
mkdir ${BUILDDIR}

JUNIT=/usr/share/java/junit4.jar
if [ ! -f ${JUNIT} ]; then
    echo "JUnit is not installed. Please install the junit4 package."
    exit 1
fi

JARS=$(find ../jars -type f -name "*.jar")
LUCENE_JARS=""
#$(find /usr/share/java -type f -name "lucene-*.jar")

CP="$JUNIT:$(echo $JARS | tr ' ' ':'):$(echo $LUCENE_JARS | tr ' ' ':'):../build"

javaFiles=$(find ../src -name "*.java")
javac -d ${BUILDDIR} -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

javaFiles=$(find org -name "*.java")
javac -d ${BUILDDIR} -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Test Build failed"
    exit 1
fi

testClasses=$(find org -name "*Test.java" | sed 's,.java,,g' | tr '/' '.')
echo "Running $testClasses"
java -Xmx1024m -classpath ".:$CP" org.junit.runner.JUnitCore $testClasses