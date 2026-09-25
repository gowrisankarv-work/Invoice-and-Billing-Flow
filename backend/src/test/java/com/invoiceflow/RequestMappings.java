package com.invoiceflow;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** ArchUnit condition matching classes whose type or method mappings declare a path with a prefix. */
final class RequestMappings {

    private static Stream<Class<?>> mappingAnnotations() {
        return Stream.of(RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class,
                PatchMapping.class, DeleteMapping.class);
    }

    private RequestMappings() {
    }

    static ArchCondition<JavaClass> startingWith(String prefix) {
        return new ArchCondition<>("declare a request mapping starting with " + prefix) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Stream.concat(Stream.of(javaClass), javaClass.getMethods().stream())
                        .flatMap(RequestMappings::paths)
                        .filter(path -> path.startsWith(prefix))
                        .forEach(path -> events.add(SimpleConditionEvent.satisfied(javaClass,
                                javaClass.getName() + " maps " + path)));
            }
        };
    }

    private static Stream<String> paths(HasAnnotations<?> element) {
        return mappingAnnotations()
                .map(type -> element.tryGetAnnotationOfType(type.getName()))
                .flatMap(Optional::stream)
                .flatMap(RequestMappings::pathValues);
    }

    private static Stream<String> pathValues(JavaAnnotation<?> annotation) {
        return Stream.of("value", "path")
                .map(annotation::get)
                .flatMap(Optional::stream)
                .flatMap(value -> value instanceof Object[] array ? Arrays.stream(array) : Stream.of(value))
                .map(String::valueOf);
    }
}
