package com.study.annotation;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.springframework.util.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class CustomLombokProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(CustomLombok.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(CustomLombok.class);

        for (Element element : elements) {
            Name elementName = element.getSimpleName();

            if (element.getKind() != ElementKind.CLASS) {
                printMessage(Diagnostic.Kind.ERROR, "CustomLombok can not be used on " + elementName);
            } else {
                printMessage(Diagnostic.Kind.NOTE, "Processing " + elementName);
            }

            TypeElement typeElement = (TypeElement) element;
            ClassName className = ClassName.get(typeElement);

            TypeSpec typeSpec = createTypeSpec(typeElement, className);

            Filer filer = processingEnv.getFiler();
            try {
                JavaFile.builder(className.packageName(), typeSpec)
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FETAL ERROR: " + e);
            }
        }

        return true;
    }

    private void printMessage(Diagnostic.Kind kind, String message) {
        processingEnv.getMessager().printMessage(kind, message);
    }

    private TypeSpec createTypeSpec(TypeElement typeElement, ClassName className) {
        List<? extends Element> fields = typeElement.getEnclosedElements();

        List<FieldSpec> fieldSpecs = new ArrayList<>();
        List<MethodSpec> methodSpecs = new ArrayList<>();

        for (Element field : fields) {
            if (field.getKind() != ElementKind.FIELD) {
                continue;
            }

            TypeName fieldTypeName = TypeName.get(field.asType());
            String fieldName = field.getSimpleName().toString();

            fieldSpecs.add(getFieldSpec(fieldTypeName, fieldName));
            methodSpecs.add(getGetterSpec(fieldTypeName, fieldName));
            methodSpecs.add(getSetterSpec(fieldTypeName, fieldName));
        }

        return TypeSpec.classBuilder(className.simpleName() + "Data")
                .addModifiers(Modifier.PUBLIC)
                .addFields(fieldSpecs)
                .addMethods(methodSpecs)
                .build();
    }

    private FieldSpec getFieldSpec(TypeName fieldTypeName, String fieldName) {
        return FieldSpec.builder(fieldTypeName, fieldName)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec getGetterSpec(TypeName fieldTypeName, String fieldName) {
        String methodName = String.format("get%s", StringUtils.capitalize(fieldName));
        String statement = String.format("return this.%s", fieldName);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldTypeName)
                .addStatement(statement)
                .build();
    }

    private MethodSpec getSetterSpec(TypeName fieldTypeName, String fieldName) {
        String methodName = String.format("set%s", StringUtils.capitalize(fieldName));

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldTypeName, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }

}
