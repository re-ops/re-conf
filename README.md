# Intro

Re-conf is a configuration management implemented in Clojurescript.

Re-conf aims to be:

* Fast(er) then existing solutions (Puppet, Chef etc..)
* Portable (should run on any hardware and Unix like OS).
* Simple to operate and deploy.
* REPL first workflow.
* Debug and tracing built in.

And not to be:

* Be Magical
* A data format
* A DSL

# Resources

Expect the following resources:

* File, create a file from a source.
* Template for generating a file.
* Edit for editing a file.
* Service, start stop and restart.
* Package for installing packages.
* Repo for adding package repositories.
* Exec for executing shell commands.
* Download for performing file download operations.
* Checksum for verifying files.

# Platforms

NodeJs will be the initial platform to be supported (JVM might be supported after).

# Development

```bash
$ npm install
$ lein repl
```

Using a second window:

```bash
$ node target/js/compiled/shim.js
```

From your VIM session:

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
