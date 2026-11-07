package pl.ks.profiling.gc.generator;

/**
 * Lets the JIT compile methods under optimistic assumptions and then breaks those assumptions, so compiled
 * methods are "made not entrant" and recompiled:
 * <ul>
 *     <li>monomorphic call sites that later see other receiver types (class check traps),</li>
 *     <li>a branch that is never taken during warm-up (unstable if trap).</li>
 * </ul>
 */
final class DeoptimizationScenario implements Scenario {
    private static final int WARM_UP_ITERATIONS = 30_000;
    private static final int ARRAY_SIZE = 64;

    private final Config config;
    private volatile double sink;

    DeoptimizationScenario(Config config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "JIT deoptimization";
    }

    @Override
    public void run() {
        Shape[] circles = fill(new Circle(1.5));
        Shape[] squares = fill(new Square(2.0));
        Shape[] triangles = fill(new Triangle(3.0, 4.0));

        // Each method has its own call site and its own profile
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            sink += sumAreasA(circles) + sumAreasB(squares) + sumAreasC(triangles);
        }
        Timing.sleep(config.millis(300));
        sink += sumAreasA(squares) + sumAreasB(triangles) + sumAreasC(circles);

        // Warm up again with two types (bimorphic) and break it with a third one
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            sink += sumAreasA(i % 2 == 0 ? circles : squares);
        }
        Timing.sleep(config.millis(300));
        sink += sumAreasA(triangles);

        // Unstable if: the branch was never taken while the method was profiled
        long total = 0;
        for (int i = 0; i < WARM_UP_ITERATIONS * 10; i++) {
            total += branchy(i);
        }
        Timing.sleep(config.millis(300));
        total += branchy(-1);
        sink += total;
        Timing.sleep(config.millis(500));
    }

    private static Shape[] fill(Shape shape) {
        Shape[] shapes = new Shape[ARRAY_SIZE];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = shape;
        }
        return shapes;
    }

    private static double sumAreasA(Shape[] shapes) {
        double sum = 0;
        for (Shape shape : shapes) {
            sum += shape.area();
        }
        return sum;
    }

    private static double sumAreasB(Shape[] shapes) {
        double sum = 0;
        for (Shape shape : shapes) {
            sum += shape.area() * 2;
        }
        return sum;
    }

    private static double sumAreasC(Shape[] shapes) {
        double sum = 0;
        for (Shape shape : shapes) {
            sum += shape.area() / 2;
        }
        return sum;
    }

    private static long branchy(int value) {
        if (value < 0) {
            return slowPath(value);
        }
        return value * 31L + 7;
    }

    private static long slowPath(int value) {
        return String.valueOf(value).hashCode();
    }

    interface Shape {
        double area();
    }

    static final class Circle implements Shape {
        private final double radius;

        Circle(double radius) {
            this.radius = radius;
        }

        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    static final class Square implements Shape {
        private final double side;

        Square(double side) {
            this.side = side;
        }

        @Override
        public double area() {
            return side * side;
        }
    }

    static final class Triangle implements Shape {
        private final double base;
        private final double height;

        Triangle(double base, double height) {
            this.base = base;
            this.height = height;
        }

        @Override
        public double area() {
            return base * height / 2;
        }
    }
}
