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
package javax.safetycritical;

import joprt.RtThread;

/**
 * @author Martin Schoeberl
 *
 */
public class JopSystem {
	
	public static void startMission(Safelet scj) {
		MissionSequencer ms = scj.getSequencer();
//		MissionDescriptor md = ms.getInitialMission();
		// TODO: there is some chaos on mission and the classes
		// for it -- needs a reread on current spec
		// and a fix
//		MissionDescriptor md = ms.getNextMission();
		MissionDescriptor md = null;
		md.initialize();
		Terminal.getTerminal().writeln("SCJ Start mission on JOP");
		RtThread.startMission();
	}

}
