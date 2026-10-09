#---------------------------------------------------------------------------------
# Nano Java's ARM7 program (platform/nds/arm7/source plus the synthesizer
# from src/audio). Built by the top-level `make nds` before the ARM9 side.
#---------------------------------------------------------------------------------
.SUFFIXES:

ifeq ($(strip $(DEVKITARM)),)
$(error "Please set DEVKITARM in your environment. export DEVKITARM=<path to>devkitARM")
endif

include $(DEVKITARM)/ds_rules

TARGET   := nanojava7
BUILD    := build
SOURCES  := source ../../../src/audio
# Only these files run on the ARM7 (src/audio also has ARM9-side code).
CFILES   := main.c synth.c

ARCH     := -march=armv4t -mtune=arm7tdmi -mthumb
CFLAGS   := -g -Wall -Os -std=gnu11 -ffunction-sections -fdata-sections \
            $(ARCH) $(INCLUDE) -DARM7
ASFLAGS  := -g $(ARCH)
LDFLAGS   = -specs=ds_arm7.specs -g $(ARCH) -Wl,--gc-sections -Wl,-Map,$(notdir $*).map
LIBS     := -lnds7
LIBDIRS  := $(LIBNDS)

ifneq ($(BUILD),$(notdir $(CURDIR)))

export ARM7ELF := $(CURDIR)/$(TARGET).elf
export DEPSDIR := $(CURDIR)/$(BUILD)
export VPATH   := $(foreach dir,$(SOURCES),$(CURDIR)/$(dir))
export LD      := $(CC)
export OFILES  := $(CFILES:.c=.o)
export INCLUDE := $(foreach dir,$(LIBDIRS),-I$(dir)/include) -I$(CURDIR)/$(BUILD)
export LIBPATHS := $(foreach dir,$(LIBDIRS),-L$(dir)/lib)

.PHONY: $(BUILD) clean

$(BUILD):
	@mkdir -p $@
	@$(MAKE) --no-print-directory -C $(BUILD) -f $(CURDIR)/Makefile

clean:
	@rm -fr $(BUILD) $(TARGET).elf

else

$(ARM7ELF): $(OFILES)

-include $(DEPSDIR)/*.d

endif
