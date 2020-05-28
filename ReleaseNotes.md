# Release Notes

## v1.7.0

* New [parameter API](https://fjage.readthedocs.io/en/latest/params.html)
* Experimental feature: `setYieldDuringReceive()` to allow other behaviors to execute while waiting for messages
* Behaviors now have priorities, allowing multiple conflicting MessageBehaviors to be active at one time
* JSON serialization/deserialization of messages is now part of public API
* Support for Windows in C gateway API
* Bug fixes in FSM implementation

## v1.6.5

* Javascript gateway bug fixes
* Fixed bug in PutFileReq, causing file overwrites to possibly end up with junk at end
* Fixed bug causing null fields in message to complain about bad JSON
* Fixed bug causing concurrent modification exception during intermittent Javscript gateway connection
* Fixed bug causing an harmless null pointer expection during termination

## v1.6.4

* Bug fix related to behaviors added from a different thread
* Jetty version update

## v1.6.3

* Added null checks in connector streams
* Fixed encoding for unicode strings
* Aligned Javascript API to other language APIs
* Added support to FilePutReq to write file in chunks (append file)

## v1.6.2

* Bug fixes in C, Javascript and Python gatways
* Bug fixes in shell agent, and handling of ANSI sequences
* Minor performance fixes
* Added method to access currently running web servers

## v1.6.1

* Minor fixes in fjagepy
* Minor hterm fixes for compatibility with Firefox

## v1.6

* New persistence API
* Improved consistency of Gateway API
* New Julia gateway
* Refactoring of connector and shell framework
* Minor changes to core API for consistency
* UI enhancements, bug fixes, performance improvements

## v1.5.1

* Major improvements to shell agent for usability, support for user input, clickable URLs, etc
* Performance and visual improvements to web shell
* Small improvements to logging
* Changed JSON encoding of java.util.Date to epoch UTC timestamp for remote protocol
* Improved Gateway implementation to allow for effective extending
* Added fjage.js regression tests
* Updated fjagepy for compatibility with fjage 1.5
* Several bug fixes

## v1.5

* New connectors framework for better integration with websockets, RS232, etc
* New framework for shell extensions and shell documentation
* New Javascript API for access from web browsers
* Improved C and Python APIs
* Use of Travis CI for continuous integration & regression testing
* Use of ReadTheDocs for hosting developer's documentation
* Various bug fixes
