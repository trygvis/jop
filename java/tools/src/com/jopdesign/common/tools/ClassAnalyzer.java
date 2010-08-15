/*
 * This file is part of JOP, the Java Optimized Processor
 *   see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2010, Stefan Hepp (stefan@stefant.org).
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

package com.jopdesign.common.tools;

import com.jopdesign.common.ClassInfo;

import java.util.Set;

/**
 * A helper class to perform various ClassInfo/MethodInfo/FieldInfo query tasks.
 *
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class ClassAnalyzer {

    /**
     * Get a set of all classes referenced by the given class, including superclasses, interfaces and
     * references from the code, from parameters and from attributes.
     *
     * @param classInfo the classInfo to search
     * @return a set of all fully qualified class names referenced by this class.
     */
    public static Set<String> findReferencedClasses(ClassInfo classInfo) {

        return null;
    }


}
