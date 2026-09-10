package com.otilm.openapi.codegen;

import com.otilm.openapi.codegen.testdata.sub1.TestModel;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeResolverTest {

    interface SimpleInterface {
        void method1(String param1, int param2);

        List<String> method2();

        TestModel method3(TestModel model);
    }

    @Test
    void testSimpleTypes() {
        TypeResolver resolver = new TypeResolver(SimpleInterface.class);
        assertEquals("String", resolver.getTypeName(String.class));
        assertEquals("int", resolver.getTypeName(int.class));
        assertEquals("TestModel", resolver.getTypeName(TestModel.class));
    }

    @Test
    void testParameterizedTypes() throws NoSuchMethodException {
        TypeResolver resolver = new TypeResolver(SimpleInterface.class);
        Method method2 = SimpleInterface.class.getMethod("method2");
        Type returnType = method2.getGenericReturnType();

        assertEquals("List<String>", resolver.getTypeName(returnType));
    }

    @Test
    void testImports() {
        TypeResolver resolver = new TypeResolver(SimpleInterface.class);
        Set<String> imports = resolver.getNonConflictingImports();

        assertTrue(imports.contains("java.util.List"));
        assertTrue(imports.contains("com.otilm.openapi.codegen.testdata.sub1.TestModel"));
        assertFalse(imports.contains("java.lang.String"));
        assertFalse(imports.contains("int"));
    }

    interface ConflictInterface {
        com.otilm.openapi.codegen.testdata.sub1.TestModel method1();

        com.otilm.openapi.codegen.testdata.sub2.TestModel method2();
    }

    @Test
    void testNamingConflicts() {
        TypeResolver resolver = new TypeResolver(ConflictInterface.class);

        assertEquals("com.otilm.openapi.codegen.testdata.sub1.TestModel",
                resolver.getTypeName(com.otilm.openapi.codegen.testdata.sub1.TestModel.class));
        assertEquals("com.otilm.openapi.codegen.testdata.sub2.TestModel",
                resolver.getTypeName(com.otilm.openapi.codegen.testdata.sub2.TestModel.class));

        Set<String> imports = resolver.getNonConflictingImports();
        assertFalse(imports.contains("com.otilm.openapi.codegen.testdata.sub1.TestModel"));
        assertFalse(imports.contains("com.otilm.openapi.codegen.testdata.sub2.TestModel"));
    }

    interface ComplexInterface {
        Map<String, List<TestModel>> complexMethod(Set<? extends TestModel> set, List<? super TestModel> list);

        TestModel[] arrayMethod(int[] primitiveArray, TestModel[][] multiArray);
    }

    @Test
    void testComplexTypes() throws NoSuchMethodException {
        TypeResolver resolver = new TypeResolver(ComplexInterface.class);
        Method method = ComplexInterface.class.getMethod("complexMethod", Set.class, List.class);

        assertEquals("Map<String, List<TestModel>>", resolver.getTypeName(method.getGenericReturnType()));
        assertEquals("Set<? extends TestModel>", resolver.getTypeName(method.getGenericParameterTypes()[0]));
        assertEquals("List<? super TestModel>", resolver.getTypeName(method.getGenericParameterTypes()[1]));
    }

    @Test
    void testArrays() throws NoSuchMethodException {
        TypeResolver resolver = new TypeResolver(ComplexInterface.class);
        Method method = ComplexInterface.class.getMethod("arrayMethod", int[].class, TestModel[][].class);

        assertEquals("TestModel[]", resolver.getTypeName(method.getGenericReturnType()));
        assertEquals("int[]", resolver.getTypeName(method.getGenericParameterTypes()[0]));
        assertEquals("TestModel[][]", resolver.getTypeName(method.getGenericParameterTypes()[1]));
    }

    interface GenericBoundInterface<T extends TestModel> {
        T genericMethod(T param);
    }

    @Test
    void testGenericBounds() throws NoSuchMethodException {
        TypeResolver resolver = new TypeResolver(GenericBoundInterface.class);
        Method method = GenericBoundInterface.class.getMethod("genericMethod", TestModel.class);

        // TypeVariable T should return "T"
        assertEquals("T", resolver.getTypeName(method.getGenericReturnType()));
    }

    interface WildcardBoundInterface {
        void wildcardMethod(List<? extends TestModel> list1, List<? super TestModel> list2, List<?> list3);
    }

    @Test
    void testWildcardBounds() throws NoSuchMethodException {
        TypeResolver resolver = new TypeResolver(WildcardBoundInterface.class);
        Method method = WildcardBoundInterface.class.getMethod("wildcardMethod", List.class, List.class, List.class);

        assertEquals("List<? extends TestModel>", resolver.getTypeName(method.getGenericParameterTypes()[0]));
        assertEquals("List<? super TestModel>", resolver.getTypeName(method.getGenericParameterTypes()[1]));
        assertEquals("List<?>", resolver.getTypeName(method.getGenericParameterTypes()[2]));
    }
}
