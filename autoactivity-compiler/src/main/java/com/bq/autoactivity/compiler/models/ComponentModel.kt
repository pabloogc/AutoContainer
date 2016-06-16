package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.compiler.AutoActivityProcessor
import com.squareup.javapoet.*
import dagger.Component
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class ComponentModel(val element: TypeElement) {

    val SUFFIX: String = "Component"
    var generated = false;
    val originalClassName: ClassName
    val generatedClassName: ClassName

    init {
        originalClassName = ClassName.get(element)
        generatedClassName = ClassName.get(
                originalClassName.packageName(),
                originalClassName.simpleName() + SUFFIX)
    }

    fun generateClass() {
        if (generated) return
        generated = true;

        val componentTypeSpec = TypeSpec.interfaceBuilder(generatedClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component::class.java)

        element.enclosedElements
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                .forEach {
                    componentTypeSpec.addMethod(
                            MethodSpec.methodBuilder(it.simpleName.toString())
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .returns(TypeName.get(it.returnType))
                                    .build()
                    )
                }


        AutoActivityProcessor.env.filer
        val file = JavaFile.builder(generatedClassName.packageName(), componentTypeSpec.build()).build()
        file.writeTo(AutoActivityProcessor.env.filer)
    }
}