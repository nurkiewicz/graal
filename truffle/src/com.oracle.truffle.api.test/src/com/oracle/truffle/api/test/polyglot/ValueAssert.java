/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.test.polyglot;

import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.ARRAY_ELEMENTS;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.BOOLEAN;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.EXECUTABLE;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.HOST_OBJECT;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.INSTANTIABLE;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.MEMBERS;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.NATIVE;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.NULL;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.NUMBER;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.PROXY_OBJECT;
import static com.oracle.truffle.api.test.polyglot.ValueAssert.Trait.STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess.Implementable;
import org.graalvm.polyglot.proxy.Proxy;

public class ValueAssert {

    private static final TypeLiteral<List<Object>> OBJECT_LIST = new TypeLiteral<List<Object>>() {
    };
    private static final TypeLiteral<Map<Object, Object>> OBJECT_OBJECT_MAP = new TypeLiteral<Map<Object, Object>>() {
    };
    private static final TypeLiteral<Map<String, Object>> STRING_OBJECT_MAP = new TypeLiteral<Map<String, Object>>() {
    };
    private static final TypeLiteral<Map<Long, Object>> LONG_OBJECT_MAP = new TypeLiteral<Map<Long, Object>>() {
    };
    private static final TypeLiteral<Map<Integer, Object>> INTEGER_OBJECT_MAP = new TypeLiteral<Map<Integer, Object>>() {
    };
    private static final TypeLiteral<Map<Short, Object>> SHORT_OBJECT_MAP = new TypeLiteral<Map<Short, Object>>() {
    };
    private static final TypeLiteral<Map<Byte, Object>> BYTE_OBJECT_MAP = new TypeLiteral<Map<Byte, Object>>() {
    };
    private static final TypeLiteral<Map<Number, Object>> NUMBER_OBJECT_MAP = new TypeLiteral<Map<Number, Object>>() {
    };
    private static final TypeLiteral<Map<Float, Object>> FLOAT_OBJECT_MAP = new TypeLiteral<Map<Float, Object>>() {
    };
    private static final TypeLiteral<Map<Double, Object>> DOUBLE_OBJECT_MAP = new TypeLiteral<Map<Double, Object>>() {
    };
    private static final TypeLiteral<Function<Object, Object>> FUNCTION = new TypeLiteral<Function<Object, Object>>() {
    };

    public static void assertValue(Value value) {
        assertValue(value, detectSupportedTypes(value));
    }

    public static void assertValue(Value value, Trait... expectedTypes) {
        try {
            assertValueImpl(value, 0, true, expectedTypes);
        } catch (AssertionError e) {
            e.addSuppressed(new AssertionError(String.format("assertValue: %s traits: %s", value, Arrays.asList(expectedTypes))));
            throw e;
        }
    }

    public static void assertValue(Value value, boolean hasHostAccess, Trait... expectedTypes) {
        try {
            assertValueImpl(value, 0, hasHostAccess, expectedTypes);
        } catch (AssertionError e) {
            e.addSuppressed(new AssertionError(String.format("assertValue: %s traits: %s", value, Arrays.asList(expectedTypes))));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertValueImpl(Value value, int depth, boolean hasHostAccess, Trait... expectedTypes) {
        if (depth > 10) {
            // stop at a certain recursion depth for recursive data structures
            return;
        }

        assertNotNull(value.toString());
        Value metaObject = value.getMetaObject();
        if (metaObject != null && depth == 0) { // meta-object may be null
            assertValueImpl(metaObject, depth + 1, hasHostAccess, detectSupportedTypes(metaObject));
            assertNotNull(metaObject.toString());
        }

        assertSame(value, value.as(Value.class));

        for (Trait supportedType : expectedTypes) {
            String msg = "expected " + supportedType.name() + " but was " + value.toString();
            switch (supportedType) {
                case NULL:
                    assertTrue(msg, value.isNull());
                    break;
                case BOOLEAN:
                    assertTrue(msg, value.isBoolean());
                    boolean booleanValue = value.asBoolean();
                    assertEquals(booleanValue, value.as(Boolean.class));
                    assertEquals(booleanValue, value.as(boolean.class));
                    break;
                case STRING:
                    assertTrue(msg, value.isString());
                    String stringValue = value.asString();
                    assertEquals(stringValue, value.as(String.class));
                    if (stringValue.length() == 1) {
                        assertEquals(stringValue.charAt(0), (char) value.as(Character.class));
                        assertEquals(stringValue.charAt(0), (char) value.as(char.class));
                    }
                    break;
                case NUMBER:
                    assertValueNumber(value);
                    break;
                case ARRAY_ELEMENTS:
                    assertTrue(msg, value.hasArrayElements());
                    assertValueArrayElements(value, depth, hasHostAccess);
                    break;
                case EXECUTABLE:
                    assertTrue(msg, value.canExecute());
                    assertFunctionalInterfaceMapping(value);
                    break;
                case INSTANTIABLE:
                    assertTrue(msg, value.canInstantiate());
                    value.as(Function.class);
                    // otherwise its ambiguous with the executable semantics.
                    if (!value.canExecute()) {
                        assertFunctionalInterfaceMapping(value);
                    }
                    break;
                case HOST_OBJECT:
                    assertTrue(msg, value.isHostObject());
                    Object hostObject = value.asHostObject();
                    assertFalse(hostObject instanceof Proxy);
                    if (hasHostAccess && hostObject != null && value.hasMembers() && !java.lang.reflect.Proxy.isProxyClass(hostObject.getClass())) {
                        if (hostObject instanceof Class) {
                            boolean isStaticClass = value.hasMember("class");
                            if (isStaticClass) {
                                assertClassMembers(value, (Class<?>) hostObject, true);
                            } else {
                                if (hasHostAccess) {
                                    assertClassMembers(value, Class.class, false);
                                    assertTrue(value.hasMember("static"));
                                }
                            }
                        } else {
                            assertClassMembers(value, hostObject.getClass(), false);
                        }
                    }
                    break;
                case PROXY_OBJECT:
                    assertTrue(msg, value.isProxyObject());
                    Object proxyObject = value.asProxyObject();
                    assertTrue(proxyObject instanceof Proxy);
                    break;
                case MEMBERS:
                    assertTrue(msg, value.hasMembers());

                    Map<Object, Object> expectedValues = new HashMap<>();
                    for (String key : value.getMemberKeys()) {
                        Value child = value.getMember(key);
                        expectedValues.put(key, child.as(Object.class));
                        if (!isSameHostObject(value, child)) {
                            assertValueImpl(child, depth + 1, hasHostAccess, detectSupportedTypes(child));
                        }
                    }

                    if (value.isHostObject() && value.asHostObject() instanceof Map) {
                        expectedValues = value.asHostObject();
                    } else {
                        Map<String, Object> stringMap = value.as(STRING_OBJECT_MAP);
                        assertTrue(expectedValues.equals(expectedValues));
                        assertTrue(stringMap.equals(stringMap));
                        assertFalse(value.as(STRING_OBJECT_MAP).equals(expectedValues));
                        assertTrue(value.as(STRING_OBJECT_MAP).equals(value.as(STRING_OBJECT_MAP)));
                        assertNotEquals(0, value.as(STRING_OBJECT_MAP).hashCode());
                        assertNotNull(value.as(STRING_OBJECT_MAP).toString());

                        assertContentEquals(expectedValues, value.as(STRING_OBJECT_MAP));

                        Set<String> keySet = value.as(Map.class).keySet();
                        assertEquals(value.getMemberKeys(), keySet);
                        for (String key : keySet) {
                            assertTrue(value.hasMember(key));
                        }
                    }
                    assertContentEquals(expectedValues, value.as(Map.class));
                    assertEquals(value.toString(), value.as(Map.class).toString());

                    break;
                case NATIVE:
                    assertTrue(msg, value.isNativePointer());
                    value.asNativePointer();
                    break;
            }
        }

        assertUnsupported(value, expectedTypes);
    }

    private static void assertContentEquals(Map<? extends Object, ? extends Object> map1, Map<? extends Object, ? extends Object> map2) {
        assertEquals(convertMap(map1), convertMap(map2));
    }

    private static Map<? extends Object, ? extends Object> convertMap(Map<? extends Object, ? extends Object> map) {
        if (map instanceof HashMap) {
            return map;
        } else {
            return new HashMap<>(map);
        }
    }

    private static boolean isSameHostObject(Value a, Value b) {
        return a.isHostObject() && b.isHostObject() && a.asHostObject() == b.asHostObject();
    }

    static void assertUnsupported(Value value, Trait... supported) {
        Set<Trait> supportedSet = new HashSet<>(Arrays.asList(supported));
        for (Trait unsupportedType : Trait.values()) {
            if (supportedSet.contains(unsupportedType)) {
                continue;
            }

            switch (unsupportedType) {
                case NUMBER:
                    assertFalse(value.isNumber());
                    assertFalse(value.fitsInByte());
                    assertFalse(value.fitsInShort());
                    assertFalse(value.fitsInInt());
                    assertFalse(value.fitsInLong());
                    assertFalse(value.fitsInFloat());
                    assertFalse(value.fitsInDouble());

                    if (value.isNull()) {
                        assertNull(value.as(Number.class));
                        assertNull(value.as(Byte.class));
                        assertNull(value.as(Short.class));
                        assertNull(value.as(Integer.class));
                        assertNull(value.as(Long.class));
                        assertNull(value.as(Float.class));

                        assertFails(() -> value.as(byte.class), NullPointerException.class);
                        assertFails(() -> value.as(short.class), NullPointerException.class);
                        assertFails(() -> value.as(int.class), NullPointerException.class);
                        assertFails(() -> value.as(long.class), NullPointerException.class);
                        assertFails(() -> value.as(float.class), NullPointerException.class);

                        assertFails(() -> value.asByte(), NullPointerException.class);
                        assertFails(() -> value.asShort(), NullPointerException.class);
                        assertFails(() -> value.asInt(), NullPointerException.class);
                        assertFails(() -> value.asLong(), NullPointerException.class);
                        assertFails(() -> value.asFloat(), NullPointerException.class);
                        assertFails(() -> value.asDouble(), NullPointerException.class);

                    } else {
                        if (value.isHostObject() && value.asHostObject() instanceof Number) {
                            assertSame(value.asHostObject(), value.as(Number.class));
                        } else {
                            assertFails(() -> value.as(Number.class), ClassCastException.class);
                        }
                        assertFails(() -> value.as(Byte.class), ClassCastException.class);
                        assertFails(() -> value.as(Short.class), ClassCastException.class);
                        assertFails(() -> value.as(Integer.class), ClassCastException.class);
                        assertFails(() -> value.as(Long.class), ClassCastException.class);
                        assertFails(() -> value.as(Float.class), ClassCastException.class);

                        assertFails(() -> value.as(byte.class), ClassCastException.class);
                        assertFails(() -> value.as(short.class), ClassCastException.class);
                        assertFails(() -> value.as(int.class), ClassCastException.class);
                        assertFails(() -> value.as(long.class), ClassCastException.class);
                        assertFails(() -> value.as(float.class), ClassCastException.class);

                        assertFails(() -> value.asByte(), ClassCastException.class);
                        assertFails(() -> value.asShort(), ClassCastException.class);
                        assertFails(() -> value.asInt(), ClassCastException.class);
                        assertFails(() -> value.asLong(), ClassCastException.class);
                        assertFails(() -> value.asFloat(), ClassCastException.class);
                        assertFails(() -> value.asDouble(), ClassCastException.class);
                    }

                    break;
                case BOOLEAN:
                    assertFalse(value.isBoolean());

                    if (value.isNull()) {
                        assertFails(() -> value.asBoolean(), NullPointerException.class);
                        assertFails(() -> value.as(boolean.class), NullPointerException.class);
                        assertNull(value.as(Boolean.class));
                    } else {
                        assertFails(() -> value.asBoolean(), ClassCastException.class);
                        assertFails(() -> value.as(boolean.class), ClassCastException.class);
                        assertFails(() -> value.as(Boolean.class), ClassCastException.class);
                    }

                    break;
                case STRING:
                    assertFalse(value.isString());

                    if (value.isNull()) {
                        assertNull(value.as(String.class));
                        assertNull(value.as(Character.class));
                        assertNull(value.asString());
                    } else {
                        assertFails(() -> value.asString(), ClassCastException.class);
                        assertFails(() -> value.as(String.class), ClassCastException.class);
                        if (value.fitsInInt() && value.asInt() >= 0 && value.asInt() < 65536) {
                            assertEquals((Character) (char) value.asInt(), value.as(Character.class));
                            assertEquals((Character) (char) value.asInt(), value.as(char.class));
                        } else {
                            assertFails(() -> value.as(Character.class), ClassCastException.class);
                            assertFails(() -> value.as(char.class), ClassCastException.class);
                        }
                    }

                    break;
                case MEMBERS:
                    assertFalse(value.hasMembers());
                    assertFalse(value.hasMember("asdf"));
                    assertFails(() -> value.getMember("asdf"), UnsupportedOperationException.class);
                    assertFails(() -> value.putMember("", ""), UnsupportedOperationException.class);
                    assertFails(() -> value.removeMember(""), UnsupportedOperationException.class);
                    assertFails(() -> value.invokeMember(""), UnsupportedOperationException.class);
                    assertTrue(value.getMemberKeys().isEmpty());

                    if (value.isNull()) {
                        assertNull(value.as(Map.class));
                    } else {
                        if (!value.isHostObject() || (!(value.asHostObject() instanceof Map))) {
                            assertFails(() -> value.as(Map.class), ClassCastException.class);
                        }
                    }

                    break;
                case EXECUTABLE:
                    assertFalse(value.toString(), value.canExecute());
                    assertFails(() -> value.execute(), UnsupportedOperationException.class);
                    if (value.isNull()) {
                        assertNull(value.as(Function.class));
                        assertNull(value.as(IsFunctionalInterfaceVarArgs.class));
                    } else if (!value.canInstantiate()) {
                        if (value.hasMembers()) {
                            assertFails(() -> value.as(FUNCTION).apply(null), UnsupportedOperationException.class);
                            assertFails(() -> value.as(IsFunctionalInterfaceVarArgs.class).foobarbaz(123), UnsupportedOperationException.class);
                        } else if (!value.isHostObject() || (!(value.asHostObject() instanceof Function))) {
                            assertFails(() -> value.as(FUNCTION), ClassCastException.class);
                            assertFails(() -> value.as(IsFunctionalInterfaceVarArgs.class), ClassCastException.class);
                        }
                    }
                    break;
                case INSTANTIABLE:
                    assertFalse(value.canInstantiate());
                    assertFails(() -> value.newInstance(), UnsupportedOperationException.class);
                    if (value.isNull()) {
                        assertNull(value.as(Function.class));
                        assertNull(value.as(IsFunctionalInterfaceVarArgs.class));
                    } else if (!value.canExecute()) {
                        if (value.hasMembers()) {
                            assertFails(() -> value.as(FUNCTION).apply(null), UnsupportedOperationException.class);
                            assertFails(() -> value.as(IsFunctionalInterfaceVarArgs.class).foobarbaz(123), UnsupportedOperationException.class);
                        } else if (!value.isHostObject() || (!(value.asHostObject() instanceof Function))) {
                            assertFails(() -> value.as(FUNCTION), ClassCastException.class);
                            assertFails(() -> value.as(IsFunctionalInterfaceVarArgs.class), ClassCastException.class);
                        }
                    }
                    break;
                case NULL:
                    assertFalse(value.isNull());
                    break;
                case ARRAY_ELEMENTS:
                    assertFalse(value.hasArrayElements());
                    assertFails(() -> value.getArrayElement(0), UnsupportedOperationException.class);
                    assertFails(() -> value.setArrayElement(0, null), UnsupportedOperationException.class);
                    assertFails(() -> value.getArraySize(), UnsupportedOperationException.class);
                    if (!value.isNull()) {
                        if ((!value.isHostObject() || (!(value.asHostObject() instanceof List) && !(value.asHostObject() instanceof Object[])))) {
                            assertFails(() -> value.as(List.class), ClassCastException.class);
                            assertFails(() -> value.as(Object[].class), ClassCastException.class);
                        }
                    } else {
                        assertNull(value.as(List.class));
                        assertNull(value.as(Object[].class));
                    }
                    break;
                case HOST_OBJECT:
                    assertFalse(value.isHostObject());
                    assertFails(() -> value.asHostObject(), ClassCastException.class);
                    break;
                case PROXY_OBJECT:
                    assertFalse(value.isProxyObject());
                    assertFails(() -> value.asProxyObject(), ClassCastException.class);
                    break;
                case NATIVE:
                    assertFalse(value.isNativePointer());
                    assertFails(() -> value.asNativePointer(), ClassCastException.class);
                    break;

            }

        }
    }

    @SuppressWarnings("unchecked")
    private static void assertValueArrayElements(Value value, int depth, boolean hasHostAccess) {
        assertTrue(value.hasArrayElements());

        List<Object> receivedObjects = new ArrayList<>();
        Map<Long, Object> receivedObjectsLongMap = new HashMap<>();
        Map<Integer, Object> receivedObjectsIntMap = new HashMap<>();
        for (long i = 0L; i < value.getArraySize(); i++) {
            Value arrayElement = value.getArrayElement(i);
            receivedObjects.add(arrayElement.as(Object.class));
            receivedObjectsLongMap.put(i, arrayElement.as(Object.class));
            receivedObjectsIntMap.put((int) i, arrayElement.as(Object.class));
            assertValueImpl(arrayElement, depth + 1, hasHostAccess, detectSupportedTypes(arrayElement));
        }

        List<Object> objectList1 = value.as(OBJECT_LIST);
        List<Object> objectList2 = Arrays.asList(value.as(Object[].class));

        if (!value.isHostObject() || !(value.asHostObject() instanceof List<?>)) {
            assertFalse(objectList1.equals(objectList2));
        }
        assertTrue(objectList1.equals(objectList1));
        assertTrue(value.as(OBJECT_LIST).equals(value.as(OBJECT_LIST)));
        assertNotEquals(0, objectList1.hashCode());
        assertNotNull(objectList1.toString());

        assertEquals(receivedObjects, objectList1);
        assertEquals(receivedObjects, objectList2);

        if (value.hasMembers()) {
            Map<Object, Object> objectMap1 = value.as(OBJECT_OBJECT_MAP);
            assertTrue(objectMap1.keySet().equals(value.getMemberKeys()));
        } else {
            assertFails(() -> value.as(OBJECT_OBJECT_MAP), ClassCastException.class);
        }

        Map<Long, Object> objectMap2 = value.as(LONG_OBJECT_MAP);
        Map<Integer, Object> objectMap3 = value.as(INTEGER_OBJECT_MAP);
        Map<Number, Object> objectMap4 = value.as(NUMBER_OBJECT_MAP);

        assertFails(() -> value.as(SHORT_OBJECT_MAP), ClassCastException.class);
        assertFails(() -> value.as(BYTE_OBJECT_MAP), ClassCastException.class);
        assertFails(() -> value.as(FLOAT_OBJECT_MAP), ClassCastException.class);
        assertFails(() -> value.as(DOUBLE_OBJECT_MAP), ClassCastException.class);

        try {
            assertEquals(receivedObjectsLongMap, objectMap2);
        } catch (AssertionError e) {
            throw e;
        }
        assertEquals(receivedObjectsIntMap, objectMap3);
        assertEquals(receivedObjectsLongMap, objectMap4);

        if (value.isHostObject()) {
            assertTrue(value.as(Object.class) instanceof List || value.as(Object.class).getClass().isArray());
        } else if (!value.hasMembers()) {
            List<Object> objectMap5 = (List<Object>) value.as(Object.class);
            assertEquals(receivedObjects, objectMap5);
        }

        // write them all
        for (long i = 0L; i < value.getArraySize(); i++) {
            value.setArrayElement(i, value.getArrayElement(i));
        }

        for (int i = 0; i < value.getArraySize(); i++) {
            objectList1.set(i, receivedObjects.get(i));
            objectList2.set(i, receivedObjects.get(i));
        }
    }

    static void assertFails(Runnable runnable, Class<? extends Throwable> exceptionType) {
        assertFails(() -> {
            runnable.run();
            return null;
        }, exceptionType);
    }

    static <T extends Throwable> void assertFails(Callable<?> callable, Class<T> exceptionType, Consumer<T> verifier) {
        try {
            callable.call();
        } catch (Throwable t) {
            if (!exceptionType.isInstance(t)) {
                throw new AssertionError("expected instanceof " + exceptionType.getName() + " was " + t.getClass().getName(), t);
            }
            verifier.accept(exceptionType.cast(t));
            return;
        }
        fail("expected " + exceptionType.getName() + " but no exception was thrown");
    }

    static <T extends Throwable> void assertFails(Runnable run, Class<T> exceptionType, Consumer<T> verifier) {
        try {
            run.run();
        } catch (Throwable t) {
            if (!exceptionType.isInstance(t)) {
                throw new AssertionError("expected instanceof " + exceptionType.getName() + " was " + t.getClass().getName(), t);
            }
            verifier.accept(exceptionType.cast(t));
            return;
        }
        fail("expected " + exceptionType.getName() + " but no exception was thrown");
    }

    public static void assertFails(Callable<?> callable, Class<? extends Throwable> exceptionType) {
        try {
            callable.call();
        } catch (Throwable t) {
            if (!exceptionType.isInstance(t)) {
                throw new AssertionError("expected instanceof " + exceptionType.getName() + " was " + t.getClass().getName(), t);
            }
            return;
        }
        fail("expected " + exceptionType.getName() + " but no exception was thrown");
    }

    private static void assertValueNumber(Value value) {
        assertTrue(value.isNumber());

        if (value.fitsInByte()) {
            value.asByte();
        } else {
            assertFails(() -> value.asByte(), ClassCastException.class);
        }
        if (value.fitsInShort()) {
            short shortValue = value.asShort();
            assertEquals((byte) shortValue == shortValue, value.fitsInByte());
        } else {
            assertFails(() -> value.asShort(), ClassCastException.class);
        }

        if (value.fitsInInt()) {
            int intValue = value.asInt();
            assertEquals((byte) intValue == intValue, value.fitsInByte());
            assertEquals((short) intValue == intValue, value.fitsInShort());
        } else {
            assertFails(() -> value.asInt(), ClassCastException.class);
        }

        if (value.fitsInLong()) {
            long longValue = value.asLong();
            assertEquals((byte) longValue == longValue, value.fitsInByte());
            assertEquals((short) longValue == longValue, value.fitsInShort());
            assertEquals((int) longValue == longValue, value.fitsInInt());
        } else {
            assertFails(() -> value.asLong(), ClassCastException.class);
        }

        if (value.fitsInFloat()) {
            value.asFloat();
        } else {
            assertFails(() -> value.asFloat(), ClassCastException.class);
        }

        if (value.fitsInDouble()) {
            value.asDouble();
        } else {
            assertFails(() -> value.asDouble(), ClassCastException.class);
        }
    }

    private static void assertClassMembers(Value value, Class<?> expectedClass, boolean staticMembers) {
        for (java.lang.reflect.Method m : expectedClass.getMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()) == staticMembers) {
                assertTrue(m.getName(), value.hasMember(m.getName()));
            }
        }
    }

    @Implementable
    public interface EmptyInterface {

    }

    @Implementable
    public interface NonFunctionalInterface {
        void foobarbaz();

        // rare name that has no conflicts
        void oldmacdonaldhadafarm();
    }

    @FunctionalInterface
    public interface IsFunctionalInterfaceVarArgs {
        Object foobarbaz(Object... args);
    }

    @SuppressWarnings({"unchecked", "cast"})
    private static void assertFunctionalInterfaceMapping(Value value) {
        if (value.isHostObject()) {
            assertNotNull(value.as(Function.class));
        } else {
            assertNotNull((Function<Object, Object>) value.as(Object.class));
        }

        assertNotNull(value.as(Function.class));
        assertNotNull(value.as(IsFunctionalInterfaceVarArgs.class));

        // mapping an empty interface works with any value.
        if (value.isNull()) {
            assertNull(value.as(EmptyInterface.class));
        } else {
            if (value.hasMembers()) {
                assertTrue(value.as(EmptyInterface.class) instanceof EmptyInterface);
            } else {
                assertFails(() -> value.as(EmptyInterface.class), ClassCastException.class);
            }
        }
        Function<Object, Object> f = (Function<Object, Object>) value.as(Function.class);
        assertEquals(f, f);
        assertEquals(value.as(Function.class), value.as(Function.class));
        assertNotEquals(value.as(Function.class), (Function<Object, Object>) (e) -> e);
        assertNotNull(value.as(Function.class).toString());
        assertNotEquals(0, value.as(Function.class).hashCode());

        if (value.hasMembers() && !value.hasMember("foobarbaz")) {
            assertFails(() -> value.as(NonFunctionalInterface.class).foobarbaz(), UnsupportedOperationException.class);
        }

    }

    static Trait[] detectSupportedTypes(Value value) {
        List<Trait> valueTypes = new ArrayList<>();
        if (value.isBoolean()) {
            valueTypes.add(BOOLEAN);
        }
        if (value.isHostObject()) {
            valueTypes.add(HOST_OBJECT);
        }
        if (value.isNativePointer()) {
            valueTypes.add(NATIVE);
        }
        if (value.isString()) {
            valueTypes.add(STRING);
        }
        if (value.isNumber()) {
            valueTypes.add(NUMBER);
        }
        if (value.hasMembers()) {
            valueTypes.add(MEMBERS);
        }
        if (value.hasArrayElements()) {
            valueTypes.add(ARRAY_ELEMENTS);
        }
        if (value.canInstantiate()) {
            valueTypes.add(INSTANTIABLE);
        }
        if (value.canExecute()) {
            valueTypes.add(EXECUTABLE);
        }
        if (value.isNull()) {
            valueTypes.add(NULL);
        }
        if (value.isProxyObject()) {
            valueTypes.add(PROXY_OBJECT);
        }
        return valueTypes.toArray(new Trait[0]);
    }

    public enum Trait {

        NULL,
        HOST_OBJECT,
        PROXY_OBJECT,
        NUMBER,
        STRING,
        BOOLEAN,
        NATIVE,
        EXECUTABLE,
        INSTANTIABLE,
        MEMBERS,
        ARRAY_ELEMENTS

    }

}
