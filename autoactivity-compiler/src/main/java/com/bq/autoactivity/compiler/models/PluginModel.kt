package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.Callback
import com.bq.autoactivity.compiler.asElement
import com.bq.autoactivity.compiler.enclosedAndInheritedElements
import com.bq.autoactivity.compiler.hasAnnotation
import com.squareup.javapoet.ClassName
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class PluginModel(val method: ExecutableElement) {
    val className: ClassName
    val type: TypeMirror
    val element: TypeElement

    val callbackMethods: List<ExecutableElement>

    init {
        type = method.returnType
        element = type.asElement() as TypeElement
        className = ClassName.get(method.returnType) as ClassName

        callbackMethods = element.enclosedAndInheritedElements()
                .filter { it.kind == ElementKind.METHOD }
                .filter { it.hasAnnotation(Callback::class.java) }
                .map { it as ExecutableElement }
    }
}