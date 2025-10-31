# Spring Tx Board

**Spring Tx Board** is an intelligent, auto-configurable transaction monitoring and diagnostics tool for Spring-based
applications. It provides **deep visibility** into transactional behavior — capturing execution time, nested
transactions, executed SQL queries, connection usage, and post-transaction activity for Spring-based applications.

With both **in-console logs** and a **built-in web dashboard**, developers can **analyze transaction performance**,
**detect slow or alarming operations**, and **visualize query patterns** — all without adding any manual instrumentation
or modifying existing code.
![Spring Tx Board Image](spring-tx-board-looks-like.png)

## Features

* Autoconfigures itself when added as a dependency
* Captures transaction start/end time, duration, thread, method and others info
* Can observe the complex hierarchy of inner transactions
* Track each step of transactions and database connections
* In-memory and Redis-based storage support
* Alarming threshold to flag slow transactions and database connections
* Lightweight API endpoint for fetching transaction logs
* Supports filtering, sorting, pagination, and duration distribution
* Export transactions with CSV formated file

---

## Getting Started

Good question — your current README is **already quite good**, but yes, it can be improved slightly to make the
compatibility information **clearer and more professional**.

Right now, it tells users which dependency to use, but it doesn’t explicitly explain *why* there are two versions or
*which one* to pick beyond the section titles.

Here’s how you can **update and finalize your README.md section** so it’s crystal clear and developer-friendly 👇

---

## Getting Started

### 1. Add Dependency

Add the following dependency to your `pom.xml`, based on your **Spring Boot** version.

#### For Spring Boot **3.x or higher**

> Built with **Spring Boot 3.2.0**, compatible with all **3.x** versions and later.

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Mamun-Al-Babu-Shikder</groupId>
        <artifactId>spring-tx-board</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>
```

#### For Spring Boot **2.4.x – 2.7.x**

> Use this version if your project still runs on **Spring Boot 2.x**.

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Mamun-Al-Babu-Shikder</groupId>
        <artifactId>spring-tx-board-boot2</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>
```

### 2. Configuration

```yaml
sdlc:
  pro:
    spring:
      tx:
        board:
          enabled: true
          log-type: simple # simple | details
          storage: in_memory # in_memory | redis
          alarming-threshold:
            transaction: 1000 # 1000 ms
            connection: 1000 # 1000 ms
          duration-buckets: [ 100, 500, 1000, 2000, 5000 ]
```

> `alarming-threshold.transaction`: If any transaction duration (ms) took more than the configured value then the
> transaction will be highlighted.
> `alarming-threshold.connection`: The database connection will be highlighted if the connection lease duration (ms)
> took higher than configured value.

## Web UI

If your application includes Spring Web, a minimal built-in UI is accessible at:

> http://localhost:8080/tx-board/ui

This dashboard provides a real-time view of transaction activity including filtering, status, execution time, and more.

## Storage Options

* **IN\_MEMORY** (default): Simple, thread-safe `List` with in-memory counters
* **REDIS** (planned): Store and distribute logs across instances (not-implemented)

## Configurable transaction logging

Spring Tx Board emits a completion log when a transaction ends. You can choose between two logging modes via the
property below:

- `sdlc.pro.spring.tx.board.log-type=SIMPLE` (default)
- `sdlc.pro.spring.tx.board.log-type=DETAILS`

**Health-based severity**

- Healthy transaction (<= alarming thresholds) logs at INFO level.
- Unhealthy transaction (exceeds transaction duration or connection occupied-time thresholds) logs at WARN level.

**Example configuration (YAML)**

```yaml
sdlc.pro.spring.tx.board:
  enabled: true
  # select logging style
  log-type: details  # simple | details
  # thresholds used to determine INFO vs WARN
  alarming-threshold:
    transaction: 1000   # 1000 ms
    connection: 1000    # 1000 ms
```

### Examples

**SIMPLE** (healthy -> INFO)

```
Transaction [UserService.createUser] took 152 ms, Status: COMMITTED
```

**SIMPLE** (unhealthy -> WARN)

```
Transaction [UserService.createUser] took 2150 ms, Status: COMMITTED, Connections: 3, Queries: 12
```

**DETAILS** (healthy -> INFO)

```
[TX-Board] Transaction Completed:
  • ID: 123
  • Method: UserService.createUser
  • Propagation: REQUIRED
  • Isolation: DEFAULT
  • Status: COMMITTED
  • Started At: 2025-08-17T10:15:30.123Z
  • Ended At: 2025-08-17T10:15:30.275Z
  • Duration: 152 ms
  • Connections Acquired: 2
  • Executed Query Count: 5
  • Post Transaction Query Count: 0
```

**DETAILS** (with inner transactions, unhealthy -> WARN)

```
[TX-Board] Transaction Completed:
  • ID: 789
  • Method: CheckoutService.checkout
  • Propagation: REQUIRED
  • Isolation: DEFAULT
  • Status: COMMITTED
  • Started At: 2025-08-17T10:15:30.123Z
  • Ended At: 2025-08-17T10:15:32.323Z
  • Duration: 2200 ms
  • Connections Acquired: 4
  • Executed Query Count: 18
  • Post Transaction Query Count: 0
  • Inner Transactions:
    ├── InventoryService.reserveStock (Duration: 450 ms, Propagation: MANDATORY, Isolation: DEFAULT, Status: COMMITTED)
    ├── PaymentService.charge (Duration: 1200 ms, Propagation: MANDATORY, Isolation: DEFAULT, Status: COMMITTED)
    └── EmailService.sendReceipt (Duration: 120 ms, Propagation: MANDATORY, Isolation: DEFAULT, Status: COMMITTED)
```

## Developer Usage

Just annotate your service methods with `@Transactional` or use `TransactionTemplate`, and Spring Tx Board will
automatically hook into them using transaction lifecycle listeners.

```java
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    @Transactional
    public void placeOrder() {
        // Your logic here
    }
}
```

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {
    @Autowired
    private TransactionTemplate template;

    @Transactional
    public void placeOrder() {
        template.executeWithoutResult(transactionStatus -> {
            // Your logic here
        });
    }
}
```

No additional annotations or API calls required.

## Using the demo project for development

You can use the community demo project at `https://github.com/jamilxt/tx-board-banking-demo.git` to develop and test
changes to this library quickly. The demo is a small Spring Boot application that integrates `spring-tx-board` so you
can
see the UI, logs, and transaction traces locally.

Follow the demo project's `README.md` for setup and usage
instructions: https://github.com/jamilxt/tx-board-banking-demo.git

## Utilities

### Duration Distribution

Calculates and buckets transaction durations into defined ranges like:

* `0-100ms`
* `100-500ms`
* `500ms+`

### Configuration Metadata

Spring Boot metadata support for IDE auto-completion is provided via `spring-configuration-metadata.json`.

## Future Enhancements

* Redis-backed storage with TTL
* Spring Boot Admin integration

## Contribution

Pull requests and feedback are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## Maintainer

**Spring Tx Board** — Built and maintained by `Abdulla-Al-Mamun` / SDLC.PRO
