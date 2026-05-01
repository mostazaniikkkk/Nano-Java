# Nano Java — J2ME MIDP 2.0 para Nintendo DS

Nano Java es un proyecto de reconstrucción y mantenimiento de **Pstros**, un runtime J2ME MIDP 2.0 para Nintendo DS.
El objetivo es ejecutar MIDlets J2ME (aplicaciones Java ME para móviles) de forma nativa en hardware NDS usando la K Virtual Machine (KVM).

---

## ¿De qué trata esto?

El Nintendo DS corre un procesador ARM9 con 4 MB de RAM, sin sistema operativo y sin soporte Java de ningún tipo.
Nano Java cierra esa brecha combinando:

- **KVM** — K Virtual Machine, una JVM CLDC 1.0/1.1 desarrollada originalmente por Sun para dispositivos embebidos
- **Capa de API MIDP 2.0** — una implementación nativa del Mobile Information Device Profile de J2ME, escrita en C y Java
- **Backend nativo para NDS** — código específico del hardware para video (framebuffer BGR555), entrada, audio y sistema de archivos vía libNDS

El proyecto apunta a hardware real (cartucho R4) y emuladores (DeSmuME, melonDS).

---

## El problema con KVM

**El código fuente de KVM no puede incluirse en este repositorio.**

KVM fue publicada originalmente por Sun Microsystems bajo una licencia que prohíbe la redistribución de su código fuente.
La implementación de referencia del CLDC (que incluye KVM) debe obtenerse de forma independiente.

Para compilar Nano Java necesitás conseguir KVM por tu cuenta desde:
- El tarball original de Sun/Oracle CLDC 1.1 (buscá `j2me_cldc` o `cldc-1.1`)
- Archive.org u otros recursos históricos del ecosistema Java ME

Una vez obtenido, el árbol de fuentes de KVM debe colocarse en `kvm/` respetando la estructura de directorios que espera el sistema de build.
Los directorios `kvm/VmSkel/` y `kvm/VmCommon/` en este repo contienen únicamente los archivos específicos de NDS que se apoyan sobre KVM.

---

## Estado del proyecto

> **Fase de pruebas muy inicial — no apto para producción.**

Tenemos un build Release funcional. El ROM bootea, carga clases desde el sistema de archivos FAT virtual y ejecuta MIDlets.

### Pruebas confirmadas

| Prueba | Estado | Qué ejercita |
|--------|--------|--------------|
| `VideoTest` | PASA | Blit de tiles, blit de sprites, `drawLine`, `fillRect`, `drawString`, gradiente con `drawRGB`, animación de pelota rebotando |

### Limitaciones conocidas / aún no probado

- Cargar `.jar` **no se recomienda** en esta etapa — el desempaquetado de JARs y la resolución del classpath desde archivos comprimidos es inestable.
  Usá archivos `.class` sueltos en el sistema de archivos FAT en su lugar.
- Audio (`Sound`, `Player`) — no probado aún
- Entrada (`GameCanvas`, eventos de teclado) — no probado aún
- Red — no implementado
- `SpriteTest`, `MoveMe` y las pruebas de entrada completas están pendientes

---

## Compilación

Requisitos:
- Docker (contenedor de build: `pstros20-build` basado en el toolchain BlocksDS/Wonderful)
- JDK 8 para compilar la capa Java del API

```sh
# Compilar el ROM de NDS
docker run --rm -v "$(pwd):/proj" -w /proj/kvm/VmSkel/build pstros20-build make
```

La salida es `kvm/VmSkel/build/nanojava.nds`.

Las clases y recursos van en `kvm/VmSkel/build/FAT/` — esta es la raíz de la tarjeta SD virtual.

---

## Estructura del repositorio

```
api/          Fuentes del API Java (clases MIDP 2.0, stubs nativos)
kvm/          Árbol de KVM (solo se incluyen los archivos específicos de NDS; el núcleo de KVM no es redistribuible)
  VmSkel/     Capa de port para NDS, implementaciones nativas en C, sistema de build
  VmCommon/   Headers de KVM usados por la capa nativa
```

---

## Historia

Este proyecto es la continuación del **Pstros** original de ole,
que implementó MIDP en NDS usando KVM con trabajo de porteo para GBA/NDS por Torlus y davr.
Nano Java retoma desde donde quedó, arreglando el decodificador PNG, modernizando el toolchain de build
y trabajando hacia un runtime de MIDlets estable.
