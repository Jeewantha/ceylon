void testNonempty() {
    Integer[]? seq1 = [1,1,1];
    assert(nonempty [Integer+] result1 = seq1);
    Integer?[] seq2 = [1,1,1,null];
    assert(nonempty $error [Integer+] result2 = seq2);
    Object[] seq3 = [1,1,1,""];
    assert(nonempty $error [Integer+] result3 = seq3);
    String str1 = "xyz";
    $error assert(nonempty result4 = str1);
    String str2 = "xyz";
    $error assert(nonempty [Character+] result5 = str2);
    Integer|Float? num = null;
    assert (exists $error Integer int = num);
}
