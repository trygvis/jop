/*
 * This file is part of JOP, the Java Optimized Processor
 *   see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2011, Stefan Hepp (stefan@stefant.org).
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

package com.jopdesign.common.type;

import com.jopdesign.common.misc.AppInfoError;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

/**
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class TypeHelper {

    public static int getNumSlots(Type[] types) {
        if (types == null) return 0;
        int i = 0;
        for (Type t : types) {
            i += t.getSize();
        }
        return i;
    }

    /**
     * Check if we can assign something with type 'from' to something with type 'to'.
     *
     * @see ReferenceType#isAssignmentCompatibleWith(Type)
     * @param from source type.
     * @param to target type.
     * @return true if source type can be implicitly converted to target type.
     */
    public static boolean canAssign(Type from, Type to) {

        // TODO should we do size-check first??
        if (from.equals(Type.UNKNOWN) || to.equals(Type.UNKNOWN)) return true;

        if (to.equals(Type.VOID)) return true;

        if (from.getSize() != to.getSize()) return false;

        if (from instanceof BasicType) {
            if (!(to instanceof BasicType)) return false;
            if (from.getType() == to.getType()) return true;

            switch (from.getType()) {
                case Constants.T_BOOLEAN:
                    return to.getType() == Constants.T_CHAR ||
                           to.getType() == Constants.T_SHORT ||
                           to.getType() == Constants.T_INT;
                case Constants.T_CHAR:
                    return to.getType() == Constants.T_SHORT ||
                           to.getType() == Constants.T_INT;
                case Constants.T_SHORT:
                    return to.getType() == Constants.T_INT;
                default:
                    return false;
            }
        }
        if (from instanceof ReferenceType) {
            try {
                return ((ReferenceType)from).isCastableTo(to);
            } catch (ClassNotFoundException e) {
                // TODO maybe silently ignore, just return true / false?
                throw new AppInfoError("Error checking assignment from "+from+" to "+to, e);
            }
        }
        // should not happen..
        throw new AppInfoError("Unknown Type type "+from);
    }

    /**
     * Check if an array of types is assignment-compatible to another array of types.
     * If the arrays have different length, then the suffix of the longer one is compared to the shorter
     * array.
     *
     * @see #canAssign(Type, Type)
     * @param from source types
     * @param to target types
     * @return true if the types from source can be assigned to the target
     */
    public static boolean canAssign(Type[] from, Type[] to) {
        int p1 = (from.length > to.length) ? from.length - to.length : 0;
        int p2 = (from.length < to.length) ? to.length - from.length : 0;
        while (p1 < from.length) {
            if (!canAssign(from[p1++], to[p2++])) {
                return false;
            }
        }
        return true;
    }
}
