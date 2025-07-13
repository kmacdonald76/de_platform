package com.dataplatform.model;

public class Processing {
    private String mode;
    private String cadence;
    private int taskCpu;
    private String taskMemory;
    private int parallelism;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCadence() {
        return cadence;
    }

    public void setCadence(String cadence) {
        this.cadence = cadence;
    }

    public int getTaskCpu() {
        return taskCpu;
    }

    public void setTaskCpu(int taskCpu) {
        this.taskCpu = taskCpu;
    }

    public String getTaskMemory() {
        return taskMemory;
    }

    public void setTaskMemory(String taskMemory) {
        this.taskMemory = taskMemory;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }
}
