/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2008, Benedikt Huber (benedikt.huber@gmail.com)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.jopdesign.wcet.report;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.jopdesign.build.ClassInfo;
import com.jopdesign.build.MethodInfo;
import com.jopdesign.wcet.Project;
import com.jopdesign.wcet.annotations.LoopBound;
import com.jopdesign.wcet.frontend.ControlFlowGraph;
import com.jopdesign.wcet.frontend.CallGraph.CallGraphNode;
import com.jopdesign.wcet.graphutils.Pair;

public class MethodReport {
	private MethodInfo info;
	private Collection<LoopBound> loopBounds;
	private Set<String> referenced;
	String page;
	private int sizeInWords;
	private ControlFlowGraph fg;
	private int cacheBlocks;
	public MethodReport(Project p, MethodInfo m, String page) {
		this.info = m;
		fg = p.getAppInfo().getFlowGraph(info);
		this.loopBounds = fg.getLoopBounds().values();
		this.sizeInWords = fg.getNumberOfWords();		
		this.referenced = new TreeSet<String>();
		for(CallGraphNode cgn : p.getCallGraph().getReferencedMethods(m)) {
			Pair<ClassInfo, String> ref = cgn.getReferencedMethod();
			this.referenced.add(ref.fst().clazz.getClassName()+"."+ref.snd());
		}
		this.page = page;
		this.cacheBlocks = p.getProcessorModel().getMethodCache().requiredNumberOfBlocks(fg.getNumberOfWords());
	}
	public MethodInfo getInfo() {
		return info;
	}
	public Collection<LoopBound> getLoopBounds() {
		return loopBounds;
	}
	public Set<String> getReferenced() {
		return referenced;
	}
	public String getPage() {
		return page;
	}
	public int getSizeInWords() {
		return this.sizeInWords;
	}
	public int getCacheBlocks() {
		return cacheBlocks;
	}
}
