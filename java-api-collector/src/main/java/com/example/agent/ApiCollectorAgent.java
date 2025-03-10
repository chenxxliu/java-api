package com.example.agent;

import java.lang.instrument.Instrumentation;

public class ApiCollectorAgent {
    
    public static void premain(String args, Instrumentation inst) {
        System.out.println("Java Agent启动时加载 - premain方法");
        initializeAgent(inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("Java Agent动态加载 - agentmain方法");
        initializeAgent(inst);
    }

    private static void initializeAgent(Instrumentation inst) {
        inst.addTransformer(new ApiTransformer(), true);
    }
} 
