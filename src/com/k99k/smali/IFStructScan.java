/**
 * 
 */
package com.k99k.smali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.k99k.tools.StringUtil;

/**
 * if结构扫描器
 * @author keel
 *
 */
public class IFStructScan {

	public IFStructScan(SentenceMgr mgr,ArrayList<Sentence> senList) {
		this.mgr = mgr;
		this.senList = senList;
		this.len = this.senList.size();
	}
	static final Logger log = Logger.getLogger(IFStructScan.class);
	
	private SentenceMgr mgr;
	
	private ArrayList<Sentence> senList;
	
	private int len;
	
	private HashMap<String,TagSentence> condMap = new HashMap<String, TagSentence>();
	
//	private ArrayList<Sentence> ifsList = new ArrayList<Sentence>();
	
//	/**
//	 * 某一内容块相关的if语句集
//	 */
//	private ArrayList<Sentence> ifls = new ArrayList<Sentence>();

//	/**
//	 * 是否已包含内容块
//	 */
//	private boolean contentIn = false;
	
//	/**
//	 * 内容块反向开始的lineNum,注意是反向开始,数值>contStart
//	 */
//	private int contStart = -1;
//	/**
//	 * 内容块反向结束的lineNum,注意是反向结束,数值<contStart
//	 */
//	private int contEnd = -1;
//	/**
//	 * return开始行所在的senList中的index,保存此位置以在return后置后识别后置前if的真正endSen
//	 */
//	private int returnLineNum = -1;
	
	/**
	 * return语句
	 */
	private ReturnSentence returnSentence;
	
//	/**
//	 * return 所在的index
//	 */
//	private int returnIndex = -1;
	
//	/**
//	 * 本语句中最大的lineNum
//	 */
//	private int maxLineNum = -1;
//	/**
//	 * contStart 所在Sen在senList中的index,注意是内容块下方结束位置
//	 */
//	private int contStartIndex = -1;
//	
//	/**
//	 * contEnd所在Sen在senList中的index,注意是内容块上方结束位置
//	 */
//	private int contEndIndex = -1;
	/**
	 * 每个if内容块内结束标记map,用于判断是否为else
	 */
	private HashMap<String,IfSentence> ifEndMap = new HashMap<String, IfSentence>();
	
	/**
	 * 判断是否是结构语句,返回false为内容,true为未处理的结构
	 * @param sen Sentence
	 * @return 返回false为内容,true为未处理的结构
	 */
	public static final boolean isStructSen(Sentence sen){
		if (sen.getType() != Sentence.TYPE_STRUCT) {
			return false;
		}else if(sen.getState() == Sentence.STATE_OVER){
			return false;
		}
//		else if(sen.getOut().trim().startsWith("//")){
//			return false;
//		}
		return true;
	}
	
	/**
	 * 扫描并处理if结构
	 * @param senList
	 */
	public void scan(){
		if(!this.init()){
			return;
		}
		
		try {
			this.setIfContendEnd();
			
			this.shiftIfBlock();
			
			this.finishIf();
			
		} catch (Exception e) {
			log.error(this.mgr.getMeth().getName()+" ERR:"+e.getStackTrace()[0]);
			e.printStackTrace();
		}
		
		
		log.debug("IfScan end:"+this.mgr.getMeth().getName());
	}
	
	
	
	
	
	private void finishIf(){
		int level = 0;
		IfSentence ifs = new IfSentence(this.mgr, "");
		level = reCheckIfBlock(ifs,-1,this.senList.size(),level,false);
		log.debug(this.mgr.getMeth().getName()+" level:"+level);
	}
	
	/**
	 * 重置if块中的if状态为doing，为重新merge作准备
	 * @param ifStartIndex
	 */
	private int[] reMergePrepare(int ifStartIndex){
		int[] re = this.defineIfBlock(ifStartIndex);
		for (int i = ifStartIndex; i <= re[1]; i++) {
			Sentence s = this.senList.get(i);
			if (s.getName().equals("if")) {
				IfSentence ifs = (IfSentence)s;
				ifs.setState(Sentence.STATE_DOING);
				ifs.setReversed(false);
			}
		}
		return re;
	}
	
	
	private int reCheckIfBlock(IfSentence outIf,int outStart,int outEnd,int level,boolean isDoWhile){
		for (int i = outStart+1; i < outEnd; i++) {
			Sentence s = this.senList.get(i);
			s.setLevel(level);
//			//保存外部if的endSenLineNum,以便内部的if做修改
//			int outIfEndLn = (outIf.getLineNum() > -1) ? outIf.getEndSenLineNum() : -1;
			if (s.getName().equals("if") && s.getState() == Sentence.STATE_DOING) {
				//内部,level++
				level++;
				IfSentence ifs = (IfSentence)s;
				TagSentence tag = ifs.getCondTag();
				int tagIndex = this.senList.indexOf(tag);
				if (outIf.isWhile()) {
					//while处理
					if (tagIndex < outStart) {
						//do while处理,注意这里通过判断外部if结束句是否在本if条件句以下确定是否是doWhile
						boolean reallyDoWhile = outEnd < i;
						int end = outEnd;
						if (reallyDoWhile) {
							end = tagIndex;
							//误认的while变为if
							if (this.senList.get(tagIndex+1).getName().equals("if")) {
								IfSentence iff = (IfSentence)this.senList.get(tagIndex+1);
								iff.setAsIf();
								//需要重新mergeCond
								int[] re = this.reMergePrepare(tagIndex+1);
								this.mergeConds(tagIndex+1, re, this.senList.get(re[0]).getLineNum()+1F);
								iff.over();
							}
							tag.setOut("do {");
							ifs.setDoWhile();
						}
						level = this.reCheckIfBlock(ifs, i, end,level,reallyDoWhile);
					}else{
						level = this.reCheckIfBlock(ifs, i, tagIndex,level,false);
					}
				}else{
					//if处理
					if (tagIndex < outEnd) {
						if (tagIndex < i) {
							//真正的do while
							if (this.senList.get(tagIndex+1).getName().equals("if")) {
								IfSentence iff = (IfSentence)this.senList.get(tagIndex+1);
								iff.setAsIf();
								// 误认的while变为if,需要重新mergeCond
								int[] re = this.reMergePrepare(tagIndex+1);
								this.mergeConds(tagIndex+1, re, this.senList.get(re[0]).getLineNum()+1F);
								iff.over();
							}
							tag.setOut("do {");
							ifs.setDoWhile();
							level = this.reCheckIfBlock(ifs, i, tagIndex,level,true);
						}else{
							level = this.reCheckIfBlock(ifs, i, tagIndex,level,false);
						}
					}else{
						//补一个endStruct在outEnd上
						if (this.senList.size() != outEnd) {
//							((TagSentence)this.senList.get(outEnd)).setEndStruct().over(); //不需要在此处补,结束时会自动补
							level = this.reCheckIfBlock(ifs, i, outEnd,level,false);
						}else{
							log.error(this.mgr.getMeth().getName()+" Can't setEndStruct in the end of senList. ");
						}
					}
				}
				//判断是否else
				if (!ifs.isWhile()) {
					if (this.ifEndMap.containsKey(level+"#"+ifs.getEndSenLineNum())) {
						ifs.setElse(true);
					}else{
						this.ifEndMap.put(level+"#"+ifs.getEndSenLineNum(), ifs);
					}
					//更新外部if的endSenLn
					if (outIf.getLineNum() > -1 && ifs.getEndSenLineNum() > outIf.getEndSenLineNum()) {
						//FIXME 内部if中包含continue与break时不应该这样处理 
						outIf.setEndSenLineNum(ifs.getEndSenLineNum());
					}
				}
			}else if(s.getName().equals("goto")){
				GotoSentence gt = (GotoSentence)s;
				GotoTagSentence gtTag = (GotoTagSentence) gt.getTargetSen();
				int gtTagIndex = this.senList.indexOf(gtTag);
				//FIXME 如果是嵌套多层if外部才是while的情况呢？
				if (outIf.isWhile()) {
					//while中的转向句,return 已经处理过
					if (!gt.isReturn()) {
						if (!isDoWhile) {
							if (gtTagIndex == outStart) {
								gt.setContinue(null, "");
							}else if(gtTagIndex < outStart){
								gt.setContinue(null, gtTag.getLine());
							}else if(gtTagIndex > outEnd){
								gt.setBreak(null, gtTag.getLine());
							}
							
						}else{
							if (gtTagIndex == outEnd) {
								gt.setContinue(null, "do while continue");
							}else if(gtTagIndex < outStart){
								gt.setContinue("label", "do while continue: "+gtTag.getLine());
							}else if(gtTagIndex > outEnd){
								gt.setBreak(null, "do while break: "+gtTag.getLine());
							}
						}
					}
					
				}else{
					//if块内的goto
					if (!gt.isReturn()) {
						//其他gt应该下方就是其gtTag，无需处理
					}
				}
				gt.over();
				gtTag.over();
				
			}
			//TODO 未在else块内做level定义
//			else if(s.getName().equals("tag") && ((TagSentence)s).isLastElseStart()){
//				level++;
//			}
		}
		if (this.senList.size() != outEnd) {
			TagSentence endTag = (TagSentence)this.senList.get(outEnd);
			if (endTag.getOut().startsWith("do")) {
				endTag.over();
			}else{
				endTag.setEndStruct().over();
			}
			outIf.over();
	
			//清除内部的ifEnd
			String[] removeKeys = new String[this.ifEndMap.size()];
			Iterator<Entry<String, IfSentence>> it = this.ifEndMap.entrySet().iterator();
			int j = 0;
			while (it.hasNext()) {
				Entry<String, IfSentence> en = it.next();
				String key = en.getKey();
				String[] ar = key.split("#");
				int le = Integer.parseInt(ar[0]);
				if (le >= level) {
					removeKeys[j] = key;
					j++;
				}
			}
			for (int i = 0; i < removeKeys.length; i++) {
				if (StringUtil.isStringWithLen(removeKeys[i], 1)) {
					this.ifEndMap.remove(removeKeys[i]);
				}
			}
			level--;
		}
		return level;
	}

	
	
	/**
	 * 在移动语句之前扫描所有if的内容块结束位置
	 */
	private void setIfContendEnd(){
		for (int i = 0; i < this.senList.size(); i++) {
			Sentence s = this.senList.get(i);
			if (s.getName().equals("if") && s.getState() == Sentence.STATE_DOING) {
				IfSentence ifs = (IfSentence)s;
				int[] re = this.defineIfBlock(i);
				int endLN = this.findIfEndLineNum(ifs, re[1]+1);
				//把这个if块内所有的if的EndSenLineNum都指向endLN
				for (int j = i; j <= re[1]; j++) {
					Sentence s1 = this.senList.get(j);
					if (s1.getName().equals("if")) {
						IfSentence if2 = (IfSentence)s1;
						if2.setEndSenLineNum(endLN);
						//单独处理倒置cond的if
						if (if2.getCondTag().getLineNum() < if2.getLineNum()) {
							Sentence afterCondSen = this.senList.get(this.senList.indexOf(if2.getCondTag())+1);
							if (afterCondSen.getName().equals("if")) {
								//先通通算做while
								((IfSentence)afterCondSen).setReversedWhile();
								
							}else{
								//TODO 倒置if的cond后面不是if
								log.info(this.mgr.getMeth().getName()+" reverse if's cond ,but after it is not if sen."+ifs.getOut());
							}
						}
					}
				}
				/*
				int ifTagLn = ifs.getCondTag().getLineNum();
				boolean isReversedWhile = false;
				if (i > this.senList.indexOf(this.returnSentence)) {
					//对于倒置tag的if，但tag仍在return之后的if，且中间没有goto语句的if,先设置成while
					if (ifTagLn< ifs.getLineNum() && ifTagLn > this.returnSentence.getLineNum()) {
						isReversedWhile = true;
						for (int j = this.senList.indexOf(ifs.getCondTag()); j < i; j++) {
							Sentence s3 = this.senList.get(j);
							if (s3.getName().equals("goto")) {
								isReversedWhile = false;
								break;
							}
						}
					}
					if (isReversedWhile) {
						ifs.setWhile();
						for (int j = i; j <= re[1]; j++) {
							Sentence s1 = this.senList.get(j);
							if (s1.getName().equals("if")) {
								IfSentence if2 = (IfSentence)s1;
								if2.setMergeAsIf(true);
							}
						}
					}
				}*/
				//预处理所有可能的while,即if上方为gotoTag
				//!isReversedWhile && 
				if (i>1 && this.senList.get(i-1).getName().equals("gotoTag")) {
					ifs.setWhile();
				}
				i = re[0];
			}
		}
	}

	private void shiftIfBlock(){
		//用于保存ifs之前的gotoTag
//		GotoTagSentence preGotoTag = null;
		for (int i = 0; i < this.senList.size(); i++) {
			Sentence s = this.senList.get(i);
//			if (s.getName().equals("gotoTag")) {
//				preGotoTag = (GotoTagSentence)s;
//				continue;
//			}else
			if (s.getName().equals("if") && s.getState() == Sentence.STATE_DOING) {
				IfSentence ifs = (IfSentence)s;
				int[] re = this.defineIfBlock(i);
				TagSentence tag = ((IfSentence)(this.senList.get(re[1]))).getCondTag();
				int tagIndex = this.senList.indexOf(tag);
				//tag在return之后时需要移动
				if (tagIndex > this.senList.indexOf(this.returnSentence)) {
					
					//定位ifs之后的gotoTag
					GotoTagSentence afterGotoTag = null;
					for (int j = i+1; j < this.senList.size(); j++) {
						Sentence s2 = this.senList.get(j);
						if (s2.getName().equals("gotoTag")) {
							afterGotoTag = (GotoTagSentence) s2;
							break;
						}else if(s2.getName().equals("return")){
							//return 语句时停止
							break;
						}
					}
//					if (tag.getLineNum() > ifs.getLineNum()) {
//						//倒置cond的if,为while语句 //应该是倒置的cond后面的if为while
//						ifs.setWhile();
//					}
					int start = tagIndex;
					//取出需要移动的块
					ArrayList<Sentence> ls = new ArrayList<Sentence>();
					GotoTagSentence gotoTag = null;
					GotoSentence gts = null;
					int gotoTagIndex = -1;
					for (int j = start; j <this.senList.size(); ) {
						Sentence s1 = this.senList.get(j);
						ls.add(this.senList.remove(j));
						if (s1.getName().equals("goto")) {
							gts = (GotoSentence)s1;
							//goto语句为止
							gotoTag = (GotoTagSentence) gts.getTargetSen();
							gotoTagIndex = this.senList.indexOf(gotoTag);
							if (gotoTagIndex < 0) {
								//gotoTag在被移动的块中时继续向下找 //不能继续向下找
//								j++;
//								continue;
								log.error(this.mgr.getMeth().getName()+" -- Maybe there's lots of while circle. maybe some structs need be fixed.");
								break;
							}else{
								break;
							}
						}
//							//不可能为return
//							else if(s1.getName().equals("return")){
//								break;
//							}
					}
					//确定插入的位置 ----------------------
					int po = -1;
					//是否需要最后移动（跳过gotoTag之前的tag等）
					boolean lastMove = true;
					//确认是否是真正的倒置while,如果不是则设置回if
					if (ifs.isWhile()) {
						//上一句为gotoTag的while,需要判断gotoTag对应的goto是否有在ifs之后的
						this.checkWhileAgain(ifs, i);
					}
					//一直到最后句未找到goto语句时,一般为if的最后一个else语句
					if (gotoTag == null) {
						if (ifs.isWhile()) {
							po = re[0]+1;
						}else{
							po = this.senList.indexOf(afterGotoTag);
							gotoTag = afterGotoTag;
						}
					}
					else{
						boolean isWhile = ifs.isWhile();
						if (gotoTagIndex>=0 && gotoTagIndex < i) {
							//gotoTag为倒置时插入到if块之后,此时判断是while语句,不再进行最终move
							isWhile = true;
							//gotoTagIndex后面不能有其他的if，否则视为while内部的if
							for (int j = gotoTagIndex+1; j < i; j++) {
								Sentence s4 = this.senList.get(j);
								if (s4.getName().equals("if")) {
									IfSentence ifw = (IfSentence)s4;
									if (ifw.isWhile()) {
										isWhile = false;
										break;
									}
								}
							}
						}
						if(isWhile){
							ifs.setWhile();
							lastMove = false;
							if (gotoTag.getInsertPoSen() == null) {
								po = re[0]+1;
							}else{
								//如果gotoTag所在的while已经有插入的内容块了,则插入点在其之后
								int wEnd = this.senList.indexOf(gotoTag.getInsertPoSen());
								if (gts.getLineNum() < gotoTag.getInsertPoSen().getLineNum()) {
									po = wEnd;
								}else{
									//goto的原始位置在WhileContentEnd之前时,从re[0]向下找到插入位置
									po = re[0]+1;
									for (int j = po; j < wEnd; j++) {
										if (gts.getLineNum() < this.senList.get(j).getLineNum()) {
											po = j+1;
											continue;
										}else{
											break;
										}
									}
								}
							}
						}
						//正常顺序的if时
						else{
							//gotoTagIndex为-1,应该与gotoTag==null同样，此情况应该不存在
							if (gotoTagIndex == -1) {
								log.error(this.mgr.getMeth().getName()+" gotoTagIndex is -1!");
							}
							//ifs后面没有afterGotoTag,取return的位置
							if(afterGotoTag == null){
								po = this.senList.indexOf(this.returnSentence);
							}else{
								//正常if语句插入位置在ifs下方的第一个gotoTag处  ,注意不能直接是gotoTag
								po = this.senList.indexOf(afterGotoTag);
							}
						}
					}
					//最后移动插入位置（跳过gotoTag之前的tag等）
					if (lastMove) {
						//先跳过tag等
						for (int j = po - 1; j >= 0; j--) {
							Sentence s3 = this.senList.get(j);
							String sn = s3.getName();
							//向上跳过tag和catch部分
							if (sn.equals("gotoTag") || sn.equals("try")) {
								po--;
							} else if(sn.equals("tag")){
//								//TODO 特定的跳出,待验证
//								if (((TagSentence)s3).isReverseWhileStart()) {
//									break;
//								}else{
									
									po--;
//								}
							}else{
								break;
							}
						}
						//前面有过插入的块时
						if (gotoTag != null && gotoTag.getInsertPoSen()!= null) {
							int wEnd = this.senList.indexOf(gotoTag.getInsertPoSen());
							if (gts.getLineNum() < gotoTag.getInsertPoSen().getLineNum()) {
								po = wEnd;
							}else{
								//向后找到插入位置
								int curPo = po;
								for (int j = wEnd+1; j < curPo; j++) {
									if (gts.getLineNum() > this.senList.get(j).getLineNum()) {
										po = j+1;
										continue;
									}else{
										break;
									}
								}
							}
						}
						
					}
					//保存本次移动的po位置sen
					if (gotoTag == null) {
						System.out.println("-------??????--------"+this.mgr.getMeth().getName());
					}
					if (ifs.isWhile()) {
						//while语句将移动块的最后一句保存为insertPoSen
						gotoTag.setInserPoSen(ls.get(ls.size()-1));
						//将开头的tag放到最后
						ls.add(ls.remove(0));
					}else{
						//if语句将移动块的第一句保存为insertPoSen
						gotoTag.setInserPoSen(ls.get(0));
					}
					//插入取出的块 -----------------
					if (po > -1) {
						this.senList.addAll(po, ls);
					}else{
						log.error(this.mgr.getMeth().getName()+" - insert po can not be found: "+ifs.getOut());
					}
					//合并条件并处理初步结构--------------------------
					if (ifs.isWhile()) {
						ifs = this.mergeWhileConds(i, re);
						ifs.setWhile();
						
					}else{
						ifs = this.mergeConds(i, re,this.senList.get(re[0]).getLineNum()+1F);
						
//						//处理直接的return,更多的由reCheck去处理
//						if (ifs.isToReturn()) {
//							TagSentence ifTag = ifs.getCondTag();
//							//直接复制return句的输出
////							if (this.mgr.getMeth().getReturnStr().equals("void")) {
////							}else{
////								
////							}
//							ifTag.setOut(new StringBuilder(this.returnSentence.getOut()).append(StaticUtil.NEWLINE).append(StaticUtil.TABS[ifTag.level]).append(ifTag.getOut()).toString());
//						}
						//处理最后的else块
						if (!ls.get(1).getName().equals("if")) {
							this.addLastElse(ls.get(0),ls.get(ls.size()-1));
						}
					}
				}else{
					//正常if
					if (ifs.isWhile()) {
						this.checkWhileAgain(ifs, i);
					}
					if (ifs.isWhile()) {
						//正常的却被误认为倒置cond的while，应该是do while的do部分s
//						log.info(this.mgr.getMeth().getName()+" ========= maybe do{}?"+ifs.getOut());
						ifs = this.mergeWhileConds(i, re);
					}else{
						ifs = this.mergeConds(i, re,this.senList.get(re[0]).getLineNum()+1F);
					}
					
				}
			}
		}
	}
	
	
	/**
	 * 进一步确认gotoTag+if形成的while，后面必须有指向此tag的goto才是真正的while，否则设回if
	 * @param ifs
	 * @param index ifs所在的index
	 */
	private void checkWhileAgain(IfSentence ifs,int index){
		boolean checkWhile = false;
		if (!ifs.isReversedWhile() && this.senList.get(index-1).getName().equals("gotoTag")) {
			GotoTagSentence gtTa = (GotoTagSentence) this.senList.get(index-1);
			String wTag = gtTa.getTag();
			for (int j = index+1; j < this.senList.size(); j++) {
				Sentence s5 = this.senList.get(j);
				if (s5.getName().equals("goto")) {
					GotoSentence gtt = (GotoSentence)s5;
					if (gtt.getTarget().equals(wTag)) {
						checkWhile = true;
						break;
					}
				}
			}
			if (!checkWhile) {
				ifs.setAsIf();
			}
		}
	}
	
	
	
	/**
	 * 增加else处理部分
	 */
	private void addLastElse(Sentence startSen,Sentence endSen){
		TagSentence eStart = (TagSentence)startSen;
		eStart.setLastElseStart(true);
		endSen.appendToOutEnd(StaticUtil.NEWLINE).appendToOutEnd(StaticUtil.TABS[endSen.level]).appendToOutEnd("} //else made");
	}
	
	/**
	 * 合并多条件,合并结束后进行over处理
	 * @param startIndex 条件正向开始语句(包含)
	 * @param endRE defineIfBlock结果
	 * @return 合并后的IfSentence
	 */
	private IfSentence mergeWhileConds(int startIndex,int[] endRE){
		
		IfSentence lastIf = (IfSentence) this.senList.get(endRE[1]);
		//特定的同if处理情况
//		if (lastIf.isMergeAsIf()) {
//			return this.mergeConds(startIndex, endRE, this.senList.get(endRE[0]).getLineNum()+1F);
//		}
		TagSentence lastIfTag = lastIf.getCondTag();
		int lastIfTagLn = lastIfTag.getLineNum();
		//if块下方紧接着的tag,如果没有则创建一个
		TagSentence afterIfTag = null;
		if (endRE[0] == endRE[1]) {
			afterIfTag = new TagSentence(mgr, "");
			afterIfTag.setLineNum(lastIf.getLineNum()+1);
		}else{
			afterIfTag = (TagSentence) this.senList.get(endRE[0]);
		}
		int afterIfTagLn = afterIfTag.getLineNum();
		//if倒置的情况
		boolean isWhileTagReversed = this.senList.indexOf(lastIfTag) < startIndex;
		if (isWhileTagReversed) {
			lastIfTag.setLineNum(afterIfTagLn+1);
		}
		//内容块位于afterIfTag和lastIfTag之间
		float contentLn = afterIfTagLn+0.5F;
		//交换afterIfTag与lastIfTag的lineNum
		afterIfTag.setLineNum(lastIfTagLn);
		lastIfTag.setLineNum(afterIfTagLn);
		
		//改变lastIf的指向，并进行反向
		lastIf.reverseCompare();
		lastIf.setReversed(false);
		lastIf.setSpecialTag(afterIfTag);
		/*
		//判断是否倒置cond形式的while
		if (!isWhileTagReversed) {
			lastIf.reverseCompare();
			lastIf.setReversed(false);
			lastIf.setSpecialTag(afterIfTag);
//			if (endRE[0] == endRE[1]) {
//				tmp.setLineNum(lastIfTagLn+1);
//				contentLn = lastIfTagLn + 0.5F;
//				lastIf.setSpecialTag(tmp);
//			}else{
//				
//				contentLn = lastIfTagLn + 0.5F;
//				lastIfTag.setLineNum(afterIfLn);
//			}
		}else{
			contentLn = contentLn+1F;
			lastIfTag.setLineNum(afterIfTagLn+1);
		}*/
		
		IfSentence ifs =  this.mergeConds(startIndex, endRE, contentLn);
		//恢复状态
		lastIfTag.setLineNum(lastIfTagLn);
		afterIfTag.setLineNum(afterIfTagLn);
		lastIf.setSpecialTag(null);
		//将合并后的ifs指向lastIfTag
		ifs.setCondTag(lastIfTag);
		return ifs;
	}
	
	
	
	
	/**
	 * 合并多条件,合并结束后进行over处理
	 * @param startIndex 条件正向开始语句(包含)
	 * @param endRE defineIfBlock的结果
	 * @return 合并后的IfSentence
	 */
	private IfSentence mergeConds(int startIndex,int[] endRE,float contentLn){
		IfSentence ifs = null;
		if (startIndex == endRE[0]) {
			ifs = (IfSentence) this.senList.get(startIndex);
			ifs.reverseCompare();
			return ifs;
		}
		int ifCount = 2;
		TagSentence tag = null;
		int maxTimes = 1000;
//		float contentLn = this.senList.get(endRE[1]).getLineNum()+0.5F;
		while (ifCount >= 2) {
			maxTimes--;
			if (maxTimes <= 0) {
				log.error(this.mgr.getMeth().getName()+" - mergeConds out of maxTimes.");
				return null;
			}
			ifCount = 0;
			for (int i = startIndex; i <= endRE[0]; i++) {
				Sentence s = this.senList.get(i);
				boolean doMerge = true;
				//第一个if(doing状态)
				if (s.getName().equals("if") && s.getState() == Sentence.STATE_DOING) {
					ifCount++;
					ifs = (IfSentence)s;
					tag = ifs.getSpecialTag();
					IfSentence ifs2 = null;
					TagSentence tag2 = null; 
					//在if和对应的tag之间查找第二个if
					//内部if数,仅有一个时才进行合并
					int innerIfCount = 0;
					for (int j = i+1; j <= endRE[0]; j++) {
						
						Sentence sen = this.senList.get(j);
						//中间不能有未over的tag
						if (sen.getName().equals("tag") && sen.getState() == Sentence.STATE_DOING) {
							break;
						}
						//第二个if(doing状态)
						if (sen.getName().equals("if") && sen.getState() == Sentence.STATE_DOING) {
							ifCount++;
							innerIfCount++;
							ifs2 = (IfSentence)sen;
							tag2 = ifs2.getSpecialTag();
						}
						if (innerIfCount > 1) {
							ifCount = ifCount-2;
							doMerge = false;
							break;
						}else{
							doMerge = true;
						}
					}
					//仅有两个if包含时进行合并
					if (doMerge) {
						//仅有一个if，无需要合并的情况下
						if (ifs2 == null) {
							if (tag.getLineNum()>contentLn) {
								ifs.reverseCompare();
							}
							continue;
						}
						//需要合并,先处理倒置的if2的情况//不处理,已在mergeWhile中处理
						boolean ifs2Reversed = false;
						int tag2Ln = tag2.getLineNum();
						if (ifs2.getCondTag().getLineNum() < ifs2.getLineNum() && this.senList.indexOf(ifs2) < this.senList.indexOf(tag2) && !ifs.isWhile()) {
							System.out.println(this.mgr.getMeth().getName());
							ifs2Reversed = true;
							tag2.setLineNum(Math.round(contentLn+1));
						}
						boolean isAnd = true;
						//如果外部if对应的tag指向内容块之前
						if(tag.getLineNum() < contentLn){
							isAnd = true;
							if (tag.getLineNum()<ifs2.getLineNum()) {
								isAnd = false;
							}else{
								if (tag2.getLineNum() > contentLn) {
									ifs2.reverseCompare();
									isAnd = false;
								}else if(tag2.getLineNum() < contentLn){
									isAnd = true;
								}
							}
						}
						//外部if正指向内容块后
						else if(tag.getLineNum() >  contentLn){
							if (tag.getLineNum()<ifs2.getLineNum()) {
								isAnd = false;
							}else{
								if (tag2.getLineNum() > contentLn) {
									ifs2.reverseCompare();
									isAnd = true;
								}else if(tag2.getLineNum() < contentLn){
									isAnd = false;
								}
							}
						}
						// 合并|| 或 &&
						if (isAnd) {
							ifs.reverseCompare();
						}
						ifs.mergeIf(isAnd, ifs2);
						//是否要加括号保护
						if (tag.getLineNum() != tag2.getLineNum()) {
							ifs.addCondProtect();
						}
						tag.over();
						//外部if新的tag,//修改合并后的指向
						ifs.setCondTag(tag2);
						ifs.setCond(tag2.getTag());
						tag2.over();
						//合并后减少1
						ifCount--;
						//回复倒置的ifs2
						if (ifs2Reversed) {
							tag2.setLineNum(tag2Ln);
						}
						
					}
				}
				
			}
		}
		return ifs;
	}

	
	
	
	
//	/**
//	 * else 入口,初始值为-1
//	 */
//	private int elseEntry = -1;
	
//	private ArrayList<GotoTagSentence> elseEntrys = new ArrayList<GotoTagSentence>();
	
	
	/**
	 * 定位if条件块,去除if条件之外的if
	 * @param beginIndex 开始if的index,包含
	 * @return 数组:[条件块真正结束的index,最后一个if的index]
	 */
	private int[] defineIfBlock(int beginIndex){
		int endIndex = beginIndex;
		int lastCondIndex = beginIndex;
		int realEndIndex = endIndex;
		//正向查询,先将连续的if和tag扫描完
		for (int i = beginIndex; i < this.len; i++) {
			Sentence s = this.senList.get(i);
			if (s.getName().equals("if") && s.getState() == Sentence.STATE_DOING) {
				endIndex = i;
				continue;
			}else if(s.getName().equals("tag") && s.getState() == Sentence.STATE_DOING){
				endIndex = i;
				continue;
			}else{
				break;
			}
		}
		//beginIndex到endIndex为index区域
		boolean hasAReverseTag = false;
		int lastIfTagIndex = -1;
		for (int i = beginIndex; i <= endIndex ; i++) {
			Sentence s = this.senList.get(i);
			if (s.getName().equals("if")) {
				IfSentence ifs = (IfSentence)s;
				int condIndex = this.senList.indexOf(ifs.getCondTag());
				if (condIndex>=beginIndex && condIndex <= endIndex) {
					//cond在index区块内
					lastCondIndex = i;
					realEndIndex = i;
					continue;
				}else if(condIndex < i){
					//倒置tag
					if (!hasAReverseTag) {
						//有一个if对应的倒置tag可以不在这个index区域内
						hasAReverseTag = true;
						lastCondIndex = i;
						realEndIndex = i;
						continue;
					}else if(lastIfTagIndex == -1){
						//最末一个if对应的tag以及以tag对应的其他if可以不在这个index区域内
						lastIfTagIndex = condIndex;
						lastCondIndex = i;
						realEndIndex = i;
						continue;
					}else if(lastIfTagIndex == condIndex){
						lastCondIndex = i;
						realEndIndex = i;
						continue;
					}else{
						break;
					}
				}else if(lastIfTagIndex == -1){
					//最末一个if对应的tag以及以tag对应的其他if可以不在这个index区域内
					lastIfTagIndex = condIndex;
					lastCondIndex = i;
					realEndIndex = i;
					continue;
				}else if(lastIfTagIndex == condIndex){
					lastCondIndex = i;
					realEndIndex = i;
					continue;
				}else{
					break;
				}
			}else if(s.getName().equals("tag")){
				TagSentence tag = (TagSentence)s;
				//注意只对应最上方的if的index
				int ifIndex = this.senList.indexOf(tag.getIfSen());
				if (ifIndex >= beginIndex && ifIndex <= endIndex) {
					realEndIndex = i;
					continue;
				}else{
					break;
				}
			}else{
				break;
			}
		}
		if (realEndIndex < beginIndex) {
			realEndIndex = beginIndex;
		}
		return new int[]{realEndIndex,lastCondIndex};
	}
	
	


//	private int findSenIndex(int start,String typeName){
//		for (int i = start; i < this.len; i++) {
//			Sentence sen = this.senList.get(i);
//			if (sen.getName().equals(typeName)) {
//				return i;
//			}
//		}
//		return -1;
//	}
	
//	private int findSenIndex(int start,String typeName,String[] exceptNames){
//		for (int i = start; i < this.len; i++) {
//			Sentence sen = this.senList.get(i);
//			if (exceptNames != null) {
//				for (int j = 0; j < exceptNames.length; j++) {
//					if (sen.getName().equals(exceptNames[j])) {
//						return -1;
//					}
//				}
//			}
//			if (sen.getName().equals(typeName)) {
//				return i;
//			}
//		}
//		return -1;
//	}
	
	/**
	 * 向下查找if内容块中最后一条语句的lineNum
	 * @param ifs IfSentence
	 * @param start 开始扫描的index(包含)
	 * @return 内容块结束的lineNum
	 */
	private int findIfEndLineNum(IfSentence ifs,int start){
		int ln = start;
		for (int i = start; i < this.len; i++) {
			Sentence s = this.senList.get(i);
			if (s.getName().equals("return")) {
				//直接返回最后一行，即return行
				return s.getLineNum();
			}
			//真正有输出的语句
			else if(s.getName().equals("goto")){
				int n = this.senList.indexOf(((GotoSentence)s).getTargetSen());
				return this.findIfEndLineNum(ifs, n+1);
			} 
//			if (s.getType() == Sentence.TYPE_LINE || s.getName().equals("if")|| s.getName().equals("return")) {
			else if(s.getName().equals("tag") || s.getName().equals("gotoTag") || s.getName().equals("switch")){
				ln = s.getLineNum();
				continue;
			}
			else if (!isStructSen(s)) {
				ln = s.getLineNum();
				continue;
			} else{
				ln = s.getLineNum();
				break;
			}
		}
		return ln;
	}
	/**
	 * 初始化cond和ifsen以及goto的对应关系
	 */
	private boolean init(){
//		this.maxLineNum = this.senList.get(len-1).getLineNum();
		//先扫描两次将cond和if对应上
		boolean returnScope = false;
		boolean isTagToReturnOK = true;
		boolean returnUpHasTag = true;
		for (int i = len-1; i >= 0; i--) {
			Sentence s = senList.get(i);
			if(returnScope && s.getName().equals("if")){
				returnScope = false;
				if (!returnUpHasTag) {
					((IfSentence)s).setToReturn(true);
				}
			}else if(returnScope && s.getName().equals("goto")){
				returnScope = false;
			}
			else if ((s.getName().equals("tag") || s.getName().equals("gotoTag"))) {
				TagSentence ts = (TagSentence)s;
				condMap.put(ts.getTag(), ts);
				if (returnScope) {
					if (isTagToReturnOK) {
						ts.setReturn(true);
					}
					returnUpHasTag = true;
				}
			}else if(s.getName().equals("return")){
//				this.returnLineNum = s.getLineNum();
				this.returnSentence = (ReturnSentence) s;
				returnScope = true;
				returnUpHasTag = false;
			}else{
				if (returnScope) {
					isTagToReturnOK = false;
				}
				continue;
			}
		}
		if (condMap.isEmpty()) {
			//无tag结构，直接返回
			return false;
		}
		for (int i = len-1; i >= 0; i--) {
			Sentence s = senList.get(i);
			if (s.getName().equals("if")) {
				IfSentence ifs = (IfSentence)s;
				TagSentence ts = condMap.get(ifs.getCond());
				if (ts != null) {
					//对于1个tag对应多个if的情况,最终为最上方的if与tag相互对应,其他为if单向对应tag
					ifs.setCondTag(ts);
					ts.setIfSen(ifs);
				}else{
					log.error(this.mgr.getMeth().getName()+" - ifs cond not found: "+ifs.getCond());
				}
			}else if(s.getName().equals("goto")){
				GotoSentence gs = (GotoSentence)s;
				TagSentence ts = condMap.get(gs.getTarget());
				if (ts != null) {
					ts.setIfSen(gs);
					gs.setTargetSen(ts);
//					((GotoTagSentence)ts).addGotoTimes();
				}else{
					log.error(this.mgr.getMeth().getName()+" - goto target not found: "+gs.getTarget());
				}
			}
		}
//		lastSenLineNum = this.senList.get(this.len-1).getLineNum();
		
		
		
		
		return true;
	}
	
}
