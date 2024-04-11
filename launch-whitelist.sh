#!/bin/bash

echo "======================================================================================="
echo "  Converting Java code to TypeScript definition files..."
echo "======================================================================================="

START_TIME=$SECONDS

# Define quiet and verbose functions
run_quiet() {
    "$@" > /dev/null
}

run_verbose() {
    "$@"
}

# Check for -v or --verbose option
if [[ $* == *-v* ]] || [[ $* == *--verbose* ]]; then
    echo " Using verbose mode..."
    WGET="wget"
    UNZIP="unzip"
    JAR="jar"
    GIT_CLONE="git clone"
    MVN="mvn"
    JAVA="java"
    RUN_COMMAND=run_verbose
else
    echo " Using quiet mode, using -v to activate verbose mode..."
    WGET="wget -q"
    UNZIP="unzip -q"
    JAR="jar"
    GIT_CLONE="git clone -q"
    MVN="mvn"
    JAVA="java"
    RUN_COMMAND=run_quiet
fi

project_dir="npm-modules-engine-whitelist"

# Create a directory for the source code and JAR files
echo " Creating directories..."
mkdir -p $project_dir/build $project_dir/build/sources $project_dir/build/jar-symbols

# Define the source directories
jdk11_dir="$project_dir/build/sources/jdk11"
jcr2_dir="$project_dir/build/sources/jcr2"
servlet_api_dir="$project_dir/build/sources/servlet-api"
felix_framework_dir="$project_dir/build/sources/felix-framework"
npm_modules_engine_dir="$project_dir/build/sources/npm-modules-engine"
jahia_private_dir="$project_dir/build/sources/jahia-private"
jar_symbols_dir="$project_dir/build/jar-symbols"

# Define the source files
jdk11_file="jdk11u-master.zip"
jcr2_file="jcr-2.0-sources.jar"
servlet_api_file="servlet-api-3.0.1-sources.jar"
felix_framework_file="org.apache.felix.framework-6.0.5-source-release.zip"

# Define the JAR files
jar_files=("org/apache/felix/org.apache.felix.framework/6.0.5/org.apache.felix.framework-6.0.5.jar"
  "org/springframework/spring-web/3.2.18.jahia4_OSGI/spring-web-3.2.18.jahia4_OSGI.jar"
  "org/springframework/spring-beans/3.2.18.RELEASE_OSGI/spring-beans-3.2.18.RELEASE_OSGI.jar"
  "org/graalvm/sdk/graal-sdk/22.3.3/graal-sdk-22.3.3.jar"
  "org/jboss/spec/javax/servlet/jsp/jboss-jsp-api_2.3_spec/1.0.3.Final/jboss-jsp-api_2.3_spec-1.0.3.Final.jar"
  "javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar"
  "javax/servlet/jsp/jsp-api/2.1/jsp-api-2.1.jar"
  "javax/jcr/jcr/2.0/jcr-2.0.jar"
  "commons-lang/commons-lang/2.6/commons-lang-2.6.jar"
  "xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.jar"
  "org/osgi/osgi.annotation/7.0.0/osgi.annotation-7.0.0.jar"
  "org/springframework/spring-context/3.2.18.RELEASE_OSGI/spring-context-3.2.18.RELEASE_OSGI.jar"
  "org/apache/jackrabbit/jackrabbit-spi-commons/2.18.4-jahia1/jackrabbit-spi-commons-2.18.4-jahia1.jar")

# Create the source directories if they don't exist
mkdir -p $jdk11_dir $jcr2_dir $servlet_api_dir $felix_framework_dir $npm_modules_engine_dir $jahia_private_dir $jar_symbols_dir

# Download and uncompress JDK 11 source code if it doesn't exist
if [ ! -f "$jdk11_dir/$jdk11_file" ]; then
    echo " Downloading and uncompressing JDK 11 source code..."
    $WGET -P $jdk11_dir -O $jdk11_dir/$jdk11_file https://github.com/openjdk/jdk11u/archive/refs/heads/master.zip
    $UNZIP $jdk11_dir/$jdk11_file -d $jdk11_dir
fi

# Download and uncompress JCR 2.0 API source code if it doesn't exist
if [ ! -f "$jcr2_dir/$jcr2_file" ]; then
    echo " Downloading and uncompressing JCR 2.0 API source code..."
    $WGET -P $jcr2_dir -O $jcr2_dir/$jcr2_file https://repo1.maven.org/maven2/javax/jcr/jcr/2.0/jcr-2.0-sources.jar
    pushd $jcr2_dir
    $RUN_COMMAND $JAR -xf $jcr2_file
    popd
fi

# Download and uncompress Servlet API 3.0.1 source code if it doesn't exist
if [ ! -f "$servlet_api_dir/$servlet_api_file" ]; then
    echo " Downloading and uncompressing Servlet API 3.1.0 source code..."
    $WGET -P $servlet_api_dir -O $servlet_api_dir/$servlet_api_file https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0-sources.jar
    pushd $servlet_api_dir
    $RUN_COMMAND $JAR -xf $servlet_api_file
    popd
fi

# Download and uncompress Apache Felix Framework 6.0.5 source code if it doesn't exist
if [ ! -f "$felix_framework_dir/$felix_framework_file" ]; then
    echo " Downloading and uncompressing Apache Felix Framework 6.0.5 source code..."
    $WGET -P $felix_framework_dir -O $felix_framework_dir/$felix_framework_file http://archive.apache.org/dist/felix/org.apache.felix.framework-6.0.5-source-release.zip
    $UNZIP $felix_framework_dir/$felix_framework_file -d $felix_framework_dir
fi

# Clone npm-modules-engine repository if it doesn't exist
if [ ! -d "$npm_modules_engine_dir/.git" ]; then
    echo " Cloning npm-modules-engine repository..."
    $GIT_CLONE git@github.com:Jahia/npm-modules-engine.git $npm_modules_engine_dir
    cd $npm_modules_engine_dir
    echo " Compiling project using Maven..."
    $RUN_COMMAND mvn clean install
    echo " Copying JAR files to $jar_symbols_dir ..."
    cp target/*.jar ../../../../$jar_symbols_dir/
    cd -
fi

# Clone jahia-private repository if it doesn't exist
if [ ! -d "$jahia_private_dir/.git" ]; then
    echo " Cloning jahia-private repository..."
    $GIT_CLONE git@github.com:Jahia/jahia-private.git $jahia_private_dir
    cd $jahia_private_dir
    echo " Compiling project using Maven..."
    $RUN_COMMAND mvn clean install
    echo " Copying JAR files to $jar_symbols_dir ..."
    cp core/target/*.jar ../../../../$jar_symbols_dir/
    cd -
fi

# Download JAR files from Maven Central if they don't exist in the jar-symbols directory
for jar_file in "${jar_files[@]}"; do
    jar_file_name=$(basename $jar_file)
    if [ ! -f "$jar_symbols_dir/$jar_file_name" ]; then
        echo " Downloading $jar_file_name ..."
        $WGET -P $jar_symbols_dir -O $jar_symbols_dir/$jar_file_name https://devtools.jahia.com/nexus/content/groups/public/$jar_file
    fi
done

echo " Creating directories and cleaning up previous builds..."
$RUN_COMMAND mkdir -p $project_dir/build/types
$RUN_COMMAND rm -rf $project_dir/build/types/*

echo " Running Java to Typescript tool..."
$RUN_COMMAND $JAVA -jar build/libs/java-ts-bind-all.jar --packageJson $project_dir/package.json

echo " Patching generated output..."
$RUN_COMMAND pushd $project_dir
$RUN_COMMAND source fix-gen.sh
echo " Building TypeScript definition files..."
$RUN_COMMAND yarn build
$RUN_COMMAND popd

ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo "======================================================================================="
echo " Finished converting Java code to TypeScript definition files in $ELAPSED_TIME seconds "
echo "======================================================================================="
