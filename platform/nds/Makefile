#---------------------------------------------------------------------------------
# Nano Java for Nintendo DS (devkitARM + libnds/calico). Built from the top
# level with `make nds`, which first copies build/classlib.jar to
# build/nds-data/classlib.bin so it is linked into the ROM.
#---------------------------------------------------------------------------------
.SUFFIXES:

ifeq ($(strip $(DEVKITARM)),)
$(error "Please set DEVKITARM in your environment. export DEVKITARM=<path to>devkitARM")
endif

GAME_TITLE     := Nano Java
GAME_SUBTITLE1 := J2ME runtime
GAME_SUBTITLE2 := CLDC 1.1 / MIDP 2.0

include $(DEVKITARM)/ds_rules

# Our own ARM7 program (platform/nds/arm7) replaces calico's default one.
ARM7_ELF   ?= $(CURDIR)/arm7/nanojava7.elf
export ARM7_ELF
_ARM7_ELF  := -7 $(ARM7_ELF)

TARGET   := nanojava
BUILD    := build
SOURCES  := source ../../src/vm ../../src/util ../../src/midp ../../src/audio
INCLUDES := source
DATA     := ../../build/nds-data
ICON     :=

ARCH := -march=armv5te -mtune=arm946e-s

CFLAGS   := -g -Wall -O2 -std=gnu99 -ffunction-sections -fdata-sections \
            -Wno-unused-parameter $(ARCH) $(INCLUDE) -DARM9 -DNJ_NDS $(EXTRA_CFLAGS)
ASFLAGS  := -g $(ARCH)
LDFLAGS   = -specs=ds_arm9.specs -g $(ARCH) -Wl,--gc-sections -Wl,-Map,$(notdir $*.map)

LIBS    := -lfat -lnds9 -lm
LIBDIRS := $(LIBNDS) $(PORTLIBS)

ifneq ($(BUILD),$(notdir $(CURDIR)))

export OUTPUT := $(CURDIR)/$(TARGET)
export VPATH  := $(foreach dir,$(SOURCES),$(CURDIR)/$(dir)) \
                 $(foreach dir,$(DATA),$(CURDIR)/$(dir))
export DEPSDIR := $(CURDIR)/$(BUILD)

CFILES   := $(foreach dir,$(SOURCES),$(notdir $(wildcard $(dir)/*.c)))
BINFILES := $(foreach dir,$(DATA),$(notdir $(wildcard $(dir)/*.bin)))

export LD := $(CC)
export OFILES_BIN     := $(addsuffix .o,$(BINFILES))
export OFILES_SOURCES := $(CFILES:.c=.o)
export OFILES         := $(OFILES_BIN) $(OFILES_SOURCES)
export HFILES         := $(addsuffix .h,$(subst .,_,$(BINFILES)))

export INCLUDE  := $(foreach dir,$(INCLUDES),-iquote $(CURDIR)/$(dir)) \
                   $(foreach dir,$(LIBDIRS),-I$(dir)/include) \
                   -I$(CURDIR)/$(BUILD)
export LIBPATHS := $(foreach dir,$(LIBDIRS),-L$(dir)/lib)

.PHONY: $(BUILD) clean

$(BUILD):
	@mkdir -p $@
	@$(MAKE) --no-print-directory -C $(BUILD) -f $(CURDIR)/Makefile

clean:
	@rm -fr $(BUILD) $(TARGET).elf $(TARGET).nds
	@$(MAKE) --no-print-directory -C arm7 clean

else

$(OUTPUT).nds: $(OUTPUT).elf $(ARM7_ELF)
$(OUTPUT).elf: $(OFILES)

$(OFILES_SOURCES): $(HFILES)

%.bin.o %_bin.h: %.bin
	@echo $(notdir $<)
	@$(bin2o)

-include $(DEPSDIR)/*.d

endif
