JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Lab2.java

default: all

all: classes run

classes: $(CLASSES:.java=.class)

run: Lab2.class
	java Lab2

clean:
	$(RM) *.class