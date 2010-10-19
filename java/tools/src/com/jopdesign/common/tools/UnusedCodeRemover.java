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

import com.jopdesign.common.AppInfo;
import com.jopdesign.common.ClassInfo;
import com.jopdesign.common.FieldInfo;
import com.jopdesign.common.KeyManager;
import com.jopdesign.common.MemberInfo;
import com.jopdesign.common.MethodInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class UnusedCodeRemover {

    private static KeyManager.CustomKey keyUsed;

    private static KeyManager.CustomKey getCustomKey() {
        if (keyUsed == null) {
            keyUsed = KeyManager.getSingleton().registerKey(KeyManager.KeyType.STRUCT, "UnusedCodeRemover");
        }
        return keyUsed;
    }

    public UnusedCodeRemover() {
    }

    /**
     * Check if a MemberInfo (class, method, field) has been marked as used.
     * If a member has not been marked, we assume it is unused.
     *
     * @param member the member to check.
     * @return true if it has been marked used by {@link #markUsedMembers()}, else false.
     */
    public boolean isUsed(MemberInfo member) {
        Boolean used = (Boolean) member.getCustomValue(getCustomKey());
        // if it hasn't been marked, assume unused
        return used != null && used;
    }

    /**
     * Mark all used members and remove the rest.
     *
     * @param rebuildConstantPools if true, cleanup the ConstantPool of the remaining classes.
     */
    public void run(boolean rebuildConstantPools) {
        AppInfo appInfo = AppInfo.getSingleton();
        KeyManager.getSingleton().clearAllValues(getCustomKey());

        markUsedMembers();
        removeUnusedMembers();

        if ( rebuildConstantPools ) {
            for (ClassInfo cls : appInfo.getClassInfos()) {
                cls.rebuildConstantPool();
            }
        }
    }

    public void markUsedMembers() {

        // TODO build callgraph, traverse callgraph starting at AppInfo roots, mark methods/classes/fields as used

    }

    /**
     * Remove all unused classes, methods and fields.
     * <p>
     * Make sure you ran {@link #markUsedMembers()} before!</p>
     *
     * @see #run(boolean)
     */
    public void removeUnusedMembers() {
        AppInfo appInfo = AppInfo.getSingleton();

        // we cannot modify the lists while iterating through it
        List<ClassInfo> unusedClasses = new LinkedList<ClassInfo>();
        List<FieldInfo> unusedFields = new LinkedList<FieldInfo>();
        List<MethodInfo> unusedMethods = new LinkedList<MethodInfo>();

        for (ClassInfo cls : appInfo.getClassInfos()) {
            if (!isUsed(cls)) {
                unusedClasses.add(cls);
                continue;
            }

            unusedFields.clear();
            unusedMethods.clear();

            for (FieldInfo f : cls.getFields()) {
                if (!isUsed(f)) {
                    unusedFields.add(f);
                }
            }
            for (MethodInfo m : cls.getMethods()) {
                if (!isUsed(m)) {
                    unusedMethods.add(m);
                }
            }

            for (FieldInfo f : unusedFields) {
                cls.removeField(f.getSimpleName());
            }
            for (MethodInfo m : unusedMethods) {
                cls.removeMethod(m.getMemberSignature());
            }
        }

        appInfo.removeClasses(unusedClasses);
    }
}
