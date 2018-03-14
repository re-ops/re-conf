# Intro

Shim is a configuration management language which is subset of the Clojure language, it should be possible to run from any language that support the Clojure LISP dialect.

Shim aims to be:

* Fast
* Portable (should run on any hardware and Unix like OS).
* Simple to operate and deploy.
* REPL first workflow.
* Debug and tracing built in.

Non goals:

* Be Magical
* A data format
* A DSL

# Resources

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

# Syntax

Shim requires from the host language to support:

* ->
* let
* {} []
* def and defn
* map, reduce

Both JVM and NodeJS will be supported:

* Clojure
* Clojurescript

Possibly:

* [Ferret](https://github.com/nakkaya/ferret)
* [Hy](http://docs.hylang.org/en/stable/)


Pipelines are used to construct workflow:

```clojure
 (defn tmux
   (-> (package "tmux") (template "~/.tmux.conf") (print "tmux installed"))

 (defn packer
   (let [url "https://releases.hashicorp.com/packer/1.2.1/packer_1.2.1_linux_amd64.zip"
         sum "dd90f00b69c4d8f88a8d657fff0bb909c77ebb998afd1f77da110bc05e2ed9c3"]
    (-> (download url) (verify sum) (extract "/opt/packer")
        (link "/opt/packer/bin/packer" "/bin/packer")  (print "packer ready"))

```

# Development

Clojurescript:

```bash
$ npm install
$ lein figwheel dev
```

Using a second window:

```bash
$ node target/js/compiled/shim.js
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
