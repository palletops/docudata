# lein-docudata

A Leiningen plugin to extract clojure code documentation to EDN.

## Usage

Put `[com.palletops/lein-docudata "0.1.0-SNAPSHOT"]` into the
`:plugins` vector of your `:user` profile to make it available in any
project.

Put `[com.palletops/lein-docudata "0.1.0-SNAPSHOT"]` into the
`:plugins` vector of your `project.clj` to enable it on a specific
project.

    $ lein docudata

## Configuration

The `:docudata` profile is automatically added by the `docudata` task,
and can be used to customise the output file path using the
`:output-file` key (defaults to `target/docudata.edn`) and input
source paths using the `:source-paths` key (defaults to your projects
`:source-paths`).

## License

Copyright © 2014 Hugo Duncan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
