
public class CParser
{
	String filename;
	CScanner scanner;
	token t;
	boolean tokenConsumed = true;
	Exception e;
	public static void main(String args[])
	{
		CParser obj = new CParser(args[0]);
		obj.openFile();
	}
	public CParser(String file)
	{
		filename = file;
		debugPrint(file);
		scanner = new CScanner(filename);
		e = null;
	}

	public void openFile()
	{
		if(scanner.openFile())
		{
			if(this.Program())
				
				System.out.println( filename +" is a valid X program!");
	
		} 
		
	}
	public void getNextToken()
	{
		if(!tokenConsumed) return; //safety
		tokenConsumed = false;
		t = scanner.getNextToken();
		debugPrint("next token: " + t.content);
	}
	public token peekNextToken()
	{
		return scanner.peekNextToken();
	}
	public boolean expectToken(String name)
	{		
		if ((tokenConsumed = (t.content.equals(name)))) //token is consumed if valid
		{
			getNextToken();
			return true;
		}
		else return false;
	}
	public boolean expectTokens(String[] names)
	{
		for(int i = 0; i < names.length; i++)
		{
			if(this.expectToken(names[i]))
				continue;
			else 
				return false;
		}
			return true;
	}
	public boolean expectOne(String[] names)
	{
		for(int i = 0; i < names.length; i++)
		{
			if(this.expectToken(names[i]))
				return true;
		}
			return false;
	}
	public boolean expectToken(type toktype)
	{
		if ((tokenConsumed = (t.toktype == toktype))) //token is consumed if valid
		{
			getNextToken();
			return true;
		}
		else return false;
	}
	public void debugPrint(String message)
	{
		//System.out.println(message);
	}

	void createException(String name)
	{		
		debugPrint("enter Error " + name);
		if(e == null)
		{
			e = new Exception(name);
			
		}
		
	}
	public boolean Program()
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
				if(e == null) //stop if error
				while(this.ImplementationDeclaration())
				{
					
				}
			}
			else
				createException("Program");
			if(t.toktype != type.None) //end of program
				createException("Program");
			
			if(e != null)
				throw e;
			else return true;
		}
		catch(Exception e)
			{
				System.out.format( "Syntax Error: In rule %s unexpected token \"%s\" of type %s on line %d\n", e.getMessage(), t.content, t.toktype, t.lineNum  );
			}	
			return false;
	}
	public boolean InterfaceDeclaration()
	{
		if(this.expectToken("interface")) //interface
		{
			if(this.Identifier() ) // identifier {
			{
				if(this.expectToken("{")) 
				{
					while(this.MemberDeclaration());
				} // {MemberDeclaration}
				 
				if( this.expectToken("}")) return true; // }
			}
			
			createException("InterfaceDeclaration");
			return false;
		} 
		else if (this.MemberDeclaration()) 
		{
			return true; // | MemberDeclaration %Dont call getNextToken in next call
		}		
		return false;
	}
	public boolean MainDeclaration()
	{
		
		String[] mainparenthesis = { "main", "(", ")" };
		if(this.expectToken("void"))
		{
			if(expectTokens( mainparenthesis)) // void main ()
			{
				if(this.Block()) // Block
				return true;
			}
			else
			createException("MainDeclaration");
		}
		return false;
	}
	public boolean StorageDeclaration()
	{
		if(this.expectToken("storage")) //storage
		{
			 
			if( this.expectToken("global") || this.Identifier() ) //global | Identifier {
			{
				if ( this.expectToken("{") )
				{
					while( this.VariableDeclaration() )
					{
					}
					debugPrint("exit storage");
					if (this.expectToken("}")) 
						return true; // }
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
		if(this.expectToken("implementation"))
		{
			if(this.Identifier())
			{
				if( this.expectToken("{") )
				{
					while(this.FunctionDefinition())
					{
						
					}	
					if(this.expectToken("}"))
					{
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
		debugPrint("enter Member Declartion");
		if(this.DeclarationType())
		{
			
			debugPrint(t.content);
			if(this.DataDeclaration() || this.FunctionDeclaration())
			{
				return true;
			}
		}
		return false;
	}
	public boolean Block()
	{
		if(this.expectToken("{"))
		{
			while(this.VariableDeclaration())
			{
				
			};
			while(this.Statement())
			{
				
			};
			if(this.expectToken("}")) return true;
			
			createException("Block");
		}
		return false;
	}
	public boolean VariableDeclaration()
	{
		debugPrint("enter VariableDeclaration");
		if(this.DeclarationType())
		{
			if(this.expectToken("="))
			{
				if(this.Constant())
				{
					
				}
				else
				createException("VariableDeclaration");
			}
			if(this.expectToken(";"))
			{
				return true;
			}
			
		}
		return false;
	}
	public boolean FunctionDefinition()
	{
		if(this.DeclarationType())
		{
			debugPrint("enter Function Definition");
			if(this.ParameterBlock())
			{
				if(this.Block())
				{
					return true;
				}
			}
			createException("FunctionDefinition");
		}
		return false;
	}
	public boolean DeclarationType()
	{
		debugPrint("enter Declaration Type");

		if(this.DataType())
		{
			if(this.Identifier())
			{
				return true;
			}
			createException("DeclarationType");
		}
		return false;
	}
	public boolean DataDeclaration()
	{
		debugPrint("enter DataDeclaration");
		if(this.expectToken(";")) return true;
		return false;
	}	
	public boolean FunctionDeclaration()
	{
		if(this.ParameterBlock())
		{
			if(this.expectToken(";")) return true;
			createException("FunctionDeclartion");
		}
		return false;	
	}
	public boolean Statement()
	{	
		debugPrint("Enter Statement");
		if(this.Assignment())
		{
			return true;
		}
		else if( this.WhileLoop() ) 
		{
			return true;
		}
 		else if( this.IfStatement() ) 
		{
			return true;
		}
		else if( this.ReturnStatement())
		{
			return true;
		}
		else if (this.Expression())
		{
			if(this.expectToken(";"))
			return true;
			else 
			createException("Statement");
		}
		return false;
	}
	public boolean Constant()
	{
		debugPrint("Enter Constant");
		if( this.IntConstant() || this.FloatConstant() || this.InstanceConstant() )
		{
			
			debugPrint("is Constant");
			return true;
		}
	return false;
	}
	public boolean ParameterBlock()
	{
		if(this.expectToken("("))
		{
			if(this.Parameter())
			{
				while( this.expectToken(",") )
				{
					if(this.Parameter())
					{
						
					}
					else
					{
						
						createException("ParameterBlock");
						return false;
					}
				}
			}
			if(this.expectToken(")")) {
				return true;
			}			
			createException("ParameterBlock");
		}
		return false;
	}
	public boolean DataType()
	{
		
		debugPrint("enter Data type");
		if(this.IntegerType() || this.FloatType() || this.InstanceType())
		{
			
			return true;
		}

		return false;
	}
	public boolean Assignment()
	{
		debugPrint("enter Assignment");
		if(this.expectToken("let"))
		{
			if(this.Descriptor())
			{
				if(this.expectToken("="))
				{
					if(this.Expression())
					{
						
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
		if(this.expectToken("while"))
		{
			if(this.expectToken("("))
			{
				if(this.Expression())
				{
					if(this.expectToken(")"))
					{
						if(this.Block())
						{
							return true;
						}
					}
				}
			}
			createException("WhileLoop");
		}
		return false;
	}
	public boolean IfStatement()
	{		
		if(this.expectToken("if"))
		{
			if(this.expectToken("("))
			{
				if(this.Expression())
				{
					if(this.expectToken(")"))
					{
						if(this.Block())
						{
							return true;
						}
					}
				}
			}
		createException("IfStatement");
		}
		return false;
	}
	public boolean ReturnStatement()
	{
		if(this.expectToken("return"))
		{
			if(this.Expression()) 
			{
				if(this.expectToken(";"))
				{
					return true;
				}
			}
			createException("ReturnStatement");
			
		}
		return false;
	}
	public boolean Expression()
	{
		debugPrint("Enter Expression");
		if(this.SimpleExpression())
		{
			debugPrint("exit simple expression");
			if( this.RelationOperator() )
			{
				if(this.SimpleExpression())
				{
					return true;
				}
				createException("Expression");
				
			}
			return true;
		}
		return false;
	}
	public boolean InstanceConstant()
	{		
		if(this.expectToken("nil"))
			return true;
		return false;
	}
	public boolean Parameter()
	{
		if(this.DataType())
		{
			if(this.Identifier())
			{
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
		String[] FloatTypes = {"float", "double"};

		if( expectOne(FloatTypes) )
		{
			return true;
		}
		return false;
	}
	public boolean InstanceType()
	{
		if(this.expectToken("instance"))
		{
			if(this.Identifier())
			{
				return true;
				
			}
			createException("InstanceType");
		}
		return false;
	}
	public boolean Descriptor()
	{
		debugPrint("Enter Descriptor");
		if(this.Identifier() || this.expectToken("self") || this.expectToken("global"))
		{
			while(this.expectToken("."))
			{
				debugPrint("got .");
				if(this.Identifier())
				{
					return true;
				}
				createException("Descriptor");
				return false;
			}
			debugPrint("exit Desscriptor");
			return true;
		}
		return false;
	}
	public boolean SimpleExpression()
	{
		debugPrint("Enter Simple Expression" + t.content);
		if(this.Term())
		{
			while(this.MultOperator())
			{
				
				debugPrint("is Mult op");
				
				if(this.Factor())
				{
					
				}
				else
				{
					createException("SimpleExpression");
					return false;
				}
			}
			return true;
		}
		return false;
	}
	public boolean RelationOperator()
	{
		debugPrint("enter RelationOperaton token: " + t.content );
		if(this.expectToken("=="))
		{
			return true;
		}
		else if(this.expectToken("<"))
		{
			return true;
			
		}		
		else if(this.expectToken(">"))
		{
			
			return true;
		}		
		else if(this.expectToken("<="))
		{
			
			return true;
		}		
		else if(this.expectToken(">="))
		{
			
			return true;
		}		
		else if(this.expectToken("!="))
		{
			
			return true;
		}
		else
			return false;
	
	}
	public boolean Term()
	{
		debugPrint("Enter Term");
		if(this.Factor())
		{
			while(this.MultOperator())
			{
				debugPrint("Got Mult op");
				if(this.Factor())
				{
					
				}
				else
				{
					createException("Term");
					return false;
				}
			}
			debugPrint("exit Term");
			return true;
		}
		return false;
	}
	public boolean AddOperator()
	{
		if(this.expectToken("+"))
		{
			return true;
		}
		else if( this.expectToken ("-") )
		{
			return true;
		}
		return false;
	}
	public boolean Factor()
	{
		debugPrint("enter Factor" + t.content);
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
		if( this.expectToken("*") )
		{
			return true;
		}
		else if( this.expectToken("/") )
		{
			return true;
		}
		return false;
	}
	public boolean Allocator()
	{
		if(this.expectToken("new"))
		{
			if(this.Identifier())
			{
				return true;
			}
			createException("Allocator");
		}
		return false;
	}

	public boolean Identifier()
	{
		
		if(this.expectToken(type.Identifier))
		{
			debugPrint("is Identifier");
			return true;
		}
		return false;
	}
	
	public boolean IntConstant()
	{
		if(this.expectToken(type.IntConstant))
		{
			debugPrint("is int const");
			return true;
		}
		return false;
	}
		public boolean FloatConstant()
	{
		if(this.expectToken(type.FloatConstant))
		{
			debugPrint("is FloatConstant");
			return true;
		}
		return false;
	}

}
