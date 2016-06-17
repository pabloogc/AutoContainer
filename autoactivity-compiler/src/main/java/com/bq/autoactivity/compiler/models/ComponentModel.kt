package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.AutoActivityProcessor
import com.bq.autoactivity.compiler.copyAnnotations
import com.squareup.javapoet.*
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class ComponentModel(val element: TypeElement) {

    var generated = false;

    val plugins: List<Plugin>

    val originalClassName: ClassName
    val generatedClassName: ClassName
    val moduleClassName: ClassName

    init {
        originalClassName = ClassName.get(element)
        generatedClassName = ClassName.get(
                originalClassName.packageName(),
                originalClassName.simpleName() + "Component")

        moduleClassName = ClassName.get(
                originalClassName.packageName(),
                originalClassName.simpleName() + "Module")

        plugins = element.enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                .map { Plugin(ClassName.get(it.returnType), it) }
    }

    fun generateClass() {
        if (generated) return
        generated = true;

        generateComponentClass()
        generateModuleClass()
    }

    private fun generateComponentClass() {
        val componentTypeSpec = TypeSpec.interfaceBuilder(generatedClassName)
                .addModifiers(Modifier.PUBLIC)

        //Replace @AutoActivity Annotation with @Component, everything else is the same
        val componentAnnotations = element.copyAnnotations();
        componentAnnotations.forEachIndexed { i, annotationSpec ->
            if (annotationSpec.type.equals(ClassName.get(AutoActivity::class.java))) {
                val builder = AnnotationSpec.builder(Component::class.java);
                annotationSpec.members.forEach { member ->
                    member.value.forEach {
                        builder.addMember(member.key, it)
                    }
                }
                builder.addMember("modules", "\$T.class", moduleClassName)
                componentAnnotations[i] = builder.build();
            }
        }
        componentTypeSpec.addAnnotations(componentAnnotations.asIterable())

        //Copy all the methods
        plugins.forEach {
            componentTypeSpec.addMethod(
                    MethodSpec.methodBuilder(it.element.simpleName.toString())
                            .addAnnotations(it.element.copyAnnotations().asIterable())
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(it.typeName)
                            .build()
            )
        }

        //Generate injection points for unique types
        plugins.distinctBy { it.typeName }
                .forEach {
                    componentTypeSpec.addMethod(
                            MethodSpec.methodBuilder("inject")
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .addParameter(it.typeName, it.element.simpleName.toString())
                                    .returns(TypeName.VOID)
                                    .build()
                    )
                }

        val file = JavaFile.builder(generatedClassName.packageName(), componentTypeSpec.build()).build()
        file.writeTo(AutoActivityProcessor.env.filer)
    }

    private fun generateModuleClass() {
        val componentTypeSpec = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module::class.java)

        plugins.forEach {
            componentTypeSpec.addMethod(
                    MethodSpec.methodBuilder("provide${it.element.simpleName.toString().capitalize()}")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Provides::class.java)
                            .addAnnotations(it.element.copyAnnotations().asIterable())
                            .returns(it.typeName)
                            .addStatement("return new \$T()", it.typeName)
                            .build()
            )
        }

        val file = JavaFile.builder(moduleClassName.packageName(), componentTypeSpec.build()).build()
        file.writeTo(AutoActivityProcessor.env.filer)
    }

    data class Plugin(val typeName: TypeName, val element: ExecutableElement)
}