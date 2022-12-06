package wooga.gradle.wdk.internal

import com.wooga.gradle.test.PropertyUtils

class BasicSnippets implements BasicSnippetsTrait {}

trait BasicSnippetsTrait {

    static String mockTask(String taskName, String dryRunExecutionBlock = "") {
        return """
        ${taskName}.doFirst { 
            logger.info("dry running task $taskName")
            ${dryRunExecutionBlock}
            throw new org.gradle.api.tasks.StopExecutionException("task dry run finished")
        }
       """
    }


    static String extension(String extensionName, @DelegatesTo(Map) Closure valuesCls) {
        def values = new HashMap<String, ?>()
        def cls = valuesCls.clone()
        cls.resolveStrategy = Closure.DELEGATE_FIRST
        cls.delegate = values
        cls(values)

        return extension(values, extensionName)
    }

    static String extension(Map<String, ?> extensionValues, String extensionName) {
        return """
        ${extensionName} {
        ${
            extensionValues.entrySet()
                    .collect { "${it.key} = ${resolveValue(it.value)}" }
                    .join("\n")
        }
        }
       """
    }

    private static String resolveValue(Object value) {
        if(value instanceof Collection) {
            return wrap(value[0], value[1])
        }
        if(value instanceof GString) {
            value = value.toString()
        }
        return wrap(value, value.class)
    }

    static String wrap(Object rawValue, Class type) {
        return PropertyUtils.wrapValueBasedOnType(rawValue, type)
    }

    static String wrap(Object rawValue, String type) {
        return PropertyUtils.wrapValueBasedOnType(rawValue, type)
    }
}
