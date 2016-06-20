package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.*
import com.bq.autoactivity.compiler.AutoActivityProcessor.env
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class ActivityModel(
        originalClassName: ClassName,
        val element: TypeElement,
        val plugins: List<PluginModel>) {

    val activityClassName: ClassName
    val baseClass: TypeMirror
    val baseClassElement: TypeElement
    val callbackMap: HashMap<ExecutableElement, PluginModel>

    init {
        activityClassName = ClassName.get(
                originalClassName.packageName(),
                originalClassName.simpleName() + "Activity")

        baseClass = element.getAnnotation(AutoActivity::class.java).typeMirror(AutoActivity::baseActivity)
        baseClassElement = baseClass.asElement() as TypeElement

        callbackMap = HashMap()

        plugins.forEach {
            it.method.simpleName
        }
    }

    fun generateClass() {
        val componentTypeSpec = TypeSpec.classBuilder(activityClassName)
                .superclass(ClassName.get(baseClass))
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)

        baseClassElement.enclosedAndInheritedElements()
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                .filter { it.simpleName.matches("on[A-Z].*".toRegex()) }
                .forEach {
                    componentTypeSpec.addJavadoc("\$L\n", it.simpleName)
                }

        plugins.forEach {
            it.callbackMethods.forEach {
                componentTypeSpec.addJavadoc("\$L\n", it.simpleName)
            }
        }

        val file = JavaFile.builder(activityClassName.packageName(), componentTypeSpec.build()).build()
        file.writeTo(env.filer)
    }
}