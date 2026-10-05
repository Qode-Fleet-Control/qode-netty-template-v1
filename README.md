# Netty template

Provisioned from [`Qode-Fleet-Control/fleet-template-v1`](https://github.com/Qode-Fleet-Control/fleet-template-v1) — the fleet
lifecycle contract (`bin/`, `fleet.conf`, deploy workflows) with a
Netty HTTP server starter laid on top.

A Netty 4.2 HTTP server on Java 21, Maven build: `ServerBootstrap` + `HttpServerCodec` + `HttpObjectAggregator` and one handler that answers `GET /` with a small JSON document and `GET /health` with `{"status":"ok"}` (404 otherwise).

## Origin

Hand-written — Netty ships no project generator. Shaped after Netty's own example
`io.netty.example.http.helloworld` (HttpHelloWorldServer / Initializer / Handler, branch 4.2),
using 4.2's `MultiThreadIoEventLoopGroup` + `NioIoHandler`.

## Verified

**Not yet verified end to end on docker.** On 2026-10-05 the shared docker host's disk sat at
0-1 GB free for over 90 minutes (other builds were running), under the 6 GB floor this
scaffold's verification requires, so the `docker compose` build/run check was not run.
Run it before trusting the image:

    .claude/skills/migrate-docker-runtime/scripts/verify.sh . <port>   # run -> health 200, restart, stop

What did pass, on 2026-10-05:

- `mvn -B package` **with the test suite** in `maven:3.9-eclipse-temurin-21` (the Dockerfile's
  build image) — compiles, tests green, artifacts produced.

## Run it

**On the fleet** — nothing to do: the fleet clones the repo, injects `PORT` (plus
`DATABASE_URL` and the workspace's other services) and calls `bin/run`, which runs
fleet.conf's `DOCKER_BUILD_CMD` (`docker compose build`) then `DOCKER_START_CMD`
(`docker compose up`, in the foreground). The health check hits `/health`.

**With docker**, locally:

    PORT=8080 bin/run                  # the fleet's docker runtime
    docker compose up --build            # or plain compose; serves on ${PORT:-8080}
    curl http://localhost:8080/health

**Without docker** — a JDK 21 and Maven 3.9 (`mvn`) on `PATH`:

    FLEET_RUNTIME=process PORT=8080 bin/run

| step | command |
|---|---|
| install | `mvn -B -q dependency:go-offline` |
| build | `mvn -B -q package -DskipTests` |
| start | `env PORT="$PORT" java -jar target/app.jar` |

    ./bin/run       # install, build, start in the foreground
    ./bin/start     # start from existing build artifacts
    ./bin/restart   # rebuild and restart
    ./bin/stop      # stop whatever holds the port

## Serving

Listens on `0.0.0.0:$PORT` (default `8080`), read from the environment at run
time. The app is served at the root (`/`) of its own hostname
(`https://<hash>.<FLEET_APP_DOMAIN>/`), so every route, redirect and asset URL is
a plain root path. `/health` answers 200 for the fleet's health check.

## Layout

- `src/main/java/world/qode/app/HttpServer.java` — bootstrap, binds `0.0.0.0:$PORT`.
- `src/main/java/world/qode/app/HttpServerInitializer.java` — the pipeline.
- `src/main/java/world/qode/app/HttpServerHandler.java` — routing and responses.
- `src/test/java/...` — handler tests on an `EmbeddedChannel`.
- `pom.xml` — shades Netty into one runnable `target/app.jar`.

## What differs from stock output

- No generator exists; everything above is hand-written (see Origin).
- Added the fleet harness: `bin/`, `fleet.conf`, `Dockerfile`, `compose.yaml`, `.dockerignore`, `.gitignore`, `.github/workflows/`, `docs/fleet-lifecycle.md`.
