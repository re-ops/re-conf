Intro:

Shim is a configuration management language which is subset of the Clojure language, it should be possible to run from any language that support the Clojure LISP dialect.

The project main targets are:

* Fast
* Portable (is should run on any hardware and Unix like OS).
* Simple to operate and deploy.
* REPL first workflow.
* Debug and tracing built in.

Shim will include the following resources:

* File, create a file from a source.
* Template for generating a file.
* Edit for editing a file.
* Service, start stop and restart.
* Package for installing packages.
* Repo for adding package repositories.
* Exec for executing shell commands.
* Download for performing file download operations.
* Checksum for verifying files.


Shim requires from the host language to support:

* run and | macro support
* let statements
* {} []
* ns support
* def and defn
* map, reduce functions

A list of host language include https://github.com/chr15m/awesome-clojure-likes, we will probably support:

* Clojure
* Clojurescript
* [Hy](http://docs.hylang.org/en/stable/)

Possibly:

* [Ferret](https://github.com/nakkaya/ferret)


Pipelines are used to construct workflow:

```clojure
 (ns foo
   (:require 
     (shim :refer :all))

 (defn setup-foo
   (run (package "foo") | (template "/etc/foo.conf") | (service "foo" :restart)))

```

# Copyright and license

Copyright [2018] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
