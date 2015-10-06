This is a prototype of [carbonapi](https://github.com/dgryski/carbonapi) written in clojure. Feel free to contribute!

Currently implemented:

* Fetching data both in protobuf and json formats
* Graphs rendering with basic options (title is supported)
* Infrastructure to implement graphite functions (offset and absolute are implemented)
* url parsing and other stuff.

Server is fully functional however only very basic and essential functionality is implemented.
All the functionality is available both for server and for manual fiddling with the functions,
so that it's possible to use repl as a fully functional graphite client with all clojure goodies
without sacrificing any functionality.

To run the project you'd probably want to install [leiningen](http://leiningen.org/),
standalone jar can be built with `lein uberjar` command

If you want to see more, pull requests are always welcome!
