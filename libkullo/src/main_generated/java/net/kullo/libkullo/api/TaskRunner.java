// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from taskrunner.djinni

package net.kullo.libkullo.api;

public abstract class TaskRunner {
    /**
     * Runs the given task asynchronously (simplest case: in a new thread)
     * and returns immediately.
     */
    public abstract void runTaskAsync(Task task);
}