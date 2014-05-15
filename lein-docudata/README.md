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

The `:docudata` key can be passed a map to configure docudata.

To customise the output file path, use the `:output-file` key
(defaults to `target/docudata.edn`).  For the  input source paths use the
`:source-paths` key (defaults to your projects `:source-paths`).

Use the `:exclude-keywords` key to specify a sequence of keywords to
remove from the var metadata.

The `:docudata` profile is automatically added by the `docudata` task.

## License

Copyright © 2014 Hugo Duncan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
