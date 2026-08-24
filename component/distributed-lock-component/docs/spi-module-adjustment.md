# SPI Module UML Adjustment

After extracting distributed-lock-spi, UML rules:

- API: business-facing contracts
- SPI: extension contracts (LockProvider/FencingTokenProvider)
- Core: orchestration implementation
- Provider: concrete infrastructure implementation

All diagrams should represent SPI as an independent module boundary.
