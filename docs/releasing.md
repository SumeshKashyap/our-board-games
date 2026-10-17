# Releasing

Releases are driven by **git tags**. Pushing a tag like `v1.2.0` makes GitHub Actions test the code, build a
signed APK and publish it on the repository's **Releases** page.

## Versioning

Versions follow `MAJOR.MINOR.PATCH`:

- **PATCH** (`1.0.1`): fixes only.
- **MINOR** (`1.1.0`): new features, nothing removed.
- **MAJOR** (`2.0.0`): big changes.

The tag sets the app's version automatically: `versionName = 1.2.0` and `versionCode = 10200`
(`major×10000 + minor×100 + patch`). Android only installs an update over an existing app if the version code is
higher, so always increase the version.

## Making a release

1. Make sure `main` is committed, pushed, and the **CI** workflow is green.
2. Run the VS Code task **Publish release (push version tag)** and enter the version (e.g. `1.2.0`), or:
   ```bash
   git tag -a v1.2.0 -m "Our Board Games v1.2.0"
   git push origin v1.2.0
   ```
3. Watch the **Actions** tab on GitHub. When the *Release* workflow finishes, the release appears under
   **Releases** with `OurBoardGames-v1.2.0.apk` attached and auto-generated release notes.

To redo a failed release: delete the tag locally and on GitHub (`git tag -d v1.2.0 && git push origin :v1.2.0`),
fix the problem, and tag again.

## The family edition

Public release APKs do **not** contain the private family photos (they are never uploaded). To make the edition
with photos for family devices:

1. Run the VS Code task **Build release APK (family, with photos)** with the same version number.
   It writes `OurBoardGames-vX.Y.Z-family.apk` in the project folder (git-ignored).
2. Install it with **Install release APK on device**, or copy the APK to the device and open it.

Both editions are signed with the same key, so a device can update from one to the other.

## Signing key

Android requires every update to be signed with the **same key**. Losing it means family devices must uninstall
(and lose their progress) before installing a new version.

- The key lives at `~/.config/ourchess/ourchess-release.jks`, with its passwords in
  `~/.config/ourchess/keystore.properties`. A copy of that properties file sits in the project root as
  `keystore.properties` (git-ignored) so local release builds are signed.
- GitHub Actions gets the key from encrypted repository **secrets**:
  `OURCHESS_KEYSTORE_BASE64`, `OURCHESS_KEYSTORE_PASSWORD`, `OURCHESS_KEY_ALIAS`, `OURCHESS_KEY_PASSWORD`.
- **Back up** `~/.config/ourchess/` somewhere safe and private (e.g. an encrypted USB stick or password manager).
  Never commit it.

To set the secrets up again (e.g. on a new repository):
```bash
K=~/.config/ourchess
gh secret set OURCHESS_KEYSTORE_BASE64 < <(base64 -w0 $K/ourchess-release.jks)
gh secret set OURCHESS_KEYSTORE_PASSWORD --body "$(grep storePassword $K/keystore.properties | cut -d= -f2)"
gh secret set OURCHESS_KEY_ALIAS --body ourchess
gh secret set OURCHESS_KEY_PASSWORD --body "$(grep keyPassword $K/keystore.properties | cut -d= -f2)"
```

Without a release key (e.g. a fresh checkout), release builds fall back to the debug key so they still work, but they
cannot update an installed release build.

## Licence obligations

The app bundles Stockfish (GPL-3.0), so Our Board Games is GPL-3.0 as well. The repository is public and every release
links to its source, which satisfies the licence. The Stockfish licence text is also included inside the app
(`assets/licenses/stockfish-COPYING.txt`).
