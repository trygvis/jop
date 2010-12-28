/*
 * This file is part of JOP, the Java Optimized Processor
 * see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2010, Benedikt Huber (benedikt.huber@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jopdesign.wcet.uppaal.translator.cache;

import com.jopdesign.common.MethodInfo;
import com.jopdesign.common.code.ControlFlowGraph;
import com.jopdesign.wcet.WCETProcessorModel;
import com.jopdesign.wcet.WCETTool;
import com.jopdesign.wcet.jop.VarBlockCache;
import com.jopdesign.wcet.uppaal.model.NTASystem;
import com.jopdesign.wcet.uppaal.translator.SystemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LRUVarBlockCacheBuilder extends VarBlockCacheBuilder {
	public LRUVarBlockCacheBuilder(WCETTool p, VarBlockCache cache, Set<MethodInfo> methods) {
		super(p, cache, methods);
	}
	@Override
	protected int numBlocks() {
		return this.cache.getNumBlocks();
	}
	@Override
	public void appendDeclarations(NTASystem system,String NUM_METHODS) {
		super.appendDeclarations(system, NUM_METHODS);
		system.appendDeclaration(
				"/* LRU Cache Sim */"+
				"void access_cache(int mid) {\n"+
				"  int i = 0;\n"+
				"  int sz = NUM_BLOCKS[mid];\n"+
				"  if(cache[sz-1] == mid) {  /* No Change */\n" +
				"    lastHit = true;\n" +
				"    return; \n"+
				"  }\n" +				
				"  lastHit = false;\n"+
				"  /* search */\n" +
				"  for(i = sz; ! lastHit && i < "+numBlocks()+"; i++) {\n"+
				"      if(cache[i] == mid) {\n"+
				"        lastHit = true;\n"+
				"      }\n"+
				"  }\n"+
				" /* i points to the element after tag, move back */"+
				"  for(i = i-1; i >= sz; --i) {\n"+
				"     cache[i]=cache[i-sz];\n"+
				"  }\n"+
				"  for(i = 0; i < sz-1; i++) {\n"+
				"     cache[i] = "+NUM_METHODS+";\n"+
				"  }\n"+
				"  cache[i] = mid;\n"+
				"}\n");
	}
	protected StringBuilder initCache(String NUM_METHODS) {
		List<Object> cacheElems = new ArrayList<Object>();
		for(int i = 0; i < numBlocks(); i++) cacheElems.add(NUM_METHODS);
        // FIXME check: this has been "blocksOf(0)", check if this is the same!!
        MethodInfo method = project.getProjectConfig().getTargetMethodInfo();
		cacheElems.set(blocksOf(method)-1,0);
		return SystemBuilder.constArray(cacheElems);
	}
	public long getWaitTime(WCETProcessorModel proc, ControlFlowGraph cfg, boolean isInvoke) {
		return proc.getMethodCacheMissPenalty(cfg.getNumberOfWords(), isInvoke);
	}
}
