<div align="center">

# MultiLogin

_✨ Coexisting Minecraft Authentication and Multiple BlessingSkin Authentication ✨_

</div>

## Summary

MultiLogin is a plugin designed primarily for Minecraft proxy,
aimed at supporting the coexistence of Minecraft authentication and multiple BlessingSkin authentication.
It is used to connect players under two or more external authentication servers,
allowing them to play together on the same server.

## Features

* Supports up to 128 Yggdrasils from different sources coexisting simultaneously
* Authentication proxy and retry mechanism
* In-game profile management system
* Asynchronous/synchronous skin repair mechanism
* Support takeover of Floodgate

## Deploy

The minimum requirement is' Java 17 ',
without the need to install' authlib injector ',
without any pre plugins,
and without the need to add or change' JVM 'parameters

1. [Download](https://github.com/CaaMoe/MultiLogin/releases/latest) plugin
2. throw into plugins
3. launch the server

## Config

See details in [Wiki](https://github.com/CaaMoe/MultiLogin/wiki)

## Build

1. Clone this project
2. Refer to [Description]（ https://github.com/CaaMoe/MultiLogin/blob/v6/velocity/libraries/README.md ）Complete the dependency on velocity
3. Execute `./gradlew shadowJar`
4. Find what you need under '*/build/libs'
