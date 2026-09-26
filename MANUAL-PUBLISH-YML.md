# Manual change for `.github/workflows/publish.yml` — publish the Maven cells

NeroTech 0.4.0 adds `maven-publish` to `neoforge/build.gradle`, `forge/build.gradle` and
`fabric/build.gradle`, publishing each cell as
`za.co.neroland.nerotech:nerotech-<loader>-<mc>:<mod_version>` to Maven Local and to GitHub
Packages (`https://maven.pkg.github.com/Neroland/nerotech`). That is how NeroPower (and any other
add-on) resolves NeroTech as a Gradle dependency on a fresh clone / CI runner, exactly as NeroTech
resolves Neroland Core today.

The workflow is not edited by the automated change (`.github/` is maintainer-owned). Two edits are
needed in `.github/workflows/publish.yml`, both in the **`build`** job; they mirror Neroland Core's
`publish.yml` (its `build` job has `packages: write` and a "Publish Maven artifacts to GitHub
Packages" step right after "Build all cells").

## 1. Grant the `build` job `packages: write`

The job currently has `packages: read` (to resolve Core). Publishing NeroTech's own packages needs
`write`, which includes read, so replace the line:

```yaml
  build:
    name: Build all cells
    needs: detect
    if: needs.detect.outputs.changed == 'true'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write  # resolve Neroland Core from GitHub Packages AND publish NeroTech's own Maven cells
```

## 2. Add the publish step directly after "Build all cells"

Insert this step after the `Build all cells` step and before `Determine release channel`:

```yaml
      # Publish the nine per-loader Maven artifacts to GitHub Packages so add-on Nero mods (NeroPower)
      # can resolve NeroTech from a remote repo (CI runners + fresh clones have an empty ~/.m2). This is
      # the ONLY consumable Maven for NeroTech; CurseForge/Modrinth/GitHub Releases carry player-facing
      # jars, not Gradle dependencies. The jars were just built above, so `publish`'s `jar` dependency is
      # UP-TO-DATE and this step only uploads — no second build. GITHUB_TOKEN is the workflow's own
      # token (packages: write granted above). Privacy (POPIA/GDPR): only build artifacts + version
      # strings are handled or logged.
      - name: Publish Maven artifacts to GitHub Packages
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: >-
          ./gradlew
          :fabric:26.1.2:publish :fabric:26.2:publish :fabric:26.3:publish
          :neoforge:26.1.2:publish :neoforge:26.2:publish :neoforge:26.3:publish
          :forge:26.1.2:publish :forge:26.2:publish :forge:26.3:publish
          --stacktrace
```

## Notes

- Credentials: the Gradle `GitHubPackages` repository reads `gpr.user` / `gpr.key` Gradle
  properties first, then the `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables — the same lazy
  `getOrNull()` pattern as Core, so a contributor build without a token still configures.
- Re-runs: GitHub Packages refuses to overwrite an existing version. The `detect` job already
  skips versions with a release tag; if a run failed *after* the Maven publish but before the tag
  was created, the re-run's publish step will fail on the duplicate — bump `mod_version` or delete
  the package version in the GitHub UI first.
- Local dev needs nothing from this file: `./gradlew publishToMavenLocal` installs all nine cells
  into `~/.m2`, and NeroPower's `mavenLocal()` resolves them.
- Package visibility: for NeroPower's CI to read the package with its own `GITHUB_TOKEN`, the
  `nerotech` package must be Internal (org-visible) or explicitly granted to the NeroPower repo —
  the same requirement `USING-CORE.md` describes for Core's package.
