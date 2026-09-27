package com.cluster.dashboard.util;

public final class ResourceParser {

    private ResourceParser() {}

    public static long parseCpuToMilli(String cpu) {

        if (cpu == null || cpu.isBlank()) {
            return 0;
        }

        if (cpu.endsWith("n")) {    //nano

            long nanoCores = Long.parseLong(cpu.substring(0, cpu.length() - 1));

            return nanoCores / 1_000_000;
        }

        if (cpu.endsWith("m")) {            //Mi

            return Long.parseLong(cpu.substring( 0, cpu.length() - 1));
        }

        return (long) (Double.parseDouble(cpu) * 1000);         //Gi
    }

    public static long parseMemoryToKi(String memory) {

        if (memory == null || memory.isBlank()) {
            return 0;
        }

        if (memory.endsWith("Ki")) {

            return Long.parseLong(
                    memory.substring(
                            0,
                            memory.length() - 2
                    )
            );
        }

        if (memory.endsWith("Mi")) {

            return Long.parseLong(
                    memory.substring(
                            0,
                            memory.length() - 2
                    )
            ) * 1024;
        }

        if (memory.endsWith("Gi")) {

            return Long.parseLong(
                    memory.substring(
                            0,
                            memory.length() - 2
                    )
            ) * 1024 * 1024;
        }

        return Long.parseLong(memory);
    }

    public static String cpuToString(
            long milliCpu) {

        return milliCpu + "m";
    }

    public static String memoryToGi(
            long memoryKi) {

        double gi =
                memoryKi / (1024.0 * 1024.0);

        return String.format(
                "%.2fGi",
                gi
        );
    }

    public static double utilization(
            long usage,
            long limit) {

        if (limit <= 0) {return 0;}

        return usage * 100.0 / limit;
    }
}