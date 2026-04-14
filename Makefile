.PHONY: build run

build:
	mvn compile

run:
	mvn exec:java -Dexec.mainClass="Main"
