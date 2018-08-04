# Intro

Re-conf is a framework for implementing provisioning recipes in Clojurescript.

Re-conf aims to be:

* Fast(er) then existing solutions (Puppet, Chef etc..)
* Portable (should run on any hardware and Unix like OS)
* Simple to operate and deploy provisioning code
* REPL first workflow in development
* Tracing and profiling built in

And not to be:

* An agent for orchestrating remote operations ([Re-mote](https://github.com/re-ops/re-mote), [Re-gent](https://github.com/re-ops/re-gent) handle that)
* disguised language in a dataformat
* an external DSL
* magical


[![Build Status](https://travis-ci.org/re-ops/re-conf.png)](https://travis-ci.org/re-ops/re-conf)

## Look and feel

Re-conf use simple functions to describe configuration resources:

```clojure

(line "/tmp/foo" "bar")

(template {:keys ...} "/home/re-ops/.ssh/autorized-keys" "authorized-keys.mustache")

(package "ghc")
```

Check the [docs](https://re-ops.github.io/re-conf/) for complete resources listing.

Functions thread resources using Clojure built in macro:

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
```

Within each function each resource is executed asynchronously, once done the next resource being.

Functions are grouped into namespaces and invoked in parallel:

```clojure
(defn server
  [env]
  (report-n-exit
   (invoke-all env
               re-base.rcp.vim
               re-base.rcp.zfs
               re-base.rcp.docker
               re-base.rcp.shell)))
```

Multiple resources are running concurrently speeding up execution time:

```bash
# packer and restic installed at the same time, but still consistent
$ node re-conf.js
2018-4-8 21:56:11 rosetta info [:re-conf.cljs.core/main] - Started re-conf
2018-4-8 21:56:11 rosetta debug [:re-conf.cljs.core/invoke] - invoking packer
2018-4-8 21:56:11 rosetta debug [:re-conf.cljs.core/invoke] - invoking restic
```

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
$ node target/js/compiled/re-conf.js
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
