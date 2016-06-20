package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.AutoActivityProcessor.env
import com.bq.autoactivity.compiler.copyAnnotations
import com.bq.autoactivity.compiler.elementForName
import com.bq.autoactivity.compiler.implements
import com.squareup.javapoet.*
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror


class ComponentModel(val element: TypeElement) {

    val PLUGIN_TYPE: TypeMirror = elementForName("com.bq.autoactivity.Plugin").asType()

    var generated = false;

    val plugins: List<PluginModel>
    val originalClassName: ClassName
    val generatedClassName: ClassName
    val moduleClassName: ClassName

    val activityModel: ActivityModel

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
                .filter { it.returnType.implements(PLUGIN_TYPE) }
                .map { PluginModel(it) }

        activityModel = ActivityModel(originalClassName, element, plugins)
    }

    fun generateClass() {
        if (generated) return
        generated = true;

        generateComponentClass()
        generateModuleClass()
        generateActivityClass()
    }

    private fun generateComponentClass() {
        val componentTypeSpec = TypeSpec.interfaceBuilder(generatedClassName)
                .addModifiers(Modifier.PUBLIC)

        //Replace @AutoActivity Annotation with @Component, everything else is the same
        val componentAnnotations = element.copyAnnotations();
        componentAnnotations.forEachIndexed { i, annotationSpec ->
            if (annotationSpec.type.equals(ClassName.get(AutoActivity::class.java))) {
                val builder = AnnotationSpec.builder(Component::class.java);
                annotationSpec.members
                        //Only keep @Component values
                        .filter { it.key == "modules" || it.key == "dependencies" }
                        .forEach { member ->
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
            componentTypeSpec.addMethod(MethodSpec.methodBuilder(it.method.simpleName.toString())
                    .addAnnotations(it.method.copyAnnotations().asIterable())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(it.className)
                    .build()
            )
        }

        //Generate injection points for unique types
        plugins.distinctBy { it.className }
                .forEach {
                    componentTypeSpec.addMethod(MethodSpec.methodBuilder("inject")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(it.className, it.method.simpleName.toString())
                            .returns(TypeName.VOID)
                            .build()
                    )
                }

        val file = JavaFile.builder(generatedClassName.packageName(), componentTypeSpec.build()).build()
        file.writeTo(env.filer)
    }

    private fun generateModuleClass() {
        val componentTypeSpec = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module::class.java)

        plugins.forEach {
            componentTypeSpec.addMethod(
                    MethodSpec.methodBuilder("provide${it.method.simpleName.toString().capitalize()}")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Provides::class.java)
                            .addAnnotations(it.method.copyAnnotations().asIterable())
                            .returns(it.className)
                            .addStatement("return new \$T()", it.className)
                            .build()
            )
        }

        val file = JavaFile.builder(moduleClassName.packageName(), componentTypeSpec.build()).build()
        file.writeTo(env.filer)
    }

    private fun generateActivityClass() {
        activityModel.generateClass()
    }
}