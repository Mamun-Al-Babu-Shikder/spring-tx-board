# Contributing to Spring Tx Board

First off, thank you for considering contributing to **Spring Tx Board**! 🎉

Your contributions will help improve transaction monitoring for Spring-based applications and make this tool more
powerful for the community.

---

## Getting Started

1. **Fork the repository**
   Click the **Fork** button at the top right of the repo page to create your own copy.

2. **Clone your fork**

   ```bash
   git clone https://github.com/<your-username>/spring-tx-board.git
   cd spring-tx-board
   ```

3. **Set up upstream remote**

   ```bash
   git remote add upstream https://github.com/Mamun-Al-Babu-Shikder/spring-tx-board.git
   ```

4. **Create a new branch**

   ```bash
   git checkout -b feature/STB-{issue_number}
   ```

   or

   ```bash
   git checkout -b feature/your-feature-name
   ```

---

## Local Setup

### Prerequisites

* **Java 17+**
* **Maven 3.8+**
* Optional: Docker (if testing Redis storage mode)

### Build & Test

```bash
mvn clean install
```

Run tests:

```bash
mvn test
```

---

## Coding Standards

* Follow **Java Code Conventions**.
* Use **4 spaces** for indentation.
* All public classes/methods have Javadoc.
* Keep class responsibilities clear and methods small.
* Use meaningful variable and method names.

---

## Branching Strategy

* `master` → stable, released code.
* `dev` → active development branch.
* The branches should be named:

    * `feature/STB-{issue_number}` or `feature/<short-description>`
    * `bugfix/STB-{issue_number}` or `bugfix/<short-description>`
    * `docs/STB-{issue_number}` or `docs/<short-description>`

---

## Commit Messages

* Use [Conventional Commits](https://www.conventionalcommits.org/) style:

  ```
  feat: add JDBC connection lease time tracking
  fix: handle REQUIRED_NEW propagation correctly
  docs: update README with JitPack installation
  ```

---

## Pull Request Process

1. **Update your branch**

   ```bash
   git fetch upstream
   git merge upstream/dev
   ```
2. **Ensure all tests pass**.
3. **Write/update documentation** if needed.
4. **Submit PR to `dev` branch**.
5. **Describe changes clearly** in PR description.

---

## Areas You Can Contribute

* Transaction lifecycle enhancements (nested transactions, propagation tracking)
* JDBC connection lifecycle tracking
* SQL query capture & analysis
* Web UI improvements (timeline view, better UX)
* Documentation updates

---

## Getting Help

If you have any questions, open an [issue](https://github.com/Mamun-Al-Babu-Shikder/spring-tx-board/issues) or discuss with us.

