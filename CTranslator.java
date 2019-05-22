
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
class SemanticError extends Exception
{
	public SemanticError(String message)
	{
		super(message);
		//("Enter error" + super.getMessage());
	}
}
class IdentifierContext
{
	public String type, location, taxonomy;
	public boolean isFunction;
	public String value = "nil";
	public boolean badID = false;
	public HashMap<String,IdentifierContext> table;
	public int numArgs = 0;
	public IdentifierContext(String t, boolean b, String l, String tax, String v, int n)
	{
		type = t;
		isFunction = b;
		location = l;
		taxonomy = tax;
		value = v;
		table = new HashMap<String, IdentifierContext> ();
		numArgs = n;
	}
	public IdentifierContext(String t, boolean b, String l, String tax, int n)
	{
		type = t;
		isFunction = b;
		location = l;
		taxonomy = tax;
		value = "nil";
		table = new HashMap<String, IdentifierContext> ();
		numArgs = n;
	}
	public IdentifierContext()
	{
		badID = true;
	}
	public String toString()
	{
		String message = "type: " + type + " function:" + Boolean.toString(isFunction) + " value: " + value + " location:" + location;
		if(table != null)
			message += " table: " + table.toString() + "table end \n";
		if(isFunction)
			message += " numArgs: " + numArgs + "end \n";
		message += "\n";
		return message;
	}
	public void setTable(HashMap<String, IdentifierContext> h)
	{
		
		Set<String> k = h.keySet();
		Iterator<String> it = k.iterator();
		table = new HashMap<String, IdentifierContext>();
		while(it.hasNext())
		{
			String key = it.next();
			IdentifierContext v = h.get(key).clone();
			table.put(key,v);
		}
		//System.out.print("TABLE: " + table.toString());
	}
	public IdentifierContext clone()
	{
		return new IdentifierContext(type, isFunction, location, taxonomy, value, numArgs);
	}
	public void clear()
	{
		value = "nil";
		//table = null;
	}
	
}
class CTranslator extends CParser
{
	HashMap<String, HashMap<String,IdentifierContext>> HashMapMap = new HashMap<String, HashMap<String,IdentifierContext>>();
	HashMap<String, IdentifierContext> hashmap = new HashMap<String, IdentifierContext>(); //key = name, value = type
	String Key;
	String self = "global";
	int linenum;
	IdentifierContext Value;
	SemanticError se;
	String scope = "global";
	String lastIdentifier = null;
	boolean mustbefunction;
	String mustbefunctionname;
	FileWriter translator;
	public boolean newLine = false;
	public boolean storage = false;
	public boolean writeEqual =true;
	public boolean writestruct = false;
	public String structtowrite;
	public boolean writecommaifconstant = false;
	public boolean suppressopenparenthesis = false;
	int tablevel = 0;
	public static void main(String args[])
	{
		CTranslator CT = new CTranslator(args[0]);
	}

	public CTranslator(String file)
	{
		super(file);
		se = null;
		Value = new IdentifierContext();
		hashmap.put("printc", new IdentifierContext("char", true, "interface", "Member Function", 1));
		hashmap.put("printi", new IdentifierContext("int", true, "interface", "Member Function", 1));
		hashmap.put("printd", new IdentifierContext("double", true, "interface", "Member Function", 1));
		hashmap.put("scanc", new IdentifierContext("char", true, "interface", "Member Function", 0));
		hashmap.put("scani", new IdentifierContext("int", true, "interface", "Member Function", 0));
		hashmap.put("scand", new IdentifierContext("double", true, "interface", "Member Function", 0));
		HashMap<String,IdentifierContext> cParam = new HashMap<String, IdentifierContext>();
		HashMap<String,IdentifierContext> iParam = new HashMap<String, IdentifierContext>();
		HashMap<String,IdentifierContext> dParam = new HashMap<String, IdentifierContext>();
		cParam.put("val", new IdentifierContext("char", false, "interface","parameter",1));
		iParam.put("val", new IdentifierContext("int", false, "interface","parameter",1));
		dParam.put("val", new IdentifierContext("double", false, "interface","parameter",1));
		hashmap.get("printc").setTable(cParam);
		hashmap.get("printi").setTable(iParam);
		hashmap.get("printd").setTable(dParam);
		HashMapMap.put("global", hashmap);
		super.scanner.openFile();
		String path = file.substring(0, file.length()-1)+"c";
		
		try
		{	
			if(writeEnabled)
				translator = new FileWriter(path);
				
			this.Program();
			if(writeEnabled) 
			{
				translator.write("\n");
				translator.close();
			}
		}
		catch (IOException e)
		{
			System.err.println("\n"+e.getMessage());
		}
	}

	public void debugTranslate(String message)
	{
		//(message);
	}
	public void writeLine(String line)
	{
		
		if(!dontwriteatall)
		{
			if(writeEnabled)
			{
				try{ 
				
					if(newLine)
					{
						for(int i = 0; i < tablevel; i++)
							translator.write("    ");
						newLine = false;
					}
					translator.write(line);
				}
				catch(IOException e)
				{
				
				} 
			}
			else
			{
				if(newLine)
				{
					for(int i = 0; i < tablevel; i++)
					{
						System.out.print("    ");
					}
					newLine = false;
				}
				System.out.print(line);
			}
		}
		
	}
	public boolean dontwriteatall = true;
	public boolean dontSwitch = true;
	public final boolean writeEnabled = true;
	public int commastoprint = 0;
	public void writeEverythingBeforeMain()
	{
		dontwriteatall = false;
		writeLine("#include <stdlib.h>\n");
		writeLine("#include <stdio.h>\n");
		writeLine("#define nil NULL\n\n");
		//(HashMapMap.toString());
		Set<String> interfaces = HashMapMap.keySet();
		Set<String> keys;
		Set<String> params;
		Iterator<String> interfaceiterator = interfaces.iterator();
		Iterator<String> selfiterator;
		Iterator<String> scopeiterator;
		IdentifierContext pfun;
		IdentifierContext para;
		String keyName;
		String structName;
		String param;
		HashMap<String, IdentifierContext> printingTable;
		while(interfaceiterator.hasNext())//for each interface
		{		
			structName = interfaceiterator.next();
			writeLine("struct "+ structName + ";\n");
			writeLine("struct "+ structName + " * "+structName+"_new(void);\n");
			selfiterator = HashMapMap.get(structName).keySet().iterator();
			while(selfiterator.hasNext()) //for each key
			{
				printingTable = HashMapMap.get(structName);
				keyName = selfiterator.next();
				pfun = printingTable.get(keyName);
				
				if(pfun.isFunction)
				{
					printingTable = printingTable.get(keyName).table;
					writeLine(pfun.type + " " + structName + "__" + keyName + "( struct " + structName + " * self");
					scopeiterator = pfun.table.keySet().iterator();
					while(scopeiterator.hasNext())
					{
						param = scopeiterator.next();
						para = pfun.table.get(param);
						writeLine(", " + para.type + " __"+param);
					}
					writeLine(");\n");
				}
				else
				{
					writeLine(pfun.type + " " + structName + "_g" + keyName + "( struct " + structName + " * self );\n");
					writeLine(pfun.type + " " + structName + "_s" + keyName + "( struct " + structName + " * self, "+ pfun.type +" __val);\n");
					
				}
				//break;
			}
				writeLine("\n");
			
		}
		writeLine("extern struct global global__global;\nstruct global *global = &global__global;\n");
		writeLine("struct global *self = &global__global;\n");
	}
	public void operatorSideEffect(String op)
	{
		if(op.equals("{"))
		{
			tablevel++;
			writeLine("\n");
			newLine = true;
		}
		if(op.equals("}"))
		{
			newLine = true;
			writeLine("\n\n");
		}
		if(op.equals(";"))
		{
			writeLine("\n");
			newLine = true;
		}			
		if(op.equals("("))
		{
			if(writestruct)
			{
				writeLine(structtowrite);
				writestruct = false;
			}
		}		


	}
	public boolean suppresspair = false;
	public boolean expectToken(String name)
	{
		if (tokenConsumed = (t.content.equals(name))) //token is consumed if valid
		{
			if(t.toktype == type.Operator)
			{
				if(t.content.equals("}"))
				{
					newLine = false;
					tablevel--;
				}
				else if(t.content.equals("."))
				{
					dontwriteatall = true;
				}
				else if(t.content.equals("="))
				{
					if(!writeEqual)
					dontwriteatall = true;
				}
				else if(t.content.equals("("))
				{
					if(suppressopenparenthesis || suppresspair)
						dontwriteatall = true;
					suppressopenparenthesis = false;
				}
				if(t.content.equals(")"))
				{
					if( suppresspair)
						dontwriteatall = true;
					suppresspair = false;
				}

				writeLine(t.content);
				if(!dontSwitch)
				dontwriteatall = false;
				operatorSideEffect(t.content);
			}
			if(t.toktype == type.Keyword)
			{
				if(t.content.equals("if") || t.content.equals("return") || t.content.equals("while") || t.content.equals("nil"))
					writeLine(t.content + " ");
			}
			//writeLine(super.t.content);
			getNextToken();
			return true;
		}
		else return false;
	}
	public boolean expectToken(type toktype)
	{
		if (tokenConsumed = (t.toktype == toktype)) //token is consumed if valid
		{
		//	debugTranslate(super.t.content);
			getNextToken();
			return true;
		}
		else return false;
	}
	public String errorSetup()
	{
		return Value.taxonomy + " " + Key + " on line " + linenum + " has already been declared in "+ Value.location +"!";
	}
	public boolean addScope(String key, HashMap<String, IdentifierContext> val)
	{
		if(HashMapMap.containsKey(key))
		{
			IdentifierContext toprint = hashmap.get(key);
			if(se == null)
			{
				se = new SemanticError( errorSetup());//"Implementation member" + " " + key + " on line " + linenum + " has already been declared in "+ "implementation" +"!");
			}
			return false;
		}
		else
		{
			debugTranslate("Added new Scope: " + val + " " + key);
			HashMapMap.put(key, val);		
			return true;
		}
	}
	public void addKey()
	{
		if(hashmap.containsKey(Key))
		{
			IdentifierContext val = hashmap.get(Key);
			if(se == null)
			se = new SemanticError( errorSetup());
		}
		else
		{
			debugTranslate("Added: " + Key + " " + Value.toString() + " " + Key);
			hashmap.put(Key, Value.clone());	
			Value.clear();
		}
	}
	public void writeprintscanFunctions()
	{
		dontwriteatall = false;
		writeLine("char global__printc(struct global *self, char val){ \n    printf(\"%c\",val); \n    return val; \n} \n \nint global__printi(struct global *self, int val){ \n    printf(\"%d\",val); \n    return val; \n} \n \ndouble global__printd(struct global *self, double val){ \n    printf(\"%lf\",val); \n    return val; \n} \n \nchar global__scanc(struct global *self){ \n    char RetVal; \n    scanf(\"%c\",&RetVal); \n    return RetVal; \n} \n \nint global__scani(struct global *self){ \n    int RetVal; \n    scanf(\"%d\",&RetVal); \n    return RetVal; \n} \n \ndouble global__scand(struct global *self){ \n    double RetVal; \n    scanf(\"%lf\",&RetVal); \n    return RetVal; \n} \n");
	}
	public void writeStorageStructs()
	{
		dontwriteatall = false;
		//(HashMapMap.toString());
		Set<String> interfaces = HashMapMap.keySet();
		Set<String> keys;
		Set<String> params;
		Iterator<String> interfaceiterator = interfaces.iterator();
		Iterator<String> selfiterator;
		Iterator<String> scopeiterator;
		IdentifierContext pfun;
		IdentifierContext para;
		String keyName;
		String structName;
		String param;
		HashMap<String, IdentifierContext> printingTable;
		HashMap<String, IdentifierContext> adding;
		LinkedList<String> lineRenderer = new LinkedList<String>();
		LinkedList<String> lineRenderer2 = new LinkedList<String>();
		
		if(!HashMapMap.containsKey("global_new"))
		{
			HashMapMap.put("global_new", new HashMap<String, IdentifierContext>());
		}			
		adding = HashMapMap.get("global_new");
			printingTable = HashMapMap.get("global");
			interfaceiterator = printingTable.keySet().iterator();
			while(interfaceiterator.hasNext())
			{
				keyName = interfaceiterator.next();
				//(keyName);
				if(!adding.containsKey(keyName) && !HashMapMap.get("global").get(keyName).isFunction)
				{
					adding.put(keyName, printingTable.get(keyName));
				}
			}
			interfaceiterator = interfaces.iterator();
		//(HashMapMap.toString());
		while(interfaceiterator.hasNext())//for each interface
		{		
			structName = interfaceiterator.next();
			//(structName);
			selfiterator = HashMapMap.get(structName).keySet().iterator();
			
			if(structName.lastIndexOf("_new") != -1 && structName.lastIndexOf("_new") == structName.length()-4)
			{
				//(structName);
				//System.out.print(HashMapMap.get(structName).toString());
				String structNamenoNew = structName.substring(0,structName.lastIndexOf("_new"));
				writeLine("struct " + structNamenoNew + "\n{\n");
				
				lineRenderer.add("struct " + structNamenoNew + " * " + structName
				+ "(void){\n    struct "+ structNamenoNew + " * self = (struct " + structNamenoNew + "*)malloc(sizeof(struct "
				+ structNamenoNew + "));");
				while(selfiterator.hasNext()) //for each key
				{
					printingTable = HashMapMap.get(structName);
					keyName = selfiterator.next();
					pfun = printingTable.get(keyName);
					//	(keyName);
					//(printingTable.toString());
					
					writeLine( "    "+pfun.type + " " + keyName + ";\n");	
					lineRenderer.add("\n    self->"+keyName + " = " + (pfun.value.equals("nil")?0:pfun.value) + ";");
					lineRenderer2.add(pfun.type + " " + structNamenoNew + "_g" + keyName + "(struct " + structNamenoNew + " * self){\n");
					lineRenderer2.add("    return self->" + keyName + ";\n");
					lineRenderer2.add("}\n\n");
					lineRenderer2.add(pfun.type + " " + structNamenoNew + "_s" + keyName + "(struct " + structNamenoNew + " * self, "+pfun.type + "  __val"+ "){\n");
					lineRenderer2.add("    return self->" + keyName + " = __val;\n");
					lineRenderer2.add("}\n\n");

					
					//break;
				}
				lineRenderer.add("\n    return self;\n}\n");
				
				writeLine("};\n");
				//lineRenderer.add("\n}\n");
				int size = lineRenderer.size();
				while(!lineRenderer.isEmpty())
				{
					writeLine(lineRenderer.poll());
				}
				while(!lineRenderer2.isEmpty())
				{
					writeLine(lineRenderer2.poll());
				}
			}
			
		}
		
		if(HashMapMap.get("global_new").size() != 0)
		{
			writeLine("struct global global__global = {");
			interfaceiterator = HashMapMap.get("global_new").keySet().iterator();
			printingTable = HashMapMap.get("global_new");
			IdentifierContext k;
			while(interfaceiterator.hasNext())
			{
				k = printingTable.get(interfaceiterator.next());
				if(!k.value.equals("nil"))
				{
					writeLine(k.value);
				}
				else
				{
					if(k.type.equals("float") || k.type.equals("float"))
						writeLine("0.0");
					else 
						writeLine("0");
				}
				if(interfaceiterator.hasNext())
					writeLine(",");
			}
			writeLine("};\n\n");
		}	
		else
		{
			writeLine("struct global global__global;\n\n");
		}
		dontwriteatall = true;
	}
	public boolean Program()
	{
		try
		{
			try{
				getNextToken();
				while(this.InterfaceDeclaration())
				{
						
				};		
				debugPrint("exit interface");

				if( this.MainDeclaration() )
				{
					debugPrint("Exit Main");
					while(this.StorageDeclaration())
					{
						
					};
					writeStorageStructs();
					
					if(e == null) //stop if error
					while(this.ImplementationDeclaration())
					{
						
					}
					dontwriteatall = false;
					//writeLine("}\n");
					writeprintscanFunctions();
				}
				else
					createException("Program");
				if(t.toktype != type.None) //end of program
					createException("Program");
				
				if(super.e != null)
					throw super.e;
			}
			catch(Exception e)
				{
					System.out.format( "Syntax Error: In rule %s unexpected token \"%s\" of type %s on line %d\n", e.getMessage(), t.content, t.toktype, t.lineNum  );
				}	
			debugTranslate(HashMapMap.toString());
			if(se != null && super.e == null)
				throw se;
			return true;
		}
		catch(SemanticError se)
		{
			System.out.format( "\nSemantic Error: %s", se.getMessage());
			return true;
		}
	}
	public boolean InterfaceDeclaration()
	{
		
		Value.location = "interface";
		if(this.expectToken("interface")) //interface
		{
			String name = t.content;
			debugTranslate(name);			
			String tempself = self;
			String lastscope = scope;
			scope = name;
			addScope(name, new HashMap<String, IdentifierContext>());

			self = name;
			hashmap = HashMapMap.get(name);
			if(this.Identifier() ) // identifier {
			{
				if(this.expectToken("{")) 
				{
					while(this.MemberDeclaration());
				} // {MemberDeclaration}
				 
				if( this.expectToken("}")) 
				{ 
					//end of scope
					scope = lastscope;
					self = tempself;
					hashmap = HashMapMap.get("global");
					return true; //
					
				}
			}
			
			createException("InterfaceDeclaration");
			return false;
		} 
		else if (this.MemberDeclaration()) 
		{
			return true; // | MemberDeclaration
		}		
		return false;
	}
	public boolean MainDeclaration()
	{
		writeEverythingBeforeMain();
		debugTranslate("Self: " + self);
		scope = "main";
		self = "main";
		//self = "main";
		HashMapMap.put("main", new HashMap<String, IdentifierContext>());
		hashmap = HashMapMap.get("main"); //prepare for main declarations
		writeLine("int main(int argc, char *argv[])");

				dontwriteatall = true;
				dontSwitch = true;
		String[] mainparenthesis = { "main", "(", ")" };
		if(this.expectToken("void"))
		{
			if(expectTokens( mainparenthesis)) // void main ()
			{		
				dontwriteatall = false;
				dontSwitch = false;
				if(this.Block()) // Block
				{
					
					scope = "global";
					self = "global";
					return true;
				}
			}
			else
			createException("MainDeclaration");
		}
		return false;
	}
	public boolean StorageDeclaration()
	{
		debugTranslate("Enter Storage");
		
		dontSwitch = true;
		dontwriteatall = true;
		if(this.expectToken("storage")) //storage
		{
			storage = true;
			token temp = t.clone();
			if( this.expectToken("global") || this.Identifier() ) //global | Identifier {
			{
				String previousscope = scope;
				if(HashMapMap.containsKey(temp.content))
				{
					scope = temp.content+"_new";
					hashmap = new HashMap<String, IdentifierContext>();
					//HashMapMap.put(temp.content+"_new", hashmap);
					addScope(temp.content+"_new", hashmap);
					debugTranslate(hashmap.toString());
				}
				else
				{
					if(se == null)
					{
						se = new SemanticError("Storage "+ temp.content + " on line " + temp.lineNum + " has not been declared!" );
						
					}
				}
				debugTranslate("Curent scope: "+ scope);
				if ( this.expectToken("{") )
				{
					while( this.VariableDeclaration() )
					{
					}
					debugPrint("exit storage");
					if (this.expectToken("}")) 
					{
						scope = previousscope;
						return true; // }
					}
				} // {Variable Declaration}
				
				else 
				{
					createException("StorageDeclaration"); 
					return false; 
				}
				
			}
			else 
			createException("StorageDeclaration");
		}
		return false; 
	}

	public boolean ImplementationDeclaration()
	{
		//set a new a working hashmap that records all variable declarations
		storage = false;
		dontSwitch = true;
		dontwriteatall = true;
		debugTranslate("Enter implementation");
		hashmap = HashMapMap.get(scope); // no longer in main scope
		Value.location = "implementation";
		Value.isFunction = true;
		if(this.expectToken("implementation"))
		{			
			String previousscope = scope;
			String previousself = self;
			HashMap<String, IdentifierContext> returntable = hashmap;
			if(HashMapMap.containsKey(t.content))
			{
				scope = t.content;
				self = t.content;
				hashmap = HashMapMap.get(t.content);
				debugTranslate("Self is: "  + self);
			}
			else
			{
				if(se == null)
					se = new SemanticError("Implementation "+ t.content + " on line " + t.lineNum + " has not been declared!" );
			}
			if(this.Identifier())
			{
				if( this.expectToken("{") )
				{
					while(this.FunctionDefinition())
					{
						
					}	
					if(this.expectToken("}"))
					{
						scope = previousscope;
						self = previousself;
						hashmap = returntable;
						return true;
					}
				}
			}
			createException("ImplementationDeclartion");
		}
		else if (this.FunctionDefinition())
		{
			return true;
		}
		return false;
	}
	public boolean MemberDeclaration()
	{
		//add Member declaration to interface or global
		HashMap<String, IdentifierContext> throwabletable = new HashMap<String, IdentifierContext>();;
		if(this.DeclarationType())
		{
			
			String name = Key;
			debugTranslate("member name: " + name);		
			Key = name;
			debugTranslate("next token:" +t.content);
			token temp = t.clone();
			if(t.content.equals(";"))
			{
				debugTranslate("not function");
				Value.isFunction = false;
			}
			else
			{
				
				debugTranslate("is function");
				Value.isFunction = true;
			}
			t = temp;
			Value.taxonomy = Value.isFunction? "Member function" : "Data member";

			String tempscope = scope;
			HashMap<String, IdentifierContext> returntable = hashmap;
			hashmap = HashMapMap.get(scope);

			addKey();
			if(Value.isFunction)
			{
				hashmap = throwabletable;
				scope = name;
			}
			debugTranslate("scope:" +scope);
			debugTranslate("next token:" +t.content);
			if(this.DataDeclaration() || this.FunctionDeclaration())
			{
				if(Value.isFunction)
				{
					HashMapMap.get(tempscope).get(scope).setTable(throwabletable);
					HashMapMap.get(tempscope).get(scope).numArgs = throwabletable.size();
					debugTranslate("interface scope: " + tempscope);
					debugTranslate("member scope: " + scope);
					debugTranslate("table : " + throwabletable);
					debugTranslate("End.");
				}

				hashmap = returntable;
				scope = tempscope;
				//no need to set scope
				return true;
			}
		}
		return false;
	}
	public boolean Block()
	{
 		newLine = false;
		storage = false;	
		writeEqual =true;
		writestruct = false;
		writecommaifconstant = false;
		suppressopenparenthesis = false;
		dontSwitch = false;
		dontwriteatall = false; 
		tablevel = 0;
		//debugTranslate("Enter Block");
		Value.location = "block";
		if(this.expectToken("{"))
		{
			while(this.VariableDeclaration())
			{
				
			};
			//debugTranslate("Enter Statement");
			while(this.Statement())
			{
				
			};
			if(this.expectToken("}"))
			{

				return true;
			}				
			
			createException("Block");
		}
		return false;
	}
	public boolean VariableDeclaration()
	{
		token temptok = t.clone();
		//debugTranslate("Enter Var dec");
		if(this.DeclarationType())
		{
			Value.isFunction = false;
			Value.taxonomy = "Variable";
			if(this.expectToken("="))
			{
				token temp = t.clone();
				
				if(this.Constant())
				{
					if(Value.value.equals("nil"))
					{
						Value.value = temp.content;
					}
					else	
					{
						if(se == null)
						{
							se = new SemanticError(errorSetup());
						}
					}
					
				}
				else
				{
					createException("VariableDeclaration");
					return false;
				}
			}
			if(this.expectToken(";"))
			{
				
 				// debugTranslate("Scope: " + scope);
 				// debugTranslate("self: " + self);
				if(storage)
				if(HashMapMap.get(self).containsKey(Key))
				{
					//debugTranslate(HashMapMap.get(self).toString());
					if(!HashMapMap.get(self).get(Key).type.equals(Value.type))
					{
						if(se == null)
						{
							se = new SemanticError("Storage data member "+ Key +" on line " + temptok.lineNum + " has different declared type!");
						}
					}
				}/* 
				debugTranslate("KEY: " + Key);
				debugTranslate("VAL: " + Value);
				debugTranslate("table: " + hashmap.toString());  */
				
				addKey();
				return true;
			}
			
		}
		return false;
	}
	public boolean tablesEqual(String name, HashMap<String, IdentifierContext> a,HashMap<String, IdentifierContext> b)
	{
		Iterator<String> firsttable = a.keySet().iterator();
		Iterator<String> secondtable = b.keySet().iterator();
		while(firsttable.hasNext())
		{
			if(secondtable.hasNext())
			{
				String x = a.get(firsttable.next()).type;
				String y = b.get(secondtable.next()).type;
				if(!x.equals(y))
				{
					if(se == null)
					{
						se = new SemanticError("Parameter for function " + name + " does not match with implementation!");
						return false;
					}
				}
				
			}
			else
			if(se == null)
			{
				se = new SemanticError("Number of parameter for function " + name + " do not match in implementation.");
				return false;
			}
		}
		if(secondtable.hasNext())
		{
			debugTranslate("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa	"+ secondtable.next());
			if(se == null)
			{
				se = new SemanticError("Number of parameter for function " + name + " do not match in implementation.");
				return false;
			}
		}
			return true;
	}
	
	public void writeParams(HashMap<String,IdentifierContext> hash)
	{
		Iterator<String> it = hash.keySet().iterator();
		IdentifierContext t;
		String s;
		while(it.hasNext())
		{
			s = it.next();
			t = hash.get(s);
			writeLine(", " +t.type + " __" + s);
		}
	}
	public boolean FunctionDefinition()
	{
		
		debugTranslate("Enter functiondef");
		token temptok = t.clone();
		if(this.DeclarationType())
		{
			String returnScope = scope;
			HashMap<String, IdentifierContext> returntable = hashmap;
			addScope(scope+"__"+Key, new HashMap<String, IdentifierContext>());
			String name = Key;
			debugTranslate("Retrieve Scope: 	" + scope );
			IdentifierContext iC;
			hashmap = HashMapMap.get(scope+"__"+Key);
			debugTranslate( scope + " ________" +hashmap.toString());
			scope = scope+"__"+Key;

			if(this.ParameterBlock())
			{
				debugTranslate("Scope: " + scope);
				debugTranslate("self: " + self);
				if(HashMapMap.get(self).containsKey(name))
				{

					if(!Value.type.equals(HashMapMap.get(self).get(name).type))
					{
						if(se == null)
							se = new SemanticError("Implementation member on line " + temptok.lineNum + " has different declared types!");
					}
					if(!tablesEqual(name,HashMapMap.get(self).get(name).table, hashmap))
					{
					}
				}
				
				dontwriteatall = false;
				writeLine(temptok.content + " " + self +"__" + name + "(struct " + self + "* self");
				writeParams(hashmap);
				writeLine( ")" );
			    debugTranslate("KEY: " + name);
				if(this.Block())
				{
					
					//debugTranslate("VAL: " + Value);
					//debugTranslate("table: " + hashmap.toString());  
				
					
					dontSwitch = true;
					dontwriteatall = true;
					
					scope = returnScope;
					hashmap = returntable; //returns back to previous scope
					return true;
				}
			}
			createException("FunctionDefinition");
		}
		return false;
	}
	public boolean DeclarationType()
	{
		if(this.DataType())
		{
			token temptok = t.clone();
			if(this.Identifier())
			{
				if(scope.equals("main"))
				{
					writeLine("__");
				}
				writeLine(temptok.content);
				debugTranslate(Key + " "+ Value.toString());
				return true;
			}
			createException("DeclarationType");
		}
		return false;
	}
	public boolean DataDeclaration()
	{
		boolean val =  super.DataDeclaration();
		if(val)
		Value.isFunction = false;
		debugTranslate("Data Declaration ");
		//if(val)
		//addKey();
		return val;
	}	
	public boolean FunctionDeclaration()
	{
		boolean val =  super.FunctionDeclaration();
		if(val)
		Value.isFunction = true;	
		debugTranslate("Function Declaration ");
		//if(val)
		//addKey();
		return val;
	}
	public boolean Statement()
	{	
		//debugTranslate("Enter Statement");
		return super.Statement();
	}
	public boolean Constant()
	{
		token temptok = t.clone();
		boolean val = super.Constant();
		if(val)
		{
			if(writecommaifconstant)
			{
				commastoprint--;	
				if(commastoprint < 1)
				writecommaifconstant = false;
				writeLine(", ");
			}
			writeLine(temptok.content);
		}
		return val;
	}
	public boolean ParameterBlock()
	{
		//debugTranslate("Enter Parameter Block");
		IdentifierContext prevCtx = Value.clone();
		debugTranslate(t.content);
		boolean val =  super.ParameterBlock();
		Value = prevCtx;
		
		return val;
	}
	String justforUnsigned = "";
	public boolean DataType()
	{
		String temp = t.content;
		
		Value.type = "";
		if(this.IntegerType() || this.FloatType())
		{
			if(!justforUnsigned.equals(""))
			{
				writeLine(temp +" " + justforUnsigned + " ");
				Value.type = temp +" " +justforUnsigned;
			}
			else
			{
				writeLine(temp + " ");
				Value.type = temp;
			}
			justforUnsigned = "";
			return true;
		}
		else if(this.InstanceType())
		{
			
			Value.type = lastIdentifier;
			return true;
		}

		return false;
	}
	public boolean Assignment()
	{
		if(this.expectToken("let"))
		{
			isSet = true;
			if(this.Descriptor())
			{

				if(this.expectToken("="))
				{
					if(this.Expression())
					{
						if(writeEqual == false)
						{
							writeEqual = true;
							writeLine(")");
						}
						if(this.expectToken(";"))
						{
							return true;
						}
					}
				}
			}
			createException("Assignment");
		}
		return false;
	}
	public boolean WhileLoop()
	{
		return super.WhileLoop();
	}
	public boolean IfStatement()
	{
		return super.IfStatement();
	}
	public boolean ReturnStatement()
	{
		return super.ReturnStatement();
	}
	public boolean Expression()
	{
		//debugTranslate("enter expression");
		return super.Expression();
	}
	public boolean InstanceConstant()
	{		
		return super.InstanceConstant();
	}
	public boolean Parameter()
	{
		//debugTranslate("Enter Param");
		Value.location = "parameter";
		Value.isFunction = false;
		if(this.DataType())
		{
			if(this.Identifier())
			{
				debugTranslate(scope);
				debugTranslate("Context" + Value);
				addKey();
				return true;
			}
			
			createException("Parameter");
		}
		return false;
	}
	public boolean IntegerType()
	{
		String[] IntegerTypes = {"char" , "short", "int", "long"};
		if(this.expectToken("unsigned"))
		{
			justforUnsigned = t.content;
			getNextToken();
			if( expectOne(IntegerTypes) )
			{
				return true;
			}
			createException("IntegerType");
			return false;
		}		
		else
		if( expectOne(IntegerTypes) )
		{
			return true;
		}
		return false;
	}
	public boolean FloatType()
	{		
		return super.FloatType();
	}
	public boolean InstanceType()
	{
		
		if(this.expectToken("instance"))
		{
			writeLine("struct ");
			token temptok = t.clone();
			if(this.Identifier())
			{
				writeLine(temptok.content + " * ");
				return true;
				
			}
			createException("InstanceType");
		}
		return false;
	}
	public boolean isSet = false;
	public boolean isAllocator = false;
	public int numargs;
	public IdentifierContext lookfor(String name, String scope)
	{
			
		HashMap<String, IdentifierContext> search = hashmap;
		//debugTranslate("scope is: "+ scope);
		search = HashMapMap.get(scope);
		if(search.containsKey(name))
		{
			//debugTranslate("found identifier in : "+ scope);
			numargs= search.get(name).numArgs;
			commastoprint = search.get(name).numArgs;
			return search.get(name).clone();
		}
		return new IdentifierContext();
	}
	public boolean primativetype(String valtype)
	{
		String[] primativetypes = {"unsigned int", "unsigned char", "unsigned short", "int", "char", "short","long","float","double"};
		for(int i = 0; i < primativetypes.length; i++)
		{
			if(valtype.equals(primativetypes[i]))
			{
				return true;
			}
		}

		return false;
	}
	String buildDescriptor;
	public boolean Descriptor()
	{
		//debugTranslate("Enter Descriptor");
		token temptok = t.clone();			
		HashMap<String, IdentifierContext> returntable = hashmap;
		IdentifierContext val, valSelf, valScope, valGlobal;	
		val = new IdentifierContext();	
		boolean isIdentifier = this.Identifier();
		boolean isSelf = this.expectToken("self");
		boolean isGlobal = this.expectToken("global");

		String valactualname = temptok.content;
		LinkedList<String> namelist = new LinkedList<String>();
		LinkedList<Boolean> isFunctionList = new LinkedList<Boolean>();
		LinkedList<String> typeList = new LinkedList<String>();
		LinkedList<String> scopeList = new LinkedList<String>();
		if((isIdentifier || isSelf || isGlobal))
		{
			//debugTranslate(Boolean.toString(isIdentifier));
			//debugTranslate("Looking for Identifier: " + temptok.content);
			if(isIdentifier)
			{
				String temp;
				valScope = lookfor(temptok.content,  scope);
				valSelf = lookfor(temptok.content, self);
				valGlobal = lookfor(temptok.content,"global");
				val = valScope;
				temp = scope;
				if ( val.badID == true )
				{
					temp = self;
					val = valSelf;
					if( val.badID == true )
					{
						val = valGlobal;
						temp = "global";
						if(val.badID == true)
						{
							if(se == null)
							se = new SemanticError( "Invalid descriptor " + temptok.content + " on line " + temptok.lineNum);
						}
					}
				}								
				// if(val.type.equals("main"))
					// val.type = "global";
				mustbefunction = val.isFunction;
				mustbefunctionname = temptok.content;
				if(temp.equals(scope))
					namelist.push("__"+temptok.content);
				else if(temp.equals(self))
				{
					namelist.push("self->"+temptok.content);
				}
				else if(temp.equals("global"))
				{
					namelist.push(temptok.content);
				}
				isFunctionList.push(val.isFunction);
				typeList.push(val.type);
				scopeList.push(temp);
			}
			else if(isSelf)
			{
				val.type = self; 				
				namelist.push("self");
				isFunctionList.push(false);
				// if(self.equals("main"))
					// self = "global";
				typeList.push(self);
				 if(self.equals("main"))
					scopeList.push("global");
				else
					scopeList.push("self->");
					
			}
			else if(isGlobal)
			{
				val.type = "global";				
				namelist.push("global");
				isFunctionList.push(false);
				typeList.push("global");
				scopeList.push("global");
			}

			while(this.expectToken("."))
			{			
				temptok = t.clone();
				debugTranslate("Looking for "+ val.type);
				if(HashMapMap.containsKey(val.type))
				{
					debugTranslate("Found "+ val.type);
					hashmap = HashMapMap.get(val.type);
				}
				else
				{
					if(se == null)
					{
						se = new SemanticError("Interface " + val.type + " does not exist!" + temptok.lineNum);
					}
				}
				if(this.Identifier())
				{
					String valname = val.type;
					val = lookfor(temptok.content, val.type);

					if(val.badID == true )
					{
						if(se == null)
						se = new SemanticError( "Invalid descriptor " + temptok.content + " on line " + temptok.lineNum);
					}
					else
					{			
						// if(val.type.equals("main"))
							// val.type = "global";
						namelist.push(temptok.content);
						isFunctionList.push(val.isFunction);
						typeList.push(val.type);
						scopeList.push(valname);
						mustbefunction = val.isFunction;
						mustbefunctionname = temptok.content;
					}
				}
				else
				{
					createException("Descriptor");
					return false;
				}
			}
			//typeList.pop();

			String name = "";
			boolean isf = false;
			boolean first = true;
			int countOpen = 0;
			String types = "";
			String getset = isSet?"s":"g";
			boolean justIdentifier = true;
			String scopes = "";
			if(typeList.size() != 0)
				types = typeList.get(0);
			String reference = namelist.getFirst();
			while(typeList.size() > 1)
			{
				justIdentifier = false;
				countOpen++;
				name = namelist.poll();
				isf = isFunctionList.poll();
				types= typeList.poll();
				scopes = typeList.poll();
				if(scopes.equals("main"))
				{
					scopes = "global";
				}
				if(isf)
				{
					writeLine(scopes+"__"+name+"(");
					suppressopenparenthesis = true;
				}
				else
				{
					if(first)
						writeLine(scopes+"_"+getset+name+"(");
					else
						writeLine(scopes+"_"+getset+name+"(");
					first = false;
					getset = "g";
				}
			}
			isf = isFunctionList.poll();
			name = namelist.poll();
			scopes = scopeList.poll();
			if(!isf)
			{
				if(scopes.equals("global"))
				{
					if(name.equals("global"))
					{
						writeLine(name);
					}
					else
					writeLine("global_g" +name +"(global)");
				}
				// else if(scopes.equals(self))
				// {
					// writeLine(self+"_g" +name +"(__"+name+")");
				// }
				else
				writeLine(name);
				if(name.equals("self") && !scopes.equals("main")) //the biggest ducktape in the world
				{
					writeLine(", ");
				}
			}
			else
			{
				if(numargs == 0)
					suppresspair = true;
				else
				suppressopenparenthesis = true;
				writeLine(scopes+"__"+name+"(" + scopes);
				//write
			}
			if(isSet) //leave one open for set
			{
				
				countOpen--;
				if(!justIdentifier)
				writecommaifconstant = true;
				if(!justIdentifier)
				writeEqual = false;
				commastoprint = 1;
				
			}				
			if(isf)
			{
				writeEqual = false;
				writecommaifconstant = true;

			}
			if(writecommaifconstant && commastoprint > 0)
			{
				commastoprint--;
				if(commastoprint < 1)
					writecommaifconstant = false;
				writeLine(", ");
			}
			for(;countOpen>0;countOpen--)
			{
				//writeLine(")");
			}

			isSet = false;
			hashmap = returntable;
			return true;
		}
		return false;
	}
	public boolean SimpleExpression()
	{
		
		//debugTranslate("enter simple ex");
		return super.SimpleExpression();
	}
	public boolean RelationOperator()
	{
		return super.RelationOperator();
	}
	public boolean Term()
	{
		return super.Term();
	}
	public boolean AddOperator()
	{
		return super.AddOperator();
	}
	public boolean Factor()
	{
		//debugTranslate("enter Factor" + t.content);
		if( this.expectToken("(") )
		{
			
			if(this.Expression())
			{
				if(this.expectToken(")"))
				{
					return true; 
				}
			}
			
			createException("Factor");
		}
		else if( this.Constant() )
		{
			return true;
		}
		else if ( this.Descriptor())
		{
	
			if( this.expectToken( "(") )
			{
				if(!mustbefunction)
				{
					if(se == null)
						se = new SemanticError(mustbefunctionname + " is not callable!");
					}
				if( this.Expression() )
				{
					while(this.expectToken(","))
					{
						if(this.Expression())
						{
							
						}
						else 
						{
							createException("Factor");
							return false;
						}
					}

				}
				if(this.expectToken(")"))
				{
					debugPrint("exit expression");
					return true;
				}
				return false;
			}
			return true;
			
		}
		else if(this.Allocator())
		{
			return true;
		}
		return false;
	}
	public boolean MultOperator()
	{
		
		//debugTranslate("enter mult");
		return super.MultOperator();
	}
	public boolean Allocator()
		{
		boolean val = super.Allocator();
		if(val)
		{
			if(writecommaifconstant)
			{
				commastoprint--;
				if(commastoprint < 1)
				writecommaifconstant = false;
				writeLine(", ");
			}
			writeLine(Key+"_new()");
		}
		return val;
	}
	public boolean Identifier()
	{
		token temp = t.clone();
		boolean val = super.Identifier();
		if (val)
		{
			/**Temporary**/
			//writeLine(temp.content);
		}
		Key = temp.content;
		linenum = temp.lineNum;
		lastIdentifier = temp.content;
		return val;
	}
	
	public boolean IntConstant()
	{
		boolean val = super.IntConstant();
		return val;
	}

	public boolean FloatConstant()
	{
		boolean val = super.FloatConstant();
		return val;
	}
}
