import ceylon.language.metamodel {
    AppliedClass = Class,
    AppliedType
}

shared interface Class 
        satisfies ClassOrInterface {
    shared formal actual AppliedClass<Anything, Nothing> apply(AppliedType* types);
}