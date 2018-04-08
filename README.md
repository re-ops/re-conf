# Intro

Re-conf implements functional configuration management recipes in Clojurescript.

Re-conf aims to be:

* Fast(er) then existing solutions (Puppet, Chef etc..)
* Portable (should run on any hardware and Unix like OS).
* Simple to operate and deploy.
* REPL first workflow in development.
* Debug, tracing and profiling built in.

And not to be:

* disguised language in a dataformat
* an external DSL
* magical

[![Build Status](https://travis-ci.org/re-ops/re-conf.png)](https://travis-ci.org/re-ops/re-conf)

## Look and feel

  * Re-conf use simple functions to describe threading of resources
  * Within each function the execution is serial, each resource is executed asynchronously and once its done the next step begins.
  * Functions are executed concurrently so multiple resource run at the same time (but still serial within each function).
  * There is no hidden execution or dependency graph or any other hidden mechanism.

```clojure
(defn packer
  "Setup up packer"
  []
  (let [dest "/tmp/packer_1.2.2_linux_amd64.zip"
        sha "6575f8357a03ecad7997151234b1b9f09c7a5cf91c194b23a461ee279d68c6a8"
        url "https://releases.hashicorp.com/packer/1.2.2/packer_1.2.2_linux_amd64.zip"]
    (->
     (download url dest)
     (checksum dest sha :sha256)
     (exec "/usr/bin/unzip" "-o" dest "-d" "/tmp/packer")
     (summary "installing packer done"))))

(defn restic
  "Setting up restic"
  []
  (let [dest "/tmp/restic_0.8.3_linux_amd64.bz2"
        sha "1e9aca80c4f4e263c72a83d4333a9dac0e24b24e1fe11a8dc1d9b38d77883705"
        url "https://github.com/restic/restic/releases/download/v0.8.3/restic_0.8.3_linux_amd64.bz2"]
    (->
     (download url dest)
     (checksum dest sha :sha256)
     (exec "bzip2" "-f" "-d" dest)
     (summary "installing restic done"))))
```

```bash
# packer and restic installed at the same time, but still consistent
$ node re-conf.js
2018-4-8 21:56:11 rosetta info [:re-conf.cljs.core/main] - Started re-conf
2018-4-8 21:56:11 rosetta debug [:re-conf.cljs.core/invoke] - invoking packer
2018-4-8 21:56:11 rosetta debug [:re-conf.cljs.core/invoke] - invoking restic
```

## Resources

 * File, create a file from a source.
 * Template for generating a file.
 * Edit for editing a file.
 * Service, start stop and restart.
 * Package for installing packages.
 * Repo for adding package repositories.
 * Exec for executing shell commands.
 * Download for performing file download operations.
 * Checksum for verifying files.

Adding more resources is just a couple of functions away.

# Platforms

NodeJs will be the initial platform to be supported (JVM might be supported after).

# Development

```bash
$ lein npm install
```

## VIM

```bash
$ lein repl
```

Using a second window:

```bash
$ node target/js/compiled/shim.js
```

In the VIM session:

```bash
:Piggieback (figwheel-sidecar.repl-api/repl-env)
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
