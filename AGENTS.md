Java 21
Spring Boot 3.5
Thymeleaf
PostgreSQL
Constructor injection only
No hardcoded secrets
Run mvn clean test after changes
Do not rewrite the whole app

Do not scan unrelated files.
Do not rewrite templates unless needed.
Do not refactor working code.
Stop after this phase.
Ask before starting next phase.

Stop when:
- mvn clean test passes
- application starts
- no secrets remain in config
- README is updated