# Nano Java — J2ME MIDP 2.0 for Nintendo DS

Nano Java is a reconstruction and maintenance project of **Pstros**, a J2ME MIDP 2.0 runtime for the Nintendo DS.
The goal is to run J2ME MIDlets (Java ME mobile applications) natively on NDS hardware using the K Virtual Machine (KVM).

---

## What is this?

The Nintendo DS runs an ARM9 processor with 4 MB of RAM, no OS, and no standard Java support.
Nano Java bridges that gap by combining:

- **KVM** — K Virtual Machine, a CLDC 1.0/1.1 JVM originally developed by Sun for embedded devices
- **MIDP 2.0 API layer** — a native implementation of the J2ME Mobile Information Device Profile written in C and Java
- **NDS native backend** — hardware-specific code for video (BGR555 framebuffer), input, audio, and filesystem via libNDS

The project targets real hardware (R4 flash cart) and emulators (DeSmuME, melonDS).

---

## The KVM problem

**The KVM source code cannot be included in this repository.**

KVM was originally released by Sun Microsystems under a license that forbids redistribution of its source code.
The CLDC reference implementation (which includes KVM) must be obtained independently.

To build Nano Java you need to source the KVM yourself from:
- The original Sun/Oracle CLDC 1.1 source tarball (search for `j2me_cldc` or `cldc-1.1`)
- Archive.org or other historical Java ME developer resources

Once obtained, the KVM source tree must be placed at `kvm/` following the directory structure expected by the build system.
The `kvm/VmSkel/` and `kvm/VmCommon/` directories in this repo contain only the NDS-specific files that sit on top of KVM.

---

## Project status

> **Early testing phase — not production ready.**

We have a working Release build. The ROM boots, loads classes from the virtual FAT filesystem, and executes MIDlets.

### Confirmed passing tests

| Test | Status | What it exercises |
|------|--------|-------------------|
| `VideoTest` | PASS | Tile blit, sprite blit, `drawLine`, `fillRect`, `drawString`, `drawRGB` gradient, bouncing ball animation |

### Known limitations / not yet tested

- `.jar` loading is **not recommended** at this stage — JAR unpacking and classpath resolution from compressed archives is unstable.
  Use flat `.class` files on the FAT filesystem instead.
- Audio (`Sound`, `Player`) — not yet tested
- Input (`GameCanvas`, key events) — not yet tested
- Networking — not implemented
- `SpriteTest`, `MoveMe`, and full input tests are pending

---

## Building

Requires:
- Docker (build container: `pstros20-build` based on BlocksDS/Wonderful toolchain)
- JDK 8 for compiling the Java API layer

```sh
# Build the NDS ROM
docker run --rm -v "$(pwd):/proj" -w /proj/kvm/VmSkel/build pstros20-build make
```

The output is `kvm/VmSkel/build/nanojava.nds`.

Classes and resources go into `kvm/VmSkel/build/FAT/` — this is the root of the virtual SD card.

---

## Repository structure

```
api/          Java API source (MIDP 2.0 classes, native stubs)
kvm/          KVM tree (only NDS-specific files included; KVM core not redistributable)
  VmSkel/     NDS port layer, native C implementations, build system
  VmCommon/   KVM headers used by the native layer
```

---

## History

This project is a continuation of the original **Pstros** project by ole,
which implemented MIDP on NDS using KVM with GBA/NDS porting work by Torlus and davr.
Nano Java picks up where that left off, fixing the PNG decoder, modernizing the build toolchain,
and working toward a stable MIDlet runtime.
