package com.cluster.dashboard.util;

import io.kubernetes.client.custom.Quantity;

public final class ResourceParser {


    public static long parseCpu(Quantity quantity) {
        if (quantity == null) {
            return 0;
        }

        return quantity.getNumber() //usually in base core value
                .multiply(java.math.BigDecimal.valueOf(1000))
                .longValue();   //bigDecimal to long
    }

    public static long parseMemory(Quantity quantity) {

        if (quantity == null) {
            return 0;
        }

        return quantity.getNumber().longValue() / 1024;
    }

    public static long parseCpuToMilli(String cpu) {

        if (cpu == null || cpu.isBlank()) {
            return 0;
        }

        if (cpu.endsWith("n")) {

            long nanoCores =
                    Long.parseLong(
                            cpu.substring(
                                    0,
                                    cpu.length() - 1
                            )
                    );

            return nanoCores / 1_000_000;
        }

        if (cpu.endsWith("m")) {

            return Long.parseLong(
                    cpu.substring(
                            0,
                            cpu.length() - 1
                    )
            );
        }

        return (long)
                (Double.parseDouble(cpu) * 1000);
    }

    public static long parseMemoryToKi(
            String memory) {

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
                memoryKi /
                        (1024.0 * 1024.0);

        return String.format(
                "%.2fGi",
                gi
        );
    }

    public static double utilization(
            long usage,
            long limit) {

        if (limit <= 0) {
            return 0;
        }

        return usage * 100.0 / limit;
    }
}