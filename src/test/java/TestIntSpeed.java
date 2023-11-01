public class TestIntSpeed {

    public static void main(String[] args) {
        byte x = -5;
        System.out.println(x >> 1);
    }

    public static void test() {
        long before = System.nanoTime();

        int x = 1;
        for (long i = 0; i < 20000000000L; i++)
            x += 1;
        long after = System.nanoTime();
        System.out.println("int + took " + (after - before) / 1000000 + " ms");
        before = after;

        long y = 1;
        for (long j = 0; j < 20000000000L; j++)
            y += 1;
        after = System.nanoTime();
        System.out.println("long + took " + (after - before) / 1000000 + " ms");
        before = after;

        for (long i = 0; i < 20000000000L; i++)
            y = (i / 567L);
        after = System.nanoTime();
        System.out.println("long div took " + (after - before) / 1000000 + " ms");
        before = after;

        for (long i = 0; i < 20000000000L; i++)
            y = Long.divideUnsigned(i, 567L);
        after = System.nanoTime();
        System.out.println("ulong div took " + (after - before) / 1000000 + " ms");
        before = after;

    }


}
