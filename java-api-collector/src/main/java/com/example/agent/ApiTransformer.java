package com.example.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import javassist.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;

public class ApiTransformer implements ClassFileTransformer {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> processedClasses = new HashSet<>();
    private static final String OUTPUT_FILE = "api_collection.json";
    private static final List<Map<String, Object>> apiList = new ArrayList<>();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || className.startsWith("java/") || className.startsWith("sun/")) {
            return null;
        }

        try {
            String normalizedClassName = className.replace('/', '.');
            if (processedClasses.contains(normalizedClassName)) {
                return null;
            }
            processedClasses.add(normalizedClassName);

            ClassPool cp = ClassPool.getDefault();
            cp.insertClassPath(new ByteArrayClassPath(normalizedClassName, classfileBuffer));
            CtClass ctClass = cp.get(normalizedClassName);

            if (ctClass.hasAnnotation("org.springframework.web.bind.annotation.RestController") ||
                ctClass.hasAnnotation("org.springframework.stereotype.Controller")) {
                processControllerClass(ctClass);
            }

            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processControllerClass(CtClass ctClass) throws NotFoundException {
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            try {
                if (isRequestMapping(method)) {
                    Map<String, Object> apiInfo = collectApiInfo(method);
                    if (apiInfo != null) {
                        apiList.add(apiInfo);
                        saveApiInfo();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isRequestMapping(CtMethod method) {
        try {
            Object[][] annotations = method.getAvailableAnnotations();
            for (Object[] annotation : annotations) {
                String annotationName = annotation[0].getClass().getName();
                if (annotationName.contains("RequestMapping") ||
                    annotationName.contains("GetMapping") ||
                    annotationName.contains("PostMapping")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Map<String, Object> collectApiInfo(CtMethod method) {
        try {
            Map<String, Object> apiInfo = new HashMap<>();
            String path = extractPath(method);
            String httpMethod = extractHttpMethod(method);
            List<Map<String, Object>> parameters = extractParameters(method);

            apiInfo.put("path", path);
            apiInfo.put("method", httpMethod.toLowerCase());
            apiInfo.put("parameters", parameters);
            apiInfo.put("controller", method.getDeclaringClass().getName());
            
            Map<String, Object> responses = new HashMap<>();
            responses.put("200", createSuccessResponse());
            apiInfo.put("responses", responses);

            return apiInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractPath(CtMethod method) {
        // 简化版实现，实际项目中需要处理更多注解类型
        try {
            Object[][] annotations = method.getAvailableAnnotations();
            for (Object[] annotation : annotations) {
                if (annotation[0].toString().contains("RequestMapping")) {
                    // 这里简化处理，实际需要解析注解的value属性
                    return "/path/to/endpoint";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "/default/path";
    }

    private String extractHttpMethod(CtMethod method) {
        try {
            Object[][] annotations = method.getAvailableAnnotations();
            for (Object[] annotation : annotations) {
                String annotationName = annotation[0].getClass().getName();
                if (annotationName.contains("GetMapping")) {
                    return "GET";
                } else if (annotationName.contains("PostMapping")) {
                    return "POST";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "GET";
    }

    private List<Map<String, Object>> extractParameters(CtMethod method) {
        List<Map<String, Object>> parameters = new ArrayList<>();
        try {
            CtClass[] parameterTypes = method.getParameterTypes();
            Object[][] paramAnnotations = method.getAvailableParameterAnnotations();

            for (int i = 0; i < parameterTypes.length; i++) {
                CtClass paramType = parameterTypes[i];
                if (paramType.subtypeOf(ClassPool.getDefault().get(HttpServletRequest.class.getName()))) {
                    continue;
                }

                Map<String, Object> paramInfo = new HashMap<>();
                paramInfo.put("name", "param" + i);
                paramInfo.put("in", "query");
                paramInfo.put("required", true);
                
                Map<String, String> schema = new HashMap<>();
                schema.put("type", "string");
                paramInfo.put("schema", schema);

                parameters.add(paramInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parameters;
    }

    private Map<String, Object> createSuccessResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> mediaType = new HashMap<>();
        Map<String, Object> schema = new HashMap<>();
        
        schema.put("type", "object");
        mediaType.put("schema", schema);
        content.put("*/*", mediaType);
        
        response.put("description", "ok");
        response.put("content", content);
        
        return response;
    }

    private synchronized void saveApiInfo() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(OUTPUT_FILE), apiList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 