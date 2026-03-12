# Publishing to Maven Central

This document explains how to publish the pgjson library to Maven Central via the [Sonatype Central Portal](https://central.sonatype.com) and how to publish SNAPSHOT builds from the develop branch.

## Prerequisites

1. **Sonatype Central Portal account** with verified namespace `io.github.deltatango` at [central.sonatype.com](https://central.sonatype.com)
2. **GPG key** for artifact signing (release builds only)
3. **SNAPSHOT publishing enabled** on the namespace (see [SNAPSHOT Publishing](#snapshot-publishing))
4. **GitHub repository secrets** configured for CI publishing (see [CI Configuration](#ci-configuration))

## Publishing Options

Sonatype does not provide an official Gradle plugin. The following options are available for publishing from a Gradle project:


| Method                       | Description                                                                                                                                                                                              | Used by this project |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------- |
| **OSSRH Staging API**        | Compatibility layer that works with the standard `maven-publish` plugin. Artifacts are staged and finalized via a REST call.                                                                             | Yes (release builds) |
| **Central Portal Snapshots** | Standard `mvn deploy`-compatible endpoint for `-SNAPSHOT` versions. No staging or validation.                                                                                                            | Yes (develop branch) |
| **Portal Publisher API**     | Newer REST API — upload a bundle zip to `https://central.sonatype.com/api/v1/publisher/upload`.                                                                                                          | No                   |
| **Portal UI Upload**         | Manual upload via browser at [central.sonatype.com/publishing](https://central.sonatype.com/publishing).                                                                                                 | No                   |
| **Community Gradle plugins** | [JReleaser](https://jreleaser.org/), [vanniktech/gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin), [GradleUp/nmcp](https://github.com/GradleUp/nmcp), and others. | No                   |


References: [Sonatype Gradle Publishing](https://central.sonatype.org/publish/publish-portal-gradle/), [OSSRH Staging API](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api), [Publisher API](https://central.sonatype.org/publish/publish-portal-api/)

## GPG Key Setup

Generate a GPG key pair (RSA 4096-bit recommended):

```bash
gpg --full-generate-key
```

List your keys to find the key ID:

```bash
gpg --list-keys --keyid-format short
```

The short key ID is the last 8 characters of the key fingerprint.

Export the ASCII-armored private key (needed for CI and in-memory signing):

```bash
gpg --list-keys --keyid-format short
```

Publish your public key to a keyserver so Maven Central can verify signatures:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

## Portal User Token Setup

Generate a user token for publishing credentials:

1. Log in to the [Central Portal User Tokens page](https://central.sonatype.com/usertoken)
2. Click **Generate User Token**
3. Set a display name and expiration date
4. Save the generated username and password immediately — **tokens cannot be retrieved once the modal closes**

These credentials are used as `ossrhUsername` / `ossrhPassword` in the Gradle configuration.

## Local Publishing

### Configure Credentials

Set properties in your **user-level** `~/.gradle/gradle.properties` (never commit credentials to the repository):

```properties
ossrhUsername=your-central-portal-token-username
ossrhPassword=your-central-portal-token-password
signingKey=<ASCII-armored GPG private key>
signingPassword=<GPG key passphrase>
```

### Test Locally

```bash
# Publish to local Maven repository for verification
./gradlew publishToMavenLocal

# Verify the output
ls ~/.m2/repository/io/github/deltatango/pgjson/
```

### Publish Release to Central Portal

```bash
./gradlew publish
```

This builds all artifacts (JAR, sources, javadoc), signs them with your GPG key, and uploads them to the OSSRH staging API. After upload, finalize the deployment either through the [Central Portal UI](https://central.sonatype.com/publishing) or via the staging API (the CI pipeline does this automatically).

### Publish SNAPSHOT Locally

Set the version in `build.gradle` to a `-SNAPSHOT` suffix (e.g., `26.4.0-SNAPSHOT`), then:

```bash
./gradlew publish
```

SNAPSHOT builds are published directly to `https://central.sonatype.com/repository/maven-snapshots/` without signing or staging.

## SNAPSHOT Publishing

SNAPSHOT builds allow consumers to test pre-release versions from the develop branch.

### Prerequisites (one-time setup)

Enable SNAPSHOT publishing on the namespace:

1. Go to [central.sonatype.com](https://central.sonatype.com) > **Namespaces**
2. Find `io.github.deltatango` > click the dropdown menu > **Enable SNAPSHOTs**
3. Confirm

### How it works

- The `develop` branch version in `build.gradle` uses the `-SNAPSHOT` suffix (e.g., `26.4.0-SNAPSHOT`)
- The build.gradle automatically selects the correct repository based on the version suffix:
  - `-SNAPSHOT` versions go to `https://central.sonatype.com/repository/maven-snapshots/`
  - Release versions go to the OSSRH staging API
- Signing is **not required** for SNAPSHOT builds
- No staging or validation — artifacts are available immediately
- SNAPSHOTs are cleaned up after **90 days** of inactivity

### Version management workflow

1. After a release tag, bump the version on `develop` to the next SNAPSHOT (e.g., `26.4.0-SNAPSHOT`)
2. Development continues on `develop` — each push triggers a SNAPSHOT publish via CI
3. When ready to release, remove the `-SNAPSHOT` suffix, merge, and tag

### Consuming SNAPSHOTs

Consumers can depend on SNAPSHOT builds by adding the snapshot repository to their `build.gradle`:

```gradle
repositories {
    maven {
        name = 'Central Portal Snapshots'
        url = 'https://central.sonatype.com/repository/maven-snapshots/'
        content {
            includeModule("io.github.deltatango", "pgjson")
        }
    }
    mavenCentral()
}

dependencies {
    implementation("io.github.deltatango:pgjson:26.4.0-SNAPSHOT")
}
```

## CI Configuration

Publishing is automated via [GitHub Actions](https://docs.github.com/en/actions/tutorials/publish-packages/publish-java-packages-with-maven). The workflows live in `.github/workflows/`:


| Workflow file          | Trigger                           | Purpose                                                                      |
| ---------------------- | --------------------------------- | ---------------------------------------------------------------------------- |
| `ci.yml`               | Push to any branch expect master, pull requests | Build, test, checkstyle, and upload test reports                             |
| `publish-snapshot.yml` | Push to `develop`                 | Build, test, and publish SNAPSHOT artifacts                                  |
| `publish-release.yml`  | Push of a `v*` tag                | Build, test, publish signed artifacts, and finalize release on Maven Central |


### GitHub Repository Secrets

Configure the following **secrets** in GitHub (Settings > Secrets and variables > Actions):


| Secret             | Description                                                                  |
| ------------------ | ---------------------------------------------------------------------------- |
| `OSSRH_USERNAME`   | Central Portal token username                                                |
| `OSSRH_PASSWORD`   | Central Portal token password                                                |
| `SIGNING_KEY`      | ASCII-armored GPG private key (output of `gpg --armor --export-secret-keys`) |
| `SIGNING_PASSWORD` | GPG key passphrase                                                           |


### Release Workflow

1. Ensure all changes are merged and tests pass on the target branch
2. Update `build.gradle` version to the release version (remove `-SNAPSHOT` suffix)
3. Tag the commit with a `v`-prefixed version matching `build.gradle`:

```bash
git tag v26.3.1
git push origin v26.3.1
```

1. The `publish-release.yml` workflow runs two jobs in sequence:
  - **Build and Test** — runs the full test suite (including Testcontainers integration tests)
  - **Publish to Maven Central** (runs only after Build and Test succeeds) —
  publishes signed artifacts (JAR, sources, javadoc) to the OSSRH staging API,
  then finalizes the deployment by calling the OSSRH staging API with `publishing_type=automatic`,
  which releases to Maven Central after validation
2. Monitor the deployment status at [central.sonatype.com/publishing](https://central.sonatype.com/publishing)
  Secrets used: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD` (see [GitHub: Using secrets in GitHub Actions](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions))

### SNAPSHOT Workflow

1. Push changes to the `develop` branch
2. The GitHub Actions workflow will automatically:
  - Build and run the full test suite
  - Publish unsigned artifacts to the Central Portal snapshot repository
3. No finalization step is needed — SNAPSHOTs are available immediately

## Artifacts Generated

The publishing process creates the following artifacts:

- `pgjson-{version}.jar` — main library JAR (includes LICENSE in META-INF)
- `pgjson-{version}-sources.jar` — source code for IDE lookup
- `pgjson-{version}-javadoc.jar` — API documentation
- `pgjson-{version}.pom` — Maven POM with dependency metadata

Release artifacts are signed with PGP signatures (`.asc` files). SNAPSHOT artifacts are not signed.

## Available Gradle Tasks


| Task                                               | Description                                                       |
| -------------------------------------------------- | ----------------------------------------------------------------- |
| `./gradlew publishToMavenLocal`                    | Publish to local Maven repository                                 |
| `./gradlew publish`                                | Publish to Central Portal (release or snapshot, based on version) |
| `./gradlew signMavenJavaPublication`               | Sign the publication (without uploading)                          |
| `./gradlew generatePomFileForMavenJavaPublication` | Generate POM file for inspection                                  |


## Versioning

This project uses [Calendar Versioning](https://calver.org/) with the format `YY.MM.INCREMENT`:

- `YY` — two-digit year (e.g., `26` for 2026)
- `MM` — month of the release (1-12, e.g., `3` for March)
- `INCREMENT` — incremental release number within the month

SNAPSHOT versions append `-SNAPSHOT` (e.g., `26.4.0-SNAPSHOT`).

## Troubleshooting

### Common Issues

1. **Signing errors** — Ensure your GPG key is properly configured. Test with `./gradlew signMavenJavaPublication`.
2. **Authentication errors (401)** — Verify you are using Central Portal User Tokens (generated at [central.sonatype.com/usertoken](https://central.sonatype.com/usertoken)), not legacy OSSRH tokens. Tokens expire — check the expiration date.
3. **Deployment not visible in portal** — The CI pipeline calls the OSSRH staging API finalization endpoint to release automatically. If publishing locally, finalize manually via the [Central Portal UI](https://central.sonatype.com/publishing).
4. **GPG key not found by verifiers** — Ensure your public key is uploaded to `keyserver.ubuntu.com` (or another public keyserver).
5. **SNAPSHOT publish fails** — Verify that SNAPSHOT publishing is enabled on the `io.github.deltatango` namespace (Central Portal > Namespaces > Enable SNAPSHOTs).

### Useful Commands

```bash
# Check your GPG keys
gpg --list-keys --keyid-format short

# Test signing
./gradlew signMavenJavaPublication

# Inspect generated POM
cat build/publications/mavenJava/pom-default.xml

# Verify local publication
ls ~/.m2/repository/io/github/deltatango/pgjson/
```

## Security Notes

- Never commit credentials to version control — use `~/.gradle/gradle.properties` or environment variables
- In CI, always use GitHub **encrypted** [repository secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions) (values are masked in logs)
- Enable [GitHub secret scanning](https://docs.github.com/en/code-security/secret-scanning) on the repository to detect accidentally committed credentials
- The CI workflow runs a [TruffleHog](https://github.com/trufflesecurity/trufflehog) secret scan on every push and pull request
- Keep your GPG private key secure and rotate credentials regularly
- Portal tokens have an expiration date — regenerate before they expire

## References

- [Sonatype Central Portal](https://central.sonatype.com)
- [Portal User Token Generation](https://central.sonatype.org/publish/generate-portal-token/)
- [Portal OSSRH Staging API](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api)
- [Portal Publisher API](https://central.sonatype.org/publish/publish-portal-api/)
- [Portal SNAPSHOT Publishing](https://central.sonatype.org/publish/publish-portal-snapshots/)
- [Gradle Publishing Options](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Gradle Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
- [GitHub: Publishing Java packages with Maven](https://docs.github.com/en/actions/tutorials/publish-packages/publish-java-packages-with-maven)
- [GitHub Actions: Using secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
- [GitHub: Secret scanning](https://docs.github.com/en/code-security/secret-scanning)

