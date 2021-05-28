# Farseer

[JSON-RPC]: https://en.wikipedia.org/wiki/JSON-RPC

A set of modules for [JSON RPC][JSON-RPC]. Includes a transport-independent
handler, Ring HTTP handler, Jetty server, HTTP client, local stub,
documentation, and more.

## Table of Contents

<!-- toc -->

- [What and Why is JSON RPC?](#what-and-why-is-json-rpc)
  * [Benefits](#benefits)
  * [Disadvantages](#disadvantages)
- [The Structure of this Project](#the-structure-of-this-project)
  * [Installation](#installation)
- [RPC Handler](#rpc-handler)
  * [Method handlers](#method-handlers)
  * [Specs](#specs)
    + [Input Spec](#input-spec)
    + [Output Spec](#output-spec)
    + [In Production](#in-production)
  * [More on Context](#more-on-context)
    + [Static Context](#static-context)
    + [Dynamic Context](#dynamic-context)
  * [Request & Response Formats](#request--response-formats)
    + [Request](#request)
    + [Response](#response)
    + [Error Codes](#error-codes)
  * [Notifications](#notifications)
  * [Batch Requests](#batch-requests)
    + [Note on Parallelism](#note-on-parallelism)
    + [Configuring & Limiting Batch Requests](#configuring--limiting-batch-requests)
  * [Errors & Exceptions](#errors--exceptions)
    + [Runtime (Unexpected) Errors](#runtime-unexpected-errors)
    + [Expected Errors](#expected-errors)
    + [Raising Exceptions](#raising-exceptions)
  * [Configuration](#configuration)
- [Ring HTTP Handler](#ring-http-handler)
    + [Negative Responses](#negative-responses)
    + [Batch Requests in HTTP](#batch-requests-in-http)
    + [Configuration](#configuration-1)
    + [Middleware & Authorization](#middleware--authorization)
    + [HTTP Context](#http-context)
- [Jetty Server](#jetty-server)
  * [Configuration](#configuration-2)
  * [With-server macro](#with-server-macro)
  * [Component](#component)
- [HTTP Stub](#http-stub)
    + [Multiple Stub](#multiple-stub)
    + [Tests](#tests)
    + [Negative Responses](#negative-responses-1)
- [HTTP Client](#http-client)
    + [Configuration](#configuration-3)
    + [Handling Response](#handling-response)
    + [Auth](#auth)
    + [Notifications](#notifications-1)
    + [Batch Requests](#batch-requests-1)
    + [Connection Manager (Pool)](#connection-manager-pool)
    + [Component](#component-1)
- [Documentation Builder](#documentation-builder)
  * [Configuration](#configuration-4)
  * [Building](#building)
  * [Demo](#demo)
  * [Selmer & Context](#selmer--context)
  * [Rendering Specs](#rendering-specs)
- [Ideas & Further Development](#ideas--further-development)
- [Author](#author)

<!-- tocstop -->

## What and Why is JSON RPC?

Briefly, JSON RPC is a protocol based on HTTP & JSON. When calling the server,
you specify the method (procedure) name and its parameters. The parameters could
either a map of a vector. The server returns a JSON response with the `result` or
`error` fields. For example:

Request:

~~~json
{"jsonrpc": "2.0", "method": "sum", "params": [1, 2], "id": 3}
~~~

Response:

~~~clojure
{"jsonrpc": "2.0", "result": 3, "id": 3}
~~~

Pay attention: the protocol depends on neither HTTP method, nor query params,
HTTP headers and so on. Although looking a bit primitive, this schema suddenly
appears to be robust, scalable and reliable.

### Benefits

RPC protocol brings significant and positive changes in your API, namely:

- There is single API endpoint on the server, for example `/api`. You don't need
  to concatenate strings manually to build the paths like
  `/post/42/comments/52352` in REST.

- All the data is located in one place. There is no need to parse the URI, query
  params, check out the method and so on. You don't need to guess which HTTP
  method to pick (PUT, PATCH) for an operation when several entities change.

- RPC grows horizontally with ease. Once you've set it up, you only extend
  it. Technically it means adding a new key into a map.

- RPC doesn't depend on transport. You can save the payload in Cassandra or push
  to Kafka. Later on, you can replay the sequence as it has everything you need.

- RPC is a great choice for interaction between internal services. When all the
  services follow the same protocol, it's easy to develop and maintain them.
  When protected with authentication, RPC can be provided to the end customers
  as well.

### Disadvantages

The only disadvantage of RPC protocol is that it's free from caching. On the
other side, we rarely want to get cached data. Most often, it's important to get
fresh data on each request. If you share some public data that update rarely,
perhaps you should organize ordinary GET endpoints.

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

The basic `com.github.igrishaev/farseer-handler` package provides an
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
takes **exactly two** arguments. The first argument is the context map which
we'll discuss later. The second is the parameters passed to the method in
request. They might be either a map or a vector. Alternately, a method can be
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

The code above doesn't validate the incoming parameters and thus is dangerous to
execute. If you pass something like `["one" nil]` instead of two numbers, you'll
end up with `NPE`, which is not good.

For each handler, you can specify a couple of specs, the input and output
ones. The input spec validates the incoming `params` field, and the output spec
is for the result of the function call with these parameters.

#### Input Spec

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

OK, summing numbers is good for tutorial but makes no sense in real
projects. Because there, we're mostly interested in IO and database
access. Until now, it wasn't clear how a function can reach Postgres or Kafka
clients especially if the project relies on system (e.g. [Component][component]
or [Integrant][integrant]).

In OOP languages, the environment for the RPC method usually comes from the
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

Actually, there are two ways of passing context: a static and dynamic ones.

#### Static Context

When you call the `make-handler` function to build an RPC handler, the second
argument might be a context map. This map will be available in all the RPC
functions. For example:

~~~clojure
(def handler
  (make-handler
   config
   {:db (open-db-connection {...})
    :version (get-app-version)}))
~~~

We assume the `open-db-connection` returns a connection pool, which is available
to all the RPC functions as the `:db` field of the context. The `:version` field
is the application version that is fetched from the text file.

Now, if we had an RPC method that fetches a user by id, it could look like this:

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

The call:

~~~clojure
(handler {:id 1
          :method :user/get-by-id
          :params {:id 5}
          :jsonrpc "2.0"})

{:id 1, :jsonrpc "2.0", :result {:id 5 :name "Test"}}
~~~

#### Dynamic Context

Use dynamic context when to pass a value that is only needed for the current RPC
request. In that case, pass the context map as the second argument to the
function made by the `make-handler`. The example with the database would look
like this:

~~~clojure
;; no static context
(def handler (make-handler config))

;; dynamoc context
(handler {:id 1
          :method :user/get-by-id
          :params {:id 5}
          :jsonrpc "2.0"}
         {:db hikari-cp-pool})
~~~

Of course, you can use both ways to pass the context. Most likely the database
is needed by all the RPC functions, so its place in the global context. Some
minor fields might be passes on demand for certain calls:

~~~clojure
(def handler
  (make-handler config {:db (make-db ...)}))

(handler {:id 1 :method ...} {:version "0.2.1"})
~~~

The context maps are always merged, so from the function's point of view, they
act the same.

The local context map gets merged into the global one. It gives you an
opportunity to override the default values from the context. Let's say, if the
method `:user/get-by-id` needs a special (read-only) database, we can override
it like this:

~~~clojure
(def handler
  (make-handler config {:db (make-db ...)}))

(handler {:id 1 :method :user/get-by-id ...}
         {:db read-only-db})
~~~

### Request & Response Formats

#### Request

An RPC request is a map of the following fields:

- `:id`: either a number or a string value representing this request. The handler
  must return the same id in response unless it was a notification (see below).

- `:method`: either a string or a keyword (preferred) that specify the RPC
  method. If the method was a string, it gets coerced to the keyword anyway. We
  recommend using the full qualified keywords with namespaces. The namespaces
  help to group methods by semantic.

- `:params`: either a map of [`keyword?`, `any?`] pairs, or a vector of `any?`
  values (`sequential?` if more precisely). This field is optional as not all
  the methods require arguments.

- `:jsonrpc`: a string with exact value `"2.0"`, the required one.

Examples:

```clojure
;; all the fields
{:id 1
 :method :math/sum
 :params [1 2]
 :jsonrpc "2.0"}

;; no params
{:id 2
 :method :app/version
 :jsonrpc "2.0"}

;; no id (notification)
{:method :user/delete-by-id
 :params {:id 3}
 :jsonrpc "2.0"}
```

The RPC request might be of a batch form then it's a vector of such maps. Batch
is useful to perform multiple actions per one call. See the "Batch Requests"
section below.

#### Response

The response is map with the `:id` and `:jsonrpc` fields. The ID is the same you
passed in the request so you can match the response to the request by ID. If
the response was positive, its `:result` field carries the value that the RPC
function returned:

~~~clojure
{:id 1, :jsonrpc "2.0", :result 3}
~~~

A negative response has no the `:result` fields but the `:error` one
instead. The error node consists from the `:code` and `:message` fields which
are the numeric code representing an error and a text message explaining it. In
addition, there might be the `:data` fields which is an arbitrary map with some
extra context. The library adds the `:method` field to the context
automatically.

~~~clojure
{:id 1
 :jsonrpc "2.0"
 :error {:code -32603
         :message "Internal error"
         :data {:method :math/div}}}
~~~

#### Error Codes

- `-32700 Parse error`: Used then the server gets a non-JSON/broken payload.

- `-32600 Invalid Request`: The payload is JSON but has wrong shape.

- `-32601 Method not found`: No such RPC method.

- `-32602 Invalid params`: The parameters do not match the input spec.

- `-32603 Internal error`: Either uncaught exception or the result doesn't match
  the output spec.

- `-32000 Authentication failure`: Something is wrong with auth/credentials.

[jsonrpc-spec]: https://www.jsonrpc.org/specification

Find more information about the error codes [on this page][jsonrpc-spec].

### Notifications

Sometimes, you're not interested in the response from an RPC server. Say, if
you delete a user, there is nothing for you to return. In this case, you send a
notification rather than a request. Notifications are formed similar but have
no the `:id` field. When replying to the notification, the server returns
nothing. For example:

~~~clojure
(handler {:method :math/sum
          :params [1 2]
          :jsonrpc "2.0"})

nil
~~~

Notifications are useful to trigger some side effects on the server.

Remember, if you pass a missing method or wrong input data (or any other error
occurs), you'll get a negative response anyway:

~~~clojure
(handler {:method :math/sum
          :params [1 "a"]
          :jsonrpc "2.0"})

{:error
 {:code -32602
  :message "Invalid params"}
 :jsonrpc "2.0"}
~~~

### Batch Requests

Batch requests is the main feature of JSON RPC. It allows you to send multiple
request maps in one call. The server executes the requests and returns a list of
result maps. For example, you have a method `user/get-by-id` which takes a
single ID and returns a map from the database. Now you got ten IDs. With
ordinary REST API, you would run a cycle and performed ten HTTP calls. With RPC,
you make a batch call.

In our example, if we want to solve several math expressions at once, we do:

~~~clojure
(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:id 2
           :method :math/sum
           :params [3 4]
           :jsonrpc "2.0"}
          {:id 3
           :method :math/sum
           :params [5 6]
           :jsonrpc "2.0"}])
~~~

The result:

~~~clojure
({:id 1 :jsonrpc "2.0" :result 3}
 {:id 2 :jsonrpc "2.0" :result 7}
 {:id 3 :jsonrpc "2.0" :result 11})
~~~

If some of the tasks fail, they won't affect the others:

~~~clojure
(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:id 2
           :method :math/sum
           :params [3 "aaa"]  ;; bad input
           :jsonrpc "2.0"}
          {:id 3
           :method :math/missing ;; wrong method
           :params [5 6]
           :jsonrpc "2.0"}])
~~~

The result:

~~~clojure
({:id 1 :jsonrpc "2.0" :result 3}
 {:error
  {:code -32602
   :message "Invalid params"
   :data
   {:explain ...
    :method :math/sum}}
  :id 2
  :jsonrpc "2.0"}
 {:error
  {:code -32601 :message "Method not found" :data {:method :math/missing}}
  :id 3
  :jsonrpc "2.0"})
~~~

You can mix ordinary RPC tasks with notifications in a batch. There will be no
response maps for notifications in the result vector:

~~~clojure
(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:method :math/sum ;; no ID
           :params [3 4]
           :jsonrpc "2.0"}])

[{:id 1 :jsonrpc "2.0" :result 3}]
~~~

#### Note on Parallelism

[pmap]: https://clojuredocs.org/clojure.core/pmap

By default, Farseer uses the standard [`pmap` function][pmap]. It executes the
tasks in semi-parallel way. Maybe in the future, we could use a custom fixed
thread executor for more control.

#### Configuring & Limiting Batch Requests

The following options help you to control batch requests:

- `:rpc/batch-allowed?` (default is `true`): whether or not to allow batch
  requests. If you set this to `false` and someone performs a batch call, they
  will get an error like this:

~~~clojure
(def config
  {:rpc/batch-allowed? false
   :rpc/handlers ...})

(def handler
  (make-handler config))


(handler [{:id 1
           :method :math/sum
           :params [1 2]
           :jsonrpc "2.0"}
          {:id 2
           :method :math/sum
           :params [3 4]
           :jsonrpc "2.0"}])

{:error {:code -32602, :message "Batch is not allowed"}}
~~~

- `:rpc/batch-max-size` (default is 25): the max number of tasks in a single
  batch request. Sending more tasks in one request that is allowed leads to an
  error:

~~~clojure
(def config
  {:rpc/batch-allowed? true
   :rpc/batch-max-size 2
   :rpc/handlers ...})

(def handler
  (make-handler config))


(handler [{...} {...} {...}])

{:error {:code -32602, :message "Batch size is too large"}}
~~~

- `:rpc/batch-parallel?` (default is `true`): whether or not to prefer `pmap`
  over the standard `mapv` for tasks processing. When `false`, the tasks get
  executed just one by one.

### Errors & Exceptions

#### Runtime (Unexpected) Errors

The RPC handler wraps the whole logic into `try/catch` form with the `Throwable`
class. It means you'll get a negative response even if something weird happens
inside it. Here is an example of unsafe division what might lead to exception:

~~~clojure
(defn rpc-div
  [_ [a b]]
  (/ a b))

(def config
  {:rpc/handlers
   {:math/div
    {:handler/function #'rpc-div}}})

(def handler
  (make-handler config))

(handler {:id 1
          :method :math/div
          :params [1 0]
          :jsonrpc "2.0"})

{:id 1
 :jsonrpc "2.0"
 :error {:code -32603
         :message "Internal error"
         :data {:method :math/div}}}
~~~

All the unexpected exceptions end up with the "Internal error" response with the
code -32603. In the console, you'll see the the logged exception:

```
10:19:35.948 ERROR farseer.handler - Divide by zero, id: 1, method: :math/div, code: -32603, message: Internal error
java.lang.ArithmeticException: Divide by zero
	at clojure.lang.Numbers.divide(Numbers.java:188)
	at demo$rpc_div.invokeStatic(form-init9886809666544152192.clj:190)
	at demo$rpc_div.invoke(form-init9886809666544152192.clj:188)
    ...
```

#### Expected Errors

In the following cases, we expect to get a negative response (here are some
examples):

- JSON parse error:

```clojure
(farseer.error/parse-error!)
```

- RPC Method not found:

```clojure
(farseer.error/not-found!
  {:rpc/message "I don't have such method"})
```

- Wrong input parameters:

```clojure
(farseer.error/invalid-params!
  {:rpc/data {:spec-explain "..."}})
```

- Internal error:

```clojure
(farseer.error/internal-error! nil caught-exception)
```

#### Raising Exceptions

The namespace `farseer.error` provides several functions for errors. Use them to
throw exceptions to get an appropriate RPC response.

Then RPC handler catches an exception, it gets the data using the `ex-data`
function. Then it looks for some special fields to compose the
response. Namely, these fields are:

- `:rpc/code`: a number representing the error. When specified, it becomes the
  `code` field of the error response.

- `:rpc/message`: a string explaining the error. Becomes the `message` field of
  the error response.

- `:rpc/data`: a map with arbitrary data sent to the client. Becomes the `data`
  field of the error response.

- `:log/level`: a keyword meaning the logging level of this error. Valid values
  are the those that the functions from `clojure.tools.logging` package accept,
  e.g. `:debug`, `:info`, `:warn`, `:error`.

- `:log/stacktrace?`: boolean, whether to log the entire stack trace or the
  message only. Useful for "method not found" or "wrong input" cases because
  there is no need for the full stack trace in such cases.

The data fetched from the exception instance gets merged with the default error
map declared in the `internal-error` variable:

```clojure
(def internal-error
  {:log/level       :error
   :log/stacktrace? true
   :rpc/code        -32603
   :rpc/message     "Internal error"})
```

Thus, if you didn't specify some of the fields, they come from this map.

There are some shortcut functions to simplify raising exceptions, namely:

- `parse-error!`
- `invalid-request!`
- `not-found!`
- `invalid-params!`
- `internal-error!`
- `auth-error!`

For all of them, the signature is `[& [data e]]` meaning that you can call the
function even without arguments. Each function has its own default `data` map
that gets merged to the `data` you passed. For example, these are default values
for the `invalid-params!` function:

```clojure
(def invalid-params
  {:log/level       :info
   :log/stacktrace? false
   :rpc/code        -32602
   :rpc/message     "Invalid params"})
```

The logging level is `:info` as this is expected behaviour plus we don't log the
whole stack trace for the same reason.

### Configuration

## Ring HTTP Handler

With this package you create an HTTP handler from your RPC configuration. The
HTTP handler follows the official Ring protocol: it's a function that takes an
HTTP request map and returns a response map. The handler uses JSON format for
transport. It's already wrapped with Ring JSON middleware that decode and encode
the request and response. You can pass other middleware stack to use something
other that JSON, say MessagePack or EDN.

Add the package:

~~~clojure
;; deps
[com.github.igrishaev/farseer-http "..."]

;; module
(ns ...
  (:require
   [farseer.http :as http]))
~~~

The package reuses the same config we wrote above. All the HTTP-related fields
have default values, so you can just pass the config to the `make-app` function:

~~~clojure
(def app
  (http/make-app config))
~~~

Now let's compose the HTTP request to the app:

~~~clojure
(def rpc
  {:id 1
   :jsonrpc "2.0"
   :method :math/sum
   :params [1 2]})

(def request
  {:request-method :post
   :uri "/"
   :headers {"content-type" "application/json"}
   :body (-> rpc json/generate-string .getBytes)})
~~~

and call it like an HTTP server:

~~~clojure
(def response
  (-> (app request)
      (update :body json/parse-string true)))

{:status 200
 :body {:id 1 :jsonrpc "2.0" :result 3}
 :headers {"Content-Type" "application/json; charset=utf-8"}}
~~~

#### Negative Responses

A quick example of how would the handler behave in case of an error:

~~~clojure
(def rpc
  {:id 1
   :jsonrpc "2.0"
   :method :math/missing ;; wrong method
   :params [nil "a"]})

(def request
  {:request-method :post
   :uri "/"
   :headers {"content-type" "application/json"}
   :body (-> rpc json/generate-string .getBytes)})

(def response
  (-> (app request)
      (update :body json/parse-string true)))

{:status 200
 :body
 {:error
  {:code -32601 :message "Method not found" :data {:method "math/missing"}}
  :id 1
  :jsonrpc "2.0"}
 :headers {"Content-Type" "application/json; charset=utf-8"}}
~~~

Pay attention that the server **always** responds with the status code 200. This
is the main deference from the REST approach. In RPC, HTTP is nothing else than
just a transport layer. Its purpose is only to deliver messages without
interfering into the pipeline. It's up to you how to check if the RPC response
was correct or not. However, the HTTP client package (see below) prides an
option to raise an exception in case of error response.

#### Batch Requests in HTTP

If your configuration allows batch requests, you can send them via HTTP. For
this, replace the `rpc` variable above with the vector of RPC maps. The result
will be a vector of response maps.

~~~clojure
(def rpc
  [{:id 1
    :jsonrpc "2.0"
    :method :math/sum
    :params [1 2]}
   {:id 2
    :jsonrpc "2.0"
    :method :math/sum
    :params [3 4]}])

(def request
  ...)

(def response
  ...)

{:status 200
 :headers {"Content-Type" "application/json; charset=utf-8"}
 :body ({:id 1 :jsonrpc "2.0" :result 3}
        {:id 2 :jsonrpc "2.0" :result 7})}
~~~

Everything said above for batch requests also apply to HTTP as well.

#### Configuration

Here is a list of HTTP options the library support:

- `:http/method` (default is `:post`) an HTTP method to listen. POST is the one
  recommended by the RPC specification.

- `:http/path` (default is `"/"`) URI path to listen. You may specify something
  like `"/api"`, `"/rpc"` or similar.

- `:http/health?` (default is `true`) whether or not the health endpoint is
  available. When it is, `GET /health` or `GET /healthz` requests receive an
  empty `200 OK` response. This is useful for monitoring your server.

- `:http/middleware` (default is the `farseer.http/default-middleware` vector) a
  list of HTTP middleware to apply to the HTTP handler. See the next section.

#### Middleware & Authorization

By default, the handler gets wrapped into a couple of middleware. These are the
standard `wrap-json-body` and `wrap-json-response` from the
`ring.middleware.json` package. The first one is set up such that passing an
incorrect JSON payload will return a proper RPC response (pay attention to the
status 200):

~~~clojure
(app {:request-method :post
      :uri "/"
      :headers {"content-type" "application/json"}
      :body (.getBytes "1aaa-")})

{:status 200,
 :headers {"Content-Type" "application/json"},
 :body "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32700,\"message\":\"Invalid JSON was received by the server.\"}}"}
~~~

Note: we use the `wrap-json-body` middleware but not `wrap-json-params` to make
it work with batch requests. The the payload is not a map, it cannot be merged
to the `:params` field.

The `:http/middleware` parameter must be a vector of middleware. Each middleware
is either a function or a vector of `[function, arg2, arg3, ...]`. In the second
case, it will be applied to the handler as `(apply function handler arg2, arg3,
...)`. For example, if a middleware takes additional params, you specify it like
a vector `[middleware, params]`.

By overriding the `:http/middleware` field, you can add your own logic to the HTTP
pipeline. Here is a quick example how you protect the handler with Basic Auth:



~~~clojure
;; ns imports
[farseer.http :as http]
[ring.middleware.basic-authentication :refer [wrap-basic-authentication]]

;; preparing a middleware stack
(let [fn-auth?
      (fn [user pass]
        (and (= "foo" user)
             (= "bar" pass)))

      middleware-auth
      [wrap-basic-authentication fn-auth? "auth" http/non-auth-response]

      middleware-stack [middleware-auth
                        http/wrap-json-body
                        http/wrap-json-resp]

      config*
      (assoc config :http/middleware middleware-stack)]
  ...)
~~~

Also, you can replace JSON middleware with the one that uses some other format
like MessagePack, Transient or whatever.

#### HTTP Context

The function `http/make-app` also takes an additional context map. This map will
be merged with the data the RPC function accepts when being called.

~~~clojure
(def app
  (make-app config {:app/version "0.0.1"}))

(defn rpc-func [context params]
  {:message (str "The version is " (:app/version context))})
~~~

The HTTP handler adds the `:http/request` item into the context. This is the
instance of the request map that the handler accepts. Having the request, you
can you handle some extra logic in you function. For example, some middleware
can supplement the `:user` field to the request.

~~~clojure
(defn some-rpc [context params]
  (let [{:http/keys [request]} context
        {:keys [user]} request]
    (when-not request
      (throw ...))))
~~~

## Jetty Server

This sub-package allows you to run an RPC server using Jetty Ring adapter. Add
it to the project:

~~~clojure
;; deps
[com.github.igrishaev/farseer-jetty "..."]

;; require
(require '[farseer.jetty :as jetty])
~~~

All the config fields of Jetty have default values, so you can just pass a
minimal config we've been using so far.

~~~clojure
(def server
  (jetty/start-server config))

;; #object[org.eclipse.jetty.server.Server 0x3e82fe49 "Server@3e82fe49{STARTED}[9.4.12.v20180830]"]
~~~

The default port is 8080. Now that your server is being run, test it with cURL:

~~~bash
curl -X POST 'http://127.0.0.1:8080/' \
  --data '{"id": 1, "jsonrpc": "2.0", "method": "math/sum", "params": [1, 2]}' \
  -H 'content-type: application/json' | jq

{
  "id": 1,
  "jsonrpc": "2.0",
  "result": 3
}
~~~

Pay attention to the `content-type` header. Without it, the request payload won't
be decoded and the call will fail.

To stop the sever, pass it to the `stop-server` function:

~~~clojure
(jetty/stop-server server)
~~~

The `start-server` function also accepts a second optional argument which is a
context map.

### Configuration

[jetty-params]: https://github.com/ring-clojure/ring/blob/master/ring-jetty-adapter/src/ring/adapter/jetty.clj#L162

- `:jetty/port` (8080 by default) the port to listen to.

- `:jetty/join?` (`false` by default) whether or not to wait for the server
  being stopped. When it's `true`, the main thread will hang until you press
  Ctrl-C.

- any other [Jetty-related key][jetty-params] with the `:jetty/` namespace, for
  example: `:jetty/ssl-context`, `:jetty/max-threads` and so on. The library
  will scan the config for the `:jetty/`-prefixed keys, select them, unqualify
  and pass to the `run-jetty` function.

### With-server macro

The macro `with-server` temporary spawns an RPC server. It accepts a config, an
optional context map and a block of code to execute.

~~~clojure
(jetty/with-server [config {:foo 42}]
  (println 1 2 3))
~~~

### Component

The Jetty sub-package also provides a component for use with the Component
Clojure library. The `jetty/component` function creates a component. It takes a
config and an optional context map.

~~~clojure
(def jetty
  (jetty/component config {:some "field"}))
~~~

Now that you have an initiated component, you can start and stop it with
functions from the Component library.

~~~clojure
(require '[com.stuartsierra.component :as component])

(def jetty-started
  (component/start jetty))

;; Here, the server starts working

(def jetty-stopped
  (component/stop jetty-started))

;; Now it's shut down.

~~~

Of course, it's better to place the component into a system. One more benefit of
a system is, all the dependencies will become a context map. For example, if
your Jetty component depends on the database, cache, Kafka, and waterier else,
you'll have them all in the context map.

~~~clojure
(defn make-system
  [rpc-config db-config cache-config]
  (component/system-using
   (component/system-map
    :cache (cache-component cache-config)
    :db-pool (db-component db-config)
    :rpc-server (jetty/component rpc-config))
   {:rpc-server [:db-pool :cache]}))
~~~

The function above will make a new system which consists from the RPC server,
cache and database pooling connection. Once the system gets started, the context
map of all RPC functions will have the `:db-pool` and `:cache` keys.

~~~clojure
(defn rpc-user-get-by-id
  [{:keys [db-pool cache]} [user-id]]
  (or (get-user-from-cache cache user-id)
      (get-user-from-db db-pool user-id)))
~~~

## HTTP Stub

This package provides a couple of macros for making HTTP RPC stubs. These are
local HTTP servers that run on your machine. The difference with the Jetty
package is that a stub returns a pre-defined data which is useful for testing
your application.

Imagine you have a piece of code that interacts with two RPC endpoints. To make
this code well tested, you need to cover the cases:

- both sources work fine;
- the first one works, the second returns an error;
- the first one is unavailable, the second one works;
- neither of them work.

The package provides the `with-stub` macro which accepts a config map and a
block of code. The config must have the `:stub/handlers` field which is a map of
method => result. Like this:

~~~clojure
(def config
  {:stub/handlers
   {:user/get-by-id {:name "Ivan"
                     :email "test@test.com"}
    :math/sum 42}})
~~~

As the Stub package works on top of Jetty, it takes into account all the Jetty
keys. For example, to specify the port number, pass the `:jetty/port` field to
the config:

~~~clojure
(def config
  {:jetty/port 18080
   :stub/handlers {...}})
~~~

In the example above, defined the handlers such that the methods
`:user/get-by-id` and `:math/sum` would always return the same response. Now, to
run a server out from this config, there is the macro `with-stub`:

~~~clojure
(stub/with-stub config
  ;; Execute any expressions
  ;; while the RPC server is running.
  )
~~~

Inside the macro, while the server is running, you can reach it as you normally
do. If you send either `:user/get-by-id` or `:math/sum` requests to it, you'll
get the result you defined in the config. Quick check with cURL:

~~~bash
curl -X POST 'http://127.0.0.1:8080/' \
  --data '{"id": 1, "jsonrpc": "2.0", "method": "math/sum", "params": [1, 2]}' \
  -H 'content-type: application/json' | jq

{
  "id": 1,
  "jsonrpc": "2.0",
  "result": 42
}
~~~

#### Multiple Stub

If you interact with more than one RPC server at once, there is a multiple
version of this macro called `with-stub`. It takes a vector of config maps. For
each one, it runs a local HTTP Stub. All of them get stopped once you exit the
macro.

~~~clojure
(stub/with-stubs [config1 config1 ...]
  ...)
~~~

#### Tests

To test you application with stubs, you need:

- define a port for the HTTP stub, e.g. 18080;
- pass this port to the stub config: `:jetty/port ...`;
- wrap the testing code with the `with-stub` macro;
- somehow, point the code which interacts with RPC to the local address like this
  one: `http://127.0.0.1:18080/...`.

Having everything said above, you can easily check how does your application
behave when getting a positive or a negative responses from the RPC server.

[stub_test]: https://github.com/igrishaev/farseer/blob/master/farseer-stub/test/farseer/stub_test.clj

You can check out the source code of the [testing module][stub_test] as an
example.

#### Negative Responses

The result of a method can be not only regular data but also a function. Inside
it, you can raise an exception or even trigger something weird to imitate a
disaster. For example, to divide by zero:

~~~clojure
(def config
  {:stub/handlers
   {:some/failure
    (fn [& _]
      (/ 0 0))}})
~~~

This would lead to a real exception on the server side. Another way of
triggering the negative response is to pass one of the predefined
functions. These are:

- `stub/invalid-request`,
- `stub/not-found`,
- `stub/invalid-params`,
- `stub/internal-error`,
- `stub/auth-error`,

and some others. Passing them will return an RPC error result. If you want to
play the scenario when a user is not found on the server, compose the config:

~~~clojure
(def config
  {:stub/handlers
   {:user/get-by-id stub/not-found}})
~~~

## HTTP Client

This package is to communicate with an RPC Server by HTTP protocol. It relies on
`clj-http` library for making HTTP requests. Add it to the project:

~~~clojure
;; deps
[com.github.igrishaev/farseer-client ...]

;; module
[farseer.client :as client]
~~~

To reach an RPC server, first you create an instance of the client. This is done
with the `make-client` function which accepts the config map:

~~~clojure
(def config-client
  {:http/url "http://127.0.0.1:18080/"})

(def client
  (client/make-client config-client))
~~~

There is only one mandatory field in the config: the `:http/url` one which is
the endpoint of the server. Other fields get default values.

For further experiments we will spawn a local Jetty RPC server and will work
with it using the client.

~~~clojure
(def config
  {:jetty/port 18080
   :rpc/handlers
   {:math/sum
    {:handler/function #'rpc-sum
     :handler/spec-in :math/sum.in
     :handler/spec-out :math/sum.out}}})

(def server
  (jetty/start-server config))
~~~

Once you have the client, perform request with the `client/call` function. It
accepts the client, method, and optional parameters.

~~~clojure
(def response
  (client/call client :math/sum [1 2]))

;; {:id 81081, :jsonrpc "2.0", :result 3}
~~~

The parameters might be either a vector or map. Also, if the method doesn't
accept parameters, you may skip it.

~~~clojure
;; map params
(client/call client :user/create
             {:name "Ivan" :email "test@test.com"})

;; no params
(client/call client :some/side-effect)
~~~

An example of a negative response:

~~~clojure
(client/call client :math/sum [nil "a"])

{:error
 {:code -32602,
  :message "Invalid params",
  :data
  {:explain
   "nil - failed: number? in: [0] at: [0] spec: :math/sum.in\n\"a\" - failed: number? in: [1] at: [1] spec: :math/sum.in\n",
   :method "math/sum"}},
 :id 73647,
 :jsonrpc "2.0"}
~~~

You won't get an exception; the result shown above is just data. If you prefer
exceptions, you can adjust the client configuration (see below).

#### Configuration

The following fields affect the client's behaviour.

- `:rpc/fn-before-send` (default is `identity`) a function which is called
  before the HTTP request gets sent to the server. It accepts the Clj-http
  request map and should return it as well. The function useful for signing
  requests, authentication and so on.

- `:rpc/fn-id` (default is `:id/int`) determines an algorithm for generating
  IDs. The `:id/int` value means an ID will be a random integer; `:id/uuid`
  stands for a random UUID. You can also pass a custom function of no arguments
  that must return either integer or string.

- `:rpc/ensure?` (default is `false`)


- `:http/method` (default is `:post`)

- `:http/headers` (default is `{:user-agent "farseer.client"}`)

- `:http/as` (`:json`)

- `:http/content-type` (`:json`)

As you have already guessed, the HTTP package takes into account all the keys
prefixed with the `:http/` namespace. These are the standard Clj-http keys, .e.g
`:http/socket-timeout`, `:http/throw-exceptions?` and others, so you configure
the HTTP part as you want. When making a request, the client scans the config
for the `:http/`-prefixed keys, selects them, removes the namespace and passes
to the `clj-http/request` function as a map.

The `:conn-mgr/` keys specify options for the connection manager:

- `:conn-mgr/timeout`,
- `:conn-mgr/threads`,
- `:conn-mgr/default-per-route`,
- `:conn-mgr/insecure?`,

and others. The have the default values copied from Clj-http. The connection
manager is not created by default. You need to setup it manually (see below).

#### Handling Response

By default, the calling the server just returns the body of the HTTP
response. Thus, it's up to you how to handle the `:result` and `:error`
fields. But sometimes, the good old exception-based approach is better: you
either get a result or an error pops up.

The `:rpc/ensure?` option is exactly for that. When it's false (which is default
behaviour), you just a parsed body of the HTTP response. When it's true, there
is a following condition:

- for a positive response (no `:error` field) you'll get the content of the
  `:result` field. For example:

~~~clojure
(def config-client
  {:rpc/ensure? true
   :http/url "http://127.0.0.1:18080/"})

(def client
  (client/make-client config-client))

(client/call client :math/sum [1 2])
;; 3
~~~

For the error cases, you'll get an exception:

~~~clojure
(client/call client :math/sum [1 "two"])

17:04:34.780 INFO  farseer.handler - RPC error, id: 94415, method: math/sum, code: -32602, message: Invalid params

Unhandled clojure.lang.ExceptionInfo
RPC error, id: 94415, method: :math/sum, code: -32602, message: Invalid
params
#:rpc{:id 94415,
      :method :math/sum,
      :code -32602,
      :message "Invalid params",
      :data
      {:explain "\"two\" - failed: number? in: [1] at: [1] spec: :math/sum.in\n",
       :method "math/sum"}}
             client.clj:  122  farseer.client/ensure-handler
             client.clj:   97  farseer.client/ensure-handler
             client.clj:  148  farseer.client/make-request
             client.clj:  128  farseer.client/make-request
             client.clj:  187  farseer.client/call
             client.clj:  179  farseer.client/call
~~~

Pay attention that the `:rpc/ensure?` option doesn't affect batch requests (see
below).

#### Auth

[clj-http-auth]: https://github.com/dakrone/clj-http#authentication

Handling authentication for the client is simple. Clj-http [already
covers][clj-http-auth] most of the authentication types, so you only need to
pass proper options to the config. If the server is protected with Basic auth,
you extend the config like this:

~~~clojure
(def config-client
  {:http/url "http://127.0.0.1:18080/"
   :http/basic-auth ["user" "password"]})
~~~

For oAuth2, you pass another key:

~~~clojure
(def config-client
  {:http/url "http://127.0.0.1:18080/"
   :http/oauth-token "***********"})
~~~

If the server requires a constant token, you can specify it directly in headers:

~~~clojure
(def config-client
  {:http/url "http://127.0.0.1:18080/"
   :http/headers {"authorization" "Bearer *********"}})
~~~

Finally, the `:rpc/fn-before-send` parameter allows your to do everything with
the request before it gets sent to the server. There might be a custom function
which supplements the request with some custom headers that are calculated on
the fly. For example:

~~~clojure
(defn sign-request
  [{:as request :keys [body]}]
  (let [body-hash (calc-body-hash body)
        sign (sign-body-hash body-hash "*******")
        header (str "Bearer " sign)]
    (assoc-in request [:headers "authorization"] header)))

(def config-client
  {:http/url "http://127.0.0.1:18080/"
   :rpc/fn-before-send sign-request})
~~~

#### Notifications

A notification is when you're not interested in the response from the server. To
send a notification, use the `client/notify` function. Its signature looks the
same: the client, method, and optional params. The result will be `nil`.

~~~clojure
(client/notify client :math/sum [1 2])
;; nil
~~~

#### Batch Requests

To send batch requests, there is the `client/batch` function. It takes the
client and a vector of tasks. Each task is a pair of (method, params). Like
this:

~~~clojure
(client/batch client
              [[:math/sum [1 2]]
               [:math/sum [2 3]]
               [:math/sum [3 4]]])

[{:id 51499 :jsonrpc "2.0" :result 3}
 {:id 45992 :jsonrpc "2.0" :result 5}
 {:id 84590 :jsonrpc "2.0" :result 7}]
~~~

Some important notes on this:

- There will be only one HTTP request under the hood.

- The order of the result maps always match the order of the initial tasks.

- If one of the tasks fails, you'll get a negative map for it. The whole request
  won't crush.

~~~clojure
(client/batch client
              [[:math/sum [1 2]]
               [:math/sum ["aa" nil]]
               [:math/sum [3 4]]])

[{:id 75623 :jsonrpc "2.0" :result 3}
 {:error
  {:code -32602
   :message "Invalid params"
   :data
   {:explain "\"aa\" - failed: number? in: [0] at: [0] spec: :math/sum.in\nnil - failed: number? in: [1] at: [1] spec: :math/sum.in\n"
    :method "math/sum"}}
  :id 43075
  :jsonrpc "2.0"}
 {:id 13160 :jsonrpc "2.0" :result 7}]
~~~

The `:rpc/ensure?` option doesn't apply to batch requests (which is a subject to
change in the future).

Sometimes, you want one of the tasks in a batch to be a notification. To make a
task a notification, prepend its vector with the `^:rpc/notify` metadata tag:

~~~clojure
(client/batch client
              [[:math/sum [1 2]]
               ^:rpc/notify [:math/sum [2 3]]
               [:math/sum [3 4]]])

[{:id 54810 :jsonrpc "2.0" :result 3}
 {:id 34377 :jsonrpc "2.0" :result 7}]
~~~

#### Connection Manager (Pool)

[pool]: https://github.com/dakrone/clj-http#persistent-connections

Clj-http offers a [connection manager][pool] for HTTP requests. It's a pool of
open TCP connections. Sending requests within a pool is much faster then opening
and closing new connection every time. The package provides some bits to handle
connection manager for the client.

The function `client/start-conn-mgr` takes either a config or a client and
returns the same object with the new connection manager associated with it under
the `:http/connection-manager` key. If you pass the result to the `client/call`
function, it will take the manager into account, and the request will work
faster.

The function considers the keys which start with the `:conn-mgr/`
namespace. These keys become a map of standard parameters for connection
manager.

~~~clojure
(def config-client
  {:conn-mgr/timeout 5
   :conn-mgr/threads 4
   :http/url "http://127.0.0.1:18080/"})

(def client
  (-> config-client
      client/make-client
      client/start-conn-mgr))
~~~

The opposite function `client/stop-conn-mgr` stops the manager (if present) and
returns the object without the key.

~~~clojure
(client/stop-conn-mgr client)
~~~

The macro `client/with-conn-mgr` enables the connection manager temporary. It
takes a binding form and a block of code to execute. Inside the macro, the
object bound to the first symbol from the vector form will carry the open
manager.

~~~clojure
;; a client without a pool
(def client
  (client/make-client config-client))

;; temporary assing a pool
(client/with-conn-mgr [client-mgr client]
  (client/call client-mgr :math/sum [1 2]))
~~~

#### Component

Since the client might have a state (a connection manager), you can put it into
the system. There is a function `client/component` which returns an HTTP client
charged with the `start` and `stop` methods. These methods turn on and off
connection pool for the client.

~~~clojure
;; no pool yet
(def client
  (client/component config-client))

;; enabling the pool
(def client-started
  (component/start client))

;; closing the pool
(component/stop client-started)
~~~

## Documentation Builder

Perhaps you have already noticed that the config map for the server has enough
data to be rendered as a document. It would be nice to pass it into a template
and generate a file each time you build or the application. The Docs package
services exactly this purpose.

Add the `com.github.igrishaev/farseer-doc` library into your project:

~~~clojure
;; deps
[com.github.igrishaev/farseer-doc ...]

;; ns
[farseer.doc :as doc]
~~~

Pay attention that generating a docfile is usually a separate task, but not a
part of business logic. That's why the application **must not include that
library in production**. The `:dev`-specific dependencies would be a better
place for this package.

### Configuration

To generate the doc, you extend the server config with the keys that have
`:doc/` namespace. Here is an example:

~~~clojure
(def config
  {:doc/title "My API"
   :doc/description "Long API Description"

   :rpc/handlers
   {:user/delete
    {:doc/title "Delete a user by ID"
     :doc/description "Long text for deleting a user."
     :handler/spec-in pos-int?
     :handler/spec-out (s/keys :req-un [:api/message])}

    :user/get-by-id
    {:doc/title "Get a user by ID"
     :doc/description "Long text for getting a user."
     :doc/ignore? false
     :doc/resource "docs/user-get-by-id.md"
     :handler/spec-in int?
     :handler/spec-out
     (s/map-of keyword? (s/or :int int? :str string?))}

    :hidden/api
    {:doc/title "Non-documented API"
     :doc/ignore? true
     :handler/spec-in any?
     :handler/spec-out any?}}})
~~~

Here is the list of the fields used for documentation.

- `:doc/title` (string). Describes either the API in general or an RPC method.

- `:doc/description` (string). A description of an API in or an RPC method.

- `:doc/resource` (string). A path to a resource with the detailed text with
  examples, edge cases and so on. Useful when the text grows.

- `:doc/endpoint` (string). An URL of this RPC server.

- `:doc/ignore?` (boolean, `false` by default). When true, the method is not
  included into the documentation.

- `:doc/sorting` (keyword, `:method` or `:title`). How to sort RPC methods. The
  `:method` keyword means to sort by machine names, e.g. `:user/get-by-id`. The
  `:title` means to sort by the `:doc/title` field, e.g. "Get user by ID".

### Building

Not that you have a documentation-powered config, render it with the
`generate-doc` function:

~~~clojure
(doc/generate-doc
   config
   "templates/farseer/default.md"
   "dev-resources/default-out.md")
~~~

[doc-default]: https://raw.githubusercontent.com/igrishaev/farseer/master/farseer-doc/resources/templates/farseer/default.md

This function takes a config map, a resource template and a path of the output
file. The Doc package provides the [default Markdown template][doc-default] which
can be found by the path `"templates/farseer/default.md"`.

In your project, most likely you create a `dev` namespace with a function that
builds the documentation file. Every time the application gets built in CI, you
generate a file and host it somewhere.

### Demo

[doc-demo]: https://github.com/igrishaev/farseer/blob/master/farseer-doc/dev-resources/default-out.md

You can checkout a [real demo][doc-demo] generated by the test module. The file
lists all the non-ignored methods and their specs. The specs are put under
dropdown items as sometimes they might be huge.

### Selmer & Context

[selmer]: https://github.com/yogthos/Selmer

The Doc package uses the great [Selmer library][selmer] which is inspired by
Django Templates. You can pass your own template, and not only Markdown one, but
HTML, AsciiDoc, or LaTeX. The template might carry any graphic elements, your
logo, JavaScript, and so on.

The Doc package passes the config not directly but with transformation. Here is
an example of the context map that you have when rendering a template. Note that
all the keys are free from namespaces. The `:handlers` field is not a map but a
vector of maps sorted according to the `:doc/sorting` option.

~~~clojure
{:title "My API"
 :description "Long API Description"
 :resource nil
 :handlers
 ({:method "user/delete"
   :title "Delete a user by ID"
   :description "Long text for deleting a user."
   :resource nil
   :spec-in {:type "integer" :format "int64" :minimum 1}
   :spec-out
   {:type "object"
    :properties {"message" {:type "string"}}
    :required ["message"]}}
  {:method "user/get-by-id"
   :title "Get a user by ID"
   :description "Long text for getting a user."
   :resource "\n### Get user by ID examples\n\n........"
   :spec-in {:type "integer" :format "int64"}
   :spec-out
   {:type "object"
    :additionalProperties
    {:anyOf [{:type "integer" :format "int64"} {:type "string"}]}}})}
~~~

### Rendering Specs

[spec-tools]: https://github.com/metosin/spec-tools
[json-schema]: https://github.com/metosin/spec-tools/blob/master/docs/04_json_schema.md

The `:handler/spec-in` and `:handler/spec-out` fields get transformed to JSON
Schema using the [Spec-tools][spec-tools] library. You may see the result of
transformation in the context map above. For more control on transformation,
check out [the manual page][json-schema] from the repository.

To render the spec in a template, there is a special filter `json-pretty`. It
turns the Clojure data into a JSON string being well printed. To prevent quoting
some symbols, add the `safe` filter to the end. Everything together gives the
following snippet:

```jinja
{% if handler.spec-in %}
<details>
<summary>Intput schema</summary>

~~~json
{{ handler.spec-in|json-pretty|safe }}
~~~

</details>
{% endif %}
```

Pay attention to the empty lines before and after the JSON block. Without them,
GitHub renders the content in a weird way.

## Ideas & Further Development

It would be nice to:

- Keep the entire server config in an EDN file. The functions should be resolved
  by their full symbols.

- Provide a nested map like method => overrides. With this map, one could
  specify custom options for specific methods. For example, to enable batch
  requests in common, but disallow them for specific methods.

- Develop a browser version of the client. The module would have the same
  functions but rely on Js Fetch API.

- Create a re-frame wrapper for this client. Instead of calling functions, one
  triggers events.

## Author

Ivan Grishaev, 2021
https://grishaev.me
