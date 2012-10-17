/**
 * 
 */
package com.k99k.smali;

import org.apache.log4j.Logger;

/**
 * Move语句
 * @author keel
 *
 */
public class MoveSentence extends Sentence {

	/**
	 * @param mgr
	 * @param line
	 */
	public MoveSentence(SentenceMgr mgr, String line) {
		super(mgr, line);
		this.type = Sentence.TYPE_NOT_LINE;
	}
	static final Logger log = Logger.getLogger(MoveSentence.class);
	
	/* (non-Javadoc)
	 * @see com.k99k.smali.Sentence#exec()
	 */
	@Override
	public boolean exec() {
		this.doComm(this.line);
		this.line = this.line.replaceAll(",", "");
		String[] ws = this.line.split(" ");
		if (ws.length<2) {
			this.out.append("exec move error. line:").append(this.line);
			this.mgr.err(this);
			log.error(this.mgr.getMeth().getName()+" moveERR:"+this.line);
			return false;
		}
		String key = ws[0];
		if (key.startsWith("move-result")) {
			Sentence last = null;
			//向上定位到非STRUCT句
			for(int i=0;i<this.lineNum;i++){
				last = this.mgr.getLastSentence(i);
				if (last == null) {
					this.out.append("exec move error. last sentence is null. line:").append(this.line);
					this.mgr.err(this);
					log.error(this.mgr.getMeth().getName()+" moveERR:"+this.line);
					return false;
				}else if(last.getType() == Sentence.TYPE_STRUCT){
					continue;
				}
				if (last.getVar()==null ) {
					this.out.append("exec move error. last sentence's var is null. line:").append(this.line);
					this.mgr.err(this);
					log.error(this.mgr.getMeth().getName()+" moveERR:"+this.line);
					return false;
				}else{
					break;
				}
			}
			
			//使用前一个invoke语句生成的Var,改变其name后加入到Var集合
			Var v = last.getVar();
			String name = ws[1];
			v.setName(name);
//			v.setOutVar(true);
			this.mgr.setVar(v);
			this.var = v;
			this.setOut(v.getOut());
			//将last语句显示去掉
			last.setType(Sentence.TYPE_NOT_LINE);
		}else if(key.equals("move-exception")){
			//不处理,已由try catch处理
			
		}else{
			//move变量,判断是否需要clone
//			Var v1 =  this.mgr.getVar(ws[2]).cloneVar();
			Var v1 =  this.mgr.getVar(ws[2]);
			Var v2 =  this.mgr.getVar(ws[1]);
			v2.setName(ws[1]);
			this.mgr.setVar(v2);
			this.var = v2;
			this.setOut(v2.getOut()+" = "+ v1.getOut());
			this.type = Sentence.TYPE_LINE;
		}
		
		this.over();
		return true;
	}
	
	private Var var = new Var(this);
	/* (non-Javadoc)
	 * @see com.k99k.smali.Sentence#getVar()
	 */
	@Override
	public Var getVar() {
		return this.var;
	}

	/* (non-Javadoc)
	 * @see com.k99k.smali.Sentence#newOne(com.k99k.smali.SentenceMgr, java.lang.String)
	 */
	@Override
	public Sentence newOne(SentenceMgr mgr, String line) {
		return new MoveSentence(mgr, line);
	}

	/* (non-Javadoc)
	 * @see com.k99k.smali.Sentence#getName()
	 */
	@Override
	public String getName() {
		return "move";
	}
	
	static final String[] KEYS = new String[]{
		"move", 
		"move/from16",
		"move/16",
		"move-wide",
		"move-wide/from16",
		"move-wide/16",
		"move-object",
		"move-object/from16", 
		"move-object/16",
		"move-result",
		"move-result-wide",
		"move-result-object",
		"move-exception"
	};
}
