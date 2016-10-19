/*
 * Copyright 2016 Federico Tomassetti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tomassetti.symbolsolver.reflectionmodel;

import me.tomassetti.symbolsolver.model.declarations.*;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.usages.typesystem.*;
import me.tomassetti.symbolsolver.model.usages.typesystem.Type;
import me.tomassetti.symbolsolver.model.usages.typesystem.TypeVariable;

import java.lang.reflect.*;

public class ReflectionFactory {
    public static Type typeUsageFor(Class<?> clazz, TypeSolver typeSolver) {
        if (clazz.isArray()) {
            return new ArrayType(typeUsageFor(clazz.getComponentType(), typeSolver));
        } else if (clazz.isPrimitive()) {
            if (clazz.getName().equals("void")) {
                return VoidType.INSTANCE;
            } else {
                return PrimitiveType.byName(clazz.getName());
            }
        } else if (clazz.isInterface()) {
            return new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(clazz, typeSolver), typeSolver);
        } else {
            return new ReferenceTypeImpl(new ReflectionClassDeclaration(clazz, typeSolver), typeSolver);
        }
    }

    public static Type typeUsageFor(java.lang.reflect.Type type, TypeSolver typeSolver) {
        if (type instanceof java.lang.reflect.TypeVariable) {
            java.lang.reflect.TypeVariable tv = (java.lang.reflect.TypeVariable) type;
            boolean declaredOnClass = ((java.lang.reflect.TypeVariable) type).getGenericDeclaration() instanceof java.lang.reflect.Type;
            TypeParameterDeclaration typeParameter = new ReflectionTypeParameter(tv, declaredOnClass);
            return new TypeVariable(typeParameter);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            ReferenceType rawType = typeUsageFor(pt.getRawType(), typeSolver).asReferenceType();
            int i = 0;
            for (java.lang.reflect.Type actualTypeArgument : pt.getActualTypeArguments()) {
                rawType = rawType.replaceParam(i, typeUsageFor(actualTypeArgument, typeSolver)).asReferenceType();
                i++;
            }
            return rawType;
        } else if (type instanceof Class) {
            Class c = (Class) type;
            if (c.isPrimitive()) {
                if (c.getName().equals("void")) {
                    return VoidType.INSTANCE;
                } else {
                    return PrimitiveType.byName(c.getName());
                }
            } else if (c.isArray()) {
                return new ArrayType(typeUsageFor(c.getComponentType(), typeSolver));
            } else if (c.isInterface()) {
                return new ReferenceTypeImpl(new ReflectionInterfaceDeclaration(c, typeSolver), typeSolver);
            } else {
                return new ReferenceTypeImpl(new ReflectionClassDeclaration(c, typeSolver), typeSolver);
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return new ArrayType(typeUsageFor(genericArrayType.getGenericComponentType(), typeSolver));
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            if (wildcardType.getLowerBounds().length > 0 && wildcardType.getUpperBounds().length > 0) {
                if (wildcardType.getUpperBounds().length == 1 && wildcardType.getUpperBounds()[0].getTypeName().equals("java.lang.Object")) {
                    // ok, it does not matter
                }
            }
            if (wildcardType.getLowerBounds().length > 0) {
                if (wildcardType.getLowerBounds().length > 1) {
                    throw new UnsupportedOperationException();
                }
                return Wildcard.superBound(typeUsageFor(wildcardType.getLowerBounds()[0], typeSolver));
            }
            if (wildcardType.getUpperBounds().length > 0) {
                if (wildcardType.getUpperBounds().length > 1) {
                    throw new UnsupportedOperationException();
                }
                return Wildcard.extendsBound(typeUsageFor(wildcardType.getUpperBounds()[0], typeSolver));
            }
            return Wildcard.UNBOUNDED;
        } else {
            throw new UnsupportedOperationException(type.getClass().getCanonicalName() + " " + type);
        }
    }

    static AccessLevel modifiersToAccessLevel(final int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return AccessLevel.PUBLIC;
        } else if (Modifier.isProtected(modifiers)) {
            return AccessLevel.PROTECTED;
        } else if (Modifier.isPrivate(modifiers)) {
            return AccessLevel.PRIVATE;
        } else {
            return AccessLevel.PACKAGE_PROTECTED;
        }
    }
}
