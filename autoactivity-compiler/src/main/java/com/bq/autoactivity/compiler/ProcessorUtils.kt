package com.bq.autoactivity.compiler

import com.bq.autoactivity.compiler.AutoActivityProcessor.env
import com.squareup.javapoet.AnnotationSpec
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun <T> T.typeMirrors(property: KProperty1<T, Array<KClass<*>>>): List<TypeMirror> {
    try {
        property.get(this)
    } catch(ex: MirroredTypesException) {
        return ex.typeMirrors
    } catch (ex: MirroredTypeException) {
        return listOf(ex.typeMirror)
    }
    throw IllegalArgumentException("Property is not a Class<?>[]")
}

fun <T> T.typeMirror(property: KProperty1<T, KClass<*>>): TypeMirror {
    try {
        property.get(this)
    } catch(ex: MirroredTypeException) {
        return ex.typeMirror
    }
    throw IllegalArgumentException("Property is not a Class<?>")
}


fun elementForName(name: String): TypeElement {
    return env.elementUtils.getTypeElement(name)
}

fun <T : Any> KClass<T>.asElement(): Element = env.elementUtils.getTypeElement(this.java.toString())
fun TypeMirror.asElement(): Element = env.typeUtils.asElement(this)


fun <T : Any> KClass<T>.asTypeMirror(): TypeMirror = this.asElement().asType()

fun TypeMirror.implements(base: KClass<*>): Boolean {
    return this.implements(base.asTypeMirror())
}

fun TypeMirror.implements(base: TypeMirror): Boolean {
    return env.typeUtils.isAssignable(this, base)
}

fun Element.copyAnnotations(): Array<AnnotationSpec> {
    return annotationMirrors.map {
        AnnotationSpec.get(it)
    }.toTypedArray()
}

fun <T : Annotation> Element.hasAnnotation(type: Class<T>): Boolean = this.getAnnotation(type) != null

fun TypeElement.enclosedAndInheritedElements(): List<Element> {
    val out = ArrayList<Element>()
    var current: TypeElement? = this

    while (current != null && current.asType().kind != TypeKind.NONE) {
        out.addAll(current.enclosedElements)
        val elem = env.typeUtils.asElement(current.superclass) ?: break
        current = elem as TypeElement
    }
    return out
}