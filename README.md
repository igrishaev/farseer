# Farseer

[JSON-RPC]: https://en.wikipedia.org/wiki/JSON-RPC

A set of modules for [JSON RPC][JSON-RPC]. Includes a transport-independent
handler, Ring HTTP handler, Jetty server, HTTP client, local stub,
documentation, and more.

## Table of Contents

<!-- toc -->

- [What and Why is JSON RPC?](#what-and-why-is-json-rpc)
- [The Structure of this Project](#the-structure-of-this-project)
  * [Installation](#installation)
- [RPC Handler](#rpc-handler)
  * [Method handlers](#method-handlers)
  * [Specs](#specs)
    + [Intput Spec](#intput-spec)
    + [Output Spec](#output-spec)
    + [In Production](#in-production)
  * [More on Context](#more-on-context)
  * [Request Format](#request-format)
  * [Notifications](#notifications)
  * [Batch Requests](#batch-requests)
  * [Configuration](#configuration)
  * [Errors & Exceptions](#errors--exceptions)
- [Ring HTTP Handler](#ring-http-handler)
- [Jetty Server](#jetty-server)
- [HTTP Stub](#http-stub)
- [HTTP Client](#http-client)
- [Documentation Builder](#documentation-builder)
- [Ideas & Further Development](#ideas--further-development)

<!-- tocstop -->

## What and Why is JSON RPC?

## The Structure of this Project

This project consists from several minor projects that complement each one. The
thing is, each sub-project requires its own dependencies. If it was a single
project, you would download lots of stuff you don't really need. Instead, with
sub-projects, you install only those parts (and transient dependencies) you
really need in your project.

The root project is named `com.github.igrishaev/farseer-all`. It unites all the
sub-projects listed below:

- `com.github.igrishaev/farseer-common`: dependency-free parts required by other
  sub-projects;

- `com.github.igrishaev/farseer-handler`: a transport-free implementation of RPC
  handler;

- `com.github.igrishaev/farseer-http`: HTTP Ring handler for RPC;

- `com.github.igrishaev/farseer-jetty`: HTTP Jetty server for RPC;

- `com.github.igrishaev/farseer-stub`: an HTTP stub for RPC server, useful for tests;

- `com.github.igrishaev/farseer-client`: HTTP RPC client based on `clj-http`;

- `com.github.igrishaev/farseer-doc`: RPC documentation builder.

[groups]: https://github.com/clojars/clojars-web/wiki/Groups

Note: I had to add the `com.github.igrishaev` group id not because I'd like you
typing my nickname all the time. This is due to recent changes in [Clojars
policies][groups] that disallow custom group ids unless you own such a domain.

### Installation

The "-all" bundle:

- Lein:

```clojure
[com.github.igrishaev/farseer-all "0.1.0"]
```

- Deps.edn

```clojure
com.github.igrishaev/farseer-all {:mvn/version "0.1.0"}
```

- Maven

```xml
<dependency>
  <groupId>com.github.igrishaev</groupId>
  <artifactId>farseer-all</artifactId>
  <version>0.1.0</version>
</dependency>
```

Alternatevely, install only what you need:

- Lein:

```clojure
[com.github.igrishaev/farseer-http "0.1.0"]
[com.github.igrishaev/farseer-client "0.1.0"]
```

[com.github.igrishaev]: https://clojars.org/groups/com.github.igrishaev

and so on (see the list of packages [on Clojars][com.github.igrishaev]).

## RPC Handler

The basic `com.github.igrishaev/farseer-handler` package provies an
implementation of RPC protocol without a transport level. This means, you only
define the RPC handlers and specs no matter where the data comes from. There are
additional packages that bring HTTP transport for the handler you built.

First, add it to the project:

```clojure
[com.github.igrishaev/farseer-handler ...]
```

Here is the minimal usage example. Prepare a namespace:

```clojure
(ns demo
  (:require
   [farseer.handler :refer [make-handler]]))
```

Create a method handler and the config:

```clojure
(defn rpc-sum
  [_ [a b]]
  (+ a b))

(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function rpc-sum}}})
```

Now declare a handler and call it:

```clojure
(def handler
  (make-handler config))

(handler {:id 1
          :method :math/sum
          :params [1 2]
          :jsonrpc "2.0"})

;; {:id 1, :jsonrpc "2.0", :result 3}
```

### Method handlers

The `rpc-sum` function is a handler for the `:math/sum` method. The function
takes **exactly two** argumets. The first argumant is the context map which
we'll discuss later. The second is the parameters passed to the method in
request. They might be either a map or a vector. Alternatevly, a method can be
free from parameters at all.

The function might be defined in another namespace. In this case, you import it
and pass to the map as usual:

```clojure
(ns demo
  (:require
   [com.project.handlers.math :as math]))

(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function math/sum-handler}}})
```

It's useful to pass the functions as vars using the `#'` handler:

```clojure
(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum}}})
```

In this case, you can update the function by executing its `defn` form in REPL,
and the changes come into play without re-creating the RPC handler. For example,
we change plus to minus in the `rpc-sum` function:

```clojure
(defn rpc-sum
  [_ [a b]]
  (- a b))
```

Then we go to the closing bracket and perform `cider-eval-last-sexp`. Now we
perform a new RPC call and get a new result:

```clojure
(handler {:id 1
          :method :math/sum
          :params [1 2]
          :jsonrpc "2.0"})

{:id 1, :jsonrpc "2.0", :result -1}
```

Only the methods declared in the config are served by the RPC handler. If you
specify a non-existing one, you'll get a negative response:

```clojure
(handler {:id 1
          :method :system/rmrf
          :params [1 2]
          :jsonrpc "2.0"})

{:error
 {:code -32601, :message "Method not found", :data {:method :system/rmrf}},
 :id 1,
 :jsonrpc "2.0"}
```

### Specs

The code above doesn't valiate the incoming parameters and thus is dangerous to
execute. If you pass something like `["one" nil]` instead of two numbers, you'll
end up with `NPE`, which is not good.

For each handler, you can specify a couple of specs, the input and output
ones. The input spec validates the incoming `params` field, and the output spec
is for the result of the function call with these parameters.

#### Intput Spec

So, if you want to protect our `:math/sum` handler from weird data, you declare
the specs:

```clojure
(s/def :math/sum.in
  (s/tuple number? number?))

(s/def :math/sum.out
  number?)
```

Then you add these specs to the method config. Their keys are `:handler/spec-in`
and `:handler/spec-out`:

```clojure
(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})
```

Now if you pass something wrong to the handler, you'll get a negative response:

```clojure
(handler {:id 1
          :method :math/sum
          :params ["one" nil]
          :jsonrpc "2.0"})

{:id 1
 :jsonrpc "2.0"
 :error {:code -32602
         :message "Invalid params"
         :data {:explain "<spec explain string>"}}}
```

[expound]: https://github.com/bhb/expound

The `:data` field of the `:error` object has an extra `explain` field. Inside
it, there is standard explain string produced by the `s/explain-str`
function. This kind of message looks noisy sometimes, and in the future, most
likely Farseer will use [Expound][expound].

According to the RPC documentation, the `:params` field might be either a map of
keys or a vector. Thus, for the input spec, you probably use `s/tuple` or
`s/keys` specs. Our `:math/sum` method accepts vector params. Let's rewrite it
and the specs so that they work with a map:

```clojure
;; a new handler
(defn rpc-sum
  [_ {:keys [a b]}]
  (+ a b))

;; new input spec
(s/def :sum/a number?)
(s/def :sum/b number?)

(s/def :math/sum.in
  (s/keys :req-un [:sum/a :sum/a]))
```

The output spec and the config are still the same:

```clojure
(s/def :math/sum.out
  number?)

(def config
  {:rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})
```

Now we pass a map, not vector:

```clojure
(handler {:id 1
          :method :math/sum
          :params {:a 1 :b 2}
          :jsonrpc "2.0"})

{:id 1, :jsonrpc "2.0", :result 3}
```

#### Output Spec

If the result of the function doesn't match the output spec, this triggers an
internal error. Let's reproduce this scenario by changing the spec:

```clojure
(s/def :math/sum.out
  string?)

(handler {:id 1
          :method :math/sum
          :params {:a 1 :b 2}
          :jsonrpc "2.0"})


{:id 1,
 :jsonrpc "2.0"
 :error {:code -32603,
         :message "Internal error",
         :data {:method :math/sum}}}
```

In the log, you'll see the following entry:

```
10:18:31.256 ERROR farseer.handler - RPC result doesn't match the output spec,
             id: 1, method: :math/sum, code: -32603, message: Internal error
```

There is no the `s/explain` message, because sometimes it's huge and also
contains private data.

#### In Production

You can turn off checking the input or the output specs globally in the
configuration (see the "Configuration" section below). In real projects, we
always validate the input data. Regarding the output, we validate it only in
tests to save time in production.

### More on Context

[component]: https://github.com/stuartsierra/component
[integrant]: https://github.com/weavejester/integrant

OK, summing numbers is good for tuturial but makes no sense in real
projects. Because there, we're moslty intreseted in IO and database
access. Until now, it wasn't clear how a function can reach Postgres or Kafka
clients especially if the project relies on system (e.g. [Component][component]
or [Integrant][integrant]).

In OOP languages, the environment for the RPC method usualy comes from the
`this` parameter. It's an instance of some `RPCHadler` class that has fields for
the database connection, message queue client and so on. In Clojure, we act
almost like this, but instead of `this` object, we use context.

A context is a map that carries the data needed by the method handler in
runtime. This is the first argument of a function from the `:handler/function`
key. By default, the context carries the current id and method of the RPC
call. If you print the first argument of the function, you'll see:

```clojure
(defn rpc-sum
  [context {:keys [a b]}]
  (println context)
  (+ a b))

#:rpc{:id 1, :method :math/sum}
```

Both fields are prefixed with the `:rpc/` namespace to prevent the keys from
clashing, e.g. `:id` for the RPC call and `:id` for the current user. Instead,
the framework passes the `:rpc/id` field, and you should pass `:user/id` one.

The function made by the `(make-handler config)` call takes either one or two
arguments. In the first case (one argument) it's just an RPC request. In the
second case (two arguments), these are the RPC request and the context. The
context carries some data required for the RPC function.

The example below shows how to get a user from the database. Without the
context, the function should be closured over the `db` variable, or the `db`
must be a global entity, which leads to fragile design of the application. But
as far as the `db` can be passed as an argument, we're fine.

First, declare the function and specs:

~~~clojure
(defn get-user-by-id
  [{:keys [db]}
   {:keys [user-id]}]
  (jdbc/get-by-id db :users user-id))

(s/def :user/id pos-int?)

(s/def :user/user-by-id.in
  (s/keys :req-un [:user/id]))

(s/def :user/user-by-id.out
  (s/nilable map?))
~~~

The config:

~~~clojure
(def config
  {:rpc/handlers
   {:user/get-by-id
    {:handler/function #'get-user-by-id
     :handler/spec-in :user/user-by-id.in
     :handler/spec-out :user/user-by-id.out}}})
~~~

Now imagine we've built a JDBC pool named `hikari-cp-pool`. That's how we pass
it to the handler:

~~~clojure
(def handler
  (make-handler config))

(handler {:id 1
          :method :user/get-by-id
          :params {:id 5}
          :jsonrpc "2.0"}
         {:db hikari-cp-pool})
~~~





The context maps are always merged, so in your function, you get a map that
carries all the data at once (unless you overwrite keys with the same names).



### Request Format

### Notifications

### Batch Requests

### Errors & Exceptions

### Configuration

## Ring HTTP Handler

## Jetty Server

## HTTP Stub

## HTTP Client

## Documentation Builder

## Ideas & Further Development
