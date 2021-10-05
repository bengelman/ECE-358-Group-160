JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Lab1.java

default: all

all: classes run

classes: $(CLASSES:.java=.class)

run: Lab1.class
	java Lab1

clean:
	$(RM) *.class