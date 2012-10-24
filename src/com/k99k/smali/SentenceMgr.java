/**
 * 
 */
package com.k99k.smali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.k99k.tools.StringUtil;

/**
 * 语句管理者
 * @author keel
 *
 */
public class SentenceMgr {

	public SentenceMgr(ArrayList<String> srcLines,Methods meth) {
		this.srcLines = srcLines;
		outLines = new ArrayList<String>();
		vars = new HashMap<String, Var>();
		endVars = new HashMap<String, Var>();
		sentenceList = new ArrayList<Sentence>();
		this.meth = meth;
	}
	
	static final Logger log = Logger.getLogger(SentenceMgr.class);
	
	
	/**
	 * sentenceMap,用于定位不同的Sentence
	 */
	private static final HashMap<String,Sentence> sentenceMap = new HashMap<String, Sentence>();
	
	
	/**
	 * if结构相关语句name
	 */
	private static final HashMap<String,String> ifMap = new HashMap<String, String>();

	
	static{
		CommSentence c = new CommSentence(null, null);
		sentenceMap.put(StaticUtil.COMM, c);
		sentenceMap.put("nop", c);
		ErrSentence e = new ErrSentence(null, null);
		sentenceMap.put(ErrSentence.KEY, e);
		LocalSentence l = new LocalSentence(null,null);
		sentenceMap.put(LocalSentence.KEY, l);
//		GotoSentence gt = new GotoSentence(null,null);
//		sentenceMap.put(GotoSentence.KEY, gt);
		CastSentence cast = new CastSentence(null, null);
		sentenceMap.put(CastSentence.KEY, cast);
		
		GotoSentence gt = new GotoSentence(null, null);
		for (int i = 0; i < GotoSentence.KEYS.length; i++) {
			sentenceMap.put(GotoSentence.KEYS[i], gt);
		}
		GetSentence g = new GetSentence(null, null);
		for (int i = 0; i < GetSentence.KEYS.length; i++) {
			sentenceMap.put(GetSentence.KEYS[i], g);
		}
		InvokeSentence inv = new InvokeSentence(null,null);
		for (int i = 0; i < InvokeSentence.KEYS.length; i++) {
			sentenceMap.put(InvokeSentence.KEYS[i], inv);
		}
		ReturnSentence r = new ReturnSentence(null,null);
		for (int i = 0; i < ReturnSentence.KEYS.length; i++) {
			sentenceMap.put(ReturnSentence.KEYS[i], r);
		}
		PutSentence p = new PutSentence(null,null);
		for (int i = 0; i < PutSentence.KEYS.length; i++) {
			sentenceMap.put(PutSentence.KEYS[i], p);
		}
		ComputSentence com = new ComputSentence(null,null);
		for (int i = 0; i < ComputSentence.KEYS.length; i++) {
			sentenceMap.put(ComputSentence.KEYS[i], com);
		}
		NewSentence n = new NewSentence(null,null);
		for (int i = 0; i < NewSentence.KEYS.length; i++) {
			sentenceMap.put(NewSentence.KEYS[i], n);
		}
		MoveSentence m = new MoveSentence(null,null);
		for (int i = 0; i < MoveSentence.KEYS.length; i++) {
			sentenceMap.put(MoveSentence.KEYS[i], m);
		}
		IfSentence ifs = new IfSentence(null,null);
		for (int i = 0; i < IfSentence.KEYS.length; i++) {
			sentenceMap.put(IfSentence.KEYS[i], ifs);
		}
		TagSentence t = new TagSentence(null,null);
		for (int i = 0; i < TagSentence.KEYS.length; i++) {
			sentenceMap.put(TagSentence.KEYS[i], t);
		}
		GotoTagSentence gotag = new GotoTagSentence(null,null);
		for (int i = 0; i < GotoTagSentence.KEYS.length; i++) {
			sentenceMap.put(GotoTagSentence.KEYS[i], gotag);
		}
		VarSentence v = new VarSentence(null, null);
		for (int i = 0; i < VarSentence.KEYS.length; i++) {
			sentenceMap.put(VarSentence.KEYS[i], v);
		}
		
		ArraySentence ar = new ArraySentence(null, null);
		for (int i = 0; i < ArraySentence.KEYS.length; i++) {
			sentenceMap.put(ArraySentence.KEYS[i], ar);
		}
		SwitchSentence ss = new SwitchSentence(null, null);
		for (int i = 0; i < SwitchSentence.KEYS.length; i++) {
			sentenceMap.put(SwitchSentence.KEYS[i], ss);
		}
		
		TrySentence tr = new TrySentence(null, null);
		for (int i = 0; i < TrySentence.KEYS.length; i++) {
			sentenceMap.put(TrySentence.KEYS[i], tr);
		}
		
		OtherSentence other = new OtherSentence(null, null);
		for (int i = 0; i < OtherSentence.KEYS.length; i++) {
			sentenceMap.put(OtherSentence.KEYS[i], other);
		}
		//-------------------
		//if结构语句name
		ifMap.put("if", "if");
		ifMap.put("tag", "tag");
		ifMap.put("goto", "goto");
		ifMap.put("return", "return");
		
	}
	
	public final Sentence createSentence(String key,String line){
		if (!sentenceMap.containsKey(key)) {
			return null;
		}
		return sentenceMap.get(key).newOne(this, line);
	}
	
	/**
	 * 方法参数集
	 */
	private Methods meth;
	
	
	/**
	 * 当前处理行数
	 */
	private int cNum = 0;
	
	/**
	 * 最大行数
	 */
	private int maxNum = 0;
	
	/**
	 * 缩进
	 */
	private int level = 0;
	
	/**
	 * 是否静态方法
	 */
	private boolean isStatic = false;
	
	/**
	 * 准备输出的行
	 */
	private ArrayList<String> outLines;
	
	/**
	 * 原始行
	 */
	private ArrayList<String> srcLines;
	
	/**
	 * 变量
	 */
	private HashMap<String,Var> vars;
	
	/**
	 * 保存结束的变量以便restart取出
	 */
	private HashMap<String,Var> endVars;
	
	/**
	 * 匹配上的Sentence集合,按顺序排
	 */
	private ArrayList<Sentence> sentenceList;

	/**
	 * 结构语句中的标记集合,用于快递定位指定的tag
	 */
	private HashMap<String,Sentence> tags = new HashMap<String, Sentence>();
	
	/**
	 * 是否包含if结构
	 */
	private boolean hasIF = false;;
	
	private boolean hasSwitch = false;
	
	private boolean hasTry = false;
	/**
	 * 处理原始语句集
	 */
	public void execLines(){
		
		this.setProps();
		
		this.parse();
		
		this.render();
		
	}

	public void debug(){
		this.setProps();
		
		maxNum = this.srcLines.size();
		int javaLine = -1;
		while (cNum < maxNum) {
			String l = this.srcLines.get(cNum);
			int jaLine = this.javaLineNum(l);
			if (jaLine >=0 && jaLine != javaLine) {
				javaLine = jaLine;
				cNum++;
				l = this.srcLines.get(cNum);
			}
			String key = Tool.getKey(l);
			Sentence s = this.createSentence(key, l);
			if (s == null) {
				//未知key的处理
				Sentence e = new CommSentence(this, "#ERR: unknown sententce. line:"+l);
				e.debug();
				this.sentenceList.add(e);
				cNum++;
				continue;
			}
			s.setLevel(this.level);
			s.setLineNum(cNum);
			if (javaLine > -1) {
				s.setJavaLineNum(javaLine);
			}
			//对数组赋值行特殊处理
			if (key.equals(".array-data")) {
				cNum++;
				l = this.srcLines.get(cNum);
				while(!l.startsWith(".end")){
					ArraySentence as = (ArraySentence)s;
					as.addToArrMatrix(l);
					cNum++;
					l = this.srcLines.get(cNum);
				}
				s.debug();
			}
			//对switch行特殊处理
			else if (key.equals(".packed-switch") || key.equals(".sparse-switch")) {
				cNum++;
				hasSwitch = true;
				l = this.srcLines.get(cNum);
				while(!l.startsWith(".end")){
					SwitchSentence as = (SwitchSentence)s;
					as.addSwitchKey(l);
					cNum++;
					l = this.srcLines.get(cNum);
				}
				s.debug();
			}
			//其他语句
			else if (s.debug()) {
				//成功处理语句后加入语句列表
				//只有能成行输出的操作加入到sentenceList
				if (s.getType()>Sentence.TYPE_NOT_LINE) {
					this.sentenceList.add(s);
				}
			}else{
				Sentence e = new CommSentence(this, "#ERR: unknown sententce. line:"+l);
				e.debug();
				this.sentenceList.add(e);
				//后面语句不处理了
				break;
			}
			cNum++;
		}
		
		this.render();
		
	}
	
	
	
	
	
	/**
	 * 从sentenceList生成输出的outLines
	 */
	public void render(){
		//处理sentenceList,将处于非over状态的sentence进行处理,直到全部处理完成
		
		//输出时可控制缩进以及检查末尾的;号
		Iterator<Sentence> it = this.sentenceList.iterator();
		while (it.hasNext()) {
			Sentence s = it.next();
			if (s.state == Sentence.STATE_OVER) {
				if (s.getType() == Sentence.TYPE_LINE) {
					this.outLines.add(StaticUtil.TABS[s.getLevel()]+s.getOut()+";");
				}
				else if(s.getType() == Sentence.TYPE_STRUCT){
					String ostr = StaticUtil.TABS[s.getLevel()]+s.getOut();
					this.outLines.add(ostr);
				}
			}else{
				log.warn(this.getMeth().getName()+" - [Sentence not over] ["+s.getLineNum()+"] "+s.getLine());
			}
		}
	}

	/**
	 * 处理每行，并加入到sentenceList
	 */
	public void parse(){
		maxNum = this.srcLines.size();
		int javaLine = -1;
		//包含所有语句的allSenList
		ArrayList<Sentence> allSenList = new ArrayList<Sentence>();
		while (cNum < maxNum) {
			String l = this.srcLines.get(cNum);
			int jaLine = this.javaLineNum(l);
			if (jaLine >=0 && jaLine != javaLine) {
				javaLine = jaLine;
				cNum++;
				l = this.srcLines.get(cNum);
			}
			String key = Tool.getKey(l);
			Sentence s = this.createSentence(key, l);
			if (s == null) {
				//未知key的处理
				Sentence e = new CommSentence(this, "#ERR: unknown sententce. line:"+l);
				e.exec();
				this.sentenceList.add(e);
				allSenList.add(e);
				cNum++;
				continue;
			}
			s.setLevel(this.level);
			s.setLineNum(cNum);
			if (javaLine > -1) {
				s.setJavaLineNum(javaLine);
			}
			//对数组赋值行特殊处理
			if (key.equals(".array-data")) {
				cNum++;
				l = this.srcLines.get(cNum);
				while(!l.startsWith(".end")){
					ArraySentence as = (ArraySentence)s;
					as.addToArrMatrix(l);
					cNum++;
					l = this.srcLines.get(cNum);
				}
				s.exec();
			}
			//对switch行特殊处理
			else if (key.equals(".packed-switch") || key.equals(".sparse-switch")) {
				cNum++;
				hasSwitch = true;
				l = this.srcLines.get(cNum);
				while(!l.startsWith(".end")){
					SwitchSentence as = (SwitchSentence)s;
					as.addSwitchKey(l);
					cNum++;
					l = this.srcLines.get(cNum);
				}
				s.exec();
			}
			//其他语句
			else if (s.exec()) {
				//成功处理语句后加入语句列表
				//只有能成行输出的操作加入到sentenceList
				if (s.getType()>Sentence.TYPE_NOT_LINE) {
					this.sentenceList.add(s);
				}
			}else{
				Sentence e = new CommSentence(this, "#ERR: unknown sententce. line:"+l);
				e.exec();
				this.sentenceList.add(e);
				//后面语句不处理了
				break;
			}
			allSenList.add(s);
			cNum++;
		}
		//处理return
		if(!this.doReturn(allSenList)){
			return;
		}
		
		//处理switch
		if (hasSwitch) {
			SwitchScan ss = new SwitchScan(this, this.sentenceList);
			ss.scan();
		}
		//处理IFScan
		if (hasIF) {
			IFStructScan ifs = new IFStructScan(this,this.sentenceList);
			ifs.scan();
		}
		//处理try catch
		if (hasTry) {
			TryCatchScan ts = new TryCatchScan(this, this.sentenceList);
			ts.scan();
		}
	}
	
	/**
	 * 处理return语句
	 * @param allSenList
	 */
	private boolean doReturn(ArrayList<Sentence> allSenList){
		ReturnSentence returnSen = null;
		int returnIndex = -1;
		
		for (int i = 0; i < allSenList.size(); i++) {
			Sentence s = allSenList.get(i);
			if (s.getName().equals("return")) {
				returnSen = (ReturnSentence) s;
				returnIndex = i;
				break;
			}
		}
		//处理return 的对象语句
		String reStr = this.getMeth().getReturnStr();
		reStr = (reStr.equals("")) ? "void" : reStr;
		if (!reStr.equals("void")) {
			GotoTagSentence gtReturnTag = null;
			int lastBeforeReturnIndex = -1;
			//先定位return句之 上的gotoTag
			for (int i = returnIndex-1; i >= 0; i--) {
				Sentence s = allSenList.get(i);
				if (s.getName().equals("gotoTag")) {
					gtReturnTag = (GotoTagSentence) s;
				}else if(s.getName().equals("tag") || s.getName().equals("switch")){
					continue;
				}else{
					lastBeforeReturnIndex = i;
					break;
				}
			}
			String returnKey = returnSen.getReturnKey();
			if (gtReturnTag != null) {
				String gotoReturnTag = gtReturnTag.getTag();
				for (int i = 0; i < allSenList.size(); i++) {
					Sentence s = allSenList.get(i);
					if (s.getName().equals("goto")) {
						GotoSentence gt = (GotoSentence) s;
						if (gt.getTarget().equals(gotoReturnTag)) {
							//处理goto之前的赋值语句，使之变成return
							Sentence rs = allSenList.get(i-1);
							if (!this.defineReturn(rs, returnKey, gt)) {
								return false;
							}
							gt.setReturn(true);
						}
	
					}
				}
			}
			//处理return之前的语句
			Sentence ls = allSenList.get(lastBeforeReturnIndex);
			Sentence gt = ls;
			if (ls.type == Sentence.TYPE_NOT_LINE) {
				for (int i = lastBeforeReturnIndex; i > 0; i--) {
					Sentence s = allSenList.get(i);
					if (!s.getName().equals("tag") && !s.getName().equals("gotoTag") && !s.getName().equals("switch")) {
						gt = allSenList.get(i+1);
						break;
					}
				}
			}
			if (!this.defineReturn(ls, returnKey, gt)) {
				//这里不返回false
//				return false;
			}else{
				//原return语句可不显示
				Var rv = returnSen.getVar();
				if (rv == null) {
					returnSen.setOut("//"+returnSen.getOut()+";");
				}else{
					returnSen.setOut("//return "+rv.getOut()+";");
				}
			}
		}
		return true;
	}
	
	/**
	 * 处理goto之前的赋值语句，使之变成return
	 * @param rs
	 * @param returnKey
	 * @param gt
	 * @return
	 */
	private boolean defineReturn(Sentence rs,String returnKey,Sentence gt){
//		String returnKey = (reKey.equals("")) ? "void" : reKey;
		Var v = rs.getVar();
		if (v == null) {
			log.error(this.getMeth().getName()+" goto return pre sen can't getVar.!!!!!!!!!!!!");
			return false;
		}
		if (v.getName()==null || !v.getName().equals(returnKey)) {
			//这表示goto仅起到转向的作用，并不影响return
			log.error(this.getMeth().getName()+" goto return pre sen getVar() can't match returnKey.!!!!!!!!!!!!");
			return true;
		}
		String outs = ((v.getOut().equals("0") || v.getOut().equals("1")) ? (Var.checkIout(this.meth.getReturnStr(), v.getOut()) +" /* " + v.getOut() +" */") : v.getOut());
		if (rs.getName().equals("invoke") || rs.getLine().startsWith("move-result")) {
			rs.setOut("return "+ outs);
		}else{
			rs.appendOut(";"+StaticUtil.NEWLINE+StaticUtil.TABS[rs.level]+"return "+outs);
		}
		if (rs.getName().equals("get")) {
			rs.over();
		}
		if (rs.getType() == Sentence.TYPE_NOT_LINE) {
			rs.over();
			rs.type = Sentence.TYPE_LINE;
			int po = this.sentenceList.indexOf(gt);
			if (po<0) {
				log.error(this.getMeth().getName()+" pre return sen can't find insert po.!!!!!!!!!!!!");
				return false;
			}
			this.sentenceList.add(po, rs);
		}
		return true;
	}
	
	/**
	 * Sentence出错时的处理
	 * @param srcSen 原Sentence
	 */
	public final void err(Sentence srcSen ){
		Sentence s = this.createSentence(ErrSentence.KEY, srcSen.getOut());
		s.setLineNum(srcSen.getLineNum());
		s.setJavaLineNum(srcSen.getJavaLineNum());
		s.setLevel(srcSen.getLevel());
		s.exec();
		this.sentenceList.add(s);
	}
	
//	/**
//	 * 获取SentenceList中的Sentence
//	 * @param index 索引
//	 * @return
//	 */
//	public final Sentence getSentence(int index){
//		return this.sentenceList.get(index);
//	}
	
	public final void addTag(TagSentence tsen){
		tags.put(tsen.getTag(), tsen);
	}
	
	/**
	 * @param hasTry the hasTry to set
	 */
	public final void setHasTry(boolean hasTry) {
		this.hasTry = hasTry;
	}





	public final TagSentence getTag(String tag){
		return (TagSentence) tags.get(tag);
	}
	
	public final int indexOfSentence(Sentence sen){
		return this.sentenceList.indexOf(sen);
	}
	
	/**
	 * 根据行号查找在sentenceList中的index
	 * @param lineNum
	 * @return
	 */
	public final int findSentenceIndexByLineNum(int lineNum){
		int len = this.sentenceList.size();
		if (len<1) {
			return -1;
		}
		for (int i = len-1; i >= 0; i--) {
			Sentence s = this.sentenceList.get(i);
			if (s.getLineNum() == lineNum) {
				return this.sentenceList.indexOf(s);
			}
		}
		return -1;
	}
	
	public final Sentence findSentenceByIndex(int index){
		return this.sentenceList.get(index);
	}
	
	public final String getSrcline(int index){
		return this.srcLines.get(index);
	}
	
	/**
	 * 返回指定tag在sentenceList中的位置
	 * @param tag String
	 * @return 未找到则返回-1
	 */
	public final int findTagIndex(String tag){
		if (!tags.containsKey(tag)) {
			return -1;
		}
		return this.sentenceList.indexOf(tags.get(tag));
	}
	
	/**
	 * 获取SentenceList中的最后一个Sentence
	 * @return 
	 */
	public final Sentence getLastSentence(){
		int len = this.sentenceList.size();
		if (len<1) {
			return null;
		}
		return this.sentenceList.get(len -1);
	}
	
	/**
	 * 获取SentenceList中的倒数第skip个Sentence
	 * @return
	 */
	public final Sentence getLastSentence(int skip){
		int len = this.sentenceList.size();
		if (len<(1+skip)) {
			return null;
		}
		return this.sentenceList.get(len -1-skip);
	}
	
	/**
	 * 查找上一个匹配 name的Sentence
	 * @param senName
	 * @return
	 */
	public final Sentence findLastSentence(String senName){
		int len = this.sentenceList.size();
		if (len<1) {
			return null;
		}
		for (int i = len-1; i >= 0; i--) {
			Sentence s = this.sentenceList.get(i);
			if (s.getName().equals(senName)) {
				return s;
			}
		}
		return null;
	}
	
//	/**
//	 * 查找上一个匹配 name的Sentence
//	 * @param senName
//	 * @param fromIndex 从某一个index向上找
//	 * @return
//	 */
//	public final Sentence findLastSentence(String senName,int fromIndex){
//		int len = this.sentenceList.size();
//		if (len<1 || fromIndex>=len) {
//			return null;
//		}
//		for (int i = fromIndex; i >= 0; i--) {
//			Sentence s = this.sentenceList.get(i);
//			if (s.getName().equals(senName)) {
//				return s;
//			}
//		}
//		return null;
//	}
	
	/**
	 * 向前查找标签
	 * @param tagName
	 * @param lineNum
	 * @return
	 */
	public final Sentence findLastTag(String tagName,int lineNum){
		int len = this.sentenceList.size();
		if (len<1|| lineNum<=0 || lineNum>=len) {
			return null;
		}
		for (int i = lineNum-1; i >= 0; i--) {
			Sentence s = this.sentenceList.get(i);
			if (s.getName().equals("tag")) {
				TagSentence tag = (TagSentence)s;
				if (tag.getTag().equals(tagName)) {
					return s;
				}
			}
		}
		return null;
	}
	
	/**
	 * 查找上一个匹配 name的Sentence
	 * @param senName
	 * @param fromIndex 从某一个index向上找
	 * @return
	 */
	public final Sentence findLastSentence(String senName,int fromIndex){
		int len = this.sentenceList.size();
		if (len<1 || fromIndex<=0 || fromIndex>=len) {
			return null;
		}
		for (int i = fromIndex-1; i >= 0; i--) {
			Sentence s = this.sentenceList.get(i);
			if (s.getName().equals(senName)) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * 从某一位置查找IF结构语句
	 * @param isBack 查找方向是否是反向
	 * @param lineNum 从哪个行号开始
	 * @return Sentence
	 */
	public final Sentence findIFSentence(boolean isBack,int lineNum){
		int len = this.sentenceList.size();
		if (len<1 || lineNum<=0 || lineNum>=len) {
			return null;
		}
		if (isBack) {
			for (int i = lineNum-1; i >= 0; i--) {
				Sentence s = this.sentenceList.get(i);
				if (s.getLineNum() < lineNum && ifMap.containsKey(s.getName())) {
					return s;
				}
			}
		}else{
			for (int i = lineNum+1; i < len-1; i++) {
				Sentence s = this.sentenceList.get(i);
				if (s.getLineNum() > lineNum && ifMap.containsKey(s.getName())) {
					return s;
				}
			}
		}
		return null;
	}
	
	/**
	 * 是否为if结构语句
	 * @param senName
	 * @return
	 */
	public static final boolean isIFS(String senName){
		return ifMap.containsKey(senName);
	}

	/**
	 * 获取变量 
	 * @param key
	 * @return
	 */
	public final Var getVar(String key){
		return vars.get(key);
	}
	
	/**
	 * 结束某个变量
	 * @param varName
	 */
	public final void endVar(String varName){
		Var v = this.vars.get(varName);
		if (v != null && v.getSen()!= null && v.getSen().getName().equals("local")) {
			this.vars.remove(varName);
			this.endVars.put(varName, v.cloneVar());
		}
	}
	
	/**
	 * 从结束的变量集中查找var,主要用于方法参数p0,p1...
	 * @param varName
	 * @return
	 */
	public final Var getVarFromEndVars(String varName){
		return this.endVars.get(varName);
	}
	
	/**
	 * 重新开始某个变量
	 * @param varName
	 * @return
	 */
	public final Var restartVar(String varName){
		Var v = this.endVars.get(varName);
		if (v != null) {
			this.vars.put(varName, v.cloneVar());
		}else{
			log.error(this.getMeth().getName()+" restartVar can't find "+varName);
		}
		return v;
	}
	
	/**
	 * 移除某个已处理的Sentence
	 * @param sen
	 */
	public final void removeSentence(Sentence sen){
		this.sentenceList.remove(sen);
	}
	/**
	 * 设置p0和方法参数的Var，根据是否静态方法有所不同
	 * @param sen Sentence
	 * @param key 用于设置Var的key
	 * @return Var
	 */
	public final void setProps(){
		ArrayList<String> props = this.meth.getMethProps();
		int len = props.size();
		int start = 1;
		if (isStatic) {
			start = 0;
		}else{
			Var v = new Var(null);
			String c = this.getMeth().getClassName();
			v.setClassName(c);
			v.setKey("");
			v.setName("p0");
			v.setOut("this");
			v.setValue("this");
			vars.put("p0", v);
		}
		
		for (int i = 0; i < len; i++) {
			String vs = props.get(i);
			String[] ws = vs.split(" ");
			Var v = new Var(null);
			v.setClassName(ws[0]);
			v.setKey("");
			v.setName("p"+start);
			v.setValue(ws[1]);
			v.setOut(ws[1]);
			vars.put(v.getName(), v);
			this.endVars.put(v.getName(), v.cloneVar());
			if (ws[0].equals("long") || ws[0].equals("double")) {
				//这两种需要占据两个参数
				start++;
				Var nv = v.cloneVar();
				nv.setName("p"+start);
				vars.put(nv.getName(), nv);
				this.endVars.put(v.getName(), v.cloneVar());
			}
			start++;
		}
		
	}
	
	/**
	 * 设置变量
	 * @param key
	 * @param line
	 */
	public final void setVar(Var var){
		vars.put(var.getName(), var);
	}
	
	/**
	 * 去除变量 
	 * @param varName
	 */
	public final void removeVar(String varName){
		this.vars.remove(varName);
	}
	
	/**
	 * @return the hasIF
	 */
	public final boolean isHasIF() {
		return hasIF;
	}





	/**
	 * @param hasIF the hasIF to set
	 */
	public final void setHasIF(boolean hasIF) {
		this.hasIF = hasIF;
	}





	/**
	 * 处理行号
	 * @param line
	 */
	public int javaLineNum(String line){
		String[] words = line.split(" ");
		if (words[0].equals(StaticUtil.TYPE_LINE) && words.length >= 2 && StringUtil.isDigits(words[1])) {
			return Integer.parseInt(words[1]);
		}
		return -1;
	}
	
	/**
	 * @return Methods
	 */
	public final Methods getMeth(){
		return this.meth;
	}
	
	/**
	 * 增加或减少缩进
	 * @param add
	 */
	public final void addLevel(int add){
		this.level = this.level + add;
	}

	/**
	 * 增加或减少当前行
	 * @param add
	 */
	public final void addCNum(int add){
		this.cNum = this.cNum + add;
	}
	
	/**
	 * @return the outLines
	 */
	public final ArrayList<String> getOutLines() {
		return outLines;
	}

	/**
	 * @return the cNum
	 */
	public final int getcNum() {
		return cNum;
	}
	

	/**
	 * @return the maxNum
	 */
	public final int getMaxNum() {
		return maxNum;
	}

	/**
	 * @return the isStatic
	 */
	public final boolean isStatic() {
		return isStatic;
	}

	/**
	 * @param isStatic the isStatic to set
	 */
	public final void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	
	/**
	 * @return the level
	 */
	public final int getLevel() {
		return level;
	}

	
}
