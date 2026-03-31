.PHONY: build run

build:
	javac -d build ./src/main/java/*.java

run: build
	java -cp build Main
