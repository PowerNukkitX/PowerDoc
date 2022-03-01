package cn.powernukkitx.powerdoc.utils;

public final class DataUtils {
    public static class Pair<A, B> {
        private A a;
        private B b;

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }

        public A getA() {
            return a;
        }

        public Pair<A, B> setA(A a) {
            this.a = a;
            return this;
        }

        public B getB() {
            return b;
        }

        public Pair<A, B> setB(B b) {
            this.b = b;
            return this;
        }
    }
}
