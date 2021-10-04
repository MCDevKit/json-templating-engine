# json-templating-engine

JSON Templating Engine helps generate large quantities of JSON mainly for Minecraft Bedrock Edition addons.

More information on usage is at [docs.mcdevkit.com](https://docs.mcdevkit.com/json-templating-engine/)

## Prerequisites

* JDK 11 or later
* Maven

## Building

```shell
git clone https://github.com/MCDevKit/json-templating-engine.git
cd json-templating-engine
mvn clean antlr4:antlr4 package
```