/* $Id$
 * 
 * This file is a part of jPapaBench providing a Java implementation 
 * of PapaBench project.
 * Copyright (C) 2010  Michal Malohlava <michal.malohlava_at_d3s.mff.cuni.cz>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */
package papabench.core.autopilot.tasks.pids;

import papabench.core.autopilot.modules.AutopilotStatus;
import papabench.core.autopilot.modules.Estimator;
import papabench.core.autopilot.modules.Navigator;
import papabench.core.utils.MathUtils;

/**
 * Altitude PID controller computes climb rate according to desired altitude.
 * 
 * @author Michal Malohlava
 * 
 */
//@SCJAllowed
public class AltitudePIDController extends AbstractPIDController {

	private float altitudePGain = ALTITUDE_PGAIN;

	public void control(AutopilotStatus status, Estimator estimator, Navigator navigator) {		
		 float err = estimator.getPosition().z -  navigator.getDesiredAltitude();
		 
		 float desiredClimb = navigator.getPreClimb() + altitudePGain * err;
		 desiredClimb = MathUtils.symmetricalLimiter(desiredClimb, CLIMB_MAX);
		 
		 status.setClimb(desiredClimb);
	}
}
