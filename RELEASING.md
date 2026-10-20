# Releasing

Flashback's custom license states that redistribution is not permitted. Only publish this repository and its releases when you have the necessary authorization from the copyright holder. The procedure below creates a public GitHub repository and release.

## Prerequisites

- Java 17 is installed and `JAVA_HOME` points to it.
- GitHub CLI is installed and `gh auth status` succeeds.
- Minecraft is closed while replacing a locally installed JAR.

## Verify and build

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --no-daemon

$jar = "build\libs\flashback-1.0.1.jar"
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $jar).Hash.ToLowerInvariant()
"$hash  flashback-1.0.1.jar" | Set-Content -Encoding ascii "$jar.sha256"
```

## Commit the port

Review `git status` and `git diff` before staging. The port includes tracked deletions and previously untracked Forge files, so `git add -A` is intentional.

```powershell
git status --short
git diff --check
git add -A
git commit -m "Release Flashback Forge 1.20.1 v1.0.1"
```

## Create the GitHub repository

The inherited `origin` remote points to another Flashback repository. Preserve it as `upstream`, then create a new private repository under the authenticated GitHub account.

```powershell
git remote rename origin upstream
gh repo create tatuto-riku/Flashback-Forge-1.20.1 --public --source=. --remote=origin
git push -u origin master
```

## Create the release

```powershell
git tag -a v1.0.1 -m "Flashback Forge 1.20.1 v1.0.1"
git push origin v1.0.1

gh release create v1.0.1 `
  build/libs/flashback-1.0.1.jar `
  build/libs/flashback-1.0.1.jar.sha256 `
  --repo tatuto-riku/Flashback-Forge-1.20.1 `
  --title "Flashback Forge 1.20.1 - 1.0.1" `
  --notes-file CHANGELOG.md `
  --verify-tag
```

The `build/libs/flashback-1.0.1-slim.jar` file is not a distributable mod and must not be attached to the release.
