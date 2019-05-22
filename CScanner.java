import java.io.*;
import java.util.*;
import java.util.Hashtable;


public class CScanner
{

    private Scanner x;
    operatorHash opHash;
    keywordHash keyHash;
	token tokenBuffer = null;
	type lasttoktype = type.None;
    int i = 0;
    int lineNum = 0;
	boolean endoffile = false;
	String currentLine = "";
	token word;
	token temp;
	char currChar;
	String filename;
    public CScanner()
    {
        opHash = new operatorHash();
        keyHash = new keywordHash();
		tokenBuffer = new token(1,1,type.None, "\0");
		word = new token(1,1,type.None, "");
		temp = new token(1,1,type.None, "");
    }
	
	public CScanner(String file)
    {
        opHash = new operatorHash();
        keyHash = new keywordHash();
		tokenBuffer = new token(1,1,type.None, "\0");
		word = new token(1,1,type.None, "");
		temp = new token(1,1,type.None, "");
		filename = file;
    }
	
	boolean isAlphaOrU(char c)
	{
		if(c > 64 && c < 91)
			return true;
		if(c > 96 && c < 123)
			return true;
		if(c == '_')
			return true;
		return false;
	}
	
	boolean isInt(char c)
	{
		if(c > 47 && c < 58)
			return true;
		return false;
	}

    public boolean openFile()  // sets up scanner
    {
        try
        {
            x = new Scanner(new File(filename));
            return true;
        }
        catch(Exception e)
        {
            System.out.println("Error: File not Found.");
            return false;
        }

    }
    public type updateType(char c)
    {
		
		type t = word.toktype;
		if(opHash.contains(word.content))
		{
			if(c == '-' && isInt(currentLine.charAt(i+1)))
			{
				if(lasttoktype == type.IntConstant || lasttoktype == type.FloatConstant || lasttoktype == type.Identifier)
				{
					//do nothing
				}
				else{
				word.toktype = type.IntConstant;
				return type.IntConstant;
				}
			}
			word.toktype = type.Operator;
			return type.Operator;
		}
		else
		{
			if(t == type.Operator)
				word.toktype = type.Invalid;
			if(t == type.None)
			{
				word.toktype = type.Invalid;
				if(isAlphaOrU(c))
					word.toktype = type.Identifier;
				if(isInt(c) || c == '-')
				{
					if(c == '-')
					{
						
					}
					else
					word.toktype = type.IntConstant;
				}
			}
			if(t == type.Identifier)
			{
				if(!isAlphaOrU(c) && !isInt(c)) // is neither a-z, A-Z, _ ,or #
					word.toktype = type.Invalid;
					
			}
			if(t == type.IntConstant)
			{
				if(c == '.')
				{
					word.toktype = type.MaybeValid; //can still be invalid
				}
				else
				if(!isInt(c))
				{
					word.toktype = type.Invalid;
				}
			}
			if(t == type.MaybeValid || t == type.FloatConstant)
			{
				if(!isInt(c))
					word.toktype = type.Invalid;
				else
					word.toktype = type.FloatConstant;
			}
			
			return word.toktype;
		}
    }

    public void tokenAdd(token tok)
    {
		String content = tok.content;
		if(opHash.contains(content))
			tok.toktype = opHash.get(content);
		if(keyHash.contains(content))
			tok.toktype = keyHash.get(content);
		if(tok.toktype == type.MaybeValid) //too late for that
			tok.toktype = type.Invalid;
		tokenBuffer.assign(lineNum,i+1 - tok.content.length(),tok.toktype, tok.content);
		lasttoktype = tok.toktype;
	}

	public boolean getNextLine()
	{
		if(x.hasNextLine())
		{
			lineNum++;
			currentLine = x.nextLine();
			i=0;
		}
		else
		{
			tokenBuffer = new token(lineNum+1, 1, type.None, "");
			endoffile = true;
			return false;
		}
		return true;
	}    
	public token getNextToken()  // read and token the file
    {
		tokenBuffer.clear();
		while(tokenBuffer.toktype == type.None)
		{
			if(i >= currentLine.length())
			{
				if(!getNextLine())
					return tokenBuffer;
			}	
			for(;i<currentLine.length();i++)
			{
				currChar = currentLine.charAt(i);
				if(currChar == ' ' || currChar == '\t' || currChar == '\0')
				{
					if(word.toktype != type.None)
					{
						tokenAdd(word);
						//System.out.println("token added: " + word.content);
						word.clear();
						i++;
						return tokenBuffer;
					}
					continue;
				}
				temp.content = word.content; // dont do temp = word
				temp.toktype = word.toktype;
				word.append(currChar); //not a space, so add to word
				type t = updateType(currChar);
				//System.out.println(t.name() + " " + word.content);
				if(t == type.Invalid)
				{
					if(temp.toktype != type.None)
					{
						tokenAdd(temp);
						//System.out.println("token added: " + temp.content);
						word.clear();
						return tokenBuffer;
					}
					tokenAdd(word);
					//System.out.println("token added: " + word.content);
					word.clear();
					i++;
					return tokenBuffer;
				}
			}
			if(word.content.length() > 0)
			tokenAdd(word);
			word.clear();
		}
		return tokenBuffer;
		
    }
	public token peekNextToken()
	{
		token tokenPocket = tokenBuffer.clone();
		int tempi = i;
		int tempLinenum = lineNum;
		type templasttokentype = lasttoktype;
		String tempcurrentLine = currentLine;
		boolean tempend = endoffile;
		//save state
		//dostuff
		token t = getNextToken().clone();
		//reloadstate
		tokenBuffer = tokenPocket;
		i =tempi;
		lineNum = tempLinenum;
		lasttoktype = templasttokentype;
		currentLine = tempcurrentLine;
		endoffile = tempend;
		try{
			x = new Scanner(new File(filename));
		}
		catch(Exception ThisShouldntFailAnyways)
		{
			
		}
		for(int seek = lineNum; seek > 0; seek--)
			x.nextLine();
		return t;
	}


    public void printTokens()
    {

		tokenBuffer.debugPrint();
       
    }


    public static void main(String[] args)
    {

        CScanner obj = new CScanner();
		obj.filename = args[0];
		if(obj.openFile())
        while(!obj.endoffile)
		{
			//obj.peekNextToken().debugPrint();
			obj.getNextToken().debugPrint();
		}
    }
}

 class operatorHash { 
	
	Hashtable<String, type> operators = new Hashtable<String, type>();
	
	public operatorHash(){
		operators.put("(", type.Operator);
		operators.put(")", type.Operator);
		operators.put("{", type.Operator);
		operators.put("}", type.Operator);
		operators.put(",", type.Operator);
		operators.put(".", type.Operator);
		operators.put(";", type.Operator);
		operators.put("+", type.Operator);
		operators.put("*", type.Operator);
		operators.put("/", type.Operator);
		operators.put("-", type.Operator); 
		operators.put(">", type.Operator);
		operators.put("<", type.Operator);
		operators.put("=", type.Operator);
		operators.put("==", type.Operator);
		operators.put("<=", type.Operator);
		operators.put(">=", type.Operator);
		operators.put("!", type.MaybeValid);
		operators.put("!=", type.Operator);
	}
	
	public boolean contains(String c){
		
		return operators.containsKey(c);
	}
	public type get(String c)
	{
		return operators.get(c);
	}
}
 class keywordHash { 
	
	Hashtable<String, type> keywords = new Hashtable<String, type>();
	
	public keywordHash(){
		keywords.put("interface", type.Keyword);
		keywords.put("void", type.Keyword);
		keywords.put("storage", type.Keyword);
		keywords.put("global", type.Keyword);
		keywords.put("storage", type.Keyword);
		keywords.put("implementation", type.Keyword);
		keywords.put("let", type.Keyword);
		keywords.put("while", type.Keyword);
		keywords.put("if", type.Keyword);
		keywords.put("return", type.Keyword);
		keywords.put("nil", type.Keyword);
		keywords.put("unsigned", type.Keyword);
		keywords.put("char", type.Keyword);
		keywords.put("short", type.Keyword);
		keywords.put("int", type.Keyword);
		keywords.put("long", type.Keyword);
		keywords.put("float", type.Keyword);
		keywords.put("double", type.Keyword);
		keywords.put("instance", type.Keyword);
		keywords.put("self", type.Keyword);
		keywords.put("new", type.Keyword);
		keywords.put("main", type.Keyword);
	}
	
	public boolean contains(String c){
		
		return keywords.containsKey(c);
	}
		public type get(String c)
	{
		return keywords.get(c);
	}
}

