# FilesCrypt Pro

File and folder encryption for Android. Published on
[Google Play](https://play.google.com/store/apps/details?id=com.cdevworks.filescryptpro).

Encrypts files, whole directory trees, and optionally the file and folder names themselves. Built
around a streaming pipeline so a 4 GB video costs the same memory as a 4 KB text file.

Version 3.3 · minSdk 26 · targetSdk 33

## How the engine works

`CryptorEngine` splits each file across three threads that hand buffers to one another:

```
fileReader  ──▶  byteCrypter  ──▶  fileWriter
   NIO read       AES block         channel write
             (ConcurrentLinkedQueue between stages)
```

Buffers come from a shared pool rather than being allocated per chunk, and are returned to it after
writing. A stage that runs dry waits on the queue instead of the disk, so read, cipher and write
overlap — the file is never held in memory, and throughput is bounded by the slowest stage rather
than the sum of all three. The UI reports live throughput in MB/s off the back of this.

Multiple engines run in parallel over a shared work queue. Folder trees are walked first to build one
work item per file; folder name encryption is deferred until every file inside is finished, so paths
never shift under an in-flight operation.

Long jobs run in `CryptorEngineService`, a foreground service with its own notification channel, so
they survive the app being backgrounded.

## Crypto

- **Cipher** — AES-256, CBC, PKCS#7
- **Key derivation** — PBKDF2 with HMAC-SHA256, 6300 iterations
- **Integrity** — SHA-256 over file content, checked on decrypt
- **IV** — random per file from `SecureRandom.getInstanceStrong()`, or supply your own

The IV field in the UI is optional; left blank, every file gets its own random IV.

Encrypted files begin with a plaintext header recording the app version and algorithm, so a file can
still be identified years later:

```
<ENCRYPTION PROPERTIES>
This file is encrypted with FilesCrypt Pro V...
{Algorithm=AES/CBC/PKCS7Padding, ...}
</ENCRYPTION PROPERTIES>
```

## Password manager

Passwords can be stored instead of typed. Keys live in a BKS keystore that is itself sealed with an
AES master key held in the `AndroidKeyStore` under the alias `filescryptmaster` — so the master key
never leaves hardware-backed storage, and the file keystore is useless on its own. Encrypted items
carry a `PMHASH_` marker naming the alias needed to open them, which is how the app decrypts a folder
whose name it cannot yet read.

Opening the manager requires a device screen lock and passes through `BiometricPrompt`, accepting a
fingerprint or the device credential. The vault can be exported for backup, and the export itself can
be password-protected.

## Features

- Encrypt or decrypt files, folders, or a mixed selection across different directories
- Optional filename and folder-name encryption, toggled independently
- Choose an output directory or write back alongside the source
- Optionally delete sources after a successful pass — on encrypt, decrypt, both, or never
- Live progress: files done, percent complete, MB processed, throughput in MB/s
- Per-file operation log with failure reasons and a retry action
- Built-in file browser with multi-select spanning directories

## Building it

Standard Android Studio project:

```
./gradlew assembleDebug
```

Release builds run R8 with `proguard-rules.pro`. No signing config is checked in, so a release build
needs your own keystore.

Play Billing, AdMob, Play Integrity and in-app updates are wired in. The AdMob application ID in the
manifest is Google's public test ID, so a fresh clone shows test ads rather than live ones.

## Layout

```
app/src/main/java/com/cdevworks/filescryptpro/
  CryptorEngine.java          pipeline, cipher, folder walking      (1340 lines)
  MainActivity.java           UI, options, progress, biometrics     (1223 lines)
  RecyclerAdapter.java        selection list
  PasswordManagerActivity.java / PasswordManager.java
  CSFileChooser.java          file browser
  CryptorEngineService.java   foreground service
  logViewer.java              operation log
app/src/main/res/             layouts, drawables, settings
app/src/main/assets/          in-app user guide
```
