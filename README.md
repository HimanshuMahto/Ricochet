# [Ricochet](https://github.com/HimanshuMahto/Ricochet)

Project Url: https://github.com/HimanshuMahto/Ricochet

**A lightweight caching reverse-proxy server, built with Spring Boot.**

Like its namesake, Ricochet bounces your request off an origin server and straight back — fast. It sits in front of any origin, forwards incoming requests to it, and **caches the responses**. Repeat requests are served instantly from the cache instead of hitting the origin again — and every response tells you whether it came from the cache (`X-Cache: HIT`) or the origin (`X-Cache: MISS`).

---

## ✨ Features

- **Transparent forwarding** — any request to the proxy is forwarded to the configured origin, preserving the path, query string, status code, and headers.
- **Response caching** — successful responses are cached in memory; identical repeat requests are served from the cache without touching the origin.
- **Cache indicators** — every response carries an `X-Cache: HIT` or `X-Cache: MISS` header.
- **Cache clearing** — a single command empties the running server's cache.
- **Zero config** — just point it at a port and an origin.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven (or use the bundled `./mvnw` wrapper)

### Build

```bash
./mvnw clean package
```

This produces a runnable jar at `target/cachingProxy-0.0.1-SNAPSHOT.jar`.

---

##️ Usage

Start the proxy by giving it a **port** to listen on and an **origin** to forward to:

```bash
java -jar target/cachingProxy-0.0.1-SNAPSHOT.jar --port <number> --origin <url>
```

**Example** — proxy `localhost:3000` to `http://dummyjson.com`:

```bash
java -jar target/cachingProxy-0.0.1-SNAPSHOT.jar --port 3000 --origin http://dummyjson.com
```

Now a request to `http://localhost:3000/products` is forwarded to `http://dummyjson.com/products`.

> During development you can skip the build and run directly with Maven:
> ```bash
> ./mvnw spring-boot:run -Dspring-boot.run.arguments="--port 3000 --origin http://dummyjson.com"
> ```
>
> For a clean `ricochet` command, alias the jar:
> ```bash
> alias ricochet='java -jar /full/path/to/target/cachingProxy-0.0.1-SNAPSHOT.jar'
> ricochet --port 3000 --origin http://dummyjson.com
> ```

### Options

| Flag | Description |
|------|-------------|
| `--port <number>` | Port the proxy listens on. |
| `--origin <url>` | Base URL that requests are forwarded to (must include `http://` or `https://`). |
| `--clear-cache` | Clears the cache of a **running** proxy, then exits. Pair with `--port` to target the right instance (defaults to `8080`). |

---

## See it in action

```bash
# First request — fetched from the origin
$ curl -i http://localhost:3000/products
HTTP/1.1 200
Content-Type: application/json
X-Cache: MISS
...

# Same request again — served from the cache
$ curl -i http://localhost:3000/products
HTTP/1.1 200
Content-Type: application/json
X-Cache: HIT
...

# Clear the cache of the server running on port 3000
$ ricochet --clear-cache --port 3000
Cache cleared on port 3000

# Next request is a MISS again — the cache was emptied
$ curl -i http://localhost:3000/products
X-Cache: MISS
```

---

## How it works

```
                          ┌──────────────── Ricochet (port 3000) ────────────────────┐
  client                  │                                                          │
   GET /products ────────▶│  1. build a cache key from the request (path + query)    │
                          │  2. look it up in the in-memory cache                    │
                          │                                                          │
                          │     HIT  → return the stored response   (X-Cache: HIT)   │
                          │     MISS → forward to the origin, store  (X-Cache: MISS) ─┼──▶ http://dummyjson.com/products
                          │            the response, then return it                  │◀── response
                          └──────────────────────────────────────────────────────────┘
```

- **The cache is a key → response map** held in memory (`ConcurrentMapCacheManager`). It lives for as long as the server process runs.
- **The key** is the incoming request's path plus query string, so `/products?limit=5` and `/products` are cached separately.
- **The value** is the full origin response (status + headers + body).
- **`--clear-cache`** runs the program in a lightweight "client mode": instead of starting a server, it sends a `POST /_clear` to the already-running instance, which empties its cache.

---

## Project structure

| File | Responsibility |
|------|----------------|
| `CachingProxyApplication.java` | Entry point — parses `--port` / `--origin` / `--clear-cache` and boots the server (or runs clear-cache client mode). |
| `PortForwarder.java` | The proxy controller — forwards requests, caches responses, adds `X-Cache`, and exposes the `/_clear` endpoint. |
| `CacheConfig.java` | Defines the in-memory `CacheManager` bean. |

---

## Tech stack

- **Java 17**
- **Spring Boot 4.1** (Web MVC, embedded Tomcat)
- **Spring `RestClient`** for outbound requests to the origin
- **Spring Cache** (`ConcurrentMapCacheManager`) for in-memory caching

---

## Notes & limitations

- The cache is **in memory**, so it is cleared whenever the server restarts.
- `--clear-cache` requires the target server to be **running** (it clears that instance over HTTP).
- Only `GET` requests are cached/forwarded in the current implementation.
