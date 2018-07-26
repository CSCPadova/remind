
CPP := clang++
INCLUDE := -I.
CPPFLAGS := -B/usr/lib/gold-ld $(INCLUDE) --std=c++11 -MMD -MP -O2 -Wall
BINDIR := bin

LIBRARIES := `sdl2-config --libs`

SOURCES := $(notdir $(shell find . -name '*.cpp'))

.PHONY: clean all mkdirs force_check libs

vpath %.o bin
vpath %.cpp $(shell find . -type d)
vpath %.h $(shell find . -type d)

#################################

all: main

clean:
	@rm -f bin/*
	@echo "Done."

mkdirs:
	@mkdir -p $(BINDIR)

main: mkdirs $(SOURCES:%.cpp=bin/%.o)
	@echo "### LINKING...";
	$(CPP) $(CPPFLAGS) -o main bin/*.o $(LIBRARIES);
	@echo "### DONE!"

bin/%.o: %.cpp
	@echo "### CPP $<  ($?)";
	$(CPP) $(CPPFLAGS) -c $< -o $@

-include $(SOURCES:%.cpp=bin/%.d)
