# Nano Java build. Meant to run inside the build container (see
# docker/Dockerfile and build.sh / build.ps1), which provides gcc, JDK 8
# and devkitARM.
#
#   make classlib   build/classlib.jar
#   make host       build/host/nanojava (desktop build, used by tests)
#   make test       run the test suite on the host build
#   make nds        build/nanojava.nds
#   make clean

BUILD       := build
HOST_CC     ?= gcc
HOST_CFLAGS := -std=c99 -O2 -g -Wall -Wextra -Wno-unused-parameter \
               -Wno-missing-field-initializers -D_POSIX_C_SOURCE=200809L
HOST_LIBS   := -lm

CORE_SRC := $(wildcard src/vm/*.c src/util/*.c src/midp/*.c src/audio/*.c)
HOST_SRC := $(CORE_SRC) $(wildcard platform/host/*.c)
HOST_OBJ := $(patsubst %.c,$(BUILD)/host/obj/%.o,$(HOST_SRC))
HEADERS  := $(wildcard src/*/*.h platform/host/*.h)

JAVA_SRC := $(shell find classlib/src -name '*.java')
JAVAC    := javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.3 -target 1.3

.PHONY: all classlib host test nds nds-embed clean

all: classlib host nds

classlib: $(BUILD)/classlib.jar

$(BUILD)/classlib.jar: $(JAVA_SRC)
	@rm -rf $(BUILD)/classlib && mkdir -p $(BUILD)/classlib
	@echo "javac classlib ($(words $(JAVA_SRC)) files)"
	@$(JAVAC) -bootclasspath $(BUILD)/classlib -extdirs "" -d $(BUILD)/classlib $(JAVA_SRC)
	@cd $(BUILD)/classlib && jar cfM ../classlib.jar .

host: $(BUILD)/host/nanojava

$(BUILD)/host/nanojava: $(HOST_OBJ)
	@echo "link $@"
	@$(HOST_CC) -o $@ $(HOST_OBJ) $(HOST_LIBS)

$(BUILD)/host/obj/%.o: %.c $(HEADERS)
	@mkdir -p $(dir $@)
	@echo "cc $<"
	@$(HOST_CC) $(HOST_CFLAGS) -c $< -o $@

test: classlib host
	@sh tests/run.sh

nds: classlib
	@mkdir -p $(BUILD)/nds-data
	@cp $(BUILD)/classlib.jar $(BUILD)/nds-data/classlib.bin
	@$(MAKE) --no-print-directory -C platform/nds/arm7
	@$(MAKE) --no-print-directory -C platform/nds
	@cp platform/nds/nanojava.nds $(BUILD)/nanojava.nds
	@echo "built $(BUILD)/nanojava.nds"

# A ROM that boots straight into one MIDlet, for emulator tests:
#   make nds-embed JAR=build/midlets/demo.jar [SCREEN=176x208]
SCREEN ?= 240x320
nds-embed: classlib
	@test -n "$(JAR)" || (echo "usage: make nds-embed JAR=path/to/game.jar" && false)
	@mkdir -p $(BUILD)/nds-embed-data
	@cp $(BUILD)/classlib.jar $(BUILD)/nds-embed-data/classlib.bin
	@cp $(JAR) $(BUILD)/nds-embed-data/app.bin
	@rm -rf platform/nds/build-embed
	@$(MAKE) --no-print-directory -C platform/nds/arm7
	@$(MAKE) --no-print-directory -C platform/nds BUILD=build-embed TARGET=nanojava-embed \
	    DATA=../../build/nds-embed-data \
	    EXTRA_CFLAGS="-DNJ_EMBED_W=$(word 1,$(subst x, ,$(SCREEN))) -DNJ_EMBED_H=$(word 2,$(subst x, ,$(SCREEN))) -DNJ_SHOW_FPS"
	@cp platform/nds/nanojava-embed.nds $(BUILD)/nanojava-embed.nds
	@echo "built $(BUILD)/nanojava-embed.nds"

clean:
	rm -rf $(BUILD)
	@$(MAKE) --no-print-directory -C platform/nds clean
