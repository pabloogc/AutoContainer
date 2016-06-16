package com.bq.autoactivity.compiler

import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.models.ComponentModel
import com.squareup.javapoet.*
import dagger.Component
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

object AutoActivityProcessor : AbstractProcessor() {

    lateinit var env: ProcessingEnvironment

    override fun init(env: ProcessingEnvironment) {
        this.env = env
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(AutoActivity::class.java)
                .let { ElementFilter.typesIn(it) } //Poor man's filter
                .filter { it.kind == ElementKind.INTERFACE } //Only interfaces
                .map { element ->
                    ComponentModel(element)
                }
                .forEach { model ->
                    val from = ClassName.get(model.element)
                    val gen = ClassName.get(from.packageName(), from.simpleName() + "AutoComponent")
                    val component = TypeSpec.interfaceBuilder(gen)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Component::class.java)

                    component.addJavadoc("Generated at ${Date()}\n")
                    model.element.enclosedElements
                            .filter { it.kind == ElementKind.METHOD }
                            .map { it as ExecutableElement }
                            .forEach {
                                component.addMethod(
                                        MethodSpec.methodBuilder(it.simpleName.toString())
                                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                                .returns(TypeName.get(it.returnType))
                                                .build()
                                )
                            }

                    val file = JavaFile.builder(from.packageName(), component.build()).build()
                    file.writeTo(env.filer)
                }

        return true
    }

}