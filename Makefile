TARGET = *.java

all : 
	javac $(TARGET)
	
scanner :
	javac CScanner.java token.java type.java
parser : 
	javac CScanner.java CParser.java token.java type.java
translator : translator
	javac CTranslator.java CScanner.java CParser.java token.java type.java 
clean :
	rm -f *.class*
