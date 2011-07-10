interface Declarations {
    
    class Y() {}
    class X(Y y) {}
    
    void doSomething() {}
    
    Y createSomething() { return Y(); }
    
    Y something { return createSomething(); }
    
    class WithValueArg(@error Object value) {}
    void withValueArg(@error Object value) {}
    
    //@error class value() {}
    //@error void object() {}
    //@error value function = "hello";
    
    @error class WithoutArgs {}
    @error void withoutArgs {}
    
    @error assign withoutGetter {}
    
    @error X methodWithoutReturn() {
        doSomething();
    }
    
    @error X getterWithoutReturn {
        doSomething();
    }
    
    @error class WithoutClassDef();
    @error interface WithoutInterfaceDef;
    @error object withoutObjectDef;

    /*class Foo(String bar) {}
    @error Foo getFoo() {}
    @error Foo foo {}*/
    
    void duplicate() {}
    @error void duplicate(Y y) {}
    
    class Dupe() {}
    @error class Dupe(X x) {}
    
    void withDupeParam(X x, @error X x) {}
    void withoutDupeParam(X x(Y y), X y(Y y)) {}
    
    @error class () {}
    @error void () {}
    @error interface {}
    @error value { return Y(); }
    @error function () { return Y(); }
    
    object x {
        String hello = "Hello";
    }
    
    void a(String s = "a string") {}
    class A(Natural count = 1) {}
    
    void dup(String name, @error String name) {}
    
}