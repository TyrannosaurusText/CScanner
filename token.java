
class token {

	int lineNum;
	int columnNum;
	type toktype;
	String content;
		
	public token(int i, int j, type t, String c){
		lineNum = i;
		columnNum = j;
		toktype = t;
		content = c;
	}
	
	public void debugPrint(){
		if(content.length() > 0)
		System.out.format( "@%4d,%4d%14s \"%-1s\"\n",lineNum, columnNum, toktype.name(), content);
		else
		System.out.format( "@%4d,%4d%14s \"\"\n",lineNum, columnNum, toktype.name());
	}
	public boolean Compare(token rhs)
	{
		if(this.toktype == rhs.toktype)
		return true;
		return false;
	}
	
	public void assign(int i, int j, type t, String c)
	{
		lineNum = i;
		columnNum = j;
		toktype = t;
		content = c;
	}
	public void append(char c)
	{
		if(content.length() > 0)
		content = content.concat(Character.toString(c));
		else
			content = Character.toString(c);
	}
	public void clear()
	{
		content = "";
		toktype = type.None;
	}	
	public token clone()
	{
		return new token(lineNum, columnNum, toktype, content);
	}
}