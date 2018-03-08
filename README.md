Intro:

* Basic resources, File, Service and package.
* Compiles down to clj, cljs and any other lisp that has defn, let and basic macro support (check https://github.com/chr15m/awesome-clojure-likes)
* SHIM is used for defining the the resource implementation
* Pipelines are used to construct namespaces:

 (defn setup-foo
   (run (package "foo") | (template "/etc/foo.conf") | (service "foo" :restart)))
