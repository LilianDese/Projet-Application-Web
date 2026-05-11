#!/bin/bash

# Define directories
SRC_DIR="front/src"
BIN_DIR="front/bin"
LIB_DIR="front/lib"

# Create bin directory if it doesn't exist
mkdir -p "$BIN_DIR"

# Build classpath
CP=$(find "$LIB_DIR" -name "*.jar" | tr '\n' ':')

# Compile all Java files in front/src
echo "Compiling front sources..."
javac -d "$BIN_DIR" -cp "$CP" $(find "$SRC_DIR" -name "*.java")

if [ $? -eq 0 ]; then
    echo "Compilation successful."
else
    echo "Compilation failed."
    exit 1
fi
