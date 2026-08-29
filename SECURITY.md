# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| `dev` (unreleased) | :white_check_mark: |
| `main` (latest release) | :white_check_mark: |
| `< 0.1.0` | :x: |

We fix security issues on `dev` and backport to the latest `main` release when feasible.

## Reporting a Vulnerability

**Do not open a public issue.**

- **Discord (preferred):** [https://discord.gg/yJk5qdR7wn](https://discord.gg/yJk5qdR7wn) — DM a maintainer or use the `#security` channel
- **GitHub:** Use **Security → Report a vulnerability** (private advisory) on https://github.com/bejiihiu/Candyriya/security/advisories/new
- **Email:** if you have a maintainer's contact, you may email directly

We aim to acknowledge within 48h and triage within 5 days.

Please include:

- Description and impact
- Steps to reproduce / PoC (if safe)
- Affected version / commit
- Suggested fix if you have one

## What we consider a vulnerability

- Proxy bypass, auth bypass, forwarding spoofing (`MODERN`/`BUNGEEGUARD`/`LEGACY`)
- RCE, SSRF, path traversal, deserialization, protocol parsing leading to crash/exploit
- DoS via crafted packets that bypass `protocol.maxPacketSize` / compression limits
- Privilege escalation via permission system

Out of scope: social engineering, physical access, already-public CVEs in dependencies (report via Dependabot instead).

## Disclosure

We follow coordinated disclosure:

1. You report privately
2. We confirm and fix on a private branch
3. We publish a GitHub Security Advisory + CVE if needed, crediting you (unless you prefer anonymity)
4. Fix is released and announced on Discord + Releases

Thank you for making Candyriya safer.
