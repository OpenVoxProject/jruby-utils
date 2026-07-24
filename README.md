# JRuby Utils

A library for creating and interacting with a pool of JRuby instances in
Clojure.

## Branching

* The [6.x](https://github.com/OpenVoxProject/jruby-utils/tree/6.x) branch uses JRuby 10.0.6 - MRI 3.4 compatible (OpenVox Server 9 beta packages)
* The [5.x](https://github.com/OpenVoxProject/jruby-utils/tree/5.x) branch uses JRuby 9.4 - MRI 3.1 compatible (OpenVox Server 8)
* The [4.x](https://github.com/OpenVoxProject/jruby-utils/tree/4.x) branch uses JRuby 9.3 - MRI 2.6 compatible (OpenVox Server 7)

## Usage

This is a brief overview of the functionality of this library; TODO add more docs.

This library provides three public namespaces:
`puppetlabs.services.jruby-pool-manager.jruby-schemas`,
`puppetlabs.services.jruby-pool-manager.jruby-pool-manager-service`, and
`puppetlabs.services.jruby-pool-manager.jruby-core`.

The entry-point into most of the functionality this library provides is the
`jruby-pool-manager-service` Trapperkeeper service's `create-pool`, which
creates and returns a pool context and asynchronously starts filling the pool.

`create-pool` takes a config matching the `JRubyConfig` schema; this config
can be created and provided defaults by the `jruby-core/initialize-config`
function. The non-optional config settings that you must provide are
`ruby-load-path` and `gem-home`.

You can provide custom initialization and callback logic by providing
lifecycle functions for initializing the scripting container, initializing the
pool instance, cleaning up a pool instance, and cleaning up the pool for
shutdown.

After the pool has been created, use the `borrow` and `return` functions to
work with instances. There is a `with-jruby-instance` macro to make this
easier. There is also a `with-lock` macro that holds a lock on the pool (so
that no borrows can take place) while you execute some logic.

In most TK apps where you want to work with JRuby instances, you will want to
call `create-pool` in the `init` lifecycle of your service, and then call
`jruby-core/flush-pool-for-shutdown!` in the `stop` lifecycle function.

## Running tests

To run the clojure unit tests, use:

~~~sh
lein test
~~~

## License

See [LICENSE](LICENSE).

## Support

GitHub issues and PRs are welcome! Additionally, drop us a line in [the Vox Pupuli Slack](https://voxpupuli.org/connect/).
