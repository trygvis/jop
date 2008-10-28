/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2008, Martin Schoeberl (martin@jopdesign.com)

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


/**
 * 
 */
package com.jopdesign.build;

import java.io.IOException;

/**
 * Perform the JOPtimizer transformations on the class files
 * and write class fils to the output directory for the WCET
 * analysis
 * 
 * @author Martin Schoeberl
 *
 */
public class WcetPreprocess extends AppInfo {

	public WcetPreprocess(ClassInfo cliTemplate) {
		super(cliTemplate);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AppInfo ai = new AppInfo(ClassInfo.getTemplate());
		ai.parseOptions(args);
		try {
			ai.load();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		ai.iterate(new ReplaceIinc(ai));
		ai.iterate(new InsertSynchronized(ai));
		// dump the methods
//		try {
//			ai.iterate(new Dump(ai, new PrintWriter(new FileOutputStream(ai.outFile+"/dump.txt"))));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		// write the class files
		ai.iterate(new ClassWriter(ai, ai.outFile));
	}

}
