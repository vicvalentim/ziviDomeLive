# NDI Runtime

!!! warning "Experimental and unofficial integration"
    NDI output in ziviDomeLive 2.0.0 is an experimental, community-maintained
    video sender. It is not an official Processing or NDI integration and is
    not affiliated with or endorsed by Vizrt NDI AB. There is no official NDI
    library supplied by Processing: NDI support is therefore not installed from
    Processing's Contribution Manager.

    Version 2.0.0 sends video only. It does not provide NDI audio, reception,
    tally, PTZ, or a discovery user interface. Qualify the exact sender,
    receiver, network, operating system, and frame format before production use.

NDI® is a registered trademark of Vizrt NDI AB.

## How The Integration Is Packaged

ziviDomeLive uses the community-maintained Devolay fork as a Java/JNI binding:

```text
ziviDomeLive
    -> Devolay Java API
    -> packaged Devolay JNI library
    -> separately installed NDI Runtime
    -> NDI network
```

The public `io.github.vicvalentim:devolay:2.2.0-vic.1` artifact is a
**separated build**. It includes Devolay classes and desktop JNI binaries, but
does not include the proprietary `Processing.NDI.Lib.x64.dll`, `libndi.dylib`,
or `libndi.so.6`. ziviDomeLive intentionally does not use Devolay's integrated
packaging mode. An NDI Runtime must be installed independently on every machine
that sends with ziviDomeLive.

See the [Devolay runtime model](https://github.com/vicvalentim/devolay#ndi-runtime),
[official NDI Tools](https://ndi.video/tools/), and
[official NDI SDK](https://ndi.video/for-developers/ndi-sdk/). Always use the
current official NDI download and review its current license; do not obtain
runtime binaries from the ziviDomeLive or Devolay JAR.

## Windows

Processing 4 normally runs as a 64-bit application, so use a current 64-bit NDI
6 Runtime.

1. Close Processing and any application currently using NDI.
2. Download and install the current [NDI Tools for Windows](https://ndi.video/tools/),
   or the current standalone NDI Runtime provided by the official NDI
   distribution.
3. Restart Processing. Reboot Windows if the installer requests it or if an old
   Processing process retained the previous environment.
4. Enable NDI in ziviDomeLive and verify the output with NDI Studio Monitor or
   another compatible receiver.

Devolay loads `Processing.NDI.Lib.x64.dll` from the directory identified by
`NDI_RUNTIME_DIR_V6`. The official runtime installer normally configures this.
For a custom SDK/runtime installation, define that environment variable at the
user or system level **before** starting Processing; it must name the directory
that directly contains the DLL.

## macOS

The maintained Devolay fork contains JNI binaries for Intel x86-64 and Apple
Silicon aarch64, but the NDI Runtime remains a separate installation.

1. Close Processing and NDI applications.
2. Install the current [NDI Tools for macOS](https://ndi.video/tools/), or the
   current standalone NDI Runtime from the official NDI distribution.
3. Restart Processing and allow local-network access if macOS asks for it.
4. Verify the sender with NDI Video Monitor or another compatible receiver.

Devolay checks the common runtime location `/usr/local/lib/libndi.dylib`. For a
full NDI SDK installation, it also checks:

```text
/Library/NDI SDK for Apple/lib/macOS/libndi.dylib
```

For a nonstandard installation, set `NDI_RUNTIME_DIR_V6` before Processing
starts. The value must be the directory containing `libndi.dylib`, not the file
itself.

## Linux

NDI on Linux is experimental and not part of the qualified 2.0.0 output matrix.
The public NDI Tools desktop bundle is provided for Windows and macOS; Linux
users should obtain the current NDI SDK/runtime from the
[official NDI SDK page](https://ndi.video/for-developers/ndi-sdk/) and accept
its license during installation.

1. Download the current NDI SDK for Linux from the official source.
2. Run the installer supplied in the archive and locate the architecture
   directory containing `libndi.so.6`.
3. Before launching Processing from that terminal, point Devolay to the runtime:

```bash
export NDI_RUNTIME_DIR_V6="/absolute/path/to/the/directory-containing-libndi.so.6"
```

4. Start Processing from the same environment and verify the sender with a
   receiver on the network.

Devolay also checks `/usr/local/lib/libndi.so.6` and `/usr/lib/libndi.so.6`.
When installing the runtime system-wide, follow the NDI SDK instructions and
license, then refresh the dynamic linker cache where the distribution requires
it. Do not copy NDI runtime binaries into this repository or the ziviDomeLive
package.

## Verify From A Sketch

NDI initialization is lazy and begins only when publication is enabled:

```java
OutputManager outputs = ziviDome.getOutputManager();
outputs.toggleOutput("ndi");

println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
```

Expected state after successful initialization is `ENABLED`. Common failures:

| Diagnostic | Meaning | Action |
|---|---|---|
| `NDI Runtime libraries were not found` | No runtime was found in `NDI_RUNTIME_DIR_V6` or a supported system location | Install the runtime or correct the directory before starting Processing |
| `NDI Runtime libraries failed to load` | A runtime file was found but could not be loaded | Reinstall a matching current 64-bit runtime and check architecture/permissions |
| Sender is enabled but not visible | Runtime loaded; discovery, firewall, subnet, or receiver may be blocking visibility | Test on one local subnet, allow Processing/Java through the firewall, and verify with official NDI monitor tools |

The automated suite checks routing, RGBA conversion, progressive metadata,
backpressure, and shutdown without opening a real NDI session. Successful
production use still requires the [hardware qualification protocol](../qualification/2.0-release-readiness.md).

## Licensing Boundary

Devolay is Apache-2.0 software; ziviDomeLive is GPL-2.0-only. The proprietary NDI
Runtime is covered separately by the current NDI SDK license and distribution
terms. Installing or redistributing the runtime does not make it part of either
open-source license. Anyone distributing a product with NDI runtime binaries is
responsible for reviewing the current NDI license, distribution, identification,
and trademark requirements.
