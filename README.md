# Paywall

Subscription check injected into a patched app by
[Stitch](../../PycharmProjects/ApkCrawler) as an `ExternalModule`, alongside
each patcher's own `smali_generator`.

At startup it posts the app token embedded at `assets/paywall.json` to
`/api/subscription/is_device_allowed` and kills the process if the server does
not answer 200.

## How it starts

Stitch 1.3.0 registers a `ContentProvider` in the target's manifest:

```xml
<provider android:name="com.paywall.InitProviderPaywall<App>"
          android:authorities="<target package>.com.paywall.InitProviderPaywall<App>"
          android:exported="false"
          android:initOrder="2147483647" />
```

`initOrder` is the highest possible value, so `onCreate()` runs before every
other provider and before the host `Application.onCreate()`. `PaywallProvider`
therefore catches everything: an exception escaping it would kill the host app
before a line of its own code had run.

Authorities have to be unique device-wide, and Stitch already scopes them to
the target's package, so one class name would in fact work across every patched
app. The class name is varied per app regardless, so that a logcat shared by
several patched apps says which one is talking.

## The provider class name

This module does not know its own entry point at development time. The patcher
supplies it, and the two have to agree:

```python
PAYWALL_PROVIDER = 'com.paywall.InitProviderPaywallMoovit'

extra_artifacts.setdefault('PAYWALL_PROVIDER_CLASS', PAYWALL_PROVIDER.rsplit('.', 1)[1])
external_modules.append(ExternalModule(Path(args.paywall), PAYWALL_PROVIDER))
```

Stitch byte-replaces `{{PAYWALL_PROVIDER_CLASS}}` across a copy of this project
— `gradle.properties` included — before running `gradlew assembleRelease`
(`stitch/patcher.py:prepare_smali`). The `generatePaywallProvider` task reads
that property and emits

```java
public final class InitProviderPaywallMoovit extends PaywallProvider {}
```

If the key is missing the placeholder survives into `gradle.properties` and the
**build fails** with a message naming the key. That is deliberate: the
alternative failure — the manifest naming a class that is not in the dex —
installs cleanly and dies at launch with `ClassNotFoundException`.

Deriving both values from one constant, as above, is what keeps them in sync.

## Building standalone

```bash
./gradlew assembleRelease -PpaywallProviderClass=InitProviderPaywallDev
```

Output lands at `./smali_generator.apk`, which is where Stitch looks
(`SMALI_GENERATOR_OUTPUT_PATH`).

## Manifest

Stitch merges this module's manifest into the target's, but only
`<application>` *children* and the root `uses-permission` / `uses-feature` /
`queries` tags — never `<application>` or `<manifest>` attributes.

- `<uses-permission android:name="android.permission.INTERNET"/>` is declared
  here so the paywall stops assuming the host already holds it.
- The provider is **not** declared here. Stitch adds it with the scoped
  authority; a second declaration would either be dropped by the merger or
  register an unscoped authority that collides across patched apps.
- `android:usesCleartextTraffic` would be ignored, so the debug
  `http://10.0.0.14:8000` branch depends on the *host* permitting cleartext.
